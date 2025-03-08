package fi.haagahelia;

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class YoutubeSnapshot extends JFrame {

    // The stream you want to capture from:
    private static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=h9CHyRfyRaw";

    // A panel that displays the 200x200 cropped image
    private JLabel snapshotLabel;

    // The VLCJ media player component
    private EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private EmbeddedMediaPlayer mediaPlayer;

    public YoutubeSnapshot() {
        super("YouTube Snapshot Demo (VLCJ)");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1) Create the VLCJ media player component
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        mediaPlayer = mediaPlayerComponent.mediaPlayer();

        // 2) A simple button to take the snapshot
        JButton snapshotButton = new JButton("Take 200×200 Snapshot");
        snapshotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                takeSnapshotAndDisplay();
            }
        });

        // 3) A label to hold the sub-image
        snapshotLabel = new JLabel("Snapshot will appear here", SwingConstants.CENTER);

        add(mediaPlayerComponent, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(snapshotButton, BorderLayout.WEST);
        bottomPanel.add(snapshotLabel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Set up frame size and show
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        // 4) Start playing the YouTube URL
        // If your VLC installation can handle YouTube links, this should start playing
        mediaPlayer.media().play(YOUTUBE_URL);
    }

    /**
     * Uses VLCJ’s snapshot() to get the current video frame as a BufferedImage,
     * then crops a 200×200 sub-image from it, and displays the sub-image in
     * snapshotLabel.
     */
    private void takeSnapshotAndDisplay() {
        // 1) Ask VLCJ to create a raw snapshot
        // This returns a byte[] of the snapshot in PNG format by default.
        try {
            BufferedImage fullFrame = mediaPlayer.snapshots().get();

            if (fullFrame == null) {
                snapshotLabel.setText("Failed to snapshot (fullFrame == null)");
                return;
            }

            // 3) Crop a 200×200 sub-image.
            // Adjust x,y as needed. Here, just top-left corner for demonstration:
            int x = 100;
            int y = 100;
            int w = Math.min(200, fullFrame.getWidth());
            int h = Math.min(200, fullFrame.getHeight());

            BufferedImage subImg = fullFrame.getSubimage(x, y, w, h);

            // 4) Display the sub-image in the label
            snapshotLabel.setIcon(new ImageIcon(subImg));
            snapshotLabel.setText("");
        } catch (Exception e) {
            snapshotLabel.setText("Failed to decode snapshot image");
        }
    }

    public static void main(String[] args) {
        // This will try to discover the native VLC installation on your system
        // If it fails, you may need to set VLC plugin/library paths manually.
        boolean found = new NativeDiscovery().discover();
        System.out.println("VLC discovery success: " + found);

        SwingUtilities.invokeLater(() -> {
            new YoutubeSnapshot();
        });
    }
}
