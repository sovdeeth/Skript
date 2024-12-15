package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.*;
import com.destroystokyo.paper.ClientOption;
import org.bukkit.entity.Player;

@Name("Has Chat Filtering")
@Description("Checks whether a player has chat filtering enabled.")
@Examples({
	"if player doesn't have chat filtering enabled:",
		"send \"<gray>This server may contain mature chat messages. You have been warned!\" to player",
})
@RequiredPlugins("Paper")
@Since("INSERT VERSION")
public class CondChatFiltering extends PropertyCondition<Player> {

	static {
		if (Skript.classExists("com.destroystokyo.paper.ClientOption"))
			register(CondChatFiltering.class, PropertyType.HAVE,
				"(chat|text) filtering (on|enabled)", "players");
	}

	@Override
	public boolean check(Player player) {
		return player.getClientOption(ClientOption.TEXT_FILTERING_ENABLED);
	}

	@Override
	protected String getPropertyName() {
		return "chat filtering enabled";
	}

}
