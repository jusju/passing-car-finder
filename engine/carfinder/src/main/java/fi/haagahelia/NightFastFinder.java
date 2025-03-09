package fi.haagahelia;

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/**
 * A simple VLCJ program that:
 * 1) Plays a YouTube DVR/live stream,
 * 2) Sets a faster rate (which YouTube may or may not honor),
 * 3) Periodically takes snapshots on a fixed real-time interval,
 * 4) Logs frames that exceed a brightness threshold,
 * 5) Ignores the "finished" event so we don't exit prematurely.
 *
 * The program runs indefinitely until you manually kill it.
 */
public class NightFastFinder extends JFrame {

    // Replace with your YouTube DVR-enabled link.
    private static final String YOUTUBE_DVR_URL = "https://www.youtube.com/watch?v=h9CHyRfyRaw";

    // Try a 2x rate. Higher might fail on live streams.
    private static final float PLAYBACK_RATE = 2.0f;

    // Take a snapshot every 5 seconds in *real time* (not media time).
    private static final long SNAPSHOT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);

    // If average brightness > 10.0 => log detection
    private static final double BRIGHTNESS_THRESHOLD = 10.0;

    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final EmbeddedMediaPlayer mediaPlayer;

    public NightFastFinder() {
        super("Night Fast Finder (No-Exit)");

        // 1) Discover VLC
        boolean found = new NativeDiscovery().discover();
        System.out.println("VLC discovery success: " + found);

        // 2) EmbeddedMediaPlayerComponent
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        mediaPlayer = mediaPlayerComponent.mediaPlayer();

        // 3) Minimal, hidden UI so the video surface is displayable
        setUndecorated(true);
        setSize(1, 1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(mediaPlayerComponent, BorderLayout.CENTER);
        setVisible(true);

        // IMPORTANT: use addMediaPlayerEventListener() instead of .finished(...) or .error(...)
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
                // Some streams incorrectly trigger "finished" immediately.
                // We'll just ignore it to avoid exiting.
                System.out.println("[WARNING] 'finished' event fired, ignoring.");
            }

            @Override
            public void error(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
                // Similarly, ignore "error" so we can keep running.
                System.out.println("[WARNING] 'error' event fired, ignoring or continuing anyway.");
            }
        });
    }

    public void startDVRScan() {
        System.out.println("Starting to play YouTube DVR stream (may or may not support seeking)...");
        mediaPlayer.media().play(YOUTUBE_DVR_URL);

        // We'll schedule rate-setting and snapshot loop after the media starts
        SwingUtilities.invokeLater(() -> {
            // Attempt to set faster rate
            if (mediaPlayer.controls().setRate(PLAYBACK_RATE)) {
                System.out.println("Set playback rate to " + PLAYBACK_RATE + "x");
            } else {
                System.out.println("Failed to set playback rate (stream might not allow it).");
            }

            // Start a background thread that periodically snapshots
            new Thread(this::snapshotLoop).start();
        });
    }

    /**
     * A simple loop that runs until the program is terminated manually:
     * every SNAPSHOT_INTERVAL_MS real-time, attempt to grab a frame
     * and compute brightness.
     */
    private void snapshotLoop() {
        while (true) {
            try {
                Thread.sleep(SNAPSHOT_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            long currentTime = mediaPlayer.status().time();
            // Might be -1 or 0 if VLC can't fetch a time, but let's see.

            // Attempt snapshot
            try {
                BufferedImage frame = mediaPlayer.snapshots().get();
                if (frame != null) {
                    double avgB = computeAverageBrightness(frame);
                    if (avgB > BRIGHTNESS_THRESHOLD) {
                        System.out.printf("MediaTime=%dms => DETECTED bright=%.2f%n",
                                          currentTime, avgB);
                    }
                }
            } catch (Exception e) {
                System.err.println("Snapshot error: " + e.getMessage());
            }
        }
    }

    /**
     * Compute simple average brightness of the entire image.
     */
    private double computeAverageBrightness(BufferedImage img) {
        long sum = 0;
        int w = img.getWidth();
        int h = img.getHeight();
        long totalComponents = (long) w * h * 3;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb     ) & 0xFF;
                sum += (r + g + b);
            }
        }
        return (double) sum / (double) totalComponents;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NightFastFinder finder = new NightFastFinder();
            finder.startDVRScan();
        });
    }
}
