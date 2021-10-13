package org.jglrxavpok.jlsl.fragments;

import org.objectweb.asm.Opcodes;

public class AccessPolicy implements Opcodes {

    private final boolean isPublic;
    private final boolean isProtected;
    private final boolean isPrivate;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final boolean isFinal;

    public AccessPolicy(final int access) {
        isPrivate = hasModifier(access, ACC_PRIVATE);
        isProtected = hasModifier(access, ACC_PROTECTED);
        isPublic = hasModifier(access, ACC_PUBLIC);
        isStatic = hasModifier(access, ACC_STATIC);
        isAbstract = hasModifier(access, ACC_ABSTRACT);
        isFinal = hasModifier(access, ACC_FINAL);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isStatic() {
        return isStatic;
    }

    private boolean hasModifier(final int i, final int modifier) {
        return (i | modifier) == i;
    }
}
