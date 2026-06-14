/*#######################################################
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Robolectric unit tests for the ACTION_SEND text extraction / url sanitize
 *   used by the share-into entry point (DocumentShareIntoFragment).
 *
#########################################################*/
package net.gsantner.markor.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = Application.class)
public class DocumentShareIntoTextTest {

    // --- extractShareText: ACTION_SEND text -----------------------------------

    @Test
    public void extractShareText_plainText_returnedAsIs() {
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, "hello world");

        assertEquals("hello world", DocumentShareIntoFragment.extractShareText(intent));
    }

    @Test
    public void extractShareText_plainTextIsTrimmed() {
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, "   spaced text   ");

        assertEquals("spaced text", DocumentShareIntoFragment.extractShareText(intent));
    }

    @Test
    public void extractShareText_bareUrl_returnedSanitizedWithoutSubject() {
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, "https://example.com");

        // No tracking params -> sanitize leaves it untouched, no subject prepended
        assertEquals("https://example.com", DocumentShareIntoFragment.extractShareText(intent));
    }

    @Test
    public void extractShareText_urlWithSubject_prependsSubject() {
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, "Cool Site")
                .putExtra(Intent.EXTRA_TEXT, "https://example.com");

        assertEquals("Cool Site https://example.com",
                DocumentShareIntoFragment.extractShareText(intent));
    }

    @Test
    public void extractShareText_plainTextWithSubject_ignoresSubject() {
        // Subject is only merged in when the text is a web url
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, "Subject")
                .putExtra(Intent.EXTRA_TEXT, "just some notes");

        assertEquals("just some notes", DocumentShareIntoFragment.extractShareText(intent));
    }

    @Test
    public void extractShareText_emptyText_returnsEmpty() {
        final Intent intent = new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "");
        assertEquals("", DocumentShareIntoFragment.extractShareText(intent));
    }

    @Test
    public void extractShareText_missingText_returnsEmpty() {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        assertEquals("", DocumentShareIntoFragment.extractShareText(intent));
    }

    // --- sanitize: tracking-parameter stripping -------------------------------

    @Test
    public void sanitize_stripsCommonTrackers_keepsRealParams() {
        final String out = DocumentShareIntoFragment.sanitize(
                "https://x.com/a?utm_medium=e&fbclid=z&gclid=q&keep=1");

        assertTrue("real param kept: " + out, out.contains("keep=1"));
        assertFalse("utm stripped: " + out, out.contains("utm_medium"));
        assertFalse("fbclid stripped: " + out, out.contains("fbclid"));
        assertFalse("gclid stripped: " + out, out.contains("gclid"));
    }

    @Test
    public void sanitize_amazonUrl_alsoStripsQidAndSr() {
        final String out = DocumentShareIntoFragment.sanitize(
                "https://www.amazon.com/dp/B000?qid=1&sr=8-1&ref=foo&pd_rd=1");

        assertFalse("qid stripped: " + out, out.contains("qid="));
        assertFalse("sr stripped: " + out, out.contains("sr="));
        assertFalse("ref stripped: " + out, out.contains("ref="));
    }

    @Test
    public void sanitize_noTrackingParams_unchanged() {
        final String url = "https://example.com/page?id=42";
        assertEquals(url, DocumentShareIntoFragment.sanitize(url));
    }
}
