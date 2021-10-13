package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.CodeEncoder;
import org.jglrxavpok.jlsl.JLSLException;
import org.jglrxavpok.jlsl.fragments.*;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment.InvokeTypes;
import org.jglrxavpok.jlsl.glsl.GLSL.*;
import org.jglrxavpok.jlsl.glsl.fragments.StructFragment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class GLSLEncoder extends CodeEncoder {
    public static final boolean DEBUG = true;
    private static final int STRUCT = 1;
    private final int glslversion;
    private final ArrayList<String> extensions = new ArrayList<>();
    private final String space = " ";
    private final String tab = "    ";
    private final Stack<String> typesStack;
    private final HashMap<String, String> name2type;
    private final HashMap<Object, String> constants;
    private final HashMap<String, String> methodReplacements;
    private final ArrayList<String> initialized;
    private final ArrayList<String> loadedStructs = new ArrayList<>();
    private final Stack<CodeFragment> waiting;
    private final Stack<String> newInstances;
    private final String structOwnerMethodSeparator;
    private final HashMap<String, String> translations = new HashMap<>();
    private final HashMap<String, String> conversionsToStructs = new HashMap<>();
    private int indentation;
    private NewClassFragment currentClass;
    private int currentLine;
    private Stack<String> stack;
    private StartOfMethodFragment currentMethod;
    private boolean convertNumbersToChars;
    private int currentRequestType;
    private Object requestData;
    private boolean allowedToPrint;
    private PrintWriter output;

    public GLSLEncoder(final int glslversion) {
        convertNumbersToChars = true;
        this.glslversion = glslversion;
        stack = new Stack<>();
        typesStack = new Stack<>();
        initialized = new ArrayList<>();
        name2type = new HashMap<>();
        constants = new HashMap<>();
        methodReplacements = new HashMap<>();
        waiting = new Stack<>();
        newInstances = new Stack<>();
        structOwnerMethodSeparator = "__";

        init();
    }

    public void init() {
        setGLSLTranslation("boolean", "bool");
        setGLSLTranslation("double", "float"); // not every GPU has double
        // precision;
        setGLSLTranslation(vec2.class.getCanonicalName(), "vec2");
        setGLSLTranslation(vec3.class.getCanonicalName(), "vec3");
        setGLSLTranslation(vec4.class.getCanonicalName(), "vec4");
        setGLSLTranslation(mat2.class.getCanonicalName(), "mat2");
        setGLSLTranslation(mat3.class.getCanonicalName(), "mat3");
        setGLSLTranslation(mat4.class.getCanonicalName(), "mat4");
        setGLSLTranslation(Integer.class.getCanonicalName(), "int");

        setGLSLTranslation(Math.class.getCanonicalName(), "");

        setGLSLTranslation(sampler2D.class.getCanonicalName(), "sampler2D");

    }

    public void addToStructConversion(final String javaType, final String structName) {
        conversionsToStructs.put(javaType, structName);
    }

    public boolean hasStructAttached(final String javaType) {
        return conversionsToStructs.containsKey(javaType);
    }

    public void setGLSLTranslation(final String javaType, final String glslType) {
        translations.put(javaType, glslType);
    }

    public void removeGLSLTranslation(final String javaType) {
        translations.remove(javaType);
    }

    private String toGLSL(String type) {
		if (type == null) {
			return "";
		}
        String copy = type;
        final StringBuilder end = new StringBuilder();
        while (copy.contains("[]")) {
            copy = copy.replaceFirst("\\[]", "");
            end.append("[]");
        }
        type = copy;
        if (conversionsToStructs.containsKey(type)) {
            return conversionsToStructs.get(type) + end;
        }
        if (translations.containsKey(type)) {
            return translations.get(type) + end;
        }
        return type + end;
    }

    private String getEndOfLine(final int currentLine) {
        String s = "";
        // if(currentLine % 2 == 0)
        {
            s = " //Line #" + currentLine;
        }
        return s;
    }

    public void convertNumbersToChar(final boolean convert) {
        this.convertNumbersToChars = convert;
    }

    @Override
    public void onRequestResult(final ArrayList<CodeFragment> fragments) {
        if (currentRequestType == STRUCT) {
            final StructFragment currentStruct = (StructFragment) requestData;
            final HashMap<String, String> fields = currentStruct.fields;
            for (final CodeFragment fragment : fragments) {
                if (fragment.getClass() == FieldFragment.class) {
                    final FieldFragment fieldFrag = (FieldFragment) fragment;
                    fields.put(fieldFrag.name, fieldFrag.type);
                    fragment.forbiddenToPrint = true;
                }

                currentStruct.addChild(fragment);
            }
        }
    }

    @Override
    public void createSourceCode(final List<CodeFragment> in, final PrintWriter out) {
        interpret(in);
        this.output = out;
        this.allowedToPrint = true;
        println("#version " + glslversion);
        for (int index = 0; index < in.size(); index++) {
            final CodeFragment fragment = in.get(index);
            this.output = out;
            this.allowedToPrint = !fragment.forbiddenToPrint;
            if (!waiting.isEmpty()) {
                handleCodeFragment(waiting.pop(), index, in, out);
            }
            handleCodeFragment(fragment, index, in, out);
        }
        out.flush();
    }

    private void handleCodeFragment(final CodeFragment fragment, final int index, final List<CodeFragment> in, final PrintWriter out) {
        if (fragment.getClass() == NewClassFragment.class) {
            handleClassFragment((NewClassFragment) fragment, in, index, out);
            currentClass = (NewClassFragment) fragment;
        } else if (fragment.getClass() == FieldFragment.class) {
            handleFieldFragment((FieldFragment) fragment, in, index, out);
        } else if (fragment.getClass() == StartOfMethodFragment.class) {
            handleStartOfMethodFragment((StartOfMethodFragment) fragment, in, index, out);
            this.currentMethod = (StartOfMethodFragment) fragment;
        } else if (fragment.getClass() == EndOfMethodFragment.class) {
            handleEndOfMethodFragment((EndOfMethodFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LineNumberFragment.class) {
            currentLine = ((LineNumberFragment) fragment).line;
        } else if (fragment.getClass() == NewArrayFragment.class) {
            handleNewArrayFragment((NewArrayFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NewMultiArrayFragment.class) {
            handleNewMultiArrayFragment((NewMultiArrayFragment) fragment, in, index, out);
        } else if (fragment.getClass() == PutFieldFragment.class) {
            handlePutFieldFragment((PutFieldFragment) fragment, in, index, out);
        } else if (fragment.getClass() == GetFieldFragment.class) {
            handleGetFieldFragment((GetFieldFragment) fragment, in, index, out);
        } else if (fragment.getClass() == IntPushFragment.class) {
            handleBiPushFragment((IntPushFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NewPrimitiveArrayFragment.class) {
            handleNewPrimitiveArrayFragment((NewPrimitiveArrayFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LoadVariableFragment.class) {
            handleLoadVariableFragment((LoadVariableFragment) fragment, in, index, out);
        } else if (fragment.getClass() == StoreVariableFragment.class) {
            handleStoreVariableFragment((StoreVariableFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LdcFragment.class) {
            handleLdcFragment((LdcFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LoadConstantFragment.class) {
            handleLoadConstantFragment((LoadConstantFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ReturnValueFragment.class) {
            handleReturnValueFragment((ReturnValueFragment) fragment, in, index, out);
        } else if (fragment.getClass() == AddFragment.class) {
            handleAddFragment((AddFragment) fragment, in, index, out);
        } else if (fragment.getClass() == SubFragment.class) {
            handleSubFragment((SubFragment) fragment, in, index, out);
        } else if (fragment.getClass() == MulFragment.class) {
            handleMulFragment((MulFragment) fragment, in, index, out);
        } else if (fragment.getClass() == DivFragment.class) {
            handleDivFragment((DivFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ArrayOfArrayLoadFragment.class) {
            handleArrayOfArrayLoadFragment((ArrayOfArrayLoadFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ArrayStoreFragment.class) {
            handleArrayStoreFragment((ArrayStoreFragment) fragment, in, index, out);
        } else if (fragment.getClass() == IfStatementFragment.class) {
            handleIfStatementFragment((IfStatementFragment) fragment, in, index, out);
        } else if (fragment.getClass() == EndOfBlockFragment.class) {
            handleEndOfBlockFragment((EndOfBlockFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ElseStatementFragment.class) {
            handleElseStatementFragment((ElseStatementFragment) fragment, in, index, out);
        } else if (fragment.getClass() == MethodCallFragment.class) {
            handleMethodCallFragment((MethodCallFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ModFragment.class) {
            handleModFragment((ModFragment) fragment, in, index, out);
        } else if (fragment.getClass() == CastFragment.class) {
            handleCastFragment((CastFragment) fragment, in, index, out);
        } else if (fragment.getClass() == LeftShiftFragment.class) {
            handleLeftShiftFragment((LeftShiftFragment) fragment, in, index, out);
        } else if (fragment.getClass() == RightShiftFragment.class) {
            handleRightShiftFragment((RightShiftFragment) fragment, in, index, out);
        } else if (fragment.getClass() == AndFragment.class) {
            handleAndFragment((AndFragment) fragment, in, index, out);
        } else if (fragment.getClass() == OrFragment.class) {
            handleOrFragment((OrFragment) fragment, in, index, out);
        } else if (fragment.getClass() == XorFragment.class) {
            handleXorFragment((XorFragment) fragment, in, index, out);
        } else if (fragment.getClass() == IfNotStatementFragment.class) {
            handleIfNotStatementFragment((IfNotStatementFragment) fragment, in, index, out);
        } else if (fragment.getClass() == PopFragment.class) {
            handlePopFragment((PopFragment) fragment, in, index, out);
        } else if (fragment.getClass() == ReturnFragment.class) {
            handleReturnFragment((ReturnFragment) fragment, in, index, out);
        } else if (fragment.getClass() == DuplicateFragment.class) {
            handleDuplicateFragment((DuplicateFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NewInstanceFragment.class) {
            handleNewInstanceFragment((NewInstanceFragment) fragment, in, index, out);
        } else if (fragment.getClass() == EqualCheckFragment.class) {
            handleEqualCheckFragment((EqualCheckFragment) fragment, in, index, out);
        } else if (fragment.getClass() == NotEqualCheckFragment.class) {
            handleNotEqualCheckFragment((NotEqualCheckFragment) fragment, in, index, out);
        } else if (fragment.getClass() == CompareFragment.class) {
            handleCompareFragment((CompareFragment) fragment, in, index, out);
        } else if (fragment.getClass() == StructFragment.class) {
            handleStructFragment((StructFragment) fragment, in, index, out);
        }
    }

    private void handleCompareFragment(final CompareFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String right = stack.pop();
        final String left = stack.pop();
        stack.push(left + space + (fragment.inferior ? "<" : ">") + space + right);
    }

    private void handleNotEqualCheckFragment(final NotEqualCheckFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String right = stack.pop();
        final String left = stack.pop();
        stack.push("(" + left + space + "!=" + space + right + ")");
    }

    private void handleEqualCheckFragment(final EqualCheckFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String right = stack.pop();
        final String left = stack.pop();
        stack.push("(" + left + space + "==" + space + right + ")");
    }

    private void handleNewInstanceFragment(final NewInstanceFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        newInstances.push(fragment.type);
    }

    private void handleStructFragment(final StructFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        println(getIndent() + "struct " + fragment.name);
        println(getIndent() + "{");
        indentation++;
        for (final String name : fragment.fields.keySet()) {
            final String type = toGLSL(fragment.fields.get(name));
            println(getIndent() + type + space + name + ";");
        }
        indentation--;
        println(getIndent() + "};");

        StartOfMethodFragment currentMethod = null;
        final String instanceName = ("" + fragment.name.charAt(0)).toLowerCase() + fragment.name.substring(1) + "Instance";
        for (int i = 0; i < fragment.getChildren().size(); i++) {
            final CodeFragment fragment1 = fragment.getChildren().get(i);
			if (fragment1 == null) {
				continue;
			}
            this.output = out;
            this.allowedToPrint = !fragment1.forbiddenToPrint;
            if (fragment1 instanceof StartOfMethodFragment method) {
                currentMethod = method;
                final String oldName = currentMethod.name;
                method.varNameMap.put(0, instanceName);
                boolean isConstructor = false;
                if (currentMethod.name.equals("<init>") || currentMethod.name.equals(fragment.name + structOwnerMethodSeparator + "new")) {
                    currentMethod.name = "new";
                    method.returnType = fragment.name;
                    isConstructor = true;
                } else if (!method.argumentsNames.contains(instanceName)) {
                    method.argumentsNames.add(0, instanceName);
                    method.argumentsTypes.add(0, fragment.name);
                }
				if (!currentMethod.name.startsWith(fragment.name + structOwnerMethodSeparator)) {
					currentMethod.name = fragment.name + structOwnerMethodSeparator + currentMethod.name;
				}
                final String key = toGLSL(currentMethod.owner) + "." + oldName;
                methodReplacements.put(key, currentMethod.name);

                if (DEBUG && fragment1.getClass() == StartOfMethodFragment.class) {
                    System.out.println("GLSLEncoder > Mapped " + key + " to " + currentMethod.name);
                }
            }
            if (fragment1 instanceof LoadVariableFragment var) {
                var.variableName = Objects.requireNonNull(currentMethod).varNameMap.get(var.variableIndex);
            } else if (fragment1 instanceof StoreVariableFragment var) {
                var.variableName = Objects.requireNonNull(currentMethod).varNameMap.get(var.variableIndex);
            }
            if (!waiting.isEmpty()) {
                handleCodeFragment(waiting.pop(), index, in, out);
            }

            this.allowedToPrint = !fragment1.forbiddenToPrint;
            if (fragment1.getClass() == EndOfMethodFragment.class && currentMethod.name.equals(fragment.name + structOwnerMethodSeparator + "new")) {
                println(getIndent() + "return " + instanceName + ";");
            }

            handleCodeFragment(fragment1, i, fragment.getChildren(), out);
            if (fragment1.getClass() == StartOfMethodFragment.class && currentMethod.name.equals(fragment.name + structOwnerMethodSeparator + "new")) {
                println(getIndent() + fragment.name + space + instanceName + ";");
            }
        }
    }

    private void handleDuplicateFragment(final DuplicateFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
		if (!newInstances.isEmpty()) {
			return;
		}
        if (fragment.wait > 0) {
            waiting.add(fragment);
            fragment.wait--;
        } else {
            final String a = stack.pop();
            stack.push(a);
            stack.push(a);
        }
    }

    private void interpret(final List<CodeFragment> in) {
        final Stack<String> copy = stack;
        stack = new Stack<String>();
        StartOfMethodFragment currentMethod = null;
        final PrintWriter nullPrinter = new PrintWriter(new StringWriter());
        for (int i = 0; i < in.size(); i++) {
            boolean dontHandle = false;
            final CodeFragment fragment = in.get(i);
            if (fragment.getClass() == StartOfMethodFragment.class) {
                currentMethod = (StartOfMethodFragment) fragment;
            } else if (fragment.getClass() == FieldFragment.class) {
                final FieldFragment fieldFrag = (FieldFragment) fragment;
                if (hasStructAttached(fieldFrag.type) && !loadedStructs.contains(toGLSL(fieldFrag.type))) {
                    loadedStructs.add(toGLSL(fieldFrag.type));
                    final StructFragment struct = new StructFragment();
                    struct.name = conversionsToStructs.get(fieldFrag.type);
                    struct.fields = new HashMap<String, String>();
                    currentRequestType = STRUCT;
                    requestData = struct;
                    final String s = "/" + fieldFrag.type.replace(".", "/") + ".class";
                    context.requestAnalysisForEncoder(GLSLEncoder.class.getResourceAsStream(s));
                    in.add(i, struct);
                    currentRequestType = 0;
                    i--;
                }
            } else if (fragment.getClass() == StoreVariableFragment.class) {
                final StoreVariableFragment storeFrag = (StoreVariableFragment) fragment;
                final String type = storeFrag.variableType;
                if (hasStructAttached(type) && !loadedStructs.contains(toGLSL(type))) {
                    loadedStructs.add(toGLSL(type));
                    final StructFragment struct = new StructFragment();
                    struct.name = conversionsToStructs.get(type);
                    struct.fields = new HashMap<String, String>();
                    currentRequestType = STRUCT;
                    requestData = struct;
                    final String s = "/" + type.replace(".", "/") + ".class";
                    context.requestAnalysisForEncoder(GLSLEncoder.class.getResourceAsStream(s));
                    in.add(i, struct);
                    currentRequestType = 0;
                    i--;
                }
            } else if (fragment.getClass() == PutFieldFragment.class) {
                final PutFieldFragment storeFrag = (PutFieldFragment) fragment;
                if (currentMethod != null && currentMethod.name.equals("<init>")) {
                    for (final CodeFragment fragment1 : in) {
                        if (fragment1.getClass() == FieldFragment.class) {
                            final FieldFragment fieldFrag = (FieldFragment) fragment1;
                            if (fieldFrag.name.equals(storeFrag.fieldName) && fieldFrag.type.equals(storeFrag.fieldType) && !(fieldFrag.access.isFinal() && fieldFrag.initialValue != null)) {
                                fieldFrag.initialValue = stack.peek();
                                dontHandle = true;
                                storeFrag.forbiddenToPrint = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!dontHandle) {
                this.output = nullPrinter;
                this.allowedToPrint = !fragment.forbiddenToPrint;
                if (!waiting.isEmpty()) {
                    handleCodeFragment(waiting.pop(), i, in, nullPrinter);
                }
                handleCodeFragment(fragment, i, in, nullPrinter);
            }
        }

        waiting.clear();
        currentLine = 0;
        indentation = 0;
        initialized.clear();
        name2type.clear();
        currentClass = null;
        typesStack.clear();
        extensions.clear();
        constants.clear();
        stack = copy;
    }

    private void println() {
        println("");
    }

    private void println(final String s) {
		if (allowedToPrint) {
			output.println(s);
		}
    }

    private void handleReturnFragment(final ReturnFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        if (in.size() <= index + 1 || in.get(index + 1).getClass() == EndOfMethodFragment.class) {
        } else {
            println(getIndent() + "return;" + getEndOfLine(currentLine));
        }
    }

    private void handlePopFragment(final PopFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        println(getIndent() + stack.pop() + ";" + getEndOfLine(currentLine));
    }

    private int countChar(final String str, final char c) {
        int nbr = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) {
				nbr++;
			}
		}
        return nbr;
    }

    private void handleIfNotStatementFragment(final IfNotStatementFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String condition = stack.pop();
        println(getIndent() + "if(!" + condition + ")" + getEndOfLine(currentLine));
        println(getIndent() + "{");
        indentation++;
    }

    private void handleXorFragment(final XorFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String b = stack.pop();
        final String a = stack.pop();
        stack.push("(" + a + " || " + b + ")");
    }

    private void handleOrFragment(final OrFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String b = stack.pop();
        final String a = stack.pop();
        stack.push("(" + a + space + (fragment.isDouble ? "||" : "|") + space + b + ")");
    }

    private void handleAndFragment(final AndFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String b = stack.pop();
        final String a = stack.pop();
        stack.push("(" + a + space + (fragment.isDouble ? "&&" : "&") + space + b + ")");
    }

    private void handleRightShiftFragment(final RightShiftFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String b = stack.pop();
        final String a = stack.pop();
        stack.push(a + ">>" + (!fragment.signed ? ">" : "") + b);
    }

    private void handleLeftShiftFragment(final LeftShiftFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String b = stack.pop();
        final String a = stack.pop();
        stack.push(a + "<<" + (!fragment.signed ? "<" : "") + b);
    }

    private void handleCastFragment(final CastFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String toCast = stack.pop();

        String previousType = null;
		if (toCast.startsWith("(")) {
			previousType = toCast.substring(1, toCast.indexOf(")") - 1);
		} else {
			previousType = toGLSL(currentMethod.varName2TypeMap.get(toCast));
		}
		if (previousType.equals(toGLSL(fragment.to))) {
			if (DEBUG) {
				System.out.println("GLSLEncoder > Cancelling cast for " + toCast);
			}
		} else {
			stack.push("(" + toGLSL(fragment.to) + ")" + toCast);
		}
    }

    private void handleModFragment(final ModFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push("mod(" + b + ", " + a + ")");
    }

    private void handleMethodCallFragment(final MethodCallFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        String s = "";
        String n = fragment.methodName;
        boolean isConstructor = false;
        if (n.equals("<init>")) {
            n = toGLSL(fragment.methodOwner);
            isConstructor = true;
			if (!newInstances.isEmpty()) {
				newInstances.pop();
			}
        }
        String key = fragment.methodName;
        toGLSL(fragment.methodOwner);
		if (!toGLSL(fragment.methodOwner).equals("null") && !toGLSL(fragment.methodOwner).trim().equals("")) {
			key = toGLSL(fragment.methodOwner) + "." + key;
		}
        if (methodReplacements.containsKey(key)) {
            n = methodReplacements.get(key);
			if (DEBUG) {
				System.out.println("GLSLEncoder > Replacing " + key + " by " + n);
			}
        }
        if (fragment.invokeType == InvokeTypes.SPECIAL && currentMethod.name.equals("<init>") && fragment.methodOwner.equals(currentClass.superclass)) {
            this.allowedToPrint = false;
        }

        s += n + "(";
        final ArrayList<String> args = new ArrayList<>();
        for (@SuppressWarnings("unused") final
        String type : fragment.argumentsTypes) {
            String arg = stack.pop();
            if (arg.startsWith("(") && arg.endsWith(")") && countChar(arg, '(') == countChar(arg, ')')) {
                arg = arg.substring(1, arg.length() - 1);
            }
            args.add(arg);
        }
        final StringBuilder argsStr = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
			if (i != 0) {
				argsStr.append(", ");
			}
            argsStr.append(args.get(args.size() - 1 - i));
        }
        s += argsStr;
        s += ")";
        boolean ownerBefore = false;
        boolean parenthesis = true;
        int ownerPosition = 0;
        boolean actsAsField = false;
        for (final CodeFragment child : fragment.getChildren()) {
            if (child.getClass() == AnnotationFragment.class) {
                final AnnotationFragment annot = (AnnotationFragment) child;
                if (annot.name.equals(Substitute.class.getCanonicalName())) {
					if (!annot.values.get("value").equals("$")) {
						n = (String) annot.values.get("value");
					}
					if (annot.values.containsKey("ownerBefore")) {
						ownerBefore = (Boolean) annot.values.get("ownerBefore");
					}
					if (annot.values.containsKey("ownerPosition")) {
						ownerPosition = (Integer) annot.values.get("ownerPosition");
					}
					if (annot.values.containsKey("actsAsField")) {
						actsAsField = (Boolean) annot.values.get("actsAsField");
					}
					if (annot.values.containsKey("usesParenthesis")) {
						parenthesis = (Boolean) annot.values.get("usesParenthesis");
					}
                }
            }
        }
        if (fragment.invokeType == InvokeTypes.VIRTUAL) {
            String owner = stack.pop();
            if (owner.equals(currentClass.className) || owner.equals("this")) {
                owner = null;
            } else {
                if (owner.startsWith("(") && owner.endsWith(")") && countChar(owner, '(') == countChar(owner, ')')) {
                    owner = owner.substring(1, owner.length() - 1);
                }
            }
			if (!ownerBefore) {
				if (actsAsField) {
					if (n.length() >= 1) {
						s = (owner != null ? owner : "") + "." + n;
					} else {
						s = (owner != null ? owner : "");
					}
					if (argsStr.length() > 0) {
						s += " = " + argsStr;
					}
				} else {
					s = n + (parenthesis ? "(" : "") + (owner != null ? owner + (argsStr.length() > 0 ? ", " : "") : "") + argsStr + (parenthesis ? ")" : "");
				}
			} else {
				s = (owner != null ? owner : "") + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : "");
			}
			if (fragment.returnType.equals("void")) {
				println(getIndent() + s + ";" + getEndOfLine(currentLine));
			} else {
				stack.push("(" + s + ")");
			}
        } else if (fragment.invokeType == InvokeTypes.STATIC) {
            String ownership = "";
            final String owner = toGLSL(fragment.methodOwner);
			if (!owner.trim().equals("") && !owner.equals("null")) {
				ownership = owner + (n.length() > 0 ? "." : "");
			}
            stack.push(ownership + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
        } else {
            stack.push(n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
        }

    }

    private void handleElseStatementFragment(final ElseStatementFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        println(getIndent() + "else" + getEndOfLine(currentLine));
        println(getIndent() + "{");
        indentation++;
    }

    private void handleEndOfBlockFragment(final EndOfBlockFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        indentation--;
        println(getIndent() + "}");
    }

    private void handleIfStatementFragment(final IfStatementFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String condition = stack.pop();
        println(getIndent() + "if(" + condition + ")" + getEndOfLine(currentLine));
        println(getIndent() + "{");
        indentation++;
    }

    private void handleArrayStoreFragment(final ArrayStoreFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        String result = "";
        String toAdd = "";
        for (int i = 0; i < 2; i++) {
            String copy = typesStack.pop();
            int dimensions = 0;
			if (copy != null) {
				while (copy.contains("[]")) {
					copy = copy.substring(copy.indexOf("[]") + 2);
					dimensions++;
				}
			}
            final String val = stack.pop();
            final StringBuilder arrayIndex = new StringBuilder();
            for (int dim = 0; dim < dimensions; dim++) {
                arrayIndex.insert(0, "[" + stack.pop() + "]");
            }
            final String name = stack.pop();
			if (i == 1) {
				result = val + toAdd + " = " + result;
			} else if (i == 0) {
				result = val + result;
				toAdd = "[" + name + "]";
			}
        }
        println(getIndent() + result + ";" + getEndOfLine(currentLine));
    }

    private void handleArrayOfArrayLoadFragment(final ArrayOfArrayLoadFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String value = stack.pop();
        final String name = stack.pop();
        stack.push(name + "[" + value + "]");
        if (name2type.containsKey(name + "[" + value + "]")) {
            name2type.put(name + "[" + value + "]", name.substring(0, name.indexOf("[")));
        }
        typesStack.push(name2type.get(name + "[" + value + "]"));
    }

    private void handleDivFragment(final DivFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push(b + "/" + a);
    }

    private void handleMulFragment(final MulFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push(b + "*" + a);
    }

    private void handleSubFragment(final SubFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push(b + "-" + a);
    }

    private void handleAddFragment(final AddFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push(b + "+" + a);
    }

    private void handleReturnValueFragment(final ReturnValueFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        println(getIndent() + "return" + space + stack.pop() + ";" + getEndOfLine(currentLine));
    }

    private void handleLoadConstantFragment(final LoadConstantFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        stack.push(fragment.value + "");
    }

    private void handleLdcFragment(final LdcFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
		if (constants.containsKey(fragment.value)) {
			stack.push("" + constants.get(fragment.value));
		} else if (fragment.value instanceof String) {
			stack.push("\"" + fragment.value + "\"");
		} else if (fragment.value instanceof Number) {
			stack.push("" + fragment.value);
		} else if (DEBUG) {
			System.out.println("GLSLEncoder > Invalid value: " + fragment.value + " of type " + fragment.value.getClass().getCanonicalName());
		}
    }

    private void handleStoreVariableFragment(final StoreVariableFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        String value = stack.pop();
        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.equals(fragment.variableName + "+1")) {
            println(getIndent() + fragment.variableName + "++;" + getEndOfLine(currentLine));
            return;
        } else if (value.equals(fragment.variableName + "-1")) {
            println(getIndent() + fragment.variableName + "--;" + getEndOfLine(currentLine));
            return;
        }
        final String glslType = toGLSL(currentMethod.varName2TypeMap.get(fragment.variableName));
        if (glslType.equals("bool")) {
			if (value.equals("0")) {
				value = "false";
			} else if (value.equals("1")) {
				value = "true";
			}
        } else if (glslType.equals("char")) {
            if (convertNumbersToChars) {
                try {
                    value = "'" + (char) Integer.parseInt(value) + "'";
                } catch (final Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }
        if (initialized.contains(fragment.variableName)) {
            println(getIndent() + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
        } else {
            initialized.add(fragment.variableName);
            println(getIndent() + toGLSL(currentMethod.varName2TypeMap.get(fragment.variableName)) + space + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
        }
    }

    private void handleLoadVariableFragment(final LoadVariableFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        stack.push(fragment.variableName);
    }

    private void handleNewPrimitiveArrayFragment(final NewPrimitiveArrayFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String dimension = "[" + stack.pop() + "]";
        stack.push(fragment.type + dimension);
    }

    private void handleBiPushFragment(final IntPushFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        stack.push(fragment.value + "");
    }

    private void handleGetFieldFragment(final GetFieldFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String owner = toGLSL(stack.pop());
        String ownership = owner + ".";
		if (owner.equals("this")) {
			ownership = "";
		}
        stack.push(ownership + fragment.fieldName);
        typesStack.push(fragment.fieldType);
    }

    private void handlePutFieldFragment(final PutFieldFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        String value = stack.pop();
        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.equals(fragment.fieldName + "+1")) {
            println(getIndent() + fragment.fieldName + "++;" + getEndOfLine(currentLine));
            return;
        } else if (value.equals(fragment.fieldName + "-1")) {
            println(getIndent() + fragment.fieldName + "--;" + getEndOfLine(currentLine));
            return;
        }
        final String glslType = toGLSL(currentMethod.varName2TypeMap.get(fragment.fieldName));
        if (glslType.equals("bool")) {
			if (value.equals("0")) {
				value = "false";
			} else if (value.equals("1")) {
				value = "true";
			}
        }
        final String owner = stack.pop();
        String ownership = owner + ".";
        for (int i = 0; i < index; i++) {
            final CodeFragment frag = in.get(i);
            if (frag.getClass() == FieldFragment.class) {
                final FieldFragment fieldFrag = (FieldFragment) frag;
                if (fieldFrag.access.isFinal() && fieldFrag.name.equals(fragment.fieldName)) {
                    return;
                }
            }
        }
		if (owner.equals("this")) {
			ownership = "";
		}
        println(getIndent() + ownership + fragment.fieldName + space + "=" + space + value + ";" + getEndOfLine(currentLine));
    }

    private void handleNewMultiArrayFragment(final NewMultiArrayFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final StringBuilder s = new StringBuilder();
        final ArrayList<String> list = new ArrayList<>();
        for (int dim = 0; dim < fragment.dimensions; dim++) {
            list.add(stack.pop());
        }
        for (int dim = 0; dim < fragment.dimensions; dim++) {
            s.append("[").append(list.get(list.size() - dim - 1)).append("]");
        }
        stack.push(toGLSL(fragment.type) + s);
    }

    private void handleNewArrayFragment(final NewArrayFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String s = "[" + stack.pop() + "]";
        stack.push(toGLSL(fragment.type) + s);
    }

    private void handleEndOfMethodFragment(final EndOfMethodFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
		if (fragment.name.equals("<init>")) {
			return;
		}
        println("}");
        indentation--;
    }

    private void handleStartOfMethodFragment(final StartOfMethodFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
		if (fragment.name.equals("<init>")) {
			return;
		}
        initialized.clear();
        println();
        final StringBuilder args = new StringBuilder();
        for (int i = 0; i < fragment.argumentsNames.size(); i++) {
            final String s = toGLSL(fragment.argumentsTypes.get(i)) + space + fragment.argumentsNames.get(i);
			if (i != 0) {
				args.append(", ");
			}
            args.append(s);
        }
        println(toGLSL(fragment.returnType) + space + fragment.name + "(" + args + ")\n{");
        indentation++;
    }

    private void handleFieldFragment(final FieldFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        String storageType = null;
        for (final CodeFragment child : fragment.getChildren()) {
            if (child instanceof AnnotationFragment annot) {
                if (annot.name.equals(Uniform.class.getCanonicalName())) {
                    storageType = "uniform";
                } else if (annot.name.equals(Attribute.class.getCanonicalName())) {
                    storageType = "attribute";
                    if (currentClass.superclass.equals(FragmentShaderEnvironment.class.getCanonicalName())) {
                        throw new JLSLException("Attributes are not allowed in fragment shaders");
                    }
                } else if (annot.name.equals(In.class.getCanonicalName())) {
                    storageType = "in";
                } else if (annot.name.equals(Out.class.getCanonicalName())) {
                    storageType = "out";
                } else if (annot.name.equals(Varying.class.getCanonicalName())) {
                    storageType = "varying";
                } else if (annot.name.equals(Layout.class.getCanonicalName())) {
                    final int location = (Integer) annot.values.get("location");

					if (glslversion > 430 || extensions.contains("GL_ARB_explicit_uniform_location")) {
						out.print("layout(location = " + location + ") ");
					}
                }
            }
        }
        if (storageType == null) {
            storageType = "uniform";
        }
        if (fragment.access.isFinal()) {
            if (fragment.access.isStatic()) {
                println("#define" + space + fragment.name + space + fragment.initialValue);
            } else {
                storageType = "const";
                println(storageType + space + toGLSL(fragment.type) + space + fragment.name + space + "=" + space + fragment.initialValue + ";");
            }
            constants.put(fragment.initialValue, fragment.name);
        } else {
			if (fragment.initialValue != null) {
				println(storageType + space + toGLSL(fragment.type) + space + fragment.name + space + "=" + space + fragment.initialValue + ";");
			} else {
				println(storageType + space + toGLSL(fragment.type) + space + fragment.name + ";");
			}
        }
    }

    @SuppressWarnings("unchecked")
    private void handleClassFragment(final NewClassFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        println("// Original class name: " + fragment.className + " compiled from " + fragment.sourceFile + " and of version " + fragment.classVersion);
        for (final CodeFragment child : fragment.getChildren()) {
            if (child instanceof AnnotationFragment annotFragment) {
                println();
                if (annotFragment.name.equals(Extensions.class.getCanonicalName())) {
                    final ArrayList<String> values = (ArrayList<String>) annotFragment.values.get("value");
					for (final String extension : values) {
						println("#extension " + extension + " : enable" + getEndOfLine(currentLine));
					}
                }
            }
        }
    }

    private String getIndent() {
        final StringBuilder s = new StringBuilder();
		for (int i = 0; i < indentation; i++) {
			s.append(tab);
		}
        return s.toString();
    }

}
