package org.jglrxavpok.jlsl.java;

import org.jglrxavpok.jlsl.CodeDecoder;
import org.jglrxavpok.jlsl.JLSLException;
import org.jglrxavpok.jlsl.fragments.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

import static org.jglrxavpok.jlsl.java.JavaUtils.*;

public class BytecodeDecoder extends CodeDecoder {
    public static final boolean DEBUG = true;



    private boolean instructionsFromInterfaces;
    
    private static class MethodNodeContext {
        InsnNode insnNode;
        LabelNode labelNode;
        FrameNode frameNode;
        JumpInsnNode jumpNode;
        LdcInsnNode ldc;
        VarInsnNode varNode;
        FieldInsnNode fieldNode;
        IntInsnNode intNode;
        TypeInsnNode typeNode;
        MultiANewArrayInsnNode multiArrayNode;
        LineNumberNode lineNode;
        MethodInsnNode methodNode;
        boolean instanceNodeMatched;
        int j;
        int i;
        String operand;
        String desc;
        String margs;
        String[] margsArray;
        String n;
        MethodCallFragment methodFragment;
        AbstractInsnNode ainsnNode;
    }

    private static void handleMethodNode(MethodNode node, Map<Integer, String> varTypeMap, Map<Integer, String> varNameMap, List<CodeFragment.Data> out) {
        int lastFrameType = 0;
        int frames = 0;
        int framesToSkip = 0;
        Stack<LabelNode> toJump = new Stack<>();
        Stack<Label> gotos = new Stack<>();
        Stack<Label> ifs = new Stack<>();
        InsnList instructions = node.instructions;
        Label currentLabel = null;
        for (int index = 0; index < instructions.size(); index++) {
            MethodNodeContext context = new MethodNodeContext();
            context.ainsnNode = instructions.get(index);
            int type = context.ainsnNode.getType();

            CodeFragment.Data data = switch (type) {
                case AbstractInsnNode.INSN -> {
                    context.insnNode = (InsnNode) context.ainsnNode;
                    context.instanceNodeMatched = false;
                    int opcode = context.insnNode.getOpcode();

                    yield switch (opcode) {
                        case Opcodes.ACONST_NULL -> new LoadConstantFragment(null);
                        case Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 -> new LoadConstantFragment(opcode - 3);
                        case Opcodes.FCONST_0 -> new LoadConstantFragment(0.0F);
                        case Opcodes.FCONST_1 -> new LoadConstantFragment(1.0F);
                        case Opcodes.FCONST_2 -> new LoadConstantFragment(2.0F);
                        case Opcodes.DCONST_0 -> new LoadConstantFragment(0.0D);
                        case Opcodes.DCONST_1 -> new LoadConstantFragment(1.0D);

                        default -> switch (context.ainsnNode.getOpcode()) {
                            case Opcodes.IRETURN, Opcodes.FRETURN, Opcodes.ARETURN, Opcodes.LRETURN, Opcodes.DRETURN -> new ReturnValueFragment();
                            case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD -> new AddFragment();
                            case Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB -> new SubFragment();
                            case Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL -> new MulFragment();
                            case Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV -> new DivFragment();
                            case Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM -> new ModFragment();
                            case Opcodes.D2I -> new CastFragment("double", "int");
                            case Opcodes.D2L -> new CastFragment("double", "long");
                            case Opcodes.D2F -> new CastFragment("double", "float");
                            case Opcodes.I2B -> new CastFragment("int", "byte");
                            case Opcodes.I2C -> new CastFragment("int", "char");
                            case Opcodes.I2S -> new CastFragment("int", "short");
                            case Opcodes.I2L -> new CastFragment("int", "long");
                            case Opcodes.I2F -> new CastFragment("int", "float");
                            case Opcodes.L2I -> new CastFragment("long", "int");
                            case Opcodes.L2F -> new CastFragment("long", "float");
                            case Opcodes.L2D -> new CastFragment("long", "double");
                            case Opcodes.F2I -> new CastFragment("float", "int");
                            case Opcodes.F2L -> new CastFragment("float", "long");
                            case Opcodes.F2D -> new CastFragment("float", "double");
                            case Opcodes.I2D -> new CastFragment("int", "double");
                            case Opcodes.ISHR -> new RightShiftFragment(true, "int");
                            case Opcodes.ISHL -> new LeftShiftFragment(true, "int");
                            case Opcodes.IUSHR -> new RightShiftFragment(false, "int");
                            case Opcodes.LSHR -> new RightShiftFragment(true, "long");
                            case Opcodes.LSHL -> new LeftShiftFragment(true, "long");
                            case Opcodes.LUSHR -> new RightShiftFragment(false, "long");
                            case Opcodes.IAND -> new AndFragment("int");
                            case Opcodes.LAND -> new AndFragment("long");
                            case Opcodes.IOR -> new OrFragment("int");
                            case Opcodes.LOR -> new OrFragment("long");
                            case Opcodes.IXOR -> new XorFragment("int");
                            case Opcodes.LXOR -> new XorFragment("long");
                            case Opcodes.POP -> new PopFragment();
                            case Opcodes.RETURN -> new ReturnFragment();
                            case Opcodes.DUP_X1, Opcodes.DUP2_X1 -> new DuplicateFragment(1);
                            case Opcodes.FCMPG, Opcodes.DCMPG -> new CompareFragment(true);
                            case Opcodes.FCMPL, Opcodes.DCMPL -> switch (instructions.get(index + 1).getOpcode()) {
                                case Opcodes.IFEQ, Opcodes.IFNE -> new NotEqualCheckFragment();
                                default -> new CompareFragment(false);
                            };
                            case Opcodes.IASTORE, Opcodes.FASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE, Opcodes.LASTORE, Opcodes.DASTORE -> new ArrayStoreFragment();
                            case Opcodes.AALOAD -> new ArrayOfArrayLoadFragment();
                            default -> null;
                        };
                    };
                }
                case AbstractInsnNode.LABEL -> {
                    context.labelNode = (LabelNode) context.ainsnNode;
                    currentLabel = context.labelNode.getLabel();
                    if (!toJump.isEmpty() &&
                            context.labelNode.getLabel().equals(toJump.peek().getLabel())) {
                        while (!toJump.isEmpty() && toJump.pop().getLabel().equals(context.labelNode.getLabel())) {
                            if (!gotos.isEmpty()) {
                                while (gotos.contains(currentLabel)) {
                                    if (frames > 0) {
                                        out.add(new EndOfBlockFragment());
                                        frames--;
                                    }
                                    gotos.remove(currentLabel);
                                }
                            }
                        }
                    }
                    yield null;
                }
                case AbstractInsnNode.FRAME -> {
                    context.frameNode = (FrameNode) context.ainsnNode;
                    if (framesToSkip > 0) {
                        framesToSkip--;
                    } else if (frames != 0) {
                        boolean a = (!ifs.isEmpty() && ifs.contains(currentLabel));
                        boolean b = (gotos.isEmpty() || !gotos.contains(currentLabel));
                        int nbr = 0;
                        if (a || b) {
                            while (ifs.contains(currentLabel)) {
                                nbr++;
                                ifs.remove(currentLabel);
                            }
                        }

                        for (int k = 0; k < nbr; k++) {
                            out.add(new EndOfBlockFragment());
                            frames--;
                        }
                    }
                    lastFrameType = context.frameNode.type;
                    yield null;
                }
                case AbstractInsnNode.JUMP_INSN -> {
                    context.jumpNode = (JumpInsnNode) context.ainsnNode;
                    int opcode = context.jumpNode.getOpcode();
                    yield switch (opcode) {
                        case Opcodes.IFEQ -> {
                            if (instructions.get(index - 1).getOpcode() == 21 &&
                                    instructions.get(index + 1).getOpcode() == 21 &&
                                    instructions.get(index + 2).getOpcode() == 153 &&
                                    instructions.get(index + 3).getOpcode() == 4 &&
                                    instructions.get(index + 4).getOpcode() == 167 &&
                                    instructions.get(index + 5).getType() == 8 &&
                                    instructions.get(index + 6).getType() == 14 &&
                                    instructions.get(index + 7).getOpcode() == 3 &&
                                    instructions.get(index + 8).getType() == 8 &&
                                    instructions.get(index + 9).getType() == 14 &&
                                    instructions.get(index + 10).getOpcode() == 54
                            ) {
                                int k = ((VarInsnNode) instructions.get(index + 1)).var;
                                out.add(new LoadVariableFragment(varNameMap.get(k), k));

                                out.add(new AndFragment("double"));

                                int operand1 = ((VarInsnNode) instructions.get(index + 10)).var;
                                index += 10;
                                yield new StoreVariableFragment(varNameMap.get(operand1), operand1, "int");
                            }
                            frames++;
                            ifs.push(context.jumpNode.label.getLabel());
                            toJump.push(context.jumpNode.label);
                            yield new IfStatementFragment(context.jumpNode.label.getLabel().toString());
                        }
                        case Opcodes.IF_ICMPEQ -> {
                            if (instructions.get(index + 1).getOpcode() == 4 &&
                                    (instructions.get(index + 2).getOpcode() == 172 || instructions.get(index + 2).getOpcode() == 54) &&
                                    instructions.get(index + 3).getType() == 8 &&
                                    instructions.get(index + 4).getType() == 14 &&
                                    instructions.get(index + 5).getOpcode() == 3 &&
                                    (instructions.get(index + 6).getOpcode() == 172 || instructions.get(index + 2).getOpcode() == 54)) {
                                index += 5;
                                yield new NotEqualCheckFragment();
                            }
                            out.add(new NotEqualCheckFragment());
                            frames++;
                            ifs.push(context.jumpNode.label.getLabel());
                            toJump.push(context.jumpNode.label);
                            yield new IfStatementFragment(context.jumpNode.label.getLabel().toString());
                        }
                        case Opcodes.IF_ICMPNE -> {
                            if (instructions.get(index + 1).getOpcode() == 4 &&
                                    (instructions.get(index + 2).getOpcode() == 172 || instructions.get(index + 2).getOpcode() == 54) &&
                                    instructions.get(index + 3).getType() == 8 &&
                                    instructions.get(index + 4).getType() == 14 &&
                                    instructions.get(index + 5).getOpcode() == 3 &&
                                    (instructions.get(index + 6).getOpcode() == 172 || instructions.get(index + 2).getOpcode() == 54)
                            ) {
                                index += 5;
                                yield new EqualCheckFragment();
                            }
                            out.add(new EqualCheckFragment());
                            frames++;
                            ifs.push(context.jumpNode.label.getLabel());
                            toJump.push(context.jumpNode.label);
                            yield new IfStatementFragment(context.jumpNode.label.getLabel().toString());
                        }
                        case Opcodes.IFNE -> {
                            if (instructions.get(index - 1).getOpcode() == 21 &&
                                    instructions.get(index + 1).getOpcode() == 21 &&
                                    instructions.get(index + 2).getOpcode() == 154 &&
                                    instructions.get(index + 3).getOpcode() == 3 &&
                                    instructions.get(index + 4).getOpcode() == 167 &&
                                    instructions.get(index + 5).getType() == 8 &&
                                    instructions.get(index + 6).getType() == 14 &&
                                    instructions.get(index + 7).getOpcode() == 4 &&
                                    instructions.get(index + 8).getType() == 8 &&
                                    instructions.get(index + 9).getType() == 14 &&
                                    instructions.get(index + 10).getOpcode() == 54
                            ) {
                                int k = ((VarInsnNode) instructions.get(index + 1)).var;
                                out.add(new LoadVariableFragment(varNameMap.get(k), k));

                                out.add(new OrFragment(null, true));

                                int operand1 = ((VarInsnNode) instructions.get(index + 10)).var;
                                index += 10;
                                yield new StoreVariableFragment(varNameMap.get(operand1), operand1, "int");
                            }
                            frames++;
                            ifs.push(context.jumpNode.label.getLabel());
                            toJump.push(context.jumpNode.label);
                            yield new IfNotStatementFragment(context.jumpNode.label.getLabel().toString());
                        }
                        case Opcodes.IFGE, Opcodes.IFLE -> {
                            frames++;
                            ifs.push(context.jumpNode.label.getLabel());
                            toJump.push(context.jumpNode.label);
                            yield new IfStatementFragment(context.jumpNode.label.getLabel().toString());
                        }
                        case Opcodes.GOTO -> {
                            toJump.push(context.jumpNode.label);
                            gotos.push(context.jumpNode.label.getLabel());
                            if (instructions.get(index - 1) instanceof LineNumberNode && lastFrameType == 3) {
                                EndOfBlockFragment endOfBlockFragment = new EndOfBlockFragment();
                                frames--;
                                out.add(endOfBlockFragment);
                            }

                            frames--;
                            out.add(new EndOfBlockFragment());

                            frames++;
                            out.add(new ElseStatementFragment());
                            framesToSkip = 1;
                            yield null;
                        }
                        default -> null;
                    };
                }
                case AbstractInsnNode.LDC_INSN -> {
                    context.ldc = (LdcInsnNode) context.ainsnNode;
                    yield new LdcFragment(context.ldc.cst);
                }
                case AbstractInsnNode.VAR_INSN -> {
                    context.varNode = (VarInsnNode) context.ainsnNode;
                    context.j = context.varNode.var;
                    yield switch (context.ainsnNode.getOpcode()) {
                        case Opcodes.ISTORE -> new StoreVariableFragment(varNameMap.get(context.j), context.j, "int");
                        case Opcodes.DSTORE -> new StoreVariableFragment(varNameMap.get(context.j), context.j, "double");
                        case Opcodes.LSTORE -> new StoreVariableFragment(varNameMap.get(context.j), context.j, "long");
                        case Opcodes.FSTORE -> new StoreVariableFragment(varNameMap.get(context.j), context.j, "float");
                        case Opcodes.ASTORE -> new StoreVariableFragment(varNameMap.get(context.j), context.j, varTypeMap.get(context.j));
                        case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD ->
                                new LoadVariableFragment(varNameMap.get(context.j), context.j);
                        default -> null;
                    };
                }
                case AbstractInsnNode.FIELD_INSN -> {
                    context.fieldNode = (FieldInsnNode) context.ainsnNode;
                    yield switch (context.fieldNode.getOpcode()) {
                        case Opcodes.PUTFIELD -> new PutFieldFragment(typesFromDesc(context.fieldNode.desc)[0], context.fieldNode.name);
                        case Opcodes.GETFIELD -> new GetFieldFragment(typesFromDesc(context.fieldNode.desc)[0], context.fieldNode.name);
                        default -> null;
                    };
                }
                case AbstractInsnNode.INT_INSN -> {
                    context.intNode = (IntInsnNode) context.ainsnNode;
                    context.i = context.intNode.operand;
                    yield switch (context.intNode.getOpcode()) {
                        case Opcodes.BIPUSH, Opcodes.SIPUSH -> new IntPushFragment(context.i);
                        case Opcodes.NEWARRAY -> new NewPrimitiveArrayFragment(Printer.TYPES[context.i]);
                        default -> null;
                    };
                }
                case AbstractInsnNode.TYPE_INSN -> {
                    context.typeNode = (TypeInsnNode) context.ainsnNode;
                    context.operand = context.typeNode.desc;
                    yield switch (context.typeNode.getOpcode()) {
                        case Opcodes.ANEWARRAY -> new NewArrayFragment(context.operand.replace("/", "."));
                        case Opcodes.CHECKCAST -> new CastFragment("java.lang.Object", context.operand.replace("/", "."));
                        case Opcodes.NEW -> new NewInstanceFragment(context.operand.replace("/", "."));
                        default -> null;
                    };
                }
                case AbstractInsnNode.MULTIANEWARRAY_INSN -> {
                    context.multiArrayNode = (MultiANewArrayInsnNode) context.ainsnNode;
                    String arrayType = typesFromDesc(context.multiArrayNode.desc)[0].replace("[]", "");
                    int dimensions = context.multiArrayNode.dims;
                    yield new NewMultiArrayFragment(arrayType, dimensions);
                }
                case AbstractInsnNode.LINE -> {
                    context.lineNode = (LineNumberNode) context.ainsnNode;
                    yield new LineNumberFragment(context.lineNode.line);
                }
                case AbstractInsnNode.METHOD_INSN -> {
                    context.methodNode = (MethodInsnNode) context.ainsnNode;
                    yield switch (context.methodNode.getOpcode()) {
                        case Opcodes.INVOKESTATIC -> {
                            context.desc = context.methodNode.desc;
                            context.margs = context.desc.substring(context.desc.indexOf('(') + 1, context.desc.indexOf(')'));
                            context.margsArray = typesFromDesc(context.margs);
                            context.n = context.methodNode.name;

                            MethodCallFragment.InvokeTypes invokeType = MethodCallFragment.InvokeTypes.STATIC;
                            String methodName = context.n;
                            String methodOwner = context.methodNode.owner.replace("/", ".");
                            AnnotationFragment[] annotations = toAnnotationFragments(context.methodNode.owner, context.n,
                                    context.methodNode.desc, context.methodFragment, out);
                            String[] argumentsTypes = context.margsArray;
                            String returnType = typesFromDesc(context.desc.substring(context.desc.indexOf(")") + 1))[0];
                            yield new MethodCallFragment(methodName, invokeType, annotations, methodOwner, argumentsTypes, returnType);
                        }
                        case Opcodes.INVOKESPECIAL -> {
                            context.desc = context.methodNode.desc;
                            context.margs = context.desc.substring(context.desc.indexOf('(') + 1, context.desc.indexOf(')'));
                            context.margsArray = typesFromDesc(context.margs);
                            context.n = context.methodNode.name;

                            MethodCallFragment.InvokeTypes invokeType = MethodCallFragment.InvokeTypes.SPECIAL;
                            String methodName = context.n;
                            String methodOwner = context.methodNode.owner.replace("/", ".");
                            AnnotationFragment[] annotations = toAnnotationFragments(context.methodNode.owner, context.n,
                                    context.methodNode.desc, context.methodFragment, out);
                            String[] argumentsTypes = context.margsArray;
                            String returnType = typesFromDesc(context.desc.substring(context.desc.indexOf(")") + 1))[0];
                            yield new MethodCallFragment(methodName, invokeType, annotations, methodOwner, argumentsTypes, returnType);
                        }
                        case Opcodes.INVOKEVIRTUAL -> {
                            context.desc = context.methodNode.desc;
                            context.margs = context.desc.substring(context.desc.indexOf('(') + 1, context.desc.indexOf(')'));
                            context.margsArray = typesFromDesc(context.margs);
                            context.n = context.methodNode.name;

                            MethodCallFragment.InvokeTypes invokeType = MethodCallFragment.InvokeTypes.VIRTUAL;
                            String methodName = context.n;
                            String methodOwner = context.methodNode.owner.replace("/", ".");
                            AnnotationFragment[] annotations = toAnnotationFragments(context.methodNode.owner, context.n,
                                    context.methodNode.desc, context.methodFragment, out);
                            String[] argumentsTypes = context.margsArray;
                            String returnType = typesFromDesc(context.desc.substring(context.desc.indexOf(")") + 1))[0];
                            yield new MethodCallFragment(methodName, invokeType, annotations, methodOwner, argumentsTypes, returnType);
                        }
                        default -> null;
                    };
                }
                default -> null;
            };
            if (data != null) {
                out.add(data);
            }
        }
    }

    private static AnnotationFragment[] toAnnotationFragments(String methodClass, String methodName, String methodDesc,
                                              MethodCallFragment fragment, List<CodeFragment.Data> out) {
        List<AnnotationFragment> fragments = new ArrayList<>();
        try {
            ClassReader reader = new ClassReader(Objects.requireNonNull(BytecodeDecoder.class.getResourceAsStream("/" + methodClass.replace(".", "/") + ".class")));
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            List<MethodNode> methodList = classNode.methods;
            for (MethodNode methodNode : methodList) {
                if (methodNode.name.equals(methodName) && methodNode.desc.equals(methodDesc)) {
                    List<AnnotationNode> annots = methodNode.visibleAnnotations;
                    if (annots == null) {
                        return fragments.toArray(AnnotationFragment[]::new);
                    }
                    for (AnnotationNode annot : annots) {
                        fragments.add(createFromNode(annot));
                        System.out.println(annot.desc);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fragments.toArray(AnnotationFragment[]::new);
    }

    public BytecodeDecoder addInstructionsFromInterfaces(boolean add) {
        this.instructionsFromInterfaces = add;
        return this;
    }

    public void handleClass(Object data, List<CodeFragment.Data> out) {
        try {
            ClassReader reader;
            if (data == null) {
                return;
            }

            Object object = data;
            if (object instanceof byte[] byteData) {
                reader = new ClassReader(byteData);
            } else {
                if (object instanceof InputStream inputStream) {
                    reader = new ClassReader(inputStream);
                } else {
                    if (object instanceof String stringData) {
                        handleClass(Class.forName(stringData), out);
                        return;
                    }

                    if (object instanceof Class) {
                        Class<?> clazz = (Class) object;
                        String className = "/" + clazz.getName().replace('.', '/') + ".class";
                        System.out.println(className);
                        reader = new ClassReader(clazz.getResourceAsStream(className));
                    } else {
                        throw new JLSLException("Invalid type: " + data.getClass().getCanonicalName());
                    }
                }
            }
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);

            reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);



            String className = classNode.name.replace("/", ".").replace("$", ".");
            String superclass = classNode.superName.replace("/", ".").replace("$", ".");
            AccessPolicy access = new AccessPolicy(classNode.access);
            String sourceFile = classNode.sourceFile == null ? "Unknown" : classNode.sourceFile;
            int classVersion = classNode.version;

            // Interfaces
            String[] interfaces = classNode.interfaces.toArray(String[]::new);

            // Annotations
            AnnotationFragment[] annotations = JavaUtils.toAnnotationFragments(classNode.visibleAnnotations);

            // Fields
            FieldFragment[] fields = JavaUtils.toFieldFragments(classNode.fields);



            NewClassFragment classFragment = new NewClassFragment(access, className, superclass, fields, interfaces,
                    annotations, sourceFile, classVersion);

            if (this.instructionsFromInterfaces) {
                for (String interfaceInst : interfaces) {
                    List<CodeFragment.Data> fragments = new ArrayList<>();
                    handleClass(interfaceInst.replace("/", "."), fragments);

                    out.addAll(fragments);
                }
            }

            out.add(classFragment);

            List<MethodNode> methodNodes = classNode.methods;

            methodNodes.sort((arg0, arg1) -> arg0.name.equals("main") ? 1 : (arg1.name.equals("main") ? -1 : 0));


            for (MethodNode node : methodNodes) {
                if ((node.access & 0x400) != 0) {
                    continue;
                }
                StartOfMethodFragment startOfMethodFragment = toStartOfMethodFragment(classNode, node);
                out.add(startOfMethodFragment);
                handleMethodNode(node, startOfMethodFragment.varTypeMap(), startOfMethodFragment.varNameMap(), out);
                out.add(new EndOfMethodFragment(startOfMethodFragment));
            }
        } catch (Exception e) {
            throw new JLSLException(e);
        }
    }
}
