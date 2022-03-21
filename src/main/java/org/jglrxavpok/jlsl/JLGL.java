package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.glsl.GLSLEncoder;
import org.jglrxavpok.jlsl.glsl.ShaderEnvironment;
import org.jglrxavpok.jlsl.java.BytecodeDecoder;
import org.jglrxavpok.jlsl.java.JavaEncoder;

import java.io.PrintWriter;
import java.io.StringWriter;


public class JLGL {
    private final GLSLEncoder glslEncoder;
    private final BytecodeDecoder bytecodeDecoder;
    private final JavaEncoder javaEncoder;

    public JLGL(int glslVersion) {
        this.glslEncoder = new GLSLEncoder(glslVersion);
        this.javaEncoder = new JavaEncoder();
        this.bytecodeDecoder = new BytecodeDecoder();
    }


    public String generateGLSLShader(Class<? extends ShaderEnvironment> shaderClass, CodeFilter... filters) {
        JLSLContext context = new JLSLContext(this.bytecodeDecoder, this.glslEncoder);
        context.addFilters(filters);
        StringWriter stringWriter = new StringWriter();
        context.execute(shaderClass, new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

//
//    public String obfuscateClass(Class<?> clazz) {
//        JLSLContext context = new JLSLContext(this.bytecodeDecoder, this.javaEncoder);
//        context.addFilters(new ObfuscationFilter());
//        StringWriter stringWriter = new StringWriter();
//        context.execute(clazz, new PrintWriter(stringWriter));
//        return stringWriter.toString();
//    }
}
