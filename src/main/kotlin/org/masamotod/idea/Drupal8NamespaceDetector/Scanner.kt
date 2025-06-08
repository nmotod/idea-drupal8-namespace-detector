package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor

/**
  * Scans the Drupal root directory for namespace roots, including core and module directories.
 *
 * This class is responsible for identifying source and test directories within the Drupal core
 * and modules by scanning for specific directory structures and .info.yml files.
 *
 * @property project The current project instance.
 * @property drupalRoot The root directory of the Drupal installation.
 */
class Scanner(private val project: Project, private val drupalRoot: VirtualFile) {

    private val logger = logger<Scanner>()

    /**
     * Scans the Drupal root directory for namespace roots.
     *
     * This method combines the results from scanning the core directory and the modules.
     *
     * @return A list of SourceFolderTemplate objects representing the source and test directories.
     */
    fun scan(): List<SourceFolderTemplate> {
        return scanCore() + scanExtensions()
    }

    /**
     * Scans the core directory for source and test directories.
     *
     * @return A list of SourceFolderTemplate objects representing the source and test directories of Drupal core.
     */
    private fun scanCore(): List<SourceFolderTemplate> {
        logger.trace("Scan core from ${drupalRoot.path}")

        return listOfNotNull(
            drupalRoot.findDirectory("core/lib")?.let { srcDir ->
                logger.trace("Found core/lib: ${srcDir.path}")

                SourceFolderTemplate(
                    file = srcDir,
                    isTestSource = false,
                    packagePrefix = ""
                )
            },
            drupalRoot.findDirectory("core/tests")?.let { testsDir ->
                logger.trace("Found core/tests: ${testsDir.path}")

                SourceFolderTemplate(
                    file = testsDir,
                    isTestSource = true,
                    packagePrefix = ""
                )
            }
        )
    }

    /**
     * Scans the project for extensions (modules and themes) by looking for .info.yml files.
     *
     * @return A list of SourceFolderTemplate objects representing the source and test directories of extensions.
     */
    private fun scanExtensions(): List<SourceFolderTemplate> {
        val templates = mutableListOf<SourceFolderTemplate>()

        val projectScope = GlobalSearchScope.projectScope(project)

        FilenameIndex.processAllFileNames(Processor { fileName ->
            if (fileName.endsWith(".info.yml")) {
                val files = FilenameIndex.getVirtualFilesByName(fileName, projectScope)
                for (file in files) {
                    if (VfsUtilCore.isAncestor(drupalRoot, file, false)) {
                        templates.addAll(scanFromInfoFile(file))
                    }
                }
            }
            true
        }, projectScope, null)

        return templates
    }

    /**
     * Scans the extension directory from the .info.yml file.
     *
     * @param infoFile The .info.yml file to scan.
     * @return A list of SourceFolderTemplate objects representing the source and test directories.
     */
    private fun scanFromInfoFile(infoFile: VirtualFile): List<SourceFolderTemplate> {
        logger.trace("Scan extension from ${infoFile.path}")

        val dir = infoFile.parent ?: return emptyList()
        val extensionName = infoFile.name.substring(0, infoFile.name.length - ".info.yml".length)

        val relativePath = VfsUtilCore.getRelativePath(dir, drupalRoot)
        val isTestExtension = relativePath?.contains("/tests/") ?: false

        return listOfNotNull(
            dir.findChild("src")?.let { srcDir ->
                logger.trace("Found src: ${srcDir.path}")

                SourceFolderTemplate(
                    file = srcDir,
                    isTestSource = isTestExtension,
                    packagePrefix = "Drupal\\$extensionName"
                )
            },
            dir.findChild("tests")?.let { testsDir ->
                logger.trace("Found tests: ${testsDir.path}")

                SourceFolderTemplate(
                    file = testsDir,
                    isTestSource = true,
                    packagePrefix = ""
                )
            },
            dir.findDirectory("tests/src")?.let { testsSrcDir ->
                logger.trace("Found tests/src: ${testsSrcDir.path}")

                SourceFolderTemplate(
                    file = testsSrcDir,
                    isTestSource = true,
                    packagePrefix = "Drupal\\Tests\\$extensionName"
                )
            },
        )
    }
}
