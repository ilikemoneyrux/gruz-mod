package pl.gruzclient.network;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import pl.gruzclient.GruzClient;
import pl.gruzclient.compat.Compat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Udostępnianie screenów (0.5.5): po zrzucie ekranu (F2) na czacie pojawia się przycisk „Udostępnij”,
 * .share wysyła ostatni screen na serwer Gruz i daje link gruzclient.com/s/<id> (od razu w schowku).
 * Obrazek zmniejszamy do maks. 1920 px i zapisujemy jako JPEG (do ~690 KB), żeby zmieścił się w WebSocket.
 */
public final class Shots {
    private Shots() {}

    private static final int MAX_BYTES = 690 * 1024;
    private static long lastSeen = System.currentTimeMillis();
    private static String lastName = "";
    private static int ticks;
    /** Kiedy ruszyła wysyłka (0 = nic nie leci). Czas zamiast flagi: po zerwaniu połączenia odpowiedź nie przyjdzie, a .share ma dalej działać. */
    private static long uploadingSince;
    private static long dirSeen;

    private static File dir() { return new File(Minecraft.getInstance().gameDirectory, "screenshots"); }

    /** Najnowszy plik PNG w folderze screenshots (albo null) */
    private static File newest() {
        File[] files = dir().listFiles((d, n) -> n.toLowerCase(java.util.Locale.ROOT).endsWith(".png"));
        if (files == null) return null;
        File best = null;
        for (File f : files) if (best == null || f.lastModified() > best.lastModified()) best = f;
        return best;
    }

    /** Co tick: raz na sekundę sprawdza, czy pojawił się nowy screen (F2) */
    public static void tick() {
        if (++ticks % 20 != 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // nowy plik zmienia datę folderu – listę plików (wolną przy tysiącach screenów) czytamy tylko wtedy
        long dm = dir().lastModified();
        if (dm == dirSeen) return;
        dirSeen = dm;
        File f = newest();
        if (f == null || f.lastModified() <= lastSeen || f.getName().equals(lastName)) return;
        if (System.currentTimeMillis() - f.lastModified() < 700) { dirSeen = 0; return; } // gra jeszcze zapisuje plik
        lastSeen = f.lastModified();
        lastName = f.getName();
        if (!GruzNetwork.connected()) return;
        if (Modules.BETTER_CHAT.autoShare.value) { share(); return; }
        if (!Modules.BETTER_CHAT.shareButton.value) return;
        GruzNetwork.chat(Component.literal(Lang.t("shot.ready") + "  ").withStyle(ChatFormatting.GRAY)
                .append(Compat.suggest(Component.literal("[" + Lang.t("shot.btn") + "]").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), ".share")));
    }

    /** .share – wysyła ostatni screen */
    public static void share() {
        if (!GruzNetwork.connected()) { GruzNetwork.chat(Component.literal(Lang.t("net.offline")).withStyle(ChatFormatting.RED)); return; }
        if (System.currentTimeMillis() - uploadingSince < 30_000) return;
        File f = newest();
        if (f == null) { GruzNetwork.chat(Component.literal(Lang.t("shot.none")).withStyle(ChatFormatting.GRAY)); return; }
        Minecraft mc = Minecraft.getInstance();
        String server = mc.getCurrentServer() != null && !mc.isLocalServer() ? mc.getCurrentServer().ip : null;
        uploadingSince = System.currentTimeMillis();
        GruzNetwork.chat(Component.literal(Lang.t("shot.sending")).withStyle(ChatFormatting.GRAY));
        CompletableFuture.runAsync(() -> {
            try {
                BufferedImage src = ImageIO.read(f);
                if (src == null) throw new IllegalStateException("nie PNG");
                byte[] jpg = null;
                int w = 0, h = 0;
                for (int max : new int[]{1920, 1600, 1280}) {
                    float k = Math.min(1f, max / (float) Math.max(src.getWidth(), src.getHeight()));
                    w = Math.max(1, Math.round(src.getWidth() * k));
                    h = Math.max(1, Math.round(src.getHeight() * k));
                    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g.drawImage(src, 0, 0, w, h, null);
                    g.dispose();
                    for (float q : new float[]{0.88f, 0.8f, 0.7f, 0.6f}) {
                        jpg = jpeg(img, q);
                        if (jpg.length <= MAX_BYTES) break;
                    }
                    if (jpg.length <= MAX_BYTES) break;
                }
                if (jpg == null || jpg.length > MAX_BYTES) throw new IllegalStateException("za duży");
                JsonObject o = new JsonObject();
                o.addProperty("t", "shot.upload");
                o.addProperty("data", Base64.getEncoder().encodeToString(jpg));
                o.addProperty("w", w);
                o.addProperty("h", h);
                if (server != null) o.addProperty("server", server);
                GruzNetwork.sendRaw(o);
            } catch (Exception e) {
                GruzClient.LOG.warn("Gruz: nie udało się przygotować screena: {}", e.toString());
                mc.execute(() -> { uploadingSince = 0; GruzNetwork.chat(Component.literal(Lang.t("shot.err.SHOT_BAD")).withStyle(ChatFormatting.RED)); });
            }
        });
    }

    private static byte[] jpeg(BufferedImage img, float quality) throws java.io.IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam p = writer.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(quality);
            writer.write(null, new IIOImage(img, null, null), p);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /** Odpowiedź serwera: link (do schowka + przycisk „Otwórz”) albo błąd */
    static void onDone(String url) {
        uploadingSince = 0;
        Minecraft mc = Minecraft.getInstance();
        mc.keyboardHandler.setClipboard(url);
        MutableComponent line = Component.literal(Lang.t("shot.done") + " ").withStyle(ChatFormatting.GRAY)
                .append(Compat.openUrl(Component.literal(url.replaceFirst("^https://", "")).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE), url));
        GruzNetwork.chat(line);
    }

    static void onError(String code) {
        uploadingSince = 0;
        GruzNetwork.chat(Component.literal(Lang.t("shot.err." + code)).withStyle(ChatFormatting.RED));
    }
}
