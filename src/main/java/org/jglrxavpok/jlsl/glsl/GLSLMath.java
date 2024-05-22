package org.jglrxavpok.jlsl.glsl;

public class GLSLMath {
	public static Vec2 abs(Vec2 v) {
		return new Vec2(Math.abs(v.x), Math.abs(v.y));
	}

	public static Vec3 abs(Vec3 v) {
		return new Vec3(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z));
	}

	public static Vec4 abs(Vec4 v) {
		return new Vec4(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z), Math.abs(v.w));
	}

	public static float dot(Vec2 a, Vec2 b) {
		return (float) (a.x * b.x + a.y * b.y);
	}

	public static float dot(Vec3 a, Vec3 b) {
		return (float) (a.x * b.x + a.y * b.y + a.z * b.z);
	}

	public static float dot(Vec4 a, Vec4 b) {
		return (float) (a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w);
	}

	public static double smoothstep(double edge0, double edge1, double x) {
		// Note: This is an approximation of the smoothstep function.
		// Smoothstep is by no means the same as step, but I couldn't be bothered
		// of writing a smoothstep function in java.
		return step(edge0, edge1, x);
	}

	public static float smoothstep(float edge0, float edge1, float x) {
		// Note: This is an approximation of the smoothstep function.
		// Smoothstep is by no means the same as step, but I couldn't be bothered
		// of writing a smoothstep function in java.
		return Math.min(edge1, Math.max(edge0, x));
	}

	public static double step(double edge0, double edge1, double x) {
		return Math.min(edge1, Math.max(edge0, x));
	}

	public static float step(float edge0, float edge1, float x) {
		return Math.min(edge1, Math.max(edge0, x));
	}
}
