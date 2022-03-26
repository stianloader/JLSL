package org.jglrxavpok.jlsl.filters;

import org.jetbrains.annotations.NotNull;
import org.junit.platform.commons.util.StringUtils;

import static org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ObfuscationFilter implements CodeFilter<Root, Root> {

    private final Map<Method, String> methodsOld2New = new HashMap<>();
    private final Set<String> usedNames = new HashSet<>();

    @Override
    public @NotNull Root filter(@NotNull Root input) {
        List<TopLevelNode> nodes = new ArrayList<>(input.nodes());
        for (int i = 0; i < nodes.size(); i++) {
            TopLevelNode node = nodes.get(i);
            nodes.set(i, obfuscate(node));
        }
        return new Root(input.version(), nodes);
    }

    private <N extends Node> N obfuscate(@NotNull N node) {
        AtomicReference<N> result = new AtomicReference<>(node);
        process(node, result::set);
        return result.get();
    }

    private <N extends Node> void process(N node, Consumer<N> update) {
        if (node instanceof Method method) {
            String newName = methodsOld2New.computeIfAbsent(method, (ignored) -> getNewName());
            Method newMethod = new Method(method.returnType(), newName, method.parameters(), method.body());
            //noinspection unchecked
            update.accept((N) newMethod);
        }
    }

    private String getNewName() {
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
        return name;
    }
}
