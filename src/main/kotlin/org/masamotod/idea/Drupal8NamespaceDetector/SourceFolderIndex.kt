package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile

class SourceFolderIndex(model: ModifiableRootModel) {
    private val byPath = mutableMapOf<String, SourceFolder>()

    init {
        for (contentEntry in model.contentEntries) {
            for (sourceFolder in contentEntry.sourceFolders) {
                val path = sourceFolder.file?.path
                if (path != null) {
                    byPath[path] = sourceFolder
                }
            }
        }
    }

    fun getSourceFolder(file: VirtualFile): SourceFolder? {
        return byPath[file.path]
    }
}
