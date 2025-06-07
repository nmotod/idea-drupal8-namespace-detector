package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.PrimaryModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.php.drupal.settings.DrupalDataService

class DetectAction : AnAction() {

    override fun actionPerformed(action: AnActionEvent) {
        val project = action.project ?: return

        val drupalDataService = DrupalDataService.getInstance(project)

        if (!drupalDataService.isConfigValid) {
            Notifier.notifyDrupalSupportRequired(project)
            return
        }

        val drupalRoot = LocalFileSystem.getInstance().findFileByPath(drupalDataService.drupalPath)

        val primaryModule = PrimaryModuleManager.findPrimaryModule(project) ?: return

        ProgressManager.getInstance().run(DetectorTask(
            primaryModule,
            drupalRoot ?: return
        ))
    }
}
