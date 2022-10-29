

PipelineStageWidget {

  width : 150
  height : WrapContent
  padding : [1,1,0]

  children {
    name {
      text: "%(pipelineStage.name)"
      x : centered
      background.draw: false

      width : Intrinsic
      height : Intrinsic
    }

    params {
      listItemArchetype: Pixelator.ParamWidget
      listItemBinding: "pipelineStage.params -> param"

      showing: "%(?pipelineStage.params)"

      y : 2 below name
      gapSize: 1

      width: WrapContent
      height: WrapContent

      background.draw: false
    }

    outputs {
      x: centered
      y: 2 below params
      listItemArchetype: Pixelator.ArtifactWidget
      listItemBinding: "pipelineStage.outputs -> artifact"
      gapSize: 1

      width: WrapContent
      height: WrapContent

      background.draw: false
    }
  }
}

ArtifactWidget {
  type: Div

  background.draw: false
  width: WrapContent
  height: WrapContent

  children {
    imageDisplay {
      scale : scale to 128
      image : "%(artifact.image)"
    }
  }
}

ParamWidget {
  type: Div


  background.draw: false

  children {
    label {
      y: Centered

      width: Intrinsic
      height: Intrinsic

      text: "%(param.name): "
      background.draw: false
    }

    value {
      type: TextInput
      x: 5 right of label
      padding : [2,2,0]

//      width: Intrinsic
//      height: Intrinsic

      text: "%(param.value)"
      twoWayBinding : true
    }
  }
}