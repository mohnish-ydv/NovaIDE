package com.mohnishraj.novaide.m6

import com.mohnishraj.novaide.ai.AiResponseParser
import com.mohnishraj.novaide.ai.SecretRedactor
import com.mohnishraj.novaide.git.GitChange
import com.mohnishraj.novaide.git.GitChangeKind
import com.mohnishraj.novaide.git.GitCommitMode
import com.mohnishraj.novaide.git.GitCommitPlanner
import com.mohnishraj.novaide.gitlab.GitLabTokenNormalizer
import com.mohnishraj.novaide.localintel.AutocompleteEngine
import com.mohnishraj.novaide.localintel.LintSeverity
import com.mohnishraj.novaide.localintel.LocalLintEngine
import com.mohnishraj.novaide.localintel.RegexFixEngine
import com.mohnishraj.novaide.localintel.SnippetCatalog
import com.mohnishraj.novaide.localintel.StaticAnalysisEngine
import com.mohnishraj.novaide.localintel.StaticFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M6AiLocalCoreTest {
    @Test fun gitLabTokenPrefixesAreNormalized() {
        assertEquals("glpat-1234567890abcdef", GitLabTokenNormalizer.normalize("PRIVATE-TOKEN: glpat-1234567890abcdef"))
        assertTrue(runCatching { GitLabTokenNormalizer.normalize("bad token") }.isFailure)
    }

    @Test fun secretsAreRedactedAndSensitiveFilesExcluded() {
        val result = SecretRedactor.redact("api_key='super-secret-value'\nval x = 1")
        assertTrue(result.redactions > 0)
        assertFalse(result.text.contains("super-secret-value"))
        assertTrue(SecretRedactor.isSensitivePath("app/local.properties"))
        assertTrue(SecretRedactor.isSensitivePath("keys/release.jks"))
        assertFalse(SecretRedactor.isSensitivePath("src/Main.kt"))
    }

    @Test fun aiPatchParserRejectsTraversalAndDuplicates() {
        val valid = AiResponseParser.filePatches(":::nova-file path=\"src/Test.kt\"\nclass Test\n:::end")
        assertEquals("src/Test.kt", valid.single().path)
        assertTrue(runCatching { AiResponseParser.filePatches(":::nova-file path=\"../secret\"\nx\n:::end") }.isFailure)
        assertTrue(runCatching { AiResponseParser.filePatches(":::nova-file path=\"a.txt\"\nx\n:::end\n:::nova-file path=\"A.txt\"\ny\n:::end") }.isFailure)
    }

    @Test fun autocompleteUsesPrefixAndSymbols() {
        val source = "fun calculateTotal() = 1\ncal"
        val items = AutocompleteEngine.suggest("Demo.kt", source, source.length)
        assertTrue(items.any { it.label == "calculateTotal" && it.replaceStart == source.length - 3 })
    }

    @Test fun snippetsExpandWithStableCursor() {
        val snippet = SnippetCatalog.forFile("Demo.kt").first { it.id == "kt.fun" }
        val expanded = SnippetCatalog.expand(snippet, "    ")
        assertTrue(expanded.text.contains("fun name()"))
        assertFalse(expanded.text.contains("__NOVA_CURSOR__"))
        assertTrue(expanded.cursorOffset in 0..expanded.text.length)
    }

    @Test fun localLintAndRegexFixesRemainOfflineAndDeterministic() {
        val source = "import x.y\nimport x.y\nfun test() {  \n"
        val issues = LocalLintEngine.analyze("Demo.kt", source)
        assertTrue(issues.any { it.rule == "duplicate-import" })
        assertTrue(issues.any { it.severity == LintSeverity.ERROR && it.rule == "unclosed-delimiter" })
        val fixes = RegexFixEngine.proposals("Demo.kt", source)
        assertTrue(fixes.any { it.id == "trim-trailing" })
        assertTrue(fixes.any { it.id == "dedupe-imports" })
    }

    @Test fun staticAnalysisFindsConflictsAndSecretPathsWithoutEchoingValues() {
        val report = StaticAnalysisEngine.analyze(listOf(
            StaticFile(".env", 20, null),
            StaticFile("src/A.kt", 80, "<<<<<<< HEAD\na\n=======\nb\n>>>>>>> main\n")
        ))
        assertTrue(report.findings.any { it.title == "Sensitive file in workspace" })
        assertTrue(report.findings.any { it.title == "Unresolved merge conflict" })
    }

    @Test fun emptyRepositoryGetsInitialCommitPlan() {
        val added = GitChange("README.md", GitChangeKind.ADDED)
        assertEquals(GitCommitMode.INITIAL, GitCommitPlanner.plan(null, null, listOf(added)).mode)
        assertEquals(GitCommitMode.UPDATE, GitCommitPlanner.plan("abc", "abc", listOf(added)).mode)
        assertTrue(runCatching { GitCommitPlanner.plan("new", "old", listOf(added)) }.isFailure)
        assertTrue(runCatching { GitCommitPlanner.plan(null, null, listOf(GitChange("old.txt", GitChangeKind.DELETED))) }.isFailure)
        assertNotNull(added)
    }
}
