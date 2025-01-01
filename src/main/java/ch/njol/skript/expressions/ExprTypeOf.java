package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.lang.util.common.AnyTyped;
import ch.njol.skript.util.EnchantmentType;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

@Name("Type of")
@Description({
	"Type of a block, item, entity, inventory, potion effect, enchantment, or other typed value.",
	"Types of items, blocks and block datas are item types similar to them but have amounts",
	"of one, no display names and, on Minecraft 1.13 and newer versions, are undamaged.",
	"Types of entities and inventories are entity types and inventory types known to Skript.",
	"Types of potion effects are potion effect types."
})
@Examples({
	"on rightclick on an entity:",
		"\tmessage \"This is a %type of clicked entity%!\""
})
@Since("1.4, 2.5.2 (potion effect), 2.7 (block datas), INSERT VERSION (enchantments)")
public class ExprTypeOf extends SimplePropertyExpression<Object, Object> {

	static {
		register(ExprTypeOf.class, Object.class, "type", "typeds");
	}

	@Override
	protected String getPropertyName() {
		return "type";
	}

	@Override
	public @Nullable Object convert(Object object) {
		if (object instanceof AnyTyped<?> typed)
			return typed.type();
		assert false;
		return null;
	}

	@Override
	public Class<?> getReturnType() {
		Class<?> returnType = getExpr().getReturnType();
		return EntityData.class.isAssignableFrom(returnType) ? EntityData.class
			: ItemType.class.isAssignableFrom(returnType) ? ItemType.class
			: PotionEffectType.class.isAssignableFrom(returnType) ? PotionEffectType.class
			: BlockData.class.isAssignableFrom(returnType) ? ItemType.class
			: EnchantmentType.class.isAssignableFrom(returnType) ? Enchantment.class
			: Object.class;
	}

	@Override
	@SafeVarargs
	protected final <R> @Nullable ConvertedExpression<Object, ? extends R> getConvertedExpr(Class<R>... to) {
		if (!Converters.converterExists(EntityData.class, to) && !Converters.converterExists(ItemType.class, to))
			return null;
		return super.getConvertedExpr(to);
	}

}
