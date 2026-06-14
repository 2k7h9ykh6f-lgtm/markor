package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TodoTxtQuerySyntaxTests {

    private String strip(final String in) {
        return in.replace(" ", "");
    }

    @Test
    public void ParseQuery() {
        final String query = "(pri:A | pri:B | pri:C) & !+work & due< & due> | !+ | @";

        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(A) 2000-01-01 go to +work"), query))
                .isEqualTo(strip("(T | F | F) & !T & F & F | !T | F"));

        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(B) 2000-01-01 go to +work due:2000-01-01"), query))
                .isEqualTo(strip("(F | T | F) & !T & T & F | !T | F"));

        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(D) 2000-01-01 go to @work due:9999-01-01"), query))
                .isEqualTo(strip("(F | F | F) & !F & F & T| !F | T"));
    }

    @Test
    public void EvaluateExpressionTest() {
        assertThat(TodoTxtFilter.evaluateExpression(strip("T"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!T"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("(!F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("(!(F))"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(!(F))"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T | F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T & F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T | T | T | T | F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("F | F | F | F | T"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(T | F) & (T | F)"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(T | F) | (T | F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(T | F | F) & (T | F) | (F & (!T) | T)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!!!!!!F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T | T | T & F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("F & F & F | T & T"))).isEqualTo(true);
    }

    // ---------------------------------------------------------------------------------------------
    // Table-driven tests
    //
    // Each case explicitly states its input task(s), the query, and the expected result (and, for
    // the list cases, the expected order). The helpers near the bottom keep the tables free of
    // boilerplate. Every assertion goes through android-free code paths (isMatchQuery and sortTasks
    // with BY_PRIORITY) so the suite runs as a plain JVM unit test without Robolectric.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void queryMatching_priority() {
        for (final MatchCase tc : Arrays.asList(
                mc("has any priority", "(A) write report", "pri", true),
                mc("no priority -> pri false", "buy milk", "pri", false),
                mc("exact priority A", "(A) write report", "pri:A", true),
                mc("priority A is not B", "(A) write report", "pri:B", false),
                mc("task priority is normalised to upper-case", "(a) lower-case priority", "pri:A", true),
                mc("query priority letter must be upper-case", "(a) lower-case priority", "pri:a", false),
                mc("negated exact priority", "(A) write report", "!pri:A", false),
                mc("negated any priority on a plain task", "buy milk", "!pri", true)
        )) {
            assertMatch(tc);
        }
    }

    @Test
    public void queryMatching_completionAndDates() {
        final String today = TodoTxtTask.getToday();
        for (final MatchCase tc : Arrays.asList(
                mc("done matches a completed task", "x finished task", "done", true),
                mc("done does not match an open task", "buy milk", "done", false),
                mc("!done excludes a completed task", "x finished task", "!done", false),
                mc("!done keeps an open task", "buy milk", "!done", true),
                mc("overdue due date", "pay rent due:2000-01-01", "due<", true),
                mc("future due date", "pay rent due:9999-12-31", "due>", true),
                mc("future is not overdue", "pay rent due:9999-12-31", "due<", false),
                mc("no due date -> due-any false", "buy milk", "due", false),
                mc("has due date -> due-any true", "pay rent due:2000-01-01", "due", true),
                mc("due today", "do it today due:" + today, "due=", true),
                mc("today is not overdue", "do it today due:" + today, "due<", false),
                mc("invalid due date is classified by string compare", "weird task due:2000-13-45", "due<", true),
                mc("creation date matched as substring", "2024-01-01 created task", "2024-01-01", true),
                mc("completion date matched as substring", "x 2024-02-02 2024-01-01 done one", "2024-02-02", true),
                mc("invalid creation date still accepted as text", "2000-13-45 invalid date task", "2000-13-45", true)
        )) {
            assertMatch(tc);
        }
    }

    @Test
    public void queryMatching_projectsAndContexts() {
        for (final MatchCase tc : Arrays.asList(
                mc("exact project", "call client +work", "+work", true),
                mc("missing project", "call client +work", "+home", false),
                mc("has any project", "call client +work", "+", true),
                mc("no project -> any-project false", "buy milk", "+", false),
                mc("exact context", "ping @bob", "@bob", true),
                mc("has any context", "ping @bob", "@", true),
                mc("no context -> any-context false", "buy milk", "@", false),
                mc("context match is case-sensitive (miss)", "email @Boss", "@boss", false),
                mc("context match is case-sensitive (hit)", "email @Boss", "@Boss", true),
                mc("two projects via AND -> hit", "review +alpha +beta", "+alpha & +beta", true),
                mc("two projects via AND -> miss", "review +alpha +beta", "+alpha & +gamma", false),
                mc("two contexts via OR -> hit", "sync @home @work", "@home | @phone", true),
                mc("project containing a dash", "fix +home-improvement", "+home-improvement", true),
                mc("priority + project + context + due combined",
                        "(A) ship it +rel @ci due:9999-01-01", "pri:A & +rel & @ci & due>", true)
        )) {
            assertMatch(tc);
        }
    }

    @Test
    public void queryMatching_textCaseAndSpecialChars() {
        for (final MatchCase tc : Arrays.asList(
                mc("substring match is case-insensitive (lower query)", "Buy MILK", "milk", true),
                mc("substring match is case-insensitive (upper query)", "Buy MILK", "BUY", true),
                mc("substring miss", "Buy MILK", "bread", false),
                mc("parentheses in task text", "pay rent (urgent) 50% #q1", "urgent", true),
                mc("percent sign in task text", "pay rent (urgent) 50% #q1", "50%", true),
                mc("hash tag in task text", "pay rent (urgent) 50% #q1", "#q1", true),
                mc("underscore in context", "log @time_tracking", "@time_tracking", true),
                mc("key:value pair matched as substring", "review report lvl:high", "lvl:high", true),
                mc("empty query matches nothing", "buy milk", "", false),
                mc("whitespace-only query matches nothing", "buy milk", "   ", false),
                mc("dangling operator is rejected", "(A) task", "pri:A &", false),
                mc("unbalanced parenthesis is rejected", "buy milk", "milk )", false)
        )) {
            assertMatch(tc);
        }
    }

    @Test
    public void filterList_completionAndTags() {
        for (final FilterCase tc : Arrays.asList(
                fc("exclude completed tasks", "!done",
                        arr("(A) alpha", "x done beta", "gamma", "x done delta"),
                        arr("(A) alpha", "gamma")),
                fc("only completed tasks", "done",
                        arr("(A) alpha", "x done beta", "gamma", "x done delta"),
                        arr("x done beta", "x done delta")),
                fc("multiple projects via OR", "+alpha | +beta",
                        arr("t1 +alpha", "t2 +beta", "t3 +gamma", "t4 none"),
                        arr("t1 +alpha", "t2 +beta")),
                fc("multiple contexts via AND", "@home & @work",
                        arr("m1 @home @work", "m2 @home", "m3 @work"),
                        arr("m1 @home @work")),
                fc("project AND not-done", "+rel & !done",
                        arr("(A) ship +rel", "x shipped +rel", "plan +rel"),
                        arr("(A) ship +rel", "plan +rel")),
                fc("priority filter keeps input order", "pri:A",
                        arr("(A) a", "(B) b", "(A) c", "none"),
                        arr("(A) a", "(A) c")),
                fc("empty query keeps nothing", "",
                        arr("a", "b"),
                        arr())
        )) {
            assertThat(matching(tasks(tc.input), tc.query))
                    .as("[%s] query=<%s>", tc.name, tc.query)
                    .containsExactly(tc.expected);
        }
    }

    @Test
    public void sortList_byPriority() {
        for (final SortCase tc : Arrays.asList(
                sc("ascending by priority, none last", false,
                        arr("(B) b", "(A) a", "none", "(C) c"),
                        arr("(A) a", "(B) b", "(C) c", "none")),
                sc("completed tasks sink to the bottom (ascending)", false,
                        arr("(B) b", "x done", "(A) a"),
                        arr("(A) a", "(B) b", "x done")),
                sc("completed tasks stay at the bottom even when descending", true,
                        arr("(A) a", "(B) b", "x done"),
                        arr("(B) b", "(A) a", "x done")),
                sc("equal priority resolved by due date (earliest first, none last)", false,
                        arr("(A) later due:2030-01-01", "(A) noDue", "(A) sooner due:2020-01-01"),
                        arr("(A) sooner due:2020-01-01", "(A) later due:2030-01-01", "(A) noDue")),
                sc("descending by priority", true,
                        arr("(A) a", "(C) c", "(B) b"),
                        arr("(C) c", "(B) b", "(A) a"))
        )) {
            final List<TodoTxtTask> sorted = TodoTxtTask.sortTasks(
                    tasks(tc.input), TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, tc.descending);
            assertThat(linesOf(sorted))
                    .as("[%s] descending=%s", tc.name, tc.descending)
                    .containsExactly(tc.expected);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------------------------------------------

    private static void assertMatch(final MatchCase tc) {
        assertThat(TodoTxtFilter.isMatchQuery(new TodoTxtTask(tc.task), tc.query))
                .as("[%s] task=<%s> query=<%s>", tc.name, tc.task, tc.query)
                .isEqualTo(tc.expected);
    }

    private static List<TodoTxtTask> tasks(final String... lines) {
        final List<TodoTxtTask> out = new ArrayList<>();
        for (final String line : lines) {
            out.add(new TodoTxtTask(line));
        }
        return out;
    }

    private static List<String> linesOf(final List<TodoTxtTask> tasks) {
        final List<String> out = new ArrayList<>();
        for (final TodoTxtTask task : tasks) {
            out.add(task.getLine());
        }
        return out;
    }

    // Filter a list with a query, preserving the original order, returning the matching lines.
    private static List<String> matching(final List<TodoTxtTask> tasks, final String query) {
        final List<String> out = new ArrayList<>();
        for (final TodoTxtTask task : tasks) {
            if (TodoTxtFilter.isMatchQuery(task, query)) {
                out.add(task.getLine());
            }
        }
        return out;
    }

    private static String[] arr(final String... values) {
        return values;
    }

    private static MatchCase mc(final String name, final String task, final String query, final boolean expected) {
        return new MatchCase(name, task, query, expected);
    }

    private static FilterCase fc(final String name, final String query, final String[] input, final String[] expected) {
        return new FilterCase(name, query, input, expected);
    }

    private static SortCase sc(final String name, final boolean descending, final String[] input, final String[] expected) {
        return new SortCase(name, descending, input, expected);
    }

    private static final class MatchCase {
        final String name;
        final String task;
        final String query;
        final boolean expected;

        MatchCase(final String name, final String task, final String query, final boolean expected) {
            this.name = name;
            this.task = task;
            this.query = query;
            this.expected = expected;
        }
    }

    private static final class FilterCase {
        final String name;
        final String query;
        final String[] input;
        final String[] expected;

        FilterCase(final String name, final String query, final String[] input, final String[] expected) {
            this.name = name;
            this.query = query;
            this.input = input;
            this.expected = expected;
        }
    }

    private static final class SortCase {
        final String name;
        final boolean descending;
        final String[] input;
        final String[] expected;

        SortCase(final String name, final boolean descending, final String[] input, final String[] expected) {
            this.name = name;
            this.descending = descending;
            this.input = input;
            this.expected = expected;
        }
    }
}
