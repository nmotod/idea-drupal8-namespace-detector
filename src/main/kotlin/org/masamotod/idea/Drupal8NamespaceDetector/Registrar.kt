package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ProjectFileIndex

class Registrar(private val model: ModifiableRootModel) {
    private val projectFileIndex = ProjectFileIndex.getInstance(model.project)
    private val sourceFolderIndex = SourceFolderIndex(model)

    private val added = mutableSetOf<SourceFolderTemplate>()
    private val updated = mutableSetOf<SourceFolderTemplate>()
    private val skipped = mutableSetOf<SourceFolderTemplate>()
    private val invalid = mutableSetOf<SourceFolderTemplate>()

    data class Result(
        val added: Set<SourceFolderTemplate>,
        val updated: Set<SourceFolderTemplate>,
        val skipped: Set<SourceFolderTemplate>,
        val invalid: Set<SourceFolderTemplate>,
    ) {
        val addedOrUpdated: Set<SourceFolderTemplate> = added + updated
    }

    fun addAll(folders: Collection<SourceFolderTemplate>): Result {
        for (folder in folders) {
            add(folder)
        }

        return Result(
            added = added,
            updated = updated,
            skipped = skipped,
            invalid = invalid
        )
    }

    private fun add(folder: SourceFolderTemplate) {
        val contentRoot = projectFileIndex.getContentRootForFile(folder.file)
        val contentEntry = model.contentEntries.firstOrNull { contentRoot == it.file }

        if (contentEntry == null) {
            invalid.add(folder)
            return
        }

        val oldSourceFolder = sourceFolderIndex.lookup(folder.file)

        if (oldSourceFolder == null) {
            // If the source folder does not exist, add it
            contentEntry.addSourceFolder(folder.file, folder.isTestSource, folder.packagePrefix)
            added.add(folder)

        } else if (folder.equalsToSourceFolder(oldSourceFolder)) {
            // If the source folder already exists with the same properties, skip it
            skipped.add(folder)

        } else {
            // If the source folder exists but has different properties, update it
            contentEntry.removeSourceFolder(oldSourceFolder)
            contentEntry.addSourceFolder(folder.file, folder.isTestSource, folder.packagePrefix)
            updated.add(folder)
        }
    }
}
