package org.masamotod.idea.Drupal8NamespaceDetector;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

class Drupal8NamespaceRootDetector {

  private final Logger myLogger = Logger.getInstance(getClass());

  @NotNull private final Project myProject;

  @NotNull private final Path myDrupalPath;

  Drupal8NamespaceRootDetector(@NotNull Project project, @NotNull Path drupalPath) {
    this.myProject = project;
    this.myDrupalPath = drupalPath;
  }

  @NotNull
  Result detect() {
    TransactionGuard.getInstance().assertWriteSafeContext(ModalityState.NON_MODAL);

    Result total = new Result();

    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();

      for (ContentEntry contentEntry : rootModel.getContentEntries()) {
        try {
          ContentEntryDetector detector = new ContentEntryDetector(contentEntry);
          total.add(detector.detect());
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      rootModel.commit();
    }

    return total;
  }

  private static Map<VirtualFile, SourceFolder> getSourceFoldersMap(ContentEntry contentEntry) {
    Map<VirtualFile, SourceFolder> allSourceFolders = new HashMap<>();

    for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
      if (sourceFolder.getFile() != null) {
        allSourceFolders.put(sourceFolder.getFile(), sourceFolder);
      }
    }

    return allSourceFolders;
  }

  class ContentEntryDetector {

    final ContentEntry myContentEntry;

    final Path myContentRootPath;

    final Map<VirtualFile, SourceFolder> mySourceFoldersMap;

    final VirtualFileSystem myFileSystem;

    final Result myResult = new Result();

    ContentEntryDetector(ContentEntry contentEntry) {
      this.myContentEntry = contentEntry;
      this.mySourceFoldersMap = getSourceFoldersMap(contentEntry);

      VirtualFile contentEntryFile = contentEntry.getFile();
      assert contentEntryFile != null;

      this.myContentRootPath = Paths.get(contentEntryFile.getPath());
      this.myFileSystem = contentEntryFile.getFileSystem();
    }

    Result detect() throws IOException {
      Result result = new Result();

      result.add(detectDrupalCore());
      result.add(detectDrupalModules());

      return result;
    }

    /**
     * @see <a href="https://cgit.drupalcode.org/drupal/tree/core/composer.json">Drupal core's compser.json</a>
     */
    private Result detectDrupalCore() {
      Result result = new Result();

      VirtualFile dir;

      dir = findFileInDir(myDrupalPath, "core/lib/Drupal/Core");
      if (dir != null) {
        markAsSourceRoot(dir, JavaSourceRootType.SOURCE, "Drupal\\Core");
      }

      dir = findFileInDir(myDrupalPath, "core/lib/Drupal/Component");
      if (dir != null) {
        markAsSourceRoot(dir, JavaSourceRootType.SOURCE, "Drupal\\Component");
      }

      dir = findFileInDir(myDrupalPath, "core/../drivers/lib/Drupal/Driver");
      if (dir != null) {
        markAsSourceRoot(dir, JavaSourceRootType.SOURCE, "Drupal\\Driver");
      }

      return result;
    }

    private Result detectDrupalModules() throws IOException {
      VirtualFile rootDir = myContentEntry.getFile();
      assert (rootDir != null);

      Path rootPath = Paths.get(rootDir.getPath());

      Files.walkFileTree(rootPath, new Drupal8ModuleFileVisitor((drupalModuleName, drupalModulePath) -> {
        VirtualFile dir;

        dir = findFileInDir(drupalModulePath, "src");
        if (dir != null) {
          String namespacePrefix = "Drupal\\" + drupalModuleName;

          if (isTestModule(dir)) {
            markAsSourceRoot(dir, JavaSourceRootType.TEST_SOURCE, namespacePrefix);
          }
          else {
            markAsSourceRoot(dir, JavaSourceRootType.SOURCE, namespacePrefix);
          }
        }

        dir = findFileInDir(drupalModulePath, "src/Tests");
        if (dir != null) {
          markAsSourceRoot(dir, JavaSourceRootType.TEST_SOURCE, "");
        }

        dir = findFileInDir(drupalModulePath, "tests/src");
        if (dir != null) {
          String namespacePrefix = "Drupal\\Tests\\" + drupalModuleName;
          markAsSourceRoot(dir, JavaSourceRootType.TEST_SOURCE, namespacePrefix);
        }
      }));

      return myResult;
    }

    private void markAsSourceRoot(VirtualFile dir, JpsModuleSourceRootType<JavaSourceRootProperties> type, String namespacePrefix) {
      final SourceFolder sourceFolder = mySourceFoldersMap.get(dir);

      if (sourceFolder == null) {
        // Add new.
        addSourceFolder(dir, type, namespacePrefix);
      }
      else if (!type.equals(sourceFolder.getRootType())) {
        // Type miss match. Replace.
        replaceSourceFolder(sourceFolder, dir, type, namespacePrefix);
      }
      else if (!namespacePrefix.equals(sourceFolder.getPackagePrefix())) {
        // Namespace prefix miss match. Update.
        updateSourceFolderNamespacePrefix(sourceFolder, dir, namespacePrefix);
      }
    }

    private void addSourceFolder(VirtualFile dir, JpsModuleSourceRootType<JavaSourceRootProperties> type, String namespacePrefix) {
      myResult.myAdded++;

      myContentEntry.addSourceFolder(
        dir, type, JpsJavaExtensionService.getInstance().createSourceRootProperties(namespacePrefix));

      myLogger.info(MessageFormat.format("Add ''{0}'', prefix ''{1}''.", getShortPath(dir), namespacePrefix));
    }

    private void replaceSourceFolder(SourceFolder originalSourceFolder, VirtualFile dir, JpsModuleSourceRootType<JavaSourceRootProperties> type, String namespacePrefix) {
      myResult.myUpdated++;

      myContentEntry.removeSourceFolder(originalSourceFolder);
      mySourceFoldersMap.remove(dir);

      myContentEntry.addSourceFolder(
        dir, type, JpsJavaExtensionService.getInstance().createSourceRootProperties(namespacePrefix));

      myLogger.info(MessageFormat.format("Replace ''{0}'', prefix ''{1}''.", getShortPath(dir), namespacePrefix));
    }

    private void updateSourceFolderNamespacePrefix(SourceFolder sourceFolder, VirtualFile dir, String namespacePrefix) {
      myResult.myUpdated++;

      sourceFolder.setPackagePrefix(namespacePrefix);

      myLogger.info(MessageFormat.format("Update ''{0}'', prefix ''{1}''.", getShortPath(dir), namespacePrefix));
    }

    private Path getShortPath(VirtualFile file) {
      return myContentRootPath.relativize(Paths.get(file.getPath()));
    }

    private VirtualFile findFileInDir(Path dirPath, String filename) {
      return myFileSystem.findFileByPath(dirPath.resolve(filename).toAbsolutePath().toString());
    }

    private boolean isTestModule(VirtualFile moduleDir) {
      return moduleDir.getPath().contains("/tests/");
    }
  }

  static class Result {
    int myAdded = 0;
    int myUpdated = 0;

    void add(Result other) {
      myAdded += other.myAdded;
      myUpdated += other.myUpdated;
    }

    boolean isEmpty() {
      return myAdded == 0 && myUpdated == 0;
    }
  }
}
