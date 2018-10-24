import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.zeroturnaround.exec.ProcessExecutor
import java.io.File
import kotlin.coroutines.CoroutineContext


fun main(args: Array<String>) = runBlocking<Unit> {
    /*
        if (args.size < 1) {
            println("Usage: $ main.kt COMMAND")
            System.exit(1)
            return@runBlocking
        }
        val (command, option) = args
        println("Parsing $ man $command $option")
    */
/*
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) {
            println("SEND")
            channel.send(x * x)
        }
        channel.close() // we're done sending
    }
    // here we print received values using `for` loop (until the channel is closed)
    for (y in channel) println(y)
    println("Done!")
*/

    launch(newFixedThreadPoolContext(1, "meinPool")) {
        val producer = executeBashCommand(coroutineContext, "ping.exe", "localhost")
        searchForOption("Antwort", producer)
    }
}

suspend fun searchForOption(text: String, producer: ReceiveChannel<String>) {
    println("enter searchForOption")
    val tid = Thread.currentThread().id

    for (line in producer) {
        findWord(line, text, tid)
    }

/*
    while (!producer.isClosedForReceive) {
        val line = producer.receive()
        findWord(line, text, tid)
    }
*/
}

private fun findWord(line: String, text: String, tid: Long) {
    val words = line.splitToWords()
    val found = words.any { s -> s.contains(text) }
    if (found && line.isNotBlank()) println("Thread $tid FOUND for $line")
}

fun executeBashCommand(context: CoroutineContext, command: String, vararg args: String) =
    CoroutineScope(context).produce<String>(context, 1) {
        val tid = Thread.currentThread().id

        val allArgs = arrayOf(command, *args)
        Buffer().use { buffer ->
            ProcessExecutor()
                .directory(File("/Windows/System32"))
                .command(*allArgs)
                .redirectOutput(buffer.outputStream())
                .setMessageLogger { _, _, _ -> }
                .execute()

/*
            while (true) {
//                delay(4000)
                val instant = Date().toInstant()
                val millis = instant.toEpochMilli()
                val nano = System.nanoTime()
                val line = "Antwort... $millis $nano"
                println("Thread $tid SEND: $line")
                channel.send(line)
//                yield()
            }
*/

            while (!buffer.exhausted()) {
                val line = buffer.readUtf8Line() ?: break
                println("Thread $tid SEND: $line")
                channel.send(line)
//                yield()
            }

            channel.close()
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