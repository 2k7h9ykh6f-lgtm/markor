package net.gsantner.markor;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.frontend.textview.TextViewUtils;

import org.junit.Test;

public class TextViewUtilsTest {

    @Test
    public void findDiffTest() {
        assertThat(TextViewUtils.findDiff("", "", 0, 0)).isEqualTo(new int[]{0, 0, 0});
        assertThat(TextViewUtils.findDiff("abcd", "abcd", 0, 0)).isEqualTo(new int[]{4, 4, 4});
        assertThat(TextViewUtils.findDiff("ab", "abcd", 0, 0)).isEqualTo(new int[]{2, 2, 4});
        assertThat(TextViewUtils.findDiff("abcd", "ab", 0, 0)).isEqualTo(new int[]{2, 4, 2});
        assertThat(TextViewUtils.findDiff("ab1d", "ab2d", 0, 0)).isEqualTo(new int[]{2, 3, 3});
        assertThat(TextViewUtils.findDiff("ab12d", "ab34d", 0, 0)).isEqualTo(new int[]{2, 4, 4});
        assertThat(TextViewUtils.findDiff("ab12d", "ab3d", 0, 0)).isEqualTo(new int[]{2, 4, 3});
        assertThat(TextViewUtils.findDiff("ab12d", "abd", 0, 0)).isEqualTo(new int[]{2, 4, 2});
        assertThat(TextViewUtils.findDiff("abd", "ab12d", 0, 0)).isEqualTo(new int[]{2, 2, 4});
        assertThat(TextViewUtils.findDiff("abcd", "", 0, 0)).isEqualTo(new int[]{0, 4, 0});
        assertThat(TextViewUtils.findDiff("", "abcd", 0, 0)).isEqualTo(new int[]{0, 0, 4});
        assertThat(TextViewUtils.findDiff("ab11d", "ab1d", 0, 0)).isEqualTo(new int[]{3, 4, 3});
        assertThat(TextViewUtils.findDiff("aaaaa", "aaa", 0, 0)).isEqualTo(new int[]{3, 5, 3});
        assertThat(TextViewUtils.findDiff("aaa", "aaaaa", 0, 0)).isEqualTo(new int[]{3, 3, 5});
    }

    @Test
    public void getIndexFromLineOffsetTest() {
        // Empty text -> position 0
        assertThat(TextViewUtils.getIndexFromLineOffset("", 0, 0)).isEqualTo(0);

        // Single line "abc" (no newline); the offset is measured from the END of the line.
        assertThat(TextViewUtils.getIndexFromLineOffset("abc", 0, 0)).isEqualTo(3); // end of line
        assertThat(TextViewUtils.getIndexFromLineOffset("abc", 0, 1)).isEqualTo(2);
        assertThat(TextViewUtils.getIndexFromLineOffset("abc", 0, 3)).isEqualTo(0); // start of line
        assertThat(TextViewUtils.getIndexFromLineOffset("abc", 0, 5)).isEqualTo(0); // clamped; never crosses to the previous line

        // Multi-line text "ab\ncd\nef": newlines at index 2 and 5.
        assertThat(TextViewUtils.getIndexFromLineOffset("ab\ncd\nef", 0, 0)).isEqualTo(2);
        assertThat(TextViewUtils.getIndexFromLineOffset("ab\ncd\nef", 1, 0)).isEqualTo(5); // end of line 1
        assertThat(TextViewUtils.getIndexFromLineOffset("ab\ncd\nef", 1, 2)).isEqualTo(3); // start of line 1
        assertThat(TextViewUtils.getIndexFromLineOffset("ab\ncd\nef", 2, 0)).isEqualTo(8); // end of last line

        // Out-of-range line index clamps to the end of the text (no crash).
        assertThat(TextViewUtils.getIndexFromLineOffset("ab\ncd", 9, 0)).isEqualTo(5);

        // Negative inputs are rejected with -1.
        assertThat(TextViewUtils.getIndexFromLineOffset("abc", -1, 0)).isEqualTo(-1);
        assertThat(TextViewUtils.getIndexFromLineOffset("abc", 0, -1)).isEqualTo(-1);
    }
}

