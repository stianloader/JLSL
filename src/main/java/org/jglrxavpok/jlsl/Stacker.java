package org.jglrxavpok.jlsl;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Stacker<E>(@NotNull List<E> list) {

    /**
     * Stacks the given element to the stack
     *
     * @param element the element to stack
     */
    public void stack(@NotNull E element) {
        list.add(element);
    }

    /**
     * Get the element at the top of the stack
     *
     * @return the element at the top of the stack
     */
    public @NotNull
    E top() {
        return get(0);
    }

    /**
     * Get the element some number of elements down from the top of the stack
     *
     * @param offset the offset from the top of the stack
     * @return the element at the given offset
     */
    public @NotNull
    E get(int offset) {
        return list.get(list.size() - 1 - offset);
    }
}
