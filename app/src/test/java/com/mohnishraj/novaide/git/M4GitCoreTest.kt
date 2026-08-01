package com.mohnishraj.novaide.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.mohnishraj.novaide.github.GitHubTokenNormalizer

class M4GitCoreTest {
    @Test
    fun parsesCommonGitHubRemoteFormats() {
        assertEquals("owner/repo", GitUrlParser.parse("https://github.com/owner/repo.git")?.slug)
        assertEquals("owner/repo", GitUrlParser.parse("git@github.com:owner/repo.git")?.slug)
        assertEquals("feature/ui", GitUrlParser.parse("owner/repo", "feature/ui")?.branch)
        assertEquals("release/mobile", GitUrlParser.parse("https://www.github.com/owner/repo/tree/release/mobile")?.branch)
        assertEquals("release/mobile", GitUrlParser.branchFromTreeUrl("https://github.com/owner/repo/tree/release/mobile"))
        assertEquals("owner/repo", GitUrlParser.parse("github.com/owner/repo")?.slug)
        assertNull(GitUrlParser.parse("https://gitlab.com/owner/repo"))
        assertNull(GitUrlParser.parse("owner/repo/extra"))
    }

    @Test
    fun parsesRemoteConfigAndRejectsUnsafeRefs() {
        val config = """
            [remote "origin"]
                url = git@github.com:mohnish-ydv/NovaIDE.git
                fetch = +refs/heads/*:refs/remotes/origin/*
        """.trimIndent()
        assertEquals("mohnish-ydv/NovaIDE", GitUrlParser.parseRemoteConfig(config)?.slug)
        assertTrue(GitUrlParser.isValidRef("feature/mobile-ui"))
        assertFalse(GitUrlParser.isValidRef("../main"))
        assertFalse(GitUrlParser.isValidRef("feature lock"))
        assertFalse(GitUrlParser.isValidRef("bad.lock"))
        assertFalse(GitUrlParser.isValidRef("@"))
    }

    @Test
    fun normalizesCopiedTokenPrefixesAndRejectsWhitespace() {
        val token = "test_token_" + "x".repeat(32)
        assertEquals(token, GitHubTokenNormalizer.normalize("  Bearer $token  "))
        assertEquals(token, GitHubTokenNormalizer.normalize("Authorization: token $token"))
        assertTrue(runCatching { GitHubTokenNormalizer.normalize("github pat invalid token") }.isFailure)
    }

    @Test
    fun conflictParserFindsAndResolvesBlocks() {
        val source = """
            before
            <<<<<<< HEAD
            current
            =======
            incoming
            >>>>>>> origin/main
            after
        """.trimIndent() + "\n"
        val block = ConflictParser.find(source).single()
        assertEquals("HEAD", block.oursLabel)
        assertEquals("origin/main", block.theirsLabel)
        assertTrue(ConflictParser.resolve(source, 0, ConflictResolution.OURS).contains("current"))
        assertFalse(ConflictParser.resolve(source, 0, ConflictResolution.OURS).contains("incoming"))
        val both = ConflictParser.resolve(source, 0, ConflictResolution.BOTH)
        assertTrue(both.contains("current"))
        assertTrue(both.contains("incoming"))
        assertTrue(ConflictParser.find(both).isEmpty())
    }

    @Test
    fun unifiedDiffReportsAdditionsAndDeletions() {
        val result = UnifiedDiff.create("one\ntwo\nthree", "one\n2\nthree\nfour")
        assertEquals(2, result.additions)
        assertEquals(1, result.deletions)
        assertTrue(result.text.contains("-two"))
        assertTrue(result.text.contains("+2"))
        assertTrue(result.text.contains("+four"))
    }

    @Test
    fun unchangedDiffIsExplicitlyClean() {
        val result = UnifiedDiff.create("same", "same")
        assertEquals("No differences.", result.text)
        assertEquals(0, result.additions)
        assertEquals(0, result.deletions)
        assertFalse(result.truncated)
        assertNotNull(result)
    }
}
