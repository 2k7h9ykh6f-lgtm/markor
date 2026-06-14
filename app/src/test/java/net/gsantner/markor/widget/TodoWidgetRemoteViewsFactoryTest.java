/*#######################################################
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Robolectric unit tests for the todo-widget list factory, focused on the
 *   empty-list refresh path and the task-count contract.
 *
#########################################################*/
package net.gsantner.markor.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.model.AppSettings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = Application.class)
public class TodoWidgetRemoteViewsFactoryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Context ctx() {
        return RuntimeEnvironment.getApplication();
    }

    private TodoWidgetRemoteViewsFactory newFactory() {
        return new TodoWidgetRemoteViewsFactory(ctx(), new Intent());
    }

    @Test
    public void emptyList_whenTodoFileMissing_countIsZero() {
        // Widget refresh against a non-existent todo file -> loadContent null -> empty
        final File missing = new File(tmp.getRoot(), "no-such-todo.txt");
        AppSettings.get(ctx()).setTodoFile(missing);

        final TodoWidgetRemoteViewsFactory factory = newFactory();
        factory.onCreate(); // triggers onDataSetChanged()

        assertEquals(0, factory.getCount());
    }

    @Test
    public void populatedList_countMatchesNumberOfTasks() throws Exception {
        final File todo = tmp.newFile("todo.txt");
        Files.write(todo.toPath(),
                "buy milk\ncall alice\n(A) ship release".getBytes(StandardCharsets.UTF_8));
        AppSettings.get(ctx()).setTodoFile(todo);

        final TodoWidgetRemoteViewsFactory factory = newFactory();
        factory.onCreate();

        assertEquals(3, factory.getCount());
    }

    @Test
    public void factoryContract_viewTypeStableIdsAndItemId() {
        final TodoWidgetRemoteViewsFactory factory = newFactory();

        assertEquals(1, factory.getViewTypeCount());
        assertFalse(factory.hasStableIds());
        assertEquals(5L, factory.getItemId(5));
    }

    @Test
    public void getAllTasks_countRuleBackingTheWidget() {
        // The widget count is exactly TodoTxtTask.getAllTasks(content).size()
        assertEquals(3, TodoTxtTask.getAllTasks("buy milk\ncall alice\nship it").size());
    }
}
