package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;

public class sampler2D {
    public final int id;

    @Substitute(value = "", usesParenthesis = false, ownerBefore = true)
    public sampler2D(final int id) {
        this.id = id;
    }
}
