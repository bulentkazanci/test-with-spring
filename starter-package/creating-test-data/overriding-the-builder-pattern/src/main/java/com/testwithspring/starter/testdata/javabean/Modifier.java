package com.testwithspring.starter.testdata.javabean;

/**
 * Identifies the user who modified a specific task.
 *
 * @author Petri Kainulainen
 */
public class Modifier {

    private final Long userId;

    public Modifier(Long userId) {
        this.userId = userId;
    }
}
