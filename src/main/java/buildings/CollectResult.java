package buildings;
/**
 *
 */


import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;

/**
 * @author C&eacute;drik LIME
 */
public class CollectResult extends BaseASObject {
	public static final String success = "success";
	public static final String player_building_id = "player_building_id";
	public static final String reason = "reason";//not in response

	public CollectResult() {
		super();
	}

	public CollectResult(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public CollectResult(JSONObject jo) {
		super(jo);
	}

	@Override
	protected void initialize() {
	}

	public boolean getSuccess() {
		return getBoolean(success);
	}

	public String getReason() {
		return getString(reason);
	}

	public int getPlayerBuildingId() {
		return getInt(player_building_id);
	}
}
