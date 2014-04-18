/**
 *
 */


import java.util.HashMap;
import java.util.Map;

import fr.cedrik.ccbot.BaseASObject;

/**
 * @author C&eacute;drik LIME
 */
public class CCLocalPlayerChanges extends BaseASObject {
	public static final String STAMINA      = "stamina";
	public static final String ENERGY       = "energy";
	public static final String MAFIA        = "mafia";
	public static final String PLAYER_ITEM_QTY_DELTAS = "player_item_quantity_deltas";
	public static final String EXPERIENCE   = "experience";
	public static final String MAX_STAMINA  = "maxStamina";
	public static final String GOLD         = "gold";
	public static final String LEVEL        = "level";
	public static final String DEFENSE      = "defense";
	public static final String ATTACK       = "attack";
	public static final String RESPECT      = "respect";
	public static final String MAX_ENERGY   = "maxEnergy";
	public static final String SKILL_POINTS = "skillPoints";
	public static final String MONEY        = "money";
	public static final String LOCK_BOXES   = "lockboxes";

	public CCLocalPlayerChanges() {
		super();
	}

	public CCLocalPlayerChanges(Map<? extends String, ? extends Object> m) {
		super(m);
	}

	@Override
	protected void initialize() {
		put(RESPECT,      Long.valueOf(0));
		put(EXPERIENCE,   Integer.valueOf(0));
		put(ENERGY,       Integer.valueOf(0));
		put(MAX_STAMINA,  Integer.valueOf(0));
		put(SKILL_POINTS, Integer.valueOf(0));
		put(MAFIA,        Integer.valueOf(0));
		put(LEVEL,        Integer.valueOf(0));
		put(MONEY,        Long.valueOf(0));
		put(DEFENSE,      Integer.valueOf(0));
		put(GOLD,         Integer.valueOf(0));
		put(PLAYER_ITEM_QTY_DELTAS, new HashMap<>());//FIXME?
		put(STAMINA,      Integer.valueOf(0));
		put(MAX_ENERGY,   Integer.valueOf(0));
		put(ATTACK,       Integer.valueOf(0));
		put(LOCK_BOXES,   Integer.valueOf(0));
	}
}
