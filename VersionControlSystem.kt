package svcs

import java.io.File
import java.security.MessageDigest
import java.util.*

const val LENGTH_COMMAND_IN_MENU = 11

fun main(args: Array<String>) {
    val dirName = File("vcs")
    val commitsDirName = File("vcs/commits")
    val configFile = File("vcs/config.txt")
    val indexFile = File("vcs/index.txt")
    val logFile = File("vcs/log.txt")
    val newAddedFiles = File("vcs/newAdded.txt")
    dirName.mkdir()
    commitsDirName.mkdir()
    configFile.createNewFile()
    indexFile.createNewFile()
    logFile.createNewFile()
    newAddedFiles.createNewFile()

    val help = mapOf(
        "config" to "Get and set a username.",
        "add" to "Add a file to the index.",
        "log" to "Show commit logs.",
        "commit" to "Save changes.",
        "checkout" to "Restore a file."
    )

    val mapOfFiles = mutableMapOf("" to "")
    mapOfFiles.remove("")
    val newCommitDirectories: String

    indexFile.forEachLine {
        val (a, b) = it.split(" ")
        mapOfFiles[a] = b
    }

    when {
        args.isEmpty() || args[0] == "--help" -> {
            println("These are SVCS commands:")
            for (i in help) {
                println("${i.key}${" ".repeat(LENGTH_COMMAND_IN_MENU - i.key.length)}${i.value}")
            }
        }
        args[0] == "config" -> {
            when {
                configFile.readText().isEmpty() && args.size == 1 -> {
                    println("Please, tell me who you are.")
                }
                args.size > 1 -> {
                    configFile.writeText(args[1])
                    println("The username is ${configFile.readText()}.")
                }
                args[0] == "config" -> {
                    println("The username is ${configFile.readText()}.")
                }

            }
        }
        args[0] == "add" -> {
            when {
                args.size == 1 && indexFile.readText().isEmpty() -> {
                    println("Add a file to the index.")
                }
                args.size == 1 -> {
                    println("Tracked files:")
                    indexFile.forEachLine { println(it.substring(0, it.indexOf(" "))) }
                }
                File(args[1]).exists() -> {
                    println("The file '${args[1]}' is tracked.")
                    indexFile.appendText("${args[1]} ${hashString("SHA-256", File(args[1]).readText())}\n")
                    newAddedFiles.writeText(args[1])
                }
                !File(args[1]).exists() -> {
                    println("Can't find '${args[1]}'.")
                }
            }
        }
        args[0] == "commit" -> {
            when {
                args.size <= 1 -> println("Message was not passed.")

                args[1] == "" -> println("Nothing to commit.")

                else -> {

                    if (newAddedFiles.readText() != "") {
                        newCommitDirectories = hashString("SHA-256", File(newAddedFiles.readText()).readText())

                        val bufferLogFile = logFile.readText()
                        logFile.writeText(
                            "commit $newCommitDirectories\n" +
                                    "Author: ${configFile.readText()}\n" +
                                    "${args[1]}\n\n" +
                                    bufferLogFile
                        )

                        copyFilesInCommitDirectory(mapOfFiles, newCommitDirectories, indexFile)
                        println("Changes are committed.")
                        newAddedFiles.writeText("")

                    } else {
                        newCommitDirectories = areFilesChange(mapOfFiles)

                        if (newCommitDirectories.isNotEmpty()) {

                            val bufferLogFile = logFile.readText()
                            logFile.writeText(
                                "commit $newCommitDirectories\n" +
                                        "Author: ${configFile.readText()}\n" +
                                        "${args[1]}\n\n" +
                                        bufferLogFile
                            )

                            copyFilesInCommitDirectory(mapOfFiles, newCommitDirectories, indexFile)
                            println("Changes are committed.")

                        } else println("Nothing to commit.")
                    }
                }
            }
        }

        args[0] == "log" -> {
            if (logFile.readText().isEmpty()) println("No commits yet.") else logFile.forEachLine { println(it) }
        }

        args[0] == "checkout" -> {
            if (args.size == 1) println("Commit id was not passed.")
            else if (!File("vcs/commits/${args[1]}").exists()) println("Commit does not exist.")
            else {
                newCommitDirectories = args[1]
                copyFilesFromCommitDirectory(mapOfFiles, newCommitDirectories, indexFile)
            }
        }

        help.containsKey(args[0]) -> println(help[args[0]])

        else -> print("'${args[0]}' is not a SVCS command.")
    }
}

fun areFilesChange (mapOfFiles: MutableMap<String, String>): String {
    var changedFileHash = ""
    for ((key, value) in mapOfFiles) {
        val hashOfFile = hashString("SHA-256", File(key).readText())
        if (hashOfFile != value) changedFileHash = hashOfFile
    }
    return changedFileHash
}

fun copyFilesFromCommitDirectory (mapOfFiles: MutableMap<String, String>, newCommitDirectories: String, indexFile: File) {
    indexFile.writeText("")
    for ((key, value) in mapOfFiles) {
        File(key).writeText(File("vcs/commits/$newCommitDirectories/$key").readText())
        mapOfFiles[key] = newCommitDirectories
        indexFile.appendText("$key $value\n")
    }
    println("Switched to commit $newCommitDirectories.")
}

fun copyFilesInCommitDirectory (mapOfFiles: MutableMap<String, String>, newCommitDirectories: String, indexFile: File) {
    File("vcs/commits/$newCommitDirectories").mkdir()
    indexFile.writeText("")
    for ((key, value) in mapOfFiles) {
        val newCopyFile = File("vcs/commits/$newCommitDirectories/$key")
        newCopyFile.createNewFile()
        newCopyFile.writeText(File(key).readText())
        mapOfFiles[key] = hashString("SHA-256", newCopyFile.readText())
        indexFile.appendText("$key $value\n")a
    }
}

fun hashString(type: String, input: String): String {
    val bytes = MessageDigest
        .getInstance(type)
        .digest(input.toByteArray())
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
