package lockboxes;
/**
 *
 */


import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;

/**
 * @author C&eacute;drik LIME
 */
public class LockboxResult extends BaseASObject {
	public static String did_open = "did_open";//$NON-NLS-1$

	public LockboxResult() {
		super();
	}

	public LockboxResult(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public LockboxResult(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
	}

}
