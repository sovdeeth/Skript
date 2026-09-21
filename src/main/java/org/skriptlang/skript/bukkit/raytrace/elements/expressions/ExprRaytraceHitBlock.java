package org.skriptlang.skript.bukkit.raytrace.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.block.Block;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Raytrace Result - Hit Block")
@Description("""
	The block a raytrace hit.
	This is not set when the raytrace hit an entity, or when it hit nothing at all.
	""")
@Example("""
	set {_hit} to the results of a raytrace from player for 20 meters
	if {_hit} hit a block:
		send "You are looking at %the hit block of {_hit}%"
	""")
@Since("INSERT VERSION")
public class ExprRaytraceHitBlock extends SimplePropertyExpression<RayTraceResult, Block> {

	public static void register(SyntaxRegistry registry) {
		registry.register(
			SyntaxRegistry.EXPRESSION,
			infoBuilder(
				ExprRaytraceHitBlock.class,
				Block.class,
				"hit block",
				"raytraceresults",
				false
			)
				.supplier(ExprRaytraceHitBlock::new)
				.build()
		);
	}

	@Override
	public @Nullable Block convert(RayTraceResult result) {
		return result.getHitBlock();
	}

	@Override
	public Class<Block> getReturnType() {
		return Block.class;
	}

	@Override
	protected String getPropertyName() {
		return "hit block";
	}

}
