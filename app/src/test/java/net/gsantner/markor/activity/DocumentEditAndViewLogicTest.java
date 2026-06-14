/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.activity.DocumentEditAndViewLogic.SaveAction;

import org.junit.Test;

/**
 * Unit tests for {@link DocumentEditAndViewLogic}, the pure decision logic behind the
 * document edit/view main flow. Each group maps to one of the regression-prone behaviours
 * of {@code DocumentEditAndViewFragment}.
 */
public class DocumentEditAndViewLogicTest {

    // A 3-line document. Newlines at index 5 and 11; length 17.
    // line 0 = [0..4]  (ends at 5)
    // line 1 = [6..10] (ends at 11)
    // line 2 = [12..16](ends at 17)
    private static final String DOC = "line0\nline1\nline2";

    private static final int MIN_LEN = 2; // GsContextUtils.TEXT_FILE_OVERWRITE_MIN_TEXT_LENGTH

    // ---------------------------------------------------------------------------------------------
    // Scenario 1: editing then switching to preview must not lose content.
    // Preview never replaces the editor text; a reload only replaces it when the file actually
    // changed on disk AND the editor does not already hold that content.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void editingThenPreview_fileUnchanged_neverReplacesEditor() {
        // After editing (disk unchanged), a reload triggered by resume/preview must not clobber edits.
        assertThat(DocumentEditAndViewLogic.shouldReplaceEditorText(false, false)).isFalse();
        assertThat(DocumentEditAndViewLogic.shouldReplaceEditorText(false, true)).isFalse();
    }

    @Test
    public void reload_replacesOnlyWhenFileChangedAndEditorDiffers() {
        // External change + editor differs -> adopt the new on-disk content.
        assertThat(DocumentEditAndViewLogic.shouldReplaceEditorText(true, false)).isTrue();
        // External change but editor already equals it -> nothing to do (avoid resetting cursor).
        assertThat(DocumentEditAndViewLogic.shouldReplaceEditorText(true, true)).isFalse();
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 2: a failed save must keep the document marked dirty (unsaved).
    // ---------------------------------------------------------------------------------------------

    @Test
    public void changedContent_isAWriteAndStaysDirtyWhenWriteFails() {
        final SaveAction action = DocumentEditAndViewLogic.getSaveAction(false, true, 10, MIN_LEN);
        assertThat(action).isEqualTo(SaveAction.WRITE);
        assertThat(DocumentEditAndViewLogic.remainsDirtyAfterSave(action, false)).isTrue();
    }

    @Test
    public void changedContent_becomesCleanWhenWriteSucceeds() {
        final SaveAction action = DocumentEditAndViewLogic.getSaveAction(false, true, 10, MIN_LEN);
        assertThat(DocumentEditAndViewLogic.remainsDirtyAfterSave(action, true)).isFalse();
    }

    @Test
    public void unchangedContent_isNothingToSaveAndNotDirty() {
        final SaveAction action = DocumentEditAndViewLogic.getSaveAction(true, false, 10, MIN_LEN);
        assertThat(action).isEqualTo(SaveAction.NOTHING_TO_SAVE);
        assertThat(DocumentEditAndViewLogic.remainsDirtyAfterSave(action, false)).isFalse();
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 3: opening a file via an external intent restores the cursor.
    // "Go to line N" lands at the END of line N (offset 0 from end), matching existing behaviour.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void externalIntent_withLineNumber_restoresCursorOnThatLine() {
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, true, 0, 0)).isEqualTo(5);
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, true, 1, 0)).isEqualTo(11);
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, true, 2, 0)).isEqualTo(17);
    }

    @Test
    public void externalIntent_negativeLineNumber_goesToEndOfFile() {
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, true, -1, 0)).isEqualTo(DOC.length());
    }

    @Test
    public void noLineNumber_usesLastEditPosition() {
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, false, -1, 8)).isEqualTo(8);
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 4: after a configuration change (rotation) the edit/preview mode stays consistent.
    // The launch flag only applies on a genuine first launch; otherwise the persisted state wins.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void firstLaunch_launchFlagWinsOverPersistedState() {
        // arg requests preview, persisted says edit -> open preview
        assertThat(DocumentEditAndViewLogic.resolvePreviewVisible(true, true, true, true, false)).isTrue();
        // arg requests edit, persisted says preview -> open edit
        assertThat(DocumentEditAndViewLogic.resolvePreviewVisible(true, true, true, false, true)).isFalse();
    }

    @Test
    public void firstLaunch_noLaunchFlag_usesPersistedState() {
        assertThat(DocumentEditAndViewLogic.resolvePreviewVisible(true, true, false, false, true)).isTrue();
        assertThat(DocumentEditAndViewLogic.resolvePreviewVisible(false, true, false, false, true)).isTrue();
    }

    @Test
    public void configurationChange_ignoresLaunchFlag_usesPersistedState() {
        // savedInstanceState present (isFirstLaunch == false): persisted state must win, not the stale arg.
        assertThat(DocumentEditAndViewLogic.resolvePreviewVisible(true, false, true, true, false)).isFalse();
        assertThat(DocumentEditAndViewLogic.resolvePreviewVisible(true, false, true, false, true)).isTrue();
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 5: empty and large files are handled without crashing.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void emptyContent_isBlockedUnlessForced_andStaysPending() {
        // New empty content, not forced -> won't overwrite, but the change is still pending.
        final SaveAction blocked = DocumentEditAndViewLogic.getSaveAction(false, false, 0, MIN_LEN);
        assertThat(blocked).isEqualTo(SaveAction.BLOCKED_TOO_SHORT);
        assertThat(DocumentEditAndViewLogic.remainsDirtyAfterSave(blocked, false)).isTrue();

        // Forced (e.g. user pressed save) -> empty content may be written.
        assertThat(DocumentEditAndViewLogic.getSaveAction(false, true, 0, MIN_LEN)).isEqualTo(SaveAction.WRITE);
    }

    @Test
    public void emptyAndNullText_cursorResolutionDoesNotThrow() {
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition("", true, 0, 0)).isEqualTo(0);
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition("", false, -1, 5)).isEqualTo(0);
        // Null text must not NPE and resolves to position 0.
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(null, true, 0, 0)).isEqualTo(0);
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(null, false, -1, 5)).isEqualTo(0);
    }

    @Test
    public void staleCursorPosition_isClampedIntoRange() {
        // File shrank between sessions: a too-large stored position is clamped to the end.
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, false, -1, 999)).isEqualTo(DOC.length());
        // Out-of-range requested line is clamped to the end as well (no crash).
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, true, 999, 0)).isEqualTo(DOC.length());
        // Negative stored position is clamped to the start.
        assertThat(DocumentEditAndViewLogic.getStartCursorPosition(DOC, false, -1, -3)).isEqualTo(0);
    }

    @Test
    public void largeDocument_isHandledWithoutCrashing() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("the quick brown fox jumps over the lazy dog\n");
        }
        final String big = sb.toString();

        // Cursor resolution stays within bounds for a line deep in the document.
        final int pos = DocumentEditAndViewLogic.getStartCursorPosition(big, true, 2500, 0);
        assertThat(pos).isBetween(0, big.length());

        // A large changed document is a normal write (no overflow / no special-casing).
        assertThat(DocumentEditAndViewLogic.getSaveAction(false, false, big.length(), MIN_LEN)).isEqualTo(SaveAction.WRITE);
    }
}
