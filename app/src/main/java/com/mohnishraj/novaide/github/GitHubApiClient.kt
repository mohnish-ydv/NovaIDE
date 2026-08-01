package com.mohnishraj.novaide.github

import android.util.Base64
import com.mohnishraj.novaide.files.FileRepository
import com.mohnishraj.novaide.git.GitBranch
import com.mohnishraj.novaide.git.GitChange
import com.mohnishraj.novaide.git.GitChangeKind
import com.mohnishraj.novaide.git.GitCommit
import com.mohnishraj.novaide.git.GitCommitResult
import com.mohnishraj.novaide.git.GitCommitMode
import com.mohnishraj.novaide.git.GitCommitPlanner
import com.mohnishraj.novaide.git.GitHubRepoInfo
import com.mohnishraj.novaide.git.GitHubRepository
import com.mohnishraj.novaide.git.WorkflowArtifact
import com.mohnishraj.novaide.git.WorkflowRun
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class GitHubApiClient(token: String?) {
    private val token: String? = token?.trim()?.takeIf { it.isNotBlank() }

    companion object {
        private const val API = "https://api.github.com"
        private const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
        private const val MAX_COMMIT_FILES = 200
        private const val MAX_COMMIT_FILE_BYTES = 25L * 1024L * 1024L
        private const val MAX_COMMIT_TOTAL_BYTES = 50L * 1024L * 1024L
        private const val MAX_ARCHIVE_DOWNLOAD_BYTES = 300L * 1024L * 1024L
        private const val MAX_ARTIFACT_DOWNLOAD_BYTES = 1024L * 1024L * 1024L
    }

    class GitHubException(val statusCode: Int, message: String) : IOException(message)
    data class AuthenticatedUser(val login: String, val name: String?)

    fun authenticatedUser(): AuthenticatedUser? {
        if (token.isNullOrBlank()) return null
        val json = requestJson("GET", "/user")
        return AuthenticatedUser(json.getString("login"), json.optString("name", "").ifBlank { null })
    }

    /** Resolves the actual branch while explicitly supporting repositories with no commits yet. */
    fun resolveBranch(repo: GitHubRepository, requestedBranch: String?): Pair<GitHubRepoInfo, GitHubRepository> {
        val baseInfo = repositoryInfo(repo)
        val requested = requestedBranch?.trim().orEmpty()
        val targetBranch = requested.ifBlank { baseInfo.defaultBranch.ifBlank { "main" } }
        val hasCommits = repositoryHasCommits(repo)
        val info = baseInfo.copy(isEmpty = !hasCommits)
        if (!hasCommits) return info to repo.copy(branch = targetBranch)

        val candidates = linkedSetOf<String>()
        if (requested.isNotBlank()) candidates += requested else candidates += targetBranch
        var last: Exception? = null
        for (branch in candidates) {
            val candidate = repo.copy(branch = branch)
            try {
                branchHead(candidate)
                return info to candidate
            } catch (error: GitHubException) {
                if (error.statusCode != 404) throw error
                last = error
            }
        }
        throw IOException(
            if (requested.isBlank()) "GitHub did not return a usable default branch for ${repo.slug}."
            else "Branch '$requested' was not found. Repository default branch is '${baseInfo.defaultBranch}'.",
            last
        )
    }

    fun repositoryInfo(repo: GitHubRepository): GitHubRepoInfo {
        val json = requestJson("GET", "/repos/${repo.owner}/${repo.name}")
        val permissions = json.optJSONObject("permissions")
        return GitHubRepoInfo(
            fullName = json.optString("full_name", repo.slug),
            defaultBranch = json.optString("default_branch", "main"),
            isPrivate = json.optBoolean("private", false),
            canPush = permissions?.optBoolean("push", false) ?: false,
            webUrl = json.optString("html_url", repo.webUrl),
            isEmpty = false
        )
    }

    fun repositoryHasCommits(repo: GitHubRepository): Boolean = try {
        val array = requestArray("GET", "/repos/${repo.owner}/${repo.name}/commits?per_page=1")
        array.length() > 0
    } catch (error: GitHubException) {
        if (error.statusCode == 409) false else throw error
    }

    fun branchHead(repo: GitHubRepository): String {
        val json = requestJson("GET", "/repos/${repo.owner}/${repo.name}/git/ref/heads/${encodePath(repo.branch)}")
        return json.getJSONObject("object").getString("sha")
    }

    fun branchHeadOrNull(repo: GitHubRepository): String? = try {
        branchHead(repo)
    } catch (error: GitHubException) {
        if (error.statusCode == 404 || error.statusCode == 409) null else throw error
    }

    fun branches(repo: GitHubRepository): List<GitBranch> {
        val array = requestArray("GET", "/repos/${repo.owner}/${repo.name}/branches?per_page=100")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(GitBranch(
                    name = item.getString("name"),
                    sha = item.getJSONObject("commit").getString("sha"),
                    protected = item.optBoolean("protected", false)
                ))
            }
        }
    }

    fun commits(repo: GitHubRepository, limit: Int = 30): List<GitCommit> {
        if (!repositoryHasCommits(repo)) return emptyList()
        val array = requestArray("GET", "/repos/${repo.owner}/${repo.name}/commits?sha=${encodeQuery(repo.branch)}&per_page=${limit.coerceIn(1, 100)}")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val commit = item.getJSONObject("commit")
                val author = commit.optJSONObject("author")
                add(GitCommit(
                    sha = item.getString("sha"),
                    message = commit.optString("message", "Commit").lineSequence().firstOrNull().orEmpty(),
                    author = author?.optString("name", "Unknown") ?: "Unknown",
                    date = author?.optString("date", "") ?: "",
                    webUrl = item.optString("html_url", "")
                ))
            }
        }
    }

    fun workflowRuns(repo: GitHubRepository, limit: Int = 25): List<WorkflowRun> {
        val json = requestJson("GET", "/repos/${repo.owner}/${repo.name}/actions/runs?branch=${encodeQuery(repo.branch)}&per_page=${limit.coerceIn(1, 100)}")
        val array = json.optJSONArray("workflow_runs") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(WorkflowRun(
                    id = item.getLong("id"),
                    name = item.optString("name", "Workflow"),
                    status = item.optString("status", "unknown"),
                    conclusion = if (item.isNull("conclusion")) null else item.optString("conclusion", "").ifBlank { null },
                    branch = item.optString("head_branch", repo.branch),
                    event = item.optString("event", ""),
                    createdAt = item.optString("created_at", ""),
                    webUrl = item.optString("html_url", "")
                ))
            }
        }
    }

    fun artifacts(repo: GitHubRepository, runId: Long): List<WorkflowArtifact> {
        val json = requestJson("GET", "/repos/${repo.owner}/${repo.name}/actions/runs/$runId/artifacts?per_page=100")
        val array = json.optJSONArray("artifacts") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(WorkflowArtifact(
                    id = item.getLong("id"),
                    name = item.optString("name", "artifact"),
                    sizeBytes = item.optLong("size_in_bytes", 0L),
                    expired = item.optBoolean("expired", false),
                    downloadUrl = item.optString("archive_download_url", "")
                ))
            }
        }
    }

    fun createCommit(
        repo: GitHubRepository,
        message: String,
        changes: List<GitChange>,
        repository: FileRepository,
        expectedHead: String? = null
    ): GitCommitResult {
        requireToken()
        require(message.isNotBlank()) { "Commit message is required" }
        require(changes.isNotEmpty()) { "There are no local changes" }
        if (changes.size > MAX_COMMIT_FILES) throw IOException("Commit is limited to $MAX_COMMIT_FILES changed files")
        val total = changes.filter { it.kind != GitChangeKind.DELETED }.sumOf { it.size.coerceAtLeast(0L) }
        if (total > MAX_COMMIT_TOTAL_BYTES) throw IOException("Commit exceeds the 50 MB mobile safety limit")

        val info = repositoryInfo(repo)
        if (!info.canPush) {
            throw IOException("GitHub connected in read-only mode. This token or account lacks repository write access. Grant Contents read/write for this repository and ensure you have push permission.")
        }

        val head = branchHeadOrNull(repo)
        val plan = GitCommitPlanner.plan(head, expectedHead, changes)

        val baseTree = plan.parent?.let { sha ->
            requestJson("GET", "/repos/${repo.owner}/${repo.name}/git/commits/$sha")
                .getJSONObject("tree").getString("sha")
        }
        val treeEntries = createTreeEntries(repo, changes, repository)
        val treeBody = JSONObject().apply {
            if (baseTree != null) put("base_tree", baseTree)
            put("tree", treeEntries)
        }
        val tree = requestJson("POST", "/repos/${repo.owner}/${repo.name}/git/trees", treeBody)
        val commitBody = JSONObject().apply {
            put("message", message.trim())
            put("tree", tree.getString("sha"))
            if (plan.parent != null) put("parents", JSONArray().put(plan.parent))
        }
        val commit = requestJson("POST", "/repos/${repo.owner}/${repo.name}/git/commits", commitBody)
        val sha = commit.getString("sha")
        if (plan.mode == GitCommitMode.INITIAL) {
            requestJson("POST", "/repos/${repo.owner}/${repo.name}/git/refs", JSONObject().apply {
                put("ref", "refs/heads/${repo.branch}")
                put("sha", sha)
            })
        } else {
            requestJson("PATCH", "/repos/${repo.owner}/${repo.name}/git/refs/heads/${encodePath(repo.branch)}", JSONObject().apply {
                put("sha", sha)
                put("force", false)
            })
        }
        return GitCommitResult(sha, commit.optString("html_url", "${repo.webUrl}/commit/$sha"))
    }

    private fun createTreeEntries(repo: GitHubRepository, changes: List<GitChange>, repository: FileRepository): JSONArray {
        val treeEntries = JSONArray()
        for (change in changes) {
            if (Thread.currentThread().isInterrupted) throw IOException("Commit cancelled")
            val item = JSONObject().apply {
                put("path", change.path)
                put("mode", "100644")
                put("type", "blob")
            }
            if (change.kind == GitChangeKind.DELETED) {
                item.put("sha", JSONObject.NULL)
            } else {
                if (change.size > MAX_COMMIT_FILE_BYTES) throw IOException("${change.path} exceeds the 25 MB file limit")
                val uri = change.uri ?: throw IOException("Could not access ${change.path}")
                val bytes = repository.openInput(uri)?.use { it.readBytesLimited(MAX_COMMIT_FILE_BYTES) }
                    ?: throw IOException("Could not read ${change.path}")
                val blob = requestJson("POST", "/repos/${repo.owner}/${repo.name}/git/blobs", JSONObject().apply {
                    put("content", Base64.encodeToString(bytes, Base64.NO_WRAP))
                    put("encoding", "base64")
                })
                item.put("sha", blob.getString("sha"))
            }
            treeEntries.put(item)
        }
        return treeEntries
    }

    fun downloadArchive(repo: GitHubRepository, destination: File): String {
        val head = branchHeadOrNull(repo)
            ?: throw IOException("This GitHub repository is empty. Create an initial commit from NovaIDE instead of pulling.")
        download(
            "$API/repos/${repo.owner}/${repo.name}/zipball/${encodePath(repo.branch)}",
            destination.outputStream(),
            MAX_ARCHIVE_DOWNLOAD_BYTES
        )
        return head
    }

    fun downloadArtifact(artifact: WorkflowArtifact, destination: OutputStream) {
        requireToken()
        if (artifact.expired) throw IOException("This artifact has expired")
        val url = artifact.downloadUrl.ifBlank { throw IOException("Artifact download URL is missing") }
        if (artifact.sizeBytes > MAX_ARTIFACT_DOWNLOAD_BYTES) throw IOException("Artifact exceeds the 1 GB mobile download limit")
        download(url, destination, MAX_ARTIFACT_DOWNLOAD_BYTES)
    }

    private fun requestArray(method: String, path: String, body: JSONObject? = null): JSONArray {
        val response = request(method, "$API$path", body)
        return runCatching { JSONArray(response) }.getOrElse { throw IOException("GitHub returned an invalid list") }
    }

    private fun requestJson(method: String, path: String, body: JSONObject? = null): JSONObject {
        val response = request(method, "$API$path", body)
        return runCatching { JSONObject(response) }.getOrElse { throw IOException("GitHub returned invalid JSON") }
    }

    private fun request(method: String, url: String, body: JSONObject?): String {
        val connection = try {
            open(URI(url).toURL(), method)
        } catch (error: Exception) {
            throw friendlyNetworkError(error)
        }
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { input -> String(input.readBytesLimited(MAX_RESPONSE_BYTES.toLong()), Charsets.UTF_8) }.orEmpty()
            if (status !in 200..299) {
                val apiMessage = runCatching { JSONObject(text).optString("message") }.getOrNull()
                val accepted = connection.getHeaderField("X-Accepted-GitHub-Permissions").orEmpty()
                val oauthScopes = connection.getHeaderField("X-OAuth-Scopes").orEmpty()
                val permissionHint = buildString {
                    if (accepted.isNotBlank()) append(" Required token permissions: $accepted.")
                    if (oauthScopes.isNotBlank()) append(" Current classic-token scopes: $oauthScopes.")
                }
                val friendly = when (status) {
                    401 -> "GitHub token is invalid or expired. Create a new token and try again."
                    403 -> (apiMessage?.takeIf { it.isNotBlank() } ?: "GitHub denied this request.") + permissionHint.ifBlank { " Check repository access, token permissions and API rate limits." }
                    404 -> if (accepted.isNotBlank()) {
                        "GitHub hid this resource because the token lacks permission.$permissionHint"
                    } else "Repository, branch or resource was not found, or the saved token cannot access this private repository."
                    409 -> apiMessage ?: "GitHub rejected the operation because the repository state changed or the repository has no commits."
                    422 -> (apiMessage ?: "GitHub rejected the supplied data.") + permissionHint
                    else -> apiMessage ?: "GitHub request failed with HTTP $status"
                }
                throw GitHubException(status, friendly)
            }
            return text
        } catch (error: GitHubException) {
            throw error
        } catch (error: Exception) {
            throw friendlyNetworkError(error)
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyNetworkError(error: Exception): IOException = when (error) {
        is UnknownHostException -> IOException("Could not resolve api.github.com. Check internet access, Private DNS, VPN, or firewall settings.", error)
        is SocketTimeoutException -> IOException("GitHub connection timed out. Check the network and retry.", error)
        is SSLException -> IOException("Secure TLS connection to GitHub failed. Check device date/time, certificates, VPN, or HTTPS inspection.", error)
        is IOException -> error
        else -> IOException(error.message ?: "GitHub network request failed", error)
    }

    private fun download(initialUrl: String, destination: OutputStream, maxBytes: Long) {
        var url = URI(initialUrl).toURL()
        var redirects = 0
        destination.use { output ->
            while (true) {
                val connection = open(url, "GET")
                connection.instanceFollowRedirects = false
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location") ?: throw IOException("GitHub download redirect was invalid")
                    connection.disconnect()
                    if (++redirects > 5) throw IOException("Too many download redirects")
                    url = URI(location).toURL()
                    continue
                }
                if (status !in 200..299) {
                    val message = connection.errorStream?.bufferedReader()?.use { it.readText().take(1000) }.orEmpty()
                    connection.disconnect()
                    throw GitHubException(status, message.ifBlank { "GitHub download failed with HTTP $status" })
                }
                val declaredLength = connection.contentLengthLong
                if (declaredLength > maxBytes) {
                    connection.disconnect()
                    throw IOException("GitHub download exceeds the mobile safety limit")
                }
                connection.inputStream.use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) throw IOException("GitHub download exceeds the mobile safety limit")
                        output.write(buffer, 0, read)
                    }
                }
                connection.disconnect()
                return
            }
        }
    }

    private fun open(url: URL, method: String): HttpURLConnection =
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 45_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "NovaIDE-Android-M6")
            if (url.host.equals("api.github.com", ignoreCase = true)) {
                token?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
        }

    private fun requireToken() {
        if (token.isNullOrBlank()) throw IOException("A GitHub token is required for this operation. Open Credentials Center and add one with the required repository permission.")
    }

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun encodePath(value: String): String = value.split('/').joinToString("/") { segment ->
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
    }

    private fun java.io.InputStream.readBytesLimited(maxBytes: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("GitHub data exceeded the mobile safety limit")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
