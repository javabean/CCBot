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
public class guild extends BaseASObject {
	public static final String guild_id = "guild_id";

	public guild() {
		super();
	}

	public guild(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public guild(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
	}
}
