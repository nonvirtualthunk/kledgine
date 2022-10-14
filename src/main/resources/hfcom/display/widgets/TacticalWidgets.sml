

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
  x : centered
  y : 0 from bottom
  width : WrapContent
  height : WrapContent
  showing : "%(?selectedCharacter)"

  children {
    ActionDisplay {
      width: 200
      text : "%(actions.activeAction.name)"
      padding : [4,4,0]
    }
  }
}