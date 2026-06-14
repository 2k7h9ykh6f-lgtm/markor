/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.orgmode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for all Org-mode syntax highlighter regex patterns.
 * <p>
 * Org-mode uses a shared {@code COMMON_EMPHASIS_PATTERN} template for
 * inline emphasis (bold, italic, strikethrough, underline, code), plus
 * distinct patterns for headings, blocks, preamble, comments, lists, and links.
 */
public class OrgmodeSyntaxHighlighterTest {

    private static List<String> allMatches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches;
    }

    // =======================================================================
    // BOLD (COMMON_EMPHASIS_PATTERN with *)
    // =======================================================================

    public static class BoldTests {

        @Test
        public void simpleBold() {
            Matcher m = OrgmodeSyntaxHighlighter.BOLD.matcher("*bold*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*bold*");
        }

        @Test
        public void boldWithSpaces() {
            Matcher m = OrgmodeSyntaxHighlighter.BOLD.matcher("*bold text*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*bold text*");
        }

        @Test
        public void boldInSentence() {
            Matcher m = OrgmodeSyntaxHighlighter.BOLD.matcher("This is *important* text.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*important*");
        }

        @Test
        public void multipleBoldInLine() {
            Matcher m = OrgmodeSyntaxHighlighter.BOLD.matcher("*one* and *two*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*one*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*two*");
        }

        @Test
        public void singleCharBold() {
            Matcher m = OrgmodeSyntaxHighlighter.BOLD.matcher("*x*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*x*");
        }

        @Test
        public void tripleStarNotMatchedAsBold() {
            // *** is not a valid emphasis — the negative lookahead (?!\\2+\\2) prevents it
            Matcher m = OrgmodeSyntaxHighlighter.BOLD.matcher("***not bold***");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void boldInListContext() {
            Matcher m = OrgmodeSyntaxHighlighter.BOLD.matcher("- *bold item*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*bold item*");
        }
    }

    // =======================================================================
    // ITALICS (COMMON_EMPHASIS_PATTERN with /)
    // =======================================================================

    public static class ItalicTests {

        @Test
        public void simpleItalic() {
            Matcher m = OrgmodeSyntaxHighlighter.ITALICS.matcher("/italic/");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("/italic/");
        }

        @Test
        public void italicInSentence() {
            Matcher m = OrgmodeSyntaxHighlighter.ITALICS.matcher("This /word/ is italic.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("/word/");
        }

        @Test
        public void tripleSlashNotMatched() {
            Matcher m = OrgmodeSyntaxHighlighter.ITALICS.matcher("///not italic///");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void italicWithSpaces() {
            Matcher m = OrgmodeSyntaxHighlighter.ITALICS.matcher("/italic text/");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("/italic text/");
        }
    }

    // =======================================================================
    // STRIKETHROUGH (COMMON_EMPHASIS_PATTERN with +)
    // =======================================================================

    public static class StrikethroughTests {

        @Test
        public void simpleStrikethrough() {
            Matcher m = OrgmodeSyntaxHighlighter.STRIKETHROUGH.matcher("+deleted+");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("+deleted+");
        }

        @Test
        public void strikethroughInSentence() {
            Matcher m = OrgmodeSyntaxHighlighter.STRIKETHROUGH.matcher("This is +removed+ text.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("+removed+");
        }

        @Test
        public void strikethroughWithSpaces() {
            Matcher m = OrgmodeSyntaxHighlighter.STRIKETHROUGH.matcher("+strike through+");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void triplePlusNotMatched() {
            Matcher m = OrgmodeSyntaxHighlighter.STRIKETHROUGH.matcher("+++not struck+++");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // UNDERLINE (COMMON_EMPHASIS_PATTERN with _)
    // =======================================================================

    public static class UnderlineTests {

        @Test
        public void simpleUnderline() {
            Matcher m = OrgmodeSyntaxHighlighter.UNDERLINE.matcher("_underlined_");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("_underlined_");
        }

        @Test
        public void underlineInSentence() {
            Matcher m = OrgmodeSyntaxHighlighter.UNDERLINE.matcher("This is _important_ text.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("_important_");
        }

        @Test
        public void underlineWithSpaces() {
            Matcher m = OrgmodeSyntaxHighlighter.UNDERLINE.matcher("_underlined text_");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void tripleUnderscoreNotMatched() {
            Matcher m = OrgmodeSyntaxHighlighter.UNDERLINE.matcher("___not underlined___");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // CODE_INLINE (COMMON_EMPHASIS_PATTERN with =~)
    // =======================================================================

    public static class CodeInlineTests {

        @Test
        public void inlineCodeWithEquals() {
            Matcher m = OrgmodeSyntaxHighlighter.CODE_INLINE.matcher("Use =code= here.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("=code=");
        }

        @Test
        public void inlineCodeWithTilde() {
            Matcher m = OrgmodeSyntaxHighlighter.CODE_INLINE.matcher("Use ~code~ here.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~code~");
        }

        @Test
        public void singleCharCode() {
            Matcher m = OrgmodeSyntaxHighlighter.CODE_INLINE.matcher("=x=");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void tripleEqualsNotMatched() {
            Matcher m = OrgmodeSyntaxHighlighter.CODE_INLINE.matcher("===not code===");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // HEADING
    // =======================================================================

    public static class HeadingTests {

        @Test
        public void level1Heading() {
            Matcher m = OrgmodeSyntaxHighlighter.HEADING.matcher("* Top Level");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("* Top Level");
        }

        @Test
        public void level2Heading() {
            Matcher m = OrgmodeSyntaxHighlighter.HEADING.matcher("** Sub Level");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("** Sub Level");
        }

        @Test
        public void level5Heading() {
            Matcher m = OrgmodeSyntaxHighlighter.HEADING.matcher("***** Deep Level");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void starWithoutSpaceIsNotHeading() {
            Matcher m = OrgmodeSyntaxHighlighter.HEADING.matcher("*bold*");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void multipleHeadings() {
            String doc = "* H1\n** H2\n*** H3\n";
            List<String> matches = allMatches(OrgmodeSyntaxHighlighter.HEADING, doc);
            assertThat(matches).hasSize(3);
        }

        @Test
        public void headingWithTags() {
            Matcher m = OrgmodeSyntaxHighlighter.HEADING.matcher("* Heading    :tag1:tag2:");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains(":tag1:tag2:");
        }
    }

    // =======================================================================
    // BLOCK (#+BEGIN_...#+END)
    // =======================================================================

    public static class BlockTests {

        @Test
        public void srcBlock() {
            String block = "#+BEGIN_SRC python\nprint('hello')\n#+END_SRC";
            Matcher m = OrgmodeSyntaxHighlighter.BLOCK.matcher(block);
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("print('hello')");
        }

        @Test
        public void exampleBlock() {
            String block = "#+BEGIN_EXAMPLE\nSome example text.\n#+END_EXAMPLE";
            Matcher m = OrgmodeSyntaxHighlighter.BLOCK.matcher(block);
            assertThat(m.find()).isTrue();
        }

        @Test
        public void quoteBlock() {
            String block = "#+BEGIN_QUOTE\nA wise saying.\n#+END_QUOTE";
            Matcher m = OrgmodeSyntaxHighlighter.BLOCK.matcher(block);
            assertThat(m.find()).isTrue();
        }

        @Test
        public void centerBlock() {
            String block = "#+BEGIN_CENTER\nCentered text.\n#+END_CENTER";
            Matcher m = OrgmodeSyntaxHighlighter.BLOCK.matcher(block);
            assertThat(m.find()).isTrue();
        }

        @Test
        public void blockWithLanguageParameter() {
            // The BLOCK pattern .{1,15}$ limits the header line to 15 chars after BEGIN_
            // "SRC java :eval" is 14 chars, within the limit
            String block = "#+BEGIN_SRC java :eval\nSystem.out.println(\"hi\");\n#+END_SRC";
            Matcher m = OrgmodeSyntaxHighlighter.BLOCK.matcher(block);
            assertThat(m.find()).isTrue();
        }
    }

    // =======================================================================
    // PREAMBLE (#+...)
    // =======================================================================

    public static class PreambleTests {

        @Test
        public void titlePreamble() {
            Matcher m = OrgmodeSyntaxHighlighter.PREAMBLE.matcher("#+TITLE: My Document");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("#+TITLE: My Document");
        }

        @Test
        public void authorPreamble() {
            Matcher m = OrgmodeSyntaxHighlighter.PREAMBLE.matcher("#+AUTHOR: John Doe");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void datePreamble() {
            Matcher m = OrgmodeSyntaxHighlighter.PREAMBLE.matcher("#+DATE: 2024-01-01");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void optionsPreamble() {
            Matcher m = OrgmodeSyntaxHighlighter.PREAMBLE.matcher("#+OPTIONS: toc:nil");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void multiplePreambleLines() {
            String doc = "#+TITLE: Doc\n#+AUTHOR: Me\n#+DATE: today\n";
            List<String> matches = allMatches(OrgmodeSyntaxHighlighter.PREAMBLE, doc);
            assertThat(matches).hasSize(3);
        }
    }

    // =======================================================================
    // COMMENT
    // =======================================================================

    public static class CommentTests {

        @Test
        public void simpleComment() {
            Matcher m = OrgmodeSyntaxHighlighter.COMMENT.matcher("# This is a comment");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void commentWithMultipleHashes() {
            Matcher m = OrgmodeSyntaxHighlighter.COMMENT.matcher("## Double hash comment");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void preambleNotCommentedAsComment() {
            // The COMMENT pattern is (?m)^(#+) (.*?) — requires '#' followed by a space.
            // In #+TITLE: test, after the '#' the next char is '+', not a space,
            // so it does NOT match the COMMENT pattern.
            Matcher m = OrgmodeSyntaxHighlighter.COMMENT.matcher("#+TITLE: test");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // LISTS
    // =======================================================================

    public static class ListTests {

        @Test
        public void unorderedListWithPlus() {
            Matcher m = OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher("+ item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void unorderedListWithMinus() {
            Matcher m = OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher("- item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void unorderedListCheckbox() {
            Matcher m = OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher("- [ ] unchecked");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void unorderedListCheckedBox() {
            Matcher m = OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher("- [X] checked");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void orderedListWithDot() {
            Matcher m = OrgmodeSyntaxHighlighter.LIST_ORDERED.matcher("1. item");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("1");
        }

        @Test
        public void orderedListWithParen() {
            Matcher m = OrgmodeSyntaxHighlighter.LIST_ORDERED.matcher("1) item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void indentedUnorderedList() {
            Matcher m = OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher("  - indented item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void multipleOrderedItems() {
            String text = "1. first\n2. second\n3. third";
            List<String> matches = allMatches(OrgmodeSyntaxHighlighter.LIST_ORDERED, text);
            assertThat(matches).hasSize(3);
        }

        @Test
        public void asteriskListNotMatchedAsUnordered() {
            // Org-mode uses +/- for unordered lists, not *
            Matcher m = OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher("* not org list");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // LINK
    // =======================================================================

    public static class LinkTests {

        @Test
        public void orgDoubleBracketLink() {
            Matcher m = OrgmodeSyntaxHighlighter.LINK.matcher("[[https://example.com]]");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("[[https://example.com]]");
        }

        @Test
        public void orgLinkWithDescription() {
            Matcher m = OrgmodeSyntaxHighlighter.LINK.matcher("[[https://example.com][Example]]");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void angleLink() {
            Matcher m = OrgmodeSyntaxHighlighter.LINK.matcher("<https://example.com>");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void bareUrlLink() {
            Matcher m = OrgmodeSyntaxHighlighter.LINK.matcher("Visit https://example.com for info.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("https://example.com");
        }

        @Test
        public void internalLink() {
            Matcher m = OrgmodeSyntaxHighlighter.LINK.matcher("[[*Heading][Go to heading]]");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void multipleLinksInDocument() {
            String doc = "See [[link1]] and [[link2]] and https://example.com.";
            List<String> matches = allMatches(OrgmodeSyntaxHighlighter.LINK, doc);
            assertThat(matches).hasSize(3);
        }
    }

    // =======================================================================
    // Integration: full Org-mode document
    // =======================================================================

    public static class IntegrationTests {

        @Test
        public void fullDocumentCoversAllMajorPatterns() {
            String doc =
                    "#+TITLE: Test Document\n"
                            + "#+AUTHOR: Test Author\n"
                            + "\n"
                            + "* Introduction\n"
                            + "\n"
                            + "This paragraph has *bold*, /italic/, +struck+, and _underlined_ text.\n"
                            + "Inline code: =some_func()=\n"
                            + "\n"
                            + "** Lists\n"
                            + "\n"
                            + "- item one\n"
                            + "- item two\n"
                            + "  - [ ] unchecked task\n"
                            + "  - [X] checked task\n"
                            + "\n"
                            + "1. first\n"
                            + "2. second\n"
                            + "\n"
                            + "#+BEGIN_SRC python\n"
                            + "print('hello world')\n"
                            + "#+END_SRC\n"
                            + "\n"
                            + "Visit [[https://orgmode.org][Org Mode]] for details.\n";

            assertThat(OrgmodeSyntaxHighlighter.PREAMBLE.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.HEADING.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.BOLD.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.ITALICS.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.STRIKETHROUGH.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.UNDERLINE.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.CODE_INLINE.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.LIST_ORDERED.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.BLOCK.matcher(doc).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.LINK.matcher(doc).find()).isTrue();
        }

        @Test
        public void mixedEmphasisTypesDoNotInterfere() {
            String line = "This has *bold* and /italic/ and +struck+ and _underline_ and =code=.";
            assertThat(OrgmodeSyntaxHighlighter.BOLD.matcher(line).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.ITALICS.matcher(line).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.STRIKETHROUGH.matcher(line).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.UNDERLINE.matcher(line).find()).isTrue();
            assertThat(OrgmodeSyntaxHighlighter.CODE_INLINE.matcher(line).find()).isTrue();
        }

        @Test
        public void crlfAndLfProduceSameMatches() {
            String docLf = "* Heading\n\n*bold* and /italic/\n\n- list item\n";
            String docCrlf = docLf.replace("\n", "\r\n");

            // Bold
            assertThat(OrgmodeSyntaxHighlighter.BOLD.matcher(docLf).find())
                    .isEqualTo(OrgmodeSyntaxHighlighter.BOLD.matcher(docCrlf).find());
            // Heading
            assertThat(OrgmodeSyntaxHighlighter.HEADING.matcher(docLf).find())
                    .isEqualTo(OrgmodeSyntaxHighlighter.HEADING.matcher(docCrlf).find());
            // List
            assertThat(OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher(docLf).find())
                    .isEqualTo(OrgmodeSyntaxHighlighter.LIST_UNORDERED.matcher(docCrlf).find());
        }
    }
}
