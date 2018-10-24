import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.zeroturnaround.exec.ProcessExecutor
import java.io.File
import kotlin.coroutines.CoroutineContext


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

suspend fun searchForOption(text: String, producer: ReceiveChannel<String>) {
    println("enter searchForOption")
    for (line in producer) {
        val words = line.splitToWords()
        val found = words.any { s -> s.contains(text) }
        if (found && line.isNotBlank()) println(line)
    }
}

fun executeBashCommand(context: CoroutineContext, command: String, vararg args: String) =
    CoroutineScope(context).produce<String>(context, 42) {

        val allArgs = arrayOf(command, *args)
        Buffer().use { buffer ->
            ProcessExecutor()
                .directory(File("/Windows/System32"))
                .command(*allArgs)
                .redirectOutput(buffer.outputStream())
                .setMessageLogger { _, _, _ -> }
                .execute()
            while (!buffer.exhausted()) {
                val line = buffer.readUtf8Line() ?: break
                println("read line: $line")
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