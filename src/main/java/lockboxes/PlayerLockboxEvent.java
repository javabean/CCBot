package lockboxes;
/**
 *
 */


import java.util.Date;
import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;
import fr.cedrik.ccbot.CCDateUtils;

/*
	"player_lockbox_event": {
		"_explicitType": "lockboxes.PlayerLockboxEvent",
		"event_id": 71,
		"lockbox_tokens": 3,
		"lockboxes_opened": 0,
		"is_leaderboard_rewarded": 0,
		"leaderboard_rank": 0,
		"time_lockbox_unlockable": "2014-02-18 02:10:06", <-- 11:10:06 Paris time, -> PST (== GMT-8)
		"id": 7,
		"database_id": "120163574",
		"unique_id": 7,
		"player_id": "92343983947246471",
		"time_created": "2014-02-17 13:51:50",
		"time_updated": "2014-02-18 01:10:06",
		"version": 384
	},
 */
/**
 * @author C&eacute;drik LIME
 */
public class PlayerLockboxEvent extends BaseASObject {
	public static String event_id         = "event_id";//$NON-NLS-1$
	public static String lockbox_tokens   = "lockbox_tokens";//$NON-NLS-1$
	public static String lockboxes_opened = "lockboxes_opened";//$NON-NLS-1$
	public static String is_leaderboard_rewarded = "is_leaderboard_rewarded";//$NON-NLS-1$
	public static String leaderboard_rank = "leaderboard_rank";//$NON-NLS-1$
	public static String time_lockbox_unlockable = "time_lockbox_unlockable";//$NON-NLS-1$
	public static String id               = "id";//$NON-NLS-1$
	public static String database_id      = "database_id";//$NON-NLS-1$
	public static String unique_id        = "unique_id";//$NON-NLS-1$
	public static String player_id        = "player_id";//$NON-NLS-1$
	public static String time_created     = "time_created";//$NON-NLS-1$
	public static String time_updated     = "time_updated";//$NON-NLS-1$
	public static String version          = "version";//$NON-NLS-1$

	public PlayerLockboxEvent() {
		super();
	}

	public PlayerLockboxEvent(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public PlayerLockboxEvent(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
	}

	public int getEventId() {
		return getInt(event_id);
	}

	public Date getTimeLockboxUnlockable() {
		if (! has(time_lockbox_unlockable)) {
			return null;
		}
		String dateStr = getString(time_lockbox_unlockable);
		Date date = CCDateUtils.parseServerDate(dateStr);
		return date;
	}

	public boolean canUnlockBox() {
		Date timeLockboxUnlockable = getTimeLockboxUnlockable();
		if ( timeLockboxUnlockable == null) {
			return false;
		}
		boolean canUnlock = timeLockboxUnlockable.before(new Date());
//		Calendar endTime = Calendar.getInstance();
//		endTime.setTime(timeLockboxUnlockable);
//		Calendar now = Calendar.getInstance();
//		boolean canUnlock = now.after(endTime);
		return canUnlock;
	}
}
