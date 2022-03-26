package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode;

public interface GLSLEncoder<I> {
    @NotNull GLSLBytecode.Root encode(@NotNull I input);
}
