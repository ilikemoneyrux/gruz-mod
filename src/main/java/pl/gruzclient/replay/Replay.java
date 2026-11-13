package pl.gruzclient.replay;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import pl.gruzclient.GruzClient;
import pl.gruzclient.compat.Compat;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Wbudowany Instant Replay: co tick (20 klatek na sekundę) ostatnia klatka gry trafia do bufora jako JPEG 720p
 * ze znakiem wodnym „Recorded with Gruz Client”. Klawisz zapisuje ostatnie 15/30/60 s jako AVI w gruz-clips/.
 * Kodowanie idzie w osobnym wątku; w wątku gry zostaje tylko zrzut ekranu i zmniejszenie obrazu.
 */
public final class Replay {
    private Replay() {}

    public static final int FPS = 20;
    private static final int TARGET_H = 720;
    private static final ArrayDeque<byte[]> frames = new ArrayDeque<>();
    private static int width, height; // rozmiar klatek w buforze; zmiana okna czyści bufor
    private static final AtomicInteger pending = new AtomicInteger();
    private static final ExecutorService ENC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Gruz Replay");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static BufferedImage logo;
    private static boolean logoTried;

    /** Co tick, gdy bufor jest włączony (wątek gry, poza rysowaniem klatki) */
    public static void tick(Minecraft mc, int seconds) {
        // słaby procesor: gubimy klatkę zamiast zapychać pamięć kolejką
        if (mc.level == null || pending.get() > 6) return;
        pending.incrementAndGet();
        try {
            Compat.grabFrame(mc, img -> {
                try {
                    BufferedImage bi = scaled(img);
                    ENC.execute(() -> {
                        try { add(encode(bi), bi.getWidth(), bi.getHeight(), seconds); }
                        catch (Exception e) { GruzClient.LOG.debug("Replay: klatka pominięta", e); }
                        finally { pending.decrementAndGet(); }
                    });
                } catch (Throwable t) {
                    pending.decrementAndGet();
                } finally {
                    img.close();
                }
            });
        } catch (Throwable t) {
            pending.decrementAndGet(); // np. brak głównego bufora w trakcie zmiany okna
        }
    }

    public static void clear() { synchronized (frames) { frames.clear(); } }

    /** Zapisuje bufor do pliku; done dostaje nazwę pliku albo null (pusty bufor / błąd) */
    public static void save(Minecraft mc, Consumer<String> done) {
        List<byte[]> copy;
        int w, h;
        synchronized (frames) { copy = new ArrayList<>(frames); w = width; h = height; }
        if (copy.size() < FPS) { done.accept(null); return; } // mniej niż sekunda nagrania
        String name = "clip-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".avi";
        Path file = mc.gameDirectory.toPath().resolve("gruz-clips").resolve(name);
        ENC.execute(() -> {
            String result = null;
            try { Avi.write(file, copy, w, h, FPS); result = name; }
            catch (Exception e) { GruzClient.LOG.warn("Replay: nie udało się zapisać klipu", e); }
            String r = result;
            mc.execute(() -> done.accept(r));
        });
    }

    /** Zmniejszenie do 720p (najbliższy piksel) – w wątku gry, bo obraz z gry trzeba od razu zwolnić */
    private static BufferedImage scaled(NativeImage img) {
        int w0 = img.getWidth(), h0 = img.getHeight();
        float step = Math.max(1f, h0 / (float) TARGET_H);
        int h = Math.round(h0 / step) & ~1, w = Math.round(w0 / step) & ~1;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] px = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < h; y++) {
            int sy = Math.min(h0 - 1, (int) (y * step));
            for (int x = 0; x < w; x++) {
                int c = Compat.getPixelABGR(img, Math.min(w0 - 1, (int) (x * step)), sy);
                px[y * w + x] = (c & 0xFF) << 16 | (c & 0xFF00) | ((c >> 16) & 0xFF);
            }
        }
        return bi;
    }

    private static byte[] encode(BufferedImage bi) throws Exception {
        watermark(bi);
        ByteArrayOutputStream out = new ByteArrayOutputStream(96 * 1024);
        ImageWriter wr = ImageIO.getImageWritersByFormatName("jpg").next();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            wr.setOutput(ios);
            ImageWriteParam p = wr.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(0.82f);
            wr.write(null, new IIOImage(bi, null, null), p);
        } finally {
            wr.dispose();
        }
        return out.toByteArray();
    }

    private static void add(byte[] jpg, int w, int h, int seconds) {
        synchronized (frames) {
            if (w != width || h != height) { frames.clear(); width = w; height = h; }
            frames.add(jpg);
            while (frames.size() > seconds * FPS) frames.poll();
        }
    }

    /** Mały znak wodny na dole, na środku – widać go też po przycięciu do 9:16 */
    private static void watermark(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int size = Math.max(10, h / 46);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
            g.setFont(font);
            String text = "Recorded with Gruz Client";
            int tw = g.getFontMetrics().stringWidth(text);
            BufferedImage lg = logo();
            int ls = lg != null ? Math.round(size * 1.4f) : 0, gap = lg != null ? size / 2 : 0;
            int x = (w - ls - gap - tw) / 2, base = h - Math.max(6, h / 40);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            if (lg != null) g.drawImage(lg, x, base - ls + size / 5, ls, ls, null);
            // TextLayout zamiast Graphics2D.drawString: preprocesor w 26.x podmienia nazwę drawString (GuiGraphics)
            java.awt.font.TextLayout tl = new java.awt.font.TextLayout(text, font, g.getFontRenderContext());
            g.setColor(new Color(0, 0, 0, 140));
            tl.draw(g, x + ls + gap + 1, base + 1);
            g.setColor(Color.WHITE);
            tl.draw(g, x + ls + gap, base);
        } finally {
            g.dispose();
        }
    }

    private static BufferedImage logo() {
        if (!logoTried) {
            logoTried = true;
            try (InputStream in = Replay.class.getResourceAsStream("/assets/gruzclient/textures/gui/logo.png")) {
                if (in != null) logo = ImageIO.read(in);
            } catch (Exception ignored) { /* bez logo zostaje sam napis */ }
        }
        return logo;
    }
}
