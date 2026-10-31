package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.GruzClient;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Tło menu głównego: kryształy albo panorama 360° z Twojego świata.
 * Panoramę robisz klawiszem (domyślnie F7) w dowolnym świecie.
 */
public class MenuModule extends Module {
    public final Setting.Choice background = add(new Setting.Choice("background", "setting.menuBackground", 0, "menu.vanilla", "menu.crystals", "menu.panorama"));
    public final Setting.Key key = add(new Setting.Key("key", "setting.panoramaKey", GLFW.GLFW_KEY_F7));
    public final Setting.Bool seasonal = add(new Setting.Bool("seasonal", "setting.seasonal", true));

    public static final ResourceLocation PANORAMA = Compat.id(GruzClient.MOD_ID, "panorama/menu");
    private boolean wasDown;
    private boolean pending;
    private boolean loaded;

    public MenuModule() { super("menu", Category.VISUAL, true); }

    private Path dir() { return mc.gameDirectory.toPath().resolve("config").resolve("gruzclient").resolve("panorama"); }

    public boolean hasPanorama() {
        for (int i = 0; i < 6; i++) if (!Files.exists(dir().resolve("panorama_" + i + ".png"))) return false;
        return true;
    }

    /** Czarne tło z kryształami (stary wygląd menu) */
    public boolean crystals() { return isEnabled() && background.index == 1; }

    public boolean usePanorama() { return isEnabled() && background.index == 2 && hasPanorama() && ensureLoaded(); }

    /** Ładuje 6 ścian panoramy jako tekstury gruzclient:panorama/menu_0.png … _5.png */
    private boolean ensureLoaded() {
        if (loaded) return true;
        try {
            for (int i = 0; i < 6; i++) {
                try (InputStream in = Files.newInputStream(dir().resolve("panorama_" + i + ".png"))) {
                    DynamicTexture t = Compat.texture("gruz_menu", NativeImage.read(in), true);
                    mc.getTextureManager().register(PANORAMA.withPath(PANORAMA.getPath() + "_" + i + ".png"), t);
                }
            }
            loaded = true;
        } catch (Exception e) {
            GruzClient.LOG.warn("Nie udało się wczytać panoramy", e);
        }
        return loaded;
    }

    @Override
    public void tick() {
        boolean down = Modules.keyDown(key.code);
        if (down && !wasDown && mc.level != null && mc.player != null) pending = true;
        wasDown = down;
        captureIfPending(); // tick jest poza rysowaniem klatki, więc można tu wyrenderować 6 ujęć
    }

    /** Wywoływane na końcu klatki (poza renderowaniem świata) – robi 6 zdjęć i zapisuje je jako tło menu */
    public void captureIfPending() {
        if (!pending) return;
        pending = false;
        try {
            File work = dir().resolve("capture").toFile();
            mc.grabPanoramixScreenshot(work, 1024, 1024);
            Path shots;
            try (Stream<Path> s = Files.walk(work.toPath())) {
                Optional<Path> first = s.filter(p -> p.getFileName().toString().equals("panorama_0.png")).findFirst();
                if (first.isEmpty()) throw new IllegalStateException("brak plików panoramy");
                shots = first.get().getParent();
            }
            Files.createDirectories(dir());
            for (int i = 0; i < 6; i++) {
                Files.copy(shots.resolve("panorama_" + i + ".png"), dir().resolve("panorama_" + i + ".png"), StandardCopyOption.REPLACE_EXISTING);
            }
            loaded = false;
            background.index = 2;
            Modules.save();
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("§d[Gruz] §f" + Lang.t("menu.panoramaSaved")), false);
        } catch (Exception e) {
            GruzClient.LOG.warn("Panorama nie wyszła", e);
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("§c[Gruz] " + Lang.t("menu.panoramaFailed")), false);
        }
    }
}
