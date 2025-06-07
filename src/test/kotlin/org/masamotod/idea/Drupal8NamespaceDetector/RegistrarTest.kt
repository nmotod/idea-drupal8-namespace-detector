package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.junit.Test
import org.masamotod.idea.Drupal8NamespaceDetector.TestUtils.summarizeSourceFolderTemplates
import org.masamotod.idea.Drupal8NamespaceDetector.TestUtils.summarizeSourceFolders

class RegistrarTest : LightPlatformCodeInsightFixture4TestCase() {

    private val model: ModifiableRootModel by lazy {
        ModuleRootManager.getInstance(myFixture.module).modifiableModel
    }

    private val contentEntry: ContentEntry by lazy { model.contentEntries[0] }

    private val projectRoot: VirtualFile by lazy { myFixture.tempDirFixture.getFile(".")!! }

    override fun setUp() {
        super.setUp()

        // Clear existing source folders (/src) before each test
        for (contentEntry in model.contentEntries) {
            for (sourceFolder in contentEntry.sourceFolders) {
                contentEntry.removeSourceFolder(sourceFolder)
            }
        }
    }

    @Test
    fun testNew() {
        val registrar = Registrar(model)

        val folders = listOf<SourceFolderTemplate>(
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_source"),
                isTestSource = false,
                packagePrefix = "Drupal\\new_source"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_test"),
                isTestSource = true,
                packagePrefix = "Drupal\\new_test"
            ),
        )

        val result = registrar.addAll(folders)

        assertEquals(
            """
            new_source [SOURCE] [NS=Drupal\new_source]
            new_test [TEST] [NS=Drupal\new_test]
            """.trimIndent(),
            summarizeSourceFolders(contentEntry.sourceFolders.toList(), projectRoot)
        )

        assertEquals(2, result.added.size)
        assertEquals(0, result.updated.size)
        assertEquals(0, result.skipped.size)
        assertEquals(0, result.invalid.size)
    }

    @Test
    fun testUpdate() {
        contentEntry.addSourceFolder(
            myFixture.tempDirFixture.findOrCreateDir("update_ns"),
            false,
            "Drupal\\update_ns__before"
        )
        contentEntry.addSourceFolder(
            myFixture.tempDirFixture.findOrCreateDir("update_type__actual_test"),
            false,
            "Drupal\\update_type__actual_test"
        )
        contentEntry.addSourceFolder(
            myFixture.tempDirFixture.findOrCreateDir("update_type__actual_source"),
            true,
            "Drupal\\update_type__actual_source"
        )

        val registrar = Registrar(model)

        val folders = listOf<SourceFolderTemplate>(
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_source"),
                isTestSource = false,
                packagePrefix = "Drupal\\new_source"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_test"),
                isTestSource = true,
                packagePrefix = "Drupal\\new_test"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("update_ns"),
                isTestSource = false,
                packagePrefix = "Drupal\\update_ns__after"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("update_type__actual_test"),
                isTestSource = true,
                packagePrefix = "Drupal\\update_type__actual_test"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("update_type__actual_source"),
                isTestSource = false,
                packagePrefix = "Drupal\\update_type__actual_source"
            ),
        )

        val result = registrar.addAll(folders)

        assertEquals(
            """
            new_source [SOURCE] [NS=Drupal\new_source]
            new_test [TEST] [NS=Drupal\new_test]
            update_ns [SOURCE] [NS=Drupal\update_ns__after]
            update_type__actual_source [SOURCE] [NS=Drupal\update_type__actual_source]
            update_type__actual_test [TEST] [NS=Drupal\update_type__actual_test]
            """.trimIndent(),
            summarizeSourceFolders(contentEntry.sourceFolders.toList(), projectRoot)
        )

        assertEquals(2, result.added.size)
        assertEquals(3, result.updated.size)
        assertEquals(0, result.skipped.size)
        assertEquals(0, result.invalid.size)
    }

    @Test
    fun testSkip() {
        contentEntry.addSourceFolder(
            myFixture.tempDirFixture.findOrCreateDir("skip"),
            false,
            "Drupal\\skip"
        )

        val registrar = Registrar(model)

        val folders = listOf<SourceFolderTemplate>(
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_source"),
                isTestSource = false,
                packagePrefix = "Drupal\\new_source"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_test"),
                isTestSource = true,
                packagePrefix = "Drupal\\new_test"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("skip"),
                isTestSource = false,
                packagePrefix = "Drupal\\skip"
            ),
        )

        val result = registrar.addAll(folders)

        assertEquals(
            """
            new_source [SOURCE] [NS=Drupal\new_source]
            new_test [TEST] [NS=Drupal\new_test]
            skip [SOURCE] [NS=Drupal\skip]
            """.trimIndent(),
            summarizeSourceFolders(contentEntry.sourceFolders.toList(), projectRoot)
        )

        assertEquals(2, result.added.size)
        assertEquals(0, result.updated.size)
        assertEquals(1, result.skipped.size)
        assertEquals(0, result.invalid.size)
    }

    @Test
    fun testInvalid() {
        val registrar = Registrar(model)

        val folders = listOf<SourceFolderTemplate>(
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_source"),
                isTestSource = false,
                packagePrefix = "Drupal\\new_source"
            ),
            SourceFolderTemplate(
                file = myFixture.tempDirFixture.findOrCreateDir("new_test"),
                isTestSource = true,
                packagePrefix = "Drupal\\new_test"
            ),
            SourceFolderTemplate(
                file = object : LightVirtualFile("not_in_content_root") {
                    override fun isDirectory(): Boolean {
                        return true
                    }
                },
                isTestSource = false,
                packagePrefix = "Drupal\\skip"
            ),
        )

        val result = registrar.addAll(folders)

        assertEquals(
            """
            new_source [SOURCE] [NS=Drupal\new_source]
            new_test [TEST] [NS=Drupal\new_test]
            """.trimIndent(),
            summarizeSourceFolders(contentEntry.sourceFolders.toList(), projectRoot)
        )

        assertEquals(2, result.added.size)
        assertEquals(0, result.updated.size)
        assertEquals(0, result.skipped.size)
        assertEquals(1, result.invalid.size)
    }
}
