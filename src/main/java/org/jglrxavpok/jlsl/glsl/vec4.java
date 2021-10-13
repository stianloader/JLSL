package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;

public class vec4 {
    public double x;
    public double y;
    public double z;
    public double w;

    @Substitute(value = "x", usesParenthesis = false, ownerBefore = true, actsAsField = true)
    public double a;
    @Substitute(value = "y", usesParenthesis = false, ownerBefore = true, actsAsField = true)
    public double b;
    @Substitute(value = "z", usesParenthesis = false, ownerBefore = true, actsAsField = true)
    public double c;
    @Substitute(value = "w", usesParenthesis = false, ownerBefore = true, actsAsField = true)
    public double d;

    public vec4(final double x, final double y, final double z, final double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public vec4(final double x, final double y, final vec2 zw) {
        this.z = zw.x;
        this.w = zw.y;
        this.x = x;
        this.y = y;
    }

    public vec4(final double x, final vec2 yz, final double w) {
        this.y = yz.x;
        this.z = yz.y;
        this.x = x;
        this.w = w;
    }

    public vec4(final vec2 xy, final double z, final double w) {
        this.x = xy.x;
        this.y = xy.y;
        this.z = z;
        this.w = w;
    }

    public vec4(final vec3 xyz, final double w) {
        this.x = xyz.x;
        this.y = xyz.y;
        this.z = xyz.z;
        this.w = w;
    }

    public double length() {
        final double dx = x;
        final double dy = y;
        final double dz = z;
        final double dw = w;
        return Math.sqrt(dx * dx + dy * dy + dz * dz + dw * dw);
    }

    public vec4 normalize() {
        final double l = length();
        final double x1 = x / l;
        final double y1 = y / l;
        final double z1 = z / l;
        final double w1 = w / l;
        return new vec4(x1, y1, z1, w1);
    }

    @Substitute(value = "+", usesParenthesis = false, ownerBefore = true)
    public vec4 add(final vec4 v) {
        return new vec4(x + v.x, y + v.y, z + v.z, w + v.w);
    }

    @Substitute(value = "-", usesParenthesis = false, ownerBefore = true)
    public vec4 sub(final vec4 v) {
        return new vec4(x - v.x, y - v.y, z - v.z, w - v.w);
    }

    @Substitute(value = "*", usesParenthesis = false, ownerBefore = true)
    public vec4 mul(final double d) {
        return new vec4(x * d, y * d, z * d, w * d);
    }

    @Substitute(value = "*", usesParenthesis = false, ownerBefore = true)
    public vec4 mul(final vec4 v) {
        return new vec4(x * v.x, y * v.y, z * v.z, w * v.w);
    }
}
