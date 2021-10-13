package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.*;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment.InvokeTypes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeDecoder extends CodeDecoder {

    public static final boolean DEBUG = true;
    private boolean instructionsFromInterfaces;

    public BytecodeDecoder() {

    }

    @SuppressWarnings("unchecked")
    private static AnnotationFragment createFromNode(final AnnotationNode annotNode) {
        final AnnotationFragment annotFragment = new AnnotationFragment();
        annotFragment.name = typesFromDesc(annotNode.desc)[0].replace("/", ".").replace("$", ".");
        final List<Object> values = annotNode.values;
        if (values != null) {
            for (int index = 0; index < values.size(); index += 2) {
                final String key = (String) values.get(index);
                final Object value = values.get(index + 1);
                annotFragment.values.put(key, value);
            }
        }
        return annotFragment;
    }

    private static void handleMethodNode(final MethodNode node, final HashMap<Integer, String> varTypeMap, final HashMap<Integer, String> varNameMap, final List<CodeFragment> out) {
        int lastFrameType = 0;
        int frames = 0;
        int framesToSkip = 0;
        final Stack<LabelNode> toJump = new Stack<>();
        final Stack<Label> gotos = new Stack<>();
        final Stack<Label> ifs = new Stack<>();
        final InsnList instructions = node.instructions;
        Label currentLabel = null;
        for (int index = 0; index < instructions.size(); index++) {
            final AbstractInsnNode ainsnNode = instructions.get(index);
            if (ainsnNode.getType() == AbstractInsnNode.INSN) {
                final InsnNode insnNode = (InsnNode) ainsnNode;
                if (insnNode.getOpcode() == ICONST_0) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 0;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == ICONST_1) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 1;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == ICONST_2) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 2;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == ICONST_3) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 3;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == ICONST_4) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 4;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == ICONST_5) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 5;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == DCONST_0) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 0.0;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == DCONST_1) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 1.0;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == FCONST_0) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 0.f;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == FCONST_1) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 1.f;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == FCONST_2) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = 2.f;
                    out.add(loadConstantFragment);
                } else if (insnNode.getOpcode() == ACONST_NULL) {
                    final LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
                    loadConstantFragment.value = null;
                    out.add(loadConstantFragment);
                } else if (ainsnNode.getOpcode() == LRETURN || ainsnNode.getOpcode() == DRETURN || ainsnNode.getOpcode() == FRETURN || ainsnNode.getOpcode() == IRETURN || ainsnNode.getOpcode() == ARETURN) {
                    final ReturnValueFragment returnFrag = new ReturnValueFragment();
                    out.add(returnFrag);
                } else if (ainsnNode.getOpcode() == LADD || ainsnNode.getOpcode() == DADD || ainsnNode.getOpcode() == FADD || ainsnNode.getOpcode() == IADD) {
                    out.add(new AddFragment());
                } else if (ainsnNode.getOpcode() == LSUB || ainsnNode.getOpcode() == DSUB || ainsnNode.getOpcode() == FSUB || ainsnNode.getOpcode() == ISUB) {
                    out.add(new SubFragment());
                } else if (ainsnNode.getOpcode() == LMUL || ainsnNode.getOpcode() == DMUL || ainsnNode.getOpcode() == FMUL || ainsnNode.getOpcode() == IMUL) {
                    out.add(new MulFragment());
                } else if (ainsnNode.getOpcode() == LDIV || ainsnNode.getOpcode() == DDIV || ainsnNode.getOpcode() == FDIV || ainsnNode.getOpcode() == IDIV) {
                    out.add(new DivFragment());
                } else if (ainsnNode.getOpcode() == DREM || ainsnNode.getOpcode() == IREM || ainsnNode.getOpcode() == FREM || ainsnNode.getOpcode() == LREM) {
                    out.add(new ModFragment());
                } else if (ainsnNode.getOpcode() == D2I) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "double";
                    cast.to = "int";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == I2D) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "int";
                    cast.to = "double";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == I2F) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "int";
                    cast.to = "float";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == I2L) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "int";
                    cast.to = "long";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == I2C) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "int";
                    cast.to = "char";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == F2I) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "float";
                    cast.to = "int";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == F2D) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "float";
                    cast.to = "double";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == F2L) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "float";
                    cast.to = "long";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == L2D) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "long";
                    cast.to = "double";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == L2I) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "long";
                    cast.to = "int";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == L2F) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "long";
                    cast.to = "float";
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == ISHR) {
                    final RightShiftFragment shrFragment = new RightShiftFragment();
                    shrFragment.signed = true;
                    shrFragment.type = "int";
                    out.add(shrFragment);
                } else if (ainsnNode.getOpcode() == IUSHR) {
                    final RightShiftFragment shrFragment = new RightShiftFragment();
                    shrFragment.signed = false;
                    shrFragment.type = "int";
                    out.add(shrFragment);
                } else if (ainsnNode.getOpcode() == LSHR) {
                    final RightShiftFragment shrFragment = new RightShiftFragment();
                    shrFragment.signed = true;
                    shrFragment.type = "long";
                    out.add(shrFragment);
                } else if (ainsnNode.getOpcode() == LUSHR) {
                    final RightShiftFragment shrFragment = new RightShiftFragment();
                    shrFragment.signed = false;
                    shrFragment.type = "long";
                    out.add(shrFragment);
                } else if (ainsnNode.getOpcode() == ISHL) {
                    final LeftShiftFragment shlFragment = new LeftShiftFragment();
                    shlFragment.signed = true;
                    shlFragment.type = "int";
                    out.add(shlFragment);
                } else if (ainsnNode.getOpcode() == LSHL) {
                    final LeftShiftFragment shlFragment = new LeftShiftFragment();
                    shlFragment.type = "long";
                    shlFragment.signed = true;
                    out.add(shlFragment);
                } else if (ainsnNode.getOpcode() == IAND) {
                    final AndFragment andFragment = new AndFragment();
                    andFragment.type = "int";
                    out.add(andFragment);
                } else if (ainsnNode.getOpcode() == LAND) {
                    final AndFragment andFragment = new AndFragment();
                    andFragment.type = "long";
                    out.add(andFragment);
                } else if (ainsnNode.getOpcode() == IOR) {
                    final OrFragment orFragment = new OrFragment();
                    orFragment.type = "int";
                    out.add(orFragment);
                } else if (ainsnNode.getOpcode() == LOR) {
                    final OrFragment orFragment = new OrFragment();
                    orFragment.type = "long";
                    out.add(orFragment);
                } else if (ainsnNode.getOpcode() == IXOR) {
                    final XorFragment xorFragment = new XorFragment();
                    xorFragment.type = "int";
                    out.add(xorFragment);
                } else if (ainsnNode.getOpcode() == LXOR) {
                    final XorFragment xorFragment = new XorFragment();
                    xorFragment.type = "long";
                    out.add(xorFragment);
                } else if (ainsnNode.getOpcode() == POP) {
                    final PopFragment popFrag = new PopFragment();
                    out.add(popFrag);
                } else if (ainsnNode.getOpcode() == RETURN) {
                    final ReturnFragment returnFrag = new ReturnFragment();
                    out.add(returnFrag);
                } else if (ainsnNode.getOpcode() == DUP) {
                    final DuplicateFragment duplicate = new DuplicateFragment();
                    duplicate.wait = 1;
                    out.add(duplicate);
                } else if (ainsnNode.getOpcode() == DUP2_X1) {
                    final DuplicateFragment duplicate = new DuplicateFragment();
                    duplicate.wait = 1;
                    out.add(duplicate);
                } else if (ainsnNode.getOpcode() == DCMPG || ainsnNode.getOpcode() == FCMPG) {
                    final CompareFragment compareFrag = new CompareFragment();
                    compareFrag.inferior = true;
                    out.add(compareFrag);
                } else if (ainsnNode.getOpcode() == DCMPL || ainsnNode.getOpcode() == FCMPL) {
                    if (instructions.get(index + 1).getOpcode() == IFEQ) {
                        final NotEqualCheckFragment notEqualFrag = new NotEqualCheckFragment();
                        out.add(notEqualFrag);
                    } else if (instructions.get(index + 1).getOpcode() == IFNE) {
                        final NotEqualCheckFragment notEqualFrag = new NotEqualCheckFragment();
                        out.add(notEqualFrag);
                    } else {
                        final CompareFragment compareFrag = new CompareFragment();
                        compareFrag.inferior = false;
                        out.add(compareFrag);
                    }
                } else if (ainsnNode.getOpcode() == AASTORE || ainsnNode.getOpcode() == IASTORE || ainsnNode.getOpcode() == BASTORE || ainsnNode.getOpcode() == LASTORE || ainsnNode.getOpcode() == SASTORE || ainsnNode.getOpcode() == FASTORE || ainsnNode.getOpcode() == DASTORE || ainsnNode.getOpcode() == CASTORE) {
                    final ArrayStoreFragment storeFrag = new ArrayStoreFragment();
                    out.add(storeFrag);
                } else if (ainsnNode.getOpcode() == AALOAD) {
                    final ArrayOfArrayLoadFragment loadFrag = new ArrayOfArrayLoadFragment();
                    out.add(loadFrag);
                }
            } else if (ainsnNode.getType() == AbstractInsnNode.LABEL) {
                final LabelNode labelNode = (LabelNode) ainsnNode;
                currentLabel = labelNode.getLabel();
                while (!toJump.isEmpty()) {
                    if (labelNode.getLabel().equals(toJump.peek().getLabel())) {
                        while (!toJump.isEmpty() && toJump.pop().getLabel().equals(labelNode.getLabel())) {
                            if (!gotos.isEmpty()) {
                                while (gotos.contains(currentLabel)) {
                                    if (frames > 0) {
                                        final EndOfBlockFragment endOfBlockFrag = new EndOfBlockFragment();
                                        out.add(endOfBlockFrag);
                                        frames--;
                                    }
                                    gotos.remove(currentLabel);
                                }
                            }
                        }
                        break;
                    } else {
                        break;
                    }
                }
            } else if (ainsnNode.getType() == AbstractInsnNode.FRAME) {
                final FrameNode frameNode = (FrameNode) ainsnNode;
                if (framesToSkip > 0) {
                    framesToSkip--;
                } else {
                    if (frames == 0) {
                    } else {
                        final boolean a = (!ifs.isEmpty() && ifs.contains(currentLabel));
                        final boolean b = (gotos.isEmpty() || !gotos.contains(currentLabel));
                        int nbr = 0;
                        if (a || b) {
                            while (ifs.contains(currentLabel)) {
                                nbr++;
                                ifs.remove(currentLabel);
                            }
                        }

                        for (int j = 0; j < nbr; j++) {
                            final EndOfBlockFragment end = new EndOfBlockFragment();
                            out.add(end);
                            frames--;
                        }
                    }
                }
                lastFrameType = frameNode.type;
            } else if (ainsnNode.getType() == AbstractInsnNode.JUMP_INSN) {
                final JumpInsnNode jumpNode = (JumpInsnNode) ainsnNode;
                if (jumpNode.getOpcode() == IFEQ) {
                    if (instructions.get(index - 1).getOpcode() == ILOAD && instructions.get(index + 1).getOpcode() == ILOAD && instructions.get(index + 2).getOpcode() == IFEQ && instructions.get(index + 3).getOpcode() == ICONST_1 && instructions.get(index + 4).getOpcode() == GOTO && instructions.get(index + 5).getType() == AbstractInsnNode.LABEL
                            && instructions.get(index + 6).getType() == AbstractInsnNode.FRAME && instructions.get(index + 7).getOpcode() == ICONST_0 && instructions.get(index + 8).getType() == AbstractInsnNode.LABEL && instructions.get(index + 9).getType() == AbstractInsnNode.FRAME && instructions.get(index + 10).getOpcode() == ISTORE) {
                        final int operand = ((VarInsnNode) instructions.get(index + 1)).var;
                        final LoadVariableFragment loadFrag = new LoadVariableFragment();
                        loadFrag.variableName = varNameMap.get(operand);
                        loadFrag.variableIndex = operand;
                        out.add(loadFrag);

                        final AndFragment andFrag = new AndFragment();
                        andFrag.isDouble = true;
                        out.add(andFrag);

                        final int operand1 = ((VarInsnNode) instructions.get(index + 10)).var;
                        final StoreVariableFragment storeFrag = new StoreVariableFragment();
                        storeFrag.variableName = varNameMap.get(operand1);
                        storeFrag.variableIndex = operand1;
                        storeFrag.variableType = "int";
                        out.add(storeFrag);
                        index += 10;
                    } else {
                        final IfStatementFragment ifFrag = new IfStatementFragment();
                        frames++;
                        ifs.push(jumpNode.label.getLabel());
                        ifFrag.toJump = jumpNode.label.getLabel().toString();
                        out.add(ifFrag);
                        toJump.push(jumpNode.label);
                    }
                } else if (jumpNode.getOpcode() == IF_ICMPEQ && instructions.get(index + 1).getOpcode() == ICONST_1 && (instructions.get(index + 2).getOpcode() == IRETURN || instructions.get(index + 2).getOpcode() == ISTORE) && instructions.get(index + 3).getType() == AbstractInsnNode.LABEL
                        && instructions.get(index + 4).getType() == AbstractInsnNode.FRAME && instructions.get(index + 5).getOpcode() == ICONST_0 && (instructions.get(index + 6).getOpcode() == IRETURN || instructions.get(index + 2).getOpcode() == ISTORE)) {
                    final NotEqualCheckFragment notEqualFrag = new NotEqualCheckFragment();
                    out.add(notEqualFrag);
                    index += 5;
                } else if (jumpNode.getOpcode() == IF_ICMPNE && instructions.get(index + 1).getOpcode() == ICONST_1 && (instructions.get(index + 2).getOpcode() == IRETURN || instructions.get(index + 2).getOpcode() == ISTORE) && instructions.get(index + 3).getType() == AbstractInsnNode.LABEL
                        && instructions.get(index + 4).getType() == AbstractInsnNode.FRAME && instructions.get(index + 5).getOpcode() == ICONST_0 && (instructions.get(index + 6).getOpcode() == IRETURN || instructions.get(index + 2).getOpcode() == ISTORE)

                ) {
                    final EqualCheckFragment equalFrag = new EqualCheckFragment();
                    out.add(equalFrag);
                    index += 5;
                } else if (jumpNode.getOpcode() == IFNE) {
                    if (instructions.get(index - 1).getOpcode() == ILOAD && instructions.get(index + 1).getOpcode() == ILOAD && instructions.get(index + 2).getOpcode() == IFNE && instructions.get(index + 3).getOpcode() == ICONST_0 && instructions.get(index + 4).getOpcode() == GOTO && instructions.get(index + 5).getType() == AbstractInsnNode.LABEL
                            && instructions.get(index + 6).getType() == AbstractInsnNode.FRAME && instructions.get(index + 7).getOpcode() == ICONST_1 && instructions.get(index + 8).getType() == AbstractInsnNode.LABEL && instructions.get(index + 9).getType() == AbstractInsnNode.FRAME && instructions.get(index + 10).getOpcode() == ISTORE) {
                        final int operand = ((VarInsnNode) instructions.get(index + 1)).var;
                        final LoadVariableFragment loadFrag = new LoadVariableFragment();
                        loadFrag.variableName = varNameMap.get(operand);
                        loadFrag.variableIndex = operand;
                        out.add(loadFrag);

                        final OrFragment orFrag = new OrFragment();
                        orFrag.isDouble = true;
                        out.add(orFrag);

                        final int operand1 = ((VarInsnNode) instructions.get(index + 10)).var;
                        final StoreVariableFragment storeFrag = new StoreVariableFragment();
                        storeFrag.variableName = varNameMap.get(operand1);
                        storeFrag.variableIndex = operand1;
                        storeFrag.variableType = "int";
                        out.add(storeFrag);
                        index += 10;
                    } else {
                        final IfNotStatementFragment ifFrag = new IfNotStatementFragment();
                        frames++;
                        ifs.push(jumpNode.label.getLabel());
                        ifFrag.toJump = jumpNode.label.getLabel().toString();
                        out.add(ifFrag);
                        toJump.push(jumpNode.label);
                    }
                } else if (jumpNode.getOpcode() == IF_ICMPEQ) {
                    out.add(new NotEqualCheckFragment());
                    final IfStatementFragment ifFrag = new IfStatementFragment();
                    frames++;
                    ifs.push(jumpNode.label.getLabel());
                    ifFrag.toJump = jumpNode.label.getLabel().toString();
                    out.add(ifFrag);
                    toJump.push(jumpNode.label);
                } else if (jumpNode.getOpcode() == IF_ICMPNE) {
                    out.add(new EqualCheckFragment());
                    final IfStatementFragment ifFrag = new IfStatementFragment();
                    frames++;
                    ifs.push(jumpNode.label.getLabel());
                    ifFrag.toJump = jumpNode.label.getLabel().toString();
                    out.add(ifFrag);
                    toJump.push(jumpNode.label);
                } else if (jumpNode.getOpcode() == IFGE) {
                    final IfStatementFragment ifFrag = new IfStatementFragment();
                    frames++;
                    ifs.push(jumpNode.label.getLabel());
                    ifFrag.toJump = jumpNode.label.getLabel().toString();
                    out.add(ifFrag);
                    toJump.push(jumpNode.label);
                } else if (jumpNode.getOpcode() == IFLE) {
                    final IfStatementFragment ifFrag = new IfStatementFragment();
                    frames++;
                    ifs.push(jumpNode.label.getLabel());
                    ifFrag.toJump = jumpNode.label.getLabel().toString();
                    out.add(ifFrag);
                    toJump.push(jumpNode.label);
                } else if (jumpNode.getOpcode() == GOTO) {
                    toJump.push(jumpNode.label);
                    gotos.push(jumpNode.label.getLabel());
                    if (instructions.get(index - 1) instanceof LineNumberNode && lastFrameType == F_SAME) {
                        final EndOfBlockFragment end = new EndOfBlockFragment();
                        frames--;
                        out.add(end);
                    }

                    final EndOfBlockFragment end = new EndOfBlockFragment();
                    frames--;
                    out.add(end);

                    final ElseStatementFragment elseFrag = new ElseStatementFragment();
                    frames++;
                    out.add(elseFrag);
                    framesToSkip = 1;
                }
            } else if (ainsnNode.getType() == AbstractInsnNode.LDC_INSN) {
                final LdcInsnNode ldc = (LdcInsnNode) ainsnNode;
                final LdcFragment ldcFragment = new LdcFragment();
                ldcFragment.value = ldc.cst;
                out.add(ldcFragment);
            } else if (ainsnNode.getType() == AbstractInsnNode.VAR_INSN) {
                final VarInsnNode varNode = (VarInsnNode) ainsnNode;
                final int operand = varNode.var;
                if (ainsnNode.getOpcode() == ISTORE) {
                    final StoreVariableFragment storeFrag = new StoreVariableFragment();
                    storeFrag.variableName = varNameMap.get(operand);
                    storeFrag.variableIndex = operand;
                    storeFrag.variableType = "int";
                    out.add(storeFrag);
                } else if (ainsnNode.getOpcode() == DSTORE) {
                    final StoreVariableFragment storeFrag = new StoreVariableFragment();
                    storeFrag.variableName = varNameMap.get(operand);
                    storeFrag.variableIndex = operand;
                    storeFrag.variableType = "double";
                    out.add(storeFrag);
                } else if (ainsnNode.getOpcode() == LSTORE) {
                    final StoreVariableFragment storeFrag = new StoreVariableFragment();
                    storeFrag.variableName = varNameMap.get(operand);
                    storeFrag.variableIndex = operand;
                    storeFrag.variableType = "long";
                    out.add(storeFrag);
                } else if (ainsnNode.getOpcode() == FSTORE) {
                    final StoreVariableFragment storeFrag = new StoreVariableFragment();
                    storeFrag.variableName = varNameMap.get(operand);
                    storeFrag.variableIndex = operand;
                    storeFrag.variableType = "float";
                    out.add(storeFrag);
                } else if (ainsnNode.getOpcode() == ASTORE) {
                    final StoreVariableFragment storeFrag = new StoreVariableFragment();
                    storeFrag.variableName = varNameMap.get(operand);
                    storeFrag.variableIndex = operand;
                    storeFrag.variableType = varTypeMap.get(operand);
                    out.add(storeFrag);
                } else if (ainsnNode.getOpcode() == FLOAD || ainsnNode.getOpcode() == LLOAD || ainsnNode.getOpcode() == ILOAD || ainsnNode.getOpcode() == DLOAD || ainsnNode.getOpcode() == ALOAD) {
                    final LoadVariableFragment loadFrag = new LoadVariableFragment();
                    loadFrag.variableName = varNameMap.get(operand);
                    loadFrag.variableIndex = operand;
                    out.add(loadFrag);
                }
            } else if (ainsnNode.getType() == AbstractInsnNode.FIELD_INSN) {
                final FieldInsnNode fieldNode = (FieldInsnNode) ainsnNode;
                if (fieldNode.getOpcode() == PUTFIELD) {
                    final PutFieldFragment putFieldFrag = new PutFieldFragment();
                    putFieldFrag.fieldType = typesFromDesc(fieldNode.desc)[0];
                    putFieldFrag.fieldName = fieldNode.name;
                    out.add(putFieldFrag);
                } else if (fieldNode.getOpcode() == GETFIELD) {
                    final GetFieldFragment getFieldFrag = new GetFieldFragment();
                    getFieldFrag.fieldType = typesFromDesc(fieldNode.desc)[0];
                    getFieldFrag.fieldName = fieldNode.name;
                    out.add(getFieldFrag);
                }
            } else if (ainsnNode.getType() == AbstractInsnNode.INT_INSN) {
                final IntInsnNode intNode = (IntInsnNode) ainsnNode;
                final int operand = intNode.operand;
                if (intNode.getOpcode() == BIPUSH) {
                    final IntPushFragment pushFrag = new IntPushFragment();
                    pushFrag.value = operand;
                    out.add(pushFrag);
                } else if (intNode.getOpcode() == SIPUSH) {
                    final IntPushFragment pushFrag = new IntPushFragment();
                    pushFrag.value = operand;
                    out.add(pushFrag);
                } else if (intNode.getOpcode() == NEWARRAY) {
                    final NewPrimitiveArrayFragment arrayFrag = new NewPrimitiveArrayFragment();
                    arrayFrag.type = Printer.TYPES[operand];
                    out.add(arrayFrag);
                }
            } else if (ainsnNode.getType() == AbstractInsnNode.TYPE_INSN) {
                final TypeInsnNode typeNode = (TypeInsnNode) ainsnNode;
                final String operand = typeNode.desc;
                if (typeNode.getOpcode() == ANEWARRAY) {
                    final NewArrayFragment newArray = new NewArrayFragment();
                    newArray.type = operand.replace("/", ".");
                    out.add(newArray);
                } else if (ainsnNode.getOpcode() == CHECKCAST) {
                    final CastFragment cast = new CastFragment();
                    cast.from = "java.lang.Object";
                    cast.to = operand.replace("/", ".");
                    out.add(cast);
                } else if (ainsnNode.getOpcode() == NEW) {
                    final NewInstanceFragment newFrag = new NewInstanceFragment();
                    newFrag.type = operand.replace("/", ".");
                    out.add(newFrag);
                }
            } else if (ainsnNode.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN) {
                final MultiANewArrayInsnNode multiArrayNode = (MultiANewArrayInsnNode) ainsnNode;
                final NewMultiArrayFragment multiFrag = new NewMultiArrayFragment();
                multiFrag.type = typesFromDesc(multiArrayNode.desc)[0].replace("[]", "");
                multiFrag.dimensions = multiArrayNode.dims;
                out.add(multiFrag);
            } else if (ainsnNode.getType() == AbstractInsnNode.LINE) {
                final LineNumberNode lineNode = (LineNumberNode) ainsnNode;
                final LineNumberFragment lineNumberFragment = new LineNumberFragment();
                lineNumberFragment.line = lineNode.line;
                out.add(lineNumberFragment);
            } else if (ainsnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                final MethodInsnNode methodNode = (MethodInsnNode) ainsnNode;
                if (methodNode.getOpcode() == INVOKESTATIC) {
                    final String desc = methodNode.desc;
                    final String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
                    final String[] margsArray = typesFromDesc(margs);
                    final String n = methodNode.name;
                    final MethodCallFragment methodFragment = new MethodCallFragment();
                    methodFragment.invokeType = InvokeTypes.STATIC;
                    methodFragment.methodName = n;
                    methodFragment.methodOwner = methodNode.owner.replace("/", ".");
                    methodFragment.argumentsTypes = margsArray;
                    methodFragment.returnType = typesFromDesc(desc.substring(desc.indexOf(")") + 1))[0];
                    out.add(methodFragment);
                    addAnnotFragments(methodNode.owner, n, methodNode.desc, methodFragment);
                } else if (methodNode.getOpcode() == INVOKESPECIAL) {
                    final String desc = methodNode.desc;
                    final String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
                    final String[] margsArray = typesFromDesc(margs);
                    final String n = methodNode.name;
                    final MethodCallFragment methodFragment = new MethodCallFragment();
                    methodFragment.invokeType = InvokeTypes.SPECIAL;
                    methodFragment.methodName = n;
                    methodFragment.methodOwner = methodNode.owner.replace("/", ".");
                    methodFragment.argumentsTypes = margsArray;
                    methodFragment.returnType = typesFromDesc(desc.substring(desc.indexOf(")") + 1))[0];
                    out.add(methodFragment);
                    addAnnotFragments(methodNode.owner, n, methodNode.desc, methodFragment);
                } else if (methodNode.getOpcode() == INVOKEVIRTUAL) {
                    final String desc = methodNode.desc;
                    final String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
                    final String[] margsArray = typesFromDesc(margs);
                    final String n = methodNode.name;
                    final MethodCallFragment methodFragment = new MethodCallFragment();
                    methodFragment.invokeType = InvokeTypes.VIRTUAL;
                    methodFragment.methodName = n;
                    methodFragment.methodOwner = methodNode.owner.replace("/", ".");
                    methodFragment.argumentsTypes = margsArray;
                    methodFragment.returnType = typesFromDesc(desc.substring(desc.indexOf(")") + 1))[0];
                    out.add(methodFragment);
                    addAnnotFragments(methodNode.owner, n, methodNode.desc, methodFragment);
                }
            }
        }
    }

    private static String[] typesFromDesc(final String desc, final int startPos) {
        boolean parsingObjectClass = false;
        boolean parsingArrayClass = false;
        final ArrayList<String> types = new ArrayList<>();
        String currentObjectClass = null;
        StringBuilder currentArrayClass = null;
        int dims = 1;
        for (int i = startPos; i < desc.length(); i++) {
            char c = desc.charAt(i);

            if (!parsingObjectClass && !parsingArrayClass) {
                if (c == '[') {
                    parsingArrayClass = true;
                    currentArrayClass = new StringBuilder();
                } else if (c == 'L') {
                    parsingObjectClass = true;
                    currentObjectClass = "";
                } else if (c == 'I') {
                    types.add("int");
                } else if (c == 'D') {
                    types.add("double");
                } else if (c == 'B') {
                    types.add("byte");
                } else if (c == 'Z') {
                    types.add("boolean");
                } else if (c == 'V') {
                    types.add("void");
                } else if (c == 'J') {
                    types.add("long");
                } else if (c == 'C') {
                    types.add("char");
                } else if (c == 'F') {
                    types.add("float");
                } else if (c == 'S') {
                    types.add("short");
                }
            } else if (parsingObjectClass) {
                if (c == '/') {
                    c = '.';
                } else if (c == ';') {
                    parsingObjectClass = false;
                    types.add(currentObjectClass);
                    continue;
                }
                currentObjectClass += c;
            } else {
                if (c == '[') {
                    dims++;
                    continue;
                }
                if (c == '/') {
                    c = '.';
                }
                if (c == 'L') {
                    continue;
                } else if (c == ';') {
                    parsingArrayClass = false;
                    final StringBuilder dim = new StringBuilder();
                    for (int ii = 0; ii < dims; ii++) {
                        dim.append("[]");
                    }
                    types.add(currentArrayClass + dim.toString());
                    dims = 1;
                    continue;
                }
                currentArrayClass.append(c);
            }
        }
        if (parsingObjectClass) {
            types.add(currentObjectClass);
        }
        if (parsingArrayClass) {
            final StringBuilder dim = new StringBuilder();
            for (int ii = 0; ii < dims; ii++) {
                dim.append("[]");
            }
            types.add(currentArrayClass.toString() + dim);
        }
        return types.toArray(new String[0]);
    }

    private static String[] typesFromDesc(final String desc) {
        return typesFromDesc(desc, 0);
    }

    @SuppressWarnings("unchecked")
    private static void addAnnotFragments(final String methodClass, final String methodName, final String methodDesc, final CodeFragment fragment) {
        try {
            final ClassReader reader = new ClassReader(Objects.requireNonNull(BytecodeDecoder.class.getResourceAsStream("/" + methodClass.replace(".", "/") + ".class")));
            final ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            final List<MethodNode> methodList = classNode.methods;
            for (final MethodNode methodNode : methodList) {
                if (methodNode.name.equals(methodName) && methodNode.desc.equals(methodDesc)) {
                    final List<AnnotationNode> annots = methodNode.visibleAnnotations;
                    if (annots != null) {
                        for (final AnnotationNode annot : annots) {
                            fragment.addChild(createFromNode(annot));
                            System.out.println(annot.desc);
                        }
                    } else {
                        return;
                    }
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public BytecodeDecoder addInstructionsFromInterfaces(final boolean add) {
        this.instructionsFromInterfaces = add;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleClass(final Object data, final List<CodeFragment> out) {
        try {
            if (data == null) {
                return;
            }
            final ClassReader reader;
            if (data instanceof byte[]) {
                reader = new ClassReader((byte[]) data);
            } else if (data instanceof InputStream) {
                reader = new ClassReader((InputStream) data);
            } else if (data instanceof String) {
                handleClass(Class.forName((String) data), out);
                return;
            } else if (data instanceof Class<?> clazz) {
                String className = clazz.getName().replace(clazz.getPackageName() + ".", "") + ".class";
                reader = new ClassReader(clazz.getResourceAsStream(className));
            } else {
                throw new JLSLException("Invalid type: " + data.getClass().getCanonicalName());
            }
            final ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            if (DEBUG) {
                reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
            }

            final NewClassFragment classFragment = new NewClassFragment();
            classFragment.className = classNode.name.replace("/", ".").replace("$", ".");
            classFragment.superclass = classNode.superName.replace("/", ".").replace("$", ".");
            classFragment.access = new AccessPolicy(classNode.access);
            if (classNode.sourceFile != null) {
                classFragment.sourceFile = classNode.sourceFile;
            }
            classFragment.classVersion = classNode.version;

            final List<String> interfaces = classNode.interfaces;
            classFragment.interfaces = interfaces.toArray(new String[0]);

            if (instructionsFromInterfaces) {
                for (final String interfaceInst : interfaces) {
                    final ArrayList<CodeFragment> fragments = new ArrayList<>();
                    handleClass(interfaceInst.replace("/", "."), fragments);

                    out.addAll(fragments);
                }
            }
            out.add(classFragment);
            final List<MethodNode> methodNodes = classNode.methods;
            final List<FieldNode> fieldNodes = classNode.fields;

            final List<AnnotationNode> list = classNode.visibleAnnotations;
            if (list != null) {
                for (final AnnotationNode annotNode : list) {
                    final AnnotationFragment annotFragment = createFromNode(annotNode);
                    classFragment.addChild(annotFragment);
                }
            }
            for (final FieldNode field : fieldNodes) {
                final String name = field.name;
                final String type = typesFromDesc(field.desc)[0];
                final FieldFragment fieldFragment = new FieldFragment();
                fieldFragment.name = name;
                fieldFragment.type = type;
                fieldFragment.initialValue = field.value;
                fieldFragment.access = new AccessPolicy(field.access);
                final List<AnnotationNode> annotations = field.visibleAnnotations;
                if (annotations != null) {
                    for (final AnnotationNode annotNode : annotations) {
                        final AnnotationFragment annotFragment = createFromNode(annotNode);
                        fieldFragment.addChild(annotFragment);
                    }
                }
                out.add(fieldFragment);
            }

            methodNodes.sort((arg0, arg1) -> {
                if (arg0.name.equals("main")) {
                    return 1;
                }
                if (arg1.name.equals("main")) {
                    return -1;
                }
                return 0;
            }); // TODO: Better the method sorting

            for (final MethodNode node : methodNodes) {
                final List<LocalVariableNode> localVariables = node.localVariables;
                final StartOfMethodFragment startOfMethodFragment = new StartOfMethodFragment();
                startOfMethodFragment.access = new AccessPolicy(node.access);
                startOfMethodFragment.name = node.name;
                startOfMethodFragment.owner = classNode.name.replace("/", ".");
                startOfMethodFragment.returnType = typesFromDesc(node.desc.substring(node.desc.indexOf(")") + 1))[0];
                final ArrayList<String> localNames = new ArrayList<>();
                for (final LocalVariableNode var : localVariables) {
                    startOfMethodFragment.varNameMap.put(var.index, var.name);
                    startOfMethodFragment.varTypeMap.put(var.index, typesFromDesc(var.desc)[0]);
                    startOfMethodFragment.varName2TypeMap.put(var.name, typesFromDesc(var.desc)[0]);
                    if (var.index == 0 && !startOfMethodFragment.access.isStatic()) {
                    } else {
                        localNames.add(var.name);
                    }
                }
                final String[] argsTypes = typesFromDesc(node.desc.substring(node.desc.indexOf('(') + 1, node.desc.indexOf(')')));
                int argIndex = 0;
                for (final String argType : argsTypes) {
                    startOfMethodFragment.argumentsTypes.add(argType);
                    final String name = localNames.isEmpty() ? "var" + argIndex : localNames.get(argIndex);
                    startOfMethodFragment.argumentsNames.add(name);
                    argIndex++;
                }
                final List<AnnotationNode> annots = node.visibleAnnotations;
                if (node.visibleAnnotations != null) {
                    for (final AnnotationNode annotNode : annots) {
                        startOfMethodFragment.addChild(createFromNode(annotNode));
                    }
                }
                out.add(startOfMethodFragment);
                addAnnotFragments(startOfMethodFragment.owner, node.name, node.desc, startOfMethodFragment);
                handleMethodNode(node, startOfMethodFragment.varTypeMap, startOfMethodFragment.varNameMap, out);
                final EndOfMethodFragment endOfMethodFragment = new EndOfMethodFragment();
                endOfMethodFragment.access = startOfMethodFragment.access;
                endOfMethodFragment.name = startOfMethodFragment.name;
                endOfMethodFragment.owner = startOfMethodFragment.owner;
                endOfMethodFragment.argumentsNames = startOfMethodFragment.argumentsNames;
                endOfMethodFragment.argumentsTypes = startOfMethodFragment.argumentsTypes;
                endOfMethodFragment.returnType = startOfMethodFragment.returnType;
                endOfMethodFragment.varNameMap = startOfMethodFragment.varNameMap;
                endOfMethodFragment.varTypeMap = startOfMethodFragment.varTypeMap;
                endOfMethodFragment.varName2TypeMap = startOfMethodFragment.varName2TypeMap;
                endOfMethodFragment.getChildren().addAll(startOfMethodFragment.getChildren());
                addAnnotFragments(endOfMethodFragment.owner, node.name, node.desc, endOfMethodFragment);
                out.add(endOfMethodFragment);
            }
        } catch (final Exception e) {
            throw new JLSLException(e);
        }
    }

}
