package com.soda.docker.test.config;

/**
 * Annotation describes docker image which should be used during the integration test.
 * <ul>
 *     <li>name - <b>mandatory</b> docker image name - f.e. 'nginx:latest'</li>
 *     <li>configCmds - <b>optional</b> commands that should be executed on docker container</li>
 *     <li>ports - <b>optional</b> ports that have to be exposed on docker container</li>
 *     <li>initCmds - <b>optional</b> commands that should initialize container after start</li>
 *     <li>initDelayInMs - <b>optional</b> delay in milliseconds after every command and docker container start</li>
 * </ul>
 */
public @interface DockerTestImage {
    String name();
    String[] configCmds() default {};
    String[] ports() default {};
    DockerTestImageInitCmd[] initCmds() default {};
    int initDelayInMs() default 0;
}
