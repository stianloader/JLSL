package org.jglrxavpok.jlsl.java;

import org.jglrxavpok.jlsl.fragments.AccessPolicy;
import org.jglrxavpok.jlsl.fragments.AnnotationFragment;
import org.jglrxavpok.jlsl.fragments.FieldFragment;
import org.jglrxavpok.jlsl.fragments.StartOfMethodFragment;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

class JavaUtils {
    static AnnotationFragment[] toAnnotationFragments(List<AnnotationNode> nodes) {
        if (nodes == null)
            return new AnnotationFragment[0];
        AnnotationFragment[] fragments = new AnnotationFragment[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            fragments[i] = createFromNode(nodes.get(i));
        }
        return fragments;
    }

    static StartOfMethodFragment toStartOfMethodFragment(ClassNode clazz, MethodNode node) {
        AccessPolicy access = new AccessPolicy(node.access);
        String name = node.name;
        String owner = clazz.name.replace("/", ".");
        String returnType = typesFromDesc(node.desc.substring(node.desc.indexOf(")") + 1))[0];
        AnnotationFragment[] annotations = toAnnotationFragments(node.visibleAnnotations);
        StartOfMethodFragment fragment = new StartOfMethodFragment(access, name, owner, annotations, returnType);
        List<String> localNames = JavaUtils.toLocalNames(fragment, node.localVariables);

        String[] argsTypes = typesFromDesc(node.desc.substring(node.desc.indexOf('(') + 1, node.desc.indexOf(')')));
        for (int i = 0; i < argsTypes.length; i++) {
            String argType = argsTypes[i];
            fragment.argumentsTypes().add(argType);
            String localName = localNames.isEmpty() ? ("var" + i) : localNames.get(i);
            fragment.argumentsNames().add(localName);
        }
        return fragment;
    }

    static FieldFragment[] toFieldFragments(List<FieldNode> fields) {
        if (fields == null)
            return new FieldFragment[0];
        FieldFragment[] fragments = new FieldFragment[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            FieldNode field = fields.get(i);
            String name = field.name;
            String type = typesFromDesc(field.desc)[0];
            Object initialValue = field.value;
            AccessPolicy fieldAccess = new AccessPolicy(field.access);
            AnnotationFragment[] fieldAnnotations = JavaUtils.toAnnotationFragments(field.visibleAnnotations);
            fragments[i] = new FieldFragment(name, type, fieldAccess, fieldAnnotations, initialValue);
        }
        return fragments;
    }

    static AnnotationFragment createFromNode(AnnotationNode annotNode) {
        String name = typesFromDesc(annotNode.desc)[0].replace("/", ".").replace("$", ".");
        AnnotationFragment annotFragment = new AnnotationFragment(name);
        List<Object> values = annotNode.values;
        if (values != null) {
            for (int index = 0; index < values.size(); index += 2) {
                String key = (String) values.get(index);
                Object value = values.get(index + 1);
                annotFragment.values().put(key, value);
            }
        }
        return annotFragment;
    }

    static String[] typesFromDesc(String desc, int startPos) {
        boolean parsingObjectClass = false;
        boolean parsingArrayClass = false;
        ArrayList<String> types = new ArrayList<>();
        String currentObjectClass = null;
        StringBuilder currentArrayClass = null;
        int dims = 1;
        for (int i = startPos; i < desc.length(); i++) {
            char c = desc.charAt(i);

            if (!parsingObjectClass && !parsingArrayClass) {
                switch (c) {
                    case '[' -> {
                        parsingArrayClass = true;
                        currentArrayClass = new StringBuilder();
                    }
                    case 'L' -> {
                        parsingObjectClass = true;
                        currentObjectClass = "";
                    }
                    case 'I' -> types.add("int");
                    case 'D' -> types.add("double");
                    case 'B' -> types.add("byte");
                    case 'Z' -> types.add("boolean");
                    case 'V' -> types.add("void");
                    case 'J' -> types.add("long");
                    case 'C' -> types.add("char");
                    case 'F' -> types.add("float");
                    case 'S' -> types.add("short");
                }
                continue;
            }
            if (parsingObjectClass) {
                if (c == ';') {
                    parsingObjectClass = false;
                    types.add(currentObjectClass);
                    continue;
                }
                currentObjectClass = currentObjectClass + currentObjectClass;
                continue;
            }
            if (c == '[') {
                dims++;
            } else {

                if (c == '/') {
                    c = '.';
                }
                if (c != 'L') {
                    if (c == ';') {
                        parsingArrayClass = false;
                        types.add("" + currentArrayClass + currentArrayClass);
                        dims = 1;
                    } else {

                        currentArrayClass.append(c);
                    }
                }
            }
        }
        if (parsingObjectClass) {
            types.add(currentObjectClass);
        }
        if (parsingArrayClass) {
            types.add(currentArrayClass.toString() + currentArrayClass);
        }
        return types.toArray(new String[0]);
    }

    static String[] typesFromDesc(String desc) {
        return typesFromDesc(desc, 0);
    }

    static List<String> toLocalNames(StartOfMethodFragment fragment, List<LocalVariableNode> localVariables) {
        List<String> localNames = new ArrayList<>();
        for (LocalVariableNode var : localVariables) {
            fragment.varNameMap().put(var.index, var.name);
            fragment.varTypeMap().put(var.index, typesFromDesc(var.desc)[0]);
            fragment.varName2TypeMap().put(var.name, typesFromDesc(var.desc)[0]);
            if (var.index != 0 || fragment.access().isStatic()) {
                localNames.add(var.name);
            }
        }
        return localNames;
    }
}
