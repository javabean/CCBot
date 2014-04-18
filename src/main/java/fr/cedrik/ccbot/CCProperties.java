/**
 *
 */
package fr.cedrik.ccbot;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author C&eacute;drik LIME
 */
public class CCProperties extends CompositeConfiguration implements Configuration {
	// CrimeCity.properties keys
	public static final String AUTH_PARAMS_PREFIX             = "auth.param.";//$NON-NLS-1$
	public static final String BUILDINGS_COLLECT_SLEEP_MINUTE = "buildings.collect.sleep";//$NON-NLS-1$
	public static final String BUILDINGS_MINUTELY_COLLECT     = "buildings.collect.minutely";//$NON-NLS-1$
	public static final String BUILDINGS_COLLECT_PROBABILITY  = "buildings.collect.probability";//$NON-NLS-1$
	public static final String LOCKBOX_UNLOCK_SLEEP_MINUTE    = "lockbox.open.sleep";//$NON-NLS-1$
	public static final String BANK_DEPOSIT                   = "bank.deposit";//$NON-NLS-1$
	public static final String GUILD_DONATE_MONEY             = "guild.donate.money";//$NON-NLS-1$
	public static final String SLEEP_TIME_CONCURRENT_ACCESS   = "SEQUENCE_NUM_TOO_LOW.sleep";//$NON-NLS-1$
	public static final String BUILDINGS_COLLECTION_PERSISTENT_FILE = "buildings.collection.file";//$NON-NLS-1$

	private static final Logger log = LoggerFactory.getLogger(CCProperties.class);


	/**
	 * @param fileName
	 */
	public CCProperties(String fileName) throws ConfigurationException {
		super();
		PropertiesConfiguration fileProperties = new PropertiesConfiguration(fileName);
		{
			FileChangedReloadingStrategy reloadingStrategy = new FileChangedReloadingStrategy();
			reloadingStrategy.setRefreshDelay(MINUTES.toMillis(1));
			fileProperties.setReloadingStrategy(reloadingStrategy);
		}
		this.addConfiguration(fileProperties); // primary
		this.addConfiguration(new SystemConfiguration()); // fallback
	}

	/************************************************************************/

	public Object getProperty(String key, Object defaultValue) {
		String val = getString(key);
		return (val == null) ? defaultValue : val;
	}

	/************************************************************************/

	public int getBuildingCollectIntervalMinutes() {
		int seconds = getInt(BUILDINGS_COLLECT_SLEEP_MINUTE, 5); // default: 5 minutes
		seconds = max(1, seconds);
		return seconds;
	}

	public double getBuildingCollectProbability() {
		double proba = getDouble(BUILDINGS_COLLECT_PROBABILITY, 0.2); // default: 20%
		proba = min(max(0, proba), 1);
		return proba;
	}

	public List<Integer> getBuildingsCollectingEveryMinute() {
		List<Integer> minuteCollectBuildings = new ArrayList<Integer>();
		if (StringUtils.isNotBlank(getString(CCProperties.BUILDINGS_MINUTELY_COLLECT))) {
			for (String id : getStringArray(CCProperties.BUILDINGS_MINUTELY_COLLECT)) {
				minuteCollectBuildings.add(Integer.valueOf(id));
			}
		}
		return minuteCollectBuildings;
	}

	public int getLockboxUnlockIntervalMinutes() {
		int minutes = getInt(LOCKBOX_UNLOCK_SLEEP_MINUTE, 5); // default: 5 minutes
		minutes = max(1, minutes);
		return minutes;
	}

	public int getSleepTimeOnConcurrentAccessMinutes() {
		int minutes = getInt(SLEEP_TIME_CONCURRENT_ACCESS, 5); // default: 5 minutes
		minutes = max(0, minutes);
		return minutes;
	}

	public String getBuildingsCollectionPersistentFile() {
		String result = getString(BUILDINGS_COLLECTION_PERSISTENT_FILE, null);
		return StringUtils.defaultIfBlank(result, null);
	}
}
