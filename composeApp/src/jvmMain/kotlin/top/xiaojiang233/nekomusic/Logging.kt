package top.xiaojiang233.nekomusic

import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

fun initAppLogging() {
    val logDirProp = System.getProperty("nekomusic.log.dir")
    val defaultDir = System.getProperty("user.home") + File.separator + ".nekomusic" + File.separator + "logs"
    val logDir = File(logDirProp ?: defaultDir)
    try {
        if (!logDir.exists()) logDir.mkdirs()
    } catch (_: Exception) {
        // ignore
    }

    // Redirect stdout/stderr
    try {
        val outFile = File(logDir, "app.out.log")
        val errFile = File(logDir, "app.err.log")
        val outStream = FileOutputStream(outFile, true)
        val errStream = FileOutputStream(errFile, true)
        System.setOut(PrintStream(outStream, true, StandardCharsets.UTF_8.name()))
        System.setErr(PrintStream(errStream, true, StandardCharsets.UTF_8.name()))
    } catch (_: Exception) {
        // If redirect fails, continue without throwing
    }

    // Configure java.util.logging to write to a file
    try {
        val logFile = File(logDir, "app.log").absolutePath
        val handler = FileHandler(logFile, 10 * 1024 * 1024, 5, true)
        handler.formatter = SimpleFormatter()
        val root = Logger.getLogger("")
        // remove default handlers? keep them, but set level
        root.addHandler(handler)
        root.level = Level.INFO
    } catch (_: Exception) {
        // ignore
    }

    // Uncaught exception handler
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            val logger = Logger.getLogger("Uncaught")
            logger.log(Level.SEVERE, "Uncaught exception in thread ${thread.name}", throwable)
            System.err.println("Uncaught exception in thread ${thread.name}: ${throwable.stackTraceToString()}")
        } catch (_: Exception) {
            // ignore
        }
    }
}
