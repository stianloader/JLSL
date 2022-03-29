package org.jglrxavpok.jlsl.filters;

import org.jetbrains.annotations.NotNull;

import static org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ObfuscationFilter implements CodeFilter<Root, Root> {

    private final Map<String, String> old2New = new HashMap<>();
    private final Set<String> usedNames = new HashSet<>();

    private final Map<Class<?>, Processor<?>> processors = new HashMap<>();


    public ObfuscationFilter() {
        registerProcessor(Method.class, this::process);
        registerProcessor(Value.MethodCall.class, this::process);
        registerProcessor(Body.class, this::process);
        registerProcessor(Value.Reference.Variable.class, this::process);
        registerProcessor(Value.Reference.Index.class, this::process);
        registerProcessor(Field.class, this::process);
        registerProcessor(Statement.DeclareVariable.class, this::process);
        registerProcessor(Statement.If.class, this::process);
        registerProcessor(Statement.UpdateVariable.class, Processor.PASS_THROUGH);
        registerProcessor(Value.Constant.class, Processor.PASS_THROUGH);
        registerProcessor(Statement.Return.class, Processor.PASS_THROUGH);
        registerProcessor(Method.Parameter.class, this::process);
        registerProcessor(Statement.Condition.GreaterThanOrEqualTo.class, this::process);
        registerProcessor(Statement.Condition.LessThanOrEqualTo.class, this::process);
        old2New.put("this", "this");
    }

    private <N extends Node> void registerProcessor(Class<N> clazz, Processor<? super N> processor) {
        processors.put(clazz, processor);
    }

    @Override
    public @NotNull Root filter(@NotNull Root input) {
        List<TopLevelNode> nodes = new ArrayList<>(input.nodes());
        for (int i = 0; i < nodes.size(); i++) {
            TopLevelNode node = nodes.get(i);
            nodes.set(i, obfuscate(node));
        }
        return new Root(input.version(), nodes);
    }

    private final Set<Node> visited = new HashSet<>();

    private <N extends Node> N obfuscate(@NotNull N node) {
        if (visited.contains(node)) {
            return node;
        }
        visited.add(node);
        AtomicReference<N> result = new AtomicReference<>(node);
        Class<?> clazz = node.getClass();
        //noinspection unchecked
        Processor<N> processor = (Processor<N>) processors.get(clazz);
        if (processor != null) {
            processor.process(node, result::set);
        } else {
            System.out.println("Missing obfuscation processor for " + clazz.getCanonicalName().replace(clazz.getPackageName() + ".", ""));
        }
        return result.get();
    }

    private <N extends Node> @NotNull List<N> obfuscateAll(@NotNull List<N> nodes) {
        List<N> result = new ArrayList<>(nodes.size());
        for (N node : nodes) {
            result.add(obfuscate(node));
        }
        return result;
    }

    private String getNewName(String old) {
        if (old2New.containsKey(old)) {
            return old2New.get(old);
        }
        if (usedNames.contains(old)) {
            return old;
        }
        // Create a random name that is not used yet
        String name;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                char c = (char) (Math.random() * 26 + 'a');
                sb.append(c);
            }
            name = sb.toString();
        } while (usedNames.contains(name));
        usedNames.add(name);
        old2New.put(old, name);
        return name;
    }

    private void process(Value.MethodCall methodCall, Consumer<Value.MethodCall> update) {
        Value callingField = methodCall.callingField();
        if (callingField != null) {
            callingField = obfuscate(callingField);
        }
        List<? extends Value> parameters = obfuscateAll(methodCall.parameters());
        update.accept(new Value.MethodCall(callingField, methodCall.listened(), parameters));
    }

    private void process(Method method, Consumer<Method> update) {
        String newName = getNewName(method.name());

        List<Method.Parameter> params = obfuscateAll(method.parameters());
        Body body = obfuscate(method.body());

        Method newMethod = new Method(method.returnType(), newName, params, body);
        update.accept(newMethod);
    }

    private void process(@NotNull Body body, @NotNull Consumer<Body> update) {
        List<Statement> statements = obfuscateAll(body.statements());
        update.accept(new Body(statements));
    }

    private void process(@NotNull Value.Reference.Variable variable, @NotNull Consumer<Value.Reference.Variable> update) {
        String newName = getNewName(variable.reference());
        update.accept(new Value.Reference.Variable(newName));
    }
    private void process(@NotNull Field field, @NotNull Consumer<Field> update) {
        String newName = getNewName(field.name());
        update.accept(new Field(field.fieldType(), newName, field.valueType()));
    }

    private void process(@NotNull Value.Reference.Index index, @NotNull Consumer<Value.Reference.Index> update) {
        String newIndex = getNewName(index.index());
        Value previous = obfuscate(index.previous());
        update.accept(new Value.Reference.Index(previous, newIndex));
    }

    private void process(@NotNull Statement.DeclareVariable declareVariable, @NotNull Consumer<Statement.DeclareVariable> update) {
        Type type = obfuscate(declareVariable.type());
        String newName = getNewName(declareVariable.name());
        Value value = declareVariable.value() == null ? null : obfuscate(declareVariable.value());
        Statement.DeclareVariable newDeclareVariable = new Statement.DeclareVariable(type, newName, value);
        update.accept(newDeclareVariable);
    }

    private void process(@NotNull Statement.If ifStatement, @NotNull Consumer<Statement.If> update) {
        Statement.Condition condition = obfuscate(ifStatement.condition());
        Body body = obfuscate(ifStatement.body());
        Body elseBody = ifStatement.elseBody() == null ? null : obfuscate(ifStatement.elseBody());
        Statement.If newIfStatement = new Statement.If(condition, body, elseBody);
        update.accept(newIfStatement);
    }

    private void process(Method.Parameter parameter, Consumer<Method.Parameter> update) {
        String newName = getNewName(parameter.name());
        Method.Parameter newParameter = new Method.Parameter(parameter.type(), newName);
        update.accept(newParameter);
    }

    private void process(Statement.Condition.GreaterThanOrEqualTo greaterThanOrEqualTo, Consumer<Statement.Condition.GreaterThanOrEqualTo> update) {
        Value left = obfuscate(greaterThanOrEqualTo.left());
        Value right = obfuscate(greaterThanOrEqualTo.right());
        Statement.Condition.GreaterThanOrEqualTo newGreaterThanOrEqualTo = new Statement.Condition.GreaterThanOrEqualTo(left, right);
        update.accept(newGreaterThanOrEqualTo);
    }

    private void process(Statement.Condition.LessThanOrEqualTo lessThanOrEqualTo, Consumer<Statement.Condition.LessThanOrEqualTo> update) {
        Value left = obfuscate(lessThanOrEqualTo.left());
        Value right = obfuscate(lessThanOrEqualTo.right());
        Statement.Condition.LessThanOrEqualTo newLessThanOrEqualTo = new Statement.Condition.LessThanOrEqualTo(left, right);
        update.accept(newLessThanOrEqualTo);
    }

    private interface Processor<N extends Node> {
        @NotNull Processor<Node> PASS_THROUGH = (node, update) -> update.accept(node);
        void process(@NotNull N node, @NotNull Consumer<N> update);
    }
}
