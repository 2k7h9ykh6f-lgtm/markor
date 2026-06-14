/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import static net.gsantner.markor.format.FormatTestHelper.apply;
import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.ActionButtonBase;

import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link MarkdownReplacePatternGenerator}.
 * <p>
 * Exercises heading toggle, unordered/ordered list prefix, checkbox toggle,
 * and quote toggle — covering the real editing scenarios where a user
 * clicks a toolbar button and expects a specific text transformation.
 */
public class MarkdownReplacePatternGeneratorTest {

    // =======================================================================
    // Heading toggle — setOrUnsetHeadingWithLevel
    // =======================================================================

    public static class HeadingTests {

        @Test
        public void addH1ToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(1);
            assertThat(apply(patterns, "Hello World")).isEqualTo("# Hello World");
        }

        @Test
        public void addH3ToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(3);
            assertThat(apply(patterns, "Section")).isEqualTo("### Section");
        }

        @Test
        public void removeExactH2Heading() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            assertThat(apply(patterns, "## My Heading")).isEqualTo("My Heading");
        }

        @Test
        public void replaceH1WithH3() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(3);
            String result = apply(patterns, "# Title");
            assertThat(result).isEqualTo("### Title");
        }

        @Test
        public void replaceH4WithH2() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "#### Deep heading");
            assertThat(result).isEqualTo("## Deep heading");
        }

        @Test
        public void headingOnEmptyLine() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "");
            assertThat(result).isEqualTo("## ");
        }

        @Test
        public void headingPreservesLeadingWhitespace() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(1);
            // Existing heading with 2 spaces indent
            String result = apply(patterns, "  ## Indented");
            assertThat(result).contains("# ");
        }
    }

    // =======================================================================
    // Unordered list prefix — replaceWithUnorderedListPrefixOrRemovePrefix
    // =======================================================================

    public static class UnorderedListTests {

        @Test
        public void addDashListPrefixToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo("- some item");
        }

        @Test
        public void addStarListPrefixToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("*");
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo("* some item");
        }

        @Test
        public void removeExistingUnorderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "- existing item");
            assertThat(result).isEqualTo("existing item");
        }

        @Test
        public void convertOrderedListToUnordered() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "1. numbered item");
            assertThat(result).isEqualTo("- numbered item");
        }

        @Test
        public void preservesIndentation() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "  1. indented item");
            assertThat(result).isEqualTo("  - indented item");
        }
    }

    // =======================================================================
    // Ordered list prefix — replaceWithOrderedListPrefixOrRemovePrefix
    // =======================================================================

    public static class OrderedListTests {

        @Test
        public void addOrderedListPrefixToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo("1. some item");
        }

        @Test
        public void removeExistingOrderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "1. existing item");
            assertThat(result).isEqualTo("existing item");
        }

        @Test
        public void convertUnorderedListToOrdered() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "- bullet item");
            assertThat(result).isEqualTo("1. bullet item");
        }

        @Test
        public void convertHighNumberOrderedListToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "42. high number item");
            assertThat(result).isEqualTo("high number item");
        }
    }

    // =======================================================================
    // Checkbox toggle — toggleToCheckedOrUncheckedListPrefix
    // =======================================================================

    public static class CheckboxTests {

        @Test
        public void togglePlainTextToUnchecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "todo item");
            assertThat(result).isEqualTo("- [ ] todo item");
        }

        @Test
        public void toggleUncheckedToChecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "- [ ] todo item");
            assertThat(result).isEqualTo("- [x] todo item");
        }

        @Test
        public void toggleCheckedBackToUnchecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "- [x] todo item");
            assertThat(result).isEqualTo("- [ ] todo item");
        }

        @Test
        public void toggleUpperCaseCheckedToUnchecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "- [X] todo item");
            assertThat(result).isEqualTo("- [ ] todo item");
        }

        @Test
        public void addCheckboxWithStarPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("*");
            String result = apply(patterns, "todo item");
            assertThat(result).isEqualTo("* [ ] todo item");
        }

        @Test
        public void addCheckboxWithPlusPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("+");
            String result = apply(patterns, "todo item");
            assertThat(result).isEqualTo("+ [ ] todo item");
        }

        @Test
        public void preservesIndentationWhenAddingCheckbox() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "  indented item");
            assertThat(result).isEqualTo("  - [ ] indented item");
        }
    }

    // =======================================================================
    // Quote toggle — toggleQuote
    // =======================================================================

    public static class QuoteTests {

        @Test
        public void addQuotePrefixToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleQuote();
            String result = apply(patterns, "some text");
            assertThat(result).isEqualTo("> some text");
        }

        @Test
        public void removeExistingQuotePrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleQuote();
            String result = apply(patterns, "> quoted text");
            assertThat(result).isEqualTo("quoted text");
        }

        @Test
        public void convertListToQuote() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    MarkdownReplacePatternGenerator.toggleQuote();
            // PREFIX_UNORDERED_LIST matches "- " (group 1 = ""), replacement is ">$1 " = "> "
            // The list prefix is consumed, so "- list item" becomes "> list item"
            String result = apply(patterns, "- list item");
            assertThat(result).isEqualTo("> list item");
        }
    }

    // =======================================================================
    // Prefix pattern matching — verify individual patterns
    // =======================================================================

    public static class PrefixPatternTests {

        @Test
        public void prefixOrderedListMatchesVariousNumbers() {
            assertThat(MarkdownReplacePatternGenerator.PREFIX_ORDERED_LIST
                    .matcher("1. item").find()).isTrue();
            assertThat(MarkdownReplacePatternGenerator.PREFIX_ORDERED_LIST
                    .matcher("42. item").find()).isTrue();
            assertThat(MarkdownReplacePatternGenerator.PREFIX_ORDERED_LIST
                    .matcher("1) item").find()).isTrue();
        }

        @Test
        public void prefixAtxHeadingMatchesAllLevels() {
            for (int i = 1; i <= 6; i++) {
                String heading = "#".repeat(i) + " text";
                assertThat(MarkdownReplacePatternGenerator.PREFIX_ATX_HEADING
                        .matcher(heading).find())
                        .as("Level %d heading should match", i)
                        .isTrue();
            }
        }

        @Test
        public void prefixAtxHeadingRejectsLevel7() {
            assertThat(MarkdownReplacePatternGenerator.PREFIX_ATX_HEADING
                    .matcher("####### too many").find()).isFalse();
        }

        @Test
        public void prefixQuoteMatchesGreaterThan() {
            assertThat(MarkdownReplacePatternGenerator.PREFIX_QUOTE
                    .matcher("> quote").find()).isTrue();
        }

        @Test
        public void prefixCheckedListMatchesLowerAndUpperCase() {
            assertThat(MarkdownReplacePatternGenerator.PREFIX_CHECKED_LIST
                    .matcher("- [x] item").find()).isTrue();
            assertThat(MarkdownReplacePatternGenerator.PREFIX_CHECKED_LIST
                    .matcher("- [X] item").find()).isTrue();
            assertThat(MarkdownReplacePatternGenerator.PREFIX_CHECKED_LIST
                    .matcher("* [x] item").find()).isTrue();
        }

        @Test
        public void prefixUncheckedListMatchesSpaceOnly() {
            assertThat(MarkdownReplacePatternGenerator.PREFIX_UNCHECKED_LIST
                    .matcher("- [ ] item").find()).isTrue();
            assertThat(MarkdownReplacePatternGenerator.PREFIX_UNCHECKED_LIST
                    .matcher("- [x] item").find()).isFalse();
        }

        @Test
        public void prefixUnorderedListMatchesDashStarPlus() {
            assertThat(MarkdownReplacePatternGenerator.PREFIX_UNORDERED_LIST
                    .matcher("- item").find()).isTrue();
            assertThat(MarkdownReplacePatternGenerator.PREFIX_UNORDERED_LIST
                    .matcher("* item").find()).isTrue();
            assertThat(MarkdownReplacePatternGenerator.PREFIX_UNORDERED_LIST
                    .matcher("+ item").find()).isTrue();
        }

        @Test
        public void formatPatternsObjectIsNotNull() {
            assertThat(MarkdownReplacePatternGenerator.formatPatterns).isNotNull();
        }
    }
}
