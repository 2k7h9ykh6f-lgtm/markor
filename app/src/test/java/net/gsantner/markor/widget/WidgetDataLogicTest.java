/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.widget;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link WidgetDataLogic} – covers todo-widget task parsing,
 * file-browser-widget file listing, and shortcut-target validation.
 *
 * <h3>Scenarios covered</h3>
 * <ul>
 *   <li>Widget refresh with empty todo.txt (empty list)</li>
 *   <li>Widget refresh with null/missing content</li>
 *   <li>Todo task parsing with multi-line content</li>
 *   <li>File listing from valid directory</li>
 *   <li>File listing from empty directory (widget refresh empty list)</li>
 *   <li>File listing from non-existent directory</li>
 *   <li>Hidden file filtering</li>
 *   <li>Shortcut pointing to deleted file</li>
 *   <li>Shortcut pointing to valid file</li>
 * </ul>
 */
public class WidgetDataLogicTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ===================================================================
    //  Todo-widget: parseTodoTaskDescriptions
    // ===================================================================

    @Test
    public void parseTodoTasks_nullContent_returnsEmptyList() {
        List<String> tasks = WidgetDataLogic.parseTodoTaskDescriptions(null);
        assertThat(tasks).isEmpty();
    }

    @Test
    public void parseTodoTasks_emptyContent_returnsEmptyList() {
        assertThat(WidgetDataLogic.parseTodoTaskDescriptions("")).isEmpty();
    }

    @Test
    public void parseTodoTasks_whitespaceOnly_returnsEmptyList() {
        assertThat(WidgetDataLogic.parseTodoTaskDescriptions("   \n  \n  ")).isEmpty();
    }

    @Test
    public void parseTodoTasks_singleTask_returnsOneElement() {
        List<String> tasks = WidgetDataLogic.parseTodoTaskDescriptions("Buy milk");
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0)).isEqualTo("Buy milk");
    }

    @Test
    public void parseTodoTasks_multipleTasks_returnsAll() {
        String content = "Buy milk\nWrite report\nCall dentist";
        List<String> tasks = WidgetDataLogic.parseTodoTaskDescriptions(content);
        assertThat(tasks).hasSize(3);
        assertThat(tasks).containsExactly("Buy milk", "Write report", "Call dentist");
    }

    @Test
    public void parseTodoTasks_withCompletedTasks_includesAll() {
        String content = "Buy milk\nx 2025-01-15 Done task\nCall dentist";
        List<String> tasks = WidgetDataLogic.parseTodoTaskDescriptions(content);
        assertThat(tasks).hasSize(3);
        assertThat(tasks.get(1)).startsWith("x ");
    }

    @Test
    public void parseTodoTasks_withBlankLines_skipsBlankLines() {
        String content = "Buy milk\n\n\nCall dentist\n";
        List<String> tasks = WidgetDataLogic.parseTodoTaskDescriptions(content);
        assertThat(tasks).hasSize(2);
    }

    @Test
    public void parseTodoTasks_trimsWhitespace() {
        String content = "  Buy milk  \n  Call dentist  ";
        List<String> tasks = WidgetDataLogic.parseTodoTaskDescriptions(content);
        assertThat(tasks).containsExactly("Buy milk", "Call dentist");
    }

    // ===================================================================
    //  Todo-widget: getTaskCount
    // ===================================================================

    @Test
    public void getTaskCount_nullContent_returnsZero() {
        assertThat(WidgetDataLogic.getTaskCount(null)).isZero();
    }

    @Test
    public void getTaskCount_emptyContent_returnsZero() {
        assertThat(WidgetDataLogic.getTaskCount("")).isZero();
    }

    @Test
    public void getTaskCount_threeTasks_returnsThree() {
        assertThat(WidgetDataLogic.getTaskCount("A\nB\nC")).isEqualTo(3);
    }

    @Test
    public void getTaskCount_blankLinesNotCounted() {
        assertThat(WidgetDataLogic.getTaskCount("A\n\nB\n\n")).isEqualTo(2);
    }

    // ===================================================================
    //  Widget refresh empty list – the key scenario
    // ===================================================================

    @Test
    public void widgetRefresh_emptyTodoFile_showsEmptyList() throws IOException {
        // Simulate a todo.txt file that exists but is empty
        File emptyTodo = tempFolder.newFile("todo.txt");
        // File exists but has no content
        String content = "";

        assertThat(WidgetDataLogic.getTaskCount(content)).isZero();
        assertThat(WidgetDataLogic.parseTodoTaskDescriptions(content)).isEmpty();
    }

    @Test
    public void widgetRefresh_missingTodoFile_showsEmptyList() {
        // Simulate a todo.txt file that doesn't exist (deleted or not created yet)
        // loadContent would return null
        String content = null;

        assertThat(WidgetDataLogic.getTaskCount(content)).isZero();
        assertThat(WidgetDataLogic.parseTodoTaskDescriptions(content)).isEmpty();
    }

    // ===================================================================
    //  File-browser-widget: listVisibleFiles
    // ===================================================================

    @Test
    public void listVisibleFiles_validDirectory_returnsFiles() throws IOException {
        File dir = tempFolder.newFolder("notes");
        new File(dir, "note1.md").createNewFile();
        new File(dir, "note2.md").createNewFile();

        List<File> files = WidgetDataLogic.listVisibleFiles(dir);

        assertThat(files).hasSize(2);
    }

    @Test
    public void listVisibleFiles_emptyDirectory_returnsEmptyList() throws IOException {
        File dir = tempFolder.newFolder("empty_notes");

        List<File> files = WidgetDataLogic.listVisibleFiles(dir);

        assertThat(files).isEmpty();
    }

    @Test
    public void listVisibleFiles_nonExistentDirectory_returnsEmptyList() {
        File dir = new File(tempFolder.getRoot(), "does_not_exist");

        List<File> files = WidgetDataLogic.listVisibleFiles(dir);

        assertThat(files).isEmpty();
    }

    @Test
    public void listVisibleFiles_nullDirectory_returnsEmptyList() {
        assertThat(WidgetDataLogic.listVisibleFiles(null)).isEmpty();
    }

    @Test
    public void listVisibleFiles_hidesHiddenFiles() throws IOException {
        File dir = tempFolder.newFolder("notes_hidden");
        new File(dir, "visible.md").createNewFile();
        new File(dir, ".hidden.md").createNewFile();

        List<File> files = WidgetDataLogic.listVisibleFiles(dir);

        assertThat(files).hasSize(1);
        assertThat(files.get(0).getName()).isEqualTo("visible.md");
    }

    @Test
    public void listVisibleFiles_includesSubdirectories() throws IOException {
        File dir = tempFolder.newFolder("notes_subdirs");
        new File(dir, "note.md").createNewFile();
        new File(dir, "subfolder").mkdir();

        List<File> files = WidgetDataLogic.listVisibleFiles(dir);

        assertThat(files).hasSize(2);
    }

    // ===================================================================
    //  File-browser-widget: listAllFiles (includes hidden)
    // ===================================================================

    @Test
    public void listAllFiles_includesHiddenFiles() throws IOException {
        File dir = tempFolder.newFolder("all_files");
        new File(dir, "visible.md").createNewFile();
        new File(dir, ".hidden.md").createNewFile();

        List<File> files = WidgetDataLogic.listAllFiles(dir);

        assertThat(files).hasSize(2);
    }

    @Test
    public void listAllFiles_nullDirectory_returnsEmptyList() {
        assertThat(WidgetDataLogic.listAllFiles(null)).isEmpty();
    }

    // ===================================================================
    //  Shortcut: isShortcutTargetValid (deleted file scenario)
    // ===================================================================

    @Test
    public void isShortcutTargetValid_existingFile_returnsTrue() throws IOException {
        File file = tempFolder.newFile("shortcut-target.md");
        assertThat(WidgetDataLogic.isShortcutTargetValid(file)).isTrue();
    }

    @Test
    public void isShortcutTargetValid_deletedFile_returnsFalse() throws IOException {
        File file = tempFolder.newFile("shortcut-target.md");
        file.delete();
        assertThat(WidgetDataLogic.isShortcutTargetValid(file)).isFalse();
    }

    @Test
    public void isShortcutTargetValid_nullFile_returnsFalse() {
        assertThat(WidgetDataLogic.isShortcutTargetValid(null)).isFalse();
    }

    @Test
    public void isShortcutTargetValid_nonExistentPath_returnsFalse() {
        assertThat(WidgetDataLogic.isShortcutTargetValid(
                new File("/non/existent/path.md"))).isFalse();
    }

    @Test
    public void shortcutToDeletedFile_endToEnd() throws IOException {
        // Create a file, create a "shortcut" reference, then delete the file
        File note = tempFolder.newFile("important-note.md");
        File shortcutTarget = new File(note.getAbsolutePath());

        // Shortcut target is valid initially
        assertThat(WidgetDataLogic.isShortcutTargetValid(shortcutTarget)).isTrue();

        // User deletes the file
        note.delete();

        // Now the shortcut target is invalid
        assertThat(WidgetDataLogic.isShortcutTargetValid(shortcutTarget)).isFalse();
    }

    // ===================================================================
    //  filterExistingFiles
    // ===================================================================

    @Test
    public void filterExistingFiles_allExist_returnsAll() throws IOException {
        File f1 = tempFolder.newFile("a.md");
        File f2 = tempFolder.newFile("b.md");

        List<File> result = WidgetDataLogic.filterExistingFiles(Arrays.asList(f1, f2));
        assertThat(result).hasSize(2);
    }

    @Test
    public void filterExistingFiles_someDeleted_filtersOut() throws IOException {
        File f1 = tempFolder.newFile("a.md");
        File f2 = tempFolder.newFile("b.md");
        f2.delete();

        List<File> result = WidgetDataLogic.filterExistingFiles(Arrays.asList(f1, f2));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("a.md");
    }

    @Test
    public void filterExistingFiles_emptyList_returnsEmpty() {
        assertThat(WidgetDataLogic.filterExistingFiles(Collections.emptyList())).isEmpty();
    }

    @Test
    public void filterExistingFiles_nullElements_filtered() {
        List<File> input = Arrays.asList(null, null);
        assertThat(WidgetDataLogic.filterExistingFiles(input)).isEmpty();
    }

    // ===================================================================
    //  isValidWidgetDirectory
    // ===================================================================

    @Test
    public void isValidWidgetDirectory_validDir_returnsTrue() throws IOException {
        File dir = tempFolder.newFolder("widget_dir");
        assertThat(WidgetDataLogic.isValidWidgetDirectory(dir)).isTrue();
    }

    @Test
    public void isValidWidgetDirectory_file_notDirectory_returnsFalse() throws IOException {
        File file = tempFolder.newFile("not-a-dir.txt");
        assertThat(WidgetDataLogic.isValidWidgetDirectory(file)).isFalse();
    }

    @Test
    public void isValidWidgetDirectory_null_returnsFalse() {
        assertThat(WidgetDataLogic.isValidWidgetDirectory(null)).isFalse();
    }

    @Test
    public void isValidWidgetDirectory_nonExistent_returnsFalse() {
        assertThat(WidgetDataLogic.isValidWidgetDirectory(
                new File("/non/existent/dir"))).isFalse();
    }

    // ===================================================================
    //  Widget refresh with content – realistic todo.txt
    // ===================================================================

    @Test
    public void widgetRefresh_realisticTodoTxt_parsesCorrectly() {
        String content = "(A) Call Mom +family\n"
                + "(B) Buy groceries +shopping\n"
                + "x 2025-01-14 (C) Pay bills +finance\n"
                + "Write blog post @computer\n"
                + "\n"
                + "(A) Review PR #123 @work";

        List<String> tasks = WidgetDataLogic.parseTodoTaskDescriptions(content);

        assertThat(tasks).hasSize(5);
        assertThat(tasks.get(0)).isEqualTo("(A) Call Mom +family");
        assertThat(tasks.get(2)).startsWith("x ");
    }

    @Test
    public void widgetRefresh_todoWithOnlyBlankLines_returnsZero() {
        assertThat(WidgetDataLogic.getTaskCount("\n\n\n")).isZero();
    }

    @Test
    public void widgetRefresh_singleLineNoNewline_parsesAsOne() {
        assertThat(WidgetDataLogic.getTaskCount("Single task")).isEqualTo(1);
    }
}
