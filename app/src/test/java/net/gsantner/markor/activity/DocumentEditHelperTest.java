/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Map;

/**
 * Pure unit tests for {@link DocumentEditHelper}.
 * These tests have NO Android framework dependencies.
 */
public class DocumentEditHelperTest {

    // ===== shouldBlockSave =====

    @Test
    public void shouldBlockSave_blockWhenTextTooShortAndNotForced() {
        assertThat(DocumentEditHelper.shouldBlockSave(1, 2, false)).isTrue();
    }

    @Test
    public void shouldBlockSave_blockWhenTextEmpty() {
        assertThat(DocumentEditHelper.shouldBlockSave(0, 2, false)).isTrue();
    }

    @Test
    public void shouldBlockSave_allowWhenForceSaveEmpty() {
        assertThat(DocumentEditHelper.shouldBlockSave(0, 2, true)).isFalse();
        assertThat(DocumentEditHelper.shouldBlockSave(1, 2, true)).isFalse();
    }

    @Test
    public void shouldBlockSave_allowWhenTextLongEnough() {
        assertThat(DocumentEditHelper.shouldBlockSave(2, 2, false)).isFalse();
        assertThat(DocumentEditHelper.shouldBlockSave(100, 2, false)).isFalse();
    }

    @Test
    public void shouldBlockSave_allowWhenTextExactlyMinLength() {
        assertThat(DocumentEditHelper.shouldBlockSave(5, 5, false)).isFalse();
    }

    @Test
    public void shouldBlockSave_blockWhenTextOneBelowMinLength() {
        assertThat(DocumentEditHelper.shouldBlockSave(4, 5, false)).isTrue();
    }

    @Test
    public void shouldBlockSave_allowWhenForceSaveAndTextShort() {
        assertThat(DocumentEditHelper.shouldBlockSave(0, 100, true)).isFalse();
    }

    // ===== computeStartPosition =====

    @Test
    public void computeStartPosition_useLastEditPositionWhenNoLineNumber() {
        assertThat(DocumentEditHelper.computeStartPosition(false, 0, 42, 100)).isEqualTo(42);
    }

    @Test
    public void computeStartPosition_clampLastEditPositionToTextLength() {
        // lastEditPosition beyond text length should be clamped
        assertThat(DocumentEditHelper.computeStartPosition(false, 0, 200, 100)).isEqualTo(100);
    }

    @Test
    public void computeStartPosition_useEndOfTextWhenLineNumberNegative() {
        // Negative line number means "go to end"
        assertThat(DocumentEditHelper.computeStartPosition(true, -1, 42, 100)).isEqualTo(100);
    }

    @Test
    public void computeStartPosition_sentinelForLineNumberNavigation() {
        // Positive line number returns -1 sentinel (actual index needs CharSequence)
        assertThat(DocumentEditHelper.computeStartPosition(true, 5, 42, 100)).isEqualTo(-1);
    }

    @Test
    public void computeStartPosition_lineNumberZero() {
        // Line 0 still returns sentinel
        assertThat(DocumentEditHelper.computeStartPosition(true, 0, 42, 100)).isEqualTo(-1);
    }

    @Test
    public void computeStartPosition_lastEditPositionZero() {
        assertThat(DocumentEditHelper.computeStartPosition(false, 0, 0, 100)).isEqualTo(0);
    }

    @Test
    public void computeStartPosition_emptyText() {
        assertThat(DocumentEditHelper.computeStartPosition(false, 0, 0, 0)).isEqualTo(0);
        assertThat(DocumentEditHelper.computeStartPosition(true, -1, 0, 0)).isEqualTo(0);
    }

    // ===== shouldRestoreScroll =====

    @Test
    public void shouldRestoreScroll_restoreWhenHeightMatchesAndScrollPositive() {
        assertThat(DocumentEditHelper.shouldRestoreScroll(500, 500, 100)).isTrue();
    }

    @Test
    public void shouldRestoreScroll_skipWhenHeightChanged() {
        assertThat(DocumentEditHelper.shouldRestoreScroll(500, 600, 100)).isFalse();
    }

    @Test
    public void shouldRestoreScroll_skipWhenScrollIsZero() {
        assertThat(DocumentEditHelper.shouldRestoreScroll(500, 500, 0)).isFalse();
    }

    @Test
    public void shouldRestoreScroll_skipWhenLastHeightIsZero() {
        assertThat(DocumentEditHelper.shouldRestoreScroll(0, 0, 100)).isFalse();
    }

    @Test
    public void shouldRestoreScroll_skipWhenNegativeScroll() {
        assertThat(DocumentEditHelper.shouldRestoreScroll(500, 500, -1)).isFalse();
    }

    // ===== shouldRestoreViewScroll =====

    @Test
    public void shouldRestoreViewScroll_restoreWhenMatching() {
        assertThat(DocumentEditHelper.shouldRestoreViewScroll(500, 500, 100)).isTrue();
    }

    @Test
    public void shouldRestoreViewScroll_skipWhenScrollZero() {
        assertThat(DocumentEditHelper.shouldRestoreViewScroll(500, 500, 0)).isFalse();
    }

    @Test
    public void shouldRestoreViewScroll_skipWhenHeightDiffers() {
        assertThat(DocumentEditHelper.shouldRestoreViewScroll(500, 600, 100)).isFalse();
    }

    // ===== formatSearchResult =====

    @Test
    public void formatSearchResult_formatActiveMatchOfTotal() {
        assertThat(DocumentEditHelper.formatSearchResult(0, 5, true)).isEqualTo("1/5");
        assertThat(DocumentEditHelper.formatSearchResult(2, 5, true)).isEqualTo("3/5");
        assertThat(DocumentEditHelper.formatSearchResult(4, 5, true)).isEqualTo("5/5");
    }

    @Test
    public void formatSearchResult_emptyWhenNoMatches() {
        assertThat(DocumentEditHelper.formatSearchResult(0, 0, true)).isEmpty();
    }

    @Test
    public void formatSearchResult_emptyWhenNotDoneCounting() {
        assertThat(DocumentEditHelper.formatSearchResult(0, 5, false)).isEmpty();
    }

    @Test
    public void formatSearchResult_singleMatch() {
        assertThat(DocumentEditHelper.formatSearchResult(0, 1, true)).isEqualTo("1/1");
    }

    @Test
    public void formatSearchResult_emptyWhenNegativeMatches() {
        assertThat(DocumentEditHelper.formatSearchResult(0, -1, true)).isEmpty();
    }

    // ===== computeMenuVisibility =====

    @Test
    public void computeMenuVisibility_allEditItemsHiddenInPreview() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                true, true, false, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_UNDO)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_REDO)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_SAVE)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_SEARCH)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_SUBMENU_FORMAT)).isFalse();
    }

    @Test
    public void computeMenuVisibility_previewItemsHiddenInEditMode() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                true, false, false, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_EDIT)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_SEARCH_VIEW)).isFalse();
    }

    @Test
    public void computeMenuVisibility_editItemsVisibleInEditMode() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                true, false, false, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_UNDO)).isTrue();
        assertThat(map.get(DocumentEditHelper.MENU_REDO)).isTrue();
        assertThat(map.get(DocumentEditHelper.MENU_SAVE)).isTrue();
        assertThat(map.get(DocumentEditHelper.MENU_PREVIEW)).isTrue();
        assertThat(map.get(DocumentEditHelper.MENU_SEARCH)).isTrue();
    }

    @Test
    public void computeMenuVisibility_previewItemsVisibleInPreviewMode() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                true, true, false, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_EDIT)).isTrue();
        assertThat(map.get(DocumentEditHelper.MENU_SEARCH_VIEW)).isTrue();
    }

    @Test
    public void computeMenuVisibility_undoRedoHiddenForBinaryFiles() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                false, false, false, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_UNDO)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_REDO)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_SAVE)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_PREVIEW)).isFalse();
    }

    @Test
    public void computeMenuVisibility_undoRedoHiddenWhenHistoryDisabled() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                true, false, false, false, false);
        assertThat(map.get(DocumentEditHelper.MENU_UNDO)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_REDO)).isFalse();
        // Save should still be visible
        assertThat(map.get(DocumentEditHelper.MENU_SAVE)).isTrue();
    }

    @Test
    public void computeMenuVisibility_experimentalItemsHiddenByDefault() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                true, false, false, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_LOAD_EPUB)).isFalse();
    }

    @Test
    public void computeMenuVisibility_experimentalItemsVisibleWhenEnabled() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                true, false, true, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_LOAD_EPUB)).isTrue();
    }

    @Test
    public void computeMenuVisibility_shareAndToolsAvailableInBothModes() {
        Map<String, Boolean> editMode = DocumentEditHelper.computeMenuVisibility(
                true, false, false, false, true);
        Map<String, Boolean> previewMode = DocumentEditHelper.computeMenuVisibility(
                true, true, false, false, true);
        assertThat(editMode.get(DocumentEditHelper.MENU_SUBMENU_SHARE)).isTrue();
        assertThat(previewMode.get(DocumentEditHelper.MENU_SUBMENU_SHARE)).isTrue();
        assertThat(editMode.get(DocumentEditHelper.MENU_SUBMENU_TOOLS)).isTrue();
        assertThat(previewMode.get(DocumentEditHelper.MENU_SUBMENU_TOOLS)).isTrue();
    }

    @Test
    public void computeMenuVisibility_binaryFileHidesAllTextItems() {
        Map<String, Boolean> map = DocumentEditHelper.computeMenuVisibility(
                false, false, false, false, true);
        assertThat(map.get(DocumentEditHelper.MENU_SUBMENU_SHARE)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_SUBMENU_TOOLS)).isFalse();
        assertThat(map.get(DocumentEditHelper.MENU_SUBMENU_PER_FILE)).isFalse();
    }
}
