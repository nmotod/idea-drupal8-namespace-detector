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
    )

    fun addAll(templates: Collection<SourceFolderTemplate>): Result {
        for (template in templates) {
            add(template)
        }

        return Result(
            added = added,
            updated = updated,
            skipped = skipped,
            invalid = invalid
        )
    }

    private fun add(template: SourceFolderTemplate) {
        val contentRoot = projectFileIndex.getContentRootForFile(template.file)
        val contentEntry = model.contentEntries.firstOrNull { contentRoot == it.file }

        if (contentEntry == null) {
            invalid.add(template)
            return
        }

        val oldSourceFolder = sourceFolderIndex.lookup(template.file)

        if (oldSourceFolder == null) {
            // If the source folder does not exist, add it
            sourceFolderIndex.add(contentEntry.addSourceFolder(template.file, template.isTestSource, template.packagePrefix))
            added.add(template)

        } else if (template.equalsToSourceFolder(oldSourceFolder)) {
            // If the source folder already exists with the same properties, skip it
            skipped.add(template)

        } else {
            // If the source folder exists but has different properties, update it
            contentEntry.removeSourceFolder(oldSourceFolder)
            sourceFolderIndex.add(contentEntry.addSourceFolder(template.file, template.isTestSource, template.packagePrefix))
            updated.add(template)
        }
    }
}
