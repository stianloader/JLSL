package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.*;

public class Vec2
{

	public double x;
	public double y;

	public Vec2(double x, double y)
	{
		super();
		this.x = x;
		this.y = y;
	}

	@Substitute(value = "+", usesParenthesis = false, ownerBefore = true)
	public Vec2 add(Vec2 v) {
		return new Vec2(this.x + v.x, this.y + v.y);
	}

	@Substitute(value = "/", usesParenthesis = false, ownerBefore = true)
	public Vec2 div(double i) {
		return new Vec2(x / i, y / i);
	}

	@Substitute(value = "/", usesParenthesis = false, ownerBefore = true)
	public Vec2 div(int i) {
		return new Vec2(x / i, y / i);
	}

	@Substitute(value = "/", usesParenthesis = false, ownerBefore = true)
	public Vec2 div(Vec2 v) {
		return new Vec2(this.x / v.x, this.y / v.y);
	}

	public double length()
	{
		double dx = x;
		double dy = y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	@Substitute(value = "*", usesParenthesis = false, ownerBefore = true)
	public Vec2 mul(double i) {
		return new Vec2(x * i, y * i);
	}

	@Substitute(value = "*", usesParenthesis = false, ownerBefore = true)
	public Vec2 mul(int i) {
		return new Vec2(x * i, y * i);
	}

	@Substitute(value = "*", usesParenthesis = false, ownerBefore = true)
	public Vec2 mul(Vec2 v) {
		return new Vec2(this.x * v.x, this.y * v.y);
	}

	public Vec2 normalize()
	{
		double l = length();
		double x1 = x / l;
		double y1 = y / l;
		return new Vec2(x1, y1);
	}

	@Substitute(value = "-", usesParenthesis = false, ownerBefore = true)
	public Vec2 sub(Vec2 v) {
		return new Vec2(this.x - v.x, this.y - v.y);
	}
}
