package com.claimguard.fraud;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class PerceptualHash {

    private static final int WIDTH = 9;
    private static final int HEIGHT = 8;

    private PerceptualHash() {
    }

    public static Long of(byte[] image) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(image));
            return source == null ? null : compute(source);
        } catch (IOException exception) {
            return null;
        }
    }

    public static long compute(BufferedImage source) {
        BufferedImage scaled = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, WIDTH, HEIGHT, null);
        graphics.dispose();

        long hash = 0L;
        int bit = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH - 1; x++) {
                int left = scaled.getRaster().getSample(x, y, 0);
                int right = scaled.getRaster().getSample(x + 1, y, 0);
                if (left > right) {
                    hash |= 1L << bit;
                }
                bit++;
            }
        }
        return hash;
    }

    public static int distance(long left, long right) {
        return Long.bitCount(left ^ right);
    }
}
