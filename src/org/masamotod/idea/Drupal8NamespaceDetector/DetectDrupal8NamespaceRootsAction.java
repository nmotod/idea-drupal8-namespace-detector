package org.masamotod.idea.Drupal8NamespaceDetector;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.drupal.settings.DrupalConfigurable;
import com.jetbrains.php.drupal.settings.DrupalDataService;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.function.Function;

public class DetectDrupal8NamespaceRootsAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    assert project != null;

    final Application application = ApplicationManager.getApplication();

    Runnable process = () -> application.invokeLater(() -> {
      DrupalDataService drupalDataService = DrupalDataService.getInstance(project);

      if (!drupalDataService.isConfigValid()) {
        notifyDrupalSupportRequired(project);
      }
      else {
        final Path drupalPath = Paths.get(drupalDataService.getDrupalPath());

        application.runWriteAction(() -> {
          Drupal8NamespaceRootDetector detector = new Drupal8NamespaceRootDetector(project, drupalPath);
            Drupal8NamespaceRootDetector.Result result = detector.detect();

            if (result.isEmpty()) {
              notifyGlobally(project, "Detect Drupal 8 Namespace Roots", "No root detected newly.", NotificationType.INFORMATION);
            }
            else {
              notifyGlobally(project, "Detect Drupal 8 Namespace Roots", MessageFormat.format("Added {0}, updated {1}.", result.myAdded, result.myUpdated), NotificationType.INFORMATION);
            }

        });
      }
    }, ModalityState.NON_MODAL);

    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      process, "Detecting Drupal 8 Module Roots...", true, project);
  }

  private static void notifyDrupalSupportRequired(Project project) {
    Function<Notification, AnAction> enableSupportAction = (notification) -> {
      return new AnAction("Configure Drupal support") {
        public void actionPerformed(AnActionEvent e) {
          notification.expire();
          DrupalConfigurable configurable = new DrupalConfigurable(project);
          ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
        }
      };
    };

    notifyGlobally(project, "Drupal Support does not configured", "'Detect Drupal 8 Namespace Roots' requires Drupal Support.", NotificationType.WARNING, enableSupportAction);
  }

  /**
   * @see com.jetbrains.php.drupal.DrupalUtil#notifyGlobally
   */
  private static void notifyGlobally(@Nullable Project project, String title, String message, NotificationType notificationType, Function<Notification, AnAction>... actions) {
    Notification notification = new Notification(
      "DetectDrupal8NamespaceRoots",
      title,
      message,
      notificationType
    );

    for (Function<Notification, AnAction> action : actions) {
      notification.addAction(action.apply(notification));
    }

    Notifications.Bus.notify(notification, project);
  }
}
