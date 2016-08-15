package com.soda.config;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface DockerIntegrationTest {
    DockerTestImages images();
    DockerRepositoryConfig config() default @DockerRepositoryConfig();
    boolean singleInstance() default false;
}
