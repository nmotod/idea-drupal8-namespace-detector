package org.masamotod.idea.Drupal8NamespaceDetector;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

class Drupal8ModuleFileVisitor implements FileVisitor<Path> {

  @FunctionalInterface
  interface ModuleConsumer {
    void accept(String drupalModuleName, Path drupalModulePath);
  }

  private static final String INFO_YAML_SUFFIX = ".info.yml";

  private final ModuleConsumer myAction;

  Drupal8ModuleFileVisitor(ModuleConsumer action) {
    this.myAction = action;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    if (dir.getFileName().toString().startsWith(".")) {
      return FileVisitResult.SKIP_SUBTREE;
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    String filename = file.getFileName().toString();
    if (filename.endsWith(INFO_YAML_SUFFIX)) {
      String moduleName = filename.substring(0, filename.length() - INFO_YAML_SUFFIX.length());
      Path moduleDir = file.getParent();

      myAction.accept(moduleName, moduleDir);
    }

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    return FileVisitResult.CONTINUE;
  }
}
