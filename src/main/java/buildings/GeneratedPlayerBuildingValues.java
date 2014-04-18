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
public class GeneratedPlayerBuildingValues extends BaseASObject {
	public static final String hours_to_output         = "hours_to_output";
	public static final String full_harvest_quantity   = "full_harvest_quantity";
	public static final String actual_harvest_quantity = "actual_harvest_quantity";
	public static final String hourly_income = "hourly_income";
	public static final String defense       = "defense";

	public GeneratedPlayerBuildingValues() {
		super();
	}

	public GeneratedPlayerBuildingValues(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public GeneratedPlayerBuildingValues(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
	}

	public double getHoursToOutput() {
		return getDouble(hours_to_output);
	}

	public long getFullHarvestQuantity() {
		return getLong(full_harvest_quantity);
	}

	public double getActualHarvestQuantity() {
		return getDouble(actual_harvest_quantity);
	}

	public double getHourlyIncome() {
		return getDouble(hourly_income);
	}

	public boolean hasDefense() {
		return has(defense);
	}
}
