package fi.haagahelia;

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class NightObjectFinder extends JFrame {

    private static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=h9CHyRfyRaw";

    // Night time range: 23:00 to 04:00 local Helsinki time
    private static final int NIGHT_START_HOUR = 23;
    private static final int NIGHT_END_HOUR   = 4;

    // How often to grab a snapshot (in milliseconds):
    private static final long CAPTURE_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    // Brightness threshold for “detected movement/light”
    private static final double BRIGHTNESS_THRESHOLD = 10.0;

    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final EmbeddedMediaPlayer mediaPlayer;

    public NightObjectFinder() {
        super("Invisible Night Object Finder");

        // 1) Make sure VLC is discovered
        boolean found = new NativeDiscovery().discover();
        System.out.println("VLC discovery success: " + found);

        // 2) Create the EmbeddedMediaPlayerComponent
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        mediaPlayer = mediaPlayerComponent.mediaPlayer();

        // 3) Build a tiny or invisible UI so that the component is "displayable"
        setUndecorated(true);        // no window decoration
        setSize(1, 1);              // 1×1 pixel (effectively invisible on screen)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add the video surface to this frame
        add(mediaPlayerComponent, BorderLayout.CENTER);

        // We must show the frame for the surface to become displayable
        setVisible(true);

        // 4) Start playing the stream
        System.out.println("Starting playback of: " + YOUTUBE_URL);
        mediaPlayer.media().play(YOUTUBE_URL);
    }

    /**
     * Start monitoring for light or motion between 23:00 and 04:00 local time.
     */
    public void startMonitoring() {
        System.out.println("Monitoring from 23:00 to 04:00 (Helsinki time)...");
        while (true) {
            if (isNightTime()) {
                try {
                    BufferedImage snapshot = mediaPlayer.snapshots().get();
                    if (snapshot != null) {
                        double avgBrightness = computeAverageBrightness(snapshot);
                        if (avgBrightness > BRIGHTNESS_THRESHOLD) {
                            String now = java.time.ZonedDateTime.now(ZoneId.of("Europe/Helsinki"))
                                                               .toLocalTime().toString();
                            System.out.println("[" + now + "] DETECTED movement/light. Brightness=" + avgBrightness);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error taking or processing snapshot: " + e.getMessage());
                }
            }
            // Sleep until next capture
            try {
                Thread.sleep(CAPTURE_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Check if local time in Europe/Helsinki is between 23:00 and 04:00.
     * If hour >= 23 or hour < 4 => "night".
     */
    private boolean isNightTime() {
        LocalTime now = LocalTime.now(ZoneId.of("Europe/Helsinki"));
        int hour = now.getHour();
        if (hour >= NIGHT_START_HOUR) {
            return true;
        }
        return (hour < NIGHT_END_HOUR);
    }

    /**
     * Compute simple average brightness: sum(R+G+B) / (width*height*3).
     */
    private double computeAverageBrightness(BufferedImage image) {
        long sum = 0;
        int w = image.getWidth();
        int h = image.getHeight();
        long totalComponents = (long) w * h * 3;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b = (rgb      ) & 0xFF;
                sum += (r + g + b);
            }
        }
        return (double) sum / (double) totalComponents;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NightObjectFinder finder = new NightObjectFinder();
            finder.startMonitoring();
        });
    }
}
