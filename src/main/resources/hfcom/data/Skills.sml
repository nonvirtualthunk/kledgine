Skills {
  Aim {
    name : "Aim"

    ap : 5

    effects : [{
      stats {
        precision: 5
        duration: Aimed
      }
    },{
      flags {
        Aimed : true
      }
    }]

    requirements {
      Aimed : false
    }
  }

  VolleyFire {
    name : "Volley Fire"

    ap : 3 // for an attack, the ap is added to the base ap cost of the attack

    effects : [{
      attack {
        times : 2
        accuracy : -1
        damage : -1
      }
    }]
  }
}