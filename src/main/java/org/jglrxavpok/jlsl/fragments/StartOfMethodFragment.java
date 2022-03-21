package org.jglrxavpok.jlsl.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public record StartOfMethodFragment(
    AccessPolicy access,
    String name,
    String owner,
    AnnotationFragment[] annotations,
    String returnType,
    List<String> argumentsTypes,
    List<String> argumentsNames,
    Map<Integer, String> varNameMap,
    Map<Integer, String> varTypeMap,
    Map<String, String> varName2TypeMap
) implements CodeFragment.Data {
    public StartOfMethodFragment(AccessPolicy access, String name, String owner, AnnotationFragment[] annotations, String returnType) {
        this(access, name, owner, annotations, returnType, new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public StartOfMethodFragment modify(Consumer<Modifier> modifier) {
        Modifier m = new Modifier(this);
        modifier.accept(m);
        return m.build();
    }

    public static class Modifier {

        private AccessPolicy access;
        private String name;
        private String owner;
        private AnnotationFragment[] annotations;
        private String returnType;
        private final List<String> argumentsTypes;
        private final List<String> argumentsNames;
        private final Map<Integer, String> varNameMap;
        private final Map<Integer, String> varTypeMap;
        private final Map<String, String> varName2TypeMap;

        private Modifier(StartOfMethodFragment fragment) {
            this.access = fragment.access;
            this.name = fragment.name;
            this.owner = fragment.owner;
            this.annotations = fragment.annotations;
            this.returnType = fragment.returnType;
            this.argumentsTypes = new ArrayList<>(fragment.argumentsTypes);
            this.argumentsNames = new ArrayList<>(fragment.argumentsNames);
            this.varNameMap = new HashMap<>(fragment.varNameMap);
            this.varTypeMap = new HashMap<>(fragment.varTypeMap);
            this.varName2TypeMap = new HashMap<>(fragment.varName2TypeMap);
        }

        public Modifier access(AccessPolicy access) {
            this.access = access;
            return this;
        }

        public Modifier name(String name) {
            this.name = name;
            return this;
        }

        public Modifier owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Modifier annotations(AnnotationFragment[] annotations) {
            this.annotations = annotations;
            return this;
        }

        public Modifier returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Modifier argumentsTypes(Consumer<List<String>> argumentsTypesModifier) {
            argumentsTypesModifier.accept(this.argumentsTypes);
            return this;
        }

        public Modifier argumentsNames(Consumer<List<String>> argumentsNamesModifier) {
            argumentsNamesModifier.accept(this.argumentsNames);
            return this;
        }

        public Modifier varNameMap(Consumer<Map<Integer, String>> varNameMapModifier) {
            varNameMapModifier.accept(this.varNameMap);
            return this;
        }

        public Modifier varTypeMap(Consumer<Map<Integer, String>> varTypeMapModifier) {
            varTypeMapModifier.accept(this.varTypeMap);
            return this;
        }

        public Modifier varName2TypeMap(Consumer<Map<String, String>> varName2TypeMapModifier) {
            varName2TypeMapModifier.accept(this.varName2TypeMap);
            return this;
        }

        private StartOfMethodFragment build() {
            return new StartOfMethodFragment(access, name, owner, annotations, returnType, argumentsTypes,
                    argumentsNames, varNameMap, varTypeMap, varName2TypeMap);
        }
    }
}
