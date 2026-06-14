/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.orgmode;

import static net.gsantner.markor.format.FormatTestHelper.apply;
import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.ActionButtonBase;

import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link OrgmodeReplacePatternGenerator}.
 * <p>
 * Exercises heading set/unset (using Org's * prefix syntax),
 * list prefix toggling, and checkbox operations.
 */
public class OrgmodeReplacePatternGeneratorTest {

    // =======================================================================
    // Heading set/unset — setOrUnsetHeadingWithLevel
    // =======================================================================

    public static class HeadingTests {

        @Test
        public void addLevel1HeadingToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(1);
            String result = apply(patterns, "My Title");
            assertThat(result).isEqualTo("* My Title");
        }

        @Test
        public void addLevel2HeadingToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "Section");
            assertThat(result).isEqualTo("** Section");
        }

        @Test
        public void addLevel3HeadingToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(3);
            String result = apply(patterns, "Subsection");
            assertThat(result).isEqualTo("*** Subsection");
        }

        @Test
        public void removeExactLevel1Heading() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(1);
            String result = apply(patterns, "* My Title");
            assertThat(result).isEqualTo("My Title");
        }

        @Test
        public void removeExactLevel2Heading() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "** Section");
            assertThat(result).isEqualTo("Section");
        }

        @Test
        public void replaceLevel1WithLevel3() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(3);
            String result = apply(patterns, "* Title");
            assertThat(result).isEqualTo("*** Title");
        }

        @Test
        public void replaceLevel4WithLevel2() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "**** Deep heading");
            assertThat(result).isEqualTo("** Deep heading");
        }

        @Test
        public void headingOnEmptyLine() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "");
            assertThat(result).isEqualTo("** ");
        }
    }

    // =======================================================================
    // Unordered list prefix
    // =======================================================================

    public static class UnorderedListTests {

        @Test
        public void addDashListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo("- some item");
        }

        @Test
        public void addPlusListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("+");
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo("+ some item");
        }

        @Test
        public void removeExistingDashListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "- existing item");
            assertThat(result).isEqualTo("existing item");
        }

        @Test
        public void convertOrderedListToUnordered() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "1. numbered item");
            assertThat(result).isEqualTo("- numbered item");
        }

        @Test
        public void convertHeadingToUnorderedList() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            String result = apply(patterns, "** Some Heading");
            assertThat(result).isEqualTo("- Some Heading");
        }

        @Test
        public void preservesIndentation() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("-");
            // Use an ordered-list input so it matches PREFIX_ORDERED_LIST (not the target),
            // which triggers the conversion to the unordered prefix while keeping indent.
            String result = apply(patterns, "  1. indented item");
            assertThat(result).isEqualTo("  - indented item");
        }
    }

    // =======================================================================
    // Ordered list prefix
    // =======================================================================

    public static class OrderedListTests {

        @Test
        public void addOrderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo("1. some item");
        }

        @Test
        public void removeExistingOrderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "1. existing item");
            assertThat(result).isEqualTo("existing item");
        }

        @Test
        public void convertUnorderedListToOrdered() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "- bullet item");
            assertThat(result).isEqualTo("1. bullet item");
        }

        @Test
        public void removeHighNumberOrderedList() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix();
            String result = apply(patterns, "42. item");
            assertThat(result).isEqualTo("item");
        }
    }

    // =======================================================================
    // Checkbox toggle
    // =======================================================================

    public static class CheckboxTests {

        @Test
        public void addCheckboxToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "todo item");
            assertThat(result).isEqualTo("- [ ] todo item");
        }

        @Test
        public void toggleUncheckedToChecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "- [ ] todo item");
            assertThat(result).isEqualTo("- [X] todo item");
        }

        @Test
        public void toggleCheckedBackToUnchecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "- [X] todo item");
            assertThat(result).isEqualTo("- [ ] todo item");
        }

        @Test
        public void addCheckboxWithPlusPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("+");
            String result = apply(patterns, "todo item");
            assertThat(result).isEqualTo("+ [ ] todo item");
        }

        @Test
        public void preservesIndentation() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    OrgmodeReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("-");
            String result = apply(patterns, "  indented item");
            assertThat(result).isEqualTo("  - [ ] indented item");
        }
    }

    // =======================================================================
    // Prefix pattern matching verification
    // =======================================================================

    public static class PrefixPatternTests {

        @Test
        public void prefixAtxHeadingMatchesOrgHeadings() {
            for (int i = 1; i <= 5; i++) {
                String heading = "*".repeat(i) + " text";
                assertThat(OrgmodeReplacePatternGenerator.PREFIX_ATX_HEADING.matcher(heading).find())
                        .as("Level %d should match", i).isTrue();
            }
        }

        @Test
        public void prefixAtxHeadingRequiresSpace() {
            // * without space is not a heading in Org-mode
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_ATX_HEADING
                    .matcher("*bold*").find()).isFalse();
        }

        @Test
        public void prefixUnorderedListMatchesDashAndPlus() {
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_UNORDERED_LIST
                    .matcher("- item").find()).isTrue();
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_UNORDERED_LIST
                    .matcher("+ item").find()).isTrue();
        }

        @Test
        public void prefixOrderedListMatchesDotAndParen() {
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_ORDERED_LIST
                    .matcher("1. item").find()).isTrue();
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_ORDERED_LIST
                    .matcher("1) item").find()).isTrue();
        }

        @Test
        public void prefixCheckedListMatchesUpperCaseX() {
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_CHECKED_LIST
                    .matcher("- [X] item").find()).isTrue();
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_CHECKED_LIST
                    .matcher("+ [X] item").find()).isTrue();
        }

        @Test
        public void prefixUncheckedListMatchesSpaceOnly() {
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_UNCHECKED_LIST
                    .matcher("- [ ] item").find()).isTrue();
            assertThat(OrgmodeReplacePatternGenerator.PREFIX_UNCHECKED_LIST
                    .matcher("- [X] item").find()).isFalse();
        }

        @Test
        public void formatPatternsObjectIsNotNull() {
            assertThat(OrgmodeReplacePatternGenerator.formatPatterns).isNotNull();
        }
    }
}
