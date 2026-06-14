package net.gsantner.markor.format.todotxt;

import static net.gsantner.markor.format.todotxt.TodoTxtTestHelper.assertLineMatches;
import static net.gsantner.markor.format.todotxt.TodoTxtTestHelper.assertQueryMatches;
import static net.gsantner.markor.format.todotxt.TodoTxtTestHelper.filterLines;
import static net.gsantner.markor.format.todotxt.TodoTxtTestHelper.tasks;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.List;

/**
 * Table-driven tests for {@link TodoTxtFilter#isMatchQuery(TodoTxtTask, CharSequence)}.
 *
 * Each section covers a distinct query syntax element or combination.
 * Every case specifies the full task list, the query, and the expected result.
 */
public class TodoTxtQueryMatchingTests {

    // ══════════════════════════════════════════════════════════════════════
    //  1. Empty / whitespace query
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void emptyQueryMatchesNothing() {
        // An empty query produces an empty expression → evaluateExpression throws → false
        assertLineMatches("(A) task", "", false);
    }

    @Test
    public void whitespaceOnlyQueryMatchesNothing() {
        assertLineMatches("(A) task", "   ", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  2. Plain-text search (case-insensitive)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void plainTextMatchCaseInsensitive() {
        assertLineMatches("(A) Buy some Milk", "milk", true);
        assertLineMatches("(A) Buy some Milk", "MILK", true);
        assertLineMatches("(A) Buy some Milk", "MiLk", true);
    }

    @Test
    public void plainTextNoMatch() {
        assertLineMatches("(A) Buy milk", "bread", false);
    }

    @Test
    public void plainTextPartialWord() {
        assertLineMatches("Buy groceries at the store", "groc", true);
    }

    @Test
    public void plainTextMatchInDoneTask() {
        assertLineMatches("x 2024-01-15 Buy milk", "milk", true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  3. Context matching (@context)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void contextExactMatch() {
        assertLineMatches("task @home", "@home", true);
    }

    @Test
    public void contextNoMatch() {
        assertLineMatches("task @work", "@home", false);
    }

    @Test
    public void contextIsCaseSensitive() {
        // Context matching uses List.contains which is case-sensitive
        assertLineMatches("task @Home", "@home", false);
        assertLineMatches("task @home", "@Home", false);
    }

    @Test
    public void anyContext() {
        // bare '@' matches any task that has at least one context
        assertLineMatches("task @home", "@", true);
        assertLineMatches("task @work", "@", true);
        assertLineMatches("plain task", "@", false);
    }

    @Test
    public void contextAmongMultiple() {
        assertLineMatches("task @home @work @errands", "@work", true);
        assertLineMatches("task @home @work @errands", "@gym", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  4. Project matching (+project)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void projectExactMatch() {
        assertLineMatches("task +groceries", "+groceries", true);
    }

    @Test
    public void projectNoMatch() {
        assertLineMatches("task +work", "+groceries", false);
    }

    @Test
    public void projectIsCaseSensitive() {
        assertLineMatches("task +Work", "+work", false);
        assertLineMatches("task +work", "+Work", false);
    }

    @Test
    public void anyProject() {
        assertLineMatches("task +proj", "+", true);
        assertLineMatches("plain task", "+", false);
    }

    @Test
    public void projectAmongMultiple() {
        assertLineMatches("task +projA +projB +projC", "+projB", true);
        assertLineMatches("task +projA +projB +projC", "+projD", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  5. Priority matching (pri:X / pri)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void priorityExactMatch() {
        assertLineMatches("(A) task", "pri:A", true);
        assertLineMatches("(B) task", "pri:B", true);
        assertLineMatches("(Z) task", "pri:Z", true);
    }

    @Test
    public void priorityNoMatch() {
        assertLineMatches("(A) task", "pri:B", false);
    }

    @Test
    public void priorityAnyKeyword() {
        assertLineMatches("(A) task", "pri", true);
        assertLineMatches("(C) task", "pri", true);
        assertLineMatches("task without priority", "pri", false);
    }

    @Test
    public void priorityInvalidFormat() {
        // pri:AB (too long) → falls to false
        assertLineMatches("(A) task", "pri:AB", false);
        // pri: (too short, length 4 not 5)
        assertLineMatches("(A) task", "pri:", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  6. Done matching
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void doneKeywordMatchesDoneTask() {
        assertLineMatches("x completed task", "done", true);
        assertLineMatches("X completed task", "done", true);
    }

    @Test
    public void doneKeywordDoesNotMatchPending() {
        assertLineMatches("(A) pending task", "done", false);
    }

    @Test
    public void notDoneKeyword() {
        assertLineMatches("(A) pending task", "!done", true);
        assertLineMatches("x completed task", "!done", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  7. Due date matching (due, due=, due<, due>)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void dueAnyMatchesTaskWithDueDate() {
        assertLineMatches("task due:2024-06-15", "due", true);
        assertLineMatches("plain task", "due", false);
    }

    @Test
    public void dueOverdueMatchesPastDate() {
        assertLineMatches("task due:2000-01-01", "due<", true);
        assertLineMatches("task due:9999-12-31", "due<", false);
    }

    @Test
    public void dueFutureMatchesFutureDate() {
        assertLineMatches("task due:9999-12-31", "due>", true);
        assertLineMatches("task due:2000-01-01", "due>", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  8. NOT operator (!)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void notContext() {
        assertLineMatches("task @home", "!@work", true);
        assertLineMatches("task @work", "!@work", false);
    }

    @Test
    public void notProject() {
        assertLineMatches("task +projA", "!+projB", true);
        assertLineMatches("task +projB", "!+projB", false);
    }

    @Test
    public void notAnyContext() {
        assertLineMatches("plain task", "!@", true);
        assertLineMatches("task @home", "!@", false);
    }

    @Test
    public void notAnyProject() {
        assertLineMatches("plain task", "!+", true);
        assertLineMatches("task +proj", "!+", false);
    }

    @Test
    public void notPlainText() {
        assertLineMatches("Buy milk", "!bread", true);
        assertLineMatches("Buy bread", "!bread", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  9. AND operator (&)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void andBothTrue() {
        assertLineMatches("(A) task +work", "pri:A & +work", true);
    }

    @Test
    public void andOneTrue() {
        assertLineMatches("(A) task +home", "pri:A & +work", false);
    }

    @Test
    public void andBothFalse() {
        assertLineMatches("task +home", "pri:A & +work", false);
    }

    @Test
    public void andMultiple() {
        assertLineMatches("(A) task +work @office", "pri:A & +work & @office", true);
        assertLineMatches("(A) task +work @home", "pri:A & +work & @office", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  10. OR operator (|)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void orBothTrue() {
        assertLineMatches("(A) task +work", "pri:A | +work", true);
    }

    @Test
    public void orOneTrue() {
        assertLineMatches("(A) task +home", "pri:A | +work", true);
    }

    @Test
    public void orBothFalse() {
        assertLineMatches("task +home", "pri:A | +work", false);
    }

    @Test
    public void orMultiple() {
        assertLineMatches("(C) task", "pri:A | pri:B | pri:C", true);
        assertLineMatches("(D) task", "pri:A | pri:B | pri:C", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  11. Grouping with parentheses
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void groupingOrInParens() {
        assertLineMatches("(A) task +work", "(pri:A | pri:B) & +work", true);
        assertLineMatches("(A) task +home", "(pri:A | pri:B) & +work", false);
        assertLineMatches("(B) task +work", "(pri:A | pri:B) & +work", true);
    }

    @Test
    public void groupingAndInParens() {
        assertLineMatches("(A) task +work @office", "(+work & @office) | pri:B", true);
        assertLineMatches("(B) task +home", "(+work & @office) | pri:B", true);
        assertLineMatches("(C) task +home", "(+work & @office) | pri:B", false);
    }

    @Test
    public void nestedGrouping() {
        // Test nested grouping without consecutive )) which the preprocessor handles poorly
        assertLineMatches("(A) task +work @office",
                "(pri:A | pri:B) & (+work | +home) & @office", true);
        assertLineMatches("(C) task +work @office",
                "(pri:A | pri:B) & (+work | +home) & @office", false);
        assertLineMatches("(A) task +work @gym",
                "(pri:A | pri:B) & (+work | +home) & @office", false);
    }

    @Test
    public void notWithGrouping() {
        assertLineMatches("task +home", "!(pri:A | pri:B) & +home", true);
        assertLineMatches("(A) task +home", "!(pri:A | pri:B) & +home", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  12. Operator precedence (& binds tighter than |)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void operatorEvaluationIsLeftToRight() {
        // The evaluator processes operators strictly left-to-right, no precedence.
        // "pri:A | pri:B & +work" means ((pri:A | pri:B) & +work)
        // Task (A) without +work: ((T | F) & F) = (T & F) = F
        assertLineMatches("(A) task", "pri:A | pri:B & +work", false);
        // Task (A) with +work: ((T | F) & T) = (T & T) = T
        assertLineMatches("(A) task +work", "pri:A | pri:B & +work", true);
        // Task (B) with +work: ((F | T) & T) = (T & T) = T
        assertLineMatches("(B) task +work", "pri:A | pri:B & +work", true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  13. Combined queries — realistic scenarios
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void combinedPriorityAndContextAndNotDone() {
        List<TodoTxtTask> allTasks = tasks(
                "(A) Buy milk +groceries @store",
                "x 2024-01-15 (A) Buy bread +groceries @store",
                "(B) Buy eggs +groceries @store",
                "Call mom @home",
                "(A) Write report +work @office");

        // Note: "x 2024-01-15 (A)..." is done (x at start), but isDone() checks for x/X at
        // line start. The priority in a done task appears after the completion date.
        // "pri:A & !done" should match only non-done tasks with priority A.
        assertQueryMatches(allTasks, "pri:A & !done",
                "(A) Buy milk +groceries @store",
                "(A) Write report +work @office");
    }

    @Test
    public void combinedMultipleContextsOr() {
        List<TodoTxtTask> allTasks = tasks(
                "task @home",
                "task @work",
                "task @gym",
                "plain task");

        assertQueryMatches(allTasks, "@home | @work",
                "task @home",
                "task @work");
    }

    @Test
    public void combinedMultipleProjectsAnd() {
        List<TodoTxtTask> allTasks = tasks(
                "task +projA +projB",
                "task +projA",
                "task +projB",
                "task +projA +projB +projC");

        assertQueryMatches(allTasks, "+projA & +projB",
                "task +projA +projB",
                "task +projA +projB +projC");
    }

    @Test
    public void combinedDueAndPriority() {
        List<TodoTxtTask> allTasks = tasks(
                "(A) urgent task due:2000-01-01",
                "(B) task due:2000-01-01",
                "(A) task due:9999-12-31",
                "task due:2000-01-01");

        assertQueryMatches(allTasks, "pri:A & due<",
                "(A) urgent task due:2000-01-01");
    }

    @Test
    public void filterOutDoneTasks() {
        List<TodoTxtTask> allTasks = tasks(
                "(A) pending",
                "x done task",
                "another pending",
                "X another done");

        assertQueryMatches(allTasks, "!done",
                "(A) pending",
                "another pending");
    }

    @Test
    public void doneTasksOnly() {
        List<TodoTxtTask> allTasks = tasks(
                "(A) pending",
                "x done task",
                "another pending",
                "X another done");

        assertQueryMatches(allTasks, "done",
                "x done task",
                "X another done");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  14. Special characters in task text
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void specialCharsInTaskText() {
        TodoTxtTask task = new TodoTxtTask("(A) Fix bug #123 in (legacy) code +project @work");
        assertThat(TodoTxtFilter.isMatchQuery(task, "bug")).isTrue();
        assertThat(TodoTxtFilter.isMatchQuery(task, "#123")).isTrue();
        assertThat(TodoTxtFilter.isMatchQuery(task, "+project")).isTrue();
        assertThat(TodoTxtFilter.isMatchQuery(task, "@work")).isTrue();
    }

    @Test
    public void parenthesesInTaskTextDoNotBreakQuery() {
        // Parentheses in the task text should not interfere with query parsing
        TodoTxtTask task = new TodoTxtTask("Review (draft) document +writing");
        assertThat(TodoTxtFilter.isMatchQuery(task, "draft")).isTrue();
        assertThat(TodoTxtFilter.isMatchQuery(task, "+writing")).isTrue();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  15. Malformed / edge-case queries
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void danglingNotReturnsFalse() {
        // "!" alone → expression is "!" which is malformed → false
        assertLineMatches("any task", "!", false);
    }

    @Test
    public void danglingAndReturnsFalse() {
        // "task &" → malformed → false
        assertLineMatches("any task", "milk &", false);
    }

    @Test
    public void danglingOrReturnsFalse() {
        assertLineMatches("any task", "milk |", false);
    }

    @Test
    public void unmatchedOpenParenReturnsFalse() {
        assertLineMatches("any task", "(pri:A", false);
    }

    @Test
    public void unmatchedCloseParenReturnsFalse() {
        assertLineMatches("any task", "pri:A)", false);
    }

    @Test
    public void doubleNotWithSpaces() {
        // The preprocessor requires spaces between syntax chars.
        // "! !pri:A" properly negates twice: !(!pri:A)
        assertLineMatches("(A) task", "! !pri:A", true);
        assertLineMatches("no priority", "! !pri:A", false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  16. Table-driven: comprehensive filter scenarios
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void complexQueryFromExistingTest() {
        // Same query as in the original ParseQuery test, but via isMatchQuery
        String query = "(pri:A | pri:B | pri:C) & !+work & due< & due> | !+ | @";

        // Task with priority A and +work → first subexpression: T & !T → F
        //   +work → !+work = F → whole AND chain is F
        //   !+ = !T = F, @ = F → F | F | F = F
        assertLineMatches("(A) 2000-01-01 go to +work", query, false);

        // Task with +work and due:2000-01-01 (overdue)
        //   (F|T|F) & !T → T & F = F
        //   due< = T, due> = F → T & F = F
        //   Overall: F | F | !T | F = F | F | F | F = F
        assertLineMatches("(B) 2000-01-01 go to +work due:2000-01-01", query, false);

        // Task with @work and due:9999-01-01 (future) and no project
        //   (F|F|F) = F → first part of AND chain is F
        //   !+ = !F = T → second part is T
        //   due< = F, due> = T → F & T = F
        //   Overall: F & F | !F | T → F | T | T = T
        assertLineMatches("(D) 2000-01-01 go to @work due:9999-01-01", query, true);
    }

    @Test
    public void filterPreservesOrderFromTaskList() {
        List<TodoTxtTask> allTasks = tasks(
                "z task @home",
                "a task @home",
                "m task @home");

        // Filtering should preserve the original order from the task list
        List<String> result = filterLines(allTasks, "@home");
        assertThat(result).containsExactly("z task @home", "a task @home", "m task @home");
    }

    @Test
    public void mixedContextAndProjectQuery() {
        List<TodoTxtTask> allTasks = tasks(
                "task +projA @home",
                "task +projA @work",
                "task +projB @home",
                "task +projB @work",
                "task @home",
                "task +projA");

        assertQueryMatches(allTasks, "+projA & @home",
                "task +projA @home");

        assertQueryMatches(allTasks, "+projA | @home",
                "task +projA @home",
                "task +projA @work",
                "task +projB @home",
                "task @home",
                "task +projA");

        assertQueryMatches(allTasks, "!+ & !@");
        // No task without both project and context → empty result
    }

    @Test
    public void noMatchReturnsEmpty() {
        List<TodoTxtTask> allTasks = tasks(
                "(A) task +work",
                "(B) task +home");

        assertQueryMatches(allTasks, "pri:C & +gym");
        // No expected lines = empty result
    }

    @Test
    public void allMatchReturnsAll() {
        List<TodoTxtTask> allTasks = tasks(
                "(A) task",
                "(B) task",
                "task");

        // "task" is plain text that appears in all lines
        assertQueryMatches(allTasks, "task",
                "(A) task",
                "(B) task",
                "task");
    }
}
