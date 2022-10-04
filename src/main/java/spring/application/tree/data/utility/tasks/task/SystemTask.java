package spring.application.tree.data.utility.tasks.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SystemTask<T> {
    private T id;
    private Runnable task;

    public void run() {
        if (task != null) {
            task.run();
        }
    }
}
