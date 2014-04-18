/**
 *
 */


import java.util.Map;

import fr.cedrik.ccbot.BaseASObject;

/**
 * @author C&eacute;drik LIME
 */
public class Command extends BaseASObject {
	public static final String METHOD           = "method";
	public static final String SERVICE          = "service";
	public static final String SEQUENCE_NUM     = "sequence_num";
	public static final String PLAYER_DELTA     = "player_delta";
	public static final String PARAMS           = "params";
	public static final String gameDataMD5      = "gameDataMD5";
	public static final String ICP              = "icp";
	public static final String TRANSACTION_TIME = "transaction_time";

	public Command() {
		super();
	}

	public Command(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	@Override
	protected void initialize() {
//		put(METHOD,       "collect");
//		put(SERVICE,      "buildings.buildings");
		put(TRANSACTION_TIME, Long.valueOf(System.currentTimeMillis() / 1000));
		put(SEQUENCE_NUM, Integer.valueOf(0));
		put(ICP,          Boolean.FALSE);
		put(gameDataMD5,  "");
		put(PLAYER_DELTA, new CCLocalPlayerChanges());
	}

	public CCLocalPlayerChanges getPlayerDelta() {
		return (CCLocalPlayerChanges) get(PLAYER_DELTA);
	}

	public void setService(String service) {
		put(SERVICE, service);
	}

	public void setMethod(String method) {
		put(METHOD, method);
	}

	public void setSequenceNum(int sequence_num) {
		put(SEQUENCE_NUM, Integer.valueOf(sequence_num));
	}

	public void setParams(Object... params) {
		put(PARAMS, params);
	}

}
