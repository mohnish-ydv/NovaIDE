package com.mohnishraj.novaide.plugins

/** Small bounded JSON reader used for declarative plugin manifests. */
internal object MiniJson {
    private const val MAX_INPUT = 128_000
    private const val MAX_DEPTH = 24
    private const val MAX_ITEMS = 1_000

    fun parseObject(source: String): Map<String, Any?> {
        require(source.length <= MAX_INPUT) { "JSON is larger than 128 KB" }
        val parser = Parser(source)
        val value = parser.value(0)
        parser.skipWhitespace()
        require(parser.atEnd()) { "Unexpected content after JSON document" }
        @Suppress("UNCHECKED_CAST")
        return value as? Map<String, Any?> ?: error("JSON root must be an object")
    }

    private class Parser(private val source: String) {
        private var index = 0
        private var itemCount = 0

        fun atEnd(): Boolean = index >= source.length
        fun skipWhitespace() { while (!atEnd() && source[index].isWhitespace()) index++ }

        fun value(depth: Int): Any? {
            require(depth <= MAX_DEPTH) { "JSON nesting is too deep" }
            skipWhitespace()
            require(!atEnd()) { "Unexpected end of JSON" }
            return when (source[index]) {
                '{' -> obj(depth + 1)
                '[' -> array(depth + 1)
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                '-', in '0'..'9' -> number()
                else -> error("Unexpected JSON token at $index")
            }
        }

        private fun obj(depth: Int): Map<String, Any?> {
            index++
            skipWhitespace()
            val result = linkedMapOf<String, Any?>()
            if (peek('}')) { index++; return result }
            while (true) {
                countItem()
                skipWhitespace()
                require(peek('"')) { "Object keys must be strings" }
                val key = string()
                skipWhitespace(); expect(':')
                require(key !in result) { "Duplicate JSON key: $key" }
                result[key] = value(depth)
                skipWhitespace()
                when {
                    peek(',') -> index++
                    peek('}') -> { index++; return result }
                    else -> error("Expected ',' or '}' at $index")
                }
            }
        }

        private fun array(depth: Int): List<Any?> {
            index++
            skipWhitespace()
            val result = mutableListOf<Any?>()
            if (peek(']')) { index++; return result }
            while (true) {
                countItem()
                result += value(depth)
                skipWhitespace()
                when {
                    peek(',') -> index++
                    peek(']') -> { index++; return result }
                    else -> error("Expected ',' or ']' at $index")
                }
            }
        }

        private fun string(): String {
            expect('"')
            val out = StringBuilder()
            while (!atEnd()) {
                val c = source[index++]
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        require(!atEnd()) { "Invalid JSON escape" }
                        when (val e = source[index++]) {
                            '"', '\\', '/' -> out.append(e)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                require(index + 4 <= source.length) { "Invalid unicode escape" }
                                val hex = source.substring(index, index + 4)
                                out.append(hex.toInt(16).toChar())
                                index += 4
                            }
                            else -> error("Unsupported JSON escape: \\$e")
                        }
                    }
                    else -> {
                        require(c.code >= 0x20) { "Control character in JSON string" }
                        out.append(c)
                    }
                }
                require(out.length <= MAX_INPUT) { "JSON string is too large" }
            }
            error("Unterminated JSON string")
        }

        private fun number(): Number {
            val start = index
            if (peek('-')) index++
            while (!atEnd() && source[index].isDigit()) index++
            var decimal = false
            if (!atEnd() && source[index] == '.') {
                decimal = true; index++
                while (!atEnd() && source[index].isDigit()) index++
            }
            if (!atEnd() && (source[index] == 'e' || source[index] == 'E')) {
                decimal = true; index++
                if (!atEnd() && (source[index] == '+' || source[index] == '-')) index++
                while (!atEnd() && source[index].isDigit()) index++
            }
            val raw = source.substring(start, index)
            return if (decimal) raw.toDouble() else raw.toLong()
        }

        private fun <T> literal(text: String, value: T): T {
            require(source.regionMatches(index, text, 0, text.length)) { "Invalid JSON literal" }
            index += text.length
            return value
        }

        private fun countItem() {
            itemCount++
            require(itemCount <= MAX_ITEMS) { "JSON contains too many items" }
        }

        private fun peek(c: Char): Boolean = !atEnd() && source[index] == c
        private fun expect(c: Char) {
            skipWhitespace()
            require(peek(c)) { "Expected '$c' at $index" }
            index++
        }
    }
}
