/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.orgmode;

import static net.gsantner.markor.format.FormatHighlightTestHelper.allMatches;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Golden / parameterized regression tests for {@link OrgmodeSyntaxHighlighter}.
 * <p>
 * Rows cover realistic Org editing scenarios (heading nesting, checkbox &amp; ordered lists,
 * {@code #+BEGIN_SRC} blocks, the full emphasis set, links and the comment-vs-preamble
 * distinction) and assert the exact, ordered spans each pattern would produce.
 */
@RunWith(Parameterized.class)
public class OrgmodeHighlightScenarioTest {

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
        final String emphasisDoc = "*bold* /italic/ +strike+ _under_ =code= ~verb~ done";
        final String srcBlock = "#+BEGIN_SRC python\nprint(1)\nprint(2)\n#+END_SRC\n";
        final String commentDoc = "# a comment line\n#+TITLE: doc\n";

        return Arrays.asList(new Object[][]{

                // --- Scenario class 1: heading nesting ---
                {"heading levels",
                        OrgmodeSyntaxHighlighter.HEADING,
                        "* H1\n** H2\n*** H3\nbody\n",
                        new String[]{"* H1", "** H2", "*** H3"}},

                // --- Scenario class 2: lists (checkbox + ordered) ---
                {"unordered list with checkboxes and a sub item",
                        OrgmodeSyntaxHighlighter.LIST_UNORDERED,
                        "- [ ] todo\n- [X] done\n  + sub item\n",
                        new String[]{"- [ ]", "\n- [X]", "\n  +"}},

                {"ordered list with '.' and ')' markers",
                        OrgmodeSyntaxHighlighter.LIST_ORDERED,
                        "1. one\n2) two\n",
                        new String[]{"1. ", "2) "}},

                // --- Scenario class 3: source block (content + preamble fences) ---
                {"BEGIN_SRC block content captured",
                        OrgmodeSyntaxHighlighter.BLOCK,
                        srcBlock,
                        new String[]{"print(1)\nprint(2)\n"}},

                {"BEGIN_SRC / END_SRC fences highlighted as preamble",
                        OrgmodeSyntaxHighlighter.PREAMBLE,
                        srcBlock,
                        new String[]{"#+BEGIN_SRC python", "#+END_SRC"}},

                // --- Scenario class 4: full emphasis set ---
                {"bold emphasis",
                        OrgmodeSyntaxHighlighter.BOLD, emphasisDoc, new String[]{"*bold*"}},
                {"italic emphasis",
                        OrgmodeSyntaxHighlighter.ITALICS, emphasisDoc, new String[]{"/italic/"}},
                {"strikethrough emphasis",
                        OrgmodeSyntaxHighlighter.STRIKETHROUGH, emphasisDoc, new String[]{"+strike+"}},
                {"underline emphasis",
                        OrgmodeSyntaxHighlighter.UNDERLINE, emphasisDoc, new String[]{"_under_"}},
                {"inline code accepts both '=' and '~' delimiters",
                        OrgmodeSyntaxHighlighter.CODE_INLINE, emphasisDoc, new String[]{"=code=", "~verb~"}},

                // --- Scenario class 5: links of all supported syntaxes ---
                {"bracket link, angle link and bare url",
                        OrgmodeSyntaxHighlighter.LINK,
                        "[[https://x.com][label]] and <http://y.com> and https://z.org here",
                        new String[]{"[[https://x.com][label]]", "<http://y.com>", "https://z.org"}},

                // --- Scenario class 6: comment vs. preamble must not overlap ---
                {"only the plain '# ' line is a comment",
                        OrgmodeSyntaxHighlighter.COMMENT, commentDoc, new String[]{"# a comment line"}},
                {"only the '#+' line is a preamble directive",
                        OrgmodeSyntaxHighlighter.PREAMBLE, commentDoc, new String[]{"#+TITLE: doc"}},
        });
    }

    @Test
    public void highlightMatchesAreExact() {
        assertThat(allMatches(pattern, input))
                .as("scenario: %s", scenario)
                .containsExactly(expected);
    }
}
