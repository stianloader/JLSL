package org.jglrxavpok.jlsl.glsl;

public class vec3 {

    public final double y;
    public final double z;
    public double x;

    public vec3(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double length() {
        final double dx = x;
        final double dy = y;
        final double dz = z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public vec3 normalize() {
        final double l = length();
        final double x1 = x / l;
        final double y1 = y / l;
        final double z1 = z / l;
        return new vec3(x1, y1, z1);
    }

}
