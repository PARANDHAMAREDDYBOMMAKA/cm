package com.claimguard.fraud;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PerceptualHashTest {

    private static BufferedImage gradient(int shift) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (int x = 0; x < 64; x++) {
            int level = Math.min(255, Math.max(0, x * 4 + shift));
            graphics.setColor(new Color(level, level, level));
            graphics.drawLine(x, 0, x, 63);
        }
        graphics.dispose();
        return image;
    }

    private static BufferedImage checkerboard() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        for (int x = 0; x < 64; x += 8) {
            for (int y = 0; y < 64; y += 8) {
                graphics.setColor((x + y) % 16 == 0 ? Color.BLACK : Color.WHITE);
                graphics.fillRect(x, y, 8, 8);
            }
        }
        graphics.dispose();
        return image;
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void distanceIsZeroForIdenticalHashes() {
        assertThat(PerceptualHash.distance(0x0F0F0F0F0F0F0F0FL, 0x0F0F0F0F0F0F0F0FL)).isZero();
    }

    @Test
    void distanceCountsDifferingBits() {
        assertThat(PerceptualHash.distance(0b0000L, 0b1011L)).isEqualTo(3);
    }

    @Test
    void theSameImageHashesTheSameWayTwice() throws IOException {
        byte[] content = png(gradient(0));

        assertThat(PerceptualHash.of(content)).isEqualTo(PerceptualHash.of(content));
    }

    @Test
    void aSlightlyBrightenedImageStaysWithinTheNearDuplicateThreshold() throws IOException {
        Long original = PerceptualHash.of(png(gradient(0)));
        Long brightened = PerceptualHash.of(png(gradient(12)));

        assertThat(original).isNotNull();
        assertThat(brightened).isNotNull();
        assertThat(PerceptualHash.distance(original, brightened)).isLessThanOrEqualTo(6);
    }

    @Test
    void anUnrelatedImageIsFarAway() throws IOException {
        Long gradient = PerceptualHash.of(png(gradient(0)));
        Long checkerboard = PerceptualHash.of(png(checkerboard()));

        assertThat(gradient).isNotNull();
        assertThat(checkerboard).isNotNull();
        assertThat(PerceptualHash.distance(gradient, checkerboard)).isGreaterThan(6);
    }

    @Test
    void returnsNullWhenTheBytesAreNotAnImage() {
        assertThat(PerceptualHash.of("not an image".getBytes())).isNull();
    }
}
