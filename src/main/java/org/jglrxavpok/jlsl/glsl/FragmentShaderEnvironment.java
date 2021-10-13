package org.jglrxavpok.jlsl.glsl;

public abstract class FragmentShaderEnvironment extends ShaderEnvironment {
    public vec4 gl_FragColor;

    public vec4 gl_FragCoord;

    public vec4 texture(final sampler2D texture, final vec2 coords) {
        return new vec4(0, 0, 0, 0);
    }

    @Deprecated
    public vec4 texture2D(final sampler2D texture, final vec2 coords) {
        return new vec4(0, 0, 0, 0);
    }
}
