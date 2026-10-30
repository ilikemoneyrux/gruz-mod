package pl.gruzclient.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import pl.gruzclient.GruzClient;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/** Wspólne narzędzia rysowania: kolory, easing i tekstury z poświatą. */
public final class GruzDraw {
    private GruzDraw() {}

    // paleta – ta sama co w launcherze
    public static final int VOID = 0x000000;
    public static final int SLAB = 0x0C0912;
    public static final int SEAM = 0x2B2139;
    // kolor motywu przychodzi z launchera (-Dgruz.accent=b45cff)
    public static int NEON = accent();
    public static int NEON_HOT = mix(NEON, 0xFFFFFF, 0.45f);
    public static int NEON_DEEP = mix(NEON, 0x000000, 0.4f);
    public static final int TEXT = 0xF1ECF8;
    public static final int MUTE = 0x9488A8;

    public static final Identifier LOGO = Identifier.of(GruzClient.MOD_ID, "textures/gui/logo.png");
    public static final Identifier GLOW = Identifier.of(GruzClient.MOD_ID, "textures/gui/glow.png");
    public static final Identifier CRYSTAL = Identifier.of(GruzClient.MOD_ID, "textures/gui/crystal.png");

    private static final Set<Identifier> LOADED = new HashSet<>();

    /**
     * Tekstury ładujemy sami prosto z pliku moda – dzięki temu działają już na ekranie
     * ładowania, zanim Minecraft wczyta paczki zasobów.
     */
    public static Identifier tex(Identifier id) {
        if (LOADED.contains(id)) return id;
        String path = "/assets/" + id.getNamespace() + "/" + id.getPath();
        try (InputStream in = GruzDraw.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Brak pliku " + path);
            NativeImageBackedTexture t = new NativeImageBackedTexture(NativeImage.read(in));
            t.setFilter(true, false); // gładkie skalowanie
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, t);
        } catch (Exception e) {
            GruzClient.LOG.error("Nie udało się wczytać tekstury {}", id, e);
        }
        LOADED.add(id);
        return id;
    }

    /** Rysuje kwadratową teksturę w pozycji float z przezroczystością; additive = świecenie */
    public static void sprite(DrawContext c, Identifier id, int texSize, float x, float y, float w, float h,
                              float alpha, boolean additive, float rotation) {
        if (alpha <= 0.003f) return;
        tex(id);
        RenderSystem.enableBlend();
        if (additive) RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        c.setShaderColor(1f, 1f, 1f, Math.min(1f, alpha));
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x + w / 2f, y + h / 2f, 0);
        if (rotation != 0) m.multiply(RotationAxis.POSITIVE_Z.rotation(rotation));
        m.scale(w / texSize, h / texSize, 1f);
        m.translate(-texSize / 2f, -texSize / 2f, 0);
        c.drawTexture(id, 0, 0, 0f, 0f, texSize, texSize, texSize, texSize);
        m.pop();
        c.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
    }

    private static int accent() {
        try {
            String v = System.getProperty("gruz.accent", "b45cff").replace("#", "");
            return Integer.parseInt(v, 16) & 0xFFFFFF;
        } catch (Exception e) {
            return 0xB45CFF;
        }
    }

    public static int mix(int a, int b, float t) { return lerpColor(a, b, t); }

    public static boolean potato() { return Boolean.getBoolean("gruz.potato"); }

    public static int argb(float alpha, int rgb) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Tekst z alfą – Minecraft traktuje alfę < 4 jako pełną, więc bardzo przezroczysty tekst pomijamy */
    public static boolean textVisible(float alpha) { return alpha > 0.03f; }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(ar + (br - ar) * t) << 16) | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    public static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    public static float outExpo(float x) { return x >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * x); }
    public static float outCubic(float x) { float i = 1f - clamp01(x); return 1f - i * i * i; }
    public static float seconds() { return Util.getMeasuringTimeMs() / 1000f; }

    /** Wygładzanie niezależne od FPS */
    public static float damp(float current, float target, float lambda, float dt) {
        return current + (target - current) * (1f - (float) Math.exp(-lambda * dt));
    }
}
