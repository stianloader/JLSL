package org.jglrxavpok.jlsl.glsl;

@GLSL.NativeClass(name = "vec2")
public class Vec2 {
    public final double x;
    public final double y;

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double length() {
        double dx = this.x;
        double dy = this.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Vec2 normalize() {
        double l = length();
        double x1 = this.x / l;
        double y1 = this.y / l;
        return new Vec2(x1, y1);
    }

    @GLSL.Substitute(value = "/", usesParenthesis = false, ownerBefore = true)
    public Vec2 div(double i) {
        return new Vec2(this.x / i, this.y / i);
    }
}
