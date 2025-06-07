package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile

class DetectorTask(
    private val module: Module,
    private val drupalRoot: VirtualFile,
) : Task.Modal(
    module.project,
    "Detecting Drupal Namespace Roots...",
    true
) {
    private var result: Registrar.Result? = null

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true

        val folders = ReadAction.compute<List<SourceFolderTemplate>, Throwable> {
            Scanner(project, drupalRoot).scan()
        }

        ApplicationManager.getApplication().invokeAndWait({
            WriteAction.run<Throwable> {
                val model = ModuleRootManager.getInstance(module).modifiableModel

                val registrar = Registrar(model)
                result = registrar.addAll(folders)

                model.commit()
            }
        }, ModalityState.defaultModalityState())
    }

    override fun onSuccess() {
        val result = result ?: return

        Notifier.notify(
            project,
            "Detect Drupal Namespace Roots",
            result.formatMessageHtml(),
            NotificationType.INFORMATION
        )
    }
}
