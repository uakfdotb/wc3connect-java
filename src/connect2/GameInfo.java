package connect2;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GameInfo {
	int uid;
	InetAddress remoteAddress;
	int remotePort;
	int hostCounter;
	String gamename;
	String map;
	String extra;
	long millis;

	public GameInfo(int uid, byte[] addr, int port, int hostCounter, String gamename, String map, String extra) {
		this.uid = uid;
		this.remotePort = port;
		this.hostCounter = hostCounter;
		this.gamename = gamename;
		this.map = map;
		this.extra = extra;
		this.millis = System.currentTimeMillis();

		try {
			remoteAddress = InetAddress.getByAddress(addr);
		} catch(UnknownHostException uhe) {
			System.out.println("[GameInfo] Error: unknown host on addr bytes: " + uhe.getLocalizedMessage());
			remoteAddress = null;
		}
	}

	public boolean expired() {
		return (System.currentTimeMillis() - this.millis) > 30000;
	}
}
