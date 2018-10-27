package connect2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.json.JSONArray;

public class Web {
	private LoginResult loginResult = null;
	private ECHost host = null;
	private ECList list = null;

	public Web() throws IOException {
		if(Main.Debug) {
			Logger logger = Logger.getLogger("com.sun.net.httpserver");
			ConsoleHandler ch = new ConsoleHandler();
			logger.setLevel(Level.FINER);
			ch.setLevel(Level.FINER);
			logger.addHandler(ch);
		}
		HttpServer server = HttpServer.create(new InetSocketAddress(8333), 0);
		server.createContext("/", new FileHandler());
		server.createContext("/login", new LoginHandler(this));
		server.createContext("/signup", new SignupHandler());
		server.createContext("/games", new GamesHandler(this));
		server.createContext("/motd", new MotdHandler());
		server.createContext("/version", new VersionHandler());
		server.createContext("/gameinfo", new GameInfoHandler());
		server.createContext("/show", new ShowHandler(this));
		server.createContext("/validate", new ValidateHandler(this));
		server.setExecutor(null);
		server.start();
		System.out.println("[web] started on :8333");
	}

	static Map<String, String> getPostForm(HttpExchange httpExchange) throws IOException {
		Map<String, String> parameters = new HashMap<String, String>();
		String request = Utils.streamToString(httpExchange.getRequestBody());
		String[] keyValuePairs = request.split("&");
		for (String keyValuePair : keyValuePairs) {
			String[] keyValue = keyValuePair.split("=");
			if (keyValue.length != 2) {
				continue;
			}
			parameters.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
		}
		return parameters;
	}

	static class LoginResult {
		int war3Version;
		String name;
		String sessionKey;

		LoginResult(int war3Version, String name, String sessionKey) {
			this.war3Version = war3Version;
			this.name = name;
			this.sessionKey = sessionKey;
		}
	}

	static class WebGame implements Comparable<WebGame> {
		int uid;
		String gamename;
		String ip;
		int slotsTaken;
		int slotsTotal;
		String map;
		String location;
		AppGame appGame;

		public JSONObject encode() {
			JSONObject obj = new JSONObject();
			obj.put("uid", this.uid);
			obj.put("gamename", this.gamename);
			obj.put("ip", this.ip);
			obj.put("slots_taken", this.slotsTaken);
			obj.put("slots_total", this.slotsTotal);
			obj.put("map", this.map);
			obj.put("location", this.location);
			if(this.appGame == null) {
				obj.put("app_game", "");
			} else {
				obj.put("app_game", this.appGame.encode());
			}
			return obj;
		}

		public String sortKey() {
			// no app game => put at the end
			if(this.appGame == null) {
				return "z-" + this.location + "-" + this.gamename;
			}

			// autohost games come after others
			if(this.appGame.botID < 100) {
				return "y-" + this.gamename;
			}

			// order the rest by botid
			return "a-" + this.appGame.botID + "-" + this.gamename;
		}

		public int compareTo(WebGame other) {
			return this.sortKey().compareTo(other.sortKey());
		}
	}

	static class AppGame {
		int id;
		int botID;
		String location;
		String gamename;
		String map;
		String host;
		int slotsTaken;
		int slotsTotal;
		String uptime;

		public JSONObject encode() {
			JSONObject obj = new JSONObject();
			obj.put("id", this.id);
			obj.put("bot_id", this.botID);
			obj.put("location", this.location);
			obj.put("name", this.gamename);
			obj.put("map", this.map);
			obj.put("host", this.host);
			obj.put("slots_taken", this.slotsTaken);
			obj.put("slots_total", this.slotsTotal);
			obj.put("uptime", this.uptime);
			return obj;
		}

		public static AppGame decode(JSONObject obj) {
			AppGame g = new AppGame();
			g.id = obj.getInt("id");
			g.botID = obj.getInt("bot_id");
			g.location = obj.getString("location");
			g.gamename = obj.getString("name");
			g.map = obj.getString("map");
			g.host = obj.getString("host");
			g.slotsTaken = obj.getInt("slots_taken");
			g.slotsTotal = obj.getInt("slots_total");
			g.uptime = obj.getString("uptime");
			return g;
		}
	}

	static class WebGamelist {
		List<WebGame> pub;
		List<WebGame> autohost;
		List<WebGame> others;
		List<WebGame> unmoderated;

		public WebGamelist() {
			this.pub = new ArrayList<WebGame>();
			this.autohost = new ArrayList<WebGame>();
			this.others = new ArrayList<WebGame>();
			this.unmoderated = new ArrayList<WebGame>();
		}

		static JSONArray encodeList(List<WebGame> l) {
			JSONArray a = new JSONArray();
			for(WebGame g : l) {
				a.put(g.encode());
			}
			return a;
		}

		public JSONObject encode() {
			JSONObject obj = new JSONObject();
			obj.put("publicGames", WebGamelist.encodeList(this.pub));
			obj.put("autohostGames", WebGamelist.encodeList(this.autohost));
			obj.put("otherGames", WebGamelist.encodeList(this.others));
			obj.put("unmoderatedGames", WebGamelist.encodeList(this.unmoderated));
			return obj;
		}
	}

	static class FileHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			String path = t.getRequestURI().getPath();
			if(path.equals("/")) {
				path = "/index.html";
			}
			InputStream in = getClass().getResourceAsStream("/static" + path);
			if(in == null) {
				t.sendResponseHeaders(404, 0);
				t.close();
				return;
			}
			byte[] bytes = Utils.streamToByteArray(in);
			t.sendResponseHeaders(200, bytes.length);
			OutputStream os = t.getResponseBody();
			os.write(bytes);
			os.close();
		}
	}

	static class LoginHandler implements HttpHandler {
		private Web web;

		public LoginHandler(Web web) {
			this.web = web;
		}

		public void handle(HttpExchange t) throws IOException {
			Map<String, String> postForm = Web.getPostForm(t);
			postForm.put("version", "2");
			String response;
			try {
				response = Utils.postForm("https://connect.entgaming.net/gwc-login", postForm);
			} catch(Exception e) {
				Utils.respondError(t, "Error logging in: " + e.getMessage() + ".");
				return;
			}
			JSONObject obj = new JSONObject(response);
			if(obj.has("error") && obj.getString("error").length() > 0) {
				Utils.respondError(t, "Error logging in: " + obj.getString("error") + ".");
				return;
			} else if(obj.getLong("war3version") == 0) {
				Utils.respondError(t, "Error logging in: failed to login.");
				return;
			}
			synchronized(web) {
				web.loginResult = new LoginResult(obj.getInt("war3version"), obj.getString("name"), obj.getString("session_key2"));
				if(web.host == null) {
					web.host = new ECHost(web.loginResult.war3Version, web.loginResult.name, web.loginResult.sessionKey);
					web.host.init();
				} else {
					web.host.update(web.loginResult.war3Version, web.loginResult.name, web.loginResult.sessionKey);
				}
				System.out.println("[web] logged in successfully as " + web.loginResult.name);
				web.list = new ECList(web.host);
			}
			Utils.respondJSON(t, obj);
		}
	}

	static class SignupHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			byte[] request = Utils.streamToByteArray(t.getRequestBody());
			String response;
			try {
				response = Utils.post("https://connect.entgaming.net/gwc-signup", request);
			} catch(Exception e) {
				Utils.respondError(t, "Error signing up: " + e.getMessage() + ".");
				return;
			}
			JSONObject obj = new JSONObject(response);
			if(obj.has("error") && obj.getString("error").length() > 0) {
				Utils.respondError(t, "Error signing up: " + obj.getString("error") + ".");
				return;
			}
			System.out.println("[web] signed up a new account");
			JSONObject resp = new JSONObject();
			resp.put("success", true);
			Utils.respondJSON(t, resp);
		}
	}

	static class GamesHandler implements HttpHandler {
		private Web web;

		public GamesHandler(Web web) {
			this.web = web;
		}

		static Map<String, AppGame> getAppGames() throws IOException {
			JSONArray arr = new JSONArray(Utils.get("https://host.entgaming.net/allgames"));
			Map<String, AppGame> games = new HashMap<String, AppGame>();
			for(int i = 0; i < arr.length(); i++) {
				AppGame game = AppGame.decode(arr.getJSONObject(i));
				games.put(game.gamename, game);
			}
			return games;
		}

		public void handle(HttpExchange t) throws IOException {
			ECHost host;
			synchronized(web) {
				if(web.host == null) {
					Utils.respondJSON(t, new WebGamelist().encode());
					return;
				}
				host = web.host;
			}
			Map<String, AppGame> appGames = GamesHandler.getAppGames();
			WebGamelist games = new WebGamelist();
			for(GameInfo game : host.getGames()) {
				WebGame webGame = new WebGame();
				webGame.uid = game.uid;
				webGame.gamename = game.gamename;
				webGame.ip = game.remoteAddress.getHostAddress();
				webGame.map = game.map;

				if(appGames.containsKey(webGame.gamename)) {
					webGame.appGame = appGames.get(webGame.gamename);
				}

				if(webGame.appGame != null) {
					if(webGame.appGame.botID >= 100) {
						games.pub.add(webGame);
					} else {
						games.autohost.add(webGame);
					}
				} else if(Main.GetThirdPartyBot(webGame.ip) != null) {
					webGame.location = Main.GetThirdPartyBot(webGame.ip);
					if(webGame.location.contains("MMH")) {
						games.pub.add(webGame);
					} else {
						games.others.add(webGame);
					}
				} else if(game.extra.length() > 0) {
					webGame.location = game.extra;
					games.unmoderated.add(webGame);
				} else {
					System.out.println("[web] ignoring game with ip=" + webGame.ip);
				}
			}

			Collections.sort(games.pub);
			Collections.sort(games.autohost);
			Collections.sort(games.others);
			Collections.sort(games.unmoderated);
			Utils.respondJSON(t, games.encode());
		}
	}

	static class MotdHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			String motd = Utils.get("https://entgaming.net/entconnect/wc3connect_java_motd.php");
			motd = motd.trim();
			JSONObject obj = new JSONObject();
			obj.put("motd", motd);
			Utils.respondJSON(t, obj);
		}
	}

	static class VersionHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			String versionStr = Utils.get("https://entgaming.net/entconnect/wc3connect_java_version.php");
			Integer version = Integer.parseInt(versionStr.trim());
			JSONObject obj = new JSONObject();
			obj.put("up_to_date", version <= Main.Version);
			Utils.respondJSON(t, obj);
		}
	}

	static class GameInfoHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			Map<String, String> postForm = Web.getPostForm(t);
			String gameID = postForm.get("gameid");
			Utils.respondString(t, Utils.get("https://entgaming.net/forum/slots_fast.php?id=" + gameID + "&time=" + (System.currentTimeMillis()/1000)));
		}
	}

	static class ShowHandler implements HttpHandler {
		private Web web;

		public ShowHandler(Web web) {
			this.web = web;
		}

		public void handle(HttpExchange t) throws IOException {
			Map<String, String> postForm = Web.getPostForm(t);
			long uid = Long.parseLong(postForm.get("uid"));
			ECHost host;
			ECList list;
			synchronized(web) {
				if(web.host == null) {
					Utils.respondError(t, "internal server error");
					return;
				}
				host = web.host;
				list = web.list;
			}
			GameInfo gameInfo = host.searchGame((int) uid);
			if(gameInfo != null) {
				System.out.println("[web] set list filter to " + gameInfo.gamename);
				list.setFilter(gameInfo.gamename);
			} else {
				System.out.println("[web] no game found, did we click during an update?");
			}
			JSONObject resp = new JSONObject();
			resp.put("success", true);
			Utils.respondJSON(t, resp);
		}
	}

	static class ValidateHandler implements HttpHandler {
		private Web web;

		public ValidateHandler(Web web) {
			this.web = web;
		}

		public void handle(HttpExchange t) throws IOException {
			Map<String, String> postForm = Web.getPostForm(t);
			String key = postForm.get("key");
			if(key == null) {
				key = "";
			}
			LoginResult loginResult;
			synchronized(web) {
				loginResult = web.loginResult;
			}
			Map<String, String> m = new HashMap<String, String>();
			m.put("username", loginResult.name);
			m.put("sessionkey", loginResult.sessionKey);
			m.put("key", key);
			JSONObject obj = new JSONObject(Utils.postForm("https://connect.entgaming.net/gwc-validate", m));
			if(obj.has("error")) {
				Utils.respondError(t, "Error validating account: " + obj.getString("error") + ".");
				return;
			}
			JSONObject resp = new JSONObject();
			resp.put("success", true);
			Utils.respondJSON(t, resp);
		}
	}
}
