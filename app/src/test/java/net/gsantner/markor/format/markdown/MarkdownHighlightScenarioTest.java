/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import static net.gsantner.markor.format.FormatHighlightTestHelper.allMatches;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Golden / parameterized regression tests for {@link MarkdownSyntaxHighlighter}.
 * <p>
 * Rows model realistic Markdown editing scenarios that historically caused highlighting
 * regressions: nested task lists, the bold-vs-italic {@code *}/{@code _} disambiguation, links
 * and images, inline/indented code, blockquotes, ordered lists with CRLF line endings, trailing
 * double-space hard breaks, escaped emphasis, and multi-construct documents where one pattern must
 * pick up only its own construct. Each row asserts the exact, ordered set of highlighted regions.
 */
@RunWith(Parameterized.class)
public class MarkdownHighlightScenarioTest {

    @Parameterized.Parameter(0)
    public String scenario;

    @Parameterized.Parameter(1)
    public Pattern pattern;

    @Parameterized.Parameter(2)
    public String input;

    @Parameterized.Parameter(3)
    public String[] expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        final String emphasisDoc = "This **bold** and *italic* plus __b2__ and _i2_.";
        // A realistic note mixing heading, inline code, bold, list and quote.
        final String mixedDoc = "# Notes\n\nSome `code` and **bold**.\n\n- one\n- two\n\n> quote\n";

        return Arrays.asList(new Object[][]{

                // --- Scenario class 1: nested task list (mixed [ ] / [x] markers, indentation) ---
                {"nested task list keeps every bullet+checkbox marker",
                        MarkdownSyntaxHighlighter.LIST_UNORDERED,
                        "- [ ] top\n  - [x] child done\n  - [ ] child todo\n- [ ] second\n",
                        new String[]{"- [ ]", "\n  - [x]", "\n  - [ ]", "\n- [ ]"}},

                // --- Scenario class 2: bold vs italic disambiguation across * and _ ---
                {"bold spans only (** / __), not the italic ones",
                        MarkdownSyntaxHighlighter.BOLD, emphasisDoc,
                        new String[]{"**bold**", "__b2__"}},
                {"italic spans only (* / _), not the bold ones",
                        MarkdownSyntaxHighlighter.ITALICS, emphasisDoc,
                        new String[]{"*italic*", "_i2_"}},

                // --- Scenario class 3: links and images ---
                {"link and image full spans",
                        MarkdownSyntaxHighlighter.LINK,
                        "See [text](http://a.com) and ![img](pic.png) end.",
                        new String[]{"[text](http://a.com)", "![img](pic.png)"}},

                // --- Scenario class 4: inline and indented code ---
                {"inline backtick code and 4-space indented code",
                        MarkdownSyntaxHighlighter.CODE,
                        "inline `x=1` here\n    indented_code()\n",
                        new String[]{"`x=1`", "    indented_code()"}},

                // --- Scenario class 5: strikethrough ---
                {"strikethrough span",
                        MarkdownSyntaxHighlighter.STRIKETHROUGH,
                        "remove ~~this~~ now",
                        new String[]{"~~this~~"}},

                // --- Scenario class 6: ATX headings ---
                {"atx headings only",
                        MarkdownSyntaxHighlighter.HEADING,
                        "# Title\n## Sub\nNot heading\n",
                        new String[]{"# Title", "## Sub"}},

                // --- Scenario class 7: blockquote ---
                {"blockquote marker",
                        MarkdownSyntaxHighlighter.QUOTATION,
                        "intro\n> quoted line\nafter",
                        new String[]{"\n>"}},

                // --- Scenario class 8: ordered list with Windows (CRLF) line endings ---
                {"ordered list markers survive CRLF newlines",
                        MarkdownSyntaxHighlighter.LIST_ORDERED,
                        "1. first\r\n2. second\r\n",
                        new String[]{"1. ", "2. "}},

                // --- Scenario class 9: trailing double-space hard line break ---
                {"trailing double space marks a hard break",
                        MarkdownSyntaxHighlighter.DOUBLESPACE_LINE_ENDING,
                        "line with trailing  \nnext\n",
                        new String[]{"  \n"}},

                // --- Scenario class 10: escaped emphasis must NOT be highlighted ---
                {"escaped italic markers are not highlighted, real ones are",
                        MarkdownSyntaxHighlighter.ITALICS,
                        "a \\*not italic\\* and real *italic* b",
                        new String[]{"*italic*"}},
                {"escaped bold markers are not highlighted, real ones are",
                        MarkdownSyntaxHighlighter.BOLD,
                        "x \\**not bold\\** and **bold** y",
                        new String[]{"**bold**"}},

                // --- Scenario class 11: mixed document, each pattern isolates its construct ---
                {"mixed doc: list markers only",
                        MarkdownSyntaxHighlighter.LIST_UNORDERED, mixedDoc,
                        new String[]{"\n\n-", "\n-"}},
                {"mixed doc: bold only",
                        MarkdownSyntaxHighlighter.BOLD, mixedDoc, new String[]{"**bold**"}},
                {"mixed doc: inline code only",
                        MarkdownSyntaxHighlighter.CODE, mixedDoc, new String[]{"`code`"}},
                {"mixed doc: quote only",
                        MarkdownSyntaxHighlighter.QUOTATION, mixedDoc, new String[]{"\n>"}},
                {"mixed doc: heading only",
                        MarkdownSyntaxHighlighter.HEADING, mixedDoc, new String[]{"# Notes"}},
        });
    }

    @Test
    public void highlightMatchesAreExact() {
        assertThat(allMatches(pattern, input))
                .as("scenario: %s", scenario)
                .containsExactly(expected);
    }
}
