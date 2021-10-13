package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;

public class vec2 {

    public final double x;
    public final double y;

    public vec2(final double x, final double y) {
        super();
        this.x = x;
        this.y = y;
    }

    public double length() {
        final double dx = x;
        final double dy = y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public vec2 normalize() {
        final double l = length();
        final double x1 = x / l;
        final double y1 = y / l;
        return new vec2(x1, y1);
    }

    @Substitute(value = "/", usesParenthesis = false, ownerBefore = true)
    public vec2 div(final double i) {
        return new vec2(x / i, y / i);
    }

}
