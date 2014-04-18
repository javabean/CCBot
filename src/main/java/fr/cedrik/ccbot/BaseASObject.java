package fr.cedrik.ccbot;
/**
 *
 */


import java.util.Map;

import org.json.JSONObject;

/**
 * @author C&eacute;drik LIME
 */
public abstract class BaseASObject extends JSONObject /*ASObject*/ {
	public static final String TYPE = "_explicitType";//$NON-NLS-1$

	public BaseASObject() {
		super();
		setType(this.getClass().getName());
		initialize();
	}

	public BaseASObject(Map<? extends String, ? extends Object> m) {
		super(m);
		setType(this.getClass().getName());
//		this.putAll(m);
	}

	public BaseASObject(JSONObject source) {
		super();
		if (! this.getClass().getName().equals(source.getString(TYPE))) {
			throw new IllegalArgumentException(source.getString(TYPE));
		}
		String[] names = JSONObject.getNames(source);
//		setType(this.getClass().getSimpleName());
		for (int i = 0; i < names.length; ++i) {
			try {
				this.putOnce(names[i], source.opt(names[i]));
			} catch (Exception ignore) {
			}
		}
	}

	protected void initialize() {
	}

	/**
	 * get the named type, if any.  (otherwise, return null, implying it is unnamed).
	 * @return the type.
	 */
	public final String getType() {
		return this.getString(TYPE);
//		return super.getType();
	}

	/**
	 * Sets the named type.  <br/>
	 * This operation is mostly meaningless on an object that came in off the wire,
	 * but will be helpful for objects that will be serialized out to Flash.
	 * @param type the type of the object.
	 */
	public final void setType(String type) {
		this.put(TYPE, type);
//		super.setType(type);
	}
}
