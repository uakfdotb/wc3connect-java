package connect2;

import org.json.JSONArray;

import javax.net.ssl.HttpsURLConnection;
import java.nio.ByteBuffer;
import java.util.List;

public class ECList implements Runnable {
	ECHost host;
	ByteBuffer buf;
	String filter;
	List<Integer> preferred; //comma-separated list of preferred bots

	boolean terminate = false;

	public ECList(ECHost host) {
		this.host = host;
		this.buf = ByteBuffer.allocate(65536);
		Thread thread = new Thread(this);
		thread.start();
	}

	public void deinit() {
		terminate = true;

		synchronized(this) {
			this.notifyAll();
		}
	}

	public void setFilter(String filter) {
		synchronized(this) {
			this.filter = filter;
		}
		this.once();
	}

	private void once() {
		synchronized(this) {
			HttpsURLConnection conn;
			String line;

			System.out.println("[ECList] Connecting to server...");

			try {
				JSONArray arr = new JSONArray(Utils.get("https://connect.entgaming.net/games"));
				host.clearGames();
				boolean matchedFilter = false;
				if(this.filter != null && !this.filter.isEmpty()) {
					for(int i = 0; i < arr.length(); i++) {
						byte[] data = ECUtil.hexDecode(arr.getString(i));
						buf.clear();
						buf.put(data);
						buf.position(0);
						matchedFilter = host.receivedUDP(buf, this.filter) || matchedFilter;
					}
				}
				if(!matchedFilter) {
					host.clearGames(); // ignore the games we just provided since we'll be sending them again
					for(int i = 0; i < arr.length(); i++) {
						byte[] data = ECUtil.hexDecode(arr.getString(i));
						buf.clear();
						buf.put(data);
						buf.position(0);
						host.receivedUDP(buf, null);
					}
				}

				System.out.println("[ECList] Listing complete (got " + arr.length() + " games");
			} catch (Exception e) {
				System.out.println("[ECList] Error during listing: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	public void run() {
		while(!terminate) {
			this.once();

			if(terminate) break;

			synchronized(this) {
				try {
					this.wait(10000);
				} catch(InterruptedException e) {}
			}
		}
	}
}
