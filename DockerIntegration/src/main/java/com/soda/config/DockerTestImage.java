package com.soda.config;

public @interface DockerTestImage {
    String name();
    String[] configCmds() default {};
    String[] ports() default {};
    DockerTestImageInitCmd[] initCmds() default {};
    int initDelayInMs();
}
