package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.jetbrains.php.drupal.settings.DrupalConfigurable

internal object Notifier {

    val NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("org.masamotod.idea.Drupal8NamespaceDetector.notification.default")!!

    /**
     * @see com.jetbrains.php.drupal.DrupalUtil.notifyGlobally
     */
    fun notify(
        project: Project?,
        titleHtml: String,
        contentHtml: String,
        notificationType: NotificationType,
        vararg actionGenerators: (Notification) -> AnAction
    ) {
        val notification = NOTIFICATION_GROUP.createNotification(
            titleHtml,
            contentHtml,
            notificationType
        )

        for (generator in actionGenerators) {
            notification.addAction(generator(notification))
        }

        Notifications.Bus.notify(notification, project)
    }

    fun notifyDrupalSupportRequired(project: Project) {
        val enableSupportActionBuilder = { notification: Notification ->
            object : AnAction("Configure Drupal Support") {
                override fun actionPerformed(e: AnActionEvent) {
                    notification.expire()
                    val configurable = DrupalConfigurable(project)
                    ShowSettingsUtil.getInstance().editConfigurable(project, configurable)
                }
            }
        }

        notify(
            project,
            "Drupal Support does not configured",
            "'Detect Drupal Namespace Roots' requires Drupal Support.",
            NotificationType.WARNING,
            enableSupportActionBuilder
        )
    }
}
