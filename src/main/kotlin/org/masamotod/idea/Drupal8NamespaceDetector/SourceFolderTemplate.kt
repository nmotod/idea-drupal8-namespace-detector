package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile

data class SourceFolderTemplate(
    val file: VirtualFile,
    val isTestSource: Boolean,
    val packagePrefix: String,
) {
    init {
        if (!file.isDirectory) {
            throw IllegalArgumentException("Source folder must be a directory: ${file.path}")
        }
    }

    fun equalsToSourceFolder(other: SourceFolder): Boolean {
        return this.file.path == other.file?.path
                && this.isTestSource == other.isTestSource
                && this.packagePrefix == other.packagePrefix
    }
}
