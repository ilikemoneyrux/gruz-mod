package pl.gruzclient.module.impl;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/**
 * Kolor błysku przy trafieniu. Minecraft barwi uderzone encje górną połową tekstury "overlay" –
 * podmieniamy jej kolor (mixin OverlayTextureMixin przekazuje teksturę po utworzeniu).
 */
public class HitColorModule extends Module {
    public final Setting.Color color = add(new Setting.Color("color", "setting.color", 1));
    public final Setting.Number opacity = add(new Setting.Number("opacity", "setting.opacity", 70f, 10f, 100f, 5f, "%"));

    private static NativeImageBackedTexture texture;
    private int appliedHash = 0;

    public HitColorModule() { super("hitcolor", Category.PVP, false); }

    public static void setTexture(NativeImageBackedTexture t) { texture = t; }

    @Override
    public void tick() { apply(false); }

    @Override
    protected void onEnable() { apply(true); }

    @Override
    protected void onDisable() { apply(true); }

    /** Przepisuje 8 górnych wierszy tekstury overlay (kolor obrażeń) */
    public void apply(boolean force) {
        if (texture == null) return;
        int alpha = isEnabled() ? Math.round(opacity.value / 100f * 255f) : 0xB2;
        int rgb = isEnabled() ? color.rgb() : 0xFF0000;
        int hash = alpha * 31 + rgb;
        if (!force && hash == appliedHash) return;
        appliedHash = hash;
        NativeImage img = texture.getImage();
        if (img == null) return;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int abgr = (alpha << 24) | (b << 16) | (g << 8) | r; // NativeImage w 1.21.1 trzyma kolory jako ABGR
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColor(x, y, abgr);
        texture.upload();
    }
}
