package org.masamotod.idea.Drupal8NamespaceDetector

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

internal class Drupal8ModuleFileVisitor(private val myAction: ModuleConsumer) : FileVisitor<Path> {
    internal fun interface ModuleConsumer {
        fun accept(drupalModuleName: String?, drupalModulePath: Path?)
    }

    @Throws(IOException::class)
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (dir.fileName.toString().startsWith(".")) {
            return FileVisitResult.SKIP_SUBTREE
        }
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        val filename = file.fileName.toString()
        if (filename.endsWith(INFO_YAML_SUFFIX)) {
            val moduleName = filename.substring(0, filename.length - INFO_YAML_SUFFIX.length)
            val moduleDir = file.parent

            myAction.accept(moduleName, moduleDir)
        }

        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun postVisitDirectory(dir: Path, exc: IOException): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    companion object {
        private const val INFO_YAML_SUFFIX = ".info.yml"
    }
}
