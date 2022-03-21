package org.jglrxavpok.jlsl.glsl;

public abstract class FragmentShaderEnvironment
        extends ShaderEnvironment {
    public Vec4 gl_FragColor;
    public Vec4 gl_FragCoord;

    public Vec4 texture(Sampler2D texture, Vec2 coords) {
        return new Vec4(0.0D, 0.0D, 0.0D, 0.0D);
    }

    @Deprecated
    public Vec4 texture2D(Sampler2D texture, Vec2 coords) {
        return new Vec4(0.0D, 0.0D, 0.0D, 0.0D);
    }
}
