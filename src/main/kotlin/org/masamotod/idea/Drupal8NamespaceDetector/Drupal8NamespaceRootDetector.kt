package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.MessageFormat

internal class Drupal8NamespaceRootDetector(private val myProject: Project, private val myDrupalPath: Path) {
    private val myLogger = Logger.getInstance(javaClass)

    fun detect(): Result {
        TransactionGuard.getInstance().assertWriteSafeContext(ModalityState.nonModal())

        val total = Result()

        for (module in ModuleManager.getInstance(myProject).modules) {
            val rootModel = ModuleRootManager.getInstance(module).modifiableModel

            for (contentEntry in rootModel.contentEntries) {
                try {
                    val detector = ContentEntryDetector(contentEntry)
                    total.add(detector.detect())
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }

            rootModel.commit()
        }

        return total
    }

    internal inner class ContentEntryDetector(val myContentEntry: ContentEntry) {
        val myContentRootPath: Path

        val mySourceFoldersMap: MutableMap<VirtualFile?, SourceFolder> =
            getSourceFoldersMap(
                myContentEntry
            )

        val myFileSystem: VirtualFileSystem

        val myResult: Result = Result()

        init {
            val contentEntryFile = checkNotNull(myContentEntry.file)
            this.myContentRootPath = Paths.get(contentEntryFile.path)
            this.myFileSystem = contentEntryFile.fileSystem
        }

        @Throws(IOException::class)
        fun detect(): Result {
            val result = Result()

            result.add(detectDrupalCore())
            result.add(detectDrupalModules())

            return result
        }

        /**
         * @see [Drupal core's compser.json](https://cgit.drupalcode.org/drupal/tree/core/composer.json)
         */
        private fun detectDrupalCore(): Result {
            val result = Result()

            var dir = findFileInDir(myDrupalPath, "core/lib/Drupal/Core")
            if (dir != null) {
                markAsSourceRoot(dir, JavaSourceRootType.SOURCE, "Drupal\\Core")
            }

            dir = findFileInDir(myDrupalPath, "core/lib/Drupal/Component")
            if (dir != null) {
                markAsSourceRoot(dir, JavaSourceRootType.SOURCE, "Drupal\\Component")
            }

            dir = findFileInDir(myDrupalPath, "core/../drivers/lib/Drupal/Driver")
            if (dir != null) {
                markAsSourceRoot(dir, JavaSourceRootType.SOURCE, "Drupal\\Driver")
            }

            return result
        }

        @Throws(IOException::class)
        private fun detectDrupalModules(): Result {
            val rootDir = checkNotNull(myContentEntry.file)
            val rootPath = Paths.get(rootDir.path)

            Files.walkFileTree(
                rootPath,
                Drupal8ModuleFileVisitor { drupalModuleName: String?, drupalModulePath: Path? ->

                    var dir = findFileInDir(drupalModulePath!!, "src")
                    if (dir != null) {
                        val namespacePrefix = "Drupal\\$drupalModuleName"

                        if (isTestModule(dir)) {
                            markAsSourceRoot(dir, JavaSourceRootType.TEST_SOURCE, namespacePrefix)
                        } else {
                            markAsSourceRoot(dir, JavaSourceRootType.SOURCE, namespacePrefix)
                        }
                    }

                    dir = findFileInDir(drupalModulePath, "src/Tests")
                    if (dir != null) {
                        markAsSourceRoot(dir, JavaSourceRootType.TEST_SOURCE, "")
                    }

                    dir = findFileInDir(drupalModulePath, "tests/src")
                    if (dir != null) {
                        val namespacePrefix = "Drupal\\Tests\\$drupalModuleName"
                        markAsSourceRoot(dir, JavaSourceRootType.TEST_SOURCE, namespacePrefix)
                    }
                })

            return myResult
        }

        private fun markAsSourceRoot(
            dir: VirtualFile,
            type: JpsModuleSourceRootType<JavaSourceRootProperties>,
            namespacePrefix: String
        ) {
            val sourceFolder = mySourceFoldersMap[dir]

            if (sourceFolder == null) {
                // Add new.
                addSourceFolder(dir, type, namespacePrefix)
            } else if (type != sourceFolder.rootType) {
                // Type miss match. Replace.
                replaceSourceFolder(sourceFolder, dir, type, namespacePrefix)
            } else if (namespacePrefix != sourceFolder.packagePrefix) {
                // Namespace prefix miss match. Update.
                updateSourceFolderNamespacePrefix(sourceFolder, dir, namespacePrefix)
            }
        }

        private fun addSourceFolder(
            dir: VirtualFile,
            type: JpsModuleSourceRootType<JavaSourceRootProperties>,
            namespacePrefix: String
        ) {
            myResult.myAdded++

            myContentEntry.addSourceFolder(
                dir, type, JpsJavaExtensionService.getInstance().createSourceRootProperties(namespacePrefix)
            )

            myLogger.info(MessageFormat.format("Add ''{0}'', prefix ''{1}''.", getShortPath(dir), namespacePrefix))
        }

        private fun replaceSourceFolder(
            originalSourceFolder: SourceFolder,
            dir: VirtualFile,
            type: JpsModuleSourceRootType<JavaSourceRootProperties>,
            namespacePrefix: String
        ) {
            myResult.myUpdated++

            myContentEntry.removeSourceFolder(originalSourceFolder)
            mySourceFoldersMap.remove(dir)

            myContentEntry.addSourceFolder(
                dir, type, JpsJavaExtensionService.getInstance().createSourceRootProperties(namespacePrefix)
            )

            myLogger.info(MessageFormat.format("Replace ''{0}'', prefix ''{1}''.", getShortPath(dir), namespacePrefix))
        }

        private fun updateSourceFolderNamespacePrefix(
            sourceFolder: SourceFolder,
            dir: VirtualFile,
            namespacePrefix: String
        ) {
            myResult.myUpdated++

            sourceFolder.packagePrefix = namespacePrefix

            myLogger.info(MessageFormat.format("Update ''{0}'', prefix ''{1}''.", getShortPath(dir), namespacePrefix))
        }

        private fun getShortPath(file: VirtualFile): Path {
            return myContentRootPath.relativize(Paths.get(file.path))
        }

        private fun findFileInDir(dirPath: Path, filename: String): VirtualFile? {
            return myFileSystem.findFileByPath(dirPath.resolve(filename).toAbsolutePath().toString())
        }

        private fun isTestModule(moduleDir: VirtualFile): Boolean {
            return moduleDir.path.contains("/tests/")
        }
    }

    internal class Result {
        var myAdded: Int = 0
        var myUpdated: Int = 0

        fun add(other: Result) {
            myAdded += other.myAdded
            myUpdated += other.myUpdated
        }

        val isEmpty: Boolean
            get() = myAdded == 0 && myUpdated == 0
    }

    companion object {
        private fun getSourceFoldersMap(contentEntry: ContentEntry): MutableMap<VirtualFile?, SourceFolder> {
            val allSourceFolders: MutableMap<VirtualFile?, SourceFolder> = HashMap()

            for (sourceFolder in contentEntry.sourceFolders) {
                if (sourceFolder.file != null) {
                    allSourceFolders[sourceFolder.file] = sourceFolder
                }
            }

            return allSourceFolders
        }
    }
}
