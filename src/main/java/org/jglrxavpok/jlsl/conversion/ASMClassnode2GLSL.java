package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;
import static org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode.*;

import org.jglrxavpok.jlsl.OpcodeUtils;
import org.jglrxavpok.jlsl.Stacker;
import org.jglrxavpok.jlsl.glsl.GLSL;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

            // Then inputs
            if (hasAnnotation(field, GLSL.In.class)) {
                type = Field.Type.IN;
            }

            // Then outputs
            if (hasAnnotation(field, GLSL.Out.class)) {
                type = Field.Type.OUT;
            }

            String fieldTypeStr = getTypeFromDescription(field.desc);
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
                parameters.add(new Method.Parameter(parameter.name, parameterTypes.get(i - 1)));
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

    private static @NotNull String getTypeFromDescription(String desc) {
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

    /**
     * A single threaded statement generator
     */
    private static class StatementGenerator {

        private final Settings settings;

        record Context(@NotNull MethodNode method) {
        }

        public StatementGenerator(@NotNull Settings settings) {
            this.settings = settings;
            setup();
        }

        private void debug(Object text) {
            if (settings.debug())
                stacker.stack(new Comment.Line("" + text));
        }

        private List<LocalVariableNode> locals;
        private Stacker<Statement> stacker;
        private Deque<Node> stack;
        private Queue<AbstractInsnNode> nodeQueue;

        public @NotNull List<Statement> generate(@NotNull Context context) {
            MethodNode method = context.method;
            this.locals = method.localVariables;
            InsnList instructions = method.instructions;

            List<AbstractInsnNode> nodes = Stream.of(instructions.toArray())
                    .collect(Collectors.toCollection(ArrayList::new));
            List<Statement> statements = new ArrayList<>();
            Stacker<Statement> stacker = new Stacker<>(statements);
            Deque<Node> stack = new ArrayDeque<>();
            Queue<AbstractInsnNode> nodeQueue = new ArrayDeque<>(nodes);

            this.stacker = stacker;
            this.stack = stack;
            this.nodeQueue = nodeQueue;

            while (!nodeQueue.isEmpty()) {
                process();
            }

            if (!stack.isEmpty()) {
                debug("Auxiliary stack has leftover components: ");
                List<Node> leftover = new ArrayList<>(stack);

                Collections.reverse(leftover);

                for (Node node : leftover) {
                    debug(" - " + node);
                    if (node instanceof Statement statement) {
                        stacker.stack(statement);
                    }
                }
            }

            return statements;
        }

        // Processors map, Objects are used to avoid generic mess
        private final Map<Object, Object> processors = new HashMap<>();
        private <N extends AbstractInsnNode> void registerProcessor(Class<N> key, Consumer<? super N> processor) {
            processors.put(key, processor);
        }

        private void process() {
            AbstractInsnNode current = nodeQueue.poll();
            Class<? extends AbstractInsnNode> clazz = current.getClass();
            Object processor = processors.get(clazz);
            debug("NODE - " + clazz.getSimpleName() + " -> " + OpcodeUtils.name(current.getOpcode()));
            if (processor == null) {
                throw new IllegalArgumentException("Unknown instruction: " + clazz.getSimpleName());
            }
            //noinspection unchecked
            ((Consumer<AbstractInsnNode>) processor).accept(current);
        }

        private void setup() {
            // Setup all the processors
            registerProcessor(LabelNode.class, this::processLabelNode);
            registerProcessor(LineNumberNode.class, this::processLineNumberNode);
            registerProcessor(VarInsnNode.class, this::processVarInsnNode);
            registerProcessor(FieldInsnNode.class, this::processFieldInsnNode);
            registerProcessor(MethodInsnNode.class, this::processMethodInsnNode);
            registerProcessor(LdcInsnNode.class, this::processLdcInsnNode);
            registerProcessor(InsnNode.class, this::processInsnNode);
            registerProcessor(JumpInsnNode.class, this::processJumpInsnNode);
            registerProcessor(FrameNode.class, this::processFrameNode);
        }

        private void processLabelNode(LabelNode node) {
            // Labels can be ignored
        }

        private void processLineNumberNode(LineNumberNode node) {
//            stacker.stack(new Comment.Block("Line number -> " + node.line));
        }

        private void processVarInsnNode(VarInsnNode node) {
            int index = node.var;
            int opcode = node.getOpcode();
            LocalVariableNode local = locals.get(index);

            if (opcode == Opcodes.ASTORE) {
                String type = getTypeFromDescription(local.desc);
                Node previous = stack.poll();
                Objects.requireNonNull(previous, "ASTORE without previous value");
                if (previous instanceof Value value) {
                    stacker.stack(new Statement.DeclareVariable(new Type(type), local.name, value));
                } else {
                    throw new IllegalArgumentException("ASTORE without previous value");
                }
            } else {
                stack.push(new Value.Reference.Variable(local.name));
            }
        }

        private void processFieldInsnNode(FieldInsnNode node) {
            int opcode = node.getOpcode();
            switch (opcode) {
                case Opcodes.GETFIELD -> {
                    Node next = stack.poll();
                    Objects.requireNonNull(next, "FieldInsnNode without a previous node");
                    if (next instanceof Value value) {

                        if (settings.removeThisKeyword() &&
                                value instanceof Value.Reference.Variable variable &&
                                variable.reference().equals("this")
                        ) {
                            // Check if the value being indexed is a "this" access, and remove it
                            value = new Value.Reference.Variable(node.name);
                        } else {
                            value = value.index(node.name);
                        }

                        stack.push(value);
                    } else throw new IllegalStateException("FieldInsnNode invalid state: " + next);
                }
                case Opcodes.PUTFIELD -> {
                    Node assignment = stack.poll();
                    Node assignTo = stack.poll();

                    Objects.requireNonNull(assignment, "FieldInsnNode without a previous node");
                    Objects.requireNonNull(assignTo, "FieldInsnNode without a previous node");

                    if (assignTo instanceof Value.Reference reference && assignment instanceof Value value) {
                        stack.push(new Statement.UpdateVariable(reference.index(node.name), value));
                    } else throw new IllegalStateException("FieldInsnNode invalid state");
                }
            }
        }

        private void processMethodInsnNode(MethodInsnNode node) {
            int opcode = node.getOpcode();
            switch (opcode) {
                case Opcodes.INVOKEVIRTUAL -> {
                    JavaUtils.MethodSignature signature = JavaUtils.toSignature(node.desc);

                    List<Value> parameters = new ArrayList<>(signature.argTypes().size());

                    for (int i = 0; i < signature.argTypes().size(); i++) {
                        Node prev = stack.pop();
                        if (prev instanceof Value variable) {
                            parameters.add(variable);
                        } else {
                            throw new IllegalStateException("MethodInsnNode invalid state");
                        }
                    }

                    // Reverse the parameters
                    Collections.reverse(parameters);

                    // Now we have the parameters, we just need to get the previous node, check if it's a value, and then
                    // push the result
                    Node prev = stack.poll();
                    Value previousValue;
                    if (prev instanceof Value value) {
                        if (settings.removeThisKeyword() &&
                                value instanceof Value.Reference.Variable variable &&
                                variable.reference().equals("this")
                        ) {
                            // Check if the value being indexed is a "this" access, and remove it
                            previousValue = new Value.Reference.Variable(node.name);
                        } else {
                            previousValue = value.index(node.name);
                        }
                    } else {
                        if (prev != null) {
                            stack.push(prev);
                        }
                        previousValue = new Value.Reference.Variable(node.name);
                    }

                    stack.addFirst(new Value.MethodCall(previousValue, parameters));
                }
            }
        }

        private void processLdcInsnNode(LdcInsnNode node) {
            // an LDC node is either a String, a Number, or a Class
            stack.push(new Value.Constant(node.cst));
        }

        private void processInsnNode(InsnNode node) {
            // DCMPG means "double compare"
            int opcode = node.getOpcode();

            switch (opcode) {
                case Opcodes.DCMPG, Opcodes.DCMPL -> {
                    AbstractInsnNode nextNode = nodeQueue.poll();

                    if (nextNode instanceof JumpInsnNode jumpNode) {
                        int jumpOpcode = jumpNode.getOpcode();
                        switch (jumpOpcode) {
                            case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE -> {
                                // We have a comparison, we need to remove the first two values from the stack
                                Node left = stack.pop();
                                Node right = stack.pop();

                                // These should both be values
                                if (!(left instanceof Value leftValue) || !(right instanceof Value rightValue)) {
                                    throw new IllegalStateException("InsnNode invalid state");
                                }

                                Statement.Condition condition = switch (jumpOpcode) {
                                    case Opcodes.IFGE -> new Statement.Condition.GreaterThanOrEqualTo(leftValue, rightValue);
                                    case Opcodes.IFGT -> new Statement.Condition.GreaterThan(leftValue, rightValue);
                                    case Opcodes.IFLE -> new Statement.Condition.LessThanOrEqualTo(leftValue, rightValue);
                                    case Opcodes.IFLT -> new Statement.Condition.LessThan(leftValue, rightValue);
                                    case Opcodes.IFEQ -> new Statement.Condition.EqualTo(leftValue, rightValue);
                                    case Opcodes.IFNE -> new Statement.Condition.NotEqualTo(leftValue, rightValue);
                                    default -> throw new IllegalStateException("Unexpected value: " + jumpOpcode);
                                };
                                stack.push(condition);
                            }
                        }
                    } else {
                        throw new IllegalStateException("InsnNode invalid state");
                    }
                }
                case Opcodes.DCONST_0 -> stack.push(new Value.Constant(0.0));
                case Opcodes.RETURN -> stack.push(new Statement.Return(null));
            }
        }

        private void processJumpInsnNode(JumpInsnNode node) {
            // IFGE means "if greater or equal"
        }

        private void processFrameNode(FrameNode node) {
            // Frame nodes are used to end a section of code
            switch (node.type) {
                case Opcodes.F_NEW -> debug("FrameNode: F_NEW");
                case Opcodes.F_FULL -> debug("FrameNode: F_FULL");
                case Opcodes.F_CHOP -> debug("FrameNode: F_CHOP");
                case Opcodes.F_SAME1 -> debug("FrameNode: F_SAME1");
                case Opcodes.F_APPEND, Opcodes.F_SAME -> {
                    // End of a section of code

                    // Keep going down the stack until we find a condition for an if statement
                    List<Statement> body = new ArrayList<>();

                    while (!(stack.peek() instanceof Statement.Condition condition)) {
                        Statement statement = (Statement) stack.poll();
                        if (statement == null) {
                            throw new IllegalStateException("FrameNode invalid state");
                        }
                        body.add(statement);
                    }
                    stack.poll();

                    // Reverse the body so it is in the correct order
                    Collections.reverse(body);

                    // We have the condition, now we create the if statement
                    stacker.stack(new Statement.If(condition, new Body(body)));
                }
            }
        }
    }
}
