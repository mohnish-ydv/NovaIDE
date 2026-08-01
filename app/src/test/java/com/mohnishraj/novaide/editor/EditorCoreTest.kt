package com.mohnishraj.novaide.editor

import com.mohnishraj.novaide.editor.navigation.SymbolExtractor
import com.mohnishraj.novaide.editor.search.SearchEngine
import com.mohnishraj.novaide.editor.search.SearchOptions
import com.mohnishraj.novaide.editor.syntax.CodeLanguage
import com.mohnishraj.novaide.editor.syntax.SyntaxKind
import com.mohnishraj.novaide.editor.syntax.SyntaxTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorCoreTest {
    @Test
    fun literalAndWholeWordSearchBehaveDifferently() {
        assertEquals(3, SearchEngine.findAll("one ONE stone", "one", SearchOptions()).matches.size)
        assertEquals(
            1,
            SearchEngine.findAll("one stone", "one", SearchOptions(wholeWord = true)).matches.size
        )
    }

    @Test
    fun regexErrorsAreReportedWithoutCrashing() {
        assertEquals(
            2,
            SearchEngine.findAll("a1 a2", "a\\d", SearchOptions(regex = true)).matches.size
        )
        assertNotNull(SearchEngine.findAll("abc", "[", SearchOptions(regex = true)).error)
    }

    @Test
    fun replaceAllReturnsAccurateCount() {
        val result = SearchEngine.replaceAll("foo foo", "foo", "bar", SearchOptions())
        assertEquals("bar bar", result.text)
        assertEquals(2, result.replacementCount)
    }

    @Test
    fun regexReplacementSupportsContextAndCaptureGroups() {
        val contextual = SearchEngine.replaceAll(
            "xy xz",
            "(?<=x)y",
            "Y",
            SearchOptions(regex = true)
        )
        assertEquals("xY xz", contextual.text)
        assertEquals(1, contextual.replacementCount)

        val captured = SearchEngine.replaceAll(
            "first-last",
            "(first)-(last)",
            "${'$'}2, ${'$'}1",
            SearchOptions(regex = true)
        )
        assertEquals("last, first", captured.text)
    }

    @Test
    fun replaceAllNeverPartiallyMutatesPastSafetyLimit() {
        val source = "x ".repeat(SearchEngine.MAX_MATCHES + 1)
        val result = SearchEngine.replaceAll(source, "x", "y", SearchOptions())
        assertEquals(source, result.text)
        assertEquals(0, result.replacementCount)
        assertNotNull(result.error)
    }

    @Test
    fun kotlinSymbolsAndSyntaxAreDetected() {
        val source = """
            class Demo {
                fun greet(name: String) = "Hello ${'$'}name" // comment
            }
        """.trimIndent()
        val symbols = SymbolExtractor.extract("Demo.kt", source)
        assertTrue(symbols.any { it.name == "Demo" && it.kind == "class" })
        assertTrue(symbols.any { it.name == "greet" && it.kind == "function" })

        val tokens = SyntaxTokenizer.tokenize(source, CodeLanguage.KOTLIN).tokens
        assertTrue(tokens.any { it.kind == SyntaxKind.KEYWORD })
        assertTrue(tokens.any { it.kind == SyntaxKind.STRING })
        assertTrue(tokens.any { it.kind == SyntaxKind.COMMENT })
    }

    @Test
    fun syntaxTokenizerCapsVeryLargeInput() {
        val source = "val item = 1\n".repeat(40_000)
        val result = SyntaxTokenizer.tokenize(source, CodeLanguage.KOTLIN)
        assertTrue(result.truncated)
        assertTrue(result.tokens.all { it.endExclusive <= SyntaxTokenizer.MAX_HIGHLIGHT_CHARS })
    }
}
