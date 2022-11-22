package hfcom.game

import arx.core.Noto
import arx.engine.Entity
import arx.engine.GameWorld
import arx.engine.prettyString
import java.util.concurrent.ThreadLocalRandom


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
    val attStatsCon = effectiveCombatStatsContributions(attacker)
    val defStatsCon = effectiveCombatStatsContributions(defender)

    val attStats = attStatsCon.merge()
    val defStats = defStatsCon.merge()

    val range = distanceBetween(attacker, defender)?.toInt()
    if (range == null || range > attack.range) {
        return null
    }

    val accuracy = if (range > 1) {
        attStats.precision + attack.accuracy
    } else {
        attStats.accuracy + attack.accuracy
    }

    val defence = defStats.defence + 5

    val netAccuracy = accuracy - defence

    val fractionToHit = summedDNRProbabilities.getOrElse(-netAccuracy) { 0.0f }

    return AttackSummary(
        attack = attack,
        hitFraction = fractionToHit,
        minDamage = attack.damage,
        maxDamage = attack.damage,
        range = range,
        attackerStatsContributions = attStatsCon,
        defenderStatsContributions = defStatsCon
    )
}



data class AttackExplanation(
    val hitChance : List<Pair<Int, String>>,
    val damage : List<Pair<Int, String>>
)

internal fun pullFromStats(stats: List<CombatStatsContribution>, fn : (CombatStats) -> Int, negate : Boolean) : List<Pair<Int, String>> {
    val ret = mutableListOf<Pair<Int,String>>()
    val mul = if (negate) { -1 } else { 1 }
    for (s in stats) {
        val v = fn(s.stats) * mul
        if (v != 0) {
            ret.add(v to s.from)
        }
    }
    return ret
}
fun explain(summary : AttackSummary) : AttackExplanation {
    val hitChanceModifiers = mutableListOf<Pair<Int,String>>()
    val damageModifiers = mutableListOf<Pair<Int,String>>()

    hitChanceModifiers.add(summary.attack.accuracy to "base attack accuracy")

    if (summary.range > 1) {
        hitChanceModifiers.addAll(pullFromStats(summary.attackerStatsContributions, { s -> s.precision }, negate = false))
    } else {
        hitChanceModifiers.addAll(pullFromStats(summary.attackerStatsContributions, { s -> s.accuracy }, negate = false))
    }

    hitChanceModifiers.addAll(pullFromStats(summary.defenderStatsContributions, { s -> s.defence }, negate = true))

    damageModifiers.add(summary.attack.damage to "base attack damage")

    return AttackExplanation(hitChance = hitChanceModifiers, damage = damageModifiers)
}


fun GameWorld.takeDamage(ent : Entity, damage : Int) {
    val cd = ent[CharacterData] ?: return

    val hpLostBefore = cd.hpLost
    cd.hpLost += damage
    Noto.info("Character ${prettyString(ent)} attacked, dealt $damage damage, new hp ${effectiveCombatStats(ent).maxHP - cd.hpLost}")
    fireEvent(DamageTaken(ent, hpLostBefore, cd.hpLost))

    if (cd.hpLost >= effectiveCombatStats(ent).maxHP) {
        cd.dead = true
        val pd = ent[Physical] ?: return Noto.errAndReturn("target of attack died, but was not a physical entity ${prettyString(ent)}", Unit)
        val tm = global(TacticalMap) ?: return Noto.errAndReturn("somehow there's no tactical map ${prettyString(ent)}", Unit)
        val tile = tm.tiles[pd.position.xy]
        tile.removeEntity(this, ent)
        fireEvent(CharacterDied(ent))
    }
}

fun GameWorld.attack(ent : Entity, target : Entity, attack: Attack) : Boolean {

    val summary = attackSummary(ent, target, attack) ?: return Noto.errAndReturn("could not create attack summary ${prettyString(ent)}, $attack", false)
    fireEvent(CharacterAttacked(ent, target))
    if (ThreadLocalRandom.current().nextFloat() <= summary.hitFraction) {
        fireEvent(AttackHit(attacker = ent, target = target, attack = attack))
        takeDamage(target, attack.damage)
    } else {
        fireEvent(AttackMiss(attacker = ent, target = target, attack = attack))
    }


    return true
}