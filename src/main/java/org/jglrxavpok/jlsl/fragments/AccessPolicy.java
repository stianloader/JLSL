package org.jglrxavpok.jlsl.fragments;

import org.objectweb.asm.Opcodes;

public class AccessPolicy
        implements Opcodes {
    private final boolean isPublic;
    private final boolean isProtected;
    private final boolean isPrivate;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final boolean isFinal;

    public AccessPolicy(int access) {
        this.isPublic = hasModifier(access, 1);
        this.isPrivate = hasModifier(access, 2);
        this.isProtected = hasModifier(access, 4);
        this.isStatic = hasModifier(access, 8);
        this.isFinal = hasModifier(access, 16);
        this.isAbstract = hasModifier(access, 1024);
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public boolean isFinal() {
        return this.isFinal;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    public boolean isPrivate() {
        return this.isPrivate;
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    private boolean hasModifier(int i, int modifier) {
        return ((i | modifier) == i);
    }
}
