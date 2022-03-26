package org.jglrxavpok.jlsl.glsl;

import org.jetbrains.annotations.NotNull;

import static org.jglrxavpok.jlsl.glsl.GLSL.*;

public abstract class FragmentShaderEnvironment
        extends ShaderEnvironment {
    public Vec4 gl_FragColor;
    public Vec4 gl_FragCoord;

    @Native
    public @NotNull Vec4 texture(@NotNull Sampler2D texture, @NotNull Vec2 coords) {
        //noinspection ALL
        return null;
    }

    @Native
    @Deprecated
    public @NotNull Vec4 texture2D(@NotNull Sampler2D texture, @NotNull Vec2 coords) {
        //noinspection ALL
        return null;
    }
}
