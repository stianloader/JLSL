package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.conversion.ASMClassnode2GLSL;
import org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode;
import org.jglrxavpok.jlsl.filters.CodeFilter;
import org.jglrxavpok.jlsl.filters.ObfuscationFilter;
import org.jglrxavpok.jlsl.glsl.FragmentShaderEnvironment;
import org.jglrxavpok.jlsl.glsl.Sampler2D;
import org.jglrxavpok.jlsl.glsl.Vec2;
import org.jglrxavpok.jlsl.glsl.Vec4;
import org.jglrxavpok.jlsl.conversion.Java2ASMClassnode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

import static org.jglrxavpok.jlsl.glsl.GLSL.*;

public class NewTest {

    public static void main(final String[] args) {
//        System.out.println(jlgl.generateGLSLShader(ExampleShader.class));

        try {
            ClassNode node = Java2ASMClassnode.INSTANCE.decode(ExampleShader.class);
            var settings = new ASMClassnode2GLSL.Settings(330, false, true);
            GLSLBytecode.Root out = ASMClassnode2GLSL.INSTANCE.encode(node, settings);
//            out = new ObfuscationFilter().filter(out);
            System.out.println(out.generateSource("    "));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                color.b = 0;
                return;
            }
            if (color.d > 0.5) {
                color.w = 0;
                return;
            }
            fragColor = color.mul(ColorModulator);
            another(3, this);
        }

        public void another(int someInt, ExampleShader someShader) {
            Vec4 color = texture(Sampler0, texCoord0).mul(vertexColor);
            if (color.a < 0.1) {
                return;
            }
            fragColor = color.mul(ColorModulator);
        }
    }
}
