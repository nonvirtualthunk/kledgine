package arx.core



class SugaredRegexMatch(val raw : List<MatchResult>) {

    operator fun component1() : String {
        return raw.getOrNull(0)?.groupValues?.getOrNull(1) ?:  ""
    }
    operator fun component2() : String {
        return raw.getOrNull(0)?.groupValues?.getOrNull(2) ?:  ""
    }
    operator fun component3() : String {
        return raw.getOrNull(0)?.groupValues?.getOrNull(3) ?:  ""
    }
    operator fun component4() : String {
        return raw.getOrNull(0)?.groupValues?.getOrNull(4) ?:  ""
    }

    fun all() : List<String> {
        return raw.map { it.groupValues.getOrElse(1) { "" } }
    }
}

fun Regex.match(str : String) : SugaredRegexMatch? {
    val raw = this.findAll(str).toList()
    if (raw.isEmpty()) { return null }

    return SugaredRegexMatch(raw.toList())
}