package connect2;

import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ECHost implements Runnable {
	Map<Integer, GameInfo> games;
	List<GameInfo> lastGames;
	List<GameInfo> activeGames;

	ServerSocket server;
	ByteBuffer buf;

	//UDP broadcast socket
	DatagramSocket udpSocket;
	List<SocketAddress> udpTargets;

	int war3version;
	String name;
	long sessionKey;

	int serverPort = 7112;
	int counter = 0; //game counter

	boolean terminated = false;

	public ECHost(int war3Version, String name, long sessionKey) {
		this.war3version = war3Version;
		this.name = name;
		this.sessionKey = sessionKey;

		udpTargets = new ArrayList<SocketAddress>();
		try {
			udpTargets.add(new InetSocketAddress(InetAddress.getLocalHost(), 6112));
			udpTargets.add(new InetSocketAddress("255.255.255.255", 6112));
		} catch(UnknownHostException uhe) {
			System.out.println("[ECHost] UDP broadcast target error: " + uhe.getLocalizedMessage());
		}

		this.games = new HashMap<Integer, GameInfo>();
		this.lastGames = new ArrayList<GameInfo>();
		this.activeGames = new ArrayList<GameInfo>();
		this.buf = ByteBuffer.allocate(65536);
	}

	public void update(int war3Version, String name, long sessionKey) {
		synchronized(this) {
			this.war3version = war3Version;
			this.name = name;
			this.sessionKey = sessionKey;
		}
	}

	public void init() {
		System.out.println("[ECHost] Creating server socket...");

		try {
			server = new ServerSocket(serverPort);
		} catch(IOException ioe) {
			System.out.println("[ECHost] Error while initiating server socket: " + ioe.getLocalizedMessage());

			//increment port number and try again
			serverPort++;
			System.out.println("[ECHost] Trying again in three seconds on port: " + serverPort);
			new RestartThread().start();
			return;
		}

		System.out.println("[ECHost] Creating UDP socket...");
		try {
			udpSocket = new DatagramSocket();
		} catch(IOException ioe) {
			System.out.println("[ECHost] Error while initiating UDP socket: " + ioe.getLocalizedMessage());
			return;
		}

		new Thread(this).start();
	}

	public void deinit() {
		terminated = true;

		try {
			server.close();
			udpSocket.close();
		} catch(IOException ioe) {}
	}

	public void decreateGame(int counter) {
		ByteBuffer lbuf = ByteBuffer.allocate(8);
		lbuf.order(ByteOrder.LITTLE_ENDIAN);

		lbuf.put((byte) 247); //W3GS constant
		lbuf.put((byte) 51); //DECREATE
		lbuf.putShort((short) 8); //packet length

		lbuf.putInt(counter);

		try {
			for(SocketAddress udpTarget : udpTargets) {
				DatagramPacket packet = new DatagramPacket(lbuf.array(), 8, udpTarget);
				udpSocket.send(packet);
			}
		} catch(IOException ioe) {
			System.out.println("[ECHost] Decreate error: " + ioe.getLocalizedMessage());
		}
	}

	public void clearGames() {
		synchronized(this.games) {
			if(this.udpSocket != null) {
				for(GameInfo game : this.activeGames) {
					decreateGame(game.uid);
				}
			}

			this.activeGames.clear();
			this.lastGames.clear();

			Iterator<Map.Entry<Integer, GameInfo>> it = this.games.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<Integer, GameInfo> entry = it.next();
				if(entry.getValue().expired()) {
					it.remove();
				}
			}
		}
	}

	public boolean receivedUDP(ByteBuffer lbuf, String gamenameFilter) {
		if(buf == null || udpSocket == null) {
			return false;
		}

		buf.clear(); //use buf to create our own packet
		lbuf.order(ByteOrder.LITTLE_ENDIAN);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		lbuf.getShort(); //ignore header because this is from server directly
		lbuf.getShort(); //also ignore length

		buf.put((byte) 247); //W3GS
		buf.put((byte) 48); //GAMEINFO
		buf.putShort((short) 0); //packet size; do later

		byte[] addr = new byte[4];
		lbuf.get(addr); //lbuf is special packet that includes IP address (not in W3GS GAMEINFO but used for entconnect)

		int productid = lbuf.getInt();
		buf.putInt(productid); //product ID
		lbuf.getInt(); //ignore version in packet
		buf.putInt(war3version); //version

		int hostCounter = lbuf.getInt(); //hostcounter
		buf.putInt(counter); //replace hostcounter with uid

		buf.putInt(lbuf.getInt()); //unknown

		String gamename = ECUtil.getTerminatedString(lbuf);

		byte[] bytes = ECUtil.strToBytes(gamename);
		buf.put(bytes);
		buf.put((byte) 0); //null terminator

		buf.put(lbuf.get()); //unknown

		byte[] statString = ECUtil.getTerminatedArray(lbuf);
		buf.put(statString); //StatString
		buf.put((byte) 0); //null terminator

		byte[] decodedStatString = ECUtil.decodeStatString(statString);
		ByteBuffer ssbuf = ByteBuffer.wrap(decodedStatString);
		ssbuf.getInt();
		ssbuf.get();
		ssbuf.getInt();
		ssbuf.getInt();
		String mapPath = ECUtil.getTerminatedString(ssbuf);
		if(mapPath.endsWith(".w3x") || mapPath.endsWith(".w3m")) {
			mapPath = mapPath.substring(0, mapPath.length() - 4);
		}

		buf.putInt(lbuf.getInt()); //slots total
		buf.putInt(lbuf.getInt()); //game type
		buf.putInt(lbuf.getInt()); //unknown
		buf.putInt(lbuf.getInt()); //slots open
		buf.putInt(lbuf.getInt()); //up time

		//get the sender's port, but use our own server's port
		int senderPort = ECUtil.unsignedShort(lbuf.getShort());
		buf.putShort((short) serverPort); //port

		//assign length in little endian
		int length = buf.position();
		buf.putShort(2, (short) length);

		//get bytes
		byte[] packetBytes = new byte[length];
		buf.position(0);
		buf.get(packetBytes);

		//create new gameinfo
		GameInfo game = new GameInfo(counter++, addr, senderPort, hostCounter, gamename, mapPath);

		synchronized(this.games) {
			this.games.put(game.uid, game);
			this.lastGames.add(game);
		}

		//filter by gamename if desired
		if(gamenameFilter != null && gamenameFilter.length() > 4 && !gamename.contains(gamenameFilter)) {
			return false;
		}

		//send packet to LAN, or to udpTarget
		System.out.println("[ECHost] Broadcasting with gamename [" + gamename + "]; version: " + war3version +
				"; productid: " + productid + "; senderport: " + senderPort);

		try {
			synchronized(this.games) {
				this.activeGames.add(game);
			}
			for(SocketAddress udpTarget : udpTargets) {
				DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length, udpTarget);
				udpSocket.send(packet);
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
			System.out.println("[ECHost] Error while broadcast UDP: " + ioe.getLocalizedMessage());
		}

		return true;
	}

	public GameInfo searchGame(int uid) {
		synchronized(this.games) {
			return this.games.get(uid);
		}
	}

	public List<GameInfo> getGames() {
		List<GameInfo> games = new ArrayList<GameInfo>();
		synchronized(this.games) {
			for(GameInfo game : this.lastGames) {
				games.add(game);
			}
		}
		return games;
	}

	public void run() {
		while(!terminated) {
			try {
				Socket socket = server.accept();
				System.out.println("[ECHost] Receiving connection from " + socket.getInetAddress().getHostAddress());
				synchronized(this) {
					new ECConnection(this, socket, this.name, this.sessionKey);
				}
			} catch(IOException ioe) {
				System.out.println("[ECHost] Error while accepting connection: " + ioe.getLocalizedMessage());
				break;
			}
		}
	}

	//used if we failed to bind on a certain server port
	class RestartThread extends Thread {
		public void run() {
			try {
				Thread.sleep(3000);
			} catch(InterruptedException e) {}

			init();
		}
	}
}
