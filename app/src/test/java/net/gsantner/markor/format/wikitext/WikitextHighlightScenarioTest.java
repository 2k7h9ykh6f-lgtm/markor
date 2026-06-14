/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import static net.gsantner.markor.format.FormatHighlightTestHelper.allMatches;
import static net.gsantner.markor.format.FormatHighlightTestHelper.groupMatches;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Golden-case regression test for {@link WikitextSyntaxHighlighter} over one realistic Zim page.
 * <p>
 * A single fixture document combines the header, a heading, the full inline-emphasis set, an
 * unordered list, a checklist and a preformatted block. Each test pins down the exact set of
 * regions one highlighter pattern would span, verifying the patterns isolate their own construct
 * amid all the others (the realistic-editing case that unit-per-pattern tests do not cover).
 */
public class WikitextHighlightScenarioTest {

    private static final String ZIM_PAGE =
            "Content-Type: text/x-zim-wiki\n" +
                    "Wiki-Format: zim 0.6\n" +
                    "Creation-Date: 2020-12-24T18:00:30+01:00\n\n" +
                    "====== Title ======\n\n" +
                    "**bold** and //italic// and __mark__ and ~~strike~~\n\n" +
                    "* item one\n* item two\n\t* nested\n\n" +
                    "[ ] todo\n[*] done\n[x] crossed\n\n" +
                    "'''\npreformatted\nblock\n'''\n";

    @Test
    public void zimHeaderIsHighlightedAsOneBlock() {
        assertThat(allMatches(WikitextSyntaxHighlighter.ZIMHEADER, ZIM_PAGE))
                .containsExactly(
                        "Content-Type: text/x-zim-wiki\n" +
                                "Wiki-Format: zim 0.6\n" +
                                "Creation-Date: 2020-12-24T18:00:30+01:00");
    }

    @Test
    public void headingIsHighlighted() {
        assertThat(allMatches(WikitextSyntaxHighlighter.HEADING, ZIM_PAGE))
                .containsExactly("====== Title ======");
    }

    @Test
    public void inlineEmphasisVariantsAreIsolated() {
        assertThat(allMatches(WikitextSyntaxHighlighter.BOLD, ZIM_PAGE)).containsExactly("**bold**");
        assertThat(allMatches(WikitextSyntaxHighlighter.ITALICS, ZIM_PAGE)).containsExactly("//italic//");
        assertThat(allMatches(WikitextSyntaxHighlighter.HIGHLIGHTED, ZIM_PAGE)).containsExactly("__mark__");
        assertThat(allMatches(WikitextSyntaxHighlighter.STRIKETHROUGH, ZIM_PAGE)).containsExactly("~~strike~~");
    }

    @Test
    public void unorderedListBulletsAreHighlighted() {
        assertThat(allMatches(WikitextSyntaxHighlighter.LIST_UNORDERED, ZIM_PAGE))
                .containsExactly("*", "*", "*");
    }

    @Test
    public void checklistMarkersAreHighlighted() {
        assertThat(allMatches(WikitextSyntaxHighlighter.CHECKLIST, ZIM_PAGE))
                .containsExactly("[ ]", "[*]", "[x]");
    }

    @Test
    public void checklistSymbolsAreExtractableForPerStateColoring() {
        // The highlighter colors the inner symbol independently (see CHECKBOX_SYMBOL_GROUP usage).
        assertThat(groupMatches(WikitextSyntaxHighlighter.CHECKLIST, ZIM_PAGE,
                WikitextSyntaxHighlighter.CHECKBOX_SYMBOL_GROUP))
                .containsExactly(" ", "*", "x");
    }

    @Test
    public void preformattedBlockIsHighlightedAsOneSpan() {
        assertThat(allMatches(WikitextSyntaxHighlighter.PREFORMATTED_MULTILINE, ZIM_PAGE))
                .containsExactly("'''\npreformatted\nblock\n'''");
    }
}
