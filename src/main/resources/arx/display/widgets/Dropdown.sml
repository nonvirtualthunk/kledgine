

DropdownList {
  type : ListWidget

  x: 0
  y: 0
  width: WrapContent
  height: WrapContent
  border.width: 0

  listItemArchetype: Dropdown.DropdownItem
  listItemBinding: "items -> item"
//  separatorArchetype : AsciiCardWidgets.TextOptionDivider
  gapSize: 2
  selectable: true
  ignoreBounds: true
}

DropdownItem {
  type : TextDisplay

  background.draw : false

  text : "%(item)"
}