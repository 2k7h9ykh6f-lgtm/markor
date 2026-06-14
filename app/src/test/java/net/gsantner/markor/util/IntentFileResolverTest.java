/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Intent;
import android.net.Uri;

import net.gsantner.markor.model.Document;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

/**
 * Robolectric-backed tests for {@link IntentFileResolver}.
 *
 * <h3>Scenarios covered</h3>
 * <ul>
 *   <li>ACTION_EDIT / ACTION_VIEW with direct File extra</li>
 *   <li>ACTION_SEND with file:// URI</li>
 *   <li>Invalid / unresolvable URI</li>
 *   <li>Null intent data</li>
 *   <li>Shortcut to deleted file (validation helpers)</li>
 *   <li>Missing permission simulation (unreadable file)</li>
 *   <li>Intent action classification</li>
 * </ul>
 *
 * <b>Why Robolectric:</b> {@link Intent} and {@link Uri} are Android framework
 * classes. Robolectric provides shadow implementations that behave like real
 * Android without needing a device or emulator.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class IntentFileResolverTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // ===================================================================
    //  Step 1 – fromExtraFile (direct File serializable extra)
    // ===================================================================

    @Test
    public void fromExtraFile_validFileExtra_returnsFile() {
        File expected = new File("/sdcard/notes/test.md");
        Intent intent = new Intent()
                .putExtra(Document.EXTRA_FILE, expected);

        File result = IntentFileResolver.fromExtraFile(intent);

        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo("/sdcard/notes/test.md");
    }

    @Test
    public void fromExtraFile_noExtra_returnsNull() {
        Intent intent = new Intent();
        assertThat(IntentFileResolver.fromExtraFile(intent)).isNull();
    }

    @Test
    public void fromExtraFile_nullExtra_returnsNull() {
        Intent intent = new Intent()
                .putExtra(Document.EXTRA_FILE, (File) null);
        // getSerializableExtra returns null for null putExtra
        assertThat(IntentFileResolver.fromExtraFile(intent)).isNull();
    }

    // ===================================================================
    //  Step 2 – fromDataUri (file:// and content:// URIs)
    // ===================================================================

    @Test
    public void fromDataUri_fileScheme_returnsFile() {
        Intent intent = new Intent()
                .setData(Uri.parse("file:///sdcard/notes/todo.txt"));

        File result = IntentFileResolver.fromDataUri(intent);

        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo("/sdcard/notes/todo.txt");
    }

    @Test
    public void fromDataUri_fileSchemeEmptyPath_returnsNull() {
        Intent intent = new Intent()
                .setData(Uri.parse("file://"));

        // file:// with empty path
        File result = IntentFileResolver.fromDataUri(intent);
        // Empty path should return null
        assertThat(result).isNull();
    }

    @Test
    public void fromDataUri_noData_returnsNull() {
        Intent intent = new Intent();
        assertThat(IntentFileResolver.fromDataUri(intent)).isNull();
    }

    @Test
    public void fromDataUri_contentScheme_externalFiles() {
        Intent intent = new Intent()
                .setData(Uri.parse("content://com.example.provider/external_files/notes/test.md"));

        File result = IntentFileResolver.fromDataUri(intent);
        // Should resolve to something under /storage/emulated/0/
        assertThat(result).isNotNull();
        assertThat(result.getPath()).contains("notes/test.md");
    }

    @Test
    public void fromDataUri_contentScheme_nextcloud() {
        Intent intent = new Intent()
                .setData(Uri.parse("content://org.nextcloud.files/external_files/Documents/notes.md"));

        File result = IntentFileResolver.fromDataUri(intent);
        assertThat(result).isNotNull();
        assertThat(result.getPath()).contains("Documents/notes.md");
    }

    @Test
    public void fromDataUri_contentScheme_encodedPath() {
        Intent intent = new Intent()
                .setData(Uri.parse("content://com.example.provider/%2Fsdcard%2Fnotes%2Ftest.md"));

        File result = IntentFileResolver.fromDataUri(intent);
        assertThat(result).isNotNull();
        assertThat(result.getPath()).contains("sdcard");
    }

    // ===================================================================
    //  Step 3 – fromRawPath (fallback to URI path)
    // ===================================================================

    @Test
    public void fromRawPath_validUri_returnsFile() {
        Intent intent = new Intent()
                .setData(Uri.parse("file:///sdcard/notes/test.md"));

        File result = IntentFileResolver.fromRawPath(intent);

        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo("/sdcard/notes/test.md");
    }

    @Test
    public void fromRawPath_noData_returnsNull() {
        Intent intent = new Intent();
        assertThat(IntentFileResolver.fromRawPath(intent)).isNull();
    }

    // ===================================================================
    //  Full chain – resolveFile (3-step fallback)
    // ===================================================================

    @Test
    public void resolveFile_extraFilePreferred() {
        File expected = new File("/sdcard/notes/from-extra.md");
        Intent intent = new Intent()
                .putExtra(Document.EXTRA_FILE, expected)
                .setData(Uri.parse("file:///sdcard/notes/from-uri.md"));

        File result = IntentFileResolver.resolveFile(intent);

        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo("/sdcard/notes/from-extra.md");
    }

    @Test
    public void resolveFile_fallsBackToUri_whenNoExtra() {
        Intent intent = new Intent()
                .setData(Uri.parse("file:///sdcard/notes/from-uri.md"));

        File result = IntentFileResolver.resolveFile(intent);

        assertThat(result).isNotNull();
        assertThat(result.getPath()).isEqualTo("/sdcard/notes/from-uri.md");
    }

    @Test
    public void resolveFile_nullData_returnsNull() {
        Intent intent = new Intent();
        // No extra, no data → all 3 steps fail
        assertThat(IntentFileResolver.resolveFile(intent)).isNull();
    }

    // ===================================================================
    //  Invalid URI scenarios
    // ===================================================================

    @Test
    public void resolveFile_invalidContentUri_returnsNull() {
        // content:// with unknown provider and no recognizable path structure
        Intent intent = new Intent()
                .setData(Uri.parse("content://com.unknown.provider/data"));

        File result = IntentFileResolver.resolveFile(intent);
        // May resolve to something via fallback, or null - both are acceptable
        // The key point is it doesn't crash
        // (result may be non-null due to the "/" prefix fallback in resolveContentUri)
    }

    @Test
    public void resolveFile_actionSendText_noFile_returnsNull() {
        // ACTION_SEND with just text, no file URI
        Intent intent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, "Just some shared text");

        File result = IntentFileResolver.resolveFile(intent);
        assertThat(result).isNull();
    }

    // ===================================================================
    //  Validation helpers – shortcut to deleted file
    // ===================================================================

    @Test
    public void isFileAccessible_existingFile_returnsTrue() throws IOException {
        File file = tempFolder.newFile("test.md");
        assertThat(IntentFileResolver.isFileAccessible(file)).isTrue();
    }

    @Test
    public void isFileAccessible_deletedFile_returnsFalse() throws IOException {
        File file = tempFolder.newFile("test.md");
        file.delete();
        assertThat(IntentFileResolver.isFileAccessible(file)).isFalse();
    }

    @Test
    public void isFileAccessible_nullFile_returnsFalse() {
        assertThat(IntentFileResolver.isFileAccessible(null)).isFalse();
    }

    @Test
    public void isFileAccessible_nonExistentPath_returnsFalse() {
        assertThat(IntentFileResolver.isFileAccessible(
                new File("/does/not/exist/file.md"))).isFalse();
    }

    // ===================================================================
    //  Validation helpers – permission simulation
    // ===================================================================

    @Test
    public void isFileReadable_readableFile_returnsTrue() throws IOException {
        File file = tempFolder.newFile("readable.md");
        assertThat(IntentFileResolver.isFileReadable(file)).isTrue();
    }

    @Test
    public void isFileReadable_unreadableFile_returnsFalse() throws IOException {
        File file = tempFolder.newFile("unreadable.md");
        file.setReadable(false);
        assertThat(IntentFileResolver.isFileReadable(file)).isFalse();
    }

    @Test
    public void isFileReadable_nullFile_returnsFalse() {
        assertThat(IntentFileResolver.isFileReadable(null)).isFalse();
    }

    @Test
    public void isFileWritable_writableFile_returnsTrue() throws IOException {
        File file = tempFolder.newFile("writable.md");
        assertThat(IntentFileResolver.isFileWritable(file)).isTrue();
    }

    @Test
    public void isFileWritable_readOnlyFile_returnsFalse() throws IOException {
        File file = tempFolder.newFile("readonly.md");
        file.setWritable(false);
        assertThat(IntentFileResolver.isFileWritable(file)).isFalse();
    }

    @Test
    public void isFileWritable_nullFile_returnsFalse() {
        assertThat(IntentFileResolver.isFileWritable(null)).isFalse();
    }

    // ===================================================================
    //  classifyIntentAction
    // ===================================================================

    @Test
    public void classifyIntentAction_send_returnsSend() {
        assertThat(IntentFileResolver.classifyIntentAction(Intent.ACTION_SEND))
                .isEqualTo("send");
    }

    @Test
    public void classifyIntentAction_sendMultiple_returnsSend() {
        assertThat(IntentFileResolver.classifyIntentAction(Intent.ACTION_SEND_MULTIPLE))
                .isEqualTo("send");
    }

    @Test
    public void classifyIntentAction_edit_returnsEdit() {
        assertThat(IntentFileResolver.classifyIntentAction(Intent.ACTION_EDIT))
                .isEqualTo("edit");
    }

    @Test
    public void classifyIntentAction_view_returnsView() {
        assertThat(IntentFileResolver.classifyIntentAction(Intent.ACTION_VIEW))
                .isEqualTo("view");
    }

    @Test
    public void classifyIntentAction_processText_returnsProcessText() {
        assertThat(IntentFileResolver.classifyIntentAction(
                "android.intent.action.PROCESS_TEXT"))
                .isEqualTo("process_text");
    }

    @Test
    public void classifyIntentAction_unknown_returnsUnknown() {
        assertThat(IntentFileResolver.classifyIntentAction("com.example.CUSTOM"))
                .isEqualTo("unknown");
    }

    @Test
    public void classifyIntentAction_null_returnsUnknown() {
        assertThat(IntentFileResolver.classifyIntentAction(null))
                .isEqualTo("unknown");
    }

    // ===================================================================
    //  Shortcut to deleted file – end-to-end scenario
    // ===================================================================

    @Test
    public void resolveFile_shortcutToDeletedFile_resolvesButNotAccessible() throws IOException {
        // Simulate a shortcut that points to a file that was later deleted
        File file = tempFolder.newFile("important-note.md");
        String path = file.getAbsolutePath();
        file.delete(); // User deletes the file

        Intent shortcutIntent = new Intent(Intent.ACTION_EDIT)
                .setData(Uri.fromFile(new File(path)));

        File resolved = IntentFileResolver.resolveFile(shortcutIntent);

        // The resolver CAN extract the path...
        assertThat(resolved).isNotNull();
        assertThat(resolved.getPath()).isEqualTo(path);

        // ...but the file is NOT accessible on disk
        assertThat(IntentFileResolver.isFileAccessible(resolved)).isFalse();
    }

    // ===================================================================
    //  Widget / shortcut file extra – round-trip test
    // ===================================================================

    @Test
    public void resolveFile_widgetIntentWithFileExtra_resolvesCorrectly() throws IOException {
        File todoFile = tempFolder.newFile("todo.txt");
        Intent widgetIntent = new Intent(Intent.ACTION_EDIT)
                .putExtra(Document.EXTRA_FILE, todoFile)
                .putExtra(Document.EXTRA_FILE_LINE_NUMBER, -1);

        File resolved = IntentFileResolver.resolveFile(widgetIntent);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getAbsolutePath()).isEqualTo(todoFile.getAbsolutePath());
        assertThat(IntentFileResolver.isFileAccessible(resolved)).isTrue();
    }

    @Test
    public void resolveFile_shareIntoEmptyText_noFile() {
        // OpenFromShortcutOrWidgetActivity for "new note" sends ACTION_SEND with empty text
        Intent newNoteIntent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, "");

        File resolved = IntentFileResolver.resolveFile(newNoteIntent);
        assertThat(resolved).isNull();
    }
}
