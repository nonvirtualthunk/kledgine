Items {


  Longsword {
    combatStats {
      encumbrance: 1
      speed: -1
      defence: 1
    }

    slots : [ItemSlots.Hand]

    attacks : [{
      name : "Slash"
      accuracy : 1
      range : 1
      ammunition : 0
      damage : 4
      ap : 5
    }]
  }

  Bow {
    combatStats {
      encumbrance: 1
      speed: -1
      defence: -3
    }

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
    combatStats {
      encumbrance: 0
      speed: 0
      defence: 1
    }

    slots : [ItemSlots.Hand]

    attacks : [{
      name : "Stab"
      accuracy : 1
      range : 1
      ammunition : 0
      damage : 2
      ap : 4
    }]
  }

  RoundShield {
    combatStats {
      encumbrance: 1
      speed: -1
      defence: 1
      protection: 1
    }

    slots : [ItemSlots.Hand]
  }

  ChainArmor {
    combatStats {
      encumbrance: 2
      speed: -1
      defence: 0
      protection: 1
    }
  }

}