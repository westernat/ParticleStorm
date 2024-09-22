package org.mesdag.particlestorm.data.molang.utils;

public class MathUtils
{
	public static int clamp(int x, int min, int max)
	{
		return x < min ? min : (Math.min(x, max));
	}

	public static float clamp(float x, float min, float max)
	{
		return x < min ? min : (Math.min(x, max));
	}

	public static double clamp(double x, float min, double max)
	{
		return x < min ? min : (Math.min(x, max));
	}

	public static int cycler(int x, int min, int max)
	{
		return x < min ? max : (x > max ? min : x);
	}

	public static float cycler(float x, float min, float max)
	{
		return x < min ? max : (x > max ? min : x);
	}

	public static double cycler(double x, double min, double max)
	{
		return x < min ? max : (x > max ? min : x);
	}
}
