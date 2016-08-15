package com.soda.docker.test.config;

import java.lang.annotation.*;

/**
 * Annotation is just a container for grouping DockerTestImages
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface DockerTestImages {
    DockerTestImage[] images();
}
