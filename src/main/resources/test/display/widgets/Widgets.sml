ChildA {
  x : 50
  y : 50
  width : 200
  height : 150
  background {
    color : [180,180,180,255]
  }

  children {
    Nested {
      type: ImageDisplay
      x : 15
      y : 25
      width : intrinsic
      height : intrinsic
      image : "display/images/small_tree.png"
    }

    Dropdown {
      type : Dropdown
      x : 5 right of Nested
      y : 25

      width : intrinsic
      height : intrinsic

      dropdownItems : "%(dropdownState.items)"
      selectedItem : %(dropdownState.selection)
    }
  }
}

ListThing {
  x: 300
  y: 50
  width: 200
  height: WrapContent
  gapSize: -2

  listItemArchetype: Widgets.ListChild
  listItemBinding: "listItems -> item"
}

ListChild {
  x: centered
  width: intrinsic
  height: intrinsic
  padding: [6, 6, 0]
  text: "%(item.text)"

  background {
    image: "ui/singlePixelBorder.png"

  }
}



ChildB {
  x : 150
  y: 300
  width : intrinsic
  height : intrinsic
  background {
    color : [200, 200, 200, 255]
  }
  padding : [4,4,0]
  text : "%(test)"
}