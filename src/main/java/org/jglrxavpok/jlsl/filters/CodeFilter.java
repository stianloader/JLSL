package org.jglrxavpok.jlsl.filters;

import org.jetbrains.annotations.NotNull;

public interface CodeFilter<I, O> {
    @NotNull O filter(@NotNull I input);
}
