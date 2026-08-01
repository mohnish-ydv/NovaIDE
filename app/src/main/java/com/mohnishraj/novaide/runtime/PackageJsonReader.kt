package com.mohnishraj.novaide.runtime

/** Bounded parser focused on package.json strings, objects and arrays. */
object PackageJsonReader {
    private const val MAX_INPUT = 512_000
    private const val MAX_DEPTH = 32
    private const val MAX_ITEMS = 8_000

    data class PackageInfo(
        val name: String?,
        val scripts: Map<String, String>,
        val dependencies: Set<String>,
        val devDependencies: Set<String>
    )

    fun parse(source: String): PackageInfo {
        require(source.length <= MAX_INPUT) { "package.json is larger than 512 KB" }
        val root = Parser(source).parseObject()
        fun stringMap(key: String): Map<String, String> = (root[key] as? Map<*, *>)
            .orEmpty().mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }.toMap()
        return PackageInfo(
            name = root["name"] as? String,
            scripts = stringMap("scripts").toSortedMap(),
            dependencies = stringMap("dependencies").keys,
            devDependencies = stringMap("devDependencies").keys
        )
    }

    private class Parser(private val source: String) {
        private var index = 0
        private var items = 0

        fun parseObject(): Map<String, Any?> {
            val value = value(0)
            whitespace()
            require(index == source.length) { "Unexpected content after package.json" }
            @Suppress("UNCHECKED_CAST")
            return value as? Map<String, Any?> ?: error("package.json root must be an object")
        }

        private fun value(depth: Int): Any? {
            require(depth <= MAX_DEPTH) { "package.json nesting is too deep" }
            whitespace(); require(index < source.length) { "Unexpected end of package.json" }
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
            index++; whitespace()
            val result = linkedMapOf<String, Any?>()
            if (take('}')) return result
            while (true) {
                item(); whitespace(); require(peek('"')) { "Object key must be a string" }
                val key = string(); whitespace(); expect(':')
                require(key !in result) { "Duplicate package.json key: $key" }
                result[key] = value(depth)
                whitespace()
                if (take('}')) return result
                expect(',')
            }
        }

        private fun array(depth: Int): List<Any?> {
            index++; whitespace()
            val result = mutableListOf<Any?>()
            if (take(']')) return result
            while (true) {
                item(); result += value(depth); whitespace()
                if (take(']')) return result
                expect(',')
            }
        }

        private fun string(): String {
            expect('"'); val out = StringBuilder()
            while (index < source.length) {
                when (val c = source[index++]) {
                    '"' -> return out.toString()
                    '\\' -> {
                        require(index < source.length) { "Invalid JSON escape" }
                        when (val e = source[index++]) {
                            '"', '\\', '/' -> out.append(e)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000c')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                require(index + 4 <= source.length) { "Invalid unicode escape" }
                                out.append(source.substring(index, index + 4).toInt(16).toChar()); index += 4
                            }
                            else -> error("Unsupported JSON escape: \\$e")
                        }
                    }
                    else -> { require(c.code >= 32) { "Control character in JSON string" }; out.append(c) }
                }
                require(out.length <= MAX_INPUT) { "package.json string is too large" }
            }
            error("Unterminated JSON string")
        }

        private fun number(): Number {
            val start = index
            if (peek('-')) index++
            while (index < source.length && source[index].isDigit()) index++
            var decimal = false
            if (peek('.')) { decimal = true; index++; while (index < source.length && source[index].isDigit()) index++ }
            if (peek('e') || peek('E')) {
                decimal = true; index++; if (peek('+') || peek('-')) index++
                while (index < source.length && source[index].isDigit()) index++
            }
            val raw = source.substring(start, index)
            return if (decimal) raw.toDouble() else raw.toLong()
        }

        private fun <T> literal(raw: String, result: T): T {
            require(source.regionMatches(index, raw, 0, raw.length)) { "Invalid JSON literal" }
            index += raw.length; return result
        }

        private fun item() { items++; require(items <= MAX_ITEMS) { "package.json has too many items" } }
        private fun whitespace() { while (index < source.length && source[index].isWhitespace()) index++ }
        private fun peek(char: Char): Boolean = index < source.length && source[index] == char
        private fun take(char: Char): Boolean = if (peek(char)) { index++; true } else false
        private fun expect(char: Char) { whitespace(); require(take(char)) { "Expected '$char' at $index" } }
    }
}
