package com.soda;

import java.lang.annotation.*;
import java.net.URI;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface DockerRepositoryConfig {
    String serverAddress() default "";
    String email();
    String username();
    String password();
    String certPath();
}
