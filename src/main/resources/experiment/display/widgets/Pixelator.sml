
SaveWidget {
  type: FileInput
  width : intrinsic
  height : intrinsic

  x: centered
  y: 0 from bottom

  padding : [4,4,0]

  text : "Save"
  fontSize : 20
  horizontalTextAlignment : Centered

  fileInputKind : Save

  fileFilters : "png"
}

PipelineStageWidget {

  width : WrapContent
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

  width: WrapContent
  height: WrapContent

  background.draw: false

  children {
    label {
      y: Centered

      width: Intrinsic
      height: Intrinsic

      showing : "%(param.isTextInput)"
      text: "%(param.name): "
      background.draw: false
    }

    value {
      type: TextInput
      x: 5 right of label
      padding : [2,2,0]

//      width: Intrinsic
//      height: Intrinsic

      showing : "%(param.isTextInput)"
      text: "%(param.value)"
      twoWayBinding : true
    }

    fileLabel {
      x: Centered

      width: Intrinsic
      height: Intrinsic

      showing : "%(param.isFileInput)"
      text: "%(param.name)"
      background.draw: false
    }

    fileSelection {
      type: FileInput
      y: 0 below fileLabel

      width: Intrinsic
      height: Intrinsic

      padding : [2,2,0]
      showing : "%(param.isFileInput)"

      filePath: "%(param.value)"
      twoWayBinding : true
    }

    choiceLabel {
      y: Centered

      width: Intrinsic
      height: Intrinsic

      showing : "%(param.isChoiceInput)"
      text: "%(param.name): "
      background.draw: false
    }

    choiceValue {
      type: Dropdown
      x: 5 right of label
      padding : [2,2,0]

      showing : "%(param.isChoiceInput)"
      dropdownItems : "%(param.choices)"
      selectedItem: "%(param.value)"
      itemTextFunction: "%(param.itemToText)"
    }
  }
}