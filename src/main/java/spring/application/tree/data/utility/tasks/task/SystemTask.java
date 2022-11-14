package spring.application.tree.data.utility.tasks.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
public class SystemTask<T> {
    private T id;
    private Runnable task;

    public void run() {
        if (task != null) {
            log.debug("Task with ID '{}' launched", id);
            task.run();
        }
    }
}
