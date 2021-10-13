package org.jglrxavpok.jlsl.java;

import org.jglrxavpok.jlsl.CodeEncoder;
import org.jglrxavpok.jlsl.fragments.*;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment.InvokeTypes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class JavaEncoder extends CodeEncoder {
    public static final boolean DEBUG = true;
    private final Stack<String> typesStack;
    private final HashMap<String, String> name2type;
    private final HashMap<Object, String> constants;
    private final ArrayList<String> initialized;
    private final Stack<CodeFragment> waiting;
    private final Stack<String> newInstances;
    private final HashMap<String, String> imports;
    public boolean interpreting;
    private int indentation;
    private NewClassFragment currentClass;
    private int currentLine;
    private Stack<String> stack;
    private StartOfMethodFragment currentMethod;
    private boolean allowedToPrint;
    private PrintWriter output;
    private String classPackage;

    private String className;

    public JavaEncoder(final int glslversion) {
        imports = new HashMap<>();
        stack = new Stack<>();
        typesStack = new Stack<>();
        initialized = new ArrayList<>();
        name2type = new HashMap<>();
        constants = new HashMap<>();
        waiting = new Stack<>();
        newInstances = new Stack<>();

        init();
    }

    public void init() {

    }

    private String toJava(String type) {
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
		if (type.startsWith("java.lang.")) {
			type = type.replaceFirst("java.lang.", "");
		}
        if (type.contains(".") && !type.startsWith("this.") && !this.name2type.containsKey(type)) {
            final String withoutPackage = type.substring(type.lastIndexOf(".") + 1);
            if (imports.containsKey(withoutPackage)) {
                final String fullName = imports.get(withoutPackage);
                if (fullName.equals(type)) {
                    return withoutPackage + end;
                }
            } else {
                imports.put(withoutPackage, type);
                return withoutPackage + end;
            }
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

    @Override
    public void onRequestResult(final ArrayList<CodeFragment> fragments) {
    }

    @Override
    public void createSourceCode(final List<CodeFragment> in, final PrintWriter out) {
        this.interpreting = true;
        interpret(in);
        this.interpreting = false;
        this.output = out;
        this.allowedToPrint = true;
        println("package " + classPackage + ";\n");
        for (final String importName : imports.values()) {
            println("import " + importName + ";");
        }
        for (int index = 0; index < in.size(); index++) {
            final CodeFragment fragment = in.get(index);
            this.output = out;
            this.allowedToPrint = !fragment.forbiddenToPrint;
            if (!waiting.isEmpty()) {
                handleCodeFragment(waiting.pop(), index, in, out);
            }
            handleCodeFragment(fragment, index, in, out);
        }
        println("}");
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
        }
    }

    private void handleCompareFragment(final CompareFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String right = stack.pop();
        final String left = stack.pop();
        stack.push(left + " " + (fragment.inferior ? "<" : ">") + " " + right);
    }

    private void handleNotEqualCheckFragment(final NotEqualCheckFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String right = stack.pop();
        final String left = stack.pop();
        stack.push("(" + left + " " + "!=" + " " + right + ")");
    }

    private void handleEqualCheckFragment(final EqualCheckFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String right = stack.pop();
        final String left = stack.pop();
        stack.push("(" + left + " " + "==" + " " + right + ")");
    }

    private void handleNewInstanceFragment(final NewInstanceFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        newInstances.push(fragment.type);
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
            } else if (fragment.getClass() == StoreVariableFragment.class) {
                final StoreVariableFragment storeFrag = (StoreVariableFragment) fragment;
                final String type = storeFrag.variableType;
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
        stack.push("(" + a + " " + (fragment.isDouble ? "||" : "|") + " " + b + ")");
    }

    private void handleAndFragment(final AndFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String b = stack.pop();
        final String a = stack.pop();
        stack.push("(" + a + " " + (fragment.isDouble ? "&&" : "&") + " " + b + ")");
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
			previousType = toJava(currentMethod.varName2TypeMap.get(toCast));
		}
		if (previousType.equals(toJava(fragment.to))) {
			if (DEBUG) {
				System.out.println("GLSLEncoder > Cancelling cast for " + toCast);
			}
		} else {
			stack.push("(" + toJava(fragment.to) + ")" + toCast);
		}
    }

    private void handleModFragment(final ModFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push("(" + b + " % " + a + ")");
    }

    private void handleMethodCallFragment(final MethodCallFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        String s = "";
        String n = fragment.methodName;
        boolean isConstructor = false;
        if (n.equals("<init>")) {
            n = "new " + toJava(fragment.methodOwner);
            isConstructor = true;
			if (!newInstances.isEmpty()) {
				newInstances.pop();
			}
        }
        String key = fragment.methodName;
        toJava(fragment.methodOwner);
		if (!toJava(fragment.methodOwner).equals("null") && !toJava(fragment.methodOwner).trim().equals("")) {
			key = toJava(fragment.methodOwner) + "." + key;
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
        final boolean ownerBefore = false;
        final boolean parenthesis = true;
        final int ownerPosition = 0;
        final boolean actsAsField = false;
        if (fragment.invokeType == InvokeTypes.VIRTUAL) {
            String owner = stack.pop();
            if (owner.equals(currentClass.className) || owner.equals("this")) {
                owner = null;
            } else {
                if (owner.startsWith("(") && owner.endsWith(")") && countChar(owner, '(') == countChar(owner, ')')) {
                    owner = owner.substring(1, owner.length() - 1);
                }
            }
            s = (owner != null ? owner + "." : "") + n + (parenthesis ? "(" : "") + argsStr + ")";
			if (fragment.returnType.equals("void")) {
				println(getIndent() + s + ";" + getEndOfLine(currentLine));
			} else {
				stack.push("(" + s + ")");
			}
        } else if (fragment.invokeType == InvokeTypes.STATIC) {
            String ownership = "";
            final String owner = toJava(fragment.methodOwner);
			if (!owner.trim().equals("") && !owner.equals("null")) {
				ownership = owner + (n.length() > 0 ? "." : "");
			}
            stack.push(ownership + n + "(" + argsStr + ")");
        } else {
            stack.push(n + "(" + argsStr + ")");

			if (fragment.returnType.equals("void") && !fragment.methodName.equals("<init>")) {
				println(getIndent() + stack.pop() + ";");
			}
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
        stack.push("(" + b + "/" + a + ")");
    }

    private void handleMulFragment(final MulFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push("(" + b + "*" + a + ")");
    }

    private void handleSubFragment(final SubFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push("(" + b + "-" + a + ")");
    }

    private void handleAddFragment(final AddFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String a = stack.pop();
        final String b = stack.pop();
        stack.push("(" + b + "+" + a + ")");
    }

    private void handleReturnValueFragment(final ReturnValueFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        println(getIndent() + "return" + " " + stack.pop() + ";" + getEndOfLine(currentLine));
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
        final String javaType = toJava(currentMethod.varName2TypeMap.get(fragment.variableName));
        if (javaType.equals("boolean")) {
			if (value.equals("0")) {
				value = "false";
			} else if (value.equals("1")) {
				value = "true";
			}
        } else if (javaType.equals("char")) {
            try {
                value = "'" + (char) Integer.parseInt(value) + "'";
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        if (initialized.contains(fragment.variableName)) {
            println(getIndent() + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
        } else {
            initialized.add(fragment.variableName);
            println(getIndent() + toJava(currentMethod.varName2TypeMap.get(fragment.variableName)) + " " + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
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
        final String owner = toJava(stack.pop());
        final String ownership = owner + ".";
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
        final String javaType = toJava(currentMethod.varName2TypeMap.get(fragment.fieldName));
        if (javaType.equals("boolean")) {
			if (value.equals("0")) {
				value = "false";
			} else if (value.equals("1")) {
				value = "true";
			}
        }
        final String owner = stack.pop();
        final String ownership = owner + ".";
        for (int i = 0; i < index; i++) {
            final CodeFragment frag = in.get(i);
            if (frag.getClass() == FieldFragment.class) {
                final FieldFragment fieldFrag = (FieldFragment) frag;
                if (fieldFrag.access.isFinal() && fieldFrag.name.equals(fragment.fieldName)) {
                    return;
                }
            }
        }
        println(getIndent() + ownership + fragment.fieldName + " " + "=" + " " + value + ";" + getEndOfLine(currentLine));
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
        stack.push(toJava(fragment.type) + s);
    }

    private void handleNewArrayFragment(final NewArrayFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String s = "[" + stack.pop() + "]";
        stack.push(toJava(fragment.type) + s);
    }

    private void handleEndOfMethodFragment(final EndOfMethodFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        indentation--;
        println(getIndent() + "}");
    }

    private void handleStartOfMethodFragment(final StartOfMethodFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        if (fragment.name.equals("<init>")) {
            final String n = className;
            initialized.clear();
            println();
            final StringBuilder args = new StringBuilder();
            for (int i = 0; i < fragment.argumentsNames.size(); i++) {
                final String s = toJava(fragment.argumentsTypes.get(i)) + " " + fragment.argumentsNames.get(i);
				if (i != 0) {
					args.append(", ");
				}
                args.append(s);
            }
            String accessStr = "";
            if (fragment.access.isPublic()) {
                accessStr = "public";
            } else if (fragment.access.isProtected()) {
                accessStr = "protected";
            } else if (fragment.access.isPrivate()) {
                accessStr = "private";
            }
            println(getIndent() + accessStr + " " + n + "(" + args + ")\n" + getIndent() + "{");
        } else {
            initialized.clear();
            println();
            final StringBuilder args = new StringBuilder();
            for (int i = 0; i < fragment.argumentsNames.size(); i++) {
                final String s = toJava(fragment.argumentsTypes.get(i)) + " " + fragment.argumentsNames.get(i);
				if (i != 0) {
					args.append(", ");
				}
                args.append(s);
            }
            println(getIndent() + toJava(fragment.returnType) + " " + fragment.name + "(" + args + ")\n" + getIndent() + "{");
        }
        indentation++;
    }

    private void handleFieldFragment(final FieldFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        final String storageType = null;
        for (final CodeFragment child : fragment.getChildren()) {
            if (child instanceof AnnotationFragment annot) {
                println(getIndent() + "@" + toJava(annot.name));
            }
        }
        String str = "";
        if (fragment.access.isPublic()) {
            str += "public ";
        } else if (fragment.access.isPrivate()) {
            str += "private ";
        } else if (fragment.access.isProtected()) {
            str += "protected ";
        }
        if (fragment.access.isStatic()) {
            str += "static ";
        }
        if (fragment.access.isFinal()) {
            str += "final ";
        }
        str += toJava(fragment.type) + " ";
        str += fragment.name;
		if (fragment.initialValue != null) {
			str += " = " + fragment.initialValue;
		}
        println(getIndent() + str + ";");
    }

    @SuppressWarnings("unchecked")
    private void handleClassFragment(final NewClassFragment fragment, final List<CodeFragment> in, final int index, final PrintWriter out) {
        println("// Original class name: " + fragment.className + " compiled from " + fragment.sourceFile + " and of version " + fragment.classVersion);
        classPackage = fragment.className.substring(0, fragment.className.lastIndexOf("."));
        className = fragment.className.substring(fragment.className.lastIndexOf(".") + 1);
        for (final CodeFragment child : fragment.getChildren()) {
            if (child instanceof AnnotationFragment annotFragment) {
                println("@" + toJava(annotFragment.name));
            }
        }
        String hierarchy = "";
		if (fragment.superclass != null && !fragment.superclass.equals(Object.class.getCanonicalName())) {
			hierarchy += " extends " + toJava(fragment.superclass);
		}
        String access = "";
        if (fragment.access.isPublic()) {
            access += "public ";
        } else if (fragment.access.isProtected()) {
            access += "protected ";
        } else if (fragment.access.isPrivate()) {
            access += "private ";
        }
        println(access + "class " + className + hierarchy);
        println("{");
        indentation++;
    }

    private String getIndent() {
        final StringBuilder s = new StringBuilder();
		for (int i = 0; i < indentation; i++) {
			s.append("    ");
		}
        return s.toString();
    }

}
