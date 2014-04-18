package buildings;
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
public class PlayerBuilding extends BaseASObject {
	public static final String unique_id    = "unique_id";
	public static final String upgrade_rank = "upgrade_rank";
	public static final String time_last_production_started = "time_last_production_started";
	public static final String quantity_looted_since_last_production = "quantity_looted_since_last_production";
	public static final String is_sold      = "is_sold";
	public static final String is_finished  = "is_finished";
	public static final String generated_player_building_values = "generated_player_building_values";


	public PlayerBuilding() {
		super();
	}

	public PlayerBuilding(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public PlayerBuilding(JSONObject source) {
		super(source);
		put(generated_player_building_values, new GeneratedPlayerBuildingValues(getJSONObject(generated_player_building_values)));
	}

	@Override
	protected void initialize() {
	}

	public int getUniqueId() {
		return getInt(unique_id);
	}

	public boolean isSold() {
		return getInt(is_sold) != 0;
	}

	public boolean isFinished() {
		return getInt(is_finished) == 1;
	}

	public Date getTimeLastProductionStarted() {
		String dateStr = getString(time_last_production_started);
		Date date = CCDateUtils.parseServerDate(dateStr);
		return date;
	}

	public GeneratedPlayerBuildingValues getGeneratedPlayerBuildingValues() {
		return (GeneratedPlayerBuildingValues) get(generated_player_building_values);
	}

	public boolean canCollect() {
		Date timeLastProductionStarted = getTimeLastProductionStarted();
		GeneratedPlayerBuildingValues values = getGeneratedPlayerBuildingValues();
		if ( ! isFinished() || isSold() || values.getHourlyIncome() == 0 /*|| values.hasDefense()*/ || values.getFullHarvestQuantity() == 0 || timeLastProductionStarted == null) {
			return false;
		}
		double hoursToOutput = getGeneratedPlayerBuildingValues().getHoursToOutput();
//		boolean canCollect = (long) (timeLastProductionStarted.getTime() + hoursToOutput * MILLIS_PER_HOUR) < System.currentTimeMillis();
		Calendar endTime = Calendar.getInstance();
		endTime.setTime(timeLastProductionStarted);
		endTime.add(Calendar.MINUTE, (int)(hoursToOutput*60));
		Calendar now = Calendar.getInstance();
		boolean canCollect = now.after(endTime);
		return canCollect;
	}
	/**
	 * Number of milliseconds in a standard second.
	 * @since 2.1
	 */
	private static final long MILLIS_PER_SECOND = 1000;
	/**
	 * Number of milliseconds in a standard minute.
	 * @since 2.1
	 */
	private static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
	/**
	 * Number of milliseconds in a standard hour.
	 * @since 2.1
	 */
	private static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
}
