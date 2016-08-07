package com.soda;

public @interface DockerTestImage {
    String name();
    String[] configCmds() default {};
    String[] ports() default {};
    DockerTestImageInitCmd[] initCmds() default {};
}
