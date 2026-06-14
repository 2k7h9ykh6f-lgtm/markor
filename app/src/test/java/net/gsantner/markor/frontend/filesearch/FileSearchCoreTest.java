/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.filesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure-JVM unit tests for {@link FileSearchCore}. Each test builds a throw-away directory tree
 * under a fresh temp folder and asserts on result counts, relative paths, match previews / line
 * numbers and ordering. The temp tree is always removed in {@link #tearDown()}.
 */
public class FileSearchCoreTest {

    /**
     * Mirrors the production extension/MIME based text detection ({@code GsFileUtils.isTextFile}):
     * known text extensions are searchable, binary extensions and extension-less files are not.
     */
    private static final FileSearchCore.TextFileDetector TEXT_BY_EXTENSION = (file) -> {
        final String name = file.getName().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".markdown") || name.endsWith(".txt")
                || name.endsWith(".csv") || name.endsWith(".html") || name.endsWith(".xml");
    };

    private static final FileSearchCore.SymlinkDetector REAL_SYMLINK = (file) -> {
        try {
            return Files.isSymbolicLink(file.toPath());
        } catch (Exception ignored) {
            return false;
        }
    };

    private File _root;

    @Before
    public void setUp() throws IOException {
        _root = Files.createTempDirectory("filesearchcore").toFile();
    }

    @After
    public void tearDown() {
        deleteRecursively(_root);
        assertFalse("temp directory must be cleaned up", _root.exists());
    }

    // -------------------------------------------------------------------------------------------
    // Filename matching
    // -------------------------------------------------------------------------------------------

    @Test
    public void filenameMatch_isCaseInsensitiveByDefault() {
        write(_root, "MyNote.md", "irrelevant");
        write(_root, "other.md", "irrelevant");
        write(_root, "Report.txt", "irrelevant");

        final List<FileSearchCore.Result> results = run(opts(_root, "note"));

        assertEquals(1, results.size());
        assertEquals("MyNote.md", results.get(0).relPath);
        assertFalse(results.get(0).isDirectory);
        assertTrue(results.get(0).matches.isEmpty());
    }

    // -------------------------------------------------------------------------------------------
    // Content matching
    // -------------------------------------------------------------------------------------------

    @Test
    public void contentMatch_returnsPreviewsAndLineNumbers() {
        write(_root, "a.md", "intro line\nthe needle is here\nno\nneedle again\n");

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(1, results.size());
        final FileSearchCore.Result r = results.get(0);
        assertEquals("a.md", r.relPath);
        assertEquals(2, r.matches.size());
        assertEquals(1, r.matches.get(0).lineNumber);
        assertEquals("the needle is here", r.matches.get(0).preview);
        assertEquals(3, r.matches.get(1).lineNumber);
        assertEquals("needle again", r.matches.get(1).preview);
    }

    @Test
    public void contentMatch_onlyFirstMatchStopsAfterFirstLine() {
        write(_root, "a.md", "intro line\nthe needle is here\nneedle again\n");

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        o.isOnlyFirstContentMatch = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).matches.size());
        assertEquals(1, results.get(0).matches.get(0).lineNumber);
    }

    @Test
    public void showMatchPreviewFalse_keepsLineNumbersButEmptyPreview() {
        write(_root, "p.md", "needle here");

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        o.isShowMatchPreview = false;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).matches.size());
        assertEquals("", results.get(0).matches.get(0).preview);
        assertEquals(0, results.get(0).matches.get(0).lineNumber);
    }

    // -------------------------------------------------------------------------------------------
    // Regex
    // -------------------------------------------------------------------------------------------

    @Test
    public void regexContentMatch_findsAllOccurrences() {
        write(_root, "log.md", "error code 404\nERROR code 500\nharmless\n");

        final FileSearchCore.Options o = opts(_root, "error code [0-9]+");
        o.isRegexQuery = true;
        o.isSearchInContent = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(1, results.size());
        final FileSearchCore.Result r = results.get(0);
        assertEquals(2, r.matches.size());
        assertEquals(0, r.matches.get(0).lineNumber);
        assertEquals(1, r.matches.get(1).lineNumber);
        // Preview keeps the original casing of the matched line
        assertTrue(r.matches.get(0).preview.toLowerCase().contains("error code"));
        assertTrue(r.matches.get(1).preview.toLowerCase().contains("error code"));
    }

    @Test
    public void regexFilename_starIsTranslatedToDotStar() {
        write(_root, "draft1.md", "x");
        write(_root, "draft22.md", "x");
        write(_root, "final.md", "x");

        final FileSearchCore.Options o = opts(_root, "draft*");
        o.isRegexQuery = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(relPathSet("draft1.md", "draft22.md"), relPathSet(results));
    }

    // -------------------------------------------------------------------------------------------
    // Case sensitivity
    // -------------------------------------------------------------------------------------------

    @Test
    public void caseSensitiveContent_matchesExactCaseOnly() {
        write(_root, "Alpha.md", "Needle\nneedle\n");
        write(_root, "beta.md", "needle\n");

        final FileSearchCore.Options o = opts(_root, "Needle");
        o.isSearchInContent = true;
        o.isCaseSensitiveQuery = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(1, results.size());
        final FileSearchCore.Result r = results.get(0);
        assertEquals("Alpha.md", r.relPath);
        assertEquals(1, r.matches.size());
        assertEquals(0, r.matches.get(0).lineNumber);
        assertEquals("Needle", r.matches.get(0).preview);
    }

    // -------------------------------------------------------------------------------------------
    // Ignored directories
    // -------------------------------------------------------------------------------------------

    @Test
    public void ignoredDirectories_defaultAndCustomAreSkipped() {
        write(_root, "keep.md", "needle");
        write(dir(_root, ".git"), "inside.md", "needle");          // default ignore ^\.git$
        write(dir(_root, "thumbnails"), "inside.md", "needle");    // default ignore .*[Tt]humb.*
        write(dir(_root, "node_modules"), "inside.md", "needle");  // custom ignore
        write(dir(_root, "build"), "inside.md", "needle");         // NOT ignored (control)

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        o.ignoredDirectories = new ArrayList<>(Collections.singletonList("node_modules"));
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(relPathSet("keep.md", "build" + File.separator + "inside.md"), relPathSet(results));
    }

    @Test
    public void quotedIgnoreDirectory_isMatchedExactly() {
        write(_root, "keep.md", "needle");
        write(dir(_root, "weird.dir"), "inside.md", "needle");

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        o.ignoredDirectories = new ArrayList<>(Collections.singletonList("\"weird.dir\""));
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(relPathSet("keep.md"), relPathSet(results));
    }

    // -------------------------------------------------------------------------------------------
    // Unreadable / non-directory / missing roots
    // -------------------------------------------------------------------------------------------

    @Test
    public void nonDirectoryRoot_returnsEmpty() {
        final File asFile = write(_root, "plain.md", "needle");
        final List<FileSearchCore.Result> results = run(opts(asFile, "needle"));
        assertTrue(results.isEmpty());
    }

    @Test
    public void nonExistentRoot_returnsEmpty() {
        final File missing = new File(_root, "does-not-exist");
        final List<FileSearchCore.Result> results = run(opts(missing, "needle"));
        assertTrue(results.isEmpty());
    }

    @Test
    public void unreadableDirectory_isSkipped() {
        write(_root, "keep.md", "needle");
        final File secret = dir(_root, "secret");
        write(secret, "inside.md", "needle");

        final boolean toggled = secret.setReadable(false);
        // Some filesystems (e.g. Windows) ignore setReadable; only assert when it took effect.
        assumeTrue("OS does not honor setReadable(false)", toggled && !secret.canRead());
        try {
            final FileSearchCore.Options o = opts(_root, "needle");
            o.isSearchInContent = true;
            final List<FileSearchCore.Result> results = run(o);
            assertEquals(relPathSet("keep.md"), relPathSet(results));
        } finally {
            secret.setReadable(true);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Binary files
    // -------------------------------------------------------------------------------------------

    @Test
    public void binaryExtension_contentIsSkippedButNameStillMatches() {
        write(_root, "doc.md", "needle inside md");
        write(_root, "image.png", "needle inside png");  // text body, but non-text extension
        write(_root, "needle.png", "nothing here");      // matched by name only

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(relPathSet("doc.md", "needle.png"), relPathSet(results));
        assertEquals(1, find(results, "doc.md").matches.size());
        assertTrue("binary file matched by name has no content matches",
                find(results, "needle.png").matches.isEmpty());
    }

    // -------------------------------------------------------------------------------------------
    // Preview truncation for long lines
    // -------------------------------------------------------------------------------------------

    @Test
    public void longLine_previewIsTruncatedAndCentered() {
        final String line = repeat('a', 200) + "needle" + repeat('b', 200);
        write(_root, "long.md", line);

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(1, results.size());
        final String preview = results.get(0).matches.get(0).preview;
        assertTrue(preview.startsWith("… "));
        assertTrue(preview.endsWith(" …"));
        assertTrue(preview.contains("needle"));
        // window = match (6) + 2*47 padding = 100 chars, wrapped in "… " + " …"
        assertEquals(104, preview.length());
        assertTrue(preview.length() < line.length());
    }

    @Test
    public void shortLine_previewIsWholeLine() {
        write(_root, "short.md", "tiny needle here");

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        final List<FileSearchCore.Result> results = run(o);

        assertEquals(1, results.size());
        assertEquals("tiny needle here", results.get(0).matches.get(0).preview);
    }

    // -------------------------------------------------------------------------------------------
    // Search depth
    // -------------------------------------------------------------------------------------------

    @Test
    public void maxSearchDepth_limitsTraversal() {
        write(_root, "top.md", "needle");
        final File d1 = dir(_root, "d1");
        write(d1, "mid.md", "needle");
        write(dir(d1, "d2"), "deep.md", "needle");

        assertEquals(relPathSet("top.md"),
                relPathSet(run(contentOpts(_root, "needle", 1))));

        assertEquals(relPathSet("top.md", "d1" + File.separator + "mid.md"),
                relPathSet(run(contentOpts(_root, "needle", 2))));

        assertEquals(relPathSet("top.md", "d1" + File.separator + "mid.md",
                        "d1" + File.separator + "d2" + File.separator + "deep.md"),
                relPathSet(run(contentOpts(_root, "needle", 10))));
    }

    // -------------------------------------------------------------------------------------------
    // Sorting
    // -------------------------------------------------------------------------------------------

    @Test
    public void results_areSortedByRelPathCaseInsensitive() {
        write(_root, "banana.md", "needle");
        write(_root, "Apple.md", "needle");
        write(_root, "cherry.md", "needle");
        final File sub = dir(_root, "sub");
        write(sub, "ZZZ.md", "needle");
        write(sub, "aaa.md", "needle");

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        final List<String> ordered = relPaths(run(o));

        assertEquals(Arrays.asList(
                "Apple.md",
                "banana.md",
                "cherry.md",
                "sub" + File.separator + "aaa.md",
                "sub" + File.separator + "ZZZ.md"
        ), ordered);
    }

    // -------------------------------------------------------------------------------------------
    // Cancellation
    // -------------------------------------------------------------------------------------------

    @Test
    public void cancelSignalStopsSearchImmediately() {
        write(_root, "keep.md", "needle");

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        final List<FileSearchCore.Result> results = FileSearchCore.search(
                o, () -> true, null, null, TEXT_BY_EXTENSION, REAL_SYMLINK);

        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------------------------------------------------
    // Symbolic links
    // -------------------------------------------------------------------------------------------

    @Test
    public void symbolicLinkDirectory_isNotTraversed() {
        final File realDir = dir(_root, "realdir");
        write(realDir, "inside.md", "needle");

        final File link = new File(_root, "link");
        try {
            Files.createSymbolicLink(link.toPath(), realDir.toPath());
        } catch (Exception unsupported) {
            assumeTrue("symlinks unsupported on this platform", false);
        }

        final FileSearchCore.Options o = opts(_root, "needle");
        o.isSearchInContent = true;
        final List<String> rels = relPaths(run(o));

        assertEquals(Collections.singletonList("realdir" + File.separator + "inside.md"), rels);
    }

    // -------------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------------

    private List<FileSearchCore.Result> run(final FileSearchCore.Options options) {
        return FileSearchCore.search(options, null, null, null, TEXT_BY_EXTENSION, REAL_SYMLINK);
    }

    private static FileSearchCore.Options opts(final File root, final String query) {
        final FileSearchCore.Options o = new FileSearchCore.Options();
        o.rootSearchDir = root;
        o.query = query;
        o.maxSearchDepth = 25;
        o.ignoredDirectories = new ArrayList<>();
        return o;
    }

    private static FileSearchCore.Options contentOpts(final File root, final String query, final int maxDepth) {
        final FileSearchCore.Options o = opts(root, query);
        o.isSearchInContent = true;
        o.maxSearchDepth = maxDepth;
        return o;
    }

    private static File dir(final File parent, final String name) {
        final File d = new File(parent, name);
        assertTrue("could not create dir " + d, d.mkdirs() || d.isDirectory());
        return d;
    }

    private static File write(final File parent, final String name, final String content) {
        final File f = new File(parent, name);
        try {
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return f;
    }

    private static List<String> relPaths(final List<FileSearchCore.Result> results) {
        final List<String> out = new ArrayList<>(results.size());
        for (final FileSearchCore.Result r : results) {
            out.add(r.relPath);
        }
        return out;
    }

    private static java.util.Set<String> relPathSet(final List<FileSearchCore.Result> results) {
        return new java.util.HashSet<>(relPaths(results));
    }

    private static java.util.Set<String> relPathSet(final String... relPaths) {
        return new java.util.HashSet<>(Arrays.asList(relPaths));
    }

    private static FileSearchCore.Result find(final List<FileSearchCore.Result> results, final String relPath) {
        for (final FileSearchCore.Result r : results) {
            if (r.relPath.equals(relPath)) {
                return r;
            }
        }
        throw new AssertionError("no result with relPath " + relPath + " in " + relPaths(results));
    }

    private static String repeat(final char c, final int count) {
        final char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static void deleteRecursively(final File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            file.setReadable(true);
            final File[] children = file.listFiles();
            if (children != null) {
                for (final File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        // Best effort; ensure deletable then remove
        file.setWritable(true);
        file.delete();
    }
}
