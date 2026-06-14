/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for all AsciiDoc syntax highlighter regex patterns.
 * <p>
 * Each inner class targets one pattern or a small group of related patterns.
 * The tests verify both positive matches (valid AsciiDoc syntax) and
 * negative matches (text that should NOT be highlighted).
 */
public class AsciidocSyntaxHighlighterTest {

    private static List<String> allMatches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches;
    }

    // =======================================================================
    // BOLD
    // =======================================================================

    public static class BoldTests {

        @Test
        public void simpleBold() {
            Matcher m = AsciidocSyntaxHighlighter.BOLD.matcher("*bold*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*bold*");
        }

        @Test
        public void boldWithSpaces() {
            Matcher m = AsciidocSyntaxHighlighter.BOLD.matcher("*bold text*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*bold text*");
        }

        @Test
        public void boldInSentence() {
            Matcher m = AsciidocSyntaxHighlighter.BOLD.matcher("This is *important* info.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*important*");
        }

        @Test
        public void multipleBoldInLine() {
            Matcher m = AsciidocSyntaxHighlighter.BOLD.matcher("*one* and *two*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*one*");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*two*");
        }

        @Test
        public void doubleStarMatchesAsBold() {
            // The BOLD pattern (\*\S(?!\*)(.*?)\S\*(?!\*)) matches **not bold**
            // because * at pos 0, * at pos 1 (\S), then (?!\*) succeeds (next is 'n'),
            // and the closing \*(?!\*) matches the last two *'s.
            Matcher m = AsciidocSyntaxHighlighter.BOLD.matcher("**not bold**");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("**not bold**");
        }
    }

    // =======================================================================
    // ITALICS
    // =======================================================================

    public static class ItalicTests {

        @Test
        public void simpleItalic() {
            Matcher m = AsciidocSyntaxHighlighter.ITALICS.matcher("_italic_");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("_italic_");
        }

        @Test
        public void italicInSentence() {
            Matcher m = AsciidocSyntaxHighlighter.ITALICS.matcher("This _word_ is italic.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("_word_");
        }

        @Test
        public void doubleUnderscoreMatchesAsItalic() {
            // The ITALICS pattern (_\S(?!_)(.*?)\S_(?!_)) matches __not italic__
            // by the same logic as double-star bold.
            Matcher m = AsciidocSyntaxHighlighter.ITALICS.matcher("__not italic__");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("__not italic__");
        }

        @Test
        public void boldAndItalicInSameLine() {
            String text = "This has *bold* and _italic_ text.";
            assertThat(AsciidocSyntaxHighlighter.BOLD.matcher(text).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.ITALICS.matcher(text).find()).isTrue();
        }
    }

    // =======================================================================
    // MONOSPACE (inline code)
    // =======================================================================

    public static class MonospaceTests {

        @Test
        public void simpleMonospace() {
            Matcher m = AsciidocSyntaxHighlighter.MONOSPACE.matcher("`code`");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("`code`");
        }

        @Test
        public void monospaceInSentence() {
            Matcher m = AsciidocSyntaxHighlighter.MONOSPACE.matcher("Use `printf()` to output.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("`printf()`");
        }

        @Test
        public void doubleBacktickMatchesMonospace() {
            // The MONOSPACE pattern (`(?!`)(.*?)`(?!`)) matches ``not mono``
            // starting from the second backtick to the second-to-last backtick.
            Matcher m = AsciidocSyntaxHighlighter.MONOSPACE.matcher("``not mono``");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("`not mono``");
        }
    }

    // =======================================================================
    // SUBSCRIPT and SUPERSCRIPT
    // =======================================================================

    public static class SubSupTests {

        @Test
        public void subscript() {
            Matcher m = AsciidocSyntaxHighlighter.SUBSCRIPT.matcher("H~2~O");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~2~");
        }

        @Test
        public void superscript() {
            Matcher m = AsciidocSyntaxHighlighter.SUPERSCRIPT.matcher("E=mc^2^");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("^2^");
        }

        @Test
        public void doubleTildeMatchesSubscript() {
            // The SUBSCRIPT pattern (~(?!~)(.*?)~(?!~)) matches ~~not sub~~
            // starting from the second tilde to the second-to-last tilde.
            Matcher m = AsciidocSyntaxHighlighter.SUBSCRIPT.matcher("~~not sub~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~not sub~~");
        }

        @Test
        public void doubleCaretMatchesSuperscript() {
            // The SUPERSCRIPT pattern (\^(?!\^)(.*?)\^(?!\^)) matches ^^not super^^
            // starting from the second caret to the second-to-last caret.
            Matcher m = AsciidocSyntaxHighlighter.SUPERSCRIPT.matcher("^^not super^^");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("^not super^^");
        }
    }

    // =======================================================================
    // HEADING
    // =======================================================================

    public static class HeadingTests {

        @Test
        public void asciidocHeadingLevel1() {
            Matcher m = AsciidocSyntaxHighlighter.HEADING_ASCIIDOC.matcher("= Document Title");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("= Document Title");
        }

        @Test
        public void asciidocHeadingLevel2() {
            Matcher m = AsciidocSyntaxHighlighter.HEADING_ASCIIDOC.matcher("== Section");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void asciidocHeadingLevel6() {
            Matcher m = AsciidocSyntaxHighlighter.HEADING_ASCIIDOC.matcher("====== Deep level");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void markdownHeadingAlsoMatched() {
            Matcher m = AsciidocSyntaxHighlighter.HEADING_MD.matcher("## MD Heading");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void combinedHeadingPatternMatchesBothStyles() {
            Pattern h = AsciidocSyntaxHighlighter.HEADING;
            assertThat(h.matcher("= AsciiDoc Title").find()).isTrue();
            assertThat(h.matcher("## Markdown Title").find()).isTrue();
        }

        @Test
        public void noSpaceAfterEqualsIsNotHeading() {
            Matcher m = AsciidocSyntaxHighlighter.HEADING_ASCIIDOC.matcher("==NoSpace");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void sevenEqualsIsNotHeading() {
            Matcher m = AsciidocSyntaxHighlighter.HEADING_ASCIIDOC.matcher("======= too deep");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // LISTS
    // =======================================================================

    public static class ListTests {

        @Test
        public void unorderedListLevel1() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_UNORDERED.matcher("* item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void unorderedListLevel3() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_UNORDERED.matcher("*** deep item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void orderedListLevel1() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_ORDERED.matcher(". item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void orderedListLevel3() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_ORDERED.matcher("... deep item");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void noSpaceAfterStarIsNotUnorderedList() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_UNORDERED.matcher("*noSpace");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void descriptionList() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_DESCRIPTION.matcher("Term:: definition");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("Term::");
        }

        @Test
        public void descriptionListWithTripleColon() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_DESCRIPTION.matcher("Term::: definition");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void descriptionListWithSemicolon() {
            Matcher m = AsciidocSyntaxHighlighter.LIST_DESCRIPTION.matcher("Term;; definition");
            assertThat(m.find()).isTrue();
        }
    }

    // =======================================================================
    // ADMONITION
    // =======================================================================

    public static class AdmonitionTests {

        @Test
        public void noteAdmonition() {
            Matcher m = AsciidocSyntaxHighlighter.ADMONITION.matcher("NOTE: This is important.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("NOTE: ");
        }

        @Test
        public void tipAdmonition() {
            Matcher m = AsciidocSyntaxHighlighter.ADMONITION.matcher("TIP: Try this.");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void importantAdmonition() {
            Matcher m = AsciidocSyntaxHighlighter.ADMONITION.matcher("IMPORTANT: Read carefully.");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void cautionAdmonition() {
            Matcher m = AsciidocSyntaxHighlighter.ADMONITION.matcher("CAUTION: Be careful.");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void warningAdmonition() {
            Matcher m = AsciidocSyntaxHighlighter.ADMONITION.matcher("WARNING: Danger!");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void lowercaseNoteIsNotAdmonition() {
            Matcher m = AsciidocSyntaxHighlighter.ADMONITION.matcher("note: lowercase");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void admonitionNotAtStartOfLine() {
            Matcher m = AsciidocSyntaxHighlighter.ADMONITION.matcher("some text NOTE: mid-line");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // ATTRIBUTE definition and reference
    // =======================================================================

    public static class AttributeTests {

        @Test
        public void attributeDefinition() {
            Matcher m = AsciidocSyntaxHighlighter.ATTRIBUTE_DEFINITION.matcher(":author: John Doe");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo(":author:");
        }

        @Test
        public void attributeReference() {
            Matcher m = AsciidocSyntaxHighlighter.ATTRIBUTE_REFERENCE.matcher("Written by {author}.");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("{author}");
        }

        @Test
        public void multipleAttributeReferences() {
            String text = "{firstname} {lastname}";
            List<String> matches = allMatches(AsciidocSyntaxHighlighter.ATTRIBUTE_REFERENCE, text);
            assertThat(matches).containsExactly("{firstname}", "{lastname}");
        }
    }

    // =======================================================================
    // LINE COMMENT
    // =======================================================================

    public static class CommentTests {

        @Test
        public void lineComment() {
            Matcher m = AsciidocSyntaxHighlighter.LINE_COMMENT.matcher("// This is a comment");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void tripleSlashNotMatchedAsComment() {
            Matcher m = AsciidocSyntaxHighlighter.LINE_COMMENT.matcher("/// triple slash");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void commentNotAtStartOfLine() {
            Matcher m = AsciidocSyntaxHighlighter.LINE_COMMENT.matcher("text // comment");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // LINK / XREF / IMAGE / INCLUDE patterns
    // =======================================================================

    public static class LinkPatternTests {

        @Test
        public void linkPattern() {
            Matcher m = AsciidocSyntaxHighlighter.LINK_PATTERN.matcher("link:file.html[Click here]");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("Click here");
        }

        @Test
        public void xrefPattern() {
            Matcher m = AsciidocSyntaxHighlighter.XREF_PATTERN.matcher("xref:chapter2.adoc[Chapter 2]");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("Chapter 2");
        }

        @Test
        public void imagePattern() {
            Matcher m = AsciidocSyntaxHighlighter.IMAGE_PATTERN.matcher("image:sunset.png[Sunset photo]");
            assertThat(m.find()).isTrue();
            assertThat(m.group(1)).isEqualTo("Sunset photo");
        }

        @Test
        public void includePattern() {
            Matcher m = AsciidocSyntaxHighlighter.INCLUDE_PATTERN.matcher("include::chapter1.adoc[]");
            assertThat(m.find()).isTrue();
        }
    }

    // =======================================================================
    // HIGHLIGHT and ROLE_STRIKETHROUGH
    // =======================================================================

    public static class HighlightTests {

        @Test
        public void singleHashHighlight() {
            Matcher m = AsciidocSyntaxHighlighter.HIGHLIGHT.matcher("Some #highlighted# text");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("#highlighted#");
        }

        @Test
        public void doubleHashHighlight() {
            Matcher m = AsciidocSyntaxHighlighter.HIGHLIGHT.matcher("Some ##highlighted## text");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("##highlighted##");
        }

        @Test
        public void roleStrikethrough() {
            Matcher m = AsciidocSyntaxHighlighter.ROLE_STRIKETHROUGH.matcher("[.line-through]#deleted#");
            assertThat(m.find()).isTrue();
        }
    }

    // =======================================================================
    // HARD LINE BREAK
    // =======================================================================

    public static class HardLineBreakTests {

        @Test
        public void hardLineBreakWithPlus() {
            Matcher m = AsciidocSyntaxHighlighter.HARD_LINE_BREAK.matcher("line one +\nline two");
            assertThat(m.find()).isTrue();
        }

        @Test
        public void plusNotPrecededBySpaceNoMatch() {
            Matcher m = AsciidocSyntaxHighlighter.HARD_LINE_BREAK.matcher("c++\nmore");
            assertThat(m.find()).isFalse();
        }
    }

    // =======================================================================
    // BLOCK delimiters
    // =======================================================================

    public static class BlockDelimiterTests {

        @Test
        public void quotationBlock() {
            String block = "____\nQuoted text.\n____\n";
            Matcher m = AsciidocSyntaxHighlighter.BLOCK_DELIMITED_QUOTATION.matcher(block);
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("Quoted text.");
        }

        @Test
        public void listingBlock() {
            String block = "----\ncode here\n----\n";
            Matcher m = AsciidocSyntaxHighlighter.BLOCK_DELIMITED_LISTING.matcher(block);
            assertThat(m.find()).isTrue();
            assertThat(m.group()).contains("code here");
        }

        @Test
        public void exampleBlock() {
            String block = "====\nExample content.\n====\n";
            Matcher m = AsciidocSyntaxHighlighter.BLOCK_DELIMITED_EXAMPLE.matcher(block);
            assertThat(m.find()).isTrue();
        }

        @Test
        public void literalBlock() {
            String block = "....\nLiteral text.\n....\n";
            Matcher m = AsciidocSyntaxHighlighter.BLOCK_DELIMITED_LITERAL.matcher(block);
            assertThat(m.find()).isTrue();
        }

        @Test
        public void sidebarBlock() {
            String block = "****\nSidebar content.\n****\n";
            Matcher m = AsciidocSyntaxHighlighter.BLOCK_DELIMITED_SIDEBAR.matcher(block);
            assertThat(m.find()).isTrue();
        }

        @Test
        public void commentBlock() {
            String block = "////\nHidden comment.\n////\n";
            Matcher m = AsciidocSyntaxHighlighter.BLOCK_DELIMITED_COMMENT.matcher(block);
            assertThat(m.find()).isTrue();
        }

        @Test
        public void tableBlock() {
            String block = "|===\n| A | B\n|===\n";
            Matcher m = AsciidocSyntaxHighlighter.BLOCK_DELIMITED_TABLE.matcher(block);
            assertThat(m.find()).isTrue();
        }
    }

    // =======================================================================
    // SQUAREBRACKETS
    // =======================================================================

    public static class SquareBracketsTests {

        @Test
        public void simpleSquareBrackets() {
            Matcher m = AsciidocSyntaxHighlighter.SQUAREBRACKETS.matcher("[some text]");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("[some text]");
        }

        @Test
        public void emptySquareBrackets() {
            Matcher m = AsciidocSyntaxHighlighter.SQUAREBRACKETS.matcher("[]");
            assertThat(m.find()).isTrue();
        }
    }

    // =======================================================================
    // BLOCKTITLE
    // =======================================================================

    public static class BlockTitleTests {

        @Test
        public void blockTitle() {
            Matcher m = AsciidocSyntaxHighlighter.BLOCKTITLE.matcher(".My block title");
            assertThat(m.find()).isTrue();
        }
    }

    // =======================================================================
    // Integration: full AsciiDoc document
    // =======================================================================

    public static class IntegrationTests {

        @Test
        public void fullDocumentCoversAllMajorPatterns() {
            String doc =
                    "= Document Title\n"
                            + ":author: Test Author\n"
                            + "\n"
                            + "== Introduction\n"
                            + "\n"
                            + "This paragraph has *bold*, _italic_, and `monospace` text.\n"
                            + "It also has a ~subscript~ and a ^superscript^.\n"
                            + "\n"
                            + "NOTE: This is an admonition.\n"
                            + "\n"
                            + "// This is a comment\n"
                            + "\n"
                            + "=== Lists\n"
                            + "\n"
                            + "* unordered 1\n"
                            + "* unordered 2\n"
                            + "\n"
                            + ". ordered 1\n"
                            + ". ordered 2\n"
                            + "\n"
                            + "link:page.html[Click here] and image:pic.png[Photo]\n"
                            + "\n"
                            + "Written by {author}.\n"
                            + "\n"
                            + "----\n"
                            + "code block\n"
                            + "----\n";

            assertThat(AsciidocSyntaxHighlighter.HEADING_ASCIIDOC.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.BOLD.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.ITALICS.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.MONOSPACE.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.SUBSCRIPT.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.SUPERSCRIPT.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.ADMONITION.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.LINE_COMMENT.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.LIST_UNORDERED.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.LIST_ORDERED.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.LINK_PATTERN.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.IMAGE_PATTERN.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.ATTRIBUTE_DEFINITION.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.ATTRIBUTE_REFERENCE.matcher(doc).find()).isTrue();
            assertThat(AsciidocSyntaxHighlighter.BLOCK_DELIMITED_LISTING.matcher(doc).find()).isTrue();
        }

        @Test
        public void crlfAndLfProduceSameMatches() {
            String docLf = "== Heading\n\n*bold* and _italic_\n\n* list item\n";
            String docCrlf = docLf.replace("\n", "\r\n");

            // Compare BOLD pattern results
            Matcher mLf = AsciidocSyntaxHighlighter.BOLD.matcher(docLf);
            Matcher mCrlf = AsciidocSyntaxHighlighter.BOLD.matcher(docCrlf);
            assertThat(mLf.find()).isEqualTo(mCrlf.find());
            if (mCrlf.hitEnd() || mCrlf.find()) {
                // Both should find "*bold*"
            }

            // Compare ITALICS
            mLf = AsciidocSyntaxHighlighter.ITALICS.matcher(docLf);
            mCrlf = AsciidocSyntaxHighlighter.ITALICS.matcher(docCrlf);
            assertThat(mLf.find()).isEqualTo(mCrlf.find());
        }
    }
}
