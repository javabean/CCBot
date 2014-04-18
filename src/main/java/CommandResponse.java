/**
 *
 */


import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;

/**
 * @author C&eacute;drik LIME
 */
public class CommandResponse extends BaseASObject {
	public static final String SERVICE = "service";
	public static final String METHOD  = "method";
	public static final String STATUS  = "status";
	public static final String SEQUENCE_NUM  = "sequence_num";
	public static final String RETURN_VALUE  = "return_value";

	public CommandResponse() {
		super();
	}

	public CommandResponse(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public CommandResponse(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
	}

	public String getService() {
		return (String) get(SERVICE);
	}
	public void setService(String service) {
		put(SERVICE, service);
	}

	public String getMethod() {
		return (String) get(METHOD);
	}
	public void setMethod(String method) {
		put(METHOD, method);
	}

	public Integer getSequence_num() {
		return (Integer) get(SEQUENCE_NUM);
	}
	public void setSequence_num(Integer sequence_num) {
		put(SEQUENCE_NUM, sequence_num);
	}

}
