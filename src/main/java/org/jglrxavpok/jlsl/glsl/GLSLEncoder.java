package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.CodeEncoder;
import org.jglrxavpok.jlsl.JLSLException;
import org.jglrxavpok.jlsl.fragments.*;
import org.jglrxavpok.jlsl.glsl.fragments.StructFragment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Consumer;

public class GLSLEncoder extends CodeEncoder {
    private final List<String> extensions = new ArrayList<>();
    private final int glslversion;
    private final Stack<String> typesStack;
    private final Map<String, String> name2type;
    private final Map<Object, String> constants;
    private final Map<String, String> methodReplacements;
    private final List<String> initialized;
    private final List<String> loadedStructs = new ArrayList<>();
    private final Stack<CodeFragment.Data> waiting;
    private final Stack<String> newInstances;
    private final String structOwnerMethodSeparator;
    private final Map<String, String> translations = new HashMap<>();
    private final Map<String, String> conversionsToStructs = new HashMap<>();
    private int indentation;
    private NewClassFragment currentClass;
    private int currentLine;
    private Stack<String> stack;
    private StartOfMethodFragment currentMethod;
    private boolean convertNumbersToChars;
    private int currentRequestType;
    private Object requestData;
    private StructFragment currentStruct;
    private boolean allowedToPrint;
    private PrintWriter out;
    private List<CodeFragment.Data> in;

    private final Map<Class<CodeFragment.Data>, Consumer<CodeFragment.Data>> codeFragmentHandlers = new HashMap<>();

    public GLSLEncoder(int glslversion) {
        this.convertNumbersToChars = true;
        this.glslversion = glslversion;
        this.stack = new Stack<>();
        this.typesStack = new Stack<>();
        this.initialized = new ArrayList<>();
        this.name2type = new HashMap<>();
        this.constants = new HashMap<>();
        this.methodReplacements = new HashMap<>();
        this.waiting = new Stack<>();
        this.newInstances = new Stack<>();
        this.structOwnerMethodSeparator = "__";

        init();
    }

    public void init() {
        setGLSLTranslation("boolean", "bool");
        setGLSLTranslation("double", "float");

        setGLSLTranslation(Vec2.class.getCanonicalName(), "vec2");
        setGLSLTranslation(Vec3.class.getCanonicalName(), "vec3");
        setGLSLTranslation(Vec4.class.getCanonicalName(), "vec4");
        setGLSLTranslation(Mat2.class.getCanonicalName(), "mat2");
        setGLSLTranslation(Mat3.class.getCanonicalName(), "mat3");
        setGLSLTranslation(Mat4.class.getCanonicalName(), "mat4");
        setGLSLTranslation(Integer.class.getCanonicalName(), "int");

        setGLSLTranslation(Math.class.getCanonicalName(), "");
        setGLSLTranslation(Sampler2D.class.getCanonicalName(), "sampler2D");

        registerHandlers();
    }

    private <D extends CodeFragment.Data> void registerHandler(Class<D> dataType, Consumer<D> handler) {
        //noinspection unchecked
        codeFragmentHandlers.put((Class<CodeFragment.Data>) dataType, (Consumer<CodeFragment.Data>) handler);
    }

    private void registerHandlers() {
        registerHandler(NewArrayFragment.class, this::handleNewArrayFragment);
        registerHandler(StructFragment.class, this::handleStructFragment);
        registerHandler(CompareFragment.class, this::handleCompareFragment);
        registerHandler(NotEqualCheckFragment.class, this::handleNotEqualCheckFragment);
        registerHandler(EqualCheckFragment.class, this::handleEqualCheckFragment);
        registerHandler(NewInstanceFragment.class, this::handleNewInstanceFragment);
        registerHandler(DuplicateFragment.class, this::handleDuplicateFragment);
        registerHandler(ReturnFragment.class, this::handleReturnFragment);
        registerHandler(PopFragment.class, this::handlePopFragment);
        registerHandler(IfNotStatementFragment.class, this::handleIfNotStatementFragment);
        registerHandler(XorFragment.class, this::handleXorFragment);
        registerHandler(OrFragment.class, this::handleOrFragment);
        registerHandler(AndFragment.class, this::handleAndFragment);
        registerHandler(RightShiftFragment.class, this::handleRightShiftFragment);
        registerHandler(LeftShiftFragment.class, this::handleLeftShiftFragment);
        registerHandler(CastFragment.class, this::handleCastFragment);
        registerHandler(ModFragment.class, this::handleModFragment);
        registerHandler(MethodCallFragment.class, this::handleMethodCallFragment);
        registerHandler(ElseStatementFragment.class, this::handleElseStatementFragment);
        registerHandler(EndOfBlockFragment.class, this::handleEndOfBlockFragment);
        registerHandler(IfStatementFragment.class, this::handleIfStatementFragment);
        registerHandler(ArrayStoreFragment.class, this::handleArrayStoreFragment);
        registerHandler(ArrayOfArrayLoadFragment.class, this::handleArrayOfArrayLoadFragment);
        registerHandler(DivFragment.class, this::handleDivFragment);
        registerHandler(MulFragment.class, this::handleMulFragment);
        registerHandler(SubFragment.class, this::handleSubFragment);
        registerHandler(AddFragment.class, this::handleAddFragment);
        registerHandler(ReturnValueFragment.class, this::handleReturnValueFragment);
        registerHandler(LoadConstantFragment.class, this::handleLoadConstantFragment);
        registerHandler(LdcFragment.class, this::handleLdcFragment);
        registerHandler(StoreVariableFragment.class, this::handleStoreVariableFragment);
        registerHandler(LoadVariableFragment.class, this::handleLoadVariableFragment);
        registerHandler(NewPrimitiveArrayFragment.class, this::handleNewPrimitiveArrayFragment);
        registerHandler(IntPushFragment.class, this::handleBiPushFragment);
        registerHandler(GetFieldFragment.class, this::handleGetFieldFragment);
        registerHandler(PutFieldFragment.class, this::handlePutFieldFragment);
        registerHandler(NewMultiArrayFragment.class, this::handleNewMultiArrayFragment);
        registerHandler(NewArrayFragment.class, this::handleNewArrayFragment);
        registerHandler(LineNumberFragment.class, fragment -> this.currentLine = fragment.line());
        registerHandler(EndOfMethodFragment.class, this::handleEndOfMethodFragment);
        registerHandler(StartOfMethodFragment.class, this::handleStartOfMethodFragment);
        registerHandler(FieldFragment.class, this::handleFieldFragment);
        registerHandler(NewClassFragment.class, this::handleClassFragment);
    }

    public void addToStructConversion(String javaType, String structName) {
        this.conversionsToStructs.put(javaType, structName);
    }

    public boolean hasStructAttached(String javaType) {
        return this.conversionsToStructs.containsKey(javaType);
    }

    public void setGLSLTranslation(String javaType, String glslType) {
        this.translations.put(javaType, glslType);
    }

    public void removeGLSLTranslation(String javaType) {
        this.translations.remove(javaType);
    }

    private String toGLSL(String type) {
        if (type == null) {
            return "";
        }
        String copy = type;
        StringBuilder end = new StringBuilder();
        while (copy.contains("[]")) {
            copy = copy.replaceFirst("\\[]", "");
            end.append("[]");
        }
        type = copy;
        if (this.conversionsToStructs.containsKey(type)) {
            return this.conversionsToStructs.get(type) + this.conversionsToStructs.get(type);
        }
        if (this.translations.containsKey(type)) {
            return this.translations.get(type) + this.translations.get(type);
        }
        // Why did this do type + type before?
        return type;
    }

    private String getEndOfLine(int currentLine) {
        String s;


        s = " //Line #" + currentLine;

        return s;
    }

    public void convertNumbersToChar(boolean convert) {
        this.convertNumbersToChars = convert;
    }

    public void createSourceCode(List<CodeFragment.Data> in, PrintWriter out) {
        this.in = in;
        this.out = out;
        interpret(in);
        this.allowedToPrint = true;
        println("#version " + this.glslversion);
        for (CodeFragment.Data fragment : in) {
            this.allowedToPrint = true;
            if (!this.waiting.isEmpty()) {
                handleCodeFragment(this.waiting.pop());
            }
            handleCodeFragment(fragment);
        }
        out.flush();
    }

    private void interpret(List<CodeFragment.Data> in) {
        Stack<String> copy = this.stack;
        this.stack = new Stack<>();
        StartOfMethodFragment currentMethod = null;
        PrintWriter nullPrinter = new PrintWriter(new StringWriter());
        for (int i = 0; i < in.size(); i++) {
            boolean dontHandle = false;
            CodeFragment.Data fragment = in.get(i);
            if (fragment.getClass() == StartOfMethodFragment.class) {
                currentMethod = (StartOfMethodFragment) fragment;
            } else if (fragment.getClass() == FieldFragment.class) {
                FieldFragment fieldFrag = (FieldFragment) fragment;
                if (hasStructAttached(fieldFrag.type()) && !this.loadedStructs.contains(toGLSL(fieldFrag.type()))) {
                    this.loadedStructs.add(toGLSL(fieldFrag.type()));
                    String name = this.conversionsToStructs.get(fieldFrag.type());
                    StructFragment struct = new StructFragment(name, new StartOfMethodFragment[0], new FieldFragment[0]);
                    this.currentRequestType = 1;
                    this.requestData = struct;
                    String s = "/" + fieldFrag.type().replace(".", "/") + ".class";
                    this.context.requestAnalysisForEncoder(GLSLEncoder.class.getResourceAsStream(s));
                    in.add(i, struct);
                    this.currentRequestType = 0;
                    i--;
                }
            } else if (fragment.getClass() == StoreVariableFragment.class) {
                StoreVariableFragment storeFrag = (StoreVariableFragment) fragment;
                String type = storeFrag.variableType();
                if (hasStructAttached(type) && !this.loadedStructs.contains(toGLSL(type))) {
                    this.loadedStructs.add(toGLSL(type));
                    String name = this.conversionsToStructs.get(type);
                    StructFragment struct = new StructFragment(name, new StartOfMethodFragment[0], new FieldFragment[0]);
                    this.currentRequestType = 1;
                    this.requestData = struct;
                    String s = "/" + type.replace(".", "/") + ".class";
                    this.context.requestAnalysisForEncoder(GLSLEncoder.class.getResourceAsStream(s));
                    in.add(i, struct);
                    this.currentRequestType = 0;
                    i--;
                }
            } else if (fragment.getClass() == PutFieldFragment.class) {
                PutFieldFragment storeFrag = (PutFieldFragment) fragment;
                if (currentMethod != null && currentMethod.name().equals("<init>")) {
                    for (int i1 = 0; i1 < in.size(); i1++) {
                        CodeFragment.Data fragment1 = in.get(i1);
                        if (fragment1 instanceof FieldFragment field) {
                            if (field.name().equals(storeFrag.fieldName()) &&
                                    field.type().equals(storeFrag.fieldType()) &&
                                    (!field.access().isFinal() || field.initialValue() == null)
                            ) {
                                Object initialValue = this.stack.peek();
                                FieldFragment newField = new FieldFragment(field.name(), field.type(), field.access(), field.annotations(), initialValue);
                                in.set(i1, newField);
                                dontHandle = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!dontHandle) {
                this.out = nullPrinter;
                this.allowedToPrint = true;
                if (!this.waiting.isEmpty()) {
                    handleCodeFragment(this.waiting.pop());
                }
                handleCodeFragment(fragment);
            }
        }

        this.waiting.clear();
        this.currentLine = 0;
        this.indentation = 0;
        this.initialized.clear();
        this.name2type.clear();
        this.currentClass = null;
        this.typesStack.clear();
        this.extensions.clear();
        this.constants.clear();
        this.stack = copy;
    }

    private int countChar(String str, char c) {
        int nbr = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                nbr++;
            }
        }
        return nbr;
    }

    private void println() {
        println("");
    }

    private void println(String s) {
        if (this.allowedToPrint) {
            this.out.println(s);
        }
    }

    private void handleCodeFragment(CodeFragment.Data data) {
        System.out.println("Handling data: " + data);
        codeFragmentHandlers.get(data.getClass()).accept(data);
    }

    private void handleCompareFragment(CompareFragment data) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push(left + " " + left + " " + (data.inferior() ? "<" : ">"));
    }

    private void handleNotEqualCheckFragment(NotEqualCheckFragment data) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push("(" + left + " != " + right + ")");
    }

    private void handleEqualCheckFragment(EqualCheckFragment data) {
        String right = this.stack.pop();
        String left = this.stack.pop();
        this.stack.push("(" + left + " == " + right + ")");
    }

    private void handleNewInstanceFragment(NewInstanceFragment data) {
        this.newInstances.push(data.type());
    }

    private void handleStructFragment(StructFragment data) {
        this.currentStruct = data;
        println(getIndent() + "struct " + getIndent());
        println(getIndent() + "{");
        this.indentation++;
        for (FieldFragment field : data.fields()) {
            println(getIndent() + field.type() + " " + field.name() + ";");
        }
        this.indentation--;
        println(getIndent() + "};");

        String instanceName = ("" + data.name().charAt(0)).toLowerCase() + ("" + data.name().charAt(0)).toLowerCase() + "Instance";

        for (int i = 0; i < data.methods().length; i++) {
            StartOfMethodFragment method = data.methods()[i];

            String oldName = method.name();
            method.varNameMap().put(0, instanceName);
            boolean isConstructor = false;
            if (method.name().equals("<init>") || method.name().equals(data.name() + structOwnerMethodSeparator + "new")) {
                data.methods()[i] = method.modify(modifier -> modifier.name("new").returnType(data.name()));
                isConstructor = true;
            } else if (!method.argumentsNames().contains(instanceName)) {
                data.methods()[i] = method.modify(modifier -> modifier
                        .argumentsNames(argNames -> argNames.add(0, instanceName))
                        .argumentsNames(argNames -> argNames.add(0, data.name()))
                );
            }
            if (!method.name().startsWith(data.name() + structOwnerMethodSeparator)) {
                data.methods()[i] = method.modify(modifier ->
                        modifier.name(data.name() + structOwnerMethodSeparator + method.name()));
            }
            String key = toGLSL(method.owner()) + "." + oldName;
            this.methodReplacements.put(key, method.name());

            System.out.println("GLSLEncoder > Mapped " + key + " to " + currentMethod.name());
        }
    }

    private void handleDuplicateFragment(DuplicateFragment data) {
        if (!this.newInstances.isEmpty()) {
            return;
        }
        if (data.waitCount() > 0) {
            this.waiting.add(new DuplicateFragment(data.waitCount() - 1));
        } else {
            String a = this.stack.pop();
            this.stack.push(a);
            this.stack.push(a);
        }
    }

    private void handleReturnFragment(ReturnFragment data) {
        println(getIndent() + "return;" + getIndent());
    }

    private void handlePopFragment(PopFragment data) {
        println(getIndent() + getIndent() + ";" + this.stack.pop());
    }

    private void handleIfNotStatementFragment(IfNotStatementFragment data) {
        String condition = this.stack.pop();
        println(getIndent() + "if(!" + getIndent() + ")" + condition);
        println(getIndent() + "{");
        this.indentation++;
    }

    private void handleXorFragment(XorFragment data) {
        String a = this.stack.pop();
        String b = this.stack.pop();
        this.stack.push("(" + a + " || " + b + ")");
    }

    private void handleOrFragment(OrFragment data) {
        String b = this.stack.pop();
        String a = this.stack.pop();
        this.stack.push("(" + a + " " + (data.isDouble() ? "||" : "|") + " " + b + ")");
    }

    private void handleAndFragment(AndFragment data) {
        String b = this.stack.pop();
        String a = this.stack.pop();
        this.stack.push("(" + a + " " + (data.isDouble() ? "&&" : "&") + " " + b + ")");
    }

    private void handleRightShiftFragment(RightShiftFragment data) {
        String b = this.stack.pop();
        String a = this.stack.pop();
        this.stack.push(a + ">>" + a + (!data.signed() ? ">" : ""));
    }

    private void handleLeftShiftFragment(LeftShiftFragment data) {
        String b = this.stack.pop();
        String a = this.stack.pop();
        this.stack.push(a + "<<" + a + (!data.signed() ? "<" : ""));
    }

    private void handleCastFragment(CastFragment data) {
        String toCast = this.stack.pop();

        String previousType;
        if (toCast.startsWith("(")) {
            previousType = toCast.substring(1, toCast.indexOf(")") - 1);
        } else {
            previousType = toGLSL(this.currentMethod.varName2TypeMap().get(toCast));
        }
        if (previousType.equals(toGLSL(data.to()))) {

            System.out.println("GLSLEncoder > Cancelling cast for " + toCast);
        } else {

            this.stack.push("(" + toGLSL(data.to()) + ")" + toCast);
        }
    }

    private void handleModFragment(ModFragment data) {
        String a = this.stack.pop();
        String b = this.stack.pop();
        this.stack.push("mod(" + b + ", " + a + ")");
    }

    private void handleMethodCallFragment(MethodCallFragment data) {

        StringBuilder sb = new StringBuilder();

        String name = data.methodName();
        if (data.methodName().equals("<init>")) {
            // TODO: handle constructor
        }

        this.stack.push("METHOD_CALL_" + data.methodName());
        if (true) {
            return;
        }
        String s = "";
        String n = data.methodName();
        boolean isConstructor = false;
        if (n.equals("<init>")) {
            n = toGLSL(data.methodOwner());
            if (!this.newInstances.isEmpty()) {
                this.newInstances.pop();
            }
        }
        String key = data.methodName();
        toGLSL(data.methodOwner());
        if (this.methodReplacements.containsKey(key)) {
            n = this.methodReplacements.get(key);
            System.out.println("GLSLEncoder > Replacing " + key + " by " + n);
        }

        if (data.invokeType() == MethodCallFragment.InvokeTypes.SPECIAL &&
                this.currentMethod.name().equals("<init>") &&
                data.methodOwner().equals(this.currentClass.superclass())) {
            this.allowedToPrint = false;
        }

        s = s + s + "(";
        ArrayList<String> args = new ArrayList<>();

        for (String type : data.argumentsTypes()) {
            String arg = this.stack.pop();
            if (arg.startsWith("(") && arg.endsWith(")") && countChar(arg, '(') == countChar(arg, ')')) {
                arg = arg.substring(1, arg.length() - 1);
            }
            args.add(arg);
        }
        StringBuilder argsStr = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            argsStr.append(args.get(args.size() - 1 - i));
            if (i != 0) {
                argsStr.append(", ");
            }
        }
        boolean ownerBefore = false;
        boolean parenthesis = true;
        int ownerPosition = 0;
        boolean actsAsField = false;
        for (AnnotationFragment annotation : data.annotations()) {
            if (annotation.name().equals(GLSL.Substitute.class.getCanonicalName())) {
                if (!annotation.values().get("value").equals("$")) {
                    n = (String) annotation.values().get("value");
                }
                if (annotation.values().containsKey("ownerBefore")) {
                    ownerBefore = (Boolean) annotation.values().get("ownerBefore");
                }
                if (annotation.values().containsKey("ownerPosition")) {
                    ownerPosition = (Integer) annotation.values().get("ownerPosition");
                }
                if (annotation.values().containsKey("actsAsField")) {
                    actsAsField = (Boolean) annotation.values().get("actsAsField");
                }
                if (annotation.values().containsKey("usesParenthesis")) {
                    parenthesis = (Boolean) annotation.values().get("usesParenthesis");
                }
            }
        }
        if (data.invokeType() == MethodCallFragment.InvokeTypes.VIRTUAL) {
            String owner = this.stack.pop();
            if (owner.equals(this.currentClass.className()) || owner.equals("this")) {
                owner = null;
            } else if (owner.startsWith("(") && owner.endsWith(")") && countChar(owner, '(') == countChar(owner, ')')) {
                owner = owner.substring(1, owner.length() - 1);
            }

            if (!ownerBefore) {
                if (actsAsField) {
                    if (n.length() >= 1) {
                        s = ((owner != null) ? owner : "") + "." + ((owner != null) ? owner : "");
                    } else {
                        s = (owner != null) ? owner : "";
                    }
                    if (argsStr.length() > 0) {
                        s = s + " = " + s;
                    }
                } else {
                    s = n + (parenthesis ? "(" : "") + ((owner != null) ? (owner + owner) : "") + argsStr;
                }
            } else {
                s = ((owner != null) ? owner : "") + n + (parenthesis ? "(" : "") + argsStr;
            }
            if (data.returnType().equals("void")) {
                println(getIndent() + getIndent() + ";" + s);
            } else {
                this.stack.push("(" + s + ")");
            }
        } else if (data.invokeType() == MethodCallFragment.InvokeTypes.STATIC) {
            String ownership = "";
            String owner = toGLSL(data.methodOwner());
            if (!owner.trim().equals("") && !owner.equals("null")) {
                ownership = owner + owner;
            }
            this.stack.push(ownership + ownership + n + (parenthesis ? "(" : "") + argsStr);
        } else {
            this.stack.push(n + n + (parenthesis ? "(" : "") + argsStr);
        }
    }

    private void handleElseStatementFragment(ElseStatementFragment data) {
        println(getIndent() + "else" + getIndent());
        println(getIndent() + "{");
        this.indentation++;
    }

    private void handleEndOfBlockFragment(EndOfBlockFragment data) {
        this.indentation--;
        println(getIndent() + "}");
    }

    private void handleIfStatementFragment(IfStatementFragment data) {
        String condition = this.stack.pop();
        println(getIndent() + "if(" + getIndent() + ")" + condition);
        println(getIndent() + "{");
        this.indentation++;
    }

    private void handleArrayStoreFragment(ArrayStoreFragment data) {
        String result = "";
        String toAdd = "";
        for (int i = 0; i < 2; i++) {
            String copy = this.typesStack.pop();
            int dimensions = 0;
            if (copy != null) {
                while (copy.contains("[]")) {
                    copy = copy.substring(copy.indexOf("[]") + 2);
                    dimensions++;
                }
            }
            String val = this.stack.pop();
            StringBuilder arrayIndex = new StringBuilder();
            for (int dim = 0; dim < dimensions; dim++) {
                arrayIndex.insert(0, "[" + this.stack.pop() + "]");
            }
            String name = this.stack.pop();
            if (i == 1) {
                result = val + val + " = " + toAdd;
            } else if (i == 0) {
                result = val + val;
                toAdd = "[" + name + "]";
            }
        }
        println(getIndent() + getIndent() + ";" + result);
    }

    private void handleArrayOfArrayLoadFragment(ArrayOfArrayLoadFragment data) {
        String value = this.stack.pop();
        String name = this.stack.pop();
        this.stack.push(name + "[" + name + "]");
        if (this.name2type.containsKey(name + "[" + name + "]")) {
            this.name2type.put(name + "[" + name + "]", name.substring(0, name.indexOf("[")));
        }
        this.typesStack.push(this.name2type.get(name + "[" + name + "]"));
    }

    private void handleDivFragment(DivFragment data) {
        String a = this.stack.pop();
        String b = this.stack.pop();
        this.stack.push(b + "/" + b);
    }

    private void handleMulFragment(MulFragment data) {
        String a = this.stack.pop();
        String b = this.stack.pop();
        this.stack.push(b + "*" + b);
    }

    private void handleSubFragment(SubFragment data) {
        String a = this.stack.pop();
        String b = this.stack.pop();
        this.stack.push(b + "-" + b);
    }

    private void handleAddFragment(AddFragment data) {
        String a = this.stack.pop();
        String b = this.stack.pop();
        this.stack.push(b + "+" + b);
    }

    private void handleReturnValueFragment(ReturnValueFragment data) {
        println(getIndent() + "return " + getIndent() + ";" + this.stack.pop());
    }

    private void handleLoadConstantFragment(LoadConstantFragment data) {
        this.stack.push("" + data.value());
    }

    private void handleLdcFragment(LdcFragment data) {
        if (this.constants.containsKey(data.value())) {
            this.stack.push(this.constants.get(data.value()));
        } else if (data.value() instanceof String) {
            this.stack.push("\"" + data.value() + "\"");
        } else if (data.value() instanceof Number) {
            this.stack.push("" + data.value());
        } else {
            System.out.println("GLSLEncoder > Invalid value: " + data.value() + " of type " + data.value().getClass().getCanonicalName());
        }
    }

    private void handleStoreVariableFragment(StoreVariableFragment data) {
        String value = this.stack.pop();
        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.equals(data.variableName() + "+1")) {
            println(getIndent() + getIndent() + "++;" + data.variableName());
            return;
        }
        if (value.equals(data.variableName() + "-1")) {
            println(getIndent() + getIndent() + "--;" + data.variableName());
            return;
        }
        String glslType = toGLSL(this.currentMethod.varName2TypeMap().get(data.variableName()));
        if (glslType.equals("bool")) {
            if (value.equals("0")) {
                value = "false";
            } else if (value.equals("1")) {
                value = "true";
            }
        } else if (glslType.equals("char") &&
                this.convertNumbersToChars) {
            try {
                value = "'" + (char) Integer.parseInt(value) + "'";
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }

        if (this.initialized.contains(data.variableName())) {
            println(getIndent() + getIndent() + " = " + data.variableName() + ";" + value);
        } else {
            this.initialized.add(data.variableName());
            println(getIndent() + getIndent() + " " + toGLSL(this.currentMethod.varName2TypeMap().get(data.variableName())) + " = " + data.variableName() + ";" + value);
        }
    }

    private void handleLoadVariableFragment(LoadVariableFragment data) {
        this.stack.push(data.variableName());
        System.out.println(data.variableName());
    }

    private void handleNewPrimitiveArrayFragment(NewPrimitiveArrayFragment data) {
        String dimension = "[" + this.stack.pop() + "]";
        this.stack.push(data.type() + data.type());
    }

    private void handleBiPushFragment(IntPushFragment data) {
        this.stack.push("" + data.value());
    }

    private void handleGetFieldFragment(GetFieldFragment data) {
        String owner = toGLSL(this.stack.pop());
        String ownership = owner + ".";
        if (owner.equals("this")) {
            ownership = "";
        }
        this.stack.push(ownership + ownership);
        this.typesStack.push(data.fieldType());
    }

    private void handlePutFieldFragment(PutFieldFragment data) {
        String value = this.stack.pop();
        if (value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')')) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.equals(data.fieldName() + "+1")) {
            println(getIndent() + getIndent() + "++;" + data.fieldName());
            return;
        }
        if (value.equals(data.fieldName() + "-1")) {
            println(getIndent() + getIndent() + "--;" + data.fieldName());
            return;
        }
        String glslType = toGLSL(this.currentMethod.varName2TypeMap().get(data.fieldName()));
        if (glslType.equals("bool")) {
            if (value.equals("0")) {
                value = "false";
            } else if (value.equals("1")) {
                value = "true";
            }
        }
        String owner = this.stack.pop();
        String ownership = owner + ".";
        if (owner.equals("this")) {
            ownership = "";
        }
        println(getIndent() + getIndent() + ownership + " = " + data.fieldName() + ";" + value);
    }

    private void handleNewMultiArrayFragment(NewMultiArrayFragment data) {
        StringBuilder s = new StringBuilder();
        ArrayList<String> list = new ArrayList<>();
        int dim;
        for (dim = 0; dim < data.dimensions(); dim++) {
            list.add(this.stack.pop());
        }
        for (dim = 0; dim < data.dimensions(); dim++) {
            s.append("[").append(list.get(list.size() - dim - 1)).append("]");
        }
        this.stack.push(toGLSL(data.type()) + toGLSL(data.type()));
    }

    private void handleNewArrayFragment(NewArrayFragment data) {
        String s = "[" + this.stack.pop() + "]";
        this.stack.push(toGLSL(data.type()) + toGLSL(data.type()));
    }

    private void handleEndOfMethodFragment(EndOfMethodFragment data) {
        if (data.name().equals("<init>")) {
            return;
        }
        println("}");
        this.indentation--;
    }

    private void handleStartOfMethodFragment(StartOfMethodFragment data) {
        if (data.name().equals("<init>")) {
            return;
        }
        this.initialized.clear();
        println();
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < data.argumentsNames().size(); i++) {
            String s = toGLSL(data.argumentsTypes().get(i)) + " " + toGLSL(data.argumentsTypes().get(i));
            if (i != 0) {
                args.append(", ");
            }
            args.append(s);
        }
        println(toGLSL(data.returnType()) + " " + "(" + data.name() + ")\n{");
        this.indentation++;
    }

    private void handleFieldFragment(FieldFragment data) {
        String storageType = null;
        for (AnnotationFragment annotation : data.annotations()) {
            if (annotation.name().equals(GLSL.Uniform.class.getCanonicalName())) {
                storageType = "uniform";
                continue;
            }
            if (annotation.name().equals(GLSL.Attribute.class.getCanonicalName())) {
                storageType = "attribute";
                if (this.currentClass.superclass().equals(FragmentShaderEnvironment.class.getCanonicalName()))
                    throw new JLSLException("Attributes are not allowed in data shaders");
                continue;
            }
            if (annotation.name().equals(GLSL.In.class.getCanonicalName())) {
                storageType = "in";
                continue;
            }
            if (annotation.name().equals(GLSL.Out.class.getCanonicalName())) {
                storageType = "out";
                continue;
            }
            if (annotation.name().equals(GLSL.Varying.class.getCanonicalName())) {
                storageType = "varying";
                continue;
            }
            if (annotation.name().equals(GLSL.Layout.class.getCanonicalName())) {
                int location = (Integer) annotation.values().get("location");

                if (this.glslversion > 430 || this.extensions.contains("GL_ARB_explicit_uniform_location")) {
                    out.print("layout(location = " + location + ") ");
                }
            }
        }
        if (storageType == null) {
            storageType = "uniform";
        }
        if (data.access().isFinal()) {
            if (data.access().isStatic()) {
                println("#define " + data.name() + " " + data.initialValue());
            } else {
                storageType = "const";
                println(storageType + " " + storageType + " " + toGLSL(data.type()) + " = " + data.name() + ";");
            }
            this.constants.put(data.initialValue(), data.name());
        } else if (data.initialValue() != null) {
            println(storageType + " " + storageType + " " + toGLSL(data.type()) + " = " + data.name() + ";");
        } else {
            println(storageType + " " + storageType + " " + toGLSL(data.type()) + ";");
        }
    }

    private void handleClassFragment(NewClassFragment data) {
        println("// Original class name: " + data.className() + " compiled from " + data.sourceFile() + " and of version " + data.classVersion());
        for (AnnotationFragment annotation : data.annotations()) {
            println();
            if (annotation.name().equals(GLSL.Extensions.class.getCanonicalName())) {
                //noinspection unchecked
                List<String> values = (List<String>) annotation.values().get("value");
                for (String extension : values) {
                    println("#extension " + extension + " : enable" + getEndOfLine(this.currentLine));
                }
            }

            if (annotation.name().equals(GLSL.Notes.class.getCanonicalName())) {
                //noinspection unchecked
                List<String> values = (List<String>) annotation.values().get("value");
                for (String note : values) {
                    println(note);
                }
            }
        }
    }

    private String getIndent() {
        return "    ".repeat(Math.max(0, this.indentation));
    }
}
