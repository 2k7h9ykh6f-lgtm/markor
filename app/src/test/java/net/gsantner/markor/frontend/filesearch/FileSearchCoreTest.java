/*#######################################################
 *
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
#########################################################*/
package net.gsantner.markor.frontend.filesearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive JVM unit tests for {@link FileSearchCore}.
 * All tests use temporary directories that are automatically cleaned up.
 */
public class FileSearchCoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FileSearchCore core;
    private File root;

    @Before
    public void setUp() throws Exception {
        core = new FileSearchCore();
        root = tempFolder.getRoot();
    }

    // =====================================================
    // Helper methods
    // =====================================================

    private File createFile(String relativePath, String content) throws Exception {
        return createFile(relativePath, content, StandardCharsets.UTF_8);
    }

    private File createFile(String relativePath, String content, Charset charset) throws Exception {
        File file = new File(root, relativePath);
        file.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), charset)) {
            writer.write(content);
        }
        return file;
    }

    private File createBinaryFile(String relativePath, byte[] data) throws Exception {
        File file = new File(root, relativePath);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
        return file;
    }

    private List<FileSearchCore.Match> search(
            String query, boolean isRegex, boolean isCaseSensitive,
            boolean searchContent, boolean firstMatchOnly,
            int maxDepth, List<String> ignoredDirs, boolean showPreview
    ) {
        return core.search(root, query, isRegex, isCaseSensitive,
                searchContent, firstMatchOnly, maxDepth, ignoredDirs, showPreview);
    }

    private List<FileSearchCore.Match> searchFileName(String query) {
        return search(query, false, false, false, false, 10, null, true);
    }

    private List<FileSearchCore.Match> searchContent(String query) {
        return search(query, false, false, true, false, 10, null, true);
    }

    // =====================================================
    // 1. Filename keyword search
    // =====================================================

    @Test
    public void testFileNameKeywordSearch_findsMatchingFiles() throws Exception {
        createFile("readme.md", "content");
        createFile("notes.txt", "content");
        createFile("data.csv", "content");

        List<FileSearchCore.Match> results = searchFileName("readme");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("readme.md");
        assertThat(results.get(0).children).isEmpty();
    }

    @Test
    public void testFileNameKeywordSearch_matchesMultipleFiles() throws Exception {
        createFile("readme.md", "content");
        createFile("readme_backup.md", "content");
        createFile("notes.txt", "content");

        List<FileSearchCore.Match> results = searchFileName("readme");
        assertThat(results).hasSize(2);
        assertThat(results).extracting(m -> m.relPath)
                .containsExactlyInAnyOrder("readme.md", "readme_backup.md");
    }

    @Test
    public void testFileNameSearch_noMatchReturnsEmpty() throws Exception {
        createFile("hello.md", "content");

        List<FileSearchCore.Match> results = searchFileName("nonexistent");
        assertThat(results).isEmpty();
    }

    // =====================================================
    // 2. Case sensitivity
    // =====================================================

    @Test
    public void testFileNameCaseInsensitive_findsUpperCase() throws Exception {
        createFile("README.md", "content");

        List<FileSearchCore.Match> results = search("readme", false, false, false, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("README.md");
    }

    @Test
    public void testFileNameCaseSensitive_doesNotMatchWrongCase() throws Exception {
        // Use different names (not just different cases of the same name)
        // because Windows filesystem is case-insensitive
        createFile("README_FILE.md", "content");
        createFile("notes.md", "content");

        List<FileSearchCore.Match> results = search("readme", false, true, false, false, 10, null, true);
        // Case-sensitive: "readme" does not match "README_FILE.md"
        assertThat(results).isEmpty();
    }

    @Test
    public void testContentCaseInsensitive_matchesMixedCase() throws Exception {
        createFile("file.md", "Hello WORLD foo");

        List<FileSearchCore.Match> results = search("hello world", false, false, true, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).hasSize(1);
        assertThat(results.get(0).children.get(0).first).isEqualTo("Hello WORLD foo");
    }

    @Test
    public void testContentCaseSensitive_doesNotMatchWrongCase() throws Exception {
        createFile("file.md", "Hello World");

        List<FileSearchCore.Match> results = search("hello", false, true, true, false, 10, null, true);
        assertThat(results).isEmpty();
    }

    @Test
    public void testContentCaseSensitive_matchesExactCase() throws Exception {
        createFile("file.md", "Hello World");

        List<FileSearchCore.Match> results = search("Hello", false, true, true, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children.get(0).first).isEqualTo("Hello World");
    }

    // =====================================================
    // 3. Regex search
    // =====================================================

    @Test
    public void testRegexFileNameSearch_matchesPattern() throws Exception {
        createFile("readme.md", "content");
        createFile("notes.txt", "content");
        createFile("readme_v2.md", "content");

        List<FileSearchCore.Match> results = search("readme.*\\.md", true, false, false, false, 10, null, true);
        assertThat(results).hasSize(2);
        assertThat(results).extracting(m -> m.relPath)
                .containsExactlyInAnyOrder("readme.md", "readme_v2.md");
    }

    @Test
    public void testRegexContentSearch_findsMatchingLines() throws Exception {
        createFile("log.txt", "Error: file not found\nInfo: ok\nError: timeout");

        List<FileSearchCore.Match> results = search("Error:.*", true, false, true, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).hasSize(2);
        assertThat(results.get(0).children.get(0).second).isEqualTo(0);
        assertThat(results.get(0).children.get(1).second).isEqualTo(2);
    }

    @Test
    public void testRegexBareStarConverted_matchesWildcard() throws Exception {
        createFile("readme.md", "content");
        createFile("notes.txt", "content");

        // *.md should be converted to .*\.md for regex matching
        List<FileSearchCore.Match> results = search("*.md", true, false, false, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("readme.md");
    }

    @Test
    public void testRegexContentSearch_complexPattern() throws Exception {
        createFile("code.md", "int x = 42;\nString y = \"hello\";\ndouble z = 3.14;");

        // \w+ = \d+ matches "x = 42" (line 0) and "z = 3" in "3.14" (line 2)
        List<FileSearchCore.Match> results = search("\\w+ = \\d+", true, false, true, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).hasSize(2);
        assertThat(results.get(0).children.get(0).first).isEqualTo("int x = 42;");
        assertThat(results.get(0).children.get(0).second).isEqualTo(0);
        assertThat(results.get(0).children.get(1).second).isEqualTo(2);
    }

    @Test
    public void testCompileRegex_invalidPatternReturnsNull() {
        assertNull(core.compileRegex("[invalid"));
    }

    @Test
    public void testCompileRegex_validPatternReturnsPattern() {
        assertNotNull(core.compileRegex("hello.*world"));
    }

    // =====================================================
    // 4. Content search
    // =====================================================

    @Test
    public void testContentSearch_findsKeywordInFile() throws Exception {
        createFile("doc.md", "This is a test document\nWith multiple lines");

        List<FileSearchCore.Match> results = searchContent("test");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("doc.md");
        assertThat(results.get(0).children).hasSize(1);
        assertThat(results.get(0).children.get(0).first).isEqualTo("This is a test document");
        assertThat(results.get(0).children.get(0).second).isEqualTo(0);
    }

    @Test
    public void testContentSearch_multipleMatchesInOneFile() throws Exception {
        createFile("log.md", "error on line 1\nall good\nerror on line 3");

        List<FileSearchCore.Match> results = searchContent("error");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).hasSize(2);
        assertThat(results.get(0).children.get(0).second).isEqualTo(0);
        assertThat(results.get(0).children.get(0).first).isEqualTo("error on line 1");
        assertThat(results.get(0).children.get(1).second).isEqualTo(2);
        assertThat(results.get(0).children.get(1).first).isEqualTo("error on line 3");
    }

    @Test
    public void testContentSearch_onlyFirstMatch() throws Exception {
        createFile("log.md", "error on line 1\nall good\nerror on line 3");

        List<FileSearchCore.Match> results = search("error", false, false, true, true, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).hasSize(1);
        assertThat(results.get(0).children.get(0).second).isEqualTo(0);
    }

    @Test
    public void testContentSearch_doesNotDuplicateNameAndContentMatch() throws Exception {
        // File name contains "test" and content also contains "test"
        createFile("test_file.md", "test content here");

        List<FileSearchCore.Match> results = search("test", false, false, true, false, 10, null, true);
        // Should appear only once (from content match), not duplicated
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).isNotEmpty();
    }

    @Test
    public void testContentSearch_lineNumbersAreCorrect() throws Exception {
        createFile("data.txt", "line zero\nline one\nline two\nline three");

        List<FileSearchCore.Match> results = searchContent("two");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).hasSize(1);
        assertThat(results.get(0).children.get(0).second).isEqualTo(2); // 0-indexed
    }

    @Test
    public void testContentSearch_skipsBinaryFiles() throws Exception {
        createBinaryFile("image.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A});
        createFile("text.md", "findme");

        List<FileSearchCore.Match> results = searchContent("findme");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("text.md");
    }

    @Test
    public void testContentSearch_emptyFileProducesNoMatches() throws Exception {
        createFile("empty.md", "");

        List<FileSearchCore.Match> results = searchContent("anything");
        assertThat(results).isEmpty();
    }

    // =====================================================
    // 5. Ignored directories
    // =====================================================

    @Test
    public void testDefaultIgnoredDirs_git() throws Exception {
        createFile(".git/config", "git config content");
        createFile("readme.md", "git config content");

        List<FileSearchCore.Match> results = searchContent("git config");
        // Should not search inside .git
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("readme.md");
    }

    @Test
    public void testDefaultIgnoredDirs_tmp() throws Exception {
        createFile(".tmp/cache.md", "temporary data");
        createFile("real.md", "temporary data");

        List<FileSearchCore.Match> results = searchContent("temporary");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("real.md");
    }

    @Test
    public void testDefaultIgnoredDirs_thumbs() throws Exception {
        createFile("Thumbnails/thumb.md", "thumb data");
        createFile("thumbs_dir/thumb.md", "thumb data");
        createFile("real.md", "thumb data");

        List<FileSearchCore.Match> results = searchContent("thumb data");
        // Thumbnails and thumbs_dir match .*[Tt]humb.*
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("real.md");
    }

    @Test
    public void testUserIgnoredDirectories_exactMatch() throws Exception {
        createFile("build/output.md", "build output");
        createFile("src/main.md", "build output");

        List<FileSearchCore.Match> results = search("build output", false, false, true, false, 10,
                Arrays.asList("\"build\""), true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("src" + File.separator + "main.md");
    }

    @Test
    public void testUserIgnoredDirectories_regexMatch() throws Exception {
        createFile("node_modules/pkg.md", "module content");
        createFile("src/app.md", "module content");

        List<FileSearchCore.Match> results = search("module content", false, false, true, false, 10,
                Arrays.asList("node_.*"), true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("src" + File.separator + "app.md");
    }

    @Test
    public void testIsDirIgnored_exactMatch() {
        java.util.Set<String> exact = new java.util.HashSet<>(Arrays.asList("build", "dist"));
        java.util.Set<Matcher> regex = new java.util.HashSet<>();

        assertTrue(core.isDirIgnored("build", exact, regex));
        assertTrue(core.isDirIgnored("dist", exact, regex));
        assertFalse(core.isDirIgnored("src", exact, regex));
    }

    @Test
    public void testIsDirIgnored_regexMatch() {
        java.util.Set<String> exact = new java.util.HashSet<>();
        java.util.Set<Matcher> regex = new java.util.HashSet<>();
        regex.add(Pattern.compile("node_.*").matcher(""));
        regex.add(Pattern.compile("^\\.cache$").matcher(""));

        assertTrue(core.isDirIgnored("node_modules", exact, regex));
        assertTrue(core.isDirIgnored(".cache", exact, regex));
        assertFalse(core.isDirIgnored("src", exact, regex));
    }

    @Test
    public void testSplitIgnorePatterns_exactPatterns() {
        java.util.Set<String> exact = new java.util.HashSet<>();
        java.util.Set<Matcher> regex = new java.util.HashSet<>();

        core.splitIgnorePatterns(Arrays.asList("\"build\"", "\"dist\""), exact, regex, false);

        assertThat(exact).containsExactlyInAnyOrder("build", "dist");
        assertThat(regex).isEmpty();
    }

    @Test
    public void testSplitIgnorePatterns_regexPatterns() {
        java.util.Set<String> exact = new java.util.HashSet<>();
        java.util.Set<Matcher> regex = new java.util.HashSet<>();

        core.splitIgnorePatterns(Arrays.asList("node_.*", "^\\.cache$"), exact, regex, false);

        assertThat(exact).isEmpty();
        assertThat(regex).hasSize(2);
    }

    @Test
    public void testSplitIgnorePatterns_caseInsensitive() {
        java.util.Set<String> exact = new java.util.HashSet<>();
        java.util.Set<Matcher> regex = new java.util.HashSet<>();

        core.splitIgnorePatterns(Arrays.asList("\"BUILD\""), exact, regex, false);

        assertThat(exact).containsExactly("build");
    }

    @Test
    public void testSplitIgnorePatterns_nullListIsHandled() {
        java.util.Set<String> exact = new java.util.HashSet<>();
        java.util.Set<Matcher> regex = new java.util.HashSet<>();

        core.splitIgnorePatterns(null, exact, regex, false);

        assertThat(exact).isEmpty();
        assertThat(regex).isEmpty();
    }

    // =====================================================
    // 6. Max search depth
    // =====================================================

    @Test
    public void testMaxSearchDepth_limitsRecursion() throws Exception {
        createFile("a.txt", "target");
        createFile("sub1/b.txt", "target");
        createFile("sub1/sub2/c.txt", "target");
        createFile("sub1/sub2/sub3/d.txt", "target");

        // Depth 1: only root directory
        List<FileSearchCore.Match> depth1 = search("target", false, false, true, false, 1, null, true);
        assertThat(depth1).hasSize(1);
        assertThat(depth1.get(0).relPath).isEqualTo("a.txt");

        // Depth 2: root + one level deep
        List<FileSearchCore.Match> depth2 = search("target", false, false, true, false, 2, null, true);
        assertThat(depth2).hasSize(2);

        // Depth 3: root + two levels deep
        List<FileSearchCore.Match> depth3 = search("target", false, false, true, false, 3, null, true);
        assertThat(depth3).hasSize(3);

        // Depth 4+: all levels
        List<FileSearchCore.Match> depth4 = search("target", false, false, true, false, 4, null, true);
        assertThat(depth4).hasSize(4);
    }

    @Test
    public void testMaxSearchDepth_filenameSearchAlsoLimited() throws Exception {
        createFile("root_file.md", "");
        createFile("sub/deep_file.md", "");
        createFile("sub/sub2/deeper_file.md", "");

        List<FileSearchCore.Match> depth1 = search("file", false, false, false, false, 1, null, true);
        assertThat(depth1).hasSize(1);
        assertThat(depth1.get(0).relPath).isEqualTo("root_file.md");
    }

    // =====================================================
    // 7. Multi-level directory search
    // =====================================================

    @Test
    public void testMultiLevelDirectory_findsAtAllLevels() throws Exception {
        createFile("root.md", "findme");
        createFile("level1/a.md", "findme");
        createFile("level1/level2/b.md", "findme");
        createFile("other/c.md", "findme");

        List<FileSearchCore.Match> results = searchContent("findme");
        assertThat(results).hasSize(4);
    }

    @Test
    public void testSubdirectorySearch_usesRelativePaths() throws Exception {
        createFile("docs/guide/intro.md", "hello");

        List<FileSearchCore.Match> results = searchContent("hello");
        assertThat(results).hasSize(1);
        String expected = "docs" + File.separator + "guide" + File.separator + "intro.md";
        assertThat(results.get(0).relPath).isEqualTo(expected);
    }

    // =====================================================
    // 8. Preview truncation
    // =====================================================

    @Test
    public void testLongLinePreviewTruncation() throws Exception {
        // Create a line longer than MAX_PREVIEW_LENGTH (100 chars)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) sb.append("aaaaa"); // 100 'a' chars
        sb.append("needle");
        for (int i = 0; i < 20; i++) sb.append("bbbbb"); // 100 'b' chars
        String longLine = sb.toString(); // 206 chars total

        createFile("long.md", longLine);

        List<FileSearchCore.Match> results = searchContent("needle");
        assertThat(results).hasSize(1);
        String preview = results.get(0).children.get(0).first;

        // Preview should be truncated with ellipsis markers
        assertTrue("Preview should start with ellipsis", preview.startsWith("\u2026 "));
        assertTrue("Preview should end with ellipsis", preview.endsWith(" \u2026"));
        assertTrue("Preview should contain the match", preview.contains("needle"));
        assertTrue("Preview should be reasonably short",
                preview.length() <= FileSearchCore.MAX_PREVIEW_LENGTH + 10);
    }

    @Test
    public void testShortLinePreview_noTruncation() throws Exception {
        String shortLine = "short needle here";
        createFile("short.md", shortLine);

        List<FileSearchCore.Match> results = searchContent("needle");
        assertThat(results).hasSize(1);
        String preview = results.get(0).children.get(0).first;
        assertEquals("Short lines should not be truncated", shortLine, preview);
    }

    @Test
    public void testMatchPreviewDisabled_returnsEmptyString() throws Exception {
        createFile("file.md", "some content with keyword");

        // showPreview = false
        List<FileSearchCore.Match> results = search("keyword", false, false, true, false, 10, null, false);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children).hasSize(1);
        assertEquals("", results.get(0).children.get(0).first);
    }

    @Test
    public void testMatchLine_shortLineReturnsFullLine() {
        String result = core.matchLine("hello world", "world", false, false, true, null);
        assertEquals("hello world", result);
    }

    @Test
    public void testMatchLine_noMatchReturnsNull() {
        String result = core.matchLine("hello world", "xyz", false, false, true, null);
        assertNull(result);
    }

    @Test
    public void testMatchLine_previewDisabledReturnsEmpty() {
        String result = core.matchLine("hello world", "world", false, false, false, null);
        assertEquals("", result);
    }

    @Test
    public void testMatchLine_regexMatch() {
        Matcher m = Pattern.compile("\\d+").matcher("");
        String result = core.matchLine("value is 42 today", "42", true, false, true, m);
        assertNotNull(result);
        assertTrue(result.contains("42"));
    }

    @Test
    public void testMatchLine_longLineTruncationCenteredOnMatch() {
        // 200 char line with "needle" at position 100
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append('x');
        sb.append("needle");
        for (int i = 0; i < 94; i++) sb.append('y');
        String line = sb.toString();

        String result = core.matchLine(line, "needle", false, false, true, null);
        assertNotNull(result);
        assertTrue(result.startsWith("\u2026 "));
        assertTrue(result.endsWith(" \u2026"));
        assertTrue(result.contains("needle"));
    }

    @Test
    public void testMatchLine_longLineMatchNearStart() {
        StringBuilder sb = new StringBuilder();
        sb.append("needle");
        for (int i = 0; i < 194; i++) sb.append('x');
        String line = sb.toString(); // 200 chars, "needle" at start

        String result = core.matchLine(line, "needle", false, false, true, null);
        assertNotNull(result);
        assertTrue(result.contains("needle"));
    }

    // =====================================================
    // 9. Sorting
    // =====================================================

    @Test
    public void testResultsSortedByRelPathCaseInsensitive() throws Exception {
        createFile("Zebra.md", "x");
        createFile("apple.md", "x");
        createFile("banana.md", "x");

        List<FileSearchCore.Match> results = searchFileName(".md");
        assertThat(results).hasSize(3);
        // Sorted by lowercase relPath: apple, banana, zebra
        assertThat(results.get(0).relPath).isEqualTo("apple.md");
        assertThat(results.get(1).relPath).isEqualTo("banana.md");
        assertThat(results.get(2).relPath).isEqualTo("Zebra.md");
    }

    @Test
    public void testResultsSortedAcrossDirectories() throws Exception {
        createFile("c_root.md", "");
        createFile("a_dir/file.md", "");
        createFile("b_dir/file.md", "");

        List<FileSearchCore.Match> results = searchFileName("file");
        assertThat(results).hasSize(2);
        String aDir = "a_dir" + File.separator + "file.md";
        String bDir = "b_dir" + File.separator + "file.md";
        assertThat(results.get(0).relPath).isEqualTo(aDir);
        assertThat(results.get(1).relPath).isEqualTo(bDir);
    }

    // =====================================================
    // 10. Different file extensions
    // =====================================================

    @Test
    public void testDifferentExtensions_allTextFilesSearched() throws Exception {
        createFile("doc.md", "findme");
        createFile("note.txt", "findme");
        createFile("page.html", "findme");
        createFile("config.yaml", "findme");

        List<FileSearchCore.Match> results = searchContent("findme");
        assertThat(results).hasSize(4);
    }

    @Test
    public void testNonTextExtensions_skippedForContentSearch() throws Exception {
        createBinaryFile("image.png", new byte[]{1, 2, 3});
        createBinaryFile("data.bin", new byte[]{1, 2, 3});

        List<FileSearchCore.Match> results = searchContent("anything");
        assertThat(results).isEmpty();
    }

    // =====================================================
    // 11. Different encodings
    // =====================================================

    @Test
    public void testUtf8Content_matchesCorrectly() throws Exception {
        createFile("utf8.md", "Héllo wörld café", StandardCharsets.UTF_8);

        List<FileSearchCore.Match> results = search("café", false, true, true, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).children.get(0).first).isEqualTo("Héllo wörld café");
    }

    @Test
    public void testIso8859Content_matchesAsciiChars() throws Exception {
        // Write with ISO-8859-1, but search for pure ASCII keyword
        createFile("latin.md", "hello world with special chars", Charset.forName("ISO-8859-1"));

        List<FileSearchCore.Match> results = search("hello", false, false, true, false, 10, null, true);
        assertThat(results).hasSize(1);
    }

    // =====================================================
    // 12. Edge cases
    // =====================================================

    @Test
    public void testEmptyDirectory_returnsNoResults() throws Exception {
        // root exists but is empty
        List<FileSearchCore.Match> results = searchFileName("anything");
        assertThat(results).isEmpty();
    }

    @Test
    public void testEmptyQuery_matchesAllFiles() throws Exception {
        createFile("a.md", "content");
        createFile("b.txt", "content");

        // Empty string: "".contains("") is true, so all names match
        List<FileSearchCore.Match> results = searchFileName("");
        assertThat(results).hasSize(2);
    }

    @Test
    public void testDirectoryResult_isDirectoryFlag() throws Exception {
        File subDir = tempFolder.newFolder("mydir");
        createFile("mydir/inner.md", "content");

        // Search for "mydir" - should find the directory
        List<FileSearchCore.Match> results = searchFileName("mydir");
        assertThat(results).hasSize(1);
        assertTrue(results.get(0).isDirectory);
    }

    @Test
    public void testFileResult_isDirectoryFlagFalse() throws Exception {
        createFile("test.md", "content");

        List<FileSearchCore.Match> results = searchFileName("test");
        assertThat(results).hasSize(1);
        assertFalse(results.get(0).isDirectory);
    }

    @Test
    public void testFitFileToString_withChildren() {
        List<FileSearchCore.MatchPair> children = Arrays.asList(
                new FileSearchCore.MatchPair("line1", 0),
                new FileSearchCore.MatchPair("line2", 1)
        );
        FileSearchCore.Match match = new FileSearchCore.Match(
                new File(root, "test.md"), "test.md", false, children);

        assertEquals("(2) test.md", match.toString());
    }

    @Test
    public void testFitFileToString_withoutChildren() {
        FileSearchCore.Match match = new FileSearchCore.Match(
                new File(root, "test.md"), "test.md", false, null);

        assertEquals("test.md", match.toString());
    }

    @Test
    public void testUnreadableDirectory_returnsNoResults() throws Exception {
        // Create a directory and make it unreadable
        File unreadableDir = tempFolder.newFolder("unreadable");
        createFile("unreadable/secret.md", "secret data");

        // On some platforms (especially Windows), setReadable(false) may not work
        if (unreadableDir.setReadable(false)) {
            try {
                List<FileSearchCore.Match> results = searchContent("secret");
                // If directory is truly unreadable, listFiles() returns null
                // and no results should be found inside it
                assertThat(results).isEmpty();
            } finally {
                unreadableDir.setReadable(true);
            }
        }
        // If setReadable doesn't work on this platform, the test passes vacuously
    }

    @Test
    public void testBinaryFileOnlyNameMatch() throws Exception {
        createBinaryFile("readme.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        // Content search should skip binary files, but name match should still work
        List<FileSearchCore.Match> results = search("readme", false, false, true, false, 10, null, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).relPath).isEqualTo("readme.png");
        assertThat(results.get(0).children).isEmpty(); // No content match
    }

    @Test
    public void testIsLikelyTextFile_recognizesCommonExtensions() {
        assertTrue(core.isLikelyTextFile(new File("test.md")));
        assertTrue(core.isLikelyTextFile(new File("test.txt")));
        assertTrue(core.isLikelyTextFile(new File("test.html")));
        assertTrue(core.isLikelyTextFile(new File("test.yaml")));
        assertTrue(core.isLikelyTextFile(new File("test.json")));
        assertTrue(core.isLikelyTextFile(new File("test.py")));
        assertTrue(core.isLikelyTextFile(new File("test.java")));
        assertTrue(core.isLikelyTextFile(new File("test.js")));
        assertTrue(core.isLikelyTextFile(new File("test.xml")));
        assertTrue(core.isLikelyTextFile(new File("test.csv")));
    }

    @Test
    public void testIsLikelyTextFile_rejectsBinaryExtensions() {
        assertFalse(core.isLikelyTextFile(new File("image.png")));
        assertFalse(core.isLikelyTextFile(new File("image.jpg")));
        assertFalse(core.isLikelyTextFile(new File("data.class")));
        assertFalse(core.isLikelyTextFile(new File("archive.zip")));
        assertFalse(core.isLikelyTextFile(new File("binary.exe")));
        assertFalse(core.isLikelyTextFile(new File("noext")));
    }

    @Test
    public void testIsLikelyTextFile_handlesJencExtension() {
        assertTrue(core.isLikelyTextFile(new File("test.md.jenc")));
        assertTrue(core.isLikelyTextFile(new File("test.txt.jenc")));
        assertFalse(core.isLikelyTextFile(new File("image.png.jenc")));
    }

    // =====================================================
    // 13. History management
    // =====================================================

    @Test
    public void testAddToHistory_addsToFront() {
        FileSearchEngine.queryHistory.clear();
        FileSearchEngine.addToHistory("first");
        FileSearchEngine.addToHistory("second");

        assertEquals("second", FileSearchEngine.queryHistory.getFirst());
        assertEquals("first", FileSearchEngine.queryHistory.getLast());
    }

    @Test
    public void testAddToHistory_removesDuplicate() {
        FileSearchEngine.queryHistory.clear();
        FileSearchEngine.addToHistory("first");
        FileSearchEngine.addToHistory("second");
        FileSearchEngine.addToHistory("first"); // re-add "first"

        assertThat(FileSearchEngine.queryHistory).hasSize(2);
        assertEquals("first", FileSearchEngine.queryHistory.getFirst());
    }

    @Test
    public void testAddToHistory_respectsMaxSize() {
        FileSearchEngine.queryHistory.clear();
        for (int i = 0; i < 25; i++) {
            FileSearchEngine.addToHistory("query_" + i);
        }

        assertThat(FileSearchEngine.queryHistory).hasSize(FileSearchEngine.maxQueryHistoryCount);
        // Most recent should be at front
        assertEquals("query_24", FileSearchEngine.queryHistory.getFirst());
    }

    // =====================================================
    // 14. Integration-style scenarios
    // =====================================================

    @Test
    public void testRealWorldScenario_notebookSearch() throws Exception {
        // Simulate a real notebook structure
        createFile("journal/2024-01.md", "# January\nMet with Alice about the project");
        createFile("journal/2024-02.md", "# February\nReviewed the project with Bob");
        createFile("projects/alpha.md", "# Alpha Project\nStarted in January");
        createFile("projects/beta.md", "# Beta Project\nNo updates yet");
        createFile(".git/objects/abc", "project data in git");

        List<FileSearchCore.Match> results = searchContent("project");

        // Expected matches (case-insensitive):
        // - journal/2024-01.md: content "project"
        // - journal/2024-02.md: content "project"
        // - projects: directory name "projects" contains "project" (name match)
        // - projects/alpha.md: content "Project"
        // - projects/beta.md: content "Project"
        // NOT .git/objects/abc (ignored by default .git rule)
        assertThat(results).hasSize(5);

        // Verify .git is excluded
        for (FileSearchCore.Match m : results) {
            assertThat(m.relPath).doesNotContain(".git");
        }

        // Verify expected files are present
        List<String> paths = new java.util.ArrayList<>();
        for (FileSearchCore.Match m : results) {
            paths.add(m.relPath);
        }

        String janPath = "journal" + File.separator + "2024-01.md";
        String febPath = "journal" + File.separator + "2024-02.md";
        String alphaPath = "projects" + File.separator + "alpha.md";

        assertThat(paths).contains(janPath, febPath, alphaPath, "projects");
    }

    @Test
    public void testRealWorldScenario_mixedNameAndContentSearch() throws Exception {
        createFile("todo.txt", "Buy groceries\nClean house");
        createFile("notes.txt", "Meeting notes\nTodo: review PR");
        createFile("todo_backup.txt", "Old todo items");

        // Search for "todo" with content search enabled
        List<FileSearchCore.Match> results = search("todo", false, false, true, false, 10, null, true);

        // todo.txt: name match "todo", no content match ("buy groceries", "clean house" have no "todo")
        // notes.txt: no name match, content match on "Todo: review PR" (case insensitive)
        // todo_backup.txt: name match "todo", content match "Old todo items"
        assertThat(results).hasSize(3);

        for (FileSearchCore.Match m : results) {
            if (m.relPath.equals("todo.txt")) {
                // Name match only - no content match
                assertThat(m.children).isEmpty();
            } else if (m.relPath.equals("notes.txt")) {
                // Content match on "Todo: review PR"
                assertThat(m.children).hasSize(1);
                assertThat(m.children.get(0).second).isEqualTo(1);
            } else if (m.relPath.equals("todo_backup.txt")) {
                // Content match on "Old todo items" (also has name match, but content match takes priority)
                assertThat(m.children).hasSize(1);
            }
        }
    }

    @Test
    public void testRealWorldScenario_regexWithIgnoredDirs() throws Exception {
        createFile("src/main.py", "def hello():\n    print('hello')");
        createFile("src/utils.py", "def helper():\n    pass");
        createFile("test/test_main.py", "def test_hello():\n    assert True");
        createFile("__pycache__/cached.pyc", "def hello():\n    print('hello')");
        createFile("venv/lib/site.py", "def hello():\n    print('hello')");

        // Search for function definitions, ignoring __pycache__ and venv
        List<FileSearchCore.Match> results = search(
                "def \\w+\\(", true, false, true, false, 10,
                Arrays.asList("__pycache__", "venv"), true);

        assertThat(results).hasSize(3);
        assertThat(results).noneMatch(m -> m.relPath.contains("__pycache__"));
        assertThat(results).noneMatch(m -> m.relPath.contains("venv"));
    }

    @Test
    public void testDefaultIgnoredDirsConstant_isUnmodifiable() {
        try {
            FileSearchCore.DEFAULT_IGNORED_DIRS.add("test");
            // If we get here, the list is not unmodifiable - fail
            assertTrue("DEFAULT_IGNORED_DIRS should be unmodifiable", false);
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
}
