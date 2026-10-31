package pl.gruzclient.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
//#if MC >= 1.21.2
import net.minecraft.client.renderer.RenderType;
//#else
import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC < 1.21.2
import org.joml.Vector3f;
//#endif

/**
 * Różnice w API Minecrafta między wersjami w jednym miejscu.
 * Reszta kodu woła te metody zamiast bezpośrednio API, które zmienia się co wersję.
 */
public final class Compat {
    private Compat() {}

    public static ResourceLocation id(String ns, String path) {
        //#if MC >= 1.21
        return ResourceLocation.fromNamespaceAndPath(ns, path);
        //#else
        return new ResourceLocation(ns, path);
        //#endif
    }

    // ---------------- rysowanie tekstur w GUI ----------------

    /** Fragment tekstury (u, v, rw x rh z tekstury tw x th) narysowany jako prostokąt w x h, z kolorem ARGB */
    public static void blit(GuiGraphics c, ResourceLocation tex, int x, int y, int w, int h,
                            float u, float v, int rw, int rh, int tw, int th, int argb) {
        //#if MC >= 1.21.2
        c.blit(RenderType::guiTextured, tex, x, y, u, v, w, h, rw, rh, tw, th, argb);
        //#else
        RenderSystem.enableBlend();
        c.setColor(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
        c.blit(tex, x, y, w, h, u, v, rw, rh, tw, th);
        c.setColor(1f, 1f, 1f, 1f);
        //#endif
    }

    /** Cała tekstura size x size w (x, y) */
    public static void blitWhole(GuiGraphics c, ResourceLocation tex, int x, int y, int w, int h, int argb) {
        blit(c, tex, x, y, w, h, 0f, 0f, w, h, w, h, argb);
    }

    public static void sprite(GuiGraphics c, TextureAtlasSprite sprite, int x, int y, int w, int h) {
        //#if MC >= 1.21.2
        c.blitSprite(RenderType::guiTextured, sprite, x, y, w, h);
        //#else
        c.blit(x, y, 0, w, h, sprite);
        //#endif
    }

    /** Twarz gracza z przezroczystością */
    public static void face(GuiGraphics c, net.minecraft.client.multiplayer.PlayerInfo info, int x, int y, int size, float alpha) {
        //#if MC >= 1.21.2
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(c, info.getSkin(), x, y, size, argb(alpha));
        //#else
        c.setColor(1f, 1f, 1f, alpha);
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(c, info.getSkin(), x, y, size);
        c.setColor(1f, 1f, 1f, 1f);
        //#endif
    }

    /** Twarz własnego gracza (menu główne) */
    public static void ownFace(GuiGraphics c, Minecraft mc, int x, int y, int size, float alpha) {
        var skin = mc.getSkinManager().getInsecureSkin(mc.getGameProfile());
        //#if MC >= 1.21.2
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(c, skin, x, y, size, argb(alpha));
        //#else
        c.setColor(1f, 1f, 1f, alpha);
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(c, skin, x, y, size);
        c.setColor(1f, 1f, 1f, 1f);
        //#endif
    }

    private static int argb(float alpha) {
        return ((Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f)) << 24) | 0xFFFFFF;
    }

    public static void panorama(PanoramaRenderer p, GuiGraphics c, int w, int h, float alpha, float delta) {
        //#if MC >= 1.20.5
        p.render(c, w, h, alpha, delta);
        //#else
        p.render(delta, alpha);
        //#endif
    }

    // ---------------- czas klatki ----------------

    public static float partialTick(Minecraft mc) {
        //#if MC >= 1.21.2
        return mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        //#elif MC >= 1.21
        return mc.getTimer().getGameTimeDeltaPartialTick(true);
        //#else
        return mc.getFrameTime();
        //#endif
    }

    // ---------------- serwery ----------------

    public static ServerData server(String name, String address) {
        return new ServerData(name, address, ServerData.Type.OTHER);
    }

    public static void connect(Screen parent, Minecraft mc, String address, ServerData info) {
        //#if MC >= 1.20.5
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address), info, false, null);
        //#else
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address), info, false);
        //#endif
    }

    // ---------------- obrazki (NativeImage: piksele jako ABGR jak w starszych wersjach) ----------------

    public static void setPixelABGR(NativeImage img, int x, int y, int abgr) {
        //#if MC >= 1.21.2
        img.setPixel(x, y, abgrToArgb(abgr));
        //#else
        img.setPixelRGBA(x, y, abgr);
        //#endif
    }

    public static int getPixelABGR(NativeImage img, int x, int y) {
        //#if MC >= 1.21.2
        return abgrToArgb(img.getPixel(x, y));
        //#else
        return img.getPixelRGBA(x, y);
        //#endif
    }

    /** ABGR <-> ARGB (zamiana kanałów R i B; działa w obie strony) */
    public static int abgrToArgb(int c) {
        return (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
    }

    public static int mapColorABGR(MapColor color, MapColor.Brightness b) {
        //#if MC >= 1.21.2
        return abgrToArgb(color.calculateARGBColor(b));
        //#else
        return color.calculateRGBColor(b);
        //#endif
    }

    public static int minY(Level level) {
        //#if MC >= 1.21.2
        return level.getMinY();
        //#else
        return level.getMinBuildHeight();
        //#endif
    }

    // ---------------- efekty / cząsteczki ----------------

    public static String effectName(MobEffectInstance e) {
        //#if MC >= 1.20.5
        return e.getEffect().value().getDisplayName().getString();
        //#else
        return e.getEffect().getDisplayName().getString();
        //#endif
    }

    public static DustParticleOptions dust(int rgb, float scale) {
        //#if MC >= 1.21.2
        return new DustParticleOptions(rgb & 0xFFFFFF, scale);
        //#else
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), scale);
        //#endif
    }
}
