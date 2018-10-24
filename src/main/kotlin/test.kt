import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking
import okio.Buffer
import org.zeroturnaround.exec.ProcessExecutor
import java.io.File
import kotlin.coroutines.experimental.CoroutineContext


fun main(args: Array<String>) = runBlocking {
/*
    if (args.size < 1) {
        println("Usage: $ main.kt COMMAND")
        System.exit(1)
        return@runBlocking
    }
    val (command, option) = args
    println("Parsing $ man $command $option")
*/

    val producer = executeBashCommand(coroutineContext, "ping.exe", "localhost")

    searchForOption("Antwort", producer)

}

suspend fun searchForOption(option: String, producer: ReceiveChannel<String>) {
    var foundBefore = false
    for (line in producer) {
        val words = line.splitToWords()
        val tries = listOf(words.getOrNull(0), words.getOrNull(1))
        val foundNow = tries.any { it?.startsWith(option) == true }
        if (foundNow && line.isNotBlank()) println(line)
/*
        val hasArgument = tries.any { it?.startsWith("-") == true }
        if (foundBefore && hasArgument) break
        foundBefore = foundBefore or foundNow
        if (foundBefore && line.isNotBlank()) println(line)
*/

    }
}

fun executeBashCommand(context: CoroutineContext, command: String, vararg args: String) =
    produce<String>(context, Channel.UNLIMITED) {

        val allArgs = arrayOf(command, *args)
        Buffer().use { buffer ->
            ProcessExecutor()
                .directory(File("/Windows/System32"))
                .command(*allArgs)
                .redirectOutput(buffer.outputStream())
                .setMessageLogger { _, _, _ -> }
                .execute()
            while (!buffer.exhausted()) {
//                val line = buffer.readUtf8Line() ?: break
                val line = buffer.readUtf8LineStrict() ?: break
                channel.send(line)
            }
        }
    }


private fun String.splitToWords(): List<String> {
    var line = this.trim()
    line = line.filterIndexed { i, c ->
        if (i == 0) return@filterIndexed true
        c != '\b' && line[i - 1] != '\b'
    }
    val words = line.split(' ', '\t', ',')
    return words.filter { it.isNotBlank() }
}