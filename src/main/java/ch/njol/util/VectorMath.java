package ch.njol.util;

import ch.njol.skript.expressions.ExprVectorCylindrical;
import ch.njol.skript.expressions.ExprVectorFromYawAndPitch;
import ch.njol.skript.expressions.ExprVectorSpherical;
import ch.njol.skript.expressions.ExprYawPitch;
import org.skriptlang.skript.bukkit.vector.FastVector;

@Deprecated(since = "2.10.0", forRemoval = true)
public final class VectorMath {

	public static final double PI = Math.PI;
	public static final double HALF_PI = PI / 2;
	public static final double DEG_TO_RAD = Math.PI / 180;
	public static final double RAD_TO_DEG = 180 / Math.PI;

	private VectorMath() {}

	public static FastVector fromSphericalCoordinates(double radius, double theta, double phi) {
		return ExprVectorSpherical.fromSphericalCoordinates(radius, theta, phi);
	}

	public static FastVector fromCylindricalCoordinates(double radius, double phi, double height) {
		return ExprVectorCylindrical.fromCylindricalCoordinates(radius, phi, height);
	}

	public static FastVector fromYawAndPitch(float yaw, float pitch) {
		return ExprYawPitch.fromYawAndPitch(yaw, pitch);
	}

	public static float getYaw(FastVector vector) {
		return ExprYawPitch.getYaw(vector);
	}

	public static float getPitch(FastVector vector) {
		return ExprYawPitch.getPitch(vector);
	}

	public static FastVector rotX(FastVector vector, double angle) {
		double sin = Math.sin(angle * DEG_TO_RAD);
		double cos = Math.cos(angle * DEG_TO_RAD);
		FastVector vy = new FastVector(0, cos, -sin);
		FastVector vz = new FastVector(0, sin, cos);
		FastVector clone = vector.clone();
		vector.setY(clone.dot(vy));
		vector.setZ(clone.dot(vz));
		return vector;
	}

	public static FastVector rotY(FastVector vector, double angle) {
		double sin = Math.sin(angle * DEG_TO_RAD);
		double cos = Math.cos(angle * DEG_TO_RAD);
		FastVector vx = new FastVector(cos, 0, sin);
		FastVector vz = new FastVector(-sin, 0, cos);
		FastVector clone = vector.clone();
		vector.setX(clone.dot(vx));
		vector.setZ(clone.dot(vz));
		return vector;
	}

	public static FastVector rotZ(FastVector vector, double angle) {
		double sin = Math.sin(angle * DEG_TO_RAD);
		double cos = Math.cos(angle * DEG_TO_RAD);
		FastVector vx = new FastVector(cos, -sin, 0);
		FastVector vy = new FastVector(sin, cos, 0);
		FastVector clone = vector.clone();
		vector.setX(clone.dot(vx));
		vector.setY(clone.dot(vy));
		return vector;
	}

	public static FastVector rot(FastVector vector, FastVector axis, double angle) {
		double sin = Math.sin(angle * DEG_TO_RAD);
		double cos = Math.cos(angle * DEG_TO_RAD);
		FastVector a = axis.clone().normalize();
		double ax = a.getX();
		double ay = a.getY();
		double az = a.getZ();
		FastVector rotx = new FastVector(cos+ax*ax*(1-cos), ax*ay*(1-cos)-az*sin, ax*az*(1-cos)+ay*sin);
		FastVector roty = new FastVector(ay*ax*(1-cos)+az*sin, cos+ay*ay*(1-cos), ay*az*(1-cos)-ax*sin);
		FastVector rotz = new FastVector(az*ax*(1-cos)-ay*sin, az*ay*(1-cos)+ax*sin, cos+az*az*(1-cos));
		double x = rotx.dot(vector);
		double y = roty.dot(vector);
		double z = rotz.dot(vector);
		vector.setX(x).setY(y).setZ(z);
		return vector;
	}

	public static float skriptYaw(float yaw) {
		return ExprYawPitch.skriptYaw(yaw);
	}

	public static float skriptPitch(float pitch) {
		return ExprYawPitch.skriptPitch(pitch);
	}

	public static float fromSkriptYaw(float yaw) {
		return ExprYawPitch.fromSkriptYaw(yaw);
	}

	public static float fromSkriptPitch(float pitch) {
		return ExprYawPitch.fromSkriptPitch(pitch);
	}

	public static float wrapAngleDeg(float angle) {
		return ExprVectorFromYawAndPitch.wrapAngleDeg(angle);
	}

	public static void copyVector(FastVector vector1, FastVector vector2) {
		vector1.copy(vector2);
	}

	/**
	 * Check whether or not each component of this vector is equal to 0.
	 * <br>Replaces {@code Vector#isZero()} since that method was added in spigot 1.19.3
	 * @return true if equal to zero, false if at least one component is non-zero
	 */
	public static boolean isZero(FastVector vector) {
		return (vector.getX() == 0 && vector.getY() == 0 && vector.getZ() == 0);
	}

}
