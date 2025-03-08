package fi.haagahelia;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class YoutubePixelMonitor {

    private static final int REGION_X = 200;  // X-koordinaatti
    private static final int REGION_Y = 150;  // Y-koordinaatti
    private static final int REGION_WIDTH = 50;  // Valittavan alueen leveys
    private static final int REGION_HEIGHT = 50; // Valittavan alueen korkeus

    private static final int BRIGHTNESS_THRESHOLD = 200;  // Kirkkauden raja

    private static LocalTime startTime;
    private static LocalTime endTime;

    public static void main(String[] args) {
        valitseAika(LocalDate.now(), 20, 30, 20, 40);  // Oletus: klo 09:00 - 17:00

        // YouTube-striimin URL (RTMP tai HLS .m3u8 suora linkki vaaditaan)
        String youtubeStreamUrl = "https://www.youtube.com/watch?v=h9CHyRfyRaw&ab_channel=JukkaJuslin";

        monitorPixels(youtubeStreamUrl);
    }

    public static void valitseAika(LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        startTime = LocalTime.of(startHour, startMinute);
        endTime = LocalTime.of(endHour, endMinute);
    }

    private static void monitorPixels(String streamUrl) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl);
        try {
            grabber.start();
            double prevBrightness = 0.0;

            while (true) {
                LocalDateTime now = LocalDateTime.now();
                LocalTime currentTime = now.toLocalTime();

                if (currentTime.isBefore(startTime) || currentTime.isAfter(endTime)) {
                    Thread.sleep(1000);
                    continue;
                }

                Frame frame = grabber.grabImage();
                if (frame == null) {
                    Thread.sleep(1000);
                    continue;
                }

                BufferedImage image = new Java2DFrameConverter().convert(frame);
                double avgBrightness = calculateRegionBrightness(image, REGION_X, REGION_Y, REGION_WIDTH, REGION_HEIGHT);

                if (prevBrightness < BRIGHTNESS_THRESHOLD && avgBrightness >= BRIGHTNESS_THRESHOLD) {
                    System.out.println("Pikselit muuttuivat vaaleiksi! " + now);
                }

                prevBrightness = avgBrightness;
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                grabber.stop();
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static double calculateRegionBrightness(BufferedImage image, int x, int y, int w, int h) {
        int maxX = Math.min(x + w, image.getWidth());
        int maxY = Math.min(y + h, image.getHeight());

        long totalBrightness = 0;
        long pixelCount = 0;

        for (int j = y; j < maxY; j++) {
            for (int i = x; i < maxX; i++) {
                int rgb = image.getRGB(i, j);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = (rgb) & 0xFF;

                double brightness = 0.299 * red + 0.587 * green + 0.114 * blue;
                totalBrightness += brightness;
                pixelCount++;
            }
        }
        return pixelCount == 0 ? 0 : totalBrightness / pixelCount;
    }
}

