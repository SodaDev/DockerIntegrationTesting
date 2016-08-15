package com.soda.config;

public @interface DockerTestImageInitCmd {
    String[] cmd() default {};
}
