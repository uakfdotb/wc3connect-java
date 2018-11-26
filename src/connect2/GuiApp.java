package connect2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;

public class GuiApp implements ActionListener {
	JButton launchBtn, exitBtn;

	public GuiApp(){
		JFrame frame = new JFrame("WC3Connect-Java");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.launchBtn = new JButton("Launch");
		this.exitBtn = new JButton("Exit");
		this.launchBtn.setFont(new Font("Arial", Font.PLAIN, 30));
		this.exitBtn.setFont(new Font("Arial", Font.PLAIN, 30));
		this.launchBtn.addActionListener(this);
		this.exitBtn.addActionListener(this);

		if(!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)){
			this.launchBtn.setEnabled(false);
			this.launchBtn.setText("Open your webbrowser and go to: 127.0.0.1:8033");
		}

		frame.getContentPane().add(this.launchBtn, BorderLayout.PAGE_START);
		frame.getContentPane().add(this.exitBtn, BorderLayout.PAGE_END);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void launch(){
		try {
			Desktop.getDesktop().browse(URI.create("http://127.0.0.1:8333"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.launchBtn) {
			this.launch();
		} else if(e.getSource() == this.exitBtn) {
			System.exit(0);
		}
	}
}
