package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

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

    // ── Additional evaluateExpression edge cases ─────────────────────────

    @Test
    public void evaluateExpressionSingleValue() {
        assertThat(TodoTxtFilter.evaluateExpression("T")).isTrue();
        assertThat(TodoTxtFilter.evaluateExpression("F")).isFalse();
    }

    @Test
    public void evaluateExpressionAllAndTrue() {
        assertThat(TodoTxtFilter.evaluateExpression(strip("T & T & T"))).isTrue();
    }

    @Test
    public void evaluateExpressionAllAndOneFalse() {
        assertThat(TodoTxtFilter.evaluateExpression(strip("T & T & F"))).isFalse();
    }

    @Test
    public void evaluateExpressionAllOrFalse() {
        assertThat(TodoTxtFilter.evaluateExpression(strip("F | F | F"))).isFalse();
    }

    @Test
    public void evaluateExpressionDeeplyNestedParens() {
        assertThat(TodoTxtFilter.evaluateExpression(strip("(((T)))"))).isTrue();
        assertThat(TodoTxtFilter.evaluateExpression(strip("(((F)))"))).isFalse();
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(((T)))"))).isFalse();
    }

    @Test
    public void evaluateExpressionAdjacentGroups() {
        assertThat(TodoTxtFilter.evaluateExpression(strip("(T) & (F)"))).isFalse();
        assertThat(TodoTxtFilter.evaluateExpression(strip("(T) | (F)"))).isTrue();
        assertThat(TodoTxtFilter.evaluateExpression(strip("(T & T) | (F & F)"))).isTrue();
    }

    @Test
    public void evaluateExpressionLeftToRightEvaluation() {
        // The evaluator processes operators strictly left-to-right (no & precedence over |)
        // T | F & F → (T | F) & F → T & F → F
        assertThat(TodoTxtFilter.evaluateExpression(strip("T | F & F"))).isFalse();
        // F & F | T → (F & F) | T → F | T → T
        assertThat(TodoTxtFilter.evaluateExpression(strip("F & F | T"))).isTrue();
        // F | T & F → (F | T) & F → T & F → F
        assertThat(TodoTxtFilter.evaluateExpression(strip("F | T & F"))).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void evaluateExpressionMalformedEmpty() {
        TodoTxtFilter.evaluateExpression("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void evaluateExpressionMalformedBareOp() {
        TodoTxtFilter.evaluateExpression(strip("&"));
    }

    // ── Additional parseQuery edge cases ──────────────────────────────────

    @Test
    public void parseQueryEmptyQuery() {
        TodoTxtTask task = new TodoTxtTask("any task");
        assertThat(TodoTxtFilter.parseQuery(task, "")).isEqualTo("");
    }

    @Test
    public void parseQueryPlainTextEvaluatesToTOrF() {
        TodoTxtTask task = new TodoTxtTask("Buy milk at the store");
        assertThat(TodoTxtFilter.parseQuery(task, "milk")).isEqualTo("T");
        assertThat(TodoTxtFilter.parseQuery(task, "bread")).isEqualTo("F");
    }

    @Test
    public void parseQueryContextAndProject() {
        TodoTxtTask task = new TodoTxtTask("task +project @context");
        assertThat(TodoTxtFilter.parseQuery(task, "+project")).isEqualTo("T");
        assertThat(TodoTxtFilter.parseQuery(task, "+other")).isEqualTo("F");
        assertThat(TodoTxtFilter.parseQuery(task, "@context")).isEqualTo("T");
        assertThat(TodoTxtFilter.parseQuery(task, "@other")).isEqualTo("F");
    }

    @Test
    public void parseQueryAnyContextAndAnyProject() {
        TodoTxtTask taskWithBoth = new TodoTxtTask("task +proj @ctx");
        assertThat(TodoTxtFilter.parseQuery(taskWithBoth, "@")).isEqualTo("T");
        assertThat(TodoTxtFilter.parseQuery(taskWithBoth, "+")).isEqualTo("T");

        TodoTxtTask taskWithNone = new TodoTxtTask("plain task");
        assertThat(TodoTxtFilter.parseQuery(taskWithNone, "@")).isEqualTo("F");
        assertThat(TodoTxtFilter.parseQuery(taskWithNone, "+")).isEqualTo("F");
    }

    @Test
    public void parseQueryDoneKeyword() {
        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("x done"), "done")).isEqualTo("T");
        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("pending"), "done")).isEqualTo("F");
    }

    @Test
    public void parseQueryPriorityVariants() {
        TodoTxtTask taskA = new TodoTxtTask("(A) task");
        assertThat(TodoTxtFilter.parseQuery(taskA, "pri")).isEqualTo("T");
        assertThat(TodoTxtFilter.parseQuery(taskA, "pri:A")).isEqualTo("T");
        assertThat(TodoTxtFilter.parseQuery(taskA, "pri:B")).isEqualTo("F");

        TodoTxtTask taskNoPri = new TodoTxtTask("task");
        assertThat(TodoTxtFilter.parseQuery(taskNoPri, "pri")).isEqualTo("F");
    }

    @Test
    public void parseQueryComplexExpression() {
        TodoTxtTask task = new TodoTxtTask("(A) task +work @office due:2024-06-15");
        String query = "(pri:A | pri:B) & +work & @office & due";
        String expected = strip("(T | F) & T & T & T");
        assertThat(TodoTxtFilter.parseQuery(task, query)).isEqualTo(expected);
    }
}
