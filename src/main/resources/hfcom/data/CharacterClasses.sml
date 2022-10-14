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


  Cultist {
    image : ${imgDir}/cultist_4.png

    levels : [
      [{
        name : "Cultist"
        strength : -1
        maxHP : 1
        equipment : [Dagger]
      }]
    ]
  }
}