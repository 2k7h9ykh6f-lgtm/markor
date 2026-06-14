package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link TodoTxtTask} field extraction — priority, done status,
 * creation/completion dates, contexts, projects, due dates, and description.
 */
public class TodoTxtTaskParsingTests {

    // ── Priority ──────────────────────────────────────────────────────────

    @Test
    public void priorityUpperCase() {
        assertThat(new TodoTxtTask("(A) task").getPriority()).isEqualTo('A');
    }

    @Test
    public void priorityLowerCase() {
        assertThat(new TodoTxtTask("(a) task").getPriority()).isEqualTo('A');
    }

    @Test
    public void priorityZ() {
        assertThat(new TodoTxtTask("(Z) task").getPriority()).isEqualTo('Z');
    }

    @Test
    public void priorityNone() {
        assertThat(new TodoTxtTask("plain task").getPriority())
                .isEqualTo(TodoTxtTask.PRIORITY_NONE);
    }

    @Test
    public void priorityNotAtStart() {
        // Priority must be at start of line
        assertThat(new TodoTxtTask("task (A) more").getPriority())
                .isEqualTo(TodoTxtTask.PRIORITY_NONE);
    }

    @Test
    public void priorityNoSpaceAfter() {
        // (A) without trailing space is NOT a valid priority
        assertThat(new TodoTxtTask("(A)task").getPriority())
                .isEqualTo(TodoTxtTask.PRIORITY_NONE);
    }

    // ── Done status ───────────────────────────────────────────────────────

    @Test
    public void doneLowerCase() {
        assertThat(new TodoTxtTask("x completed task").isDone()).isTrue();
    }

    @Test
    public void doneUpperCase() {
        assertThat(new TodoTxtTask("X completed task").isDone()).isTrue();
    }

    @Test
    public void notDone() {
        assertThat(new TodoTxtTask("(A) pending task").isDone()).isFalse();
    }

    @Test
    public void doneWithDate() {
        assertThat(new TodoTxtTask("x 2024-01-15 completed task").isDone()).isTrue();
    }

    @Test
    public void doneNotAtStart() {
        // 'x' in the middle of the line is not a done marker
        assertThat(new TodoTxtTask("tax x something").isDone()).isFalse();
    }

    @Test
    public void emptyLineNotDone() {
        assertThat(new TodoTxtTask("").isDone()).isFalse();
    }

    // ── Creation date ─────────────────────────────────────────────────────

    @Test
    public void creationDateSimple() {
        assertThat(new TodoTxtTask("2024-01-15 task").getCreationDate())
                .isEqualTo("2024-01-15");
    }

    @Test
    public void creationDateWithPriority() {
        assertThat(new TodoTxtTask("(A) 2024-01-15 task").getCreationDate())
                .isEqualTo("2024-01-15");
    }

    @Test
    public void creationDateNone() {
        assertThat(new TodoTxtTask("plain task").getCreationDate())
                .isEqualTo("");
    }

    @Test
    public void creationDateNoneWithDefault() {
        assertThat(new TodoTxtTask("plain task").getCreationDate("DEFAULT"))
                .isEqualTo("DEFAULT");
    }

    @Test
    public void creationDateInvalidFormat() {
        // Not matching yyyy-MM-dd
        assertThat(new TodoTxtTask("2024-1-5 task").getCreationDate())
                .isEqualTo("");
    }

    // ── Completion date ───────────────────────────────────────────────────

    @Test
    public void completionDatePresent() {
        assertThat(new TodoTxtTask("x 2024-01-15 done task").getCompletionDate())
                .isEqualTo("2024-01-15");
    }

    @Test
    public void completionDateAbsentForPending() {
        assertThat(new TodoTxtTask("(A) pending task").getCompletionDate())
                .isEqualTo("");
    }

    @Test
    public void completionDateDoneWithoutDate() {
        // x without date — the pattern matches but group(1) is null
        assertThat(new TodoTxtTask("x done task").getCompletionDate())
                .isNull();
    }

    @Test
    public void bothDatesPresent() {
        TodoTxtTask task = new TodoTxtTask("x 2024-06-20 2024-06-01 task");
        assertThat(task.getCompletionDate()).isEqualTo("2024-06-20");
        assertThat(task.getCreationDate()).isEqualTo("2024-06-01");
    }

    // ── Contexts ──────────────────────────────────────────────────────────

    @Test
    public void singleContext() {
        assertThat(new TodoTxtTask("task @home").getContexts())
                .containsExactly("home");
    }

    @Test
    public void multipleContexts() {
        assertThat(new TodoTxtTask("task @home @work").getContexts())
                .containsExactly("home", "work");
    }

    @Test
    public void noContext() {
        assertThat(new TodoTxtTask("plain task").getContexts())
                .isEmpty();
    }

    @Test
    public void contextWithDash() {
        assertThat(new TodoTxtTask("task @my-office").getContexts())
                .containsExactly("my-office");
    }

    @Test
    public void contextAtStart() {
        assertThat(new TodoTxtTask("@home task").getContexts())
                .containsExactly("home");
    }

    // ── Projects ──────────────────────────────────────────────────────────

    @Test
    public void singleProject() {
        assertThat(new TodoTxtTask("task +project").getProjects())
                .containsExactly("project");
    }

    @Test
    public void multipleProjects() {
        assertThat(new TodoTxtTask("task +projA +projB").getProjects())
                .containsExactly("projA", "projB");
    }

    @Test
    public void noProject() {
        assertThat(new TodoTxtTask("plain task").getProjects())
                .isEmpty();
    }

    @Test
    public void projectAtStart() {
        assertThat(new TodoTxtTask("+project task").getProjects())
                .containsExactly("project");
    }

    @Test
    public void mixedContextsAndProjects() {
        TodoTxtTask task = new TodoTxtTask("(B) 2024-01-01 task +proj1 @ctx1 +proj2 @ctx2");
        assertThat(task.getProjects()).containsExactly("proj1", "proj2");
        assertThat(task.getContexts()).containsExactly("ctx1", "ctx2");
    }

    // ── Due date ──────────────────────────────────────────────────────────

    @Test
    public void dueDatePresent() {
        assertThat(new TodoTxtTask("task due:2024-06-15").getDueDate())
                .isEqualTo("2024-06-15");
    }

    @Test
    public void dueDateAbsent() {
        assertThat(new TodoTxtTask("plain task").getDueDate())
                .isEqualTo("");
    }

    @Test
    public void dueDateWithPriority() {
        assertThat(new TodoTxtTask("(A) task due:2024-06-15").getDueDate())
                .isEqualTo("2024-06-15");
    }

    // ── Description ───────────────────────────────────────────────────────

    @Test
    public void descriptionSimple() {
        assertThat(new TodoTxtTask("Buy milk").getDescription().trim())
                .isEqualTo("Buy milk");
    }

    @Test
    public void descriptionStripsPriority() {
        assertThat(new TodoTxtTask("(A) Buy milk").getDescription().trim())
                .isEqualTo("Buy milk");
    }

    @Test
    public void descriptionStripsProjectAndContext() {
        String desc = new TodoTxtTask("Buy milk +groceries @store").getDescription().trim();
        assertThat(desc).isEqualTo("Buy milk");
    }

    @Test
    public void descriptionStripsDueDate() {
        String desc = new TodoTxtTask("Buy milk due:2024-06-15").getDescription().trim();
        assertThat(desc).isEqualTo("Buy milk");
    }

    @Test
    public void descriptionStripsAll() {
        String desc = new TodoTxtTask("(A) 2024-01-15 Buy milk +groceries @store due:2024-06-15")
                .getDescription().trim();
        assertThat(desc).isEqualTo("Buy milk");
    }

    // ── Edge cases ────────────────────────────────────────────────────────

    @Test
    public void emptyLine() {
        TodoTxtTask task = new TodoTxtTask("");
        assertThat(task.isDone()).isFalse();
        assertThat(task.getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
        assertThat(task.getContexts()).isEmpty();
        assertThat(task.getProjects()).isEmpty();
        assertThat(task.getCreationDate()).isEmpty();
    }

    @Test
    public void specialCharactersInText() {
        TodoTxtTask task = new TodoTxtTask("(A) Task with (parentheses) and [brackets] +proj @ctx");
        assertThat(task.getPriority()).isEqualTo('A');
        assertThat(task.getProjects()).containsExactly("proj");
        assertThat(task.getContexts()).containsExactly("ctx");
    }

    @Test
    public void multiplePlusSignInProject() {
        // The regex allows multiple + signs: ++project
        TodoTxtTask task = new TodoTxtTask("task ++project");
        assertThat(task.getProjects()).containsExactly("project");
    }

    @Test
    public void multipleAtSignInContext() {
        // The regex allows multiple @ signs: @@context
        TodoTxtTask task = new TodoTxtTask("task @@context");
        assertThat(task.getContexts()).containsExactly("context");
    }

    @Test
    public void getLinePreserved() {
        String line = "(A) 2024-01-15 Buy milk +groceries @store";
        assertThat(new TodoTxtTask(line).getLine()).isEqualTo(line);
    }

    // ── Static aggregation helpers ────────────────────────────────────────

    @Test
    public void aggregateProjects() {
        List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("task +projA"),
                new TodoTxtTask("task +projB +projA"),
                new TodoTxtTask("task"));
        assertThat(TodoTxtTask.getProjects(tasks))
                .containsExactly("projA", "projB");
    }

    @Test
    public void aggregateContexts() {
        List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("task @home"),
                new TodoTxtTask("task @work @home"),
                new TodoTxtTask("task"));
        assertThat(TodoTxtTask.getContexts(tasks))
                .containsExactly("home", "work");
    }

    @Test
    public void getAllTasks() {
        List<TodoTxtTask> tasks = TodoTxtTask.getAllTasks("(A) task1\ntask2\nx done");
        assertThat(tasks).hasSize(3);
        assertThat(tasks.get(0).getPriority()).isEqualTo('A');
        assertThat(tasks.get(1).getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
        assertThat(tasks.get(2).isDone()).isTrue();
    }

    @Test
    public void tasksToString() {
        List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("task1"),
                new TodoTxtTask("task2"));
        assertThat(TodoTxtTask.tasksToString(tasks)).isEqualTo("task1\ntask2");
    }
}
