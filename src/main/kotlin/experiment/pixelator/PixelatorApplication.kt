package experiment.pixelator

import arx.application.Application
import arx.display.core.Image
import arx.display.windowing.*
import arx.engine.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean


object PixelatorComponent : DisplayComponent() {
    var pipeline = Pipeline().apply {
        val srcImgStage = SourceImage().apply { pathParam.value = "/Users/sbock/Downloads/ai/spring_path.png" }
        addStage(srcImgStage)
        val downscaleStage = LinearDownscale()
        addStage(downscaleStage)
        setInputMapping(srcImgStage, srcImgStage.imageKey, downscaleStage, downscaleStage.imageInKey)
        val paletteStage = ReducePalette()
        addStage(paletteStage)
        setInputMapping(downscaleStage, downscaleStage.imageOutKey, paletteStage, paletteStage.imageKey)
        val reduceStage = ReduceImage()
        addStage(reduceStage)
        setInputMapping(paletteStage, paletteStage.paletteKey, reduceStage, reduceStage.paletteKey)
        setInputMapping(downscaleStage, downscaleStage.imageOutKey, reduceStage, reduceStage.imageInKey)
    }

    var stageWidgets = mapOf<PipelineStage, Widget>()

    val executor = Executors.newSingleThreadExecutor { r -> Thread(r).apply { isDaemon = true } }
    val processing = AtomicBoolean(false)

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
            params = if (stage.parameters.isNotEmpty()) { stage.parameters.toList().sortedBy { it.name } } else { null }
        )
    }

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
                    pipeline.update()
                    processing.set(false)
                }
            }

            var i = 0
            for ((stage, widget) in stageWidgets) {
                widget.x = WidgetPosition.Fixed(i * 200)
                widget.bind("pipelineStage" to PipelineStageInfo(stage))
                i += 1
            }
        }
        return false
    }
}


fun main() {


    Application(1600, 900).run(Engine(mutableListOf(), mutableListOf(WindowingSystemComponent, PixelatorComponent)))
}
