package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link TodoTxtTask#sortTasks(List, String, boolean)} and
 * {@link TodoTxtTask.SttTaskSimpleComparator}.
 *
 * <p>Note: BY_CONTEXT and BY_PROJECT sort orders use android.text.TextUtils.join
 * which is not available in plain JUnit tests, so they are excluded here.</p>
 */
public class TodoTxtSortingTests {

    // ── Done tasks always at bottom ───────────────────────────────────────

    @Test
    public void doneTasksPushedToBottom_byPriority() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false,
                "x done low",
                "(A) pending high",
                "x done medium",
                "(B) pending medium");

        assertThat(sorted).containsExactly(
                "(A) pending high",
                "(B) pending medium",
                "x done low",
                "x done medium");
    }

    @Test
    public void doneTasksPushedToBottom_byLine() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_LINE, false,
                "x zzz done",
                "aaa pending",
                "x aaa done",
                "zzz pending");

        assertThat(sorted).containsExactly(
                "aaa pending",
                "zzz pending",
                "x aaa done",
                "x zzz done");
    }

    @Test
    public void doneTasksPushedToBottom_descending() {
        // Even with descending order, done tasks stay at bottom
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, true,
                "x done",
                "(A) high",
                "(C) low",
                "x done 2");

        assertThat(sorted).containsExactly(
                "(C) low",
                "(A) high",
                "x done",
                "x done 2");
    }

    // ── BY_PRIORITY ───────────────────────────────────────────────────────

    @Test
    public void sortByPriorityAscending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false,
                "(C) low",
                "(A) high",
                "(B) medium",
                "no priority");

        // A < B < C < PRIORITY_NONE (which is '~')
        assertThat(sorted).containsExactly(
                "(A) high",
                "(B) medium",
                "(C) low",
                "no priority");
    }

    @Test
    public void sortByPriorityDescending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, true,
                "(C) low",
                "(A) high",
                "(B) medium",
                "no priority");

        // PRIORITY_NONE (which is '~') > C > B > A
        assertThat(sorted).containsExactly(
                "no priority",
                "(C) low",
                "(B) medium",
                "(A) high");
    }

    @Test
    public void sortByPriority_sameLetter_stable() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false,
                "(A) zebra",
                "(A) alpha");

        // Same priority → tie-break by due date (both empty) → then by priority (same)
        // Result depends on String.compareTo of the line, which for equal priority
        // would compare the rest. But the comparator resolves ties by due date then priority.
        // With both empty, the comparator returns 0, so order is stable (original order).
        assertThat(sorted).containsExactly("(A) zebra", "(A) alpha");
    }

    // ── BY_CREATION_DATE ──────────────────────────────────────────────────

    @Test
    public void sortByCreationDateAscending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE, false,
                "2024-03-01 march task",
                "2024-01-01 january task",
                "2024-06-01 june task",
                "no date task");

        // Dates first (chronological), then no-date tasks
        assertThat(sorted).containsExactly(
                "2024-01-01 january task",
                "2024-03-01 march task",
                "2024-06-01 june task",
                "no date task");
    }

    @Test
    public void sortByCreationDateDescending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE, true,
                "2024-03-01 march task",
                "2024-01-01 january task",
                "2024-06-01 june task",
                "no date task");

        assertThat(sorted).containsExactly(
                "no date task",
                "2024-06-01 june task",
                "2024-03-01 march task",
                "2024-01-01 january task");
    }

    // ── BY_DUE_DATE ───────────────────────────────────────────────────────

    @Test
    public void sortByDueDateAscending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_DUE_DATE, false,
                "task C due:2024-06-01",
                "task A due:2024-01-01",
                "task B due:2024-03-01",
                "no due date task");

        assertThat(sorted).containsExactly(
                "task A due:2024-01-01",
                "task B due:2024-03-01",
                "task C due:2024-06-01",
                "no due date task");
    }

    @Test
    public void sortByDueDateDescending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_DUE_DATE, true,
                "task C due:2024-06-01",
                "task A due:2024-01-01",
                "task B due:2024-03-01",
                "no due date task");

        assertThat(sorted).containsExactly(
                "no due date task",
                "task C due:2024-06-01",
                "task B due:2024-03-01",
                "task A due:2024-01-01");
    }

    // ── BY_LINE (natural sort) ────────────────────────────────────────────

    @Test
    public void sortByLineAscending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_LINE, false,
                "cherry",
                "apple",
                "banana");

        assertThat(sorted).containsExactly("apple", "banana", "cherry");
    }

    @Test
    public void sortByLineDescending() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_LINE, true,
                "cherry",
                "apple",
                "banana");

        assertThat(sorted).containsExactly("cherry", "banana", "apple");
    }

    @Test
    public void sortByLineCaseInsensitive() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_LINE, false,
                "Banana",
                "apple",
                "Cherry");

        assertThat(sorted).containsExactly("apple", "Banana", "Cherry");
    }

    // ── BY_DESCRIPTION ────────────────────────────────────────────────────

    @Test
    public void sortByDescription() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_DESCRIPTION, false,
                "(A) zebra task +proj",
                "(B) alpha task @ctx",
                "middle task");

        // Description strips priority, projects, contexts, etc.
        assertThat(sorted.get(0)).contains("alpha");
        assertThat(sorted.get(2)).contains("zebra");
    }

    // ── Tie-breaking ──────────────────────────────────────────────────────

    @Test
    public void tieBreakByDueDate() {
        // Same creation date, different due dates
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE, false,
                "2024-01-01 task B due:2024-12-01",
                "2024-01-01 task A due:2024-06-01");

        // Tie on creation date → resolve by due date
        assertThat(sorted).containsExactly(
                "2024-01-01 task A due:2024-06-01",
                "2024-01-01 task B due:2024-12-01");
    }

    @Test
    public void tieBreakByDueDateThenPriority() {
        // Same creation date, same due date, different priorities
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE, false,
                "(B) 2024-01-01 task due:2024-06-01",
                "(A) 2024-01-01 task due:2024-06-01");

        // Tie on creation date → tie on due date → resolve by priority
        assertThat(sorted).containsExactly(
                "(A) 2024-01-01 task due:2024-06-01",
                "(B) 2024-01-01 task due:2024-06-01");
    }

    // ── Mixed: done + priority + dates ────────────────────────────────────

    @Test
    public void mixedScenario() {
        List<String> sorted = TodoTxtTestHelper.sortedLines(
                TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false,
                "x 2024-06-01 (A) 2024-01-01 done high",
                "(C) 2024-03-01 pending low",
                "(A) 2024-02-01 pending high",
                "x done no priority",
                "(B) pending medium");

        // Done at bottom (2 items), then A < B < C
        assertThat(sorted).containsExactly(
                "(A) 2024-02-01 pending high",
                "(B) pending medium",
                "(C) 2024-03-01 pending low",
                "x 2024-06-01 (A) 2024-01-01 done high",
                "x done no priority");
    }
}
