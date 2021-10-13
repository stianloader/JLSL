package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.glsl.GLSLEncoder;
import org.jglrxavpok.jlsl.glsl.ShaderEnvironment;
import org.jglrxavpok.jlsl.java.JavaEncoder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class JLGL {

    private final int glslVersion;
    private final GLSLEncoder glslEncoder;
    private final BytecodeDecoder bytecodeDecoder;
    private final JavaEncoder javaEncoder;

    public JLGL(final int glslVersion) {
        this.glslVersion = glslVersion;
        this.glslEncoder = new GLSLEncoder(glslVersion);
        this.javaEncoder = new JavaEncoder(glslVersion);
        this.bytecodeDecoder = new BytecodeDecoder();
    }

    /**
     * Generates a GLSL shader from a class
     * @param shaderClass the base class
     * @param filters the filter
     * @return the shader as a string
     */
    public String generateGLSLShader(final Class<? extends ShaderEnvironment> shaderClass, final CodeFilter... filters) {
        final JLSLContext context = new JLSLContext(bytecodeDecoder, glslEncoder);
        context.addFilters(filters);
        final StringWriter stringWriter = new StringWriter();
        context.execute(shaderClass, new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Generates obfuscated code from this class
     * @param clazz the class
     * @return the result code
     */
    public String obfuscateClass(final Class<?> clazz) {
        final JLSLContext context = new JLSLContext(bytecodeDecoder, javaEncoder);
        context.addFilters(new ObfuscationFilter());
        final StringWriter stringWriter = new StringWriter();
        context.execute(clazz, new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
