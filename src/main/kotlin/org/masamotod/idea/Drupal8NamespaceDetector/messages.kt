package org.masamotod.idea.Drupal8NamespaceDetector

fun Registrar.Result.formatMessageHtml(): String {
    val addedCount = added.size
    val updatedCount = updated.size

    if (addedCount == 0 && updatedCount == 0) {
        return "No new roots detected."
    }

    val lines = mutableListOf<String>()

    if (addedCount > 0) {
        lines.add("Added $addedCount new roots.")
    }

    if (updatedCount > 0) {
        lines.add("Updated $updatedCount existing roots.")
    }

    return lines.joinToString("<br>\n")
}
