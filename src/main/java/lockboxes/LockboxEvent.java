package lockboxes;
/**
 *
 */


import java.util.Map;

import org.json.JSONObject;

import fr.cedrik.ccbot.BaseASObject;

/*
	"current_lockbox_event": {
		"_explicitType": "lockboxes.LockboxEvent",
		"id": 71,
		"name": "Dark Terror Box Event",
		"description": "Try to open Dark Terror Boxes to get BioTerror Bombs.",
		"min_data_version": "20140213_0",
		"is_universal": 1,
		"is_hd": 1,
		"is_android": 1,
		"is_amazon": 1,
		"lockbox_id": 5557,
		"requirement_1_id": 0,
		"requirement_2_id": 0,
		"ui_button_base_cache_key": "OpenCargoBiohazardBoxIcon",
		"lockpick_base_cache_key": "OpenCargoBiohazardBoxIcon",
		"welcome_base_cache_key": "CargoBiohazardBox-Dialog2",
		"splash_base_cache_key": "CargoBiohazardBox-Dialog1",
		"foreground_base_cache_key": "",
		"help_base_cache_key": "CargoBiohazardBox-Dialog3",
		"help_description": "",
		"invasion_description": "",
		"welcome_description_1": "",
		"welcome_description_2": "",
		"welcome_header_1": "",
		"welcome_header_2": "",
		"person_base_cache_key": "lockboxPerson",
		"lockpick_kit_name": "Tool",
		"lockpick_kit_name_plural": "Tools",
		"novice_lockpick_kit_name": "Bare Hands",
		"novice_lockpick_kit_info": "Low Chance",
		"novice_lockpick_open_chance": 0.2,
		"novice_lockpick_kit_money_cost_per_level": 0,
		"novice_lockpick_kit_gold_cost": 0,
		"novice_lockpick_kit_respect_cost_per_level": 0,
		"novice_minutes_to_cooldown": 60,
		"expert_lockpick_kit_name": "Rusty Scissors",
		"expert_lockpick_kit_info": "Medium Chance",
		"expert_lockpick_open_chance": 0.45,
		"expert_lockpick_kit_money_cost_per_level": 100,
		"expert_lockpick_kit_gold_cost": 0,
		"expert_lockpick_kit_respect_cost_per_level": 0,
		"expert_minutes_to_cooldown": 60,
		"master_lockpick_kit_name": "Sharp Knife",
		"master_lockpick_kit_info": "100% Chance",
		"master_lockpick_open_chance": 1,
		"master_lockpick_kit_money_cost_per_level": 0,
		"master_lockpick_kit_gold_cost": 15,
		"master_lockpick_kit_respect_cost_per_level": 0,
		"master_minutes_to_cooldown": 60,
		"is_lockbox_cooldown_cost_per_hour": 0,
		"lockbox_cooldown_gold_cost": 15,
		"minutes_to_freeze": 60,
		"leaderboard_tier1_winners": 25,
		"leaderboard_tier2_winners": 250,
		"rob_drop_chance": 0.3,
		"fight_drop_chance": 0.3,
		"token_id": 5558,
		"start_date": "2014-02-17 11:00:00",
		"duration_hours": 96,
		"min_client_version": "2.6",
		"is_available": 1,
		"event_type": ""
	},
 */
/**
 * @author C&eacute;drik LIME
 */
public class LockboxEvent extends BaseASObject {
	public static String expert_lockpick_kit_money_cost_per_level = "expert_lockpick_kit_money_cost_per_level";//$NON-NLS-1$
	public static String expert_minutes_to_cooldown               = "expert_minutes_to_cooldown";//$NON-NLS-1$
	public static String start_date   = "start_date";//$NON-NLS-1$
	public static String is_available = "is_available";//$NON-NLS-1$

	public LockboxEvent() {
		super();
	}

	public LockboxEvent(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	public LockboxEvent(JSONObject source) {
		super(source);
	}

	@Override
	protected void initialize() {
		put(is_available, 0);
	}

	public boolean isAvailable() {
		try {
			return getInt(is_available) == 1;
		} catch (Exception e) {
			return getBoolean(is_available);
		}
	}

	public void setUnAvailable() {
		put(is_available, 0);
	}

	public int getExpertLockpickKitMoneyCostPerLevel() {
		return getInt(expert_lockpick_kit_money_cost_per_level);
	}

	public int getExpertMinutesToCooldown() {
		return getInt(expert_minutes_to_cooldown);
	}
}
