package org.skriptlang.skript.bukkit.vector;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Map;

@SerializableAs("Vector")
public class FastVector extends Vector {

	public static FastVector asFastVector(Vector vec) {
		if (vec instanceof FastVector fvec)
			return fvec;
		return new FastVector(vec);

	}

	private final Vector3d vector;

	public FastVector() {
		this.vector = new Vector3d();
	}

	public FastVector(int x, int y, int z) {
		this.vector = new Vector3d(x, y, z);
	}

	public FastVector(double x, double y, double z) {
		this.vector = new Vector3d(x, y, z);
	}

	public FastVector(float x, float y, float z) {
		this.vector = new Vector3d(x, y, z);
	}

	public FastVector(@NotNull Vector vec) {
		this(vec.getX(), vec.getY(), vec.getZ());
	}

	public FastVector(@NotNull Location location) {
		this(location.getX(), location.getY(), location.getZ());
	}

	public FastVector(Vector3d vector) {
		this.vector = new Vector3d(vector);
	}

	public FastVector(@NotNull Vector3f vector) {
		this.vector = new Vector3d(vector);
	}


	// addition

	@Override
	public @NotNull FastVector add(@NotNull Vector vec) {
		return add(vec, this);
	}

	public @NotNull FastVector add(@NotNull Vector vec, @NotNull FastVector dest) {
		vector.add(vec.getX(), vec.getY(), vec.getZ(), dest.vector);
		return dest;
	}

	public @NotNull FastVector add(@NotNull FastVector vec) {
		return add(vec, this);
	}

	public @NotNull FastVector add(@NotNull FastVector vec, @NotNull FastVector dest) {
		vector.add(vec.vector, dest.vector);
		return dest;
	}

	// subtraction

	@Override
	public @NotNull FastVector subtract(@NotNull Vector vec) {
		return subtract(vec, this);
	}

	public @NotNull FastVector subtract(@NotNull Vector vec, @NotNull FastVector dest) {
		vector.sub(vec.getX(), vec.getY(), vec.getZ(), dest.vector);
		return dest;
	}

	public @NotNull FastVector subtract(@NotNull FastVector vec) {
		return subtract(vec, this);
	}

	public @NotNull FastVector subtract(@NotNull FastVector vec, @NotNull FastVector dest) {
		vector.sub(vec.vector, dest.vector);
		return dest;
	}

	// multiplication

	@Override
	public @NotNull FastVector multiply(@NotNull Vector vec) {
		return multiply(vec, this);
	}

	public @NotNull FastVector multiply(@NotNull Vector vec, @NotNull FastVector dest) {
		vector.mul(vec.getX(), vec.getY(), vec.getZ(), dest.vector);
		return dest;
	}

	public @NotNull FastVector multiply(@NotNull FastVector vec) {
		return multiply(vec, this);
	}

	public @NotNull FastVector multiply(@NotNull FastVector vec, @NotNull FastVector dest) {
		vector.mul(vec.vector, dest.vector);
		return dest;
	}

	@Override
	public @NotNull FastVector multiply(int m) {
		return multiply(m, this);
	}

	@Override
	public @NotNull FastVector multiply(double m) {
		return multiply(m, this);
	}

	@Override
	public @NotNull FastVector multiply(float m) {
		return multiply(m, this);
	}

	public @NotNull FastVector multiply(double m, @NotNull FastVector dest) {
		vector.mul(m, dest.vector);
		return dest;
	}

	// division

	@Override
	public @NotNull FastVector divide(@NotNull Vector vec) {
		return divide(vec, this);
	}

	public @NotNull FastVector divide(@NotNull Vector vec, @NotNull FastVector dest) {
		vector.div(vec.getX(), vec.getY(), vec.getZ(), dest.vector);
		return dest;
	}

	public @NotNull FastVector divide(@NotNull FastVector vec) {
		return divide(vec, this);
	}

	public @NotNull FastVector divide(@NotNull FastVector vec, @NotNull FastVector dest) {
		vector.div(vec.vector, dest.vector);
		return dest;
	}

	public @NotNull FastVector divide(int m) {
		return divide(m, this);
	}

	public @NotNull FastVector divide(double m) {
		return divide(m, this);
	}

	public @NotNull FastVector divide(float m) {
		return divide(m, this);
	}

	public @NotNull FastVector divide(double m, @NotNull FastVector dest) {
		vector.div(m, dest.vector);
		return dest;
	}

	// products

	@Override
	public double dot(@NotNull Vector other) {
		return vector.dot(other.getX(), other.getY(), other.getZ());
	}

	public double dot(@NotNull FastVector other) {
		return vector.dot(other.vector);
	}

	@Override
	public @NotNull FastVector crossProduct(@NotNull Vector vec) {
		return crossProduct(vec, this);
	}

	public @NotNull FastVector crossProduct(@NotNull Vector vec, @NotNull FastVector dest) {
		vector.cross(vec.getX(), vec.getY(), vec.getZ(), dest.vector);
		return dest;
	}

	public @NotNull FastVector crossProduct(@NotNull FastVector vec) {
		return crossProduct(vec, this);
	}

	public @NotNull FastVector crossProduct(@NotNull FastVector vec, @NotNull FastVector dest) {
		vector.cross(vec.vector, dest.vector);
		return dest;
	}

	@Override
	public @NotNull FastVector getCrossProduct(@NotNull Vector vec) {
		return crossProduct(vec, new FastVector());
	}

	// copy

	@Override
	public @NotNull FastVector copy(@NotNull Vector vec) {
		vector.set(vec.getX(), vec.getY(), vec.getZ());
		return this;
	}

	// lengths

	@Override
	public double length() {
		return vector.length();
	}

	@Override
	public double lengthSquared() {
		return vector.lengthSquared();
	}

	@Override
	public double distance(@NotNull Vector o) {
		return vector.distance(o.getX(), o.getY(), o.getZ());
	}

	public double distance(@NotNull FastVector o) {
		return vector.distance(o.vector);
	}

	@Override
	public double distanceSquared(@NotNull Vector o) {
		return vector.distanceSquared(o.getX(), o.getY(), o.getZ());
	}

	public double distanceSquared(@NotNull FastVector o) {
		return vector.distanceSquared(o.vector);
	}

	// utilities

	@Override
	public float angle(@NotNull Vector other) {
		copyToSuper();
		return super.angle(other);
//		return (float) vector.angle(other.toVector3d());
	}

	@Override
	public @NotNull FastVector midpoint(@NotNull Vector other) {
		return midpoint(other, this);
	}

	public @NotNull FastVector midpoint(@NotNull Vector other, FastVector dest) {
		return add(other, dest).multiply(0.5);
	}

	@Override
	public @NotNull FastVector getMidpoint(@NotNull Vector other) {
		return midpoint(other, new FastVector());
	}

	// norm

	@Override
	public @NotNull FastVector normalize() {
		return normalize(this);
	}

	public @NotNull FastVector normalize(@NotNull FastVector dest) {
		vector.normalize(dest.vector);
		return dest;
	}

	@Override
	public boolean isNormalized() {
		return Math.abs(vector.lengthSquared() - 1) < getEpsilon();
	}

	// zero

	@Override
	public @NotNull FastVector zero() {
		vector.zero();
		return this;
	}

	@Override
	public boolean isZero() {
		return vector.x == 0 && vector.y == 0 && vector.z == 0;
	}

	// checks

	@Override
	public boolean isInAABB(@NotNull Vector min, @NotNull Vector max) {
		copyToSuper();
		return super.isInAABB(min, max);
	}

	@Override
	public boolean isInSphere(@NotNull Vector origin, double radius) {
		copyToSuper();
		return super.isInSphere(origin, radius);
	}

	private void copyToSuper() {
		this.x = vector.x;
		this.y = vector.y;
		this.z = vector.z;
	}

	// rotate

	@Override
	public @NotNull FastVector rotateAroundX(double angle) {
		vector.rotateX(angle);
		return this;
	}

	@Override
	public @NotNull FastVector rotateAroundY(double angle) {
		vector.rotateY(angle);
		return this;
	}

	@Override
	public @NotNull FastVector rotateAroundZ(double angle) {
		vector.rotateZ(angle);
		return this;
	}

	@Override
	public @NotNull FastVector rotateAroundNonUnitAxis(@NotNull Vector axis, double angle) throws IllegalArgumentException {
		vector.rotateAxis(angle, axis.getX(), axis.getY(), axis.getZ());
		return this;
	}

	@Override
	public double getX() {
		return vector.x;
	}

	@Override
	public int getBlockX() {
		this.x = vector.x;
		return super.getBlockX();
	}

	@Override
	public double getY() {
		return vector.y;
	}

	@Override
	public int getBlockY() {
		this.y = vector.y;
		return super.getBlockY();
	}

	@Override
	public double getZ() {
		return vector.z;
	}

	@Override
	public int getBlockZ() {
		this.z = vector.z;
		return super.getBlockZ();
	}

	@Override
	public @NotNull FastVector setX(int x) {
		vector.x = x;
		return this;
	}

	@Override
	public @NotNull FastVector setX(double x) {
		vector.x = x;
		return this;
	}

	@Override
	public @NotNull FastVector setX(float x) {
		vector.x = x;
		return this;
	}

	@Override
	public @NotNull FastVector setY(int y) {
		vector.y = y;
		return this;
	}

	@Override
	public @NotNull FastVector setY(double y) {
		vector.y = y;
		return this;
	}

	@Override
	public @NotNull FastVector setY(float y) {
		vector.y = y;
		return this;
	}

	@Override
	public @NotNull FastVector setZ(int z) {
		vector.z = z;
		return this;
	}

	@Override
	public @NotNull FastVector setZ(double z) {
		vector.z = z;
		return this;
	}

	@Override
	public @NotNull FastVector setZ(float z) {
		vector.z = z;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vector other)) {
			return false;
		}

		return Math.abs(vector.x - other.getX()) < getEpsilon() && Math.abs(vector.y - other.getY()) < getEpsilon() && Math.abs(vector.z - other.getZ()) < getEpsilon();
	}

	@Override
	public int hashCode() {
		copyToSuper();
		return super.hashCode();
	}

	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public @NotNull FastVector clone() {
		return new FastVector(vector);
	}

	@Contract("_ -> this")
	private FastVector set(@NotNull Vector3d vector) {
		vector.set(vector);
		return this;
	}

	@Override
	public String toString() {
		copyToSuper();
		return super.toString();
	}

	@Override
	public @NotNull Location toLocation(@NotNull World world) {
		return new Location(world, vector.x, vector.y, vector.z);
	}

	@Override
	public @NotNull Location toLocation(@NotNull World world, float yaw, float pitch) {
		return new Location(world, vector.x, vector.y, vector.z, yaw, pitch);
	}

	@Override
	public @NotNull BlockVector toBlockVector() {
		return new BlockVector(vector.x, vector.y, vector.z);
	}

	@Override
	public @NotNull Vector3f toVector3f() {
		return new Vector3f(vector);
	}

	@Override
	public @NotNull Vector3d toVector3d() {
		return vector;
	}

	@Override
	public @NotNull Vector3i toVector3i(int roundingMode) {
		return new Vector3i(vector, roundingMode);
	}

	@Override
	public void checkFinite() throws IllegalArgumentException {
		copyToSuper();
		super.checkFinite();
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		copyToSuper();
		return super.serialize();
	}
}
