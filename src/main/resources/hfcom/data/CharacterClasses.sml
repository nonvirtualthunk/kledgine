imgDir : "hfcom/display/characters"


CharacterClasses {
  Archer {
    image : ${imgDir}/archer_1.png

    levels : [
      // level 1
      [{
        name : "Archer"
        precision : 1
        maxHP : 2
        equipment : [Bow]
        visionRange : 10
      }],
      // level 2
      [{
        name : "Marksman"
        precision : 2
        speed : -1,
        maxHP : 1
        skills : [Aim]
      },{
        name : "Fast Shot"
        speed : 2
        precision : -1
        skills : [VolleyFire]
      }]
    ]
  }

  Warden {
    image : ${imgDir}/warden.png

    levels : [
      // level 1
      [{
        name : "Warden"
        strength : 1
        maxHP : 4
        defence : 1
        encumbrance : 1
        equipment : [Longsword, RoundShield, ChainArmor]
        visionRange : 8
      }],
      // level 2
      [{
        name : "Bodyguard"
        protection : 1
        maxHP : 2
        skills : [Bodyguard]
      },{
        name : "Avenger"
        maxHP : 1
        accuracy : 1

        combatStats {
          flags {
            Revenge : 1
          }
        }
      }]
    ]
  }


  Cultist {
    image : ${imgDir}/cultist_4.png

    levels : [
      [{
        name : "Cultist"
        strength : -1
        maxHP : 1
        equipment : [Dagger]
        visionRange : 8
      }]
    ]
  }
}