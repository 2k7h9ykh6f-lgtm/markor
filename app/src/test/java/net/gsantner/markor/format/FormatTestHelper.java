/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Shared test fixture for applying ReplacePattern lists to input text.
 * <p>
 * This mirrors the logic in ActionButtonBase.runRegexReplaceAction(), but
 * operates on plain strings without requiring an Android EditText. Tests use
 * {@link #applyPatterns(List, String)} to simulate what happens when a user
 * toggles a format action (heading, list, checkbox, quote, etc.).
 */
public final class FormatTestHelper {

    private FormatTestHelper() {
    }

    /**
     * Applies the first matching ReplacePattern from the list to the input.
     *
     * @param patterns list of replace patterns (in priority order)
     * @param input    the input text line
     * @return the resulting text after replacement, or {@code null} if no pattern matched
     */
    public static String applyPatterns(List<ActionButtonBase.ReplacePattern> patterns, String input) {
        for (ActionButtonBase.ReplacePattern rp : patterns) {
            Matcher matcher = rp.matcher.reset(input);
            if (matcher.find()) {
                return rp.replaceAll ? matcher.replaceAll(rp.replacePattern)
                        : matcher.replaceFirst(rp.replacePattern);
            }
        }
        return null;
    }

    /**
     * Applies the first matching ReplacePattern and asserts the result.
     * Useful for quick one-liner checks in tests.
     *
     * @param patterns list of replace patterns
     * @param input    the input text line
     * @return the resulting text, or null if no pattern matched
     */
    public static String apply(List<ActionButtonBase.ReplacePattern> patterns, String input) {
        return applyPatterns(patterns, input);
    }
}
