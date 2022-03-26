package org.jglrxavpok.jlsl.conversion;

import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.Collectors;

class JavaUtils {

    static MethodSignature toSignature(String description) {
        // Examples:
        // void callingfield() -> ()V
        // int callingfield(int) -> (I)I
        // int callingfield(int, int) -> (II)I
        // ExampleClass callingfield(int, int) -> (II)LExampleClass;
        // java.lang.String callingfield(int, int) -> (II)Ljava/lang/String;
        // java.util.List<java.lang.String> callingfield(int, int) -> (II)Ljava/util/List<Ljava/lang/String;>;
        // TODO: Handle generics and arrays

        // Remove the return type
        char returnType = description.charAt(description.indexOf(')') + 1);
        String returnTypeStr = getTypeString(returnType);
        if (returnTypeStr == null) {
            throw new IllegalArgumentException("Invalid return type: " + returnType);
        }
        if (returnTypeStr.equals("object")) {
            // Get the contents after the ')'
            int endArgs = description.indexOf(")");
            String contents = description.substring(endArgs + 1);

            // Get the class callingfield
            String className = contents.substring(1, contents.length() - 1);
            returnTypeStr = className.replace("/", ".");

            // Remove the class callingfield from the description
            description = description.substring(0, endArgs + 1);
        } else {
            // Remove the return type from the description
            description = description.substring(0, description.length() - 1);
        }

        // Remove the '(' and ')'
        description = description.substring(1, description.length() - 1);

        Queue<String> types = new ArrayDeque<>();
        while (description.length() > 0) {
            String typeStr = getTypeString(description.charAt(0));
            if (typeStr == null) {
                throw new IllegalArgumentException("Invalid type: " + description.charAt(0));
            }

            String type = switch (typeStr) {
                // ILorg/jglrxavpok/jlsl/NewTest$ExampleShader;
                // In the case of an object, we need to get the section of the string between the 'L' and ';'
                case "object" -> {
                    int end = description.indexOf(';');
                    if (end == -1) {
                        throw new IllegalArgumentException("Invalid type: " + description);
                    }
                    String objectType = description.substring(1, end);
                    description = description.substring(end + 1);
                    objectType = objectType.replace("/", ".");
                    yield objectType;
                }
                default -> {
                    description = description.substring(1);
                    yield typeStr;
                }
            };
            types.add(type);
        }
        return new MethodSignature(returnTypeStr, List.copyOf(types));
    }

    private static String getTypeString(char type) {
        return switch (type) {
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'Z' -> "boolean";
            case 'V' -> "void";
            case 'L' -> "object";
            default -> null;
        };
    }

    public record MethodSignature(String returnType, List<String> argTypes) {
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

    static String typeFromDesc(String desc) {
        if (desc.length() < 2) {
            return desc;
        }
        // Example: Lorg/jglrxavpok/jlsl/glsl/Vec2;
        // First remove the 'L' and ';'
        desc = desc.substring(1, desc.length() - 1);
        // Then replace '/' with '.'
        return desc.replace('/', '.');
    }

    static List<String> toLocalNames(List<LocalVariableNode> localVariables) {
        return localVariables.stream().map(node -> node.name).collect(Collectors.toList());
    }
}
