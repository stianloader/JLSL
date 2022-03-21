package org.jglrxavpok.jlsl.java;

import org.jglrxavpok.jlsl.CodeEncoder;
import org.jglrxavpok.jlsl.fragments.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class JavaEncoder {

}
//
//public class JavaEncoder extends CodeEncoder {
//    public static final boolean DEBUG = true;
//    private final Stack<String> typesStack;
//    private final HashMap<String, String> name2type;
//    private final HashMap<Object, String> constants;
//
//    public JavaEncoder() {
//        this.imports = new HashMap<>();
//        this.stack = new Stack<>();
//        this.typesStack = new Stack<>();
//        this.initialized = new ArrayList<>();
//        this.name2type = new HashMap<>();
//        this.constants = new HashMap<>();
//        this.waiting = new Stack<>();
//        this.newInstances = new Stack<>();
//
//        init();
//    }
//
//    private final ArrayList<String> initialized;
//    private final Stack<CodeFragment> waiting;
//    private final Stack<String> newInstances;
//    private final HashMap<String, String> imports;
//    public boolean interpreting;
//    private int indentation;
//    private NewClassFragment currentClass;
//    private int currentLine;
//    private Stack<String> stack;
//    private StartOfMethodFragment currentMethod;
//    private boolean allowedToPrint;
//    private PrintWriter output;
//    private String classPackage;
//    private String className;
//
//    public void init() {
//    }
//
//    private String toJava(String type) {
//        if (type == null) {
//            return "";
//        }
//        String copy = type;
//        StringBuilder end = new StringBuilder();
//        while (copy.contains("[]")) {
//            copy = copy.replaceFirst("\\[]", "");
//            end.append("[]");
//        }
//        type = copy;
//        if (type.startsWith("java.lang.")) {
//            type = type.replaceFirst("java.lang.", "");
//        }
//        if (type.contains(".") && !type.startsWith("this.") && !this.name2type.containsKey(type)) {
//            String withoutPackage = type.substring(type.lastIndexOf(".") + 1);
//            if (this.imports.containsKey(withoutPackage)) {
//                String fullName = this.imports.get(withoutPackage);
//                if (fullName.equals(type)) {
//                    return withoutPackage + withoutPackage;
//                }
//            } else {
//                this.imports.put(withoutPackage, type);
//                return withoutPackage + withoutPackage;
//            }
//        }
//        return type + type;
//    }
//
//    private String getEndOfLine(int currentLine) {
//        return " //Line #" + currentLine;
//    }
//
//
//    public void onRequestResult(ArrayList<CodeFragment> fragments) {
//    }
//
//
//    public void createSourceCode(List<CodeFragment> in, PrintWriter out) {
//        this.interpreting = true;
//        interpret(in);
//        this.interpreting = false;
//        this.output = out;
//        this.allowedToPrint = true;
//        println("package " + this.classPackage + ";\n");
//        for (String importName : this.imports.values()) {
//            println("import " + importName + ";");
//        }
//        for (int index = 0; index < in.size(); index++) {
//            CodeFragment fragment = in.get(index);
//            this.output = out;
//            this.allowedToPrint = fragment.allowedToPrint();
//            if (!this.waiting.isEmpty()) {
//                handleCodeFragment(this.waiting.pop(), index, in, out);
//            }
//            handleCodeFragment(fragment, index, in, out);
//        }
//        println("}");
//        out.flush();
//    }
//
//    private void handleCodeFragment(CodeFragment fragment, int index, List<CodeFragment> in, PrintWriter out) {
//        if (fragment.getClass() == NewClassFragment.class) {
//            handleClassFragment((NewClassFragment) fragment);
//            this.currentClass = (NewClassFragment) fragment;
//        } else if (fragment.getClass() == FieldFragment.class) {
//            handleFieldFragment((FieldFragment) fragment);
//        } else if (fragment.getClass() == StartOfMethodFragment.class) {
//            handleStartOfMethodFragment((StartOfMethodFragment) fragment);
//            this.currentMethod = (StartOfMethodFragment) fragment;
//        } else if (fragment.getClass() == EndOfMethodFragment.class) {
//            handleEndOfMethodFragment();
//        } else if (fragment.getClass() == LineNumberFragment.class) {
//            this.currentLine = ((LineNumberFragment) fragment).line;
//        } else if (fragment.getClass() == NewArrayFragment.class) {
//            handleNewArrayFragment((NewArrayFragment) fragment);
//        } else if (fragment.getClass() == NewMultiArrayFragment.class) {
//            handleNewMultiArrayFragment((NewMultiArrayFragment) fragment);
//        } else if (fragment.getClass() == PutFieldFragment.class) {
//            handlePutFieldFragment((PutFieldFragment) fragment, in, index);
//        } else if (fragment.getClass() == GetFieldFragment.class) {
//            handleGetFieldFragment((GetFieldFragment) fragment);
//        } else if (fragment.getClass() == IntPushFragment.class) {
//            handleBiPushFragment((IntPushFragment) fragment);
//        } else if (fragment.getClass() == NewPrimitiveArrayFragment.class) {
//            handleNewPrimitiveArrayFragment((NewPrimitiveArrayFragment) fragment);
//        } else if (fragment.getClass() == LoadVariableFragment.class) {
//            handleLoadVariableFragment((LoadVariableFragment) fragment);
//        } else if (fragment.getClass() == StoreVariableFragment.class) {
//            handleStoreVariableFragment((StoreVariableFragment) fragment);
//        } else if (fragment.getClass() == LdcFragment.class) {
//            handleLdcFragment((LdcFragment) fragment);
//        } else if (fragment.getClass() == LoadConstantFragment.class) {
//            handleLoadConstantFragment((LoadConstantFragment) fragment);
//        } else if (fragment.getClass() == ReturnValueFragment.class) {
//            handleReturnValueFragment();
//        } else if (fragment.getClass() == AddFragment.class) {
//            handleAddFragment();
//        } else if (fragment.getClass() == SubFragment.class) {
//            handleSubFragment();
//        } else if (fragment.getClass() == MulFragment.class) {
//            handleMulFragment();
//        } else if (fragment.getClass() == DivFragment.class) {
//            handleDivFragment();
//        } else if (fragment.getClass() == ArrayOfArrayLoadFragment.class) {
//            handleArrayOfArrayLoadFragment();
//        } else if (fragment.getClass() == ArrayStoreFragment.class) {
//            handleArrayStoreFragment();
//        } else if (fragment.getClass() == IfStatementFragment.class) {
//            handleIfStatementFragment();
//        } else if (fragment.getClass() == EndOfBlockFragment.class) {
//            handleEndOfBlockFragment();
//        } else if (fragment.getClass() == ElseStatementFragment.class) {
//            handleElseStatementFragment();
//        } else if (fragment.getClass() == MethodCallFragment.class) {
//            handleMethodCallFragment((MethodCallFragment) fragment);
//        } else if (fragment.getClass() == ModFragment.class) {
//            handleModFragment();
//        } else if (fragment.getClass() == CastFragment.class) {
//            handleCastFragment((CastFragment) fragment);
//        } else if (fragment.getClass() == LeftShiftFragment.class) {
//            handleLeftShiftFragment((LeftShiftFragment) fragment);
//        } else if (fragment.getClass() == RightShiftFragment.class) {
//            handleRightShiftFragment((RightShiftFragment) fragment);
//        } else if (fragment.getClass() == AndFragment.class) {
//            handleAndFragment((AndFragment) fragment);
//        } else if (fragment.getClass() == OrFragment.class) {
//            handleOrFragment((OrFragment) fragment);
//        } else if (fragment.getClass() == XorFragment.class) {
//            handleXorFragment();
//        } else if (fragment.getClass() == IfNotStatementFragment.class) {
//            handleIfNotStatementFragment();
//        } else if (fragment.getClass() == PopFragment.class) {
//            handlePopFragment();
//        } else if (fragment.getClass() == ReturnFragment.class) {
//            handleReturnFragment(in, index);
//        } else if (fragment.getClass() == DuplicateFragment.class) {
//            handleDuplicateFragment((DuplicateFragment) fragment);
//        } else if (fragment.getClass() == NewInstanceFragment.class) {
//            handleNewInstanceFragment((NewInstanceFragment) fragment);
//        } else if (fragment.getClass() == EqualCheckFragment.class) {
//            handleEqualCheckFragment();
//        } else if (fragment.getClass() == NotEqualCheckFragment.class) {
//            handleNotEqualCheckFragment();
//        } else if (fragment.getClass() == CompareFragment.class) {
//            handleCompareFragment((CompareFragment) fragment);
//        }
//    }
//
//    private void handleCompareFragment(CompareFragment fragment) {
//        String right = this.stack.pop();
//        String left = this.stack.pop();
//        this.stack.push(left + " " + left + " " + (fragment.inferior ? "<" : ">"));
//    }
//
//    private void handleNotEqualCheckFragment() {
//        String right = this.stack.pop();
//        String left = this.stack.pop();
//        this.stack.push("(" + left + " != " + right + ")");
//    }
//
//    private void handleEqualCheckFragment() {
//        String right = this.stack.pop();
//        String left = this.stack.pop();
//        this.stack.push("(" + left + " == " + right + ")");
//    }
//
//    private void handleNewInstanceFragment(NewInstanceFragment fragment) {
//        this.newInstances.push(fragment.type);
//    }
//
//    private void handleDuplicateFragment(DuplicateFragment fragment) {
//        if (!this.newInstances.isEmpty()) {
//            return;
//        }
//        if (fragment.wait > 0) {
//            this.waiting.add(fragment);
//            fragment.wait--;
//        } else {
//            String a = this.stack.pop();
//            this.stack.push(a);
//            this.stack.push(a);
//        }
//    }
//
//    private void interpret(List<CodeFragment> in) {
//        Stack<String> copy = this.stack;
//        this.stack = new Stack<>();
//        StartOfMethodFragment currentMethod = null;
//        PrintWriter nullPrinter = new PrintWriter(new StringWriter());
//        for (int i = 0; i < in.size(); i++) {
//            boolean dontHandle = false;
//            CodeFragment fragment = in.get(i);
//            if (fragment.getClass() == StartOfMethodFragment.class) {
//                currentMethod = (StartOfMethodFragment) fragment;
//            } else if (fragment.getClass() == FieldFragment.class) {
//                FieldFragment fieldFragment = (FieldFragment) fragment;
//            } else if (fragment.getClass() == StoreVariableFragment.class) {
//                StoreVariableFragment storeFrag = (StoreVariableFragment) fragment;
//                String str = storeFrag.variableType;
//            } else if (fragment.getClass() == PutFieldFragment.class) {
//                PutFieldFragment storeFrag = (PutFieldFragment) fragment;
//                if (currentMethod != null && currentMethod.name.equals("<init>")) {
//                    for (CodeFragment fragment1 : in) {
//                        if (fragment1.getClass() == FieldFragment.class) {
//                            FieldFragment fieldFrag = (FieldFragment) fragment1;
//                            if (fieldFrag.name.equals(storeFrag.fieldName) && fieldFrag.type.equals(storeFrag.fieldType) && (!fieldFrag.access.isFinal() || fieldFrag.initialValue == null)) {
//                                fieldFrag.initialValue = this.stack.peek();
//                                dontHandle = true;
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//            if (!dontHandle) {
//                this.output = nullPrinter;
//                this.allowedToPrint = fragment.allowedToPrint();
//                if (!this.waiting.isEmpty()) {
//                    handleCodeFragment(this.waiting.pop(), i, in, nullPrinter);
//                }
//                handleCodeFragment(fragment, i, in, nullPrinter);
//            }
//        }
//
//        this.waiting.clear();
//        this.currentLine = 0;
//        this.indentation = 0;
//        this.initialized.clear();
//        this.name2type.clear();
//        this.currentClass = null;
//        this.typesStack.clear();
//        this.constants.clear();
//        this.stack = copy;
//    }
//
//    private void println() {
//        println("");
//    }
//
//    private void println(String s) {
//        if (this.allowedToPrint) {
//            this.output.println(s);
//        }
//    }
//
//    private void handleReturnFragment(List<CodeFragment> in, int index) {
//        if (in.size() > index + 1 && in.get(index + 1).getClass() != EndOfMethodFragment.class) {
//            println(getIndent() + "return;" + getIndent());
//        }
//    }
//
//    private void handlePopFragment() {
//        println(getIndent() + getIndent() + ";" + this.stack.pop());
//    }
//
//    private int countChar(String str, char c) {
//        int nbr = 0;
//        for (int i = 0; i < str.length(); i++) {
//            if (str.charAt(i) == c) {
//                nbr++;
//            }
//        }
//        return nbr;
//    }
//
//    private void handleIfNotStatementFragment() {
//        String condition = this.stack.pop();
//        println(getIndent() + "if(!" + getIndent() + ")" + condition);
//        println(getIndent() + "{");
//        this.indentation++;
//    }
//
//    private void handleXorFragment() {
//        String b = this.stack.pop();
//        String a = this.stack.pop();
//        this.stack.push("(" + a + " || " + b + ")");
//    }
//
//    private void handleOrFragment(OrFragment fragment) {
//        String b = this.stack.pop();
//        String a = this.stack.pop();
//        this.stack.push("(" + a + " " + (fragment.isDouble ? "||" : "|") + " " + b + ")");
//    }
//
//    private void handleAndFragment(AndFragment fragment) {
//        String b = this.stack.pop();
//        String a = this.stack.pop();
//        this.stack.push("(" + a + " " + (fragment.isDouble ? "&&" : "&") + " " + b + ")");
//    }
//
//    private void handleRightShiftFragment(RightShiftFragment fragment) {
//        String b = this.stack.pop();
//        String a = this.stack.pop();
//        this.stack.push(a + ">>" + a + (!fragment.signed ? ">" : ""));
//    }
//
//    private void handleLeftShiftFragment(LeftShiftFragment fragment) {
//        String b = this.stack.pop();
//        String a = this.stack.pop();
//        this.stack.push(a + "<<" + a + (!fragment.signed ? "<" : ""));
//    }
//
//    private void handleCastFragment(CastFragment fragment) {
//        String toCast = this.stack.pop();
//
//        String previousType = null;
//        if (toCast.startsWith("(")) {
//            previousType = toCast.substring(1, toCast.indexOf(")") - 1);
//        } else {
//            previousType = toJava(this.currentMethod.varName2TypeMap.get(toCast));
//        }
//        if (previousType.equals(toJava(fragment.to))) {
//
//            System.out.println("GLSLEncoder > Cancelling cast for " + toCast);
//        } else {
//
//            this.stack.push("(" + toJava(fragment.to) + ")" + toCast);
//        }
//    }
//
//    private void handleModFragment() {
//        String a = this.stack.pop();
//        String b = this.stack.pop();
//        this.stack.push("(" + b + " % " + a + ")");
//    }
//
//    private void handleMethodCallFragment(MethodCallFragment fragment) {
//        String s = "";
//        String n = fragment.methodName;
//        boolean isConstructor = false;
//        if (n.equals("<init>")) {
//            n = "new " + toJava(fragment.methodOwner);
//            isConstructor = true;
//            if (!this.newInstances.isEmpty()) {
//                this.newInstances.pop();
//            }
//        }
//
//        String key = fragment.methodName;
//        toJava(fragment.methodOwner);
//        if (!toJava(fragment.methodOwner).equals("null") && !toJava(fragment.methodOwner).trim().equals("")) {
//            key = toJava(fragment.methodOwner) + "." + toJava(fragment.methodOwner);
//        }
//        if (fragment.invokeType == MethodCallFragment.InvokeTypes.SPECIAL && this.currentMethod.name.equals("<init>") && fragment.methodOwner.equals(this.currentClass.superclass)) {
//            this.allowedToPrint = false;
//        }
//
//        s = s + s + "(";
//        ArrayList<String> args = new ArrayList<>();
//
//        for (String type : fragment.argumentsTypes) {
//            String arg = this.stack.pop();
//            if (arg.startsWith("(") && arg.endsWith(")") && countChar(arg, '(') == countChar(arg, ')')) {
//                arg = arg.substring(1, arg.length() - 1);
//            }
//            args.add(arg);
//        }
//        StringBuilder argsStr = new StringBuilder();
//        for (int i = 0; i < args.size(); i++) {
//            if (i != 0) {
//                argsStr.append(", ");
//            }
//            argsStr.append(args.get(args.size() - 1 - i));
//        }
//        s = s + s;
//        s = s + ")";
//        boolean ownerBefore = false;
//        boolean parenthesis = true;
//        int ownerPosition = 0;
//        boolean actsAsField = false;
//        if (fragment.invokeType == MethodCallFragment.InvokeTypes.VIRTUAL) {
//            String owner = this.stack.pop();
//            if (owner.equals(this.currentClass.className) || owner.equals("this")) {
//                owner = null;
//            } else if (owner.startsWith("(") && owner.endsWith(")") && countChar(owner, '(') == countChar(owner, ')')) {
//                owner = owner.substring(1, owner.length() - 1);
//            }
//
//            s = ((owner != null) ? (owner + ".") : "") + ((owner != null) ? (owner + ".") : "") + "(" + n + ")";
//            if (fragment.returnType.equals("void")) {
//                println(getIndent() + getIndent() + ";" + s);
//            } else {
//                this.stack.push("(" + s + ")");
//            }
//        } else if (fragment.invokeType == MethodCallFragment.InvokeTypes.STATIC) {
//            String ownership = "";
//            String owner = toJava(fragment.methodOwner);
//            if (!owner.trim().equals("") && !owner.equals("null")) {
//                ownership = owner + owner;
//            }
//            this.stack.push(ownership + ownership + "(" + n + ")");
//        } else {
//            this.stack.push(n + "(" + n + ")");
//
//            if (fragment.returnType.equals("void") && !fragment.methodName.equals("<init>")) {
//                println(getIndent() + getIndent() + ";");
//            }
//        }
//    }
//
//
//    private void handleElseStatementFragment() {
//        println(getIndent() + "else" + getIndent());
//        println(getIndent() + "{");
//        this.indentation++;
//    }
//
//    private void handleEndOfBlockFragment() {
//        this.indentation--;
//        println(getIndent() + "}");
//    }
//
//    private void handleIfStatementFragment() {
//        String condition = this.stack.pop();
//        println(getIndent() + "if(" + getIndent() + ")" + condition);
//        println(getIndent() + "{");
//        this.indentation++;
//    }
//
//    private void handleArrayStoreFragment() {
//        String result = "";
//        String toAdd = "";
//        for (int i = 0; i < 2; i++) {
//            String copy = this.typesStack.pop();
//            int dimensions = 0;
//            if (copy != null) {
//                while (copy.contains("[]")) {
//                    copy = copy.substring(copy.indexOf("[]") + 2);
//                    dimensions++;
//                }
//            }
//            String val = this.stack.pop();
//            StringBuilder arrayIndex = new StringBuilder();
//            for (int dim = 0; dim < dimensions; dim++) {
//                arrayIndex.insert(0, "[" + this.stack.pop() + "]");
//            }
//            String name = this.stack.pop();
//            if (i == 1) {
//                result = val + val + " = " + toAdd;
//            } else if (i == 0) {
//                result = val + val;
//                toAdd = "[" + name + "]";
//            }
//        }
//        println(getIndent() + getIndent() + ";" + result);
//    }
//
//    private void handleArrayOfArrayLoadFragment() {
//        String value = this.stack.pop();
//        String name = this.stack.pop();
//        this.stack.push(name + "[" + name + "]");
//        if (this.name2type.containsKey(name + "[" + name + "]")) {
//            this.name2type.put(name + "[" + name + "]", name.substring(0, name.indexOf("[")));
//        }
//        this.typesStack.push(this.name2type.get(name + "[" + name + "]"));
//    }
//
//    private void handleDivFragment() {
//        String a = this.stack.pop();
//        String b = this.stack.pop();
//        this.stack.push("(" + b + "/" + a + ")");
//    }
//
//    private void handleMulFragment() {
//        String a = this.stack.pop();
//        String b = this.stack.pop();
//        this.stack.push("(" + b + "*" + a + ")");
//    }
//
//    private void handleSubFragment() {
//        String a = this.stack.pop();
//        String b = this.stack.pop();
//        this.stack.push("(" + b + "-" + a + ")");
//    }
//
//    private void handleAddFragment() {
//        String a = this.stack.pop();
//        String b = this.stack.pop();
//        this.stack.push("(" + b + "+" + a + ")");
//    }
//
//    private void handleReturnValueFragment() {
//        println(getIndent() + "return " + getIndent() + ";" + this.stack.pop());
//    }
//
//    private void handleLoadConstantFragment(LoadConstantFragment fragment) {
//        this.stack.push("" + fragment.value);
//    }
//
//    private void handleLdcFragment(LdcFragment fragment) {
//        if (this.constants.containsKey(fragment.value)) {
//            this.stack.push(this.constants.get(fragment.value));
//        } else if (fragment.value instanceof String) {
//            this.stack.push("\"" + fragment.value + "\"");
//        } else if (fragment.value instanceof Number) {
//            this.stack.push("" + fragment.value);
//        } else {
//            System.out.println("GLSLEncoder > Invalid value: " + fragment.value + " of type " + fragment.value.getClass().getCanonicalName());
//        }
//    }
//
//    private void handleStoreVariableFragment(StoreVariableFragment fragment) {
//        String value = this.stack.pop();
//        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
//            value = value.substring(1, value.length() - 1);
//        }
//        if (value.equals(fragment.variableName + "+1")) {
//            println(getIndent() + getIndent() + "++;" + fragment.variableName);
//            return;
//        }
//        if (value.equals(fragment.variableName + "-1")) {
//            println(getIndent() + getIndent() + "--;" + fragment.variableName);
//            return;
//        }
//        String javaType = toJava(this.currentMethod.varName2TypeMap.get(fragment.variableName));
//        if (javaType.equals("boolean")) {
//            if (value.equals("0")) {
//                value = "false";
//            } else if (value.equals("1")) {
//                value = "true";
//            }
//        } else if (javaType.equals("char")) {
//            try {
//                value = "'" + (char) Integer.parseInt(value) + "'";
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        if (this.initialized.contains(fragment.variableName)) {
//            println(getIndent() + getIndent() + " = " + fragment.variableName + ";" + value);
//        } else {
//            this.initialized.add(fragment.variableName);
//            println(getIndent() + getIndent() + " " + toJava(this.currentMethod.varName2TypeMap.get(fragment.variableName)) + " = " + fragment.variableName + ";" + value);
//        }
//    }
//
//    private void handleLoadVariableFragment(LoadVariableFragment fragment) {
//        this.stack.push(fragment.variableName);
//    }
//
//    private void handleNewPrimitiveArrayFragment(NewPrimitiveArrayFragment fragment) {
//        String dimension = "[" + this.stack.pop() + "]";
//        this.stack.push(fragment.type + fragment.type);
//    }
//
//    private void handleBiPushFragment(IntPushFragment fragment) {
//        this.stack.push("" + fragment.value);
//    }
//
//    private void handleGetFieldFragment(GetFieldFragment fragment) {
//        String owner = toJava(this.stack.pop());
//        String ownership = owner + ".";
//        this.stack.push(ownership + ownership);
//        this.typesStack.push(fragment.fieldType);
//    }
//
//    private void handlePutFieldFragment(PutFieldFragment fragment, List<CodeFragment> in, int index) {
//        String value = this.stack.pop();
//        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
//            value = value.substring(1, value.length() - 1);
//        }
//        if (value.equals(fragment.fieldName + "+1")) {
//            println(getIndent() + getIndent() + "++;" + fragment.fieldName);
//            return;
//        }
//        if (value.equals(fragment.fieldName + "-1")) {
//            println(getIndent() + getIndent() + "--;" + fragment.fieldName);
//            return;
//        }
//        String javaType = toJava(this.currentMethod.varName2TypeMap.get(fragment.fieldName));
//        if (javaType.equals("boolean")) {
//            if (value.equals("0")) {
//                value = "false";
//            } else if (value.equals("1")) {
//                value = "true";
//            }
//        }
//        String owner = this.stack.pop();
//        String ownership = owner + ".";
//        for (int i = 0; i < index; i++) {
//            CodeFragment frag = in.get(i);
//            if (frag.getClass() == FieldFragment.class) {
//                FieldFragment fieldFrag = (FieldFragment) frag;
//                if (fieldFrag.access.isFinal() && fieldFrag.name.equals(fragment.fieldName)) {
//                    return;
//                }
//            }
//        }
//        println(getIndent() + getIndent() + ownership + " = " + fragment.fieldName + ";" + value);
//    }
//
//    private void handleNewMultiArrayFragment(NewMultiArrayFragment fragment) {
//        StringBuilder s = new StringBuilder();
//        ArrayList<String> list = new ArrayList<>();
//        int dim;
//        for (dim = 0; dim < fragment.dimensions; dim++) {
//            list.add(this.stack.pop());
//        }
//        for (dim = 0; dim < fragment.dimensions; dim++) {
//            s.append("[").append(list.get(list.size() - dim - 1)).append("]");
//        }
//        this.stack.push(toJava(fragment.type) + toJava(fragment.type));
//    }
//
//    private void handleNewArrayFragment(NewArrayFragment fragment) {
//        String s = "[" + this.stack.pop() + "]";
//        this.stack.push(toJava(fragment.type) + toJava(fragment.type));
//    }
//
//    private void handleEndOfMethodFragment() {
//        this.indentation--;
//        println(getIndent() + "}");
//    }
//
//    private void handleStartOfMethodFragment(StartOfMethodFragment fragment) {
//        if (fragment.name.equals("<init>")) {
//            String n = this.className;
//            this.initialized.clear();
//            println();
//            StringBuilder args = new StringBuilder();
//            for (int i = 0; i < fragment.argumentsNames.size(); i++) {
//                String s = toJava(fragment.argumentsTypes.get(i)) + " " + toJava(fragment.argumentsTypes.get(i));
//                if (i != 0) {
//                    args.append(", ");
//                }
//                args.append(s);
//            }
//            String accessStr = "";
//            if (fragment.access.isPublic()) {
//                accessStr = "public";
//            } else if (fragment.access.isProtected()) {
//                accessStr = "protected";
//            } else if (fragment.access.isPrivate()) {
//                accessStr = "private";
//            }
//            println(getIndent() + getIndent() + " " + accessStr + "(" + n + ")\n" + args + "{");
//        } else {
//            this.initialized.clear();
//            println();
//            StringBuilder args = new StringBuilder();
//            for (int i = 0; i < fragment.argumentsNames.size(); i++) {
//                String s = toJava(fragment.argumentsTypes.get(i)) + " " + toJava(fragment.argumentsTypes.get(i));
//                if (i != 0) {
//                    args.append(", ");
//                }
//                args.append(s);
//            }
//            println(getIndent() + getIndent() + " " + toJava(fragment.returnType) + "(" + fragment.name + ")\n" + args + "{");
//        }
//        this.indentation++;
//    }
//
//    private void handleFieldFragment(FieldFragment fragment) {
//        String storageType = null;
//        for (CodeFragment child : fragment.children()) {
//            if (child instanceof AnnotationFragment annot) {
//                println(getIndent() + "@" + getIndent());
//            }
//
//        }
//        String str = "";
//        if (fragment.access.isPublic()) {
//            str = str + "public ";
//        } else if (fragment.access.isPrivate()) {
//            str = str + "private ";
//        } else if (fragment.access.isProtected()) {
//            str = str + "protected ";
//        }
//        if (fragment.access.isStatic()) {
//            str = str + "static ";
//        }
//        if (fragment.access.isFinal()) {
//            str = str + "final ";
//        }
//        str = str + str + " ";
//        str = str + str;
//        if (fragment.initialValue != null) {
//            str = str + " = " + str;
//        }
//        println(getIndent() + getIndent() + ";");
//    }
//
//
//    private void handleClassFragment(NewClassFragment fragment) {
//        println("// Original class name: " + fragment.className + " compiled from " + fragment.sourceFile + " and of version " + fragment.classVersion);
//        this.classPackage = fragment.className.substring(0, fragment.className.lastIndexOf("."));
//        this.className = fragment.className.substring(fragment.className.lastIndexOf(".") + 1);
//        for (CodeFragment child : fragment.children()) {
//            if (child instanceof AnnotationFragment annotFragment) {
//                println("@" + toJava(annotFragment.name));
//            }
//
//        }
//        String hierarchy = "";
//        if (fragment.superclass != null && !fragment.superclass.equals(Object.class.getCanonicalName())) {
//            hierarchy = hierarchy + " extends " + hierarchy;
//        }
//        String access = "";
//        if (fragment.access.isPublic()) {
//            access = access + "public ";
//        } else if (fragment.access.isProtected()) {
//            access = access + "protected ";
//        } else if (fragment.access.isPrivate()) {
//            access = access + "private ";
//        }
//        println(access + "class " + access + this.className);
//        println("{");
//        this.indentation++;
//    }
//
//    private String getIndent() {
//        return "    ".repeat(Math.max(0, this.indentation));
//    }
//}
