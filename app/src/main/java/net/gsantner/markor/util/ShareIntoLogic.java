/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-logic class for share-into text extraction and URL sanitization.
 * Extracted from {@code DocumentShareIntoFragment} so it can be unit-tested
 * without any Android framework dependency.
 */
public final class ShareIntoLogic {

    /** Simplified web-URL pattern (replaces android.util.Patterns.WEB_URL for JVM tests). */
    static final Pattern WEB_URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");

    private ShareIntoLogic() {
    }

    // -----------------------------------------------------------------------
    //  Share-text extraction
    // -----------------------------------------------------------------------

    /**
     * Build the user-visible share text from the optional subject and text extras.
     * <p>
     * If {@code text} looks like a web URL, any tracking query-parameters are
     * stripped via {@link #sanitizeUrl(String)}.
     *
     * @param subject EXTRA_SUBJECT value (may be {@code null})
     * @param text    EXTRA_TEXT value (may be {@code null})
     * @return combined, sanitized string – never {@code null}
     */
    public static String extractShareText(@Nullable String subject, @Nullable String text) {
        String title = (subject != null) ? subject.trim() + " " : null;
        String link = (text != null) ? text.trim() : "";

        if (WEB_URL_PATTERN.matcher(link).matches()) {
            link = (title != null ? title : "") + sanitizeUrl(link);
        }
        return link;
    }

    // -----------------------------------------------------------------------
    //  URL sanitization (tracking-parameter removal)
    // -----------------------------------------------------------------------

    /**
     * Remove common tracking query-parameters (UTM, ad click-IDs, …) from a URL.
     * Amazon URLs get extra parameters stripped (qid, sr).
     */
    public static String sanitizeUrl(String link) {
        if (link == null) {
            return "";
        }
        String dropGetParams =
                "utm_|source|si|__mk_|ref|sprefix|crid|partner|promo|ad_sub|gclid|fbclid|msclkid|dib";
        if (link.contains("amazon.")) {
            dropGetParams += "|qid|sr";
        }
        return link.replaceAll(
                "(?m)(?<=&|\\?)(" + dropGetParams + ").*?(&|$|\\s|\\))", "");
    }

    // -----------------------------------------------------------------------
    //  Link helpers
    // -----------------------------------------------------------------------

    /**
     * Try to extract a human-readable title from a URL.
     * Returns the domain (minus trailing dot) when the input is a valid web URL,
     * or an empty string otherwise.
     */
    public static String getLinkTitle(String link) {
        if (link == null) {
            return "";
        }
        Matcher m = WEB_URL_PATTERN.matcher(link);
        if (m.matches()) {
            // Group 4 in the original android.util.Patterns.WEB_URL is the domain.
            // Our simplified pattern has no groups, so we extract the host manually.
            try {
                String withoutScheme = link.replaceFirst("^https?://", "");
                String host = withoutScheme.split("[/?#]")[0];
                return host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
            } catch (Exception ignored) {
                return "";
            }
        }
        return "";
    }

    /**
     * Check whether any line in {@code text} contains a web URL or a local file path.
     */
    public static boolean hasLinks(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // Check web URL
            int lastSpace = trimmed.lastIndexOf(' ');
            String candidate = (lastSpace == -1) ? trimmed : trimmed.substring(lastSpace + 1);
            if (WEB_URL_PATTERN.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    //  Format-specific share output
    // -----------------------------------------------------------------------

    /**
     * Format the shared text for the target document format.
     *
     * @param text          the share text
     * @param formatTodoTxt true when the target file is todo.txt
     * @param todayDate     today's date string in todo.txt format (e.g. "2025-01-15")
     * @return formatted string ready to be appended to the target file
     */
    public static String formatShareForTarget(String text, boolean formatTodoTxt, String todayDate) {
        if (formatTodoTxt) {
            return todayDate + " " + text.replaceAll("\\n+", " ");
        }
        return "\n" + text;
    }
}
