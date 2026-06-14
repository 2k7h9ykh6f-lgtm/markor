/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import java.util.HashMap;
import java.util.Map;

/**
 * Extracted pure logic from {@link DocumentEditAndViewFragment} for testability.
 * All methods are static and have no Android framework dependencies.
 */
public final class DocumentEditHelper {

    private DocumentEditHelper() {
        // Utility class
    }

    /**
     * Determines whether a save should be blocked due to minimum length requirements.
     * Extracted from DocumentEditAndViewFragment.saveDocument().
     *
     * @param textLength     current text length in the editor
     * @param minLength      minimum text length required (GsContextUtils.TEXT_FILE_OVERWRITE_MIN_TEXT_LENGTH)
     * @param forceSaveEmpty whether the user explicitly requested save (manual save)
     * @return true if save should be blocked (text too short and not forced)
     */
    public static boolean shouldBlockSave(int textLength, int minLength, boolean forceSaveEmpty) {
        if (forceSaveEmpty) {
            return false;
        }
        return textLength < minLength;
    }

    /**
     * Computes the initial cursor position when opening a document.
     * Extracted from DocumentEditAndViewFragment.onFragmentFirstTimeVisible().
     *
     * @param hasLineNumber    whether a specific line number was provided (e.g. from intent)
     * @param lineNumber       the requested line number (0-based), or negative for end-of-text
     * @param lastEditPosition the last saved cursor position for this document
     * @param textLength       total length of the document text
     * @return the computed cursor position, or -1 if line number navigation should be used
     */
    public static int computeStartPosition(boolean hasLineNumber, int lineNumber, int lastEditPosition, int textLength) {
        if (hasLineNumber) {
            if (lineNumber >= 0) {
                // Return sentinel -1 to indicate "use line-based navigation"
                // The actual index computation requires the CharSequence (TextViewUtils.getIndexFromLineOffset)
                return -1;
            } else {
                // Negative line number means "go to end"
                return textLength;
            }
        } else {
            // No line number specified, restore last edit position
            // Clamp to valid range
            return Math.min(lastEditPosition, textLength);
        }
    }

    /**
     * Determines whether scroll position should be restored for the editor scroll view.
     * Extracted from DocumentEditAndViewFragment.onFragmentFirstTimeVisible().
     *
     * @param lastHeight    the previously saved view height
     * @param currentHeight the current view height
     * @param lastScrollY   the previously saved scroll Y position
     * @return true if scroll should be restored
     */
    public static boolean shouldRestoreScroll(int lastHeight, int currentHeight, int lastScrollY) {
        return lastHeight > 0 && lastHeight == currentHeight && lastScrollY > 0;
    }

    /**
     * Determines whether scroll position should be restored for the preview WebView.
     * Similar to editor scroll but with slightly different semantics.
     *
     * @param lastHeight    the previously saved view height
     * @param currentHeight the current view height
     * @param lastScrollY   the previously saved scroll Y position
     * @return true if scroll should be restored
     */
    public static boolean shouldRestoreViewScroll(int lastHeight, int currentHeight, int lastScrollY) {
        return lastScrollY > 0 && lastHeight == currentHeight;
    }

    /**
     * Formats the search result indicator for WebView search.
     * Extracted from DocumentEditAndViewFragment.bindWebViewSearchListener().
     *
     * @param activeMatchOrdinal 0-based index of the current match
     * @param numberOfMatches    total number of matches found
     * @param isDoneCounting     whether the search has completed counting
     * @return formatted string like "2/5", or empty string if no results or still counting
     */
    public static String formatSearchResult(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
        if (!isDoneCounting) {
            return "";
        }
        if (numberOfMatches <= 0) {
            return "";
        }
        return (activeMatchOrdinal + 1) + "/" + numberOfMatches;
    }

    /**
     * Key constants for the menu visibility map returned by {@link #computeMenuVisibility}.
     */
    public static final String MENU_UNDO = "undo";
    public static final String MENU_REDO = "redo";
    public static final String MENU_SAVE = "save";
    public static final String MENU_EDIT = "edit";
    public static final String MENU_PREVIEW = "preview";
    public static final String MENU_SEARCH = "search";
    public static final String MENU_SEARCH_VIEW = "search_view";
    public static final String MENU_SUBMENU_FORMAT = "submenu_format";
    public static final String MENU_SUBMENU_SHARE = "submenu_share";
    public static final String MENU_SUBMENU_TOOLS = "submenu_tools";
    public static final String MENU_SUBMENU_PER_FILE = "submenu_per_file";
    public static final String MENU_LOAD_EPUB = "load_epub";
    public static final String MENU_DEBUG_LOG = "debug_log";

    /**
     * Computes visibility of menu items based on document and UI state.
     * Extracted from DocumentEditAndViewFragment.onCreateOptionsMenu().
     *
     * @param isText             true if the document is a text file (not binary)
     * @param isPreviewVisible   true if preview (WebView) mode is active
     * @param isExperimental     true if experimental features are enabled
     * @param isSearchActive     true if text search is currently active in the editor
     * @param isHistoryEnabled   true if undo/redo history is enabled
     * @return map of menu item key to visibility boolean
     */
    public static Map<String, Boolean> computeMenuVisibility(
            boolean isText,
            boolean isPreviewVisible,
            boolean isExperimental,
            boolean isSearchActive,
            boolean isHistoryEnabled) {

        Map<String, Boolean> map = new HashMap<>();

        // Undo/Redo: visible only for text files in edit mode with history enabled
        map.put(MENU_UNDO, isText && !isPreviewVisible && isHistoryEnabled);
        map.put(MENU_REDO, isText && !isPreviewVisible && isHistoryEnabled);

        // Save: visible for text files in edit mode
        map.put(MENU_SAVE, isText && !isPreviewVisible);

        // Edit/Preview toggle
        map.put(MENU_EDIT, isText && isPreviewVisible);
        map.put(MENU_PREVIEW, isText && !isPreviewVisible);

        // Search
        map.put(MENU_SEARCH, isText && !isPreviewVisible);
        map.put(MENU_SEARCH_VIEW, isText && isPreviewVisible);

        // Format selection submenu
        map.put(MENU_SUBMENU_FORMAT, isText && !isPreviewVisible);

        // Share and tools (available in both modes for text)
        map.put(MENU_SUBMENU_SHARE, isText);
        map.put(MENU_SUBMENU_TOOLS, isText);
        map.put(MENU_SUBMENU_PER_FILE, isText);

        // Experimental features
        map.put(MENU_LOAD_EPUB, isExperimental);

        // Debug log (simplified — the actual condition also checks IS_DEBUG_ENABLED and activity type)
        map.put(MENU_DEBUG_LOG, !isPreviewVisible);

        return map;
    }
}
