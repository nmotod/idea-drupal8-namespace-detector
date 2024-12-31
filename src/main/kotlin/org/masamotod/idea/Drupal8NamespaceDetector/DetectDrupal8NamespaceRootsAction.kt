package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.php.drupal.settings.DrupalConfigurable
import com.jetbrains.php.drupal.settings.DrupalDataService
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.function.Function

class DetectDrupal8NamespaceRootsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = checkNotNull(e.project)
        val application = ApplicationManager.getApplication()

        val process = Runnable {
            application.invokeLater({
                val drupalDataService = DrupalDataService.getInstance(project)
                if (!drupalDataService.isConfigValid) {
                    notifyDrupalSupportRequired(project)
                } else {
                    val drupalPath = Paths.get(drupalDataService.drupalPath)

                    application.runWriteAction {
                        val detector = Drupal8NamespaceRootDetector(project, drupalPath)
                        val result = detector.detect()
                        if (result.isEmpty) {
                            notifyGlobally(
                                project,
                                "Detect Drupal 8 Namespace Roots",
                                "No root detected newly.",
                                NotificationType.INFORMATION
                            )
                        } else {
                            notifyGlobally(
                                project,
                                "Detect Drupal 8 Namespace Roots",
                                MessageFormat.format("Added {0}, updated {1}.", result.myAdded, result.myUpdated),
                                NotificationType.INFORMATION
                            )
                        }
                    }
                }
            }, ModalityState.nonModal())
        }

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            process, "Detecting Drupal 8 Module Roots...", true, project
        )
    }

    companion object {
        private fun notifyDrupalSupportRequired(project: Project) {
            val enableSupportAction =
                Function<Notification, AnAction> { notification: Notification ->
                    object : AnAction("Configure Drupal support") {
                        override fun actionPerformed(e: AnActionEvent) {
                            notification.expire()
                            val configurable = DrupalConfigurable(project)
                            ShowSettingsUtil.getInstance().editConfigurable(project, configurable)
                        }
                    }
                }

            notifyGlobally(
                project,
                "Drupal Support does not configured",
                "'Detect Drupal 8 Namespace Roots' requires Drupal Support.",
                NotificationType.WARNING,
                enableSupportAction
            )
        }

        /**
         * @see com.jetbrains.php.drupal.DrupalUtil.notifyGlobally
         */
        private fun notifyGlobally(
            project: Project?,
            title: String,
            message: String,
            notificationType: NotificationType,
            vararg actions: Function<Notification, AnAction>
        ) {
            val notification = Notification(
                "DetectDrupal8NamespaceRoots",
                title,
                message,
                notificationType
            )

            for (action in actions) {
                notification.addAction(action.apply(notification))
            }

            Notifications.Bus.notify(notification, project)
        }
    }
}
