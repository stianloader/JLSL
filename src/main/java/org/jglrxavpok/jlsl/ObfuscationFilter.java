package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.AnnotationFragment;
import org.jglrxavpok.jlsl.fragments.CodeFragment;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment;
import org.jglrxavpok.jlsl.fragments.StartOfMethodFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

public class ObfuscationFilter
        implements CodeFilter {
    private final ArrayList<MethodEntry> entries = new ArrayList<>();

    private int nbr;

    public CodeFragment.Data filter(CodeFragment.Data fragment) {
        if (fragment instanceof MethodCallFragment methodCallFrag) {
            for (AnnotationFragment annotation : methodCallFrag.annotations()) {
                if (annotation.name().equals(NonObfuscable.class.getCanonicalName())) {
                    return fragment;
                }
            }

            if (!methodCallFrag.methodOwner().equals(((Class<?>) JLSLContext.currentInstance.getCurrentObject()).getCanonicalName())) {
                return fragment;
            }

            MethodEntry entry = getMethodEntry(methodCallFrag.methodName(), methodCallFrag.methodOwner(), methodCallFrag.argumentsTypes());
            return methodCallFrag.modify(modifier -> modifier.methodOwner(entry.newName));
        } else {
            if (fragment instanceof StartOfMethodFragment startMethodCallFrag) {
                for (AnnotationFragment annotation : startMethodCallFrag.annotations()) {
                    if (annotation.name().equals(NonObfuscable.class.getCanonicalName())) {
                        return fragment;
                    }
                }
                if (!startMethodCallFrag.owner().equals(((Class<?>) JLSLContext.currentInstance.getCurrentObject()).getCanonicalName())) {
                    return fragment;
                }
                MethodEntry entry = getMethodEntry(startMethodCallFrag.name(), startMethodCallFrag.owner(),
                        startMethodCallFrag.argumentsTypes().toArray(String[]::new));
                return startMethodCallFrag.modify(modifier -> modifier.owner(entry.newName));
            }
        }
        return fragment;
    }

    private MethodEntry getMethodEntry(String name, String owner, String[] argumentsTypes) {
        for (MethodEntry methodEntry : this.entries) {
            if (methodEntry.name.equals(name) && methodEntry.owner.equals(owner) && Arrays.deepEquals(argumentsTypes, methodEntry.argumentTypes)) {
                return methodEntry;
            }
        }

        MethodEntry entry = new MethodEntry(name, owner, argumentsTypes, getNewName());
        this.entries.add(entry);
        return entry;
    }

    private String getNewName() {
        char last = (char) (97 + this.nbr % 26);
        int nbr1 = this.nbr - 26;
        StringBuilder s = new StringBuilder();
        while (nbr1 >= 0) {
            nbr1 -= 26;
            s.insert(0, (char) (97 + nbr1 % 26));
        }
        this.nbr++;
        System.out.println("created name " + s + last);
        return s.toString() + s;
    }


    @Retention(RetentionPolicy.RUNTIME)
    public @interface NonObfuscable {
    }

    private record MethodEntry(String name, String owner, String[] argumentTypes, String newName) {
    }
}
