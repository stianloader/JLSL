package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.jlsl.glsl.GLSL;
import org.objectweb.asm.tree.*;

import java.lang.annotation.Annotation;
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

    static @NotNull String getTypeFromDescription(String desc) {
        // Example:
        // Lorg/jglrxavpok/jlsl/glsl/Sampler2D;
        // I
        // F
        char firstChar = desc.charAt(0);
        return switch (firstChar) {
            case 'I' -> "int";
            case 'F' -> "float";
            case 'L' -> {
                // Remove the first and last character
                desc = desc.substring(1, desc.length() - 1);

                // Replace / with .
                desc = desc.replace('/', '.');

                // Now we have the package and class callingfield, now we need to check if the class is a native GLSL class
                try {
                    Class<?> clazz = Class.forName(desc);
                    for (Annotation annotation : clazz.getAnnotations()) {
                        if (annotation instanceof GLSL.NativeClass nativeClass) {
                            yield nativeClass.name();
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                // At this point we know the class is not a native GLSL class, so we can just return the class callingfield
                yield desc.substring(desc.lastIndexOf('.') + 1);
            }
            default -> throw new IllegalArgumentException("Unknown type: " + desc);
        };
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
}
