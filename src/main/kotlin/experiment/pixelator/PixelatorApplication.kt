@file:OptIn(ExperimentalSerializationApi::class)

package experiment.pixelator

import arx.application.Application
import arx.core.Noto
import arx.core.Resources.config
import arx.display.core.Image
import arx.display.windowing.*
import arx.display.windowing.components.FileInput
import arx.display.windowing.components.FileInputChosen
import arx.engine.*
import com.typesafe.config.ConfigRenderOptions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.hocon.encodeToConfig
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean


object PixelatorComponent : DisplayComponent() {
    val parametersFile = File("${System.getProperty("user.home")}/.pixelator/parameters.hocon")

    var output = Output(File("/tmp"))

    var pipeline = Pipeline().apply {
        val srcImgStage = SourceImage().apply { pathParam.value = File("/tmp/isometric_tree_1.png") }
        addStage(srcImgStage)

//        val downscaleStage = LinearDownscale()
//        addStage(downscaleStage)
//        setInputMapping(srcImgStage, srcImgStage.imageKey, downscaleStage, downscaleStage.imageInKey)

        val removeBackground = RemoveBackground()
        addStage(removeBackground)
        setInputMapping(srcImgStage, srcImgStage.imageKey, removeBackground, removeBackground.imageInKey)

        val paletteStage = ReducePalette()
        addStage(paletteStage)
        setInputMapping(removeBackground, removeBackground.imageOutKey, paletteStage, paletteStage.imageKey)

        val saturatePaletteStage = AdjustPaletteSaturation()
        addStage(saturatePaletteStage)
        setInputMapping(paletteStage, paletteStage.paletteKey, saturatePaletteStage, saturatePaletteStage.paletteInKey)

        val reduceStage = ReduceImage()
        addStage(reduceStage)
        setInputMapping(saturatePaletteStage, saturatePaletteStage.paletteOutKey, reduceStage, reduceStage.paletteKey)
        setInputMapping(removeBackground, removeBackground.imageOutKey, reduceStage, reduceStage.imageInKey)
    }

    init {
        try {
            val params = Hocon.decodeFromConfig<PipelineConfig>(config(parametersFile.absolutePath))
            for (param in params.parameters) {
                for (stage in pipeline.stages) {
                    if (stage.javaClass.simpleName == param.stageClassName) {
                        for (stageParam in stage.parameters) {
                            if (stageParam.name == param.parameterName) {
                                stageParam.setFromString(param.parameterValue)
                            }
                        }
                    }
                }
            }
            output.directory = File(params.outputPath)
        } catch (e : Exception) {
            Noto.warn("Failed to decode saved pipeline info: $e")
        }
    }

    var stageWidgets = mapOf<PipelineStage, Widget>()
    lateinit var saveWidget : Widget

    val executor = Executors.newSingleThreadExecutor { r -> Thread(r).apply { isDaemon = true } }
    val processing = AtomicBoolean(false)
    val updating = AtomicBoolean(false)

    fun save(outFile : File) {
        val srcStage = pipeline.stages.firstNotNullOf { it as? SourceImage }
        val lastStage = pipeline.stages.last()

//        val fileName = srcStage.pathParam.value.path.substringAfterLast("/")
//        pipeline.artifacts[Pipeline.SlotIdentifier(lastStage, (lastStage as ReduceImage).imageOutKey)]!!.image.writeToFile(output.directory.absolutePath + "/" + fileName)
        pipeline.artifacts[Pipeline.SlotIdentifier(lastStage, (lastStage as ReduceImage).imageOutKey)]!!.image.writeToFile(outFile.absolutePath)
    }

    override fun initialize(world: World) {
        with(world) {
            saveWidget = world[WindowingSystem].createWidget("Pixelator.SaveWidget").apply {
                onEventDo<FileInputChosen> {
                    save(it.file)
                }
            }
        }

    }

    data class ArtifactInfo(
        val image : Image?
    )

    fun artifactInfo(artifact : Artifact?) : ArtifactInfo? {
        if (artifact == null) { return null }
        return ArtifactInfo(artifact.image)
        /*return when(artifact) {
            is Artifact.Gradients -> TODO()
            is Artifact.Image -> {
                ArtifactInfo(artifact.image)
            }
            is Artifact.Palette -> TODO()
            is Artifact.Weights -> TODO()
        }*/
    }

    data class PipelineStageInfo(
        val name : String,
        val outputs : List<ArtifactInfo>,
        val params : List<PipelineStage.Parameter>?
    ) {
        constructor(stage : PipelineStage) : this(
            name = stage.javaClass.simpleName,
            outputs = stage.outputs.mapNotNull {
                artifactInfo(pipeline.artifacts[Pipeline.SlotIdentifier(stage, it)])
            }.toList(),
            params = if (stage.parameters.isNotEmpty()) { stage.parameters.toList().sortedBy { it.name } } else { null },

        )
    }


    data class Output (var directory : File)
    @Serializable
    internal data class PipelineConfig(val parameters : List<ParameterState>, val outputPath: String = "/tmp")
    @Serializable
    internal data class ParameterState(val stageClassName : String, val parameterName : String, val parameterValue : String)

    override fun update(world: World): Boolean {
        with(world) {
            val ws = world[WindowingSystem]
            for (stage in pipeline.stages) {
                if (!stageWidgets.containsKey(stage)) {
                    stageWidgets += stage to ws.createWidget("Pixelator.PipelineStageWidget")
                }
            }
            val excessWidgets = stageWidgets.filter { (k, _) -> !pipeline.stages.contains(k) }
            stageWidgets = stageWidgets - excessWidgets.keys
            excessWidgets.forEach { (_, v) -> v.destroy() }

            if (processing.compareAndSet(false, true)) {
                executor.submit {
                    updating.set(true)
                    if (pipeline.update()) {
                        val parameterStates = mutableListOf<ParameterState>()
                        for (stage in pipeline.stages) {
                            for (parameter in stage.parameters) {
                                parameterStates.add(ParameterState(stage.javaClass.simpleName, parameter.name, parameter.valueAsString()))
                            }
                        }

                        parametersFile.parentFile.mkdirs()
                        parametersFile.writeText(Hocon.encodeToConfig(PipelineConfig(parameterStates, output.directory.absolutePath)).root().render(ConfigRenderOptions.concise()))

                    }
                    processing.set(false)
                    updating.set(false)
                }
            }

            if (! updating.get()) {
                pipeline.cleanUp()
                var i = 0
                var x = 0
                var y = 0
                for ((stage, widget) in stageWidgets) {
                    widget.x = WidgetPosition.Fixed(x)
                    widget.y = WidgetPosition.Fixed(y)
                    widget.bind("pipelineStage" to PipelineStageInfo(stage))
                    i += 1

                    x += 200
                    if (x > 800) {
                        x = 0
                        y += 200
                    }
                }
                saveWidget[FileInput]!!.file
            }
        }
        return false
    }
}


fun main() {


    Application(1600, 900).run(Engine(mutableListOf(), mutableListOf(WindowingSystemComponent, PixelatorComponent)))
}
