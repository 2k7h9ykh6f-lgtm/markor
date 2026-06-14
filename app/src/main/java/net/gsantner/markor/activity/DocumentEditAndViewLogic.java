/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import net.gsantner.markor.frontend.textview.TextViewUtils;

/**
 * Pure (Android-free) decision logic extracted from {@link DocumentEditAndViewFragment}.
 * <p>
 * The fragment mixes editing, preview switching, saving, external-change reload, rotation
 * restore and the unsaved-content guard. The branching that decides <em>what</em> should
 * happen is small but regression-prone, so it lives here where it can be unit tested with
 * plain JUnit. The fragment keeps the <em>side effects</em> (touching views, settings, disk).
 */
public final class DocumentEditAndViewLogic {

    private DocumentEditAndViewLogic() {
        throw new AssertionError();
    }

    /**
     * Where to place the cursor when a document first becomes visible.
     * Mirrors {@code DocumentEditAndViewFragment.onFragmentFirstTimeVisible}.
     *
     * @param text         current editor text (source of truth for line resolution)
     * @param hasLineNumber whether the launch intent requested a specific line
     * @param lineNumber   requested 0-based line; a negative value means "end of file"
     * @param fallbackPos  last saved edit position, used when no line was requested
     * @return a position clamped to {@code [0, text.length()]}
     */
    public static int getStartCursorPosition(final CharSequence text, final boolean hasLineNumber, final int lineNumber, final int fallbackPos) {
        final int len = text != null ? text.length() : 0;
        if (hasLineNumber) {
            // getIndexFromLineOffset already clamps an out-of-range line to the end of the text
            return lineNumber >= 0 ? TextViewUtils.getIndexFromLineOffset(text, lineNumber, 0) : len;
        }
        // The file may have shrunk since the position was stored, so clamp into range
        return Math.max(0, Math.min(fallbackPos, len));
    }

    public enum SaveAction {
        /** Editor matches the document; nothing needs to be written. */
        NOTHING_TO_SAVE,
        /** Content changed but is below the minimum length and the save was not forced. */
        BLOCKED_TOO_SHORT,
        /** Content changed and may be written to disk. */
        WRITE
    }

    /**
     * Decide what {@code saveDocument} should do, excluding the activity / storage / state
     * guards which require Android. Mirrors the body of
     * {@code DocumentEditAndViewFragment.saveDocument}.
     *
     * @param contentSame    whether the editor content equals the last persisted content
     * @param forceSaveEmpty whether the caller forces a save even for very short content
     * @param textLength     length of the editor content (treat null as 0)
     * @param minLength      minimum length required to overwrite a file when not forced
     */
    public static SaveAction getSaveAction(final boolean contentSame, final boolean forceSaveEmpty, final int textLength, final int minLength) {
        if (contentSame) {
            return SaveAction.NOTHING_TO_SAVE;
        }
        if (!forceSaveEmpty && textLength < minLength) {
            return SaveAction.BLOCKED_TOO_SHORT;
        }
        return SaveAction.WRITE;
    }

    /**
     * Whether the document is still dirty (unsaved) after a save attempt.
     * <p>
     * A successful write updates the stored content hash, clearing the dirty flag; a failed
     * write leaves the hash untouched so the change is still pending. A blocked (too short)
     * change was never written, so it also stays pending.
     */
    public static boolean remainsDirtyAfterSave(final SaveAction action, final boolean writeSucceeded) {
        switch (action) {
            case NOTHING_TO_SAVE:
                return false;
            case BLOCKED_TOO_SHORT:
                return true;
            case WRITE:
                return !writeSucceeded;
            default:
                return false;
        }
    }

    /**
     * Whether {@code loadDocument} should overwrite the editor with the on-disk content.
     * <p>
     * This is the guard that prevents an {@code onResume}/reload from clobbering unsaved edits:
     * we only replace the editor text when the file actually changed on disk <em>and</em> the
     * editor does not already hold that content. Switching to preview never replaces the editor
     * text, so a preview round-trip cannot lose content.
     *
     * @param fileChangedSinceLastLoad whether the backing file changed since the last load
     * @param editorMatchesFile        whether the editor already holds the file's content
     */
    public static boolean shouldReplaceEditorText(final boolean fileChangedSinceLastLoad, final boolean editorMatchesFile) {
        return fileChangedSinceLastLoad && !editorMatchesFile;
    }

    /**
     * Resolve whether the document should open in preview mode.
     * Mirrors the preview-mode selection in {@code DocumentEditAndViewFragment.onViewCreated}.
     * <p>
     * The launch flag only applies on a genuine first launch. After a configuration change
     * (rotation) {@code savedInstanceState} is non-null, so the persisted state always wins,
     * which keeps the edit/preview mode stable across rotation.
     *
     * @param hasArgs              whether launch arguments are present
     * @param isFirstLaunch        whether this is a first launch (no saved instance state)
     * @param argHasStartPreview   whether the launch arguments contain the preview flag
     * @param argStartPreview      value of the launch preview flag (if present)
     * @param persistedPreviewState last persisted preview state for this document
     */
    public static boolean resolvePreviewVisible(
            final boolean hasArgs,
            final boolean isFirstLaunch,
            final boolean argHasStartPreview,
            final boolean argStartPreview,
            final boolean persistedPreviewState
    ) {
        if (hasArgs && isFirstLaunch) {
            return argHasStartPreview ? argStartPreview : persistedPreviewState;
        }
        return persistedPreviewState;
    }
}
