package com.soda.docker.test.config;

import java.lang.annotation.*;

/**
 * Annotation describes docker repository config.
 *
 * This annotation should be set by Docker Toolbox users who don't want to
 * set system DOCKER_HOST variable. In case when DOCKER_HOST variable is set
 * and DockerRepositoryConfig annotation is left blank system environment config
 * will be used to set up docker client.
 *
 * <ul>
 *     <li>serverAddress - <b>optional</b> docker registry address. Default value points to the public Docker registry
 *     </li>
 *     <li>email - <b>optional</b> email</li>
 *     <li>username - <b>optional</b> username</li>
 *     <li>password - <b>optional</b> password</li>
 *     <li>certPath - <b>optional</b> local path to the docker certificates</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface DockerRepositoryConfig {
    String serverAddress() default "";
    String email() default "";
    String username() default "";
    String password() default "";
    String certPath() default "";
}
