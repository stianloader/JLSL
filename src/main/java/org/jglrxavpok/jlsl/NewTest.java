package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.glsl.*;
import static org.jglrxavpok.jlsl.glsl.GLSL.*;

public class NewTest {

    private static final JLGL jlgl = new JLGL(150);

    public static void main(final String[] args) {
        System.out.println(jlgl.generateGLSLShader(ExampleShader.class, new ObfuscationFilter()));
    }

    private static class ExampleShader extends FragmentShaderEnvironment {

        @Uniform Sampler2D Sampler0;
        @Uniform Vec4 ColorModulator;

        @In Vec4 vertexColor;
        @In Vec2 texCoord0;
        @In Vec2 texCoord2;
        @In Vec4 normal;

        @Out Vec4 fragColor;

        @Override
        public void main() {
            Vec4 color = texture(Sampler0, texCoord0).mul(vertexColor);
            if (color.a < 0.1) {
                return;
            }
            fragColor = color.mul(ColorModulator);
        }
    }
}
