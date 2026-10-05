package com.iwfc;

/**
 * Application entry point.
 *
 * JavaFX requires the main class NOT to extend Application when running
 * from a plain executable JAR (without the JavaFX module on the classpath
 * as an automatic module). This thin launcher delegates to MainApp so that
 * the JavaFX runtime is initialised correctly in all execution contexts.
 */
public class Main {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
