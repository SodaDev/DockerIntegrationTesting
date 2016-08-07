package com.soda;

public @interface DockerTestImageInitCmd {
    String[] cmd() default {};
}
