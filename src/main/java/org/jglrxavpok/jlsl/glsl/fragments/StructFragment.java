package org.jglrxavpok.jlsl.glsl.fragments;

import org.jglrxavpok.jlsl.fragments.CodeFragment;
import org.jglrxavpok.jlsl.fragments.FieldFragment;
import org.jglrxavpok.jlsl.fragments.StartOfMethodFragment;
import org.jglrxavpok.jlsl.fragments.StoreVariableFragment;

public record StructFragment(
        String name,
        StartOfMethodFragment[] methods,
        FieldFragment[] fields
) implements CodeFragment.Data {
}
