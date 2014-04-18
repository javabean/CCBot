package guilds;
/**
 *
 */


import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;

/**
 * @author C&eacute;drik LIME
 */
public class GuildDetails extends BaseASObject {
	public static final String summary = "summary";////$NON-NLS-1$

	public GuildDetails() {
		super();
	}

	public GuildDetails(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public GuildDetails(JSONObject source) {
		super(source);
		put(summary, new guild(getJSONObject(summary)));
	}

	@Override
	protected void initialize() {
	}

	public String getId() {
		guilds.guild guildsGuild = (guilds.guild) get(summary);
		return guildsGuild.getString(guilds.guild.guild_id);
	}
}
