package org.masamotod.idea.Drupal8NamespaceDetector.TestUtils

import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile
import org.masamotod.idea.Drupal8NamespaceDetector.SourceFolderTemplate

// "PATH [TYPE] [NS=namespace]"
fun summarizeSourceFolderTemplates(folders: Collection<SourceFolderTemplate>, projectRoot: VirtualFile): String {
    return folders.map {
        val path = it.file.path.removePrefix(projectRoot.path + "/")
        val type = if (it.isTestSource) "[TEST]" else "[SOURCE]"
        val ns = "[NS=${it.packagePrefix}]"
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
