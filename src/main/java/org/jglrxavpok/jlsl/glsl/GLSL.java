package org.jglrxavpok.jlsl.glsl;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class GLSL {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Substitute {
        String value();

        boolean usesParenthesis() default true;

        boolean ownerBefore() default false;

        boolean actsAsField() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Out {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface In {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Uniform {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Notes {
        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Extensions {
        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Native {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface NativeClass {
        @NotNull String name();
    }
}
