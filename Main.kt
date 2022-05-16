package svcs

import java.io.File
import java.security.MessageDigest

fun main(args: Array<String>) {

    val vcsDir = File("vcs")
    val commitsDir = File("vcs/commits")
    val configFile = File("vcs/config.txt")
    val indexFile = File("vcs/index.txt")
    val logFile = File("vcs/log.txt")
    if (!vcsDir.exists()) {
        vcsDir.mkdir()
    }
    if (!vcsDir.resolve("commits").exists()) {
        vcsDir.resolve("commits").mkdir()
    }
    if (!configFile.exists()) {
        vcsDir.resolve("config.txt").createNewFile()
    }
    if (!indexFile.exists()) {
        vcsDir.resolve("index.txt").createNewFile()
    }
    if (!logFile.exists()) {
        vcsDir.resolve("log.txt").createNewFile()
    }

    val configuration = mutableMapOf<String, String>()
    configFile.forEachLine { val a = it.split(" : "); configuration[a.first()] = a.last() }
    val indexation = mutableListOf<String>()
    indexFile.forEachLine { indexation.add(it) }

    fun logationic(): MutableList<String> {
        var n = 3
        var str = ""
        val log = mutableListOf<String>()
        logFile.forEachLine {
            if (it.isNotEmpty()) {
                str += "$it\n"
                n--
                if (n == 0) { log.add(str); n = 3; str = "" }
            }
        }
        return log
    }

    val logation = logationic()

    fun saveCommit(hash: String) {
        commitsDir.resolve(hash).mkdir()
        val hashDir = File("$commitsDir/$hash")
        for (i in indexation) {
            File(i).copyTo(File("$hashDir/$i"),true)
        }
    }

    fun changeConfig(item: String, value: String) {
            configuration[item] = value
            configFile.writeText("")
            var str = ""
            configuration.forEach { k, v -> str += "$k : $v\n" }
            configFile.writeText(str)
    }

    fun saveLog(hash: String, comm: String) {
        val str = "commit $hash\nAuthor: ${configuration["username"]}\n$comm"
        logation.add(str)
        logFile.writeText("")
        logFile.writeText(logation.joinToString("\n"))
    }

    fun changeIndexAdd(item: String) {
        indexation.add(item)
        indexFile.writeText("")
        indexFile.writeText(indexation.joinToString("\n"))
        /*val h = indexation.toHashIndexation("SHA-256")
        val ht = if (logation.isNotEmpty()) logation.last().substringBefore("\n").split(" ").last() else ""
        saveCommit(h)
        saveLog(h, "Index add '$item'")*/
    }

    fun checkyout(hash: String) {
        val hashDir = File("$commitsDir/$hash")
        for (i in indexation) {
            hashDir.resolve(i).copyTo(File(i),true)
        }
    }

    val help = "These are SVCS commands:\n" +
            "config     Get and set a username.\n" +
            "add        Add a file to the index.\n" +
            "log        Show commit logs.\n" +
            "commit     Save changes.\n" +
            "checkout   Restore a file."
    val config = { item: String, value: String ->
        if ((configuration[item] == null || configuration[item] == "") && value == "") {
            "Please, tell me who you are."
        } else {
            if (value != "" && configuration[item] != value) changeConfig(item, value)
            "The username is ${configuration[item]}."
        }
    }
    val add = { value: String ->
        if (value == "" && indexation.isEmpty()) "Add a file to the index." else {
            if (value == "") { "Tracked files:\n${indexation.joinToString("\n")}" } else {
                if (File(value).exists()) {
                    if (value !in indexation) changeIndexAdd(value)
                    "The file '$value' is tracked."
                } else "Can't find '$value'."
            }
        }
    }
    val log = {
        var str = ""
        if (logation.isEmpty()) str = "No commits yet." else {
            for (i in logation.lastIndex downTo 0) str += "${logation[i]}\n"
        }
        str
    }
    val commit = { value: String ->
        if (value.isEmpty() || indexation.isEmpty()) "Message was not passed." else {
            val h = indexation.toHashIndexation("SHA-256")
            val ht = if (logation.isNotEmpty()) logation.last().substringBefore("\n").split(" ").last() else ""
            if (h == ht) "Nothing to commit." else {
                saveCommit(h)
                saveLog(h, value)
                "Changes are committed."
            }
        }
    }
    val checkout = { value: String ->
        if (value.isEmpty()) "Commit id was not passed." else
            if (commitsDir.resolve(value).exists()) {
                checkyout(value)
                "Switched to commit $value."
            } else "Commit does not exist."
    }

    if (args.isEmpty()) println(help) else {
        for (i in 0..args.lastIndex step 2) {
            println (
                when (args[i]) {
                "--help", "" -> help
                "config" -> {
                    val value = if (i != args.lastIndex) args[i + 1] else ""
                    config("username", value)
                }
                "add" -> {
                    val value = if (i != args.lastIndex) args[i + 1] else ""
                    add(value)
                }
                "log" -> log()
                "commit" -> {
                    val value = if (i != args.lastIndex) args[i + 1] else ""
                    commit(value)
                }
                "checkout" -> {
                    val value = if (i != args.lastIndex) args[i + 1] else ""
                    checkout(value)
                }
                else -> "'${args[i]}' is not a SVCS command."
                }
            )
        }
    }
}

fun MutableList<String>.toHashIndexation(algorithm: String): String {
    var str = ""
    for (i in this) {
        str += File(i).readText()
    }
    return str.toHash(algorithm)
}

fun String.toHash(algorithm: String): String {
    return MessageDigest.getInstance(algorithm).digest(this.toByteArray())
        .joinToString("") { String.format("%02x", it) }
}
