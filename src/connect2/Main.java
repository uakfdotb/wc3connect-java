package connect2;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
	public static Map<String, String> ThirdPartyBotMap = new HashMap<String, String>();
	public static boolean Debug = true;
	public static int Version = 2018102602;

	public static void main(String[] args) throws java.io.IOException {
		ThirdPartyBotMap.put("192.99.6.98", "MMH-USA");
		ThirdPartyBotMap.put("85.10.199.252", "MMH-Euro");

		new Thread(new LoadThirdPartyThread()).start();
		new Web();

		if(!GraphicsEnvironment.isHeadless()){
			new GuiApp();
		}
	}

	public static String GetThirdPartyBot(String ip) {
		synchronized(ThirdPartyBotMap) {
			return ThirdPartyBotMap.get(ip);
		}
	}

	static class LoadThirdPartyThread implements Runnable {
		public void run() {
			try {
				String[] lines = Utils.get("https://entgaming.net/entconnect/wc3connect_thirdpartymap.php").split("\n");
				for(String line : lines) {
					line = line.trim();
					String[] parts = line.split(" ");
					if(parts.length == 2) {
						System.out.println("[main] load third party bot: " + parts[0] + " -> " + parts[1]);
						synchronized(ThirdPartyBotMap) {
							ThirdPartyBotMap.put(parts[0], parts[1]);
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
