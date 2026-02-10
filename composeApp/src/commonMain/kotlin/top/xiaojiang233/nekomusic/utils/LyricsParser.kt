package top.xiaojiang233.nekomusic.utils

/**
 * A simple regex-based lyrics parser.
 * Handles standard LRC format [mm:ss.xx] Lyric
 */
object LyricsParser {
    data class LyricWord(
        val text: String,
        val startTime: Long,
        val duration: Long
    )

    data class LyricLine(
        val time: Long,
        val text: String,
        val words: List<LyricWord>? = null,
        val translation: String? = null
    )

    private val TIME_REGEX = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]")
    private val WORD_REGEX = Regex("\\((\\d+),(\\d+),(\\d+)\\)([^\\(]*)")

    fun parse(lrc: String): List<LyricLine> {
        val lines = lrc.split("\n")
        val result = mutableListOf<LyricLine>()

        for (line in lines) {
            val trimLine = line.trim()
            if (trimLine.isEmpty()) continue

            // Find all timestamps in the line (sometimes lines have multiple like [00:12.34][00:24.56]Text)
            val matcher = TIME_REGEX.findAll(trimLine)
            val text = trimLine.replace(TIME_REGEX, "").trim()

            if (text.isEmpty()) continue

            for (match in matcher) {
                val min = match.groupValues[1].toLongOrNull() ?: 0
                val sec = match.groupValues[2].toLongOrNull() ?: 0
                val msStr = match.groupValues[3]
                // 2 digits = 10ms, 3 digits = 1ms. Usually .xx is 10ms.
                val ms = if (msStr.length == 2) msStr.toLongOrNull()?.times(10) ?: 0
                         else msStr.toLongOrNull() ?: 0

                val totalMs = min * 60 * 1000 + sec * 1000 + ms
                result.add(LyricLine(totalMs, text))
            }
        }

        return result.sortedBy { it.time }
    }

    fun parseYrc(yrc: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        // YRC format: {"t":... metadata or [time,duration](wordTime,wordDuration,0)Word
        // We focus on lines starting with [
        val lines = yrc.split("\n")

        for (line in lines) {
            val trimLine = line.trim()
            if (!trimLine.startsWith("[")) continue // Skip metadata JSON lines for now

            // Example: [16210,3460](16210,670,0)还(16880,410,0)没
            // Extract line start time
            val bracketEndIndex = trimLine.indexOf("]")
            if (bracketEndIndex == -1) continue

            val lineTimeStr = trimLine.substring(1, bracketEndIndex)
            val lineTimeParts = lineTimeStr.split(",")
            val lineStartTime = lineTimeParts.getOrNull(0)?.toLongOrNull() ?: 0L
            // val lineDuration = lineTimeParts.getOrNull(1)?.toLongOrNull() ?: 0L // Not used yet

            val content = trimLine.substring(bracketEndIndex + 1)

            val words = mutableListOf<LyricWord>()
            val fullTextBuilder = StringBuilder()

            val matches = WORD_REGEX.findAll(content)
            for (match in matches) {
                val wordStart = match.groupValues[1].toLongOrNull() ?: 0L
                val wordDuration = match.groupValues[2].toLongOrNull() ?: 0L
                // group 3 is '0', ignored
                val text = match.groupValues[4]

                words.add(LyricWord(text, wordStart, wordDuration))
                fullTextBuilder.append(text)
            }

            if (words.isNotEmpty()) {
                result.add(LyricLine(lineStartTime, fullTextBuilder.toString(), words))
            }
        }

        return result.sortedBy { it.time }
    }
}
