package org.jglrxavpok.jlsl.glsl;

public class Vertex {
    private final Vec3 pos = new Vec3(1.0D, 1.0D, 1.0D);

    private final Vec2 texCoords = new Vec2(0.0D, 0.0D);


    public double test(double v) {
        this.pos.x += v;
        return this.pos.x;
    }
}
