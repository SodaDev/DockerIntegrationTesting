package com.soda.config;

import java.lang.annotation.*;
import java.net.URI;

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
