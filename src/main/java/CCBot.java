/**
 *
 */

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import fr.cedrik.ccbot.BuildingCollectionManager;
import fr.cedrik.ccbot.CCProperties;
import fr.cedrik.ccbot.GameDownException;
import fr.cedrik.spring.http.client.HttpRequestExecutor;
import fr.cedrik.spring.http.client.SimpleClientHttpRequestFactory;

/**
 * @author C&eacute;drik LIME
 */
public class CCBot {
//	private static final String URL = "http://ios.crimecitygame.com/ccios/index.php/amf_gateway";
	private static final String URL_PREFIX_IOS     = "http://ios.crimecitygame.com/ccios/index.php/json_gateway?svc=";
	private static final String URL_PREFIX_ANDROID = "http://android.crimecitygame.com/ccand//index.php/json_gateway?svc=";
	private static final String IOS_CONFIG_URL    = "http://ios.crimecitygame.com/ccios/index.php/config";
	private static final String CONFIG_USER_AGENT = "GREE-RPG/1.0 CC/4.0.1-401-20140415_11 Apple iOS/7.1 \"iPhone 5\"";//FIXME
	private static final Logger log = LoggerFactory.getLogger(CCBot.class);

	private final CCContext context;

	/**
	 *
	 */
	public CCBot(CCProperties props) {
		context = new CCContext(props);
	}

	public void run() throws IOException, InterruptedException {
		Authentification auth = new Authentification(context);
		JSONObject reply = auth.call();
		log.info("status: " + reply.get("status"));
		context.sequenceNum = auth.getSequenceNum();
		++context.sequenceNum;
		context.session = auth.getSessionFromAuthResponse();
		context.session.setStart_sequence_num(context.sequenceNum);
		log.debug("Session: {}", context.session);
		context.playerPlayer = auth.getPlayerPlayer();
		log.debug("player.Player: {}", context.playerPlayer);
		buildings.PlayerBuilding[] buildings = auth.getBuildingsPlayerBuilding();
		context.setBuildings(buildings);
		context.guildDetails = auth.getGuildsGuildDetailsFromAuthResponse();
		context.playerLockboxEvent = auth.getLockboxesPlayerLockboxEvent();
		context.lockboxEvent = auth.getLockboxesLockboxEvent();
		auth = null;

//		context.sequenceNum = new BuyUziTokensAndDonateToGuild(context, 15000).call();
//		context.session.setStart_sequence_num(context.sequenceNum);
//		for (int i = 0; i < 50; ++i) {
//			context.sequenceNum = new SummonRaidBoss(context, 70).call(); // level 70: 2 force-attacks, but low loot
//			context.session.setStart_sequence_num(context.sequenceNum);
//			context.sequenceNum = new FightRaidBoss(context, 80).call();
//			context.session.setStart_sequence_num(context.sequenceNum);
//			sleep(2, SECONDS);
//		}
//		{
//			context.sequenceNum = new WorldDominationAttackCommandCenter(context, 500).call(); // about 70 points per hit
//			context.session.setStart_sequence_num(context.sequenceNum);
//		}
//		if (true) {
//			System.exit(0);
//			return;
//		}

		for (int i = 0; i < DAYS.toMinutes(1); ++i) {//XXX
			sleep(context.props.getBuildingCollectIntervalMinutes(), MINUTES);

			context.sequenceNum = new CollectBuildings(context, buildings).call();
			context.session.setStart_sequence_num(context.sequenceNum);

			checkConcurrentAccess();

			if (context.lockboxEvent.isAvailable()
					&& context.playerLockboxEvent.canUnlockBox()
					&& context.lastLockboxUnlockTime + MINUTES.toMillis(context.props.getLockboxUnlockIntervalMinutes()) < System.currentTimeMillis()) {
				sleep(1, SECONDS);
				context.sequenceNum = new UnlockBox(context).call();
				context.session.setStart_sequence_num(context.sequenceNum);

				checkConcurrentAccess();
			}

			long bankDepositAmount = context.props.getLong(CCProperties.BANK_DEPOSIT, -1);
			if (bankDepositAmount == -1) {
				bankDepositAmount = context.buildingCollectionManager.getBankDepositAmount();
			}
			if (bankDepositAmount > 0) {
				sleep(1, SECONDS);
				context.sequenceNum = new DepositMoneyInBank(context, bankDepositAmount).call();
				context.session.setStart_sequence_num(context.sequenceNum);

				checkConcurrentAccess();
			}

			long guildMoneyDonateAmount = context.props.getLong(CCProperties.GUILD_DONATE_MONEY);
			if (guildMoneyDonateAmount > 0) {
				sleep(1, SECONDS);
				context.sequenceNum = new DonateMoneyToGuild(context, guildMoneyDonateAmount).call();
				context.session.setStart_sequence_num(context.sequenceNum);

				checkConcurrentAccess();
			}
		}//XXX
	}

	protected void checkConcurrentAccess() throws InterruptedException {
		if (context.concurrentAccessDetected) {
			sleep(context.props.getSleepTimeOnConcurrentAccessMinutes(), MINUTES);
			context.concurrentAccessDetected = false;
		}
	}

	public void logAndSaveBuildingCollectionStatistics() {
		printStatusString("Building collections: minimum time to collect");
		context.buildingCollectionManager.logAndSaveBuildingCollectionStatistics();
	}

	protected static void sleep(long sleepTime, TimeUnit unit) throws InterruptedException {
		long sleepTimeMillis = (long) (unit.toMillis(sleepTime) * ThreadLocalRandom.current().nextDouble(1.01, 1.1));
		sleepTimeMillis += ThreadLocalRandom.current().nextLong(200, SECONDS.toMillis(1));
		log.trace("Sleeping for {} ms", sleepTimeMillis);
		Thread.sleep(sleepTimeMillis);
	}

	public static void printStatusString(String msg) {
		String dateTime = javax.xml.bind.DatatypeConverter.printDateTime(Calendar.getInstance());
		String overlay = "[" + dateTime + "][ " + msg + " ]";
		StringBuilder result = new StringBuilder("============================================================================");
		result.replace(3, overlay.length(), overlay);
		log.info(result.toString());
	}

	/************************************************************************/
	/************************************************************************/

	public static class CCContext {
		public String urlPrefix;
		protected boolean concurrentAccessDetected = false;
		public CCProperties props;
		public int sequenceNum;
		public Session session;
		public player.Player playerPlayer;
		public guilds.GuildDetails guildDetails;
		public lockboxes.PlayerLockboxEvent playerLockboxEvent;
		public lockboxes.LockboxEvent lockboxEvent;
		public long lastLockboxUnlockTime = 0;
		public final BuildingCollectionManager buildingCollectionManager;

		protected final HttpRequestExecutor httpRequestExecutor = new HttpRequestExecutor();
		protected final CookieManager cookieManager = new CookieManager();

		static {
//			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			// Note: the following line means this application must be single-user only!
			/*
			 * Non-Oracle JVM (i.e. OpenJDK 6) do not implement {@link java.net.CookieManager} correctly.
			 * And anyway, having a JVM singleton ({@link CookieHandler#setDefault(java.net.CookieHandler)}) for cookies management sucks.
			 * Thus we need to manage cookies manually...
			 */
			//CookieHandler.setDefault(cookieManager);
		}

		public CCContext(CCProperties props) {
			this.props = props;
			this.buildingCollectionManager = new BuildingCollectionManager(props);
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			{
				SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
				httpRequestFactory.setAllowUserInteraction(false);
				httpRequestFactory.setUseCaches(false);
				httpRequestFactory.setConnectTimeout((int)SECONDS.toMillis(15));
				httpRequestFactory.setReadTimeout((int)SECONDS.toMillis(80));
				httpRequestExecutor.setRequestFactory(httpRequestFactory);
			}
		}

		public void setBuildings(buildings.PlayerBuilding[] buildings) {
			this.buildingCollectionManager.setBuildings(buildings);
		}
	}

	/************************************************************************/
	/************************************************************************/

	static abstract class BaseCommandExecutor {
		protected CCContext context;
		protected JSONArray commands;
		protected JSONObject reply;

		public BaseCommandExecutor(CCContext context, JSONArray commands) {
			this.context = context;
			this.commands = commands;
		}
		public BaseCommandExecutor(CCContext context, List<Command> commandsList) {
			this.context = context;
			this.commands = new JSONArray(commandsList);
		}
		public BaseCommandExecutor(CCContext context, Command... commandsList) {
			this.context = context;
			if (commandsList != null) {
				this.commands = new JSONArray(commandsList);
			} else {
				this.commands = new JSONArray();
			}
		}


//		static AMFConnection getAMFConnection() throws ClientStatusException {
//			AMFConnection amfConnection = new AMFConnection();
//			amfConnection.addHttpRequestHeader("User-Agent", "CocoaAMF");
//			amfConnection.addHttpRequestHeader("Content-Type", "application/x-amf");
//			amfConnection.connect(URL);
//			return amfConnection;
//		}

		/**
		 * @see HttpRequestExecutor#createRequest(URL, HttpMethod, Map, Map)
		 */
		protected ClientHttpRequest createRequest(String svc, String content) throws IOException {
			URL url = new URL(context.urlPrefix + svc);
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			if (URL_PREFIX_IOS.equals(context.urlPrefix)) {
				httpHeaders.set("X-Timestamp", Long.toString(System.currentTimeMillis()/1000));
//				httpHeaders.set("X-Signature", "1697fc7c4541d92ae4a9b756cfc53592");
				httpHeaders.set("User-Agent", "Gree");
			}
			// put back cookies in httpHeaders
			try {
				Map<String, List<String>> cookies = context.cookieManager.get(url.toURI(), Collections.<String,List<String>>emptyMap());
				httpHeaders.putAll(cookies);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			// create http request
			ClientHttpRequest request = context.httpRequestExecutor.createRequest(url, HttpMethod.POST, null, httpHeaders);
			if (content != null && ! content.isEmpty()) {
				OutputStream body = request.getBody();
				try {
					body.write(content.getBytes(context.httpRequestExecutor.getCharset(request)));
				} finally {
					body.close();
				}
			}
			return request;
		}

		protected JSONObject executeCommands(String svc, JSONArray commands) throws IOException {
			if (commands.length() != 0) {
				JSONArray requestParams = new JSONArray();
				requestParams.put(context.session);
				requestParams.put(commands);
				requestParams.put(context.playerPlayer);
				{
					JSONObject param3 = new JSONObject();
					param3.put("using", context.session.get(Session.game_data_version));
					param3.put("active", context.session.get(Session.game_data_version));
					requestParams.put(param3);
				}
				{
					JSONObject param4 = new JSONObject();
					param4.put("method", "player_sync");
					param4.put("transactionTime", "970");//FIXME
					param4.put("ConnectionType", "WIFI");
					param4.put("numCommands", 1);
					param4.put("httpResponseStatus", "200");
					param4.put("isTimeout", false);
					param4.put("status", "OK");
					param4.put("service", "profile.profile");
					param4.put("transactionStartTime", Long.toString(System.currentTimeMillis() / 1000));
					param4.put("isError", false);
					requestParams.put(param4);
				}
				assert requestParams.length() == 5 : requestParams.length();
				log.trace(">>> {}", requestParams);
				ClientHttpRequest request = createRequest(svc, requestParams.toString());
				log.debug("Executing {} commands", requestParams.length());
				try (ClientHttpResponse response = request.execute(); InputStream body = response.getBody()) {
					context.cookieManager.put(request.getURI(), response.getHeaders());
					String bodyStr = IOUtils.toString(body, context.httpRequestExecutor.getCharset(response));
					reply = new JSONObject(bodyStr);
					log.trace("<<< {}", reply);
					log.info("status: " + reply.get("status"));
					checkConcurrentAccess();
				}
			} else {
				log.debug("No command to execute");
			}
			return reply;
		}

		protected void checkConcurrentAccess() {
			if ("SEQUENCE_NUM_TOO_LOW".equals(reply.get("status"))) {
				log.trace("SEQUENCE_NUM_TOO_LOW: {} -> {}", context.sequenceNum, reply.get("server_sequence_num"));
				context.sequenceNum = reply.getInt("server_sequence_num");
				context.concurrentAccessDetected = true;
			}
			if ("GAME_DOWN".equals(reply.get("status"))) {
				log.error("GAME_DOWN");
				throw new GameDownException();
			}
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class CollectBuildings extends BaseCommandExecutor implements Callable<Integer> {
		protected buildings.PlayerBuilding[] playerBuildings;
		public CollectBuildings(CCContext context, buildings.PlayerBuilding[] buildings) {
			super(context);
			this.playerBuildings = buildings;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray collectCommands = new JSONArray();
			List<buildings.PlayerBuilding> buildingsToCollect = new ArrayList<>();
			boolean allowBuildingsCollection = Math.random() < context.props.getBuildingCollectProbability();
			for (buildings.PlayerBuilding building : playerBuildings) {
				if ( ! ( context.buildingCollectionManager.isBuildingCollectingEveryMinute(building) || (allowBuildingsCollection && context.buildingCollectionManager.canCollect(building)) ) ) {
					continue;
				}
				log.debug("Setting to collect building {}", building);
				buildingsToCollect.add(building);
				Command command = new Command();
				command.setService("buildings.buildings");
				command.setMethod("collect");
				command.setSequenceNum(context.sequenceNum);
				context.session.setEnd_sequence_num(context.sequenceNum);
				++context.sequenceNum;
				command.getPlayerDelta().put(CCLocalPlayerChanges.MONEY, Long.valueOf((long)building.getGeneratedPlayerBuildingValues().getActualHarvestQuantity()));
				command.setParams(Integer.valueOf(building.getUniqueId()), Integer.valueOf((int)building.getGeneratedPlayerBuildingValues().getActualHarvestQuantity()),
						command.get(Command.TRANSACTION_TIME));
				collectCommands.put(command);
			}

			if (collectCommands.length() != 0) {
				printStatusString("buildings.buildings.collect " + collectCommands.length());
				reply = executeCommands("BatchController.call", collectCommands);
				if (! context.concurrentAccessDetected) {
					List<buildings.CollectResult> collectResults = getBuildingsCollectResultFromCollectResponse();
					context.buildingCollectionManager.gatherStatistics(buildingsToCollect, collectResults);
					for (buildings.CollectResult collectResult : collectResults) {
						if (collectResult.getSuccess()) {
							log.info("" + collectResult.getPlayerBuildingId() + ": " + collectResult.getSuccess());
						} else {
							log.info("" + collectResult.getReason());
						}
					}
				}
			} else {
				log.info("No building to collect!");
			}
			return Integer.valueOf(context.sequenceNum);
		}

		List<buildings.CollectResult> getBuildingsCollectResultFromCollectResponse() {
			List<buildings.CollectResult> result = new ArrayList<buildings.CollectResult>();
			JSONArray responses = reply.getJSONArray("responses");
			try {//XXX DEBUG
			for (int i = 0; i < responses.length(); ++i) {
				CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(i));
				JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
				boolean success = returnValue.getBoolean("success");
				buildings.CollectResult collectResult;
				if (success) {
					collectResult = new buildings.CollectResult(returnValue.getJSONObject("result"));
				} else {
					collectResult = new buildings.CollectResult();
					collectResult.put(buildings.CollectResult.success, success);
					collectResult.put(buildings.CollectResult.reason, returnValue.get(buildings.CollectResult.reason));
				}
				result.add(collectResult);
			}
			} catch (org.json.JSONException debug) {
				System.err.println("responses: " + responses);
				throw debug;
			}
			return result;
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class DepositMoneyInBank extends BaseCommandExecutor implements Callable<Integer> {
		protected long amount;
		public DepositMoneyInBank(CCContext context, long amount) {
			super(context);
			this.amount = amount;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray commands = new JSONArray();
			Command command = new Command();
			command.setService("profile.profile");
			command.setMethod("deposit");
			command.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			command.setParams(Long.toString(amount));
			commands.put(command);

			printStatusString("profile.profile.deposit " + amount);
			reply = executeCommands("BatchController.call", commands);
			return Integer.valueOf(context.sequenceNum);
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class WithdrawMoneyFromBank extends BaseCommandExecutor implements Callable<Integer> {
		protected long amount;
		public WithdrawMoneyFromBank(CCContext context, long amount) {
			super(context);
			this.amount = amount;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray commands = new JSONArray();
			Command command = new Command();
			command.setService("profile.profile");
			command.setMethod("withdraw");
			command.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			command.setParams(Long.toString(amount));
			commands.put(command);

			printStatusString("profile.profile.withdraw " + amount);
			reply = executeCommands("BatchController.call", commands);
			return Integer.valueOf(context.sequenceNum);
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class DonateMoneyToGuild extends BaseCommandExecutor implements Callable<Integer> {
		protected long amount;
		public DonateMoneyToGuild(CCContext context, long amount) {
			super(context);
			this.amount = amount;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray commands = new JSONArray();
			Command command = new Command();
			command.setService("guilds.guilds");
			command.setMethod("update_guild_resources");
			command.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			Map<String, Object> params0 = new LinkedHashMap<String, Object>();
			{
				params0.put("donation_type", "money");
				params0.put("player_id", context.playerPlayer.get(player.Player.player_id));
				params0.put("guild_id", context.guildDetails.getId());
				params0.put("donation_type_id", null);
				params0.put("donation_amount", Long.valueOf(amount));
			}
			command.setParams(params0);
			commands.put(command);

			printStatusString("guilds.guilds.update_guild_resources " + amount);
			reply = executeCommands("BatchController.call", commands);
			return Integer.valueOf(context.sequenceNum);
		}
	}

	/************************************************************************/
	/************************************************************************/


	static class UnlockBox extends BaseCommandExecutor implements Callable<Integer> {
		public UnlockBox(CCContext context) {
			super(context);
		}

		@Override
		public Integer call() throws IOException {
			JSONArray unlockCommands = new JSONArray();
			if (context.lockboxEvent.isAvailable()
					&& context.playerLockboxEvent.canUnlockBox()
					&& context.lastLockboxUnlockTime + MINUTES.toMillis(context.props.getLockboxUnlockIntervalMinutes()) < System.currentTimeMillis()) {
				Command command = new Command();
				command.setService("lockboxes.lockboxes");
				command.setMethod("open_lockbox");
				command.setSequenceNum(context.sequenceNum);
				context.session.setEnd_sequence_num(context.sequenceNum);
				++context.sequenceNum;
				int cost = context.lockboxEvent.getExpertLockpickKitMoneyCostPerLevel() * context.playerPlayer.getLevel();
				command.setParams(Integer.valueOf(context.playerLockboxEvent.getEventId()), 2, cost, 0, 0);//FIXME 2
				unlockCommands.put(command);
			}

			if (unlockCommands.length() != 0) {
				printStatusString("lockboxes.lockboxes.open_lockbox");
				reply = executeCommands("BatchController.call", unlockCommands);
				if (! context.concurrentAccessDetected) {
					log.info("success: " + getLockboxSuccess());
					if (getLockboxSuccess()) {
						log.info(String.valueOf(getLockboxResult()));
						context.lastLockboxUnlockTime = System.currentTimeMillis();
					} else {
						JSONArray responses = reply.getJSONArray("responses");
						CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
						JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
						log.info(String.valueOf(returnValue.get("reason")));
						if ("NO_CURRENT_EVENT".equals(returnValue.get("reason"))) { // end of event
							context.lockboxEvent.setUnAvailable();
						}
					}
				}
			} else {
				log.info("No box to open!");
			}
			return Integer.valueOf(context.sequenceNum);
		}

		public boolean getLockboxSuccess() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
			boolean success = returnValue.getBoolean("success");
			return success;
		}

		public lockboxes.LockboxResult getLockboxResult() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
			if (returnValue.optJSONObject("lockbox_result") != null) {
				return new lockboxes.LockboxResult(returnValue.getJSONObject("lockbox_result"));
			} else {
				return null;
			}
		}
	}


	/************************************************************************/
	/************************************************************************/


	static class Scratcher extends BaseCommandExecutor implements Callable<Integer> {
		public Scratcher(CCContext context) {
			super(context);
		}

		@Override
		public Integer call() throws IOException {
			JSONArray scratchCommands = new JSONArray();
			if (context.playerPlayer.canScratch()) {
				Command command = new Command();
				command.setService("scratchers.scratchers");
				command.setMethod("finish_scratch");
				command.setSequenceNum(context.sequenceNum);
				context.session.setEnd_sequence_num(context.sequenceNum);
				++context.sequenceNum;
				command.setParams(1002, 1);//FIXME
				scratchCommands.put(command);
			}

			if (scratchCommands.length() != 0) {
				printStatusString("scratchers.scratchers.finish_scratch");
				reply = executeCommands("BatchController.call", scratchCommands);
			} else {
				log.info("No scratching yet!");
			}
			return Integer.valueOf(context.sequenceNum);
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class BuyUziTokensAndDonateToGuild extends BaseCommandExecutor implements Callable<Integer> {
		protected int amount;
		public BuyUziTokensAndDonateToGuild(CCContext context, int amount) {
			super(context);
			this.amount = amount;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray commands = new JSONArray();
			context.session.setStart_sequence_num(context.sequenceNum);
			Command buyCommand = new Command();
			buyCommand.setService("raidboss.raidbosses");
			buyCommand.setMethod("buy_tokens");
			buyCommand.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			buyCommand.setParams(amount, "gold");
			commands.put(buyCommand);
			Command donateCommand = new Command();
			donateCommand.setService("raidboss.raidbosses");
			donateCommand.setMethod("donate_token");
			donateCommand.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			donateCommand.setParams(amount);
			commands.put(donateCommand);

			printStatusString("raidboss.raidbosses.buy_tokens + donate_token " + amount);
			reply = executeCommands("BatchController.call", commands);
			log.info("{}", reply.getJSONArray("responses"));
			return Integer.valueOf(context.sequenceNum);
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class SummonRaidBoss extends BaseCommandExecutor implements Callable<Integer> {
		/**
		 * number between 131 (level 5) and 150 (level 100): lvl/5+constant
		 */
		private static final int LEVEL_CONSTANT = 151;
		protected int level;
		/**
		 * @param level between (level 5) and (level 100)
		 */
		public SummonRaidBoss(CCContext context, int level) {
			super(context);
			if (level < 5 || level > 100 || level % 5 != 0) {
				throw new IllegalArgumentException(Integer.toString(level));
			}
			this.level = level;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray commands = new JSONArray();
			context.session.setStart_sequence_num(context.sequenceNum);
			Command command = new Command();
			command.setService("raidboss.raidbosses");
			command.setMethod("summon_boss");
			command.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			command.setParams(level/5+LEVEL_CONSTANT);
			commands.put(command);

			printStatusString("raidboss.raidbosses.summon_boss " + level);
			reply = executeCommands("BatchController.call", commands);
			boolean success = ((JSONObject)reply.getJSONArray("responses").get(0)).getJSONObject("return_value").getBoolean("success");
//			log.info("success: {}", success);
			if (! success) {
				throw new IllegalStateException("Not enought Uzi Tokens?");
			}
			log.info("{}", ((JSONObject)reply.getJSONArray("responses").get(0)).getJSONObject("return_value"));
			return Integer.valueOf(context.sequenceNum);
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class FightRaidBoss extends BaseCommandExecutor implements Callable<Integer> {
		protected int amount;
		public FightRaidBoss(CCContext context, int maxAmount) {
			super(context);
			this.amount = maxAmount;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray commands = new JSONArray();
			Command command = new Command();
			command.setService("raidboss.raidbosses");
			command.setMethod("get_active_fights");
			command.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			commands.put(command);

			printStatusString("raidboss.raidbosses.get_active_fights");
			reply = executeCommands("BatchController.call", commands);
			int fightId = getFightId();
			if (fightId != Integer.MIN_VALUE) {
				commands = new JSONArray();
				context.session.setStart_sequence_num(context.sequenceNum);
				command = new Command();
				command.setService("raidboss.raidbosses");
				command.setMethod("attack");
				command.setSequenceNum(context.sequenceNum);
				context.session.setEnd_sequence_num(context.sequenceNum);
				++context.sequenceNum;
				command.setParams(fightId, "power_attack");
				commands.put(command);

				for (int i = 1; i <= amount; ++i) {
					context.session.setStart_sequence_num(context.sequenceNum);
					command.setSequenceNum(context.sequenceNum);
					++context.sequenceNum;
					printStatusString("raidboss.raidbosses.attack " + i + '/' + amount);
					reply = executeCommands("BatchController.call", commands);
					log.info("{}", reply.getJSONArray("responses"));
					if (checkEndOfFight()) {
						break;
					}
					checkHealth();
				}
			} else {
				log.info("No raid boss to fight!");
			}
			return Integer.valueOf(context.sequenceNum);
		}

		protected int getFightId() {
			JSONObject response = (JSONObject) reply.getJSONArray("responses").get(0);
			if (! "OK".equals(response.getString("status"))) {
				return Integer.MIN_VALUE;
			}
			response = response.getJSONObject("return_value");
			if (! response.getBoolean("success")) {
				return Integer.MIN_VALUE;
			}
			try {
				return ((JSONObject) response.getJSONArray("active_fights").get(0)).getInt("fight_id");
			} catch (JSONException e) {
				return Integer.MIN_VALUE;
			}
		}

		protected void checkHealth() throws IOException {
			JSONObject response = ((JSONObject) reply.getJSONArray("responses").get(0)).getJSONObject("return_value");
			if ( ! response.getBoolean("success") && "HEALTH_NOT_ENOUGH".equals(response.getString("reason"))) {
				JSONArray commands = new JSONArray();
				Command command = new Command();
				command.setService("raidboss.raidbosses");
				command.setMethod("refill_health");
				command.setSequenceNum(context.sequenceNum);
				context.session.setEnd_sequence_num(context.sequenceNum);
				++context.sequenceNum;
				commands.put(command);
				printStatusString("raidboss.raidbosses.refill_health");
				reply = executeCommands("BatchController.call", commands);
				boolean success = ((JSONObject)reply.getJSONArray("responses").get(0)).getJSONObject("return_value").getBoolean("success");
				log.info("success: {}", success);
			}
		}

		protected boolean checkEndOfFight() {
			JSONObject response = ((JSONObject) reply.getJSONArray("responses").get(0)).getJSONObject("return_value");
			try {
				return (response.getBoolean("success") && response.getBoolean("is_stale"));
			} catch (JSONException e) {
				return false;
			}
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class WorldDominationAttackCommandCenter extends BaseCommandExecutor implements Callable<Integer> {
		protected int amount;
		public WorldDominationAttackCommandCenter(CCContext context, int maxAmount) {
			super(context);
			this.amount = maxAmount;
		}

		@Override
		public Integer call() throws IOException {
			JSONArray commands = new JSONArray();
			Command command = new Command();
			command.setService("worlddomination.worlddomination");
			command.setMethod("get_event_details");
			command.setSequenceNum(context.sequenceNum);
			context.session.setEnd_sequence_num(context.sequenceNum);
			++context.sequenceNum;
			commands.put(command);

			printStatusString("worlddomination.worlddomination.get_event_details");
			reply = executeCommands("BatchController.call", commands);
			int gvg_war_id = getGvgWarId();
			String enemy_guild_id = getEnemyGuildId();
			if (gvg_war_id != Integer.MIN_VALUE && enemy_guild_id != null) {
				commands = new JSONArray();
				context.session.setStart_sequence_num(context.sequenceNum);
				command = new Command();
				command.setService("worlddomination.worlddomination");
				command.setMethod("attack_commandcenter");
				command.setSequenceNum(context.sequenceNum);
				context.session.setEnd_sequence_num(context.sequenceNum);
				++context.sequenceNum;
				command.setParams(new JSONObject().put("gvg_war_id", gvg_war_id).put("enemy_guild_id", enemy_guild_id));
				commands.put(command);

				for (int i = 1; i <= amount; ++i) {
					context.session.setStart_sequence_num(context.sequenceNum);
					command.setSequenceNum(context.sequenceNum);
					++context.sequenceNum;
					printStatusString("worlddomination.worlddomination.attack_commandcenter " + i + '/' + amount);
					reply = executeCommands("BatchController.call", commands);
					log.info("{}", reply.getJSONArray("responses"));
					if (checkEndOfWar()) {
						break;
					}
					checkHealth();
				}
			} else {
				log.info("No World Domination event to fight!");
			}
			return Integer.valueOf(context.sequenceNum);
		}

		protected int getGvgWarId() {
			JSONObject response = (JSONObject) reply.getJSONArray("responses").get(0);
			if (! "OK".equals(response.getString("status"))) {
				return Integer.MIN_VALUE;
			}
			response = response.getJSONObject("return_value");
			if (! response.getBoolean("success")) {
				return Integer.MIN_VALUE;
			}
			try {
				return response.getJSONObject("event_details").getJSONObject("wd_guild").getInt("active_gvg_war_id");
			} catch (JSONException e) {
				return Integer.MIN_VALUE;
			}
		}

		protected String getEnemyGuildId() {
			JSONObject response = (JSONObject) reply.getJSONArray("responses").get(0);
			if (! "OK".equals(response.getString("status"))) {
				return null;
			}
			response = response.getJSONObject("return_value");
			if (! response.getBoolean("success")) {
				return null;
			}
			try {
				String ourGuildId = response.getJSONObject("event_details").getJSONObject("wd_guild").getString("guild_id");
				JSONObject guilds = response.getJSONObject("event_details").getJSONObject("recent_battle").getJSONObject("guilds");
				for (String guildName : JSONObject.getNames(guilds)) {
					String guildId = guilds.getJSONObject(guildName).getJSONObject("guild").getString("guild_id");
					if ( ! guildId.equals(ourGuildId)) {
						return guildId;
					}
				}
			} catch (JSONException e) {
				return null;
			}
			return null;
		}

		protected void checkHealth() throws IOException {
			JSONObject response = ((JSONObject) reply.getJSONArray("responses").get(0)).getJSONObject("return_value");
			if ( ! response.getBoolean("success") && "INSUFFICIENT_HEALTH".equals(response.getString("reason"))) {
				JSONArray commands = new JSONArray();
				Command command = new Command();
				command.setService("worlddomination.worlddomination");
				command.setMethod("refill_battle_health");
				command.setSequenceNum(context.sequenceNum);
				context.session.setEnd_sequence_num(context.sequenceNum);
				++context.sequenceNum;
				commands.put(command);
				printStatusString("worlddomination.worlddomination.refill_battle_health");
				reply = executeCommands("BatchController.call", commands);
				boolean success = ((JSONObject)reply.getJSONArray("responses").get(0)).getJSONObject("return_value").getBoolean("success");
				log.info("success: {}", success);
			}
		}

		protected boolean checkEndOfWar() {
			JSONObject response = ((JSONObject) reply.getJSONArray("responses").get(0)).getJSONObject("return_value");
			try {
				String reason = response.getString("reason");
				boolean endOfWar = ( ! response.getBoolean("success") &&
						( "WAR_NOT_ACTIVE".equals(reason) || "INVALID_WAR".equals(reason) || "FORTIFICATION_EXISTS".equals(reason) )
					);
				if (endOfWar) {
					log.error(reason);
				}
				return endOfWar;
			} catch (JSONException e) {
				return false;
			}
		}
	}

	/************************************************************************/
	/************************************************************************/

	static class Authentification extends BaseCommandExecutor implements Callable<JSONObject> {

		private static enum AUTH_TYPE {
			IOS, ANDROID
		}

		public Authentification(CCContext context) {
			super(context);
		}

		@Override
		public JSONObject call() throws IOException {
			printStatusString("Authenticate");
			final String currentStaticDataVersion = getCurrentStaticDataVersion();
			log.info("Current static data version: {}", currentStaticDataVersion);
			JSONObject param1 = new JSONObject();
			param1.put("client_version",     context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.client_version", JSONObject.NULL))
				.put("client_md5",           context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.client_md5", JSONObject.NULL))
				.put("starting_area_name",   context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.starting_area_name", JSONObject.NULL))
				.put("client_build",         context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.client_build", JSONObject.NULL))
				.put("data_connection_type", context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.data_connection_type", JSONObject.NULL))
				.put("previous_client_version", context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.previous_client_version", JSONObject.NULL))
				.put("device_type",          context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.device_type", JSONObject.NULL))
				.put("transaction_time",     Long.toString(System.currentTimeMillis() / 1000))
				.put("os",                   context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.os", JSONObject.NULL))
				.put("game_name",            context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.game_name", JSONObject.NULL))
				.put("client_static_table_data",
					new JSONObject()//TODO client_static_table_data.start_time if present
						.put("active", context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.client_static_table_data.active", currentStaticDataVersion))
						.put("using",  context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.client_static_table_data.using", currentStaticDataVersion)
					)
				)
				.put("game_data_md5",       context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.game_data_md5", JSONObject.NULL))
				.put("game_data_version",   context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.game_data_version", currentStaticDataVersion))
				.put("assets_loaded_level", context.props.getInt(CCProperties.AUTH_PARAMS_PREFIX+"1.assets_loaded_level"))
				.put("session_id",          context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.session_id", JSONObject.NULL))
				.put("market",              context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.market", JSONObject.NULL))
				.put("seconds_from_gmt",    context.props.getDouble(CCProperties.AUTH_PARAMS_PREFIX+"1.seconds_from_gmt"))
				.put("load_source",         context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.load_source", JSONObject.NULL));
			if (context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.force_load_override", JSONObject.NULL) != JSONObject.NULL) { // iOS
				param1.put("force_load_override",  context.props.getBoolean(CCProperties.AUTH_PARAMS_PREFIX+"1.force_load_override"));
			}
			if (context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.ios_version", JSONObject.NULL) != JSONObject.NULL) { // iOS
				param1.put("ios_version",          context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.ios_version", JSONObject.NULL));
			} else if (context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.os_version", JSONObject.NULL) != JSONObject.NULL) { // Android
				param1.put("os_version",           context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.os_version", JSONObject.NULL));
			}
			if (context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.analytics_referrer", JSONObject.NULL) != JSONObject.NULL) { // Android
				param1.put("analytics_referrer",   context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.analytics_referrer", JSONObject.NULL));
			}
			if (context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.secure_id", JSONObject.NULL) != JSONObject.NULL) { // Android
				param1.put("secure_id",            context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"1.secure_id", JSONObject.NULL));
			}

			JSONArray paramCommand = new JSONArray();
			{
				Command startGameLoadCommand = new Command();
				startGameLoadCommand.setService("start.game");
				startGameLoadCommand.setMethod("load");
				paramCommand.put(startGameLoadCommand);
			}
			JSONArray params = new JSONArray();
			Object param0;
			AUTH_TYPE authType = AUTH_TYPE.valueOf(context.props.getString(CCProperties.AUTH_PARAMS_PREFIX+"1.os", AUTH_TYPE.IOS.name()).toUpperCase());
			switch (authType) {
			case IOS:
				context.urlPrefix = URL_PREFIX_IOS;
				param0 = new JSONObject()
					.put("udid",             context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"0.udid", JSONObject.NULL))
					.put("cached_mac_address", context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"0.cached_mac_address", JSONObject.NULL))
					.put("app_uuid",           context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"0.app_uuid", JSONObject.NULL))
					.put("advertiser_id",      context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"0.advertiser_id", JSONObject.NULL))
					.put("vendor_id",          context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"0.vendor_id", JSONObject.NULL))
					.put("mac_address",        context.props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"0.mac_address", JSONObject.NULL));
				params.put(param0).put(param1).put(paramCommand);
				break;
			case ANDROID:
				context.urlPrefix = URL_PREFIX_ANDROID;
				param0 = context.props.getString(CCProperties.AUTH_PARAMS_PREFIX+"0", "");
				params.put(param0).put(paramCommand).put(param1);
				break;
			default:
				throw new IllegalArgumentException(authType.name());
			}
			ClientHttpRequest request = createRequest("BatchController.authenticate_device", params.toString());//$NON-NLS-1$
			try (ClientHttpResponse response = request.execute(); InputStream body = response.getBody()) {
				String bodyStr = IOUtils.toString(body, context.httpRequestExecutor.getCharset(response));
				reply = new JSONObject(bodyStr);
				log.trace("<<< {}", reply);
			}
			return reply;
		}

		protected String getCurrentStaticDataVersion() throws IOException {
			URL url = new URL(IOS_CONFIG_URL);
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.set("User-Agent", CONFIG_USER_AGENT);
			// create http request
			ClientHttpRequest request = context.httpRequestExecutor.createRequest(url, HttpMethod.GET, null, httpHeaders);
			try (ClientHttpResponse response = request.execute(); InputStream body = response.getBody()) {
				String bodyStr = IOUtils.toString(body, context.httpRequestExecutor.getCharset(response));
				JSONObject reply = new JSONObject(bodyStr);
				log.trace("<<< {}", reply);
				return reply.getString("static_data_version");
			}
		}

//		protected ASObject authenticate() throws ClientStatusException, ServerStatusException {
//		printStatusString("Authenticate");
//		AMFConnection amfConnection = getConnection();
//		ASObject reply;
//		try {
//			reply = (ASObject) amfConnection.call("BatchController.authenticate_iphone",
//					getAuthenticateParameters());
//		} finally {
//			amfConnection.close();
//		}
//		return reply;
//	}
//
//	protected Object[] getAuthenticateParameters() {
//		List<Object> result = new ArrayList<Object>(25);
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+'0'));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+'1'));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+'2'));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+'3'));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+'4'));
//		Map<String, Object> map = new LinkedHashMap<>();
//		{
//			map.put("using", props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"5.0"));
//			map.put("active", map.get("using"));
//		}
//		result.add(map);
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+'6'));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+'7'));
//		result.add(Long.valueOf(props.getLongProperty(CCProperties.AUTH_PARAMS_PREFIX+'8')));
//		List<Object> list = new ArrayList<Object>();
//		{
//			Command command = new Command();
//			command.setMethod("load");
//			command.setService("start.game");
//			list.add(command);
//		}
//		result.add(list.toArray());
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"10"));
//		result.add(Long.valueOf(props.getLongProperty(CCProperties.AUTH_PARAMS_PREFIX+"11"))); // seconds_from_gmt
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"12"));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"13"));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"14"));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"15"));
//		result.add(Long.toString(System.currentTimeMillis()));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"17"));//FIXME
//		result.add(result.get(1));
//		result.add(Integer.valueOf(props.getIntProperty(CCProperties.AUTH_PARAMS_PREFIX+"19")));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"20")); // app_uuid
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"21"));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"22"));
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"23")); // vendor_id
//		result.add(props.getProperty(CCProperties.AUTH_PARAMS_PREFIX+"24"));
//		assert result.size() == 25 : result.size();
//		log.trace("Authenticate parameters: {}", result);
//		return result.toArray();
//	}

		public int getSequenceNum() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			Integer sequenceNum = commandResponse.getSequence_num();
			return sequenceNum.intValue();
		}

		public player.Player getPlayerPlayer() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
			if (returnValue.has("player")) {
				return new player.Player(returnValue.getJSONObject("player"));
			} else {
				JSONObject metadata = reply.getJSONObject("metadata");
				return new player.Player(metadata.getJSONObject("player"));
			}
		}

		public Session getSessionFromAuthResponse() {
			Session session = new Session(reply.getJSONObject("session"));
			return session;
		}

		public buildings.PlayerBuilding[] getBuildingsPlayerBuilding() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
			JSONArray playerBuildingsObject = returnValue.getJSONArray("player_buildings");
			buildings.PlayerBuilding[] playerBuildings = new buildings.PlayerBuilding[playerBuildingsObject.length()];
			for (int i = 0; i < playerBuildings.length; ++i) {
				buildings.PlayerBuilding playerBuilding = new buildings.PlayerBuilding(playerBuildingsObject.getJSONObject(i));
				playerBuildings[i] = playerBuilding;
			}
			return playerBuildings;
		}

		public guilds.GuildDetails getGuildsGuildDetailsFromAuthResponse() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
			return new guilds.GuildDetails(returnValue.getJSONObject("guild_details"));
		}

		public lockboxes.PlayerLockboxEvent getLockboxesPlayerLockboxEvent() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
			if (returnValue.opt("player_lockbox_event") != null && returnValue.opt("player_lockbox_event") != JSONObject.NULL) {
				return new lockboxes.PlayerLockboxEvent(returnValue.getJSONObject("player_lockbox_event"));
			} else {
				return new lockboxes.PlayerLockboxEvent();
			}
		}

		public lockboxes.LockboxEvent getLockboxesLockboxEvent() {
			JSONArray responses = reply.getJSONArray("responses");
			CommandResponse commandResponse = new CommandResponse(responses.getJSONObject(0));
			JSONObject returnValue = commandResponse.getJSONObject(CommandResponse.RETURN_VALUE);
			if (returnValue.opt("current_lockbox_event") != null && returnValue.opt("current_lockbox_event") != JSONObject.NULL) {
				return new lockboxes.LockboxEvent(returnValue.getJSONObject("current_lockbox_event"));
			} else {
				return new lockboxes.LockboxEvent();
			}
		}
	}

	/************************************************************************/
	/************************************************************************/

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException, ConfigurationException {
		if (args.length != 1 || ! new File(args[0]).canRead()) {
			System.out.println("Usage: java -jar ccbot.jar <fileName>");
			return;
		}
		CCProperties props = new CCProperties(args[0]);
		final CCBot ccBot = new CCBot(props);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				ccBot.logAndSaveBuildingCollectionStatistics();
			}
		});
		while (true) {
			try {
				ccBot.run();
				sleep(1, MINUTES);
			} catch (GameDownException | java.net.SocketTimeoutException e) {
				sleep(30, MINUTES);
			}
		}
	}

}
