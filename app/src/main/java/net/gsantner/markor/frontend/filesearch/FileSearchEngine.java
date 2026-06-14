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
import java.util.Arrays;
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

    private static final List<String> defaultIgnoredDirs = Arrays.asList("^\\.git$", "^\\.tmp$", ".*[Tt]humb.*");
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
        private final FileSearchCore _core;
        private final boolean _regexValid;

        private Snackbar _snackBar;
        private final List<FitFile> _result = new ArrayList<>();

        public QueueSearchFilesTask(final SearchOptions config, final GsCallback.a1<List<FitFile>> callback) {
            _config = config;
            _callback = callback;
            _core = new FileSearchCore();

            // Validate regex pattern upfront
            boolean regexOk = true;
            if (_config.isRegexQuery) {
                String query = _config.isCaseSensitiveQuery ? _config.query : _config.query.toLowerCase();
                if (_core.compileRegex(query) == null) {
                    regexOk = false;
                    final Activity a = activity.get().get();
                    if (a != null) {
                        final String errorMessage = a.getString(R.string.regex_can_not_be_compiled) + ": " + _config.query;
                        Toast.makeText(a, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
            _regexValid = regexOk;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (_config.isRegexQuery && !_regexValid) {
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
            if (_config.isRegexQuery && !_regexValid) {
                return _result;
            }

            List<FileSearchCore.Match> matches = _core.search(
                    _config.rootSearchDir,
                    _config.query,
                    _config.isRegexQuery,
                    _config.isCaseSensitiveQuery,
                    _config.isSearchInContent,
                    _config.isOnlyFirstContentMatch,
                    _config.maxSearchDepth,
                    _config.ignoredDirectories,
                    _config.isShowMatchPreview
            );

            for (FileSearchCore.Match m : matches) {
                if (isCancelled()) break;

                List<Pair<String, Integer>> children = null;
                if (!m.children.isEmpty()) {
                    children = new ArrayList<>();
                    for (FileSearchCore.MatchPair mp : m.children) {
                        children.add(new Pair<>(mp.first, mp.second));
                    }
                }
                _result.add(new FitFile(m.file, m.relPath, m.isDirectory, children));
            }

            return _result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (_snackBar != null) {
                _snackBar.setText("\u2b55" + _result.size() + " || \uD83D\uDD0D" + _config.query);
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

        private InputStream getInputStream(File file) throws FileNotFoundException {
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
