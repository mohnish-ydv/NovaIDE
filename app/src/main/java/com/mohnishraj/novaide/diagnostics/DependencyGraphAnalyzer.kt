package com.mohnishraj.novaide.diagnostics

import java.util.ArrayDeque

object DependencyGraphAnalyzer {
    private const val MAX_EDGES = 8_000

    fun analyze(files: List<DiagnosticFile>): DependencyGraphReport {
        val sources = files.filter { it.text != null && isSource(it.path) }
        val nodes = sources.map { it.path }.toSet()
        val packageMap = mutableMapOf<String, String>()
        val simpleNameMap = mutableMapOf<String, MutableList<String>>()
        sources.forEach { file ->
            val source = file.text.orEmpty()
            val packageName = Regex("""(?m)^\s*package\s+([\w.]+)""").find(source)?.groupValues?.get(1)
            val simple = file.path.substringAfterLast('/').substringBeforeLast('.')
            simpleNameMap.getOrPut(simple) { mutableListOf() } += file.path
            if (packageName != null) packageMap["$packageName.$simple"] = file.path
        }
        val edges = mutableListOf<DependencyEdge>()
        sources.forEach { file ->
            if (edges.size >= MAX_EDGES) return@forEach
            parseImports(file.path, file.text.orEmpty()).forEach { (targetName, kind) ->
                val target = resolveTarget(targetName, packageMap, simpleNameMap, nodes)
                if (target != null && target != file.path && edges.none { it.from == file.path && it.to == target && it.kind == kind }) {
                    edges += DependencyEdge(file.path, target, kind)
                }
            }
        }
        val inbound = edges.groupingBy { it.to }.eachCount()
        val outbound = edges.groupingBy { it.from }.eachCount()
        val hubs = nodes.map { it to ((inbound[it] ?: 0) + (outbound[it] ?: 0)) }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(20)
        val orphans = nodes.filter { (inbound[it] ?: 0) == 0 && (outbound[it] ?: 0) == 0 }
            .filterNot { path -> path.substringAfterLast('/').substringBeforeLast('.').lowercase() in setOf("main", "mainactivity", "app", "index") }
            .sorted().take(100)
        return DependencyGraphReport(nodes, edges, findCycles(nodes, edges), hubs, orphans, edges.size >= MAX_EDGES)
    }

    fun render(report: DependencyGraphReport): String = buildString {
        append("DEPENDENCY MAP\nNodes: ${report.nodes.size} • Edges: ${report.edges.size} • Cycles: ${report.cycles.size}")
        if (report.truncated) append(" • edge limit reached")
        append("\n\nHUBS\n")
        report.hubs.forEach { (path, degree) -> append("$degree  $path\n") }
        append("\nCYCLES\n")
        if (report.cycles.isEmpty()) append("No project dependency cycles detected.\n")
        else report.cycles.forEachIndexed { i, cycle -> append("${i + 1}. ${cycle.joinToString(" → ")}\n") }
        append("\nORPHAN SOURCE FILES\n")
        if (report.orphanSources.isEmpty()) append("None detected.\n") else report.orphanSources.forEach { append("• $it\n") }
        append("\nEDGES\n")
        report.edges.take(500).forEach { append("${it.from} --${it.kind}--> ${it.to}\n") }
        if (report.edges.size > 500) append("… ${report.edges.size - 500} more edges omitted from this view\n")
    }

    private fun parseImports(path: String, source: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        Regex("""(?m)^\s*import\s+(?:static\s+)?([\w.]+)""").findAll(source)
            .forEach { result += it.groupValues[1].removeSuffix(".*") to "import" }
        Regex("""(?m)(?:import\s+[^'\"]+\s+from\s+|require\s*\(\s*|import\s*\(\s*)['\"]([^'\"]+)['\"]""")
            .findAll(source).forEach { result += resolveRelative(path, it.groupValues[1]) to "module" }
        Regex("""(?m)^\s*from\s+([\w.]+)\s+import\s+""").findAll(source)
            .forEach { result += it.groupValues[1] to "python" }
        return result.distinct()
    }

    private fun resolveRelative(from: String, target: String): String {
        if (!target.startsWith('.')) return target
        val base = from.substringBeforeLast('/', "").split('/').filter { it.isNotBlank() }.toMutableList()
        var remaining = target
        while (remaining.startsWith("../")) { if (base.isNotEmpty()) base.removeAt(base.lastIndex); remaining = remaining.removePrefix("../") }
        remaining = remaining.removePrefix("./")
        return (base + remaining).joinToString("/")
    }

    private fun resolveTarget(name: String, packages: Map<String, String>, simpleNames: Map<String, List<String>>, nodes: Set<String>): String? {
        packages[name]?.let { return it }
        val normalized = name.replace('.', '/').removeSuffix(".kt").removeSuffix(".java").removeSuffix(".js").removeSuffix(".ts")
        nodes.firstOrNull { it.substringBeforeLast('.').endsWith(normalized) }?.let { return it }
        val simple = name.substringAfterLast('.').substringAfterLast('/').substringBeforeLast('.')
        return simpleNames[simple]?.singleOrNull()
    }

    private fun findCycles(nodes: Set<String>, edges: List<DependencyEdge>): List<List<String>> {
        val adjacency = edges.groupBy { it.from }.mapValues { (_, values) -> values.map { it.to }.distinct() }
        var index = 0
        val stack = ArrayDeque<String>()
        val onStack = mutableSetOf<String>()
        val indices = mutableMapOf<String, Int>()
        val low = mutableMapOf<String, Int>()
        val components = mutableListOf<List<String>>()
        fun strongConnect(node: String) {
            indices[node] = index; low[node] = index; index++; stack.addLast(node); onStack += node
            adjacency[node].orEmpty().forEach { next ->
                if (next !in indices) { strongConnect(next); low[node] = minOf(low[node]!!, low[next]!!) }
                else if (next in onStack) low[node] = minOf(low[node]!!, indices[next]!!)
            }
            if (low[node] == indices[node]) {
                val component = mutableListOf<String>()
                while (stack.isNotEmpty()) {
                    val value = stack.removeLast(); onStack -= value; component += value
                    if (value == node) break
                }
                if (component.size > 1 || adjacency[node].orEmpty().contains(node)) components += component.reversed()
            }
        }
        nodes.forEach { if (it !in indices && components.size < 50) strongConnect(it) }
        return components
    }

    private fun isSource(path: String): Boolean = path.substringAfterLast('.', "").lowercase() in
        setOf("kt", "kts", "java", "js", "mjs", "cjs", "ts", "tsx", "jsx", "py", "go", "rs")
}
