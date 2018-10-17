package connect2;

import java.util.Map;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

public class Utils {
	public static ByteArrayOutputStream streamToByteArrayOutputStream(InputStream in) throws IOException {
		byte[] buf = new byte[4096];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while(true) {
			int n = in.read(buf);
			if(n == -1) {
				break;
			}
			out.write(buf, 0, n);
		}
		return out;
	}

	public static byte[] streamToByteArray(InputStream in) throws IOException {
		return streamToByteArrayOutputStream(in).toByteArray();
	}

	public static String streamToString(InputStream in) throws IOException {
		return streamToByteArrayOutputStream(in).toString("UTF-8");
	}

	public static String post(String url, byte[] request) throws IOException {
		URL u = new URL(url);
		HttpURLConnection con = (HttpURLConnection) u.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		con.setConnectTimeout(5000);
		con.setDoOutput(true);
		DataOutputStream out = new DataOutputStream(con.getOutputStream());
		out.write(request);
		out.flush();
		out.close();
		String response = Utils.streamToString(con.getInputStream());
		return response;
	}

	public static String postForm(String url, Map<String, String> request) throws IOException {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for(Map.Entry<String, String> entry : request.entrySet()) {
			if(first) {
				first = false;
			} else {
				result.append("&");
			}
			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}
		return Utils.post(url, result.toString().getBytes("UTF-8"));
	}

	public static String get(String url) throws IOException {
		URL u = new URL(url);
		HttpURLConnection con = (HttpURLConnection) u.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		con.setConnectTimeout(5000);
		String response = Utils.streamToString(con.getInputStream());
		return response;
	}

	public static void respondString(HttpExchange t, String str) throws IOException {
		byte[] bytes = str.getBytes("UTF-8");
		t.sendResponseHeaders(200, bytes.length);
		OutputStream os = t.getResponseBody();
		os.write(bytes);
		os.close();
	}

	public static void respondJSON(HttpExchange t, JSONObject object) throws IOException {
		t.getResponseHeaders().set("Content-Type", "application/json");
		respondString(t, object.toString());
	}

	public static void respondError(HttpExchange t, String msg) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("error", msg);
		respondJSON(t, obj);
	}
}
