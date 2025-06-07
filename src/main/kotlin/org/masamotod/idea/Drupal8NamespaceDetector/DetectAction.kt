package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.PrimaryModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.php.drupal.settings.DrupalDataService

class DetectAction : AnAction() {

    private val logger = logger<DetectAction>()

    override fun actionPerformed(action: AnActionEvent) {
        val project = action.project
        if (project == null) {
            logger.warn("Action performed without a project context.")
            return
        }

        val drupalDataService = DrupalDataService.getInstance(project)
        if (!drupalDataService.isConfigValid) {
            Notifier.notifyDrupalSupportRequired(project)
            return
        }

        val drupalRoot = LocalFileSystem.getInstance().findFileByPath(drupalDataService.drupalPath)
        if (drupalRoot == null) {
            logger.warn("Drupal root (${drupalDataService.drupalPath}) not found for project: ${project.name}")
            return
        }

        val primaryModule = PrimaryModuleManager.findPrimaryModule(project)
        if (primaryModule == null) {
            logger.warn("Primary module not found for project: ${project.name}")
            return
        }

        ProgressManager.getInstance().run(DetectorTask(primaryModule, drupalRoot))
    }
}
