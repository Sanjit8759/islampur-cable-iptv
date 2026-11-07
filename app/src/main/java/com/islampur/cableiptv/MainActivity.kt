
private fun parsePlaylist(raw: String): MutableList<Channel> {

    val result = mutableListOf<Channel>()

    var name = ""
    var group = "Other"

    val cleanRaw = raw.removePrefix("\uFEFF")

    cleanRaw.lineSequence().forEach { line ->

        val item = line.trim()

        if (item.isBlank()) return@forEach

        if (item.startsWith("#EXTINF", true)) {

            name = item.substringAfter(",", "").trim()

            val match = Regex(
                """group-title\s*=\s*["']([^"']*)["']""",
                RegexOption.IGNORE_CASE
            ).find(item)

            group = match?.groupValues?.get(1)
                ?.trim()
                ?.ifBlank { "Other" }
                ?: "Other"

        } else if (
            item.startsWith("http://", true) ||
            item.startsWith("https://", true)
        ) {

            result.add(
                Channel(
                    name = name.ifBlank {
                        "Channel ${result.size + 1}"
                    },
                    url = item,
                    group = group
                )
            )

            name = ""
            group = "Other"
        }
    }

    Log.d(
        "ISLAMPUR_IPTV",
        "Parsed channels=${result.size}, RADIO=${result.count { it.group.equals("RADIO", true) }}"
    )

    return result
}
