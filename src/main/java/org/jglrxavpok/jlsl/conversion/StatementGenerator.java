package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.jlsl.OpcodeUtils;
import org.jglrxavpok.jlsl.Stacker;
import org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A single threaded statement generator
 */
class StatementGenerator {

    private final ASMClassnode2GLSL.Settings settings;

    record Context(@NotNull MethodNode method) {
    }

    public StatementGenerator(@NotNull ASMClassnode2GLSL.Settings settings) {
        this.settings = settings;
        setup();
    }

    private void debug(Object text) {
        if (settings.debug())
            stacker.stack(new GLSLBytecode.Comment.Line("" + text));
    }

    private List<LocalVariableNode> locals;
    private Stacker<GLSLBytecode.Statement> stacker;
    private Deque<GLSLBytecode.Node> stack;
    private Queue<AbstractInsnNode> nodeQueue;

    public @NotNull List<GLSLBytecode.Statement> generate(@NotNull Context context) {
        MethodNode method = context.method;
        this.locals = method.localVariables;
        InsnList instructions = method.instructions;

        List<AbstractInsnNode> nodes = Stream.of(instructions.toArray())
                .collect(Collectors.toCollection(ArrayList::new));
        List<GLSLBytecode.Statement> statements = new ArrayList<>();
        Stacker<GLSLBytecode.Statement> stacker = new Stacker<>(statements);
        Deque<GLSLBytecode.Node> stack = new ArrayDeque<>();
        Queue<AbstractInsnNode> nodeQueue = new ArrayDeque<>(nodes);

        this.stacker = stacker;
        this.stack = stack;
        this.nodeQueue = nodeQueue;

        while (!nodeQueue.isEmpty()) {
            process();
        }

        if (!stack.isEmpty()) {
            debug("Auxiliary stack has leftover components: ");
            List<GLSLBytecode.Node> leftover = new ArrayList<>(stack);

            Collections.reverse(leftover);

            for (GLSLBytecode.Node node : leftover) {
                debug(" - " + node);
                if (node instanceof GLSLBytecode.Statement statement) {
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
        Objects.requireNonNull(current);
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
        registerProcessor(TypeInsnNode.class, this::processTypeInsn);
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
            String type = JavaUtils.getTypeFromDescription(local.desc);
            GLSLBytecode.Node previous = stack.poll();
            Objects.requireNonNull(previous, "ASTORE without previous value");
            if (previous instanceof GLSLBytecode.Value value) {
                stacker.stack(new GLSLBytecode.Statement.DeclareVariable(new GLSLBytecode.Type(type), local.name, value));
            } else {
                throw new IllegalArgumentException("ASTORE without previous value");
            }
        } else {
            stack.push(new GLSLBytecode.Value.Reference.Variable(local.name));
        }
    }

    private void processFieldInsnNode(FieldInsnNode node) {
        int opcode = node.getOpcode();
        switch (opcode) {
            case Opcodes.GETFIELD -> {
                GLSLBytecode.Node next = stack.poll();
                Objects.requireNonNull(next, "FieldInsnNode without a previous node");
                if (next instanceof GLSLBytecode.Value value) {

                    if (settings.removeThisKeyword() &&
                            value instanceof GLSLBytecode.Value.Reference.Variable variable &&
                            variable.reference().equals("this")
                    ) {
                        // Check if the value being indexed is a "this" access, and remove it
                        value = new GLSLBytecode.Value.Reference.Variable(node.name);
                    } else {
                        value = value.index(node.name);
                    }

                    stack.push(value);
                } else throw new IllegalStateException("FieldInsnNode invalid state: " + next);
            }
            case Opcodes.PUTFIELD -> {
                GLSLBytecode.Node assignment = stack.poll();
                GLSLBytecode.Node assignTo = stack.poll();

                Objects.requireNonNull(assignment, "FieldInsnNode without a previous node");
                Objects.requireNonNull(assignTo, "FieldInsnNode without a previous node");

                if (assignTo instanceof GLSLBytecode.Value.Reference reference && assignment instanceof GLSLBytecode.Value value) {
                    stack.push(new GLSLBytecode.Statement.UpdateVariable(reference.index(node.name), value));
                } else throw new IllegalStateException("FieldInsnNode invalid state");
            }
        }
    }

    private void processMethodInsnNode(MethodInsnNode insn) {
        if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
            JavaUtils.MethodSignature signature = JavaUtils.toSignature(insn.desc);
            List<GLSLBytecode.Value> parameters = new ArrayList<>(signature.argTypes().size());

            for (int i = 0; i < signature.argTypes().size(); i++) {
                GLSLBytecode.Node prev = stack.pop();
                if (prev instanceof GLSLBytecode.Value variable) {
                    parameters.add(variable);
                } else {
                    throw new IllegalStateException("MethodInsnNode invalid state");
                }
            }

            // Reverse the parameters
            Collections.reverse(parameters);

            // Now we have the parameters, we just need to get the previous node, check if it's a value, and then
            // push the result
            GLSLBytecode.Node prev = stack.peek();
            GLSLBytecode.Value previousValue;
            if (prev instanceof GLSLBytecode.Value value) {
                stack.poll();
                if (settings.removeThisKeyword() &&
                        value instanceof GLSLBytecode.Value.Reference.Variable variable &&
                        variable.reference().equals("this")
                ) {
                    // Check if the value being indexed is a "this" access, and remove it
                    previousValue = new GLSLBytecode.Value.Reference.Variable(insn.name);
                } else {
                    previousValue = value.index(insn.name);
                }
            } else {
                previousValue = new GLSLBytecode.Value.Reference.Variable(insn.name);
            }

            // Check if this is method's result is listened to, or not
            boolean listenedTo = nodeQueue.peek() instanceof VarInsnNode || nodeQueue.peek() instanceof FieldInsnNode;
            this.stack.addFirst(new GLSLBytecode.Value.MethodCall(previousValue, listenedTo, parameters));
        } else {
            throw new UnsupportedOperationException("Unsupported opcode: " + insn.getOpcode());
        }
    }

    private void processTypeInsn(TypeInsnNode insn) {
        if (insn.getOpcode() == Opcodes.NEW) {
            // Bulk logic should be handled by the INVOKESPECIAL method
        } else {
            throw new IllegalStateException("TypeInsnNodes for opcodes other than NEW are unsupported at the moment. Opcode is: " + insn.getOpcode());
        }
    }

    private void processLdcInsnNode(LdcInsnNode node) {
        // an LDC node is either a String, a Number, or a Class
        stack.push(new GLSLBytecode.Value.Constant(node.cst));
    }

    private void processInsnNode(InsnNode node) {
        // DCMPG means "double compare"
        int opcode = node.getOpcode();

        switch (opcode) {
            case Opcodes.DCONST_0 -> stack.push(new GLSLBytecode.Value.Constant(0.0));
            case Opcodes.DCONST_1 -> stack.push(new GLSLBytecode.Value.Constant(1.0));
            case Opcodes.FCONST_0 -> stack.push(new GLSLBytecode.Value.Constant(0.0f));
            case Opcodes.FCONST_1 -> stack.push(new GLSLBytecode.Value.Constant(1.0f));
            case Opcodes.FCONST_2 -> stack.push(new GLSLBytecode.Value.Constant(2.0f));
            case Opcodes.ICONST_0 -> stack.push(new GLSLBytecode.Value.Constant(0));
            case Opcodes.ICONST_1 -> stack.push(new GLSLBytecode.Value.Constant(1));
            case Opcodes.ICONST_2 -> stack.push(new GLSLBytecode.Value.Constant(2));
            case Opcodes.ICONST_3 -> stack.push(new GLSLBytecode.Value.Constant(3));
            case Opcodes.RETURN -> stack.push(new GLSLBytecode.Statement.Return(null));
            case Opcodes.DCMPG, Opcodes.DCMPL -> {
                AbstractInsnNode nextNode = nodeQueue.poll();

                if (nextNode instanceof JumpInsnNode jumpNode) {
                    int jumpOpcode = jumpNode.getOpcode();
                    switch (jumpOpcode) {
                        case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE -> {
                            // We have a comparison, we need to remove the first two values from the stack
                            GLSLBytecode.Node left = stack.pop();
                            GLSLBytecode.Node right = stack.pop();

                            // These should both be values
                            if (!(left instanceof GLSLBytecode.Value leftValue) || !(right instanceof GLSLBytecode.Value rightValue)) {
                                throw new IllegalStateException("InsnNode invalid state");
                            }

                            GLSLBytecode.Statement.Condition condition = switch (jumpOpcode) {
                                case Opcodes.IFGE -> new GLSLBytecode.Statement.Condition.GreaterThanOrEqualTo(leftValue, rightValue);
                                case Opcodes.IFGT -> new GLSLBytecode.Statement.Condition.GreaterThan(leftValue, rightValue);
                                case Opcodes.IFLE -> new GLSLBytecode.Statement.Condition.LessThanOrEqualTo(leftValue, rightValue);
                                case Opcodes.IFLT -> new GLSLBytecode.Statement.Condition.LessThan(leftValue, rightValue);
                                case Opcodes.IFEQ -> new GLSLBytecode.Statement.Condition.EqualTo(leftValue, rightValue);
                                case Opcodes.IFNE -> new GLSLBytecode.Statement.Condition.NotEqualTo(leftValue, rightValue);
                                default -> throw new IllegalStateException("Unexpected value: " + jumpOpcode);
                            };
                            stack.push(condition);
                        }
                    }
                } else {
                    throw new IllegalStateException("InsnNode invalid state");
                }
            }
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
                List<GLSLBytecode.Statement> body = new ArrayList<>();

                while (!(stack.peek() instanceof GLSLBytecode.Statement.Condition condition)) {
                    GLSLBytecode.Statement statement = (GLSLBytecode.Statement) stack.poll();
                    if (statement == null) {
                        throw new IllegalStateException("FrameNode invalid state");
                    }
                    body.add(statement);
                }
                stack.poll();

                // Reverse the body so it is in the correct order
                Collections.reverse(body);

                // We have the condition, now we create the if statement
                stacker.stack(new GLSLBytecode.Statement.If(condition, new GLSLBytecode.Body(body)));
            }
        }
    }
}
