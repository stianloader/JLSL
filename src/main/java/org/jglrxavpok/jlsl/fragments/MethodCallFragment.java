package org.jglrxavpok.jlsl.fragments;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.function.Consumer;

public record MethodCallFragment(
    String methodName,
    InvokeTypes invokeType,
    AnnotationFragment[] annotations,
    String methodOwner,
    String[] argumentsTypes,
    String returnType
) implements CodeFragment.Data {

    public MethodCallFragment(MethodCallFragment other) {
        this(other.methodName, other.invokeType, other.annotations, other.methodOwner, other.argumentsTypes, other.returnType);
    }

    public enum InvokeTypes {
        STATIC, VIRTUAL, SPECIAL
    }

    public class Modifier {

        private String methodName;
        private InvokeTypes invokeType;
        private AnnotationFragment[] annotations;
        private String methodOwner;
        private String[] argumentsTypes;
        private String returnType;

        private Modifier(MethodCallFragment other) {
            this.methodName = other.methodName;
            this.invokeType = other.invokeType;
            this.annotations = Arrays.copyOf(other.annotations, other.annotations.length);
            this.methodOwner = other.methodOwner;
            this.argumentsTypes = Arrays.copyOf(other.argumentsTypes, other.argumentsTypes.length);
            this.returnType = other.returnType;
        }

        public Modifier methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Modifier invokeType(InvokeTypes invokeType) {
            this.invokeType = invokeType;
            return this;
        }

        public Modifier annotations(AnnotationFragment... annotations) {
            this.annotations = annotations;
            return this;
        }

        public Modifier methodOwner(String methodOwner) {
            this.methodOwner = methodOwner;
            return this;
        }

        public Modifier argumentsTypes(String... argumentsTypes) {
            this.argumentsTypes = argumentsTypes;
            return this;
        }

        public Modifier returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        private MethodCallFragment build() {
            return new MethodCallFragment(methodName, invokeType, annotations, methodOwner, argumentsTypes, returnType);
        }
    }

    /**
     * Creates a new MethodCallFragment modified with the given modifier consumer
     * @param modifier the modifier consumer
     * @return the modified MethodCallFragment
     */
    public MethodCallFragment modify(Consumer<Modifier> modifier) {
        Modifier m = new Modifier(this);
        modifier.accept(m);
        return m.build();
    }
}
