package org.jglrxavpok.jlsl.glsl;


public class Sampler2D {
    public final int id;

    @GLSL.Substitute(value = "", usesParenthesis = false, ownerBefore = true)
    public Sampler2D(int id) {
        this.id = id;
    }
}
