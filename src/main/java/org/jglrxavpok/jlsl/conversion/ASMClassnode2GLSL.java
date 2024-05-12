package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;
import static org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode.*;

import org.jglrxavpok.jlsl.glsl.GLSL;
import org.objectweb.asm.tree.*;

import java.lang.annotation.Annotation;
import java.util.*;

public enum ASMClassnode2GLSL implements GLSLEncoder<ClassNode> {
    INSTANCE;

    public @NotNull Root encode(@NotNull ClassNode input, @NotNull Settings settings) {
        synchronized (ASMClassnode2GLSL.INSTANCE) {
            List<Field> fields = getFields(input, settings);
            List<Method> methods = getMethods(input, settings);
            //noinspection unchecked
            return new Root(330, fields, methods);
        }
    }

    @Override
    public @NotNull Root encode(@NotNull ClassNode input) {
        return encode(input, Settings.DEFAULT);
    }

    private List<Field> getFields(ClassNode input, @NotNull Settings settings) {
        List<Field> fields = new ArrayList<>();
        for (FieldNode field : input.fields) {

            Field.Type type = null;
            // Do uniforms first
            if (hasAnnotation(field, GLSL.Uniform.class)) {
                type = Field.Type.UNIFORM;
            }

            if (hasAnnotation(field, GLSL.Attribute.class)) {
                type = Field.Type.ATTRIBUTE;
            }

            if (hasAnnotation(field, GLSL.Varying.class)) {
                type = Field.Type.VARYING;
            }

            // Then inputs
            if (hasAnnotation(field, GLSL.In.class)) {
                type = Field.Type.IN;
            }

            // Then outputs
            if (hasAnnotation(field, GLSL.Out.class)) {
                type = Field.Type.OUT;
            }

            String fieldTypeStr = JavaUtils.getTypeFromDescription(field.desc);
            Type fieldType = new Type(fieldTypeStr);

            fields.add(new Field(type, field.name, fieldType));
        }
        return fields;
    }

    private List<Method> getMethods(ClassNode input, @NotNull Settings settings) {
        List<Method> methods = new ArrayList<>();

        for (MethodNode method : input.methods) {
            // Get the return type and parameter types from the method signature
            JavaUtils.MethodSignature sig = JavaUtils.toSignature(method.desc);
            String returnTypeStr = sig.returnType();
            List<Type> parameterTypes = sig.argTypes().stream().map(Type::new).toList();
            int parameterCount = parameterTypes.size();

            // Exit now if the method is a constructor
            if (method.name.equals("<init>"))
                continue;

            // Get the parameter names from the local variables
            // Skip i = 0, because it's the "this" parameter
            List<Method.Parameter> parameters = new ArrayList<>();
            for (int i = 1; i < parameterCount + 1; i++) {
                LocalVariableNode parameter = method.localVariables.get(i);
                parameters.add(new Method.Parameter(parameterTypes.get(i - 1), parameter.name));
            }

            // Convert to GLSL types
            Type returnType = new Type(returnTypeStr);

            // TODO: Instructions
            // Prepare the statement generator
            StatementGenerator.Context context = new StatementGenerator.Context(method);
            StatementGenerator statementGenerator = new StatementGenerator(settings);

            // Generate the statements
            List<Statement> statements = statementGenerator.generate(context);

            // Add the method
            methods.add(new Method(returnType, method.name, parameters, new Body(statements)));
        }

        return methods;
    }

    private boolean hasAnnotation(FieldNode field, Class<? extends Annotation> annotation) {
        // TODO: Find a better way to get the annotation description than this
        String uniformAnnotationName = "L" + annotation.getTypeName().replace('.', '/') + ";";
        if (field.visibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotationNode : field.visibleAnnotations) {
            if (Objects.equals(annotationNode.desc, uniformAnnotationName)) {
                return true;
            }
        }
        return false;
    }

    public record Settings(int version, boolean debug, boolean removeThisKeyword) {
        public static final Settings DEFAULT = new Settings(330, false, true); // TODO: Flesh this out
    }

}
