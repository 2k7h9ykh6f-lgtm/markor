/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import static net.gsantner.markor.format.FormatHighlightTestHelper.allMatches;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Golden / parameterized regression tests for {@link AsciidocSyntaxHighlighter}.
 * <p>
 * Each row models a realistic editing scenario (nested lists, delimited code/table blocks,
 * admonitions, link &amp; image macros, inline emphasis, headings, hard line breaks) and asserts
 * the <em>exact, ordered</em> set of regions a highlighter pattern would span. This pins down the
 * regression-prone interaction between block markers and inline markup rather than only checking
 * that highlighting does not crash.
 */
@RunWith(Parameterized.class)
public class AsciidocHighlightScenarioTest {

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
        return Arrays.asList(new Object[][]{

                // --- Scenario class 1: nested / multi-level lists ---
                {"nested unordered list keeps each level marker",
                        AsciidocSyntaxHighlighter.LIST_UNORDERED,
                        "* a\n** b\n*** c\n",
                        new String[]{"* ", "** ", "*** "}},

                {"ordered (dotted) list markers",
                        AsciidocSyntaxHighlighter.LIST_ORDERED,
                        ". one\n.. two\n",
                        new String[]{". ", ".. "}},

                // --- Scenario class 2: delimited blocks (code / table) ---
                {"listing block captured as one span incl. delimiters",
                        AsciidocSyntaxHighlighter.BLOCK_DELIMITED_LISTING,
                        "before\n----\ncode line 1\ncode line 2\n----\nafter\n",
                        new String[]{"----\ncode line 1\ncode line 2\n----\n"}},

                {"table block captured as one span",
                        AsciidocSyntaxHighlighter.BLOCK_DELIMITED_TABLE,
                        "|===\n|a|b\n|c|d\n|===\n",
                        new String[]{"|===\n|a|b\n|c|d\n|===\n"}},

                // --- Scenario class 3: admonitions ---
                {"admonition labels highlighted, plain text ignored",
                        AsciidocSyntaxHighlighter.ADMONITION,
                        "NOTE: take note\nWARNING: be careful\nplain line\n",
                        new String[]{"NOTE: ", "WARNING: "}},

                // --- Scenario class 4: link & image macros ---
                {"link macro span",
                        AsciidocSyntaxHighlighter.LINK_PATTERN,
                        "See link:https://x.com[Site] and image:pic.png[Alt] end",
                        new String[]{"link:https://x.com[Site]"}},

                {"image macro span",
                        AsciidocSyntaxHighlighter.IMAGE_PATTERN,
                        "See link:https://x.com[Site] and image:pic.png[Alt] end",
                        new String[]{"image:pic.png[Alt]"}},

                // --- Scenario class 5: inline emphasis trio ---
                {"bold inline",
                        AsciidocSyntaxHighlighter.BOLD,
                        "*bold* and _italic_ and `mono` end",
                        new String[]{"*bold*"}},

                {"italic inline",
                        AsciidocSyntaxHighlighter.ITALICS,
                        "*bold* and _italic_ and `mono` end",
                        new String[]{"_italic_"}},

                {"monospace inline",
                        AsciidocSyntaxHighlighter.MONOSPACE,
                        "*bold* and _italic_ and `mono` end",
                        new String[]{"`mono`"}},

                // --- Scenario class 6: headings (AsciiDoc '=' and Markdown-style '#') ---
                {"headings of both syntaxes",
                        AsciidocSyntaxHighlighter.HEADING,
                        "== Section\n=== Sub\n# Md style\n",
                        new String[]{"== Section", "=== Sub", "# Md style"}},

                // --- Scenario class 7: hard line break marker ---
                {"hard line break marker",
                        AsciidocSyntaxHighlighter.HARD_LINE_BREAK,
                        "line one +\nline two\n",
                        new String[]{" +\n"}},

                // --- Scenario class 8: a mixed document, pattern must isolate its own construct ---
                {"mixed doc: only list markers picked up",
                        AsciidocSyntaxHighlighter.LIST_UNORDERED,
                        "== Title\n\nNOTE: read this\n\n* first\n* second\n\nSee link:https://x[X]\n",
                        new String[]{"* ", "* "}},

                {"mixed doc: only admonition picked up",
                        AsciidocSyntaxHighlighter.ADMONITION,
                        "== Title\n\nNOTE: read this\n\n* first\n* second\n\nSee link:https://x[X]\n",
                        new String[]{"NOTE: "}},

                {"mixed doc: only heading picked up",
                        AsciidocSyntaxHighlighter.HEADING,
                        "== Title\n\nNOTE: read this\n\n* first\n* second\n\nSee link:https://x[X]\n",
                        new String[]{"== Title"}},
        });
    }

    @Test
    public void highlightMatchesAreExact() {
        assertThat(allMatches(pattern, input))
                .as("scenario: %s", scenario)
                .containsExactly(expected);
    }
}
