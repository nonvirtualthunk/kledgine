package hfcom.game

import arx.core.Noto
import arx.engine.Entity
import arx.engine.GameWorld


fun GameWorld.attackSummary(attacker : Entity, target : EffectTarget, attack : Attack) : AttackSummary? {
    return when (target) {
        is EffectTarget.Entity -> attackSummary(attacker, target.entity, attack)
        else -> {
            Noto.err("attackSummary(...) only implemented for entity targets at this time")
            null
        }
    }
}

fun GameWorld.attackSummary(attacker : Entity, defender : Entity, attack : Attack) : AttackSummary? {
    val attStats = effectiveCombatStats(attacker)
    val defStats = effectiveCombatStats(defender)

    val range = distanceBetween(attacker, defender)?.toInt()
    if (range == null || range > attack.range) {
        return null
    }

    val accuracy = if (range > 1) {
        attStats.precision + attack.accuracy
    } else {
        attStats.accuracy + attack.accuracy
    }

    val defence = defStats.defence

    val netAccuracy = accuracy - defence

    val fractionToHit = summedDNRProbabilities.getOrElse(-netAccuracy) { 0.0f }

    return AttackSummary(
        attack = attack,
        hitFraction = fractionToHit,
        minDamage = attack.damage,
        maxDamage = attack.damage,
        range = range
    )
}


