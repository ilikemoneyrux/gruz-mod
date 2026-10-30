package pl.gruzclient.status;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pl.gruzclient.i18n.Lang;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Test wydajności uruchamiany z launchera (-Dgruz.benchmark=1).
 * Po wejściu do świata: 5 s rozgrzewki, potem 60 s pomiaru czasu każdej klatki.
 * Wynik (średni FPS i 1% low) trafia do gruz-benchmark.json, który czyta launcher.
 */
public final class Benchmark {
    private Benchmark() {}

    private static final long WARMUP = 5_000, DURATION = 60_000;
    private static final boolean ENABLED = Boolean.getBoolean("gruz.benchmark");
    private static long worldAt = 0, lastFrame = 0;
    private static float[] frames = new float[60 * 600];
    private static int count = 0;
    private static boolean done = false;
    private static String result = "";

    public static boolean active() { return ENABLED && !done; }

    /** Co klatkę (z rysowania HUD) */
    public static void frame(DrawContext c) {
        if (!ENABLED) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = Util.getMeasuringTimeNano() / 1_000_000L;
        if (mc.world == null || mc.player == null) { worldAt = 0; return; }
        if (worldAt == 0) { worldAt = now; lastFrame = 0; count = 0; }
        long t = now - worldAt;
        if (!done && t > WARMUP) {
            long nano = Util.getMeasuringTimeNano();
            if (lastFrame != 0 && count < frames.length) frames[count++] = (nano - lastFrame) / 1_000_000f;
            lastFrame = nano;
            if (t > WARMUP + DURATION) finish(mc);
        }
        // pasek na górze ekranu
        int w = c.getScaledWindowWidth();
        String msg;
        float prog;
        if (done) { msg = result; prog = 1f; }
        else if (t <= WARMUP) { msg = Lang.t("bench.warmup"); prog = 0f; }
        else {
            long left = (WARMUP + DURATION - t) / 1000;
            msg = Lang.t("bench.running").replace("{s}", String.valueOf(left));
            prog = (t - WARMUP) / (float) DURATION;
        }
        int bw = Math.max(200, mc.textRenderer.getWidth(msg) + 24), x = (w - bw) / 2, y = 22;
        c.fill(x, y, x + bw, y + 22, argb(0.8f, SLAB));
        c.fill(x, y + 20, x + Math.round(bw * prog), y + 22, argb(1f, NEON));
        c.drawText(mc.textRenderer, msg, (w - mc.textRenderer.getWidth(msg)) / 2, y + 7, argb(1f, TEXT), false);
    }

    private static void finish(MinecraftClient mc) {
        done = true;
        if (count < 30) return;
        float[] f = Arrays.copyOf(frames, count);
        double total = 0;
        for (float v : f) total += v;
        double avg = 1000.0 * count / total;
        Arrays.sort(f);
        // 1% low = średnia z 1% najwolniejszych klatek
        int n = Math.max(1, count / 100);
        double slow = 0;
        for (int i = count - n; i < count; i++) slow += f[i];
        double low1 = 1000.0 / (slow / n);
        JsonObject o = new JsonObject();
        o.addProperty("avg", Math.round(avg));
        o.addProperty("low1", Math.round(low1));
        o.addProperty("frames", count);
        o.addProperty("at", System.currentTimeMillis());
        o.addProperty("render", mc.options.getViewDistance().getValue());
        try {
            Files.writeString(mc.runDirectory.toPath().resolve("gruz-benchmark.json"), o.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        result = Lang.t("bench.done").replace("{avg}", String.valueOf(Math.round(avg))).replace("{low}", String.valueOf(Math.round(low1)));
        if (mc.player != null) mc.player.sendMessage(Text.literal("§d[Gruz] §f" + result), false);
    }
}
