package com.mohnishraj.novaide.productivity

data class NovaTask(val id: String, val name: String, val description: String, val commands: List<String>)
data class TaskRunResult(val task: NovaTask, val output: String, val success: Boolean, val completedCommands: Int)

object TaskRunner {
    private const val MAX_COMMANDS = 20

    fun validate(task: NovaTask): NovaTask {
        require(task.id.matches(Regex("[a-z0-9][a-z0-9._-]{2,63}"))) { "Task id is invalid" }
        require(task.name.isNotBlank() && task.name.length <= 70) { "Task name is invalid" }
        require(task.commands.isNotEmpty()) { "Task needs at least one command" }
        require(task.commands.size <= MAX_COMMANDS) { "Task has too many commands" }
        task.commands.forEach { command ->
            require(command.length <= 4_000 && !command.contains('\n')) { "Each task command must be one bounded line" }
            val executable = NovaConsoleEngine.tokenize(command).firstOrNull()?.lowercase() ?: error("Task command is blank")
            require(executable in NovaConsoleEngine.supportedCommands()) { "Unsupported safe command: $executable" }
            require(executable != "clear") { "clear is not valid inside a task" }
        }
        return task
    }

    fun run(task: NovaTask, context: ConsoleContext): TaskRunResult {
        validate(task)
        val output = StringBuilder()
        var completed = 0
        for ((index, command) in task.commands.withIndex()) {
            val result = NovaConsoleEngine.execute(command, context)
            output.append("$ ").append(command).append('\n').append(result.output).append('\n')
            if (!result.success) return TaskRunResult(task, output.toString().take(NovaConsoleEngine.MAX_OUTPUT_CHARS), false, completed)
            completed = index + 1
            if (output.length >= NovaConsoleEngine.MAX_OUTPUT_CHARS) break
        }
        return TaskRunResult(task, output.toString().take(NovaConsoleEngine.MAX_OUTPUT_CHARS), true, completed)
    }

    fun builtIns(): List<NovaTask> = listOf(
        NovaTask("nova.project.summary", "Project summary", "Show indexed project size and file types.", listOf("project-info")),
        NovaTask("nova.todo.scan", "TODO/FIXME scan", "Find unfinished work markers across indexed text files.", listOf("grep -i TODO", "grep -i FIXME")),
        NovaTask("nova.source.map", "Source map", "List common source files.", listOf("find .kt", "find .java", "find .js", "find .py")),
        NovaTask("nova.readme.preview", "README preview", "Display the first 80 README lines.", listOf("head README.md 80"))
    ).map(::validate)
}
