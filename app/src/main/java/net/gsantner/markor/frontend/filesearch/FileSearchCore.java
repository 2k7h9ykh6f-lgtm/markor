/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.filesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Framework-free core of the file search feature.
 * <p>
 * This class deliberately depends on nothing but the JDK so it can be unit tested on a plain JVM
 * (the Android specifics - {@code AsyncTask}, {@code android.util.Pair}, encryption, snackbar
 * progress - live in {@link FileSearchEngine.QueueSearchFilesTask}, which delegates here).
 * <p>
 * All collaborators that would otherwise pull in Android are injected as small callbacks:
 * {@link CancelSignal}, {@link ProgressListener}, {@link StreamProvider}, {@link TextFileDetector}
 * and {@link SymlinkDetector}.
 */
public final class FileSearchCore {

    public static final int MAX_PREVIEW_LENGTH = 100;

    /** Directories that are always ignored, regardless of caller configuration. */
    public static final List<String> DEFAULT_IGNORED_DIRS =
            Collections.unmodifiableList(Arrays.asList("^\\.git$", "^\\.tmp$", ".*[Tt]humb.*"));

    private FileSearchCore() {
    }

    // -------------------------------------------------------------------------------------------
    // Injected collaborators (kept Android-free)
    // -------------------------------------------------------------------------------------------

    public interface CancelSignal {
        boolean isCancelled();
    }

    public interface ProgressListener {
        void onProgress(int queueSize, int depth, int resultCount, int checkedCount);
    }

    public interface StreamProvider {
        InputStream open(File file) throws IOException;
    }

    public interface TextFileDetector {
        boolean isTextFile(File file);
    }

    public interface SymlinkDetector {
        boolean isSymbolicLink(File file);
    }

    // -------------------------------------------------------------------------------------------
    // Input / output value types
    // -------------------------------------------------------------------------------------------

    public static final class Options {
        public File rootSearchDir;
        public String query;

        public boolean isRegexQuery;
        public boolean isCaseSensitiveQuery;
        public boolean isSearchInContent;
        public boolean isOnlyFirstContentMatch;

        public int maxSearchDepth;
        public List<String> ignoredDirectories;
        public boolean isShowMatchPreview = true;
    }

    /** A single matching line inside a file. */
    public static final class Match {
        public final String preview;
        public final int lineNumber;

        Match(final String preview, final int lineNumber) {
            this.preview = preview;
            this.lineNumber = lineNumber;
        }
    }

    /** A file or directory that matched, with its (possibly empty) content matches. */
    public static final class Result {
        public final File file;
        public final String relPath;
        public final boolean isDirectory;
        public final List<Match> matches;

        Result(final File file, final String relPath, final boolean isDirectory, final List<Match> matches) {
            this.file = file.getAbsoluteFile();
            this.relPath = relPath;
            this.isDirectory = isDirectory;
            this.matches = Collections.unmodifiableList(matches != null ? matches : Collections.<Match>emptyList());
        }
    }

    // -------------------------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------------------------

    /**
     * Compile a query into a {@link Pattern} using the same normalisation the search uses
     * ({@code *} -> {@code .*} unless preceded by a dot, optional lower-casing).
     * Returns {@code null} when {@code isRegex} is false. Throws on an invalid regex so callers
     * can surface the error before launching a search.
     */
    public static Pattern compileQueryPattern(final String query, final boolean isRegex, final boolean caseSensitive) {
        if (!isRegex) {
            return null;
        }
        final String normalized = (caseSensitive ? query : query.toLowerCase()).replaceAll("(?<![.])[*]", ".*");
        return Pattern.compile(normalized);
    }

    /**
     * Walk {@code options.rootSearchDir} and return every matching file / directory, sorted by
     * relative path (case-insensitive). Never returns {@code null}; on an invalid regex query it
     * returns an empty list.
     */
    public static List<Result> search(
            final Options options,
            final CancelSignal cancel,
            final ProgressListener progress,
            final StreamProvider streamProvider,
            final TextFileDetector textDetector,
            final SymlinkDetector symlinkDetector
    ) {
        return new Engine(options, cancel, progress, streamProvider, textDetector, symlinkDetector).run();
    }

    // -------------------------------------------------------------------------------------------
    // Implementation - mirrors the original QueueSearchFilesTask logic 1:1
    // -------------------------------------------------------------------------------------------

    private static final class Node {
        final File dir;
        final int depth;

        Node(final File dir, final int depth) {
            this.dir = dir;
            this.depth = depth;
        }
    }

    private static final class Engine {
        private final Options _o;
        private final CancelSignal _cancel;
        private final ProgressListener _progress;
        private final StreamProvider _stream;
        private final TextFileDetector _textDetector;
        private final SymlinkDetector _symlinkDetector;

        private final boolean _caseSensitive;
        private String _query;
        private Matcher _matcher;
        private boolean _regexCompiled = true;

        private final Set<String> _ignoredExactDirs = new HashSet<>();
        private final List<Matcher> _ignoredRegexDirs = new ArrayList<>();

        private final ArrayDeque<Node> _stack = new ArrayDeque<>();
        private final List<Result> _result = new ArrayList<>();
        private int _checkedFiles = 0;
        private int _trimLength = 0;

        Engine(
                final Options options,
                final CancelSignal cancel,
                final ProgressListener progress,
                final StreamProvider streamProvider,
                final TextFileDetector textDetector,
                final SymlinkDetector symlinkDetector
        ) {
            _o = options;
            _cancel = cancel != null ? cancel : NEVER_CANCELLED;
            _progress = progress;
            _stream = streamProvider != null ? streamProvider : DEFAULT_STREAM_PROVIDER;
            _textDetector = textDetector != null ? textDetector : ALWAYS_TEXT;
            _symlinkDetector = symlinkDetector != null ? symlinkDetector : NEVER_SYMLINK;

            _caseSensitive = _o.isCaseSensitiveQuery;
            _query = _caseSensitive ? _o.query : _o.query.toLowerCase();

            splitRegexExactFiles(_o.ignoredDirectories);
            splitRegexExactFiles(DEFAULT_IGNORED_DIRS);

            if (_o.isRegexQuery) {
                try {
                    _query = _query.replaceAll("(?<![.])[*]", ".*");
                    _matcher = Pattern.compile(_query).matcher("");
                } catch (Exception ex) {
                    _regexCompiled = false;
                }
            }
        }

        List<Result> run() {
            if (_o.rootSearchDir == null || (_o.isRegexQuery && !_regexCompiled)) {
                return _result;
            }

            _stack.add(new Node(_o.rootSearchDir, 0));
            _trimLength = _o.rootSearchDir.getAbsolutePath().length() + 1;

            Node node;
            while ((node = _stack.pollLast()) != null && !_cancel.isCancelled()) {
                final int depth = node.depth;
                final File dir = node.dir;

                if (depth < _o.maxSearchDepth && dir.canRead()) {
                    handleDirectory(dir, depth);
                    if (_progress != null) {
                        _progress.onProgress(_stack.size(), depth, _result.size(), _checkedFiles);
                    }
                }
            }

            Collections.sort(_result, (a, b) -> a.relPath.toLowerCase().compareTo(b.relPath.toLowerCase()));
            return _result;
        }

        private void handleDirectory(final File dir, final int depth) {
            final File[] files = dir.listFiles();
            if (files == null) {
                return;
            }

            _checkedFiles += files.length;

            for (final File file : files) {
                if (_cancel.isCancelled()) {
                    return;
                }

                final String name = _caseSensitive ? file.getName() : file.getName().toLowerCase();

                if (!isIgnored(name)) {
                    final boolean isDir = file.isDirectory();
                    final String relPath = file.getAbsolutePath().substring(_trimLength);

                    final int beforeContentCount = _result.size();
                    if (_o.isSearchInContent && !isDir && file.canRead() && _textDetector.isTextFile(file)) {
                        getContentMatches(file, relPath, _o.isOnlyFirstContentMatch);
                    }

                    // Search name if directory or not already included due to content
                    if (isDir || _result.size() == beforeContentCount) {
                        final boolean nameMatch = _o.isRegexQuery
                                ? _matcher.reset(name).matches()
                                : name.contains(_query);
                        if (nameMatch) {
                            _result.add(new Result(file, relPath, isDir, null));
                        }
                    }

                    // Only check for symbolic link directories
                    if (isDir && depth < _o.maxSearchDepth && !_symlinkDetector.isSymbolicLink(file)) {
                        _stack.addLast(new Node(file, depth + 1));
                    }
                }
            }
        }

        // Match line and return preview string. Preview will be null if no match found
        private String matchLine(final String line) {
            final String preparedLine = _caseSensitive ? line : line.toLowerCase();

            int start = -1, end = -1;
            if (_o.isRegexQuery) {
                if (_matcher.reset(preparedLine).find()) {
                    start = _matcher.start();
                    end = _matcher.end();
                }
            } else {
                start = preparedLine.indexOf(_query);
                if (start >= 0) {
                    end = start + _query.length();
                }
            }

            // Preview is based on original line
            if (start >= 0 && end <= line.length()) {
                if (!_o.isShowMatchPreview) {
                    return "";
                }
                if (line.length() < MAX_PREVIEW_LENGTH) {
                    return line;
                } else {
                    int offset = (MAX_PREVIEW_LENGTH - (end - start)) / 2;
                    int subStart = Math.max(start - offset, 0);
                    int subEnd = Math.min(end + offset, line.length());
                    return String.format("… %s …", line.substring(subStart, subEnd));
                }
            }
            return null;
        }

        private void getContentMatches(final File file, final String relPath, final boolean isFirstMatchOnly) {
            List<Match> contentMatches = null;

            try (final BufferedReader br = new BufferedReader(new InputStreamReader(_stream.open(file)))) {
                int lineNumber = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    if (_cancel.isCancelled()) {
                        break;
                    }
                    final String preview = matchLine(line);
                    if (preview != null) {
                        if (contentMatches == null) {
                            contentMatches = new ArrayList<>();
                        }
                        contentMatches.add(new Match(preview, lineNumber));

                        if (isFirstMatchOnly) {
                            break;
                        }
                    }
                    lineNumber++;
                }
            } catch (Exception ignored) {
            }

            if (contentMatches != null) {
                _result.add(new Result(file, relPath, false, contentMatches));
            }
        }

        private boolean isIgnored(final String dirName) {
            for (final String pattern : _ignoredExactDirs) {
                if (dirName.equals(pattern)) {
                    return true;
                }
            }
            for (final Matcher matcher : _ignoredRegexDirs) {
                if (matcher.reset(dirName).matches()) {
                    return true;
                }
            }
            return false;
        }

        private void splitRegexExactFiles(final List<String> list) {
            if (list == null) {
                return;
            }
            for (String pattern : list) {
                if (pattern.isEmpty()) {
                    continue;
                }
                if (!_caseSensitive) {
                    pattern = pattern.toLowerCase();
                }

                if (pattern.startsWith("\"")) {
                    pattern = pattern.replace("\"", "");
                    if (pattern.isEmpty()) {
                        continue;
                    }
                    _ignoredExactDirs.add(pattern);
                } else {
                    pattern = pattern.replaceAll("(?<![.])[*]", ".*");
                    try {
                        _ignoredRegexDirs.add(Pattern.compile(pattern).matcher(""));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private static final CancelSignal NEVER_CANCELLED = () -> false;
    private static final SymlinkDetector NEVER_SYMLINK = (f) -> false;
    private static final TextFileDetector ALWAYS_TEXT = (f) -> true;
    private static final StreamProvider DEFAULT_STREAM_PROVIDER = FileInputStream::new;
}
