package net.gsantner.markor.frontend.filesearch;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

@SuppressWarnings("WeakerAccess")

public class FileSearchEngine {
    public static final AtomicBoolean isSearchExecuting = new AtomicBoolean(false);
    public static final AtomicReference<WeakReference<Activity>> activity = new AtomicReference<>();

    public static final int maxQueryHistoryCount = 20;
    public static final LinkedList<String> queryHistory = new LinkedList<>();

    public static void addToHistory(String query) {
        queryHistory.remove(query);

        if (queryHistory.size() == maxQueryHistoryCount) {
            queryHistory.removeLast();
        }
        queryHistory.addFirst(query);
    }

    public static class SearchOptions {
        public File rootSearchDir;
        public String query;

        public boolean isRegexQuery;
        public boolean isCaseSensitiveQuery;
        public boolean isSearchInContent;
        public boolean isOnlyFirstContentMatch;

        public int maxSearchDepth;
        public List<String> ignoredDirectories;
        public boolean isShowMatchPreview = true;
        public char[] password = new char[0];
        public int message = 0;
    }

    public static class FitFile {
        public final File file;
        public final String relPath;
        public final boolean isDirectory;
        public final @NonNull List<Pair<String, Integer>> children;

        public FitFile(
                final File file,
                final String relPath,
                final boolean isDirectory,
                final @Nullable List<Pair<String, Integer>> lineNumbers
        ) {
            this.file = file.getAbsoluteFile();
            this.relPath = relPath;
            this.isDirectory = isDirectory;
            this.children = Collections.unmodifiableList(lineNumbers != null ? lineNumbers : Collections.emptyList());
        }

        @NonNull
        @Override
        public String toString() {
            return (!children.isEmpty() ? String.format("(%s) ", children.size()) : "") + relPath;
        }
    }

    public static FileSearchEngine.QueueSearchFilesTask queueFileSearch(
            @NonNull final Activity activity,
            final SearchOptions config,
            final GsCallback.a1<List<FitFile>> callback
    ) {
        FileSearchEngine.activity.set(new WeakReference<>(activity));
        FileSearchEngine.isSearchExecuting.set(true);
        FileSearchEngine.addToHistory(config.query);
        FileSearchEngine.QueueSearchFilesTask task = new FileSearchEngine.QueueSearchFilesTask(config, callback);
        task.execute();

        return task;
    }

    public static class QueueSearchFilesTask extends AsyncTask<Void, Integer, List<FitFile>> {
        private final SearchOptions _config;
        private final GsCallback.a1<List<FitFile>> _callback;

        private Snackbar _snackBar;
        private boolean _regexCompiled = true;

        public QueueSearchFilesTask(final SearchOptions config, final GsCallback.a1<List<FitFile>> callback) {
            _config = config;
            _callback = callback;

            // Validate the regex up front so we can surface a compile error and cancel before walking
            if (_config.isRegexQuery) {
                try {
                    FileSearchCore.compileQueryPattern(_config.query, true, _config.isCaseSensitiveQuery);
                } catch (Exception ex) {
                    _regexCompiled = false;
                    final WeakReference<Activity> ref = activity.get();
                    final Activity a = ref != null ? ref.get() : null;
                    if (a != null) {
                        final String errorMessage = a.getString(R.string.regex_can_not_be_compiled) + ": " + _config.query;
                        Toast.makeText(a, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (_config.isRegexQuery && !_regexCompiled) {
                cancel(true);
                return;
            }
            bindSnackBar(_config.query);
        }

        public void bindSnackBar(String text) {
            if (!FileSearchEngine.isSearchExecuting.get()) {
                return;
            }

            try {
                final View view = activity.get().get().findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
                _snackBar.addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (FileSearchEngine.isSearchExecuting.get()) {
                                    bindSnackBar(text);
                                }
                            }
                        })
                        .setAction(android.R.string.cancel, (v) -> {
                            _snackBar.dismiss();
                            cancel(true);
                        })
                        .show();
            } catch (Exception ignored) {
                cancel(true);
            }
        }

        @Override
        protected List<FitFile> doInBackground(final Void... ignored) {
            final FileSearchCore.Options options = new FileSearchCore.Options();
            options.rootSearchDir = _config.rootSearchDir;
            options.query = _config.query;
            options.isRegexQuery = _config.isRegexQuery;
            options.isCaseSensitiveQuery = _config.isCaseSensitiveQuery;
            options.isSearchInContent = _config.isSearchInContent;
            options.isOnlyFirstContentMatch = _config.isOnlyFirstContentMatch;
            options.maxSearchDepth = _config.maxSearchDepth;
            options.ignoredDirectories = _config.ignoredDirectories;
            options.isShowMatchPreview = _config.isShowMatchPreview;

            final List<FileSearchCore.Result> coreResults = FileSearchCore.search(
                    options,
                    this::isCancelled,
                    (queueSize, depth, resultCount, checkedCount) -> publishProgress(queueSize, depth, resultCount, checkedCount),
                    this::openStream,
                    GsFileUtils::isTextFile,
                    GsFileUtils::isSymbolicLink
            );

            final List<FitFile> ret = new ArrayList<>(coreResults.size());
            for (final FileSearchCore.Result result : coreResults) {
                List<Pair<String, Integer>> children = null;
                if (!result.matches.isEmpty()) {
                    children = new ArrayList<>(result.matches.size());
                    for (final FileSearchCore.Match match : result.matches) {
                        children.add(Pair.create(match.preview, match.lineNumber));
                    }
                }
                ret.add(new FitFile(result.file, result.relPath, result.isDirectory, children));
            }
            return ret;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (_snackBar != null) {
                // _currentQueueLength, _currentSearchDepth, _result.size(), _countCheckedFiles
                _snackBar.setText("⭕" + values[2] + " || \uD83D\uDD0D" + values[0] + " || ⬇️ " + values[1] + " || \uD83D\uDC41️" + values[3] + "\n" + _config.query);
            }
        }

        @Override
        protected void onPostExecute(List<FitFile> ret) {
            super.onPostExecute(ret);
            FileSearchEngine.isSearchExecuting.set(false);
            if (_snackBar != null) {
                _snackBar.dismiss();
            }
            if (!isCancelled() && _callback != null) {
                try {
                    _callback.callback(ret);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            FileSearchEngine.isSearchExecuting.set(false);
        }

        private InputStream openStream(File file) throws FileNotFoundException {
            if (isEncryptedFile(file)) {
                final byte[] encryptedContext = GsFileUtils.readCloseStreamWithSize(new FileInputStream(file), (int) file.length());
                return new ByteArrayInputStream(JavaPasswordbasedCryption.getDecryptedText(encryptedContext, _config.password.clone()).getBytes(StandardCharsets.UTF_8));
            } else {
                return new FileInputStream(file);
            }
        }
    }

    private static boolean isEncryptedFile(File file) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && file.getName().endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
    }
}
