/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link ShareIntoLogic} – covers share-text extraction,
 * URL sanitization, link detection and format-specific output.
 *
 * <h3>Scenarios covered</h3>
 * <ul>
 *   <li>ACTION_SEND text (plain, with subject, with URL)</li>
 *   <li>URL sanitization (UTM, Amazon, Google, social)</li>
 *   <li>Null / empty / edge-case inputs</li>
 *   <li>Todo.txt vs. other format output</li>
 * </ul>
 */
public class ShareIntoLogicTest {

    // ===================================================================
    //  extractShareText – ACTION_SEND plain text
    // ===================================================================

    @Test
    public void extractShareText_plainText_returnsText() {
        assertThat(ShareIntoLogic.extractShareText(null, "Hello World"))
                .isEqualTo("Hello World");
    }

    @Test
    public void extractShareText_nullText_returnsEmpty() {
        assertThat(ShareIntoLogic.extractShareText(null, null))
                .isEmpty();
    }

    @Test
    public void extractShareText_emptyText_returnsEmpty() {
        assertThat(ShareIntoLogic.extractShareText(null, ""))
                .isEmpty();
    }

    @Test
    public void extractShareText_whitespaceOnly_returnsEmpty() {
        assertThat(ShareIntoLogic.extractShareText(null, "   "))
                .isEmpty();
    }

    // ===================================================================
    //  extractShareText – ACTION_SEND with EXTRA_SUBJECT
    // ===================================================================

    @Test
    public void extractShareText_withSubjectAndUrl_prependsSubject() {
        String result = ShareIntoLogic.extractShareText(
                "My Page", "https://example.com");
        assertThat(result).startsWith("My Page ");
        assertThat(result).contains("https://example.com");
    }

    @Test
    public void extractShareText_subjectOnly_noUrl() {
        // When text is not a URL, subject is NOT prepended
        String result = ShareIntoLogic.extractShareText("Title", "plain text");
        assertThat(result).isEqualTo("plain text");
    }

    @Test
    public void extractShareText_subjectWithUrl_sanitizesUrl() {
        String result = ShareIntoLogic.extractShareText(
                "Cool Article",
                "https://example.com/page?utm_source=twitter&content=hello");
        assertThat(result).startsWith("Cool Article ");
        assertThat(result).doesNotContain("utm_source");
    }

    // ===================================================================
    //  extractShareText – ACTION_SEND with URL
    // ===================================================================

    @Test
    public void extractShareText_urlWithTracking_stripsTracking() {
        String result = ShareIntoLogic.extractShareText(
                null,
                "https://example.com/page?utm_source=twitter&utm_medium=social");
        assertThat(result).doesNotContain("utm_source");
        assertThat(result).doesNotContain("utm_medium");
    }

    @Test
    public void extractShareText_cleanUrl_unchanged() {
        String url = "https://example.com/page?q=search";
        assertThat(ShareIntoLogic.extractShareText(null, url))
                .isEqualTo(url);
    }

    @Test
    public void extractShareText_httpUrl_recognized() {
        String result = ShareIntoLogic.extractShareText(
                null, "http://example.com/path?ref=homepage");
        assertThat(result).doesNotContain("ref=homepage");
    }

    // ===================================================================
    //  sanitizeUrl – tracking parameter removal
    // ===================================================================

    @Test
    public void sanitizeUrl_removesUtmParams() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?utm_source=test&utm_campaign=spring"))
                .doesNotContain("utm_source")
                .doesNotContain("utm_campaign");
    }

    @Test
    public void sanitizeUrl_removesGclid() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?gclid=abc123&q=hello"))
                .doesNotContain("gclid");
    }

    @Test
    public void sanitizeUrl_removesFbclid() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?fbclid=xyz&real=value"))
                .doesNotContain("fbclid");
    }

    @Test
    public void sanitizeUrl_removesMsclkid() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?msclkid=bingad"))
                .doesNotContain("msclkid");
    }

    @Test
    public void sanitizeUrl_amazonRemovesQidAndSr() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://amazon.com/dp/B001?ref=sr_1&qid=1234&sr=8-1"))
                .doesNotContain("qid")
                .doesNotContain("sr=");
    }

    @Test
    public void sanitizeUrl_nonAmazonKeepsQid() {
        // qid is only stripped for Amazon URLs
        String url = "https://example.com?qid=1234";
        assertThat(ShareIntoLogic.sanitizeUrl(url)).contains("qid");
    }

    @Test
    public void sanitizeUrl_nullInput_returnsEmpty() {
        assertThat(ShareIntoLogic.sanitizeUrl(null)).isEmpty();
    }

    @Test
    public void sanitizeUrl_noTrackingParams_unchanged() {
        String url = "https://example.com/page?name=value&foo=bar";
        assertThat(ShareIntoLogic.sanitizeUrl(url)).isEqualTo(url);
    }

    @Test
    public void sanitizeUrl_removesRefParam() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?ref=homepage&real=data"))
                .doesNotContain("ref=homepage");
    }

    @Test
    public void sanitizeUrl_removesSourceParam() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?source=newsletter&topic=tech"))
                .doesNotContain("source=newsletter");
    }

    @Test
    public void sanitizeUrl_removesSiParam() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?si=shareid&content=hello"))
                .doesNotContain("si=shareid");
    }

    @Test
    public void sanitizeUrl_removesPartnerParam() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?partner=acme&data=real"))
                .doesNotContain("partner=acme");
    }

    @Test
    public void sanitizeUrl_removesPromoParam() {
        assertThat(ShareIntoLogic.sanitizeUrl(
                "https://example.com?promo=summer&item=shoe"))
                .doesNotContain("promo=summer");
    }

    // ===================================================================
    //  hasLinks – URL detection in multi-line text
    // ===================================================================

    @Test
    public void hasLinks_nullText_returnsFalse() {
        assertThat(ShareIntoLogic.hasLinks(null)).isFalse();
    }

    @Test
    public void hasLinks_emptyText_returnsFalse() {
        assertThat(ShareIntoLogic.hasLinks("")).isFalse();
    }

    @Test
    public void hasLinks_textWithUrl_returnsTrue() {
        assertThat(ShareIntoLogic.hasLinks(
                "Check this out https://example.com/page")).isTrue();
    }

    @Test
    public void hasLinks_textWithHttpUrl_returnsTrue() {
        assertThat(ShareIntoLogic.hasLinks(
                "Visit http://example.com for more")).isTrue();
    }

    @Test
    public void hasLinks_textWithoutUrl_returnsFalse() {
        assertThat(ShareIntoLogic.hasLinks("Just some plain text here")).isFalse();
    }

    @Test
    public void hasLinks_multiLineWithUrl_returnsTrue() {
        assertThat(ShareIntoLogic.hasLinks(
                "Line 1\nLine 2 https://example.com\nLine 3")).isTrue();
    }

    // ===================================================================
    //  getLinkTitle – title extraction from URL
    // ===================================================================

    @Test
    public void getLinkTitle_normalUrl_returnsHost() {
        assertThat(ShareIntoLogic.getLinkTitle("https://example.com/page"))
                .isEqualTo("example.com");
    }

    @Test
    public void getLinkTitle_urlWithPort_returnsHostWithPort() {
        assertThat(ShareIntoLogic.getLinkTitle("https://example.com:8080/page"))
                .isEqualTo("example.com:8080");
    }

    @Test
    public void getLinkTitle_nullInput_returnsEmpty() {
        assertThat(ShareIntoLogic.getLinkTitle(null)).isEmpty();
    }

    @Test
    public void getLinkTitle_notAUrl_returnsEmpty() {
        assertThat(ShareIntoLogic.getLinkTitle("not a url")).isEmpty();
    }

    // ===================================================================
    //  formatShareForTarget – format-specific output
    // ===================================================================

    @Test
    public void formatShareForTarget_todoTxt_prependsDate() {
        String result = ShareIntoLogic.formatShareForTarget(
                "Buy milk", true, "2025-01-15");
        assertThat(result).startsWith("2025-01-15 ");
        assertThat(result).contains("Buy milk");
    }

    @Test
    public void formatShareForTarget_todoTxt_collapsesNewlines() {
        String result = ShareIntoLogic.formatShareForTarget(
                "Line1\nLine2\nLine3", true, "2025-01-15");
        assertThat(result).doesNotContain("\n");
        assertThat(result).isEqualTo("2025-01-15 Line1 Line2 Line3");
    }

    @Test
    public void formatShareForTarget_markdown_prependsNewline() {
        String result = ShareIntoLogic.formatShareForTarget(
                "# Heading", false, "2025-01-15");
        assertThat(result).startsWith("\n");
        assertThat(result).isEqualTo("\n# Heading");
    }

    @Test
    public void formatShareForTarget_plaintext_prependsNewline() {
        String result = ShareIntoLogic.formatShareForTarget(
                "Some note", false, "2025-01-15");
        assertThat(result).isEqualTo("\nSome note");
    }

    // ===================================================================
    //  Edge cases – ACTION_SEND_MULTIPLE / unusual inputs
    // ===================================================================

    @Test
    public void extractShareText_subjectOnlyNullText_returnsEmpty() {
        // When text is null but subject exists, the URL-match branch is skipped
        // and the empty link is returned
        assertThat(ShareIntoLogic.extractShareText("Title", null))
                .isEmpty();
    }

    @Test
    public void extractShareText_urlWithFragment_preserved() {
        String url = "https://example.com/page#section";
        String result = ShareIntoLogic.extractShareText(null, url);
        assertThat(result).contains("#section");
    }

    @Test
    public void extractShareText_urlWithQueryAndFragment_sanitizesButPreservesFragment() {
        String result = ShareIntoLogic.extractShareText(
                null, "https://example.com?utm_source=test#section");
        assertThat(result).doesNotContain("utm_source");
        assertThat(result).contains("#section");
    }
}
