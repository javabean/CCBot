/**
 *
 */
package fr.cedrik.ccbot;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PreDestroy;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author C&eacute;drik LIME
 */
public class BuildingCollectionManager {
	private static final Logger log = LoggerFactory.getLogger(BuildingCollectionManager.class);

	private final CCProperties props;
	private buildings.PlayerBuilding[] buildings;
	private final Map<Integer, Long> buildingCollectDates = new HashMap<>();
//	protected final SortedMap<Integer, Long> buildingCollectDuration = new ConcurrentSkipListMap<>();
	/**
	 * Key: building ID
	 * Value: list of (min, max) collection duration, in seconds
	 */
	private final FileConfiguration buildingCollectDuration = new PropertiesConfiguration();

	public BuildingCollectionManager(CCProperties props) {
		this.props = props;
		buildingCollectDuration.setAutoSave(false);
		if (props.getBuildingsCollectionPersistentFile() != null) {
			try {
				buildingCollectDuration.setFileName(props.getBuildingsCollectionPersistentFile());
				buildingCollectDuration.load();
			} catch (ConfigurationException ignore) {
				log.info("Can not load {}", props.getBuildingsCollectionPersistentFile());
			}
		}
	}

	public void setBuildings(buildings.PlayerBuilding[] buildings) {
		this.buildings = buildings;
	}

	public buildings.PlayerBuilding getBuilding(int buildingId) {
		if (buildings == null) {
			throw new IllegalStateException();
		}
		for (buildings.PlayerBuilding building : buildings) {
			if (building.getUniqueId() == buildingId) {
				return building;
			}
		}
		throw new IllegalArgumentException(Integer.toString(buildingId));
	}

	/**
	 * List of buildings IDs that can be harvested every minute (server bug)
	 */
	public boolean isBuildingCollectingEveryMinute(buildings.PlayerBuilding building) {
		return props.getBuildingsCollectingEveryMinute().contains(building.getUniqueId()) // forced collection
				|| building.getGeneratedPlayerBuildingValues().getHoursToOutput() == 1; // dynamically allow 1-hour buildings
	}

	@SuppressWarnings("boxing")
	public boolean canCollect(buildings.PlayerBuilding building) {
		boolean canCollect = building.canCollect() || isBuildingCollectingEveryMinute(building) || ThreadLocalRandom.current().nextDouble() < 0.1;//TODO put in properties
		Long collectDate = buildingCollectDates.get(building.getUniqueId());
		long collectMinDuration = SECONDS.toMillis(getBuildingCollectMinDuration(building));
		long collectMaxDuration = SECONDS.toMillis(getBuildingCollectMaxDuration(building));
		if (collectDate != null && collectMaxDuration != Long.MAX_VALUE) {
			long nextCollectionDate = collectDate + collectMaxDuration;
			canCollect |= nextCollectionDate < System.currentTimeMillis();
		}
		if (collectDate != null && collectMinDuration != 0) {
			long nextCollectionDate = collectDate + collectMinDuration;
			canCollect &= nextCollectionDate < System.currentTimeMillis();
		}
		log.debug("canCollect building {}: min: {} max: {} result: {}",
				building.getUniqueId(), MILLISECONDS.toMinutes(collectMinDuration), MILLISECONDS.toMinutes(collectMaxDuration), canCollect);
		return canCollect;
	}

	public long getBankDepositAmount() {
		long amount = 0;
		for (buildings.PlayerBuilding building : buildings) {
			boolean canCollect = building.canCollect() || isBuildingCollectingEveryMinute(building);
			if (canCollect) {
				amount += building.getGeneratedPlayerBuildingValues().getFullHarvestQuantity();
			}
		}
		return (long) (amount * 1.1);//FIXME magic number
	}

	public void gatherStatistics(List<buildings.PlayerBuilding> buildingsToCollect, List<buildings.CollectResult> collectResults) {
		boolean onlyWithId = true;
		if (buildingsToCollect.size() != collectResults.size()) {
			log.debug("gatherStatistics: buildingsToCollect.size() != collectResults.size()");
			onlyWithId = false; // server is broken...
		}
		int index = 0;
		try {
			for (buildings.PlayerBuilding building : buildingsToCollect) {
				buildings.CollectResult collectResult = collectResults.get(index);
				if (collectResult.getSuccess()) {
					if (building.getUniqueId() != collectResult.getPlayerBuildingId()) {
						// server is broken...
						break;
					}
					assert building.getUniqueId() == collectResult.getPlayerBuildingId();
					builingCollected(building.getUniqueId());
				} else if ( (!onlyWithId) && "NOT_HARVESTABLE".equals(collectResult.getReason())) {
					builingNotCollected(building.getUniqueId());
				}
				++index;
			}
		} catch (IndexOutOfBoundsException ignoreServerIsBroken) {
		}
	}

	@SuppressWarnings("boxing")
	protected void builingCollected(final int buildingId) {
		long currentDate = System.currentTimeMillis();
		Long collectDate = buildingCollectDates.get(buildingId);
		long collectMinDuration = SECONDS.toMillis(getBuildingCollectMinDuration(buildingId));
		long collectMaxDuration = SECONDS.toMillis(getBuildingCollectMaxDuration(buildingId));
		if (collectDate == null) {
			buildingCollectDates.put(buildingId, currentDate);
		} else {
			buildingCollectDates.put(buildingId, currentDate);
			collectMaxDuration = min(collectMaxDuration, currentDate-collectDate);
			if (collectMaxDuration <= collectMinDuration) {
				// probably end of upgrade
				collectMinDuration = 0;
				setBuildingCollectMinDuration(buildingId, (int)MILLISECONDS.toSeconds(collectMinDuration));
			}
			setBuildingCollectMaxDuration(buildingId, (int)MILLISECONDS.toSeconds(collectMaxDuration));
		}
	}

	@SuppressWarnings("boxing")
	protected void builingNotCollected(final int buildingId) {
		long currentDate = System.currentTimeMillis();
		Long collectDate = buildingCollectDates.get(buildingId);
		long collectMinDuration = SECONDS.toMillis(getBuildingCollectMinDuration(buildingId));
		long collectMaxDuration = SECONDS.toMillis(getBuildingCollectMaxDuration(buildingId));
		if (collectDate == null) {
			buildingCollectDates.put(buildingId, currentDate);
		} else {
			buildingCollectDates.put(buildingId, currentDate);
			collectMinDuration = max(collectMinDuration, currentDate-collectDate);
			if (collectMinDuration >= collectMaxDuration) {
				// probably being upgraded
				collectMinDuration = 0;
			}
			setBuildingCollectMinDuration(buildingId, (int)MILLISECONDS.toSeconds(collectMinDuration));
		}
	}

	@PreDestroy
	public void logAndSaveBuildingCollectionStatistics() {
		if (props.getBuildingsCollectionPersistentFile() != null) {
			try {
				buildingCollectDuration.setFileName(props.getBuildingsCollectionPersistentFile());
				buildingCollectDuration.save();
			} catch (ConfigurationException e) {
				log.warn("Can not save to {}", props.getBuildingsCollectionPersistentFile(), e);
			}
		}
		if (log.isInfoEnabled()) {
			Iterator<String> keys = buildingCollectDuration.getKeys();
			while (keys.hasNext()) {
				int key = Integer.parseInt(keys.next());
//				log.info("Building {}: min: {} max: {} minutes", key, SECONDS.toMinutes(getBuildingCollectMinDuration(key)), SECONDS.toMinutes(getBuildingCollectMaxDuration(key)));
				log.debug(String.format(
						"Building %3s: min: %3d max: %4d minutes",
						key, SECONDS.toMinutes(getBuildingCollectMinDuration(key)), SECONDS.toMinutes(getBuildingCollectMaxDuration(key))));
			}
		}
	}

	protected int getBuildingCollectMinDuration(buildings.PlayerBuilding building) {
		return getBuildingCollectDurations(building.getUniqueId())[0];
	}
	protected int getBuildingCollectMinDuration(int buildingId) {
		return getBuildingCollectDurations(buildingId)[0];
	}
	protected void setBuildingCollectMinDuration(buildings.PlayerBuilding building, int durationSeconds) {
		setBuildingCollectMinDuration(building.getUniqueId(), durationSeconds);
	}
	protected void setBuildingCollectMinDuration(int buildingId, int durationSeconds) {
		log.trace("setBuilding: {} CollectMinDuration: ", buildingId, durationSeconds);
		int[] durations = getBuildingCollectDurations(buildingId);
		durations[0] = durationSeconds;
		buildingCollectDuration.setProperty(Integer.toString(buildingId), durations);
	}

	protected int getBuildingCollectMaxDuration(buildings.PlayerBuilding building) {
		return getBuildingCollectDurations(building.getUniqueId())[1];
	}
	private int getBuildingCollectMaxDuration(int buildingId) {
		return getBuildingCollectDurations(buildingId)[1];
	}
	protected void setBuildingCollectMaxDuration(buildings.PlayerBuilding building, int durationSeconds) {
		setBuildingCollectMaxDuration(building.getUniqueId(), durationSeconds);
	}
	private void setBuildingCollectMaxDuration(int buildingId, int durationSeconds) {
		log.trace("setBuilding: {} CollectMaxDuration: ", buildingId, durationSeconds);
		int[] durations = getBuildingCollectDurations(buildingId);
		durations[1] = durationSeconds;
		buildingCollectDuration.setProperty(Integer.toString(buildingId), durations);
	}

	private int[] getBuildingCollectDurations(int buildingId) {
		String[] durationsStr = buildingCollectDuration.getStringArray(Integer.toString(buildingId));//TODO debug to see inner type: String[] or List
		if (durationsStr == null || durationsStr.length == 0) {
			return new int[] {0, Integer.MAX_VALUE};
		}
		assert durationsStr.length == 2 : durationsStr.length;
		int[] durations = new int[2];
		durations[0] = Integer.parseInt(durationsStr[0]);
		durations[1] = Integer.parseInt(durationsStr[1]);
		return durations;
	}
}
