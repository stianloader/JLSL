package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public interface JavaDecoder<R> {
    @NotNull R decode(@NotNull ClassReader reader);

    default @NotNull R decode(@NotNull Class<?> clazz) throws IOException {
        String classResourceName = "/" + clazz.getName().replace('.', '/') + ".class";
        InputStream resource = clazz.getResourceAsStream(classResourceName);
        Objects.requireNonNull(resource, "Resource: " + classResourceName + " not found in classpath");
        return decode(new ClassReader(resource));
    }

    default @NotNull R decode(@NotNull String className) throws IOException, ClassNotFoundException {
        return decode(Class.forName(className));
    }
}
