/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import static net.gsantner.markor.format.FormatTestHelper.apply;
import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.ActionButtonBase;

import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link AsciidocReplacePatternGenerator}.
 * <p>
 * Exercises heading set/unset, indent/deindent, list prefix toggling,
 * and checkbox toggling — covering the key editing operations for AsciiDoc.
 */
public class AsciidocReplacePatternGeneratorTest {

    // =======================================================================
    // Heading set/unset — setOrUnsetHeadingWithLevel
    // =======================================================================

    public static class HeadingTests {

        @Test
        public void addLevel1HeadingToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(1);
            String result = apply(patterns, "My Title");
            assertThat(result).isEqualTo("= My Title");
        }

        @Test
        public void addLevel2HeadingToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "Section");
            assertThat(result).isEqualTo("== Section");
        }

        @Test
        public void addLevel3HeadingToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(3);
            String result = apply(patterns, "Subsection");
            assertThat(result).isEqualTo("=== Subsection");
        }

        @Test
        public void removeExactLevel1Heading() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(1);
            String result = apply(patterns, "= My Title");
            assertThat(result).isEqualTo("My Title");
        }

        @Test
        public void removeExactLevel2Heading() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "== Section");
            assertThat(result).isEqualTo("Section");
        }

        @Test
        public void replaceLevel2WithLevel3() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(3);
            String result = apply(patterns, "== Section");
            assertThat(result).isEqualTo("=== Section");
        }

        @Test
        public void replaceLevel4WithLevel1() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(1);
            String result = apply(patterns, "==== Deep");
            assertThat(result).isEqualTo("= Deep");
        }

        @Test
        public void headingOnEmptyLine() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(2);
            String result = apply(patterns, "");
            assertThat(result).isEqualTo("== ");
        }
    }

    // =======================================================================
    // Indent / Deindent
    // =======================================================================

    public static class IndentTests {

        @Test
        public void indentHeadingFromLevel1ToLevel2() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.indentLevel();
            String result = apply(patterns, "= Title");
            assertThat(result).isEqualTo("== Title");
        }

        @Test
        public void indentHeadingFromLevel2ToLevel3() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.indentLevel();
            String result = apply(patterns, "== Section");
            assertThat(result).isEqualTo("=== Section");
        }

        @Test
        public void indentUnorderedList() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.indentLevel();
            String result = apply(patterns, "* item");
            assertThat(result).isEqualTo("** item");
        }

        @Test
        public void indentOrderedList() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.indentLevel();
            String result = apply(patterns, ". item");
            assertThat(result).isEqualTo(".. item");
        }

        @Test
        public void deindentHeadingFromLevel3ToLevel2() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.deindentLevel();
            String result = apply(patterns, "=== Sub");
            assertThat(result).isEqualTo("== Sub");
        }

        @Test
        public void deindentUnorderedListFromLevel3ToLevel2() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.deindentLevel();
            String result = apply(patterns, "*** item");
            assertThat(result).isEqualTo("** item");
        }

        @Test
        public void deindentOrderedListFromLevel2ToLevel1() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.deindentLevel();
            String result = apply(patterns, ".. item");
            assertThat(result).isEqualTo(". item");
        }

        @Test
        public void deindentLevel1HeadingNoChange() {
            // Level 1 heading (=) should NOT match PREFIX_HEADING_GT1 (needs >= 2 ==)
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.deindentLevel();
            String result = apply(patterns, "= Title");
            assertThat(result).isNull();
        }
    }

    // =======================================================================
    // Unordered list prefix
    // =======================================================================

    public static class UnorderedListTests {

        @Test
        public void addUnorderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("*");
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo("* some item");
        }

        @Test
        public void removeExistingUnorderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("*");
            String result = apply(patterns, "* existing item");
            assertThat(result).isEqualTo("existing item");
        }

        @Test
        public void convertOrderedListToUnordered() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("*");
            String result = apply(patterns, ". numbered item");
            assertThat(result).isEqualTo("* numbered item");
        }

        @Test
        public void convertHeadingToUnorderedList() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix("*");
            String result = apply(patterns, "== Some Heading");
            assertThat(result).isEqualTo("* Some Heading");
        }
    }

    // =======================================================================
    // Ordered list prefix
    // =======================================================================

    public static class OrderedListTests {

        @Test
        public void addOrderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix(".");
            String result = apply(patterns, "some item");
            assertThat(result).isEqualTo(". some item");
        }

        @Test
        public void removeExistingOrderedListPrefix() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix(".");
            String result = apply(patterns, ". existing item");
            assertThat(result).isEqualTo("existing item");
        }

        @Test
        public void convertUnorderedListToOrdered() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix(".");
            String result = apply(patterns, "* bullet item");
            assertThat(result).isEqualTo(". bullet item");
        }
    }

    // =======================================================================
    // Checkbox toggle
    // =======================================================================

    public static class CheckboxTests {

        @Test
        public void addCheckboxToPlainText() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("*");
            String result = apply(patterns, "todo item");
            assertThat(result).isEqualTo("* [ ] todo item");
        }

        @Test
        public void toggleUncheckedToChecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("*");
            String result = apply(patterns, "* [ ] todo item");
            assertThat(result).isEqualTo("* [x] todo item");
        }

        @Test
        public void toggleCheckedBackToUnchecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("*");
            String result = apply(patterns, "* [x] todo item");
            assertThat(result).isEqualTo("* [ ] todo item");
        }

        @Test
        public void toggleUpperCaseXCheckedToUnchecked() {
            List<ActionButtonBase.ReplacePattern> patterns =
                    AsciidocReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix("*");
            String result = apply(patterns, "* [X] todo item");
            assertThat(result).isEqualTo("* [ ] todo item");
        }
    }

    // =======================================================================
    // Prefix pattern matching verification
    // =======================================================================

    public static class PrefixPatternTests {

        @Test
        public void prefixHeadingMatchesAllLevels() {
            for (int i = 1; i <= 6; i++) {
                String heading = "=".repeat(i) + " text";
                assertThat(AsciidocReplacePatternGenerator.PREFIX_HEADING.matcher(heading).find())
                        .as("Level %d should match", i).isTrue();
            }
        }

        @Test
        public void prefixUnorderedListMatchesStarLevels() {
            for (int i = 1; i <= 6; i++) {
                String item = "*".repeat(i) + " text";
                assertThat(AsciidocReplacePatternGenerator.PREFIX_UNORDERED_LIST.matcher(item).find())
                        .as("Star level %d should match", i).isTrue();
            }
        }

        @Test
        public void prefixOrderedListMatchesDotLevels() {
            for (int i = 1; i <= 6; i++) {
                String item = ".".repeat(i) + " text";
                assertThat(AsciidocReplacePatternGenerator.PREFIX_ORDERED_LIST.matcher(item).find())
                        .as("Dot level %d should match", i).isTrue();
            }
        }

        @Test
        public void prefixCheckboxMatchesAllStates() {
            assertThat(AsciidocReplacePatternGenerator.PREFIX_CHECKBOX_LIST
                    .matcher("* [ ] item").find()).isTrue();
            assertThat(AsciidocReplacePatternGenerator.PREFIX_CHECKBOX_LIST
                    .matcher("* [*] item").find()).isTrue();
            assertThat(AsciidocReplacePatternGenerator.PREFIX_CHECKBOX_LIST
                    .matcher("* [x] item").find()).isTrue();
            assertThat(AsciidocReplacePatternGenerator.PREFIX_CHECKBOX_LIST
                    .matcher("* [X] item").find()).isTrue();
        }

        @Test
        public void formatPatternsObjectIsNotNull() {
            assertThat(AsciidocReplacePatternGenerator.formatPatterns).isNotNull();
        }
    }
}
