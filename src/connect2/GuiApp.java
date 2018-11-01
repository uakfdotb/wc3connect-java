package connect2;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class GuiApp {
    public GuiApp(){
        JFrame frame = new JFrame("WC3Connect-Java");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton launch = new JButton("Launch");
        JButton exit = new JButton("Exit");
        launch.setFont(new Font("Arial", Font.PLAIN, 30));
        exit.setFont(new Font("Arial", Font.PLAIN, 30));
        launch.addActionListener(e -> this.launch());
        exit.addActionListener(e -> System.exit(0));

        if(!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)){
            launch.setEnabled(false);
            launch.setText("Open your webbrowser and go to: 127.0.0.1:8033");
        }

        frame.getContentPane().add(launch, BorderLayout.PAGE_START);
        frame.getContentPane().add(exit, BorderLayout.PAGE_END);
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
}
