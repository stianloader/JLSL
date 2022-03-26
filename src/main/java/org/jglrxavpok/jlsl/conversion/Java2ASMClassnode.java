package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public enum Java2ASMClassnode implements JavaDecoder<ClassNode> {
    INSTANCE;

    @Override
    public @NotNull ClassNode decode(@NotNull ClassReader reader) {
        synchronized (Java2ASMClassnode.INSTANCE) {
            ClassNode clazz = new ClassNode();
            reader.accept(clazz, 0);
            return clazz;
        }
    }
}
