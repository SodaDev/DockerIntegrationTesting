package com.soda.docker.test.config;

/**
 * Annotation is just a container for multiple initialization commands
 */
public @interface DockerTestImageInitCmd {
    String[] cmd() default {};
}
