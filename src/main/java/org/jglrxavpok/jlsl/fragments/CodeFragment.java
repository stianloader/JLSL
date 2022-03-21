package org.jglrxavpok.jlsl.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a code fragment
 * @param children the children of this fragment
 * @param allowPrint true if the fragment can print
 * @param data the data associated with this fragment
 */
public record CodeFragment<D extends CodeFragment.Data>(List<CodeFragment<?>> children, AtomicBoolean allowPrint, D data) {
    public CodeFragment(D data) {
        this(new ArrayList<>(), new AtomicBoolean(true), data);
    }

    public Class<D> type() {
        //noinspection unchecked
        return (Class<D>) data.getClass();
    }

    public interface Data {
        default <D extends Data> CodeFragment<D> toFragment(List<CodeFragment<?>> children, AtomicBoolean allowPrint) {
            return new CodeFragment<>(children, allowPrint, (D) this);
        }

        default <D extends Data> CodeFragment<D> toFragment() {
            return toFragment(new ArrayList<>(), new AtomicBoolean(true));
        }
    }
}
