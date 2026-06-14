/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure-logic helpers for widget data loading.
 * <p>
 * Extracted from {@link TodoWidgetRemoteViewsFactory} and
 * {@code WrFilesWidgetFactory} so the data-selection paths can be
 * unit-tested without any Android framework dependency.
 */
public final class WidgetDataLogic {

    private WidgetDataLogic() {
    }

    // -----------------------------------------------------------------------
    //  Todo-widget task parsing
    // -----------------------------------------------------------------------

    /**
     * Parse todo.txt tasks from raw file content.
     * Returns an empty list when content is {@code null} or blank.
     */
    @NonNull
    public static List<String> parseTodoTaskDescriptions(@Nullable String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tasks = new ArrayList<>();
        for (String line : content.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                tasks.add(trimmed);
            }
        }
        return tasks;
    }

    /**
     * Return the number of tasks that would be shown in the widget.
     */
    public static int getTaskCount(@Nullable String content) {
        return parseTodoTaskDescriptions(content).size();
    }

    // -----------------------------------------------------------------------
    //  File-browser-widget file listing
    // -----------------------------------------------------------------------

    /**
     * List the visible (non-hidden) children of a directory.
     * Returns an empty list when the directory does not exist, is not readable,
     * or contains no children.
     */
    @NonNull
    public static List<File> listVisibleFiles(@Nullable File directory) {
        if (directory == null || !directory.exists() || !directory.canRead()) {
            return Collections.emptyList();
        }
        File[] files = directory.listFiles(f -> !f.isHidden() && !f.getName().startsWith("."));
        if (files == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(files));
    }

    /**
     * List all children of a directory, including hidden ones.
     * Returns an empty list when the directory is not accessible.
     */
    @NonNull
    public static List<File> listAllFiles(@Nullable File directory) {
        if (directory == null || !directory.exists() || !directory.canRead()) {
            return Collections.emptyList();
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(files));
    }

    /**
     * Filter a file list, removing files that no longer exist (e.g. deleted
     * after a shortcut was created).
     */
    @NonNull
    public static List<File> filterExistingFiles(@NonNull List<File> files) {
        List<File> result = new ArrayList<>();
        for (File f : files) {
            if (f != null && f.exists()) {
                result.add(f);
            }
        }
        return result;
    }

    /**
     * Check whether a file referenced by a shortcut still exists.
     * This is the key guard for the "shortcut to deleted file" scenario.
     */
    public static boolean isShortcutTargetValid(@Nullable File file) {
        return file != null && file.exists();
    }

    /**
     * Check whether a widget directory is valid (exists and is a directory).
     */
    public static boolean isValidWidgetDirectory(@Nullable File dir) {
        return dir != null && dir.exists() && dir.isDirectory();
    }
}
