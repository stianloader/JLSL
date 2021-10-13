package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.AnnotationFragment;
import org.jglrxavpok.jlsl.fragments.CodeFragment;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment;
import org.jglrxavpok.jlsl.fragments.StartOfMethodFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

public class ObfuscationFilter implements CodeFilter {

    private final ArrayList<MethodEntry> entries = new ArrayList<>();
    private int nbr;

    @Override
    public CodeFragment filter(final CodeFragment fragment) {
        if (fragment instanceof MethodCallFragment methodCallFrag) {
            for (final CodeFragment child : methodCallFrag.getChildren()) {
                if (child instanceof AnnotationFragment annot) {
					if (annot.name.equals(NonObfuscable.class.getCanonicalName())) {
						return fragment;
					}
                }
            }
            if (!methodCallFrag.methodOwner.equals(((Class<?>) JLSLContext.currentInstance.getCurrentObject()).getCanonicalName())) {
                return fragment;
            }
            final MethodEntry entry = getMethodEntry(methodCallFrag.methodName, methodCallFrag.methodOwner, methodCallFrag.argumentsTypes);
            methodCallFrag.methodName = entry.newName;
        } else if (fragment instanceof StartOfMethodFragment startMethodCallFrag) {
            for (final CodeFragment child : startMethodCallFrag.getChildren()) {
                if (child instanceof AnnotationFragment annot) {
					if (annot.name.equals(NonObfuscable.class.getCanonicalName())) {
						return fragment;
					}
                }
            }
            if (!startMethodCallFrag.owner.equals(((Class<?>) JLSLContext.currentInstance.getCurrentObject()).getCanonicalName())) {
                return fragment;
            }
            final MethodEntry entry = getMethodEntry(startMethodCallFrag.name, startMethodCallFrag.owner, startMethodCallFrag.argumentsTypes.toArray(new String[0]));
            startMethodCallFrag.name = entry.newName;
        }
        return fragment;
    }

    private MethodEntry getMethodEntry(final String name, final String owner, final String[] argumentsTypes) {
        for (final MethodEntry entry : entries) {
			if (entry.name.equals(name) && entry.owner.equals(owner) && Arrays.deepEquals(argumentsTypes, entry.argumentTypes)) {
				return entry;
			}
        }

        final MethodEntry entry = new MethodEntry(name, owner, argumentsTypes, getNewName());
        entries.add(entry);
        return entry;
    }

    private String getNewName() {
        final char last = (char) ('a' + (nbr % 26));
        int nbr1 = nbr - 26;
        final StringBuilder s = new StringBuilder();
        while (nbr1 >= 0) {
            nbr1 -= 26;
            s.insert(0, (char) ('a' + (nbr1 % 26)));
        }
        nbr++;
        System.out.println("created name " + s + last);
        return s.toString() + last;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface NonObfuscable {

    }

    private static class MethodEntry {
        public final String name;
        public final String owner;
        public final String[] argumentTypes;
        public final String newName;

        public MethodEntry(final String name, final String owner, final String[] argumentTypes, final String newName) {
            this.name = name;
            this.owner = owner;
            this.argumentTypes = argumentTypes;
            this.newName = newName;
        }
    }

}
