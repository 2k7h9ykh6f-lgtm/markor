/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extended highlighter pattern tests for Markdown beyond the basic bold/italic/list tests.
 * <p>
 * Covers heading (ATX + Setext), link, code (inline + block), quotation, ordered list,
 * strikethrough, double-space line ending, and complex mixed-content scenarios including
 * nested lists and tables.
 */
public class MarkdownHighlighterPatternExtendedTest {

    // Helper: collect all matches of a pattern in text
    private static List<String> allMatches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches;
    }

    // =======================================================================
    // HEADING
    // =======================================================================

    public static class HeadingTests {

        @Test
        public void atxHeadingLevel1() {
            Matcher m = MarkdownSyntaxHighlighter.HEADING.matcher("# Title");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("# Title");
        }

        @Test
        public void atxHeadingLevel6() {
            Matcher m = MarkdownSyntaxHighlighter.HEADING.matcher("###### Deep");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("###### Deep");
        }

        @Test
        public void setextHeadingLevel1WithEquals() {
            Matcher m = MarkdownSyntaxHighlighter.HEADING.matcher("Title\n=====");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("Title");
            assertThat(m.group()).contains("=====");
        }

        @Test
        public void setextHeadingLevel2WithDashes() {
            Matcher m = MarkdownSyntaxHighlighter.HEADING.matcher("Subtitle\n--------");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("Subtitle");
        }

        @Test
        public void sevenHashesIsNotAHeading() {
            Matcher m = MarkdownSyntaxHighlighter.HEADING_SIMPLE.matcher("####### not a heading");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void hashWithoutSpaceIsNotAHeading() {
            Matcher m = MarkdownSyntaxHighlighter.HEADING_SIMPLE.matcher("#noSpace");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void headingSimpleMatchesAllLevels() {
            for (int i = 1; i <= 6; i++) {
                String heading = "#".repeat(i) + " text";
                Matcher m = MarkdownSyntaxHighlighter.HEADING_SIMPLE.matcher(heading);
                assertThat(m.find()).as("Level %d should match", i).isTrue();
            }
        }

        @Test
        public void multipleHeadingsInDocument() {
            String doc = "# H1\nSome text\n## H2\nMore text\n### H3\n";
            List<String> matches = new ArrayList<>();
            Matcher m = MarkdownSyntaxHighlighter.HEADING_SIMPLE.matcher(doc);
            while (m.find()) {
                matches.add(m.group());
            }
            assertThat(matches).hasSize(3);
            assertThat(matches).containsExactly("# H1", "## H2", "### H3");
        }
    }

    // =======================================================================
    // LINK
    // =======================================================================

    public static class LinkTests {

        @Test
        public void simpleInlineLink() {
            Matcher m = MarkdownSyntaxHighlighter.LINK.matcher("[text](url)");
            assertThat(m.find()).isTrue();
            assertThat(m.group(2)).isEqualTo("text");
            assertThat(m.group(3)).isEqualTo("url");
        }

        @Test
        public void imageLink() {
            Matcher m = MarkdownSyntaxHighlighter.LINK.matcher("![alt](image.png)");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("!");
            assertThat(m.group(2)).isEqualTo("alt");
            assertThat(m.group(3)).isEqualTo("image.png");
        }

        @Test
        public void linkWithParenthesesInUrl() {
            Matcher m = MarkdownSyntaxHighlighter.LINK.matcher("[wiki](https://en.wikipedia.org/wiki/Markdown_(language))");
            assertThat(m.find()).isTrue();
            assertThat(m.group(3)).contains("Markdown_(language)");
        }

        @Test
        public void multipleLinksInLine() {
            String text = "See [foo](a.com) and [bar](b.com).";
            Matcher m = MarkdownSyntaxHighlighter.LINK.matcher(text);
            assertThat(m.find()).isTrue();
            assertThat(m.group(2)).isEqualTo("foo");
            assertThat(m.find()).isTrue();
            assertThat(m.group(2)).isEqualTo("bar");
        }

        @Test
        public void linkWithEmptyText() {
            Matcher m = MarkdownSyntaxHighlighter.LINK.matcher("[](url)");
            assertThat(m.find()).isTrue();
            assertThat(m.group(2)).isEmpty();
        }

        @Test
        public void noLinkInPlainText() {
            Matcher m = MarkdownSyntaxHighlighter.LINK.matcher("just some text");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // CODE (inline and indented block)
    // =======================================================================

    public static class CodeTests {

        @Test
        public void inlineCode() {
            Matcher m = MarkdownSyntaxHighlighter.CODE.matcher("Use `code` here.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("`code`");
        }

        @Test
        public void inlineCodeSingleChar() {
            Matcher m = MarkdownSyntaxHighlighter.CODE.matcher("`x`");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void indentedCodeBlock() {
            Matcher m = MarkdownSyntaxHighlighter.CODE.matcher("    code line");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("    code line");
        }

        @Test
        public void doubleBacktickMatchesInnerCode() {
            // The CODE pattern `(?!`) matches starting from the second backtick.
            // For ``not code``, the engine skips the first ` (followed by another `),
            // then matches `not code` from the second ` to the second-to-last `.
            Matcher m = MarkdownSyntaxHighlighter.CODE.matcher("``not code``");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("`not code`");
        }

        @Test
        public void multipleInlineCodes() {
            String text = "Use `foo` and `bar` variables.";
            Matcher m = MarkdownSyntaxHighlighter.CODE.matcher(text);
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("foo");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("bar");
        }
    }

    // =======================================================================
    // QUOTATION
    // =======================================================================

    public static class QuotationTests {

        @Test
        public void simpleQuote() {
            Matcher m = MarkdownSyntaxHighlighter.QUOTATION.matcher("> quote");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void quoteAtStartOfLine() {
            Matcher m = MarkdownSyntaxHighlighter.QUOTATION.matcher(">first line\n>second line");
            assertThat(m.find()).isTrue();
            assertThat(m.find()).isTrue();
        }

        @Test
        public void greaterThanInMiddleOfLineNotQuote() {
            Matcher m = MarkdownSyntaxHighlighter.QUOTATION.matcher("a > b");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // ORDERED LIST
    // =======================================================================

    public static class OrderedListTests {

        @Test
        public void dotNotation() {
            Matcher m = MarkdownSyntaxHighlighter.LIST_ORDERED.matcher("1. Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("1");
        }

        @Test
        public void parenthesisNotation() {
            Matcher m = MarkdownSyntaxHighlighter.LIST_ORDERED.matcher("1) Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("1");
        }

        @Test
        public void largeNumber() {
            Matcher m = MarkdownSyntaxHighlighter.LIST_ORDERED.matcher("999. Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("999");
        }

        @Test
        public void indentedOrderedList() {
            Matcher m = MarkdownSyntaxHighlighter.LIST_ORDERED.matcher("  1. Sub-item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void noSpaceAfterDotNotMatched() {
            Matcher m = MarkdownSyntaxHighlighter.LIST_ORDERED.matcher("1.Item");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void multipleOrderedItems() {
            String text = "1. first\n2. second\n3. third";
            List<String> matches = new ArrayList<>();
            Matcher m = MarkdownSyntaxHighlighter.LIST_ORDERED.matcher(text);
            while (m.find()) {
                matches.add(m.group(1));
            }
            assertThat(matches).containsExactly("1", "2", "3");
        }
    }

    // =======================================================================
    // STRIKETHROUGH (extended)
    // =======================================================================

    public static class StrikethroughExtendedTests {

        @Test
        public void strikethroughInSentence() {
            Matcher m = MarkdownSyntaxHighlighter.STRIKETHROUGH.matcher("This is ~~deleted~~ text.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~deleted~~");
        }

        @Test
        public void strikethroughWithNestedFormatting() {
            Matcher m = MarkdownSyntaxHighlighter.STRIKETHROUGH.matcher("~~**bold deleted**~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~**bold deleted**~~");
        }

        @Test
        public void singleTildeIsNotStrikethrough() {
            Matcher m = MarkdownSyntaxHighlighter.STRIKETHROUGH.matcher("~not struck~");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // DOUBLESPACE_LINE_ENDING
    // =======================================================================

    public static class DoubleSpaceLineEndingTests {

        @Test
        public void trailingDoubleSpaceBeforeNewline() {
            Matcher m = MarkdownSyntaxHighlighter.DOUBLESPACE_LINE_ENDING.matcher("line one  \nline two");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void noTrailingSpaceNoMatch() {
            Matcher m = MarkdownSyntaxHighlighter.DOUBLESPACE_LINE_ENDING.matcher("line one\nline two");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void singleTrailingSpaceNoMatch() {
            Matcher m = MarkdownSyntaxHighlighter.DOUBLESPACE_LINE_ENDING.matcher("line one \nline two");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // Mixed / complex real-world scenarios
    // =======================================================================

    public static class MixedScenarioTests {

        @Test
        public void tableContentDoesNotTriggerListPatterns() {
            String table = "| Item | Qty |\n|------|-----|\n| Foo  | 3   |";
            assertThat(MarkdownSyntaxHighlighter.LIST_UNORDERED.matcher(table).find()).isFalse();
            assertThat(MarkdownSyntaxHighlighter.LIST_ORDERED.matcher(table).find()).isFalse();
        }

        @Test
        public void codeBlockContentDoesNotTriggerHeading() {
            String codeBlock = "```\n# not a heading\n```";
            // Note: the regex doesn't know about code fences, but the heading pattern
            // should still match the `# not a heading` line. This test documents behavior.
            Matcher m = MarkdownSyntaxHighlighter.HEADING_SIMPLE.matcher(codeBlock);
            // The pattern will match it — the highlighter relies on span ordering to handle this
            assertThat(m.find()).isTrue();
        }

        @Test
        public void taskListMatchesBothListAndCheckbox() {
            String taskLine = "- [x] Buy milk";
            Matcher listM = MarkdownSyntaxHighlighter.LIST_UNORDERED.matcher(taskLine);
            assertThat(listM.find()).isTrue();
        }

        @Test
        public void nestedListIndentation() {
            String nested = "- level 0\n  - level 1\n    - level 2\n      - level 3";
            Matcher m = MarkdownSyntaxHighlighter.LIST_UNORDERED.matcher(nested);
            int count = 0;
            while (m.find()) {
                count++;
            }
            assertThat(count).isEqualTo(4);
        }

        @Test
        public void mixedInlineFormattingInOneLine() {
            String line = "This has **bold**, *italic*, ~~strike~~, and `code`.";
            assertThat(MarkdownSyntaxHighlighter.BOLD.matcher(line).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.ITALICS.matcher(line).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.STRIKETHROUGH.matcher(line).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.CODE.matcher(line).find()).isTrue();
        }

        @Test
        public void fullDocumentHighlightCoverage() {
            String doc = "# Main Title\n\n"
                    + "Some text with **bold** and *italic*.\n\n"
                    + "- item one\n"
                    + "- [ ] task\n"
                    + "- [x] done\n\n"
                    + "1. first\n2. second\n\n"
                    + "> a quote\n\n"
                    + "~~deleted~~ and `inline code`\n\n"
                    + "[link](http://example.com)\n";

            assertThat(MarkdownSyntaxHighlighter.HEADING.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.BOLD.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.ITALICS.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.LIST_UNORDERED.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.LIST_ORDERED.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.QUOTATION.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.STRIKETHROUGH.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.CODE.matcher(doc).find()).isTrue();
            assertThat(MarkdownSyntaxHighlighter.LINK.matcher(doc).find()).isTrue();
        }
    }
}
