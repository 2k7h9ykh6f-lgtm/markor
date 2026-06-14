/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link ShortcutUtils} label-truncation logic.
 * <p>
 * The {@code createShortLabel} and {@code createLongLabel} methods were made
 * package-private to allow direct testing.
 *
 * <h3>Scenarios covered</h3>
 * <ul>
 *   <li>Short names that fit within the limit</li>
 *   <li>Names that exceed the limit and get truncated with "…"</li>
 *   <li>Boundary cases (exactly at the limit)</li>
 *   <li>Edge cases (empty, single char)</li>
 * </ul>
 */
public class ShortcutUtilsTest {

    // ===================================================================
    //  createShortLabel (limit: 10 chars, truncate to 7 + "...")
    // ===================================================================

    @Test
    public void createShortLabel_shortName_notTruncated() {
        assertThat(ShortcutUtils.createShortLabel("Todo")).isEqualTo("Todo");
    }

    @Test
    public void createShortLabel_exactlyAtLimit_notTruncated() {
        // 10 characters – should NOT be truncated
        assertThat(ShortcutUtils.createShortLabel("1234567890"))
                .isEqualTo("1234567890");
    }

    @Test
    public void createShortLabel_exceedsLimit_truncated() {
        // 11 characters – should be truncated to 7 + "..."
        assertThat(ShortcutUtils.createShortLabel("12345678901"))
                .isEqualTo("1234567...");
    }

    @Test
    public void createShortLabel_longName_truncated() {
        assertThat(ShortcutUtils.createShortLabel("My Very Long Document Name"))
                .isEqualTo("My Very...");
    }

    @Test
    public void createShortLabel_emptyString_returnsEmpty() {
        assertThat(ShortcutUtils.createShortLabel("")).isEmpty();
    }

    @Test
    public void createShortLabel_singleChar_returnsChar() {
        assertThat(ShortcutUtils.createShortLabel("A")).isEqualTo("A");
    }

    @Test
    public void createShortLabel_sevenChars_notTruncated() {
        assertThat(ShortcutUtils.createShortLabel("1234567")).isEqualTo("1234567");
    }

    @Test
    public void createShortLabel_eightChars_notTruncated() {
        // 8 chars < 10 limit → not truncated
        assertThat(ShortcutUtils.createShortLabel("12345678")).isEqualTo("12345678");
    }

    @Test
    public void createShortLabel_truncatedLength_is10() {
        // "1234567..." = 10 characters total
        String result = ShortcutUtils.createShortLabel("Very Long Name Here");
        assertThat(result).hasSize(10);
        assertThat(result).endsWith("...");
    }

    // ===================================================================
    //  createLongLabel (limit: 25 chars, truncate to 22 + "...")
    // ===================================================================

    @Test
    public void createLongLabel_shortName_notTruncated() {
        assertThat(ShortcutUtils.createLongLabel("QuickNote"))
                .isEqualTo("QuickNote");
    }

    @Test
    public void createLongLabel_exactlyAtLimit_notTruncated() {
        // 25 characters – should NOT be truncated
        String label25 = "1234567890123456789012345";
        assertThat(label25).hasSize(25);
        assertThat(ShortcutUtils.createLongLabel(label25)).isEqualTo(label25);
    }

    @Test
    public void createLongLabel_exceedsLimit_truncated() {
        // 26 characters – should be truncated to 22 + "..."
        String label26 = "12345678901234567890123456";
        assertThat(ShortcutUtils.createLongLabel(label26))
                .isEqualTo("1234567890123456789012...");
    }

    @Test
    public void createLongLabel_longName_truncated() {
        String result = ShortcutUtils.createLongLabel(
                "My Very Very Long Document Name That Exceeds Limit");
        assertThat(result).hasSize(25);
        assertThat(result).endsWith("...");
    }

    @Test
    public void createLongLabel_emptyString_returnsEmpty() {
        assertThat(ShortcutUtils.createLongLabel("")).isEmpty();
    }

    @Test
    public void createLongLabel_singleChar_returnsChar() {
        assertThat(ShortcutUtils.createLongLabel("X")).isEqualTo("X");
    }

    @Test
    public void createLongLabel_truncatedLength_is25() {
        String result = ShortcutUtils.createLongLabel(
                "This is an extremely long shortcut label that will definitely be truncated");
        assertThat(result).hasSize(25);
        assertThat(result).endsWith("...");
    }

    // ===================================================================
    //  Shortcut intent scenarios (conceptual)
    // ===================================================================

    @Test
    public void createShortLabel_todoShortcut_typicalName() {
        assertThat(ShortcutUtils.createShortLabel("To-do"))
                .isEqualTo("To-do");
    }

    @Test
    public void createLongLabel_quickNoteShortcut_typicalName() {
        assertThat(ShortcutUtils.createLongLabel("QuickNote"))
                .isEqualTo("QuickNote");
    }

    @Test
    public void createShortLabel_filenameWithExtension_truncated() {
        // A filename like "meeting-notes-2025-01-15.md" would be truncated
        String result = ShortcutUtils.createShortLabel("meeting-notes-2025-01-15.md");
        assertThat(result).endsWith("...");
        assertThat(result).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    public void createLongLabel_filenameWithExtension_truncated() {
        String result = ShortcutUtils.createLongLabel("meeting-notes-2025-01-15.md");
        assertThat(result).endsWith("...");
        assertThat(result).hasSizeLessThanOrEqualTo(25);
    }
}
