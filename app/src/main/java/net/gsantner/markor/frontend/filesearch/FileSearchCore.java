/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Markor contributors
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
#########################################################*/
package net.gsantner.markor.frontend.filesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core file search engine with no Android dependencies.
 * Contains all pure-Java search logic for file name matching,
 * content matching, regex support, directory filtering, and
 * result sorting. Designed to be fully testable in JVM unit tests.
 */
public class FileSearchCore {

    public static final List<String> DEFAULT_IGNORED_DIRS =
            Collections.unmodifiableList(Arrays.asList("^\\.git$", "^\\.tmp$", ".*[Tt]humb.*"));

    public static final int MAX_PREVIEW_LENGTH = 100;

    /**
     * Simple pair class to avoid android.util.Pair dependency.
     */
    public static class MatchPair {
        public final String first;
        public final Integer second;

        public MatchPair(String first, Integer second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * Represents a single search result: a file with optional content match lines.
     */
    public static class Match {
        public final File file;
        public final String relPath;
        public final boolean isDirectory;
        public final List<MatchPair> children;

        public Match(File file, String relPath, boolean isDirectory, List<MatchPair> children) {
            this.file = file.getAbsoluteFile();
            this.relPath = relPath;
            this.isDirectory = isDirectory;
            this.children = children != null
                    ? Collections.unmodifiableList(children)
                    : Collections.<MatchPair>emptyList();
        }

        @Override
        public String toString() {
            return (!children.isEmpty() ? String.format("(%s) ", children.size()) : "") + relPath;
        }
    }

    /**
     * Perform a file search.
     *
     * @param rootSearchDir       Root directory to search from
     * @param query               Search query string or regex pattern
     * @param isRegex             Whether to treat query as regex
     * @param isCaseSensitive     Whether search is case-sensitive
     * @param isSearchInContent   Whether to search inside file contents
     * @param isOnlyFirstMatch    Stop after first content match per file
     * @param maxDepth            Maximum directory recursion depth
     * @param ignoredDirs         List of directory patterns to ignore (regex or "exact")
     * @param isShowMatchPreview  Whether to generate match preview snippets
     * @return Sorted list of search results
     */
    public List<Match> search(
            File rootSearchDir,
            String query,
            boolean isRegex,
            boolean isCaseSensitive,
            boolean isSearchInContent,
            boolean isOnlyFirstMatch,
            int maxDepth,
            List<String> ignoredDirs,
            boolean isShowMatchPreview
    ) {
        // Prepare query
        String preparedQuery = isCaseSensitive ? query : query.toLowerCase();

        // Compile regex pattern if needed
        Matcher queryMatcher = null;
        if (isRegex) {
            Pattern pattern = compileRegex(preparedQuery);
            if (pattern != null) {
                queryMatcher = pattern.matcher("");
            }
        }

        // Set up ignored directories
        Set<String> exactIgnored = new HashSet<>();
        Set<Matcher> regexIgnored = new HashSet<>();
        splitIgnorePatterns(ignoredDirs, exactIgnored, regexIgnored, isCaseSensitive);
        splitIgnorePatterns(DEFAULT_IGNORED_DIRS, exactIgnored, regexIgnored, isCaseSensitive);

        // Execute DFS search
        List<Match> results = new ArrayList<>();
        ArrayDeque<Object[]> stack = new ArrayDeque<>();
        stack.addLast(new Object[]{rootSearchDir, 0});
        int trimLength = rootSearchDir.getAbsolutePath().length() + 1;

        Object[] entry;
        while ((entry = stack.pollLast()) != null) {
            File dir = (File) entry[0];
            int depth = (Integer) entry[1];

            if (depth < maxDepth && dir.canRead()) {
                searchDirectory(dir, trimLength, depth, maxDepth,
                        preparedQuery, isRegex, isCaseSensitive,
                        isSearchInContent, isOnlyFirstMatch, isShowMatchPreview,
                        queryMatcher, exactIgnored, regexIgnored,
                        results, stack);
            }
        }

        // Sort by lowercase relPath
        Collections.sort(results, new Comparator<Match>() {
            @Override
            public int compare(Match a, Match b) {
                return a.relPath.toLowerCase().compareTo(b.relPath.toLowerCase());
            }
        });

        return results;
    }

    /**
     * Process all files in a single directory, adding matches to results
     * and pushing subdirectories to the stack.
     */
    void searchDirectory(
            File dir, int trimSize, int depth, int maxDepth,
            String query, boolean isRegex, boolean isCaseSensitive,
            boolean isSearchInContent, boolean isOnlyFirstMatch,
            boolean isShowMatchPreview,
            Matcher queryMatcher,
            Set<String> exactIgnored, Set<Matcher> regexIgnored,
            List<Match> results, ArrayDeque<Object[]> stack
    ) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String name = isCaseSensitive ? file.getName() : file.getName().toLowerCase();

            if (isDirIgnored(name, exactIgnored, regexIgnored)) {
                continue;
            }

            boolean isDir = file.isDirectory();
            String relPath = file.getAbsolutePath().substring(trimSize);

            int beforeCount = results.size();

            // Content search for readable text files
            if (isSearchInContent && !isDir && file.canRead() && isLikelyTextFile(file)) {
                findContentMatches(file, relPath, query, isRegex, isCaseSensitive,
                        isShowMatchPreview, isOnlyFirstMatch, queryMatcher, results);
            }

            // Name search: always for directories, for files only if no content match found
            if (isDir || results.size() == beforeCount) {
                boolean nameMatch;
                if (isRegex) {
                    nameMatch = queryMatcher != null && queryMatcher.reset(name).matches();
                } else {
                    nameMatch = name.contains(query);
                }

                if (nameMatch) {
                    results.add(new Match(file, relPath, isDir, null));
                }
            }

            // Push directories to stack for further traversal (skip symlinks)
            if (isDir && depth < maxDepth && !isSymbolicLink(file)) {
                stack.addLast(new Object[]{file, depth + 1});
            }
        }
    }

    /**
     * Search file contents line by line, adding matches to results.
     */
    private void findContentMatches(
            File file, String relPath,
            String query, boolean isRegex, boolean isCaseSensitive,
            boolean showPreview, boolean isFirstMatchOnly,
            Matcher queryMatcher,
            List<Match> results
    ) {
        List<MatchPair> contentMatches = null;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)))) {
            int lineNumber = 0;
            String line;
            while ((line = br.readLine()) != null) {
                String preview = matchLine(line, query, isRegex, isCaseSensitive,
                        showPreview, queryMatcher);
                if (preview != null) {
                    if (contentMatches == null) {
                        contentMatches = new ArrayList<>();
                        results.add(new Match(file, relPath, false, contentMatches));
                    }
                    contentMatches.add(new MatchPair(preview, lineNumber));

                    if (isFirstMatchOnly) {
                        break;
                    }
                }
                lineNumber++;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Match a single line against the query. Returns a preview string if matched,
     * or null if no match. The preview is truncated to {@link #MAX_PREVIEW_LENGTH}
     * characters with ellipsis markers when the line is too long.
     *
     * @param line            The original line text
     * @param query           The prepared (case-adjusted) query
     * @param isRegex         Whether to use regex matching
     * @param isCaseSensitive Whether matching is case-sensitive
     * @param showPreview     Whether to generate preview text
     * @param queryMatcher    Pre-compiled regex matcher (nullable)
     * @return Preview string, empty string if preview disabled, or null if no match
     */
    public String matchLine(
            String line, String query, boolean isRegex,
            boolean isCaseSensitive, boolean showPreview, Matcher queryMatcher
    ) {
        String preparedLine = isCaseSensitive ? line : line.toLowerCase();

        int start = -1, end = -1;

        if (isRegex && queryMatcher != null) {
            if (queryMatcher.reset(preparedLine).find()) {
                start = queryMatcher.start();
                end = queryMatcher.end();
            }
        } else {
            start = preparedLine.indexOf(query);
            if (start >= 0) {
                end = start + query.length();
            }
        }

        if (start >= 0 && end <= line.length()) {
            if (!showPreview) {
                return "";
            }
            if (line.length() < MAX_PREVIEW_LENGTH) {
                return line;
            }
            int matchLen = end - start;
            int offset = (MAX_PREVIEW_LENGTH - matchLen) / 2;
            int subStart = Math.max(start - offset, 0);
            int subEnd = Math.min(end + offset, line.length());
            return "\u2026 " + line.substring(subStart, subEnd) + " \u2026";
        }
        return null;
    }

    /**
     * Check if a directory name matches any of the ignore patterns.
     */
    public boolean isDirIgnored(
            String dirName, Set<String> exactPatterns, Set<Matcher> regexPatterns
    ) {
        for (String pattern : exactPatterns) {
            if (dirName.equals(pattern)) {
                return true;
            }
        }
        for (Matcher matcher : regexPatterns) {
            if (matcher.reset(dirName).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse a list of ignore patterns into exact-match and regex-match sets.
     * Patterns starting with '"' are treated as exact matches (quotes stripped).
     * Other patterns are treated as regex (bare '*' is converted to '.*').
     */
    public void splitIgnorePatterns(
            List<String> patterns,
            Set<String> exactSet,
            Set<Matcher> regexSet,
            boolean isCaseSensitive
    ) {
        for (String pattern : (patterns != null ? patterns : Collections.<String>emptyList())) {
            if (pattern.isEmpty()) {
                continue;
            }
            if (!isCaseSensitive) {
                pattern = pattern.toLowerCase();
            }

            if (pattern.startsWith("\"")) {
                pattern = pattern.replace("\"", "");
                if (pattern.isEmpty()) {
                    continue;
                }
                exactSet.add(pattern);
            } else {
                pattern = pattern.replaceAll("(?<![.])[*]", ".*");
                try {
                    regexSet.add(Pattern.compile(pattern).matcher(""));
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Compile a regex pattern, converting bare '*' to '.*'.
     * Returns null if the pattern is invalid.
     */
    public Pattern compileRegex(String pattern) {
        try {
            String converted = pattern.replaceAll("(?<![.])[*]", ".*");
            return Pattern.compile(converted);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a file is likely a text file based on its extension.
     * This is a pure-Java heuristic that does not depend on Android APIs.
     */
    public boolean isLikelyTextFile(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jenc")) {
            name = name.substring(0, name.length() - 5);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String ext = name.substring(dot + 1);
        return TEXT_EXTENSIONS.contains(ext);
    }

    /**
     * Check if a file is a symbolic link using java.nio.file.
     */
    static boolean isSymbolicLink(File file) {
        try {
            return java.nio.file.Files.isSymbolicLink(file.toPath());
        } catch (Exception e) {
            return false;
        }
    }

    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "md", "markdown", "mkd", "mdown", "mkdn", "mdwn", "mdx", "rmd",
            "txt", "text", "taskpaper",
            "html", "htm", "xhtml",
            "xml", "xsl", "xsd", "xslt", "svg",
            "css", "scss", "sass", "less",
            "js", "jsx", "ts", "tsx", "mjs",
            "java", "kt", "kts", "scala", "groovy",
            "c", "h", "cpp", "hpp", "cc", "cxx",
            "py", "pyw",
            "rb", "rs", "go", "swift",
            "sh", "bash", "zsh", "fish", "bat", "cmd", "ps1",
            "sql", "r", "m", "mm",
            "json", "jsonc",
            "yaml", "yml",
            "toml", "ini", "cfg", "conf", "properties",
            "csv", "tsv",
            "log",
            "org",
            "adoc", "asciidoc",
            "tex", "latex", "bib",
            "diff", "patch",
            "makefile", "dockerfile",
            "gradle", "sbt"
    ));
}
