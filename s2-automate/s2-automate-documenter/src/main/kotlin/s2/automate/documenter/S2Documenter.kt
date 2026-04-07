package s2.automate.documenter

import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.serialization.json.Json
import s2.dsl.automate.S2Automate

class S2Documenter(
    val outputFolder: String = getDefaultOutputDirectory(),
    val json: Json = Json { prettyPrint = true },
) {

    fun recreateFile(name: String, outputFolder: String): Path {
        Files.createDirectories(Paths.get(outputFolder))
        val filePath = Paths.get(outputFolder, name)
        Files.deleteIfExists(filePath)
        return Files.createFile(filePath)
    }
}

fun getDefaultOutputDirectory(): String {
    return (if (File("pom.xml").exists()) "target" else "build") + "/s2-documenter"
}


fun S2Documenter.writeS2Automate(s2: S2Automate): S2Documenter {
    val json = json.encodeToString(s2)
    val file: Path = recreateFile("${s2.name}.json", outputFolder )
    FileWriter(file.toFile()).use { writer -> writer.write(json) }
    return this
}
