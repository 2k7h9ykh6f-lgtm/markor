/*#######################################################
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Robolectric unit tests for intent -> target-file resolution used by the
 *   widget, launcher-shortcut and share-into entry points.
 *
#########################################################*/
package net.gsantner.markor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsContextUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = Application.class)
public class MarkorContextUtilsIntentTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Context ctx() {
        return RuntimeEnvironment.getApplication();
    }

    // --- getIntentFile: how widget/shortcut deliver the target file -----------

    @Test
    public void getIntentFile_returnsSerializableExtraFile() throws Exception {
        // Widget/shortcut pending-intents put a java.io.File into EXTRA_FILE
        final File f = tmp.newFile("widget-note.md");
        final Intent intent = new Intent(Intent.ACTION_EDIT).putExtra(Document.EXTRA_FILE, f);

        assertEquals(f, MarkorContextUtils.getIntentFile(intent));
    }

    @Test
    public void getIntentFile_resolvesFileUriFromData() throws Exception {
        final File f = tmp.newFile("data-note.md");
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(f));

        final File out = MarkorContextUtils.getIntentFile(intent);
        assertNotNull(out);
        assertEquals(f.getAbsolutePath(), out.getAbsolutePath());
    }

    @Test
    public void getIntentFile_nullIntent_returnsNull() {
        assertNull(MarkorContextUtils.getIntentFile(null));
    }

    @Test
    public void getIntentFile_sendTextOnly_returnsNull() {
        // A plain ACTION_SEND text share carries no file and no data uri
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "hello");

        assertNull(MarkorContextUtils.getIntentFile(intent));
    }

    // --- getValidIntentFile: deleted-shortcut fallback behaviour --------------

    @Test
    public void getValidIntentFile_existingFile_returnsFile() throws Exception {
        final File f = tmp.newFile("exists.md");
        final File fallback = tmp.newFile("fallback.md");
        final Intent intent = new Intent(Intent.ACTION_EDIT).putExtra(Document.EXTRA_FILE, f);

        assertEquals(f, MarkorContextUtils.getValidIntentFile(intent, fallback));
    }

    @Test
    public void getValidIntentFile_shortcutToDeletedFile_returnsFallback() throws Exception {
        // Shortcut still points at a file that has since been deleted
        final File deleted = new File(tmp.getRoot(), "deleted.md"); // never created
        final File fallback = tmp.newFile("fallback.md");
        final Intent intent = new Intent(Intent.ACTION_EDIT).putExtra(Document.EXTRA_FILE, deleted);

        assertEquals(fallback, MarkorContextUtils.getValidIntentFile(intent, fallback));
    }

    @Test
    public void getValidIntentFile_nullIntent_returnsFallback() throws Exception {
        final File fallback = tmp.newFile("fallback.md");
        assertEquals(fallback, MarkorContextUtils.getValidIntentFile(null, fallback));
    }

    // --- extractFileFromIntent: URI / extra parsing ---------------------------

    @Test
    public void extractFileFromIntent_extraFilepath_returnsFile() throws Exception {
        final File f = tmp.newFile("fp.md");
        final Intent intent = new Intent(Intent.ACTION_VIEW)
                .putExtra(GsContextUtils.EXTRA_FILEPATH, f.getAbsolutePath());

        final File out = GsContextUtils.extractFileFromIntent(intent, ctx());
        assertNotNull(out);
        assertEquals(f.getAbsolutePath(), out.getAbsolutePath());
    }

    @Test
    public void extractFileFromIntent_fileUri_returnsFile() throws Exception {
        final File f = tmp.newFile("fu.md");
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(f));

        final File out = GsContextUtils.extractFileFromIntent(intent, ctx());
        assertNotNull(out);
        assertEquals(f.getAbsolutePath(), out.getAbsolutePath());
    }

    @Test
    public void extractFileFromIntent_invalidContentUri_returnsNullGracefully() {
        // Unresolvable content:// (no such provider / no permission) must not crash
        final Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("content://com.nonexistent.provider/foo/bar"));

        assertNull(GsContextUtils.extractFileFromIntent(intent, ctx()));
    }

    @Test
    public void extractFileFromIntent_nonMatchingAction_returnsNull() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        assertNull(GsContextUtils.extractFileFromIntent(intent, ctx()));
    }
}
