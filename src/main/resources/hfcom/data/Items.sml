Items {


  Longsword {
    encumbrance : 1
    speed : -1
    defence : 1

    slots : [ItemSlots.Hand]

    attacks : [{
      name : "Slash"
      accuracy : 1
      range : 0
      ammunition : 0
      damage : 4
      ap : 5
    }]
  }

  Bow {
    encumbrance : 1
    speed : -1
    defence : -3

    slots : [ItemSlots.Hand, ItemSlots.Hand]

    attacks : [{
      name : "Shoot"
      accuracy : 1
      range : 6
      ammunition : 1
      damage : 3
      ap : 6
    }]
  }

  Dagger {
    encumbrance : 0
    speed : 0
    defence : 1

    slots : [ItemSlots.Hand]

    attacks : [{
      name : "Stab"
      accuracy : 1
      range : 0
      ammunition : 0
      damage : 2
      ap : 4
    }]
  }

}