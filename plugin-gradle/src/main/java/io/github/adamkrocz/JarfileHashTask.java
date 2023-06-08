package io.github.adamkrocz;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class JarfileHashTask extends DefaultTask {
    @TaskAction
    public void jarfileHash() {
        System.out.println("Hello World");
    }
}
