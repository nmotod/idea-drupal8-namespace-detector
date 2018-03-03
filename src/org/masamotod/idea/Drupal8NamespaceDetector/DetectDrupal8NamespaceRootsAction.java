package org.masamotod.idea.Drupal8NamespaceDetector;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

public class DetectDrupal8NamespaceRootsAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    assert project != null;

    final Application application = ApplicationManager.getApplication();

    Runnable process = () -> application.invokeLater(() -> {
      final String drupalPathValue = PropertiesComponent.getInstance().getValue("drupal.support.drupalPath");

      if (drupalPathValue == null || drupalPathValue.isEmpty()) {
        Notifications.Bus.notify(new Notification(
          getClass().getCanonicalName(),
          "Detected Drupal 8 Namespace Roots",
          "Drupal installation path does not configured.",
          NotificationType.WARNING
        ), project);
      }
      else {
        final Path drupalPath = Paths.get(drupalPathValue);

        application.runWriteAction(() -> {
          Drupal8NamespaceRootDetector detector = new Drupal8NamespaceRootDetector(project, drupalPath);
            Drupal8NamespaceRootDetector.Result result = detector.detect();

            final String message;
            if (result.isEmpty()) {
              message = "No root detected.";
            }
            else {
              message = MessageFormat.format("Added {0}, updated {1}.", result.myAdded, result.myUpdated);
            }

            Notifications.Bus.notify(new Notification(
              getClass().getCanonicalName(),
              "Detected Drupal 8 Namespace Roots",
              message,
              NotificationType.INFORMATION
            ), project);
        });
      }
    }, ModalityState.NON_MODAL);

    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      process, "Detecting Drupal 8 Module Roots...", true, project);
  }
}
