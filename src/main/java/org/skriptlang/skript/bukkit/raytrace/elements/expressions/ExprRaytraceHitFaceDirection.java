package org.skriptlang.skript.bukkit.raytrace.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Direction;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Raytrace Result - Hit Face Direction")
@Description("""
	The direction of the side of the block a raytrace hit, pointing away from the block.
	This is not set when the raytrace hit an entity, or when it hit nothing at all.
	""")
@Example("""
	set {_hit} to the results of a raytrace from player for 20 meters
	send "You are looking at the %hit face direction of {_hit}% side of the block"
	""")
@Since("INSERT VERSION")
public class ExprRaytraceHitFaceDirection extends SimplePropertyExpression<RayTraceResult, Direction> {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EXPRESSION,
			infoBuilder(
				ExprRaytraceHitFaceDirection.class,
				Direction.class,
				"hit [block] face direction",
				"raytraceresults",
				false
			)
				.supplier(ExprRaytraceHitFaceDirection::new)
				.build()
		);
	}

	@Override
	public @Nullable Direction convert(RayTraceResult result) {
		BlockFace hitFace = result.getHitBlockFace();
		if (hitFace == null)
			return null;
		return new Direction(hitFace, 1);
	}

	@Override
	public Class<Direction> getReturnType() {
		return Direction.class;
	}

	@Override
	protected String getPropertyName() {
		return "hit face direction";
	}

}
