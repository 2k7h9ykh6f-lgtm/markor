/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gsantner.markor.model.Document;

import java.io.File;

/**
 * Centralised intent-to-file resolution logic.
 * <p>
 * Extracted from {@link MarkorContextUtils#getIntentFile(Intent, android.content.Context)}
 * so the resolution chain can be tested in isolation.
 * <p>
 * The three-step fallback is:
 * <ol>
 *     <li>Read {@link Document#EXTRA_FILE} serializable extra</li>
 *     <li>Extract path from the intent data URI (file://, content://, etc.)</li>
 *     <li>Fall back to {@code intent.getData().getPath()}</li>
 * </ol>
 */
public final class IntentFileResolver {

    private IntentFileResolver() {
    }

    // -----------------------------------------------------------------------
    //  Full chain
    // -----------------------------------------------------------------------

    /**
     * Attempt to resolve a {@link File} from the given intent.
     * Returns {@code null} when no file could be determined.
     */
    @Nullable
    public static File resolveFile(@NonNull Intent intent) {
        // Step 1 – direct serializable extra
        File file = fromExtraFile(intent);

        // Step 2 – from data URI (content:// and file:// schemes)
        if (file == null) {
            file = fromDataUri(intent);
        }

        // Step 3 – raw URI path
        if (file == null) {
            file = fromRawPath(intent);
        }
        return file;
    }

    // -----------------------------------------------------------------------
    //  Individual steps (package-private for testing)
    // -----------------------------------------------------------------------

    /**
     * Step 1: Read the {@link Document#EXTRA_FILE} serializable extra.
     */
    @Nullable
    static File fromExtraFile(@NonNull Intent intent) {
        try {
            Object extra = intent.getSerializableExtra(Document.EXTRA_FILE);
            if (extra instanceof File) {
                return (File) extra;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Step 2: Try to resolve a file from the intent data URI.
     * Handles {@code file://} URIs and some common {@code content://} patterns.
     */
    @Nullable
    static File fromDataUri(@NonNull Intent intent) {
        Uri data = intent.getData();
        if (data == null) return null;

        String scheme = data.getScheme();
        if ("file".equals(scheme)) {
            String path = data.getPath();
            if (path != null && !path.isEmpty()) {
                return new File(path);
            }
        }

        if ("content".equals(scheme)) {
            return resolveContentUri(data);
        }
        return null;
    }

    /**
     * Step 3: Fall back to the raw URI path.
     */
    @Nullable
    static File fromRawPath(@NonNull Intent intent) {
        try {
            Uri data = intent.getData();
            if (data != null && data.getPath() != null) {
                return new File(data.getPath());
            }
        } catch (NullPointerException ignored) {
        }
        return null;
    }

    // -----------------------------------------------------------------------
    //  Content-URI helpers
    // -----------------------------------------------------------------------

    /**
     * Try to extract a filesystem path from a {@code content://} URI.
     * Handles common file-manager content providers.
     */
    @Nullable
    static File resolveContentUri(@NonNull Uri uri) {
        String uriStr = uri.toString();
        if (uriStr == null) return null;

        // Strip content:// prefix
        if (!uriStr.startsWith("content://")) return null;
        String remainder = uriStr.substring("content://".length());

        int slashIdx = remainder.indexOf('/');
        if (slashIdx < 0) return null;

        String fileProvider = remainder.substring(0, slashIdx);
        String filePath = remainder.substring(slashIdx + 1);

        // Some file managers don't add leading slash
        if (filePath.startsWith("storage/")) {
            filePath = "/" + filePath;
        }

        // Strip common provider-specific prefixes
        for (String prefix : new String[]{"file", "document", "root_files", "name"}) {
            if (filePath.startsWith(prefix)) {
                filePath = filePath.substring(prefix.length());
            }
        }

        // external/ media/ storage_root/ external-path/ → prepend external storage
        for (String prefix : new String[]{"external/", "media/", "storage_root/", "external-path/"}) {
            if (filePath.startsWith(prefix)) {
                String subPath = filePath.substring(prefix.length());
                File candidate = new File("/storage/emulated/0", subPath);
                if (candidate.exists()) return candidate;
            }
        }

        // Nextcloud / OwnCloud
        for (String fp : new String[]{"org.nextcloud.files", "org.nextcloud.beta.files", "org.owncloud.files"}) {
            if (fileProvider.equals(fp) && filePath.startsWith("external_files/")) {
                return new File("/storage/" + filePath.substring("external_files/".length()).trim());
            }
        }

        // AOSP DocumentsProvider
        if ("com.android.externalstorage.documents".equals(fileProvider)
                && filePath.startsWith("/primary%3A")) {
            return new File(Uri.decode("/storage/emulated/0/" + filePath.substring("/primary%3A".length())));
        }

        // Generic external_files/ prefix
        if (filePath.startsWith("external_files/")) {
            String subPath = filePath.substring("external_files/".length());
            return new File(Uri.decode("/storage/emulated/0/" + subPath));
        }

        // URI-encoded absolute paths
        if (filePath.startsWith("/") || filePath.startsWith("%2F")) {
            return new File(Uri.decode(filePath));
        }

        return null;
    }

    // -----------------------------------------------------------------------
    //  Validation helpers
    // -----------------------------------------------------------------------

    /**
     * Check whether a resolved file actually exists on disk.
     */
    public static boolean isFileAccessible(@Nullable File file) {
        return file != null && file.exists();
    }

    /**
     * Check whether a resolved file is readable.
     */
    public static boolean isFileReadable(@Nullable File file) {
        return file != null && file.exists() && file.canRead();
    }

    /**
     * Check whether a resolved file is writable.
     */
    public static boolean isFileWritable(@Nullable File file) {
        return file != null && file.exists() && file.canWrite();
    }

    /**
     * Determine the intent action category.
     *
     * @return "send", "process_text", "edit", "view", or "unknown"
     */
    @NonNull
    public static String classifyIntentAction(@Nullable String action) {
        if (action == null) return "unknown";
        switch (action) {
            case Intent.ACTION_SEND:
            case Intent.ACTION_SEND_MULTIPLE:
                return "send";
            case "android.intent.action.PROCESS_TEXT":
                return "process_text";
            case Intent.ACTION_EDIT:
                return "edit";
            case Intent.ACTION_VIEW:
                return "view";
            default:
                return "unknown";
        }
    }
}
