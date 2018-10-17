package connect2;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;

public class Main {
	public static Map<String, String> ThirdPartyBotMap = new HashMap<String, String>();
	public static boolean Debug = true;

	public static void main(String[] args) throws java.io.IOException {
		ThirdPartyBotMap.put("192.99.6.98", "MMH-USA");
		ThirdPartyBotMap.put("85.10.199.252", "MMH-Euro");

		new Thread(new LoadThirdPartyThread()).start();

		Web web = new Web();

		JFrame frame = new JFrame("WC3Connect-Java");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JButton launch = new JButton("Launch");
		JButton exit = new JButton("Exit");
		launch.setFont(new Font("Arial", Font.PLAIN, 30));
		exit.setFont(new Font("Arial", Font.PLAIN, 30));
		launch.addActionListener(new LaunchListener());
		exit.addActionListener(new ExitListener());
		frame.getContentPane().add(launch, BorderLayout.PAGE_START);
		frame.getContentPane().add(exit, BorderLayout.PAGE_END);
		frame.pack();
		frame.setVisible(true);
		launch();
	}

	public static String GetThirdPartyBot(String ip) {
		synchronized(ThirdPartyBotMap) {
			return ThirdPartyBotMap.get(ip);
		}
	}

	public static void launch() {
		try {
			Desktop.getDesktop().browse(new URI("http://127.0.0.1:8333"));
		} catch(Exception e) {
			e.printStackTrace();
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

	static class LaunchListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			Main.launch();
		}
	}

	static class ExitListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}
}
