/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.model;

import static org.assertj.core.api.Assertions.assertThat;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import net.gsantner.opoc.util.GsFileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Robolectric tests for {@link Document} state management.
 * Tests content hashing, change tracking, serialization, and pure utility methods.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DocumentStateTest {

    private File tempDir;
    private File tempFile;

    @Before
    public void setUp() throws IOException {
        Application app = ApplicationProvider.getApplicationContext();
        tempDir = new File(app.getFilesDir(), "test_docs");
        tempDir.mkdirs();
        tempFile = new File(tempDir, "test.md");
        writeFile(tempFile, "# Hello World\n\nThis is a test document.\n");
    }

    @After
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            GsFileUtils.deleteRecursive(tempDir);
        }
    }

    private void writeFile(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }

    // ===== isContentSame =====

    @Test
    public void isContentSame_sameAfterSettingHash() {
        Document doc = new Document(tempFile);
        // Simulate what loadContent does: set content hash
        String content = "# Hello World\n\nThis is a test document.\n";
        // We can't call setContentHash directly (private), so we use loadContent
        // But loadContent requires Context. Let's test via the public API.
        // Instead, we test by loading the document content.
        Application app = ApplicationProvider.getApplicationContext();
        String loaded = doc.loadContent(app);
        assertThat(loaded).isNotNull();
        assertThat(doc.isContentSame(loaded)).isTrue();
    }

    @Test
    public void isContentSame_differentAfterContentChange() {
        Document doc = new Document(tempFile);
        Application app = ApplicationProvider.getApplicationContext();
        doc.loadContent(app);
        assertThat(doc.isContentSame("completely different content")).isFalse();
    }

    @Test
    public void isContentSame_differentWhenLengthChanges() {
        Document doc = new Document(tempFile);
        Application app = ApplicationProvider.getApplicationContext();
        String loaded = doc.loadContent(app);
        assertThat(loaded).isNotNull();
        // Add extra characters to change length
        assertThat(doc.isContentSame(loaded + " extra")).isFalse();
    }

    @Test
    public void isContentSame_differentWhenNullProvided() {
        Document doc = new Document(tempFile);
        assertThat(doc.isContentSame(null)).isFalse();
    }

    @Test
    public void isContentSame_sameForEmptyContentAfterEmptyLoad() throws IOException {
        File emptyFile = new File(tempDir, "empty.md");
        writeFile(emptyFile, "");
        Document doc = new Document(emptyFile);
        Application app = ApplicationProvider.getApplicationContext();
        String loaded = doc.loadContent(app);
        assertThat(loaded).isNotNull();
        assertThat(loaded).isEmpty();
        assertThat(doc.isContentSame("")).isTrue();
    }

    // ===== filenameFromContent =====

    @Test
    public void filenameFromContent_extractFirstLineAsTitle() {
        String result = Document.filenameFromContent("# My Note\nSecond line");
        assertThat(result).isEqualTo("# My Note");
    }

    @Test
    public void filenameFromContent_truncateLongFirstLine() {
        String longLine = "This is a very long first line that exceeds the maximum title extraction length";
        String result = Document.filenameFromContent(longLine + "\nSecond line");
        assertThat(result).hasSize(25);
        assertThat(result).startsWith("This is a very long first");
    }

    @Test
    public void filenameFromContent_emptyContentReturnsTimestamp() {
        String result = Document.filenameFromContent("");
        assertThat(result).isNotEmpty();
    }

    @Test
    public void filenameFromContent_nullContentReturnsTimestamp() {
        String result = Document.filenameFromContent(null);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void filenameFromContent_singleLineNoNewline() {
        String result = Document.filenameFromContent("Single Line Title");
        assertThat(result).isEqualTo("Single Line Title");
    }

    // ===== getMaskedContent =====

    @Test
    public void getMaskedContent_maskWordsPreserveUrls() {
        String input = "Visit https://example.com for more info";
        String result = Document.getMaskedContent(input);
        // https:// prefix is preserved, but domain chars are masked to 'a'
        assertThat(result).contains("https://");
        assertThat(result).doesNotContain("Visit");
        assertThat(result).doesNotContain("info");
        // The domain itself gets masked: "example.com" -> "aaaaaaa.aaa"
        assertThat(result).contains("https://aaaaaaa.aaa");
    }

    @Test
    public void getMaskedContent_maskPreservesStructure() {
        String input = "line1\nline2\nline3";
        String result = Document.getMaskedContent(input);
        // Structure (newlines) should be preserved
        String[] lines = result.split("\n");
        assertThat(lines).hasSize(3);
    }

    @Test
    public void getMaskedContent_maskHttpUrls() {
        String input = "See http://test.org/page";
        String result = Document.getMaskedContent(input);
        assertThat(result).contains("https://");
        assertThat(result).doesNotContain("http://test");
    }

    // ===== hasFileChangedSinceLastLoad =====

    @Test
    public void hasFileChangedSinceLastLoad_changedWhenNeverRead() {
        Document doc = new Document(tempFile);
        // Fresh document: _modTime = -1, so should report changed
        assertThat(doc.hasFileChangedSinceLastLoad()).isTrue();
    }

    @Test
    public void hasFileChangedSinceLastLoad_notChangedAfterLoadContent() {
        Document doc = new Document(tempFile);
        Application app = ApplicationProvider.getApplicationContext();
        doc.loadContent(app);
        // After loading, should not report changed (file hasn't been modified)
        assertThat(doc.hasFileChangedSinceLastLoad()).isFalse();
    }

    @Test
    public void hasFileChangedSinceLastLoad_changedAfterResetChangeTracking() {
        Document doc = new Document(tempFile);
        Application app = ApplicationProvider.getApplicationContext();
        doc.loadContent(app);
        assertThat(doc.hasFileChangedSinceLastLoad()).isFalse();

        doc.resetChangeTracking();
        assertThat(doc.hasFileChangedSinceLastLoad()).isTrue();
    }

    @Test
    public void hasFileChangedSinceLastLoad_changedAfterExternalModification() throws Exception {
        Document doc = new Document(tempFile);
        Application app = ApplicationProvider.getApplicationContext();
        doc.loadContent(app);
        assertThat(doc.hasFileChangedSinceLastLoad()).isFalse();

        // Simulate external modification by rewriting the file
        // Need a small delay to ensure different mod time
        Thread.sleep(50);
        writeFile(tempFile, "# Modified externally\n");
        assertThat(doc.hasFileChangedSinceLastLoad()).isTrue();
    }

    // ===== Serialization (config change simulation) =====

    @Test
    public void serialization_survivesSerializationRoundTrip() throws Exception {
        Document doc = new Document(tempFile);
        Application app = ApplicationProvider.getApplicationContext();
        doc.loadContent(app);

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(doc);
        oos.close();

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Document restored = (Document) ois.readObject();
        ois.close();

        // Verify state survived
        assertThat(restored.file).isEqualTo(doc.file);
        assertThat(restored.path).isEqualTo(doc.path);
        assertThat(restored.title).isEqualTo(doc.title);
        assertThat(restored.extension).isEqualTo(doc.extension);
        assertThat(restored.getFormat()).isEqualTo(doc.getFormat());
    }

    @Test
    public void serialization_preservesContentHashAfterRoundTrip() throws Exception {
        Document doc = new Document(tempFile);
        Application app = ApplicationProvider.getApplicationContext();
        String content = doc.loadContent(app);

        // Serialize + deserialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(doc);
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Document restored = (Document) ois.readObject();
        ois.close();

        // Content hash should survive serialization
        assertThat(restored.isContentSame(content)).isTrue();
    }

    // ===== Document basic properties =====

    @Test
    public void document_propertiesSetCorrectly() {
        Document doc = new Document(tempFile);
        assertThat(doc.file.getName()).isEqualTo(tempFile.getName());
        assertThat(doc.extension).isEqualTo(".md");
        assertThat(doc.title).isEqualTo("test");
        // path may be canonical, so compare ends-with
        assertThat(doc.path).endsWith("test.md");
    }

    @Test
    public void document_equalsConsistentForSameFile() {
        Document doc1 = new Document(tempFile);
        Document doc2 = new Document(tempFile);
        assertThat(doc1).isEqualTo(doc2);
    }

    @Test
    public void document_notEqualForDifferentFiles() throws IOException {
        File otherFile = new File(tempDir, "other.txt");
        writeFile(otherFile, "different");
        Document doc1 = new Document(tempFile);
        Document doc2 = new Document(otherFile);
        assertThat(doc1).isNotEqualTo(doc2);
    }

    @Test
    public void document_testCreateParent() {
        Document doc = new Document(tempFile);
        assertThat(doc.testCreateParent()).isTrue();
    }
}
