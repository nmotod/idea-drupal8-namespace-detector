package org.masamotod.idea.Drupal8NamespaceDetector

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.junit.Test

class ScannerTest : LightPlatformCodeInsightFixture4TestCase() {

    @Test fun testScanCore() {
        addFilesToProject("""
            web/core/lib/Drupal.php
            web/core/lib/Drupal/Core/File.php
            web/core/tests/Drupal/Tests/File.php            
        """.trimIndent())

        val project = myFixture.project

        val drupalRoot = myFixture.findFileInTempDir("web")
        assertNotNull(drupalRoot)

        val templates = Scanner(project, drupalRoot).scan()

        val summary = summarizeSourceFolderTemplates(templates, myFixture.tempDirFixture.getFile(".")!!)

        assertEquals(
            """
            web/core/lib [SOURCE] [NS=]
            web/core/tests [TEST] [NS=]
        """.trimIndent(), summary
        )
    }


    @Test fun testScanExtensions() {
        addFilesToProject("""
            outside_of_drupal/ignored/ignored.info.yml
            outside_of_drupal/ignored/src/File.php
            web/core/lib/Drupal.php
            web/core/lib/Drupal/Core/File.php
            web/core/modules/with_src/src/File.php
            web/core/modules/with_src/with_src.info.yml
            web/core/tests/Drupal/Tests/File.php
            web/modules/custom/with_src_tests/src/File.php
            web/modules/custom/with_src_tests/tests/src/File.php
            web/modules/custom/with_src_tests/with_src_tests.info.yml
            web/modules/custom/with_test_modules/tests/src/File.php
            web/modules/custom/with_test_modules/tests/test_submodule/src/File.php
            web/modules/custom/with_test_modules/tests/test_submodule/test_submodule.info.yml
            web/modules/custom/with_test_modules/tests/test_submodule/tests/src/File.php
            web/modules/custom/with_test_modules/with_test_modules.info.yml
            web/modules/custom/with_tests/tests/src/File.php
            web/modules/custom/with_tests/with_tests.info.yml
            web/modules/custom/without_src/without_src.info.yml
            web/modules/custom/without_src/resources/File.php
            web/modules/custom/not_module/File.php
            web/modules/custom/not_module/src/File.php
            web/themes/custom/theme_with_tests/theme_with_tests.info.yml
            web/themes/custom/theme_with_tests/tests/File.php
            web/themes/custom/theme_with_tests/tests/src/File.php
            web/themes/custom/theme_without_tests/theme_without_tests.info.yml
        """)

        val project = myFixture.project

        val drupalRoot = myFixture.findFileInTempDir("web")
        assertNotNull(drupalRoot)

        val templates = Scanner(project, drupalRoot).scan()

        val summary = summarizeSourceFolderTemplates(templates, myFixture.tempDirFixture.getFile(".")!!)

        assertEquals(
            """
            web/core/lib [SOURCE] [NS=]
            web/core/modules/with_src/src [SOURCE] [NS=Drupal\with_src]
            web/core/tests [TEST] [NS=]
            web/modules/custom/with_src_tests/src [SOURCE] [NS=Drupal\with_src_tests]
            web/modules/custom/with_src_tests/tests [TEST] [NS=]
            web/modules/custom/with_src_tests/tests/src [TEST] [NS=Drupal\Tests\with_src_tests]
            web/modules/custom/with_test_modules/tests [TEST] [NS=]
            web/modules/custom/with_test_modules/tests/src [TEST] [NS=Drupal\Tests\with_test_modules]
            web/modules/custom/with_test_modules/tests/test_submodule/src [TEST] [NS=Drupal\test_submodule]
            web/modules/custom/with_test_modules/tests/test_submodule/tests [TEST] [NS=]
            web/modules/custom/with_test_modules/tests/test_submodule/tests/src [TEST] [NS=Drupal\Tests\test_submodule]
            web/modules/custom/with_tests/tests [TEST] [NS=]
            web/modules/custom/with_tests/tests/src [TEST] [NS=Drupal\Tests\with_tests]
            web/themes/custom/theme_with_tests/tests [TEST] [NS=]
            web/themes/custom/theme_with_tests/tests/src [TEST] [NS=Drupal\Tests\theme_with_tests]
        """.trimIndent(), summary
        )
    }

    private fun addFilesToProject(fileListText: String) {
        fileListText
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { myFixture.addFileToProject(it, "") }
    }
}
