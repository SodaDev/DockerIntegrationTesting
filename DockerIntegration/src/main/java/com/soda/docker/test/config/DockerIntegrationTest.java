package com.soda.docker.test.config;

import java.lang.annotation.*;

/**
 * Annotation describes integration test behaviour
 *
 * <ul>
 *     <li>images - <b>mandatory</b> list of docker containers to be started during test</li>
 *     <li>config - <b>optional</b> docker repository config - when config is left blank
 *                  or docker environment variables are set default environment config
 *                  will be used
 *     </li>
 *     <li>singleInstance - <b>optional</b> flag describing whether new container will be started
 *                          before every test or once per test instance
 *     </li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface DockerIntegrationTest {
    DockerTestImages images();
    DockerRepositoryConfig config() default @DockerRepositoryConfig();
    boolean singleInstance() default false;
}
