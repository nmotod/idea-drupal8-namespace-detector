package org.masamotod.idea.Drupal8NamespaceDetector.TestUtils

import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile
import org.masamotod.idea.Drupal8NamespaceDetector.SourceFolderTemplate

// "PATH [TYPE] [NS=namespace]"
fun summarizeSourceFolderTemplates(templates: Collection<SourceFolderTemplate>, projectRoot: VirtualFile): String {
    return templates.map { template ->
        val path = template.file.path.removePrefix(projectRoot.path + "/")
        val type = if (template.isTestSource) "[TEST]" else "[SOURCE]"
        val ns = "[NS=${template.packagePrefix}]"
        "$path $type $ns"
    }.sorted().joinToString("\n")
}

fun summarizeSourceFolders(folders: Collection<SourceFolder>, projectRoot: VirtualFile): String {
    return folders.map { folder ->
        val path = folder.file?.path?.removePrefix(projectRoot.path + "/") ?: "<no file>"
        val type = if (folder.isTestSource) "[TEST]" else "[SOURCE]"
        val ns = "[NS=${folder.packagePrefix}]"
        "$path $type $ns"
    }.sorted().joinToString("\n")
}
