/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared fixtures/helpers for syntax-highlighter regression tests.
 * <p>
 * The syntax highlighters of every format ({@code *SyntaxHighlighter}) drive their spans from
 * public {@link Pattern} fields. The actual span objects are Android {@code Spannable} based and
 * therefore cannot be evaluated in plain JVM unit tests, but the set of regions a pattern matches
 * (i.e. the text that <em>would</em> get a span) is pure {@link java.util.regex} and fully
 * assertable. These helpers extract those regions so tests can assert the exact, ordered set of
 * highlighted spans for realistic multi-construct documents instead of merely checking
 * "does not crash".
 */
public final class FormatHighlightTestHelper {

    private FormatHighlightTestHelper() {
    }

    /** All full-match regions ({@code group(0)}) the pattern produces over the input, in order. */
    public static List<String> allMatches(final Pattern pattern, final CharSequence input) {
        final List<String> out = new ArrayList<>();
        final Matcher m = pattern.matcher(input);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    /** Values of the given capture group for every match, in order. Null group values are skipped. */
    public static List<String> groupMatches(final Pattern pattern, final CharSequence input, final int group) {
        final List<String> out = new ArrayList<>();
        final Matcher m = pattern.matcher(input);
        while (m.find()) {
            final String g = m.group(group);
            if (g != null) {
                out.add(g);
            }
        }
        return out;
    }

    /** Number of matches the pattern produces over the input. */
    public static int matchCount(final Pattern pattern, final CharSequence input) {
        int n = 0;
        final Matcher m = pattern.matcher(input);
        while (m.find()) {
            n++;
        }
        return n;
    }
}
