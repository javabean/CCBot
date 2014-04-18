/**
 *
 */


import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;

/**
 * @author C&eacute;drik LIME
 */
public class Session extends BaseASObject {
	public static final String req_id             = "req_id";
	public static final String start_sequence_num = "start_sequence_num";
	public static final String end_sequence_num   = "end_sequence_num";

	public static final String client_version     = "client_version";
	public static final String invite_code        = "invite_code";
	public static final String serv_hash          = "serv_hash";
	public static final String client_build       = "client_build";
	public static final String previous_client_version = "previous_client_version";
	public static final String transaction_time   = "transaction_time";
	public static final String device_type        = "device_type";
	public static final String game_name          = "game_name";
	public static final String game_data_version  = "game_data_version";
	public static final String game_data_md5      = "game_data_md5";
	public static final String next_game_data_version = "next_game_data_version";
	public static final String session_id         = "session_id";
	public static final String api_version        = "api_version";
	public static final String app_uuid           = "app_uuid";
	public static final String mac_address        = "mac_address";
	public static final String iphone_udid        = "iphone_udid";
	public static final String player_id          = "player_id";

	public static final String session_key       = "session_key";
	public static final String seconds_from_gmt  = "seconds_from_gmt";
	public static final String client_current_data_version = "client_current_data_version";
	public static final String advertising_id    = "advertising_id";
	public static final String vendor_id         = "vendor_id";

	public Session() {
		super();
	}

	public Session(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public Session(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
		put(req_id, "1");
		put(start_sequence_num, 1);
		put(end_sequence_num, get(start_sequence_num));
		put(serv_hash, "14AB47A344B7751C692A91A31480B7BE");//FIXME
		put(transaction_time, Long.toString(System.currentTimeMillis() / 1000));
//		put(mac_address, "0F607264FC6318A92B9E13C65DB7CD3C");
	}

	public int getStart_sequence_num() {
		return getInt(start_sequence_num);
	}
	public void setStart_sequence_num(int start_sequence_num) {
		put(Session.start_sequence_num, start_sequence_num);
		put(Session.end_sequence_num, start_sequence_num);
	}

	public String getEnd_sequence_num() {
		return (String) get(end_sequence_num);
	}
	public void setEnd_sequence_num(int end_sequence_num) {
		put(Session.end_sequence_num, end_sequence_num);
	}
}
