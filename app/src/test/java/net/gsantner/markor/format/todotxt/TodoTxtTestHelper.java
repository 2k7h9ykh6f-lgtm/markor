package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared helpers for todo.txt tests. Reduces boilerplate when constructing tasks,
 * asserting query matches, and verifying sort orders.
 */
public class TodoTxtTestHelper {

    /**
     * Create a list of TodoTxtTask from raw lines.
     */
    public static List<TodoTxtTask> tasks(String... lines) {
        List<TodoTxtTask> list = new ArrayList<>();
        for (String line : lines) {
            list.add(new TodoTxtTask(line));
        }
        return list;
    }

    /**
     * Return the subset of tasks that match the given query.
     */
    public static List<TodoTxtTask> filter(List<TodoTxtTask> tasks, String query) {
        return tasks.stream()
                .filter(t -> TodoTxtFilter.isMatchQuery(t, query))
                .collect(Collectors.toList());
    }

    /**
     * Return the lines of the filtered tasks (for easy assertion).
     */
    public static List<String> filterLines(List<TodoTxtTask> tasks, String query) {
        return filter(tasks, query).stream()
                .map(TodoTxtTask::getLine)
                .collect(Collectors.toList());
    }

    /**
     * Assert that a query matches exactly the expected tasks (in order) from the full list.
     *
     * @param allTasks     full task list
     * @param query        the query string
     * @param expectedLines expected matching task lines, in order
     */
    public static void assertQueryMatches(List<TodoTxtTask> allTasks, String query, String... expectedLines) {
        List<String> actual = filterLines(allTasks, query);
        assertThat(actual).containsExactly(expectedLines);
    }

    /**
     * Assert that a single task matches (or does not match) a query.
     */
    public static void assertTaskMatches(TodoTxtTask task, String query, boolean expected) {
        assertThat(TodoTxtFilter.isMatchQuery(task, query))
                .as("Task '%s' %s query '%s'", task.getLine(),
                        expected ? "matches" : "does not match", query)
                .isEqualTo(expected);
    }

    /**
     * Assert that a single task line matches (or does not match) a query.
     */
    public static void assertLineMatches(String line, String query, boolean expected) {
        assertTaskMatches(new TodoTxtTask(line), query, expected);
    }

    /**
     * Return sorted lines from the given task lines.
     */
    public static List<String> sortedLines(String orderBy, boolean descending, String... lines) {
        List<TodoTxtTask> taskList = tasks(lines);
        TodoTxtTask.sortTasks(taskList, orderBy, descending);
        return taskList.stream().map(TodoTxtTask::getLine).collect(Collectors.toList());
    }
}
