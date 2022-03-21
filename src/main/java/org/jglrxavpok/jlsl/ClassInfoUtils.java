package org.jglrxavpok.jlsl;

import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;


public class ClassInfoUtils {
    private static final int[] methodAccessFlags = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 1024, 2048, 4096, 32768};


    public static Set<Integer> getMethodAccessFlags(MethodNode methodNode) {
        Set<Integer> accessFlags = new HashSet<>();
        int access = methodNode.access;

        for (int accessFlag : methodAccessFlags) {
            if ((access & accessFlag) != 0) {
                accessFlags.add(accessFlag);
            }
        }

        return accessFlags;
    }
}
