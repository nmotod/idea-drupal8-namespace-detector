package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.readAndWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.PrimaryModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.reportProgress
import com.jetbrains.php.drupal.settings.DrupalDataService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetectAction : AnAction() {

    private val logger = logger<DetectAction>()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
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

        runWithModalProgressBlocking(project, "Detecting Drupal Namespace Roots...") {
            val result = reportProgress { reporter ->
                reporter.indeterminateStep {
                    readAndWriteAction {
                        val templates = Scanner(project, drupalRoot).scan()

                        writeAction {
                            val model = ModuleRootManager.getInstance(primaryModule).modifiableModel
                            val registrar = Registrar(model)
                            val result = registrar.addAll(templates)
                            model.commit()

                            result
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                Notifier.notify(
                    project,
                    "Detect Drupal Namespace Roots",
                    result.formatMessageHtml(),
                    NotificationType.INFORMATION
                )
            }
        }
    }
}
