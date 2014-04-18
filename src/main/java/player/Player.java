package player;
/**
 *
 */


import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;
import fr.cedrik.ccbot.CCDateUtils;

/**
 * @author C&eacute;drik LIME
 */
public class Player extends BaseASObject {
	public static final String player_id = "player_id";//$NON-NLS-1$
	public static final String username     = "username";//$NON-NLS-1$
	public static final String money        = "money";//$NON-NLS-1$
	public static final String bank_balance = "bank_balance";//$NON-NLS-1$
	public static final String level        = "level";//$NON-NLS-1$
	public static final String number_buildings_owned   = "number_buildings_owned";//$NON-NLS-1$
	public static final String last_update_health_value = "last_update_health_value";//$NON-NLS-1$
	public static final String last_update_health_time  = "last_update_health_time";//$NON-NLS-1$
	public static final String last_free_scratcher_open_time = "last_free_scratcher_open_time";//$NON-NLS-1$

	public Player() {
		super();
	}

	public Player(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public Player(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
	}

	public int getLevel() {
		return getInt(level);
	}

	public Date getLastFreeScratcherOpenTime() {
		Date lastFreeScratcherOpenTime = CCDateUtils.parseServerDate(getString(last_free_scratcher_open_time));
		return lastFreeScratcherOpenTime;
	}

	public boolean canScratch() {
		Date lastFreeScratcherOpenTime = getLastFreeScratcherOpenTime();
		if (lastFreeScratcherOpenTime == null) {
			return false;
		}
		Calendar endTime = Calendar.getInstance();
		endTime.setTime(lastFreeScratcherOpenTime);
		endTime.add(Calendar.DAY_OF_YEAR, 1);
		Calendar now = Calendar.getInstance();
		boolean canScratch = now.after(endTime);
		return canScratch;
	}
}
