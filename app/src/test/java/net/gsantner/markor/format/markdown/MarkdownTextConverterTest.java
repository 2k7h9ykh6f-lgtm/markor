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

import java.util.Arrays;
import java.util.List;

/**
 * Tests the Flexmark-based Markdown-to-HTML conversion pipeline.
 * <p>
 * These tests exercise the public static {@code flexmarkParser} and
 * {@code flexmarkRenderer} directly — no Android Context is needed.
 * The test inputs target real-world editing scenarios that historically
 * regress: nested lists, tables, task lists, fenced code blocks,
 * inline/bare links, mixed inline formatting, and escape characters.
 */
public class MarkdownTextConverterTest {

    // -----------------------------------------------------------------------
    // Helper: parse + render through the shared Flexmark pipeline
    // -----------------------------------------------------------------------
    private String render(String markdown) {
        com.vladsch.flexmark.util.ast.Document doc =
                MarkdownTextConverter.flexmarkParser.parse(markdown);
        return MarkdownTextConverter.flexmarkRenderer.render(doc);
    }

    // =======================================================================
    // 1. Headings
    // =======================================================================

    @Test
    public void atxHeadingsProduceCorrectTags() {
        for (int level = 1; level <= 6; level++) {
            String prefix = "#".repeat(level);
            String html = render(prefix + " Heading " + level);
            assertThat(html).contains("<h" + level);
            assertThat(html).contains("Heading " + level);
            assertThat(html).contains("</h" + level + ">");
        }
    }

    @Test
    public void setextHeadingLevel1() {
        String html = render("Title\n=====");
        assertThat(html).contains("<h1");
        assertThat(html).contains("Title");
    }

    @Test
    public void setextHeadingLevel2() {
        String html = render("Subtitle\n--------");
        assertThat(html).contains("<h2");
        assertThat(html).contains("Subtitle");
    }

    // =======================================================================
    // 2. Tables (GFM)
    // =======================================================================

    @Test
    public void gfmTableWithHeaderAndRows() {
        String md = "| Name  | Age |\n"
                + "|-------|-----|\n"
                + "| Alice | 30  |\n"
                + "| Bob   | 25  |\n";
        String html = render(md);
        assertThat(html).contains("<table");
        assertThat(html).contains("<thead");
        assertThat(html).contains("<tbody");
        assertThat(html).contains("Alice");
        assertThat(html).contains("Bob");
        assertThat(html).contains("</table>");
    }

    @Test
    public void gfmTableWithAlignment() {
        String md = "| Left | Center | Right |\n"
                + "|:-----|:------:|------:|\n"
                + "| L    | C      | R     |\n";
        String html = render(md);
        assertThat(html).contains("align=\"left\"");
        assertThat(html).contains("align=\"center\"");
        assertThat(html).contains("align=\"right\"");
    }

    @Test
    public void gfmTableWithMissingColumns() {
        String md = "| A | B | C |\n"
                + "|---|---|---|\n"
                + "| 1 |\n";
        String html = render(md);
        // DISCARD_EXTRA_COLUMNS = true, APPEND_MISSING_COLUMNS = false
        assertThat(html).contains("<table");
        assertThat(html).contains("1");
    }

    // =======================================================================
    // 3. Task lists
    // =======================================================================

    @Test
    public void taskListCheckedAndUnchecked() {
        String md = "- [ ] unchecked\n- [x] checked\n- [X] also checked\n";
        String html = render(md);
        assertThat(html).contains("checkbox");
        // Checked items should have checked attribute
        assertThat(html).contains("checked");
    }

    @Test
    public void taskListNestedInUnorderedList() {
        String md = "- item\n  - [ ] sub-task\n  - [x] done\n";
        String html = render(md);
        assertThat(html).contains("checkbox");
        assertThat(html).contains("sub-task");
        assertThat(html).contains("done");
    }

    // =======================================================================
    // 4. Fenced code blocks
    // =======================================================================

    @Test
    public void fencedCodeBlockWithLanguage() {
        String md = "```java\nint x = 42;\n```\n";
        String html = render(md);
        assertThat(html).contains("<pre");
        assertThat(html).contains("<code");
        assertThat(html).contains("int x = 42;");
    }

    @Test
    public void fencedCodeBlockPreservesSpecialCharacters() {
        String md = "```html\n<div class=\"test\">&amp;</div>\n```\n";
        String html = render(md);
        assertThat(html).contains("&lt;div");
        assertThat(html).contains("&amp;amp;");
    }

    @Test
    public void indentedCodeBlock() {
        String md = "    code line 1\n    code line 2\n";
        String html = render(md);
        assertThat(html).contains("<pre");
        assertThat(html).contains("code line 1");
    }

    @Test
    public void inlineCode() {
        String html = render("Use `System.out.println()` for debug.");
        assertThat(html).contains("<code");
        assertThat(html).contains("System.out.println()");
        assertThat(html).contains("</code>");
    }

    // =======================================================================
    // 5. Links and images
    // =======================================================================

    @Test
    public void inlineLink() {
        String html = render("[Google](https://google.com)");
        assertThat(html).contains("href=\"https://google.com\"");
        assertThat(html).contains("Google");
    }

    @Test
    public void linkWithTitle() {
        String html = render("[Example](https://example.com \"Example Site\")");
        assertThat(html).contains("href=\"https://example.com\"");
        assertThat(html).contains("title=\"Example Site\"");
    }

    @Test
    public void imageRendersImgTag() {
        String html = render("![alt text](image.png)");
        assertThat(html).contains("<img");
        assertThat(html).contains("src=\"image.png\"");
        assertThat(html).contains("alt=\"alt text\"");
    }

    @Test
    public void autoLink() {
        String html = render("Visit https://example.com for info.");
        assertThat(html).contains("href=\"https://example.com\"");
    }

    // =======================================================================
    // 6. Nested and mixed lists
    // =======================================================================

    @Test
    public void nestedUnorderedList() {
        String md = "- level 1\n  - level 2\n    - level 3\n";
        String html = render(md);
        // Should have nested <ul> elements
        long ulCount = html.split("<ul").length - 1;
        assertThat(ulCount).isGreaterThanOrEqualTo(2);
        assertThat(html).contains("level 1");
        assertThat(html).contains("level 2");
        assertThat(html).contains("level 3");
    }

    @Test
    public void orderedListInsideUnorderedList() {
        String md = "- item A\n  1. sub-1\n  2. sub-2\n- item B\n";
        String html = render(md);
        assertThat(html).contains("<ul");
        assertThat(html).contains("<ol");
        assertThat(html).contains("sub-1");
        assertThat(html).contains("sub-2");
    }

    @Test
    public void multiParagraphListItem() {
        String md = "- first paragraph\n\n  second paragraph\n- another item\n";
        String html = render(md);
        assertThat(html).contains("first paragraph");
        assertThat(html).contains("second paragraph");
        assertThat(html).contains("another item");
    }

    // =======================================================================
    // 7. Inline formatting (bold, italic, strikethrough, combined)
    // =======================================================================

    @Test
    public void boldText() {
        String html = render("This is **bold** text.");
        assertThat(html).contains("<strong");
        assertThat(html).contains("bold");
    }

    @Test
    public void italicText() {
        String html = render("This is *italic* text.");
        assertThat(html).contains("<em");
        assertThat(html).contains("italic");
    }

    @Test
    public void boldAndItalicCombined() {
        String html = render("***bold and italic***");
        assertThat(html).contains("<strong");
        assertThat(html).contains("<em");
    }

    @Test
    public void strikethroughText() {
        String html = render("This is ~~deleted~~ text.");
        assertThat(html).contains("<del");
        assertThat(html).contains("deleted");
    }

    // =======================================================================
    // 8. Blockquotes
    // =======================================================================

    @Test
    public void simpleBlockquote() {
        String html = render("> This is a quote.");
        assertThat(html).contains("<blockquote");
        assertThat(html).contains("This is a quote.");
    }

    @Test
    public void nestedBlockquote() {
        String html = render("> level 1\n>> level 2\n");
        assertThat(html).contains("<blockquote");
        // Nested blockquotes produce nested <blockquote> tags
        long bqCount = html.split("<blockquote").length - 1;
        assertThat(bqCount).isGreaterThanOrEqualTo(2);
    }

    // =======================================================================
    // 9. Horizontal rule / thematic break
    // =======================================================================

    @Test
    public void horizontalRule() {
        String html = render("- - -");
        assertThat(html).contains("<hr");
    }

    // =======================================================================
    // 10. Mixed real-world document scenario
    // =======================================================================

    @Test
    public void mixedDocumentWithHeadingsListsCodeAndTable() {
        String md = "# Project Notes\n\n"
                + "## Tasks\n\n"
                + "- [x] Setup repo\n"
                + "- [ ] Write tests\n"
                + "  - Focus on **edge cases**\n"
                + "  - Use `parameterized` tests\n\n"
                + "## Data\n\n"
                + "| ID | Status |\n"
                + "|----|--------|\n"
                + "| 1  | Done   |\n"
                + "| 2  | WIP    |\n\n"
                + "See [docs](https://example.com) for details.\n";
        String html = render(md);

        // Headings
        assertThat(html).contains("<h1");
        assertThat(html).contains("Project Notes");
        assertThat(html).contains("<h2");
        assertThat(html).contains("Tasks");

        // Task list
        assertThat(html).contains("checkbox");

        // Inline formatting
        assertThat(html).contains("<strong");
        assertThat(html).contains("edge cases");
        assertThat(html).contains("<code");
        assertThat(html).contains("parameterized");

        // Table
        assertThat(html).contains("<table");
        assertThat(html).contains("Done");
        assertThat(html).contains("WIP");

        // Link
        assertThat(html).contains("href=\"https://example.com\"");
    }

    // =======================================================================
    // 11. Escape characters and edge cases
    // =======================================================================

    @Test
    public void escapedSpecialCharacters() {
        String html = render("\\*not italic\\* and \\#not heading");
        // Escaped characters should not produce formatting
        assertThat(html).doesNotContain("<em");
        assertThat(html).doesNotContain("<h1");
    }

    @Test
    public void emptyDocumentProducesEmptyOutput() {
        String html = render("");
        assertThat(html.trim()).isEmpty();
    }

    @Test
    public void lineWithOnlyWhitespaceProducesParagraph() {
        String html = render("   \n");
        // Should not crash, output may be empty or contain minimal HTML
        assertThat(html).isNotNull();
    }

    // =======================================================================
    // 12. Footnotes extension
    // =======================================================================

    @Test
    public void footnoteProducesFootnoteHtml() {
        String md = "Text with footnote[^1].\n\n[^1]: Footnote content here.\n";
        String html = render(md);
        assertThat(html).contains("footnote");
        assertThat(html).contains("Footnote content here.");
    }

    // =======================================================================
    // 13. Superscript extension
    // =======================================================================

    @Test
    public void superscriptExtension() {
        String html = render("E = mc^2^");
        assertThat(html).contains("<sup");
        assertThat(html).contains("2");
    }

    // =======================================================================
    // 14. Mixed newline handling (CRLF vs LF)
    // =======================================================================

    @Test
    public void crlfLineEndingsProduceSameOutputAsLf() {
        String mdLf = "# Title\n\nParagraph one.\n\nParagraph two.\n";
        String mdCrlf = mdLf.replace("\n", "\r\n");

        String htmlLf = render(mdLf);
        String htmlCrlf = render(mdCrlf);

        assertThat(htmlLf).isEqualTo(htmlCrlf);
    }
}
