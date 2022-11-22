

PreviewWidget {
  x : centered
  y : centered
  width : 100
  height : 50
  text : "Hello!"
}

AttackPreview {
  width : WrapContent
  height : WrapContent
  padding : [4,4,0]
  children {
    Text {
      width : intrinsic
      height : intrinsic
      background.draw : false
      text : "%(preview.attack.name) : %(preview.hitPercentDisplay)%   %(preview.damageRangeDisplay) damage"
    }
  }
}

StatsPreview {
  width : 100
  height : 50
  text : "Stats!"
}

ActionsWidget {
  type: Div
  x : 0
  y : 0 from bottom
  width : 50%
  height : WrapContent
  showing : "%(?selectedCharacter)"

  children {
    ActionDisplay {
      x : 0 from right
      y : centered
      width: 150
      horizontalAlignment: Centered
      text : "%(actions.activeAction.name)"
      padding : [4,4,0]
    }

    ActionButtons {
      width: WrapContent
      height: WrapContent
      horizontal: true
      background.draw: false
      padding : [2,2,0]

      selectable: true

      listItemArchetype: TacticalWidgets.ActionIcon
      listItemBinding: "possibleActions -> action"
    }
  }
}


ActionIcon {
  width: WrapContent
  height: WrapContent

  background {
    image : ui/minimalistBorderAllWhite.png
    centerColor : "%(action.backgroundColor)"
    edgeColor : "%(action.edgeColor)"
  }

  children {
    Icon {
      width: 32
      height: 32
      background.draw: false
      color: "%(action.iconColor)"

      image: "%(action.icon)"
    }
    Text {
      x : 0
      y : 0
      z : 1
      width : Intrinsic
      height : Intrinsic
      color : [255,255,255,255]
      background.draw: false

      text : "%(action.index)"
    }
  }
}


InvalidActionWidget {
  x : centered
  y : 20
  width: WrapContent
  height: WrapContent
  padding : [8,8,0]
  showing : "%(?invalidActionReason)"

  children {
    Text {
      text : "%(invalidActionReason)"
      background.draw : false
      fontSize: 32
      font: "fonts/ChevyRaySkullboy.ttf"
      color: [75,0,0,255]
    }
  }
}