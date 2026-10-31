package pl.gruzclient.module;

import pl.gruzclient.compat.Compat;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import pl.gruzclient.config.GruzConfig;
import pl.gruzclient.module.impl.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Rejestr wszystkich modułów + zapis/odczyt + rysowanie HUD. */
public final class Modules {
    private Modules() {}

    public static final ZoomModule ZOOM = new ZoomModule();
    public static final FreelookModule FREELOOK = new FreelookModule();
    public static final FpsModule FPS = new FpsModule();
    public static final CpsModule CPS = new CpsModule();
    public static final KeystrokesModule KEYSTROKES = new KeystrokesModule();
    public static final ArmorHudModule ARMOR = new ArmorHudModule();
    public static final PotionHudModule POTIONS = new PotionHudModule();
    public static final CoordsModule COORDS = new CoordsModule();
    public static final PingModule PING = new PingModule();
    public static final CrosshairModule CROSSHAIR = new CrosshairModule();
    public static final FullbrightModule FULLBRIGHT = new FullbrightModule();
    public static final ToggleSprintModule TOGGLE_SPRINT = new ToggleSprintModule();
    // PvP
    public static final TargetHudModule TARGET_HUD = new TargetHudModule();
    public static final ReachModule REACH = new ReachModule();
    public static final ComboModule COMBO = new ComboModule();
    public static final ItemCounterModule ITEMS = new ItemCounterModule();
    public static final HitColorModule HIT_COLOR = new HitColorModule();
    public static final HitSoundModule HIT_SOUND = new HitSoundModule();
    public static final LowHpModule LOW_HP = new LowHpModule();
    public static final RecordingModule RECORDING = new RecordingModule();
    public static final FightModule FIGHT = new FightModule();
    public static final ClipModule CLIP = new ClipModule();
    // Wygląd
    public static final BlockOverlayModule BLOCK_OVERLAY = new BlockOverlayModule();
    public static final LowFireModule LOW_FIRE = new LowFireModule();
    public static final NametagModule NAMETAGS = new NametagModule();
    public static final WorldModule WORLD = new WorldModule();
    public static final MotionBlurModule MOTION_BLUR = new MotionBlurModule();
    public static final OldAnimationsModule OLD_ANIM = new OldAnimationsModule();
    public static final ItemPhysicsModule ITEM_PHYSICS = new ItemPhysicsModule();
    public static final MenuModule MENU = new MenuModule();
    public static final HitboxesModule HITBOXES = new HitboxesModule();
    public static final ScoreboardModule SCOREBOARD = new ScoreboardModule();
    public static final HudTweaksModule HUD_TWEAKS = new HudTweaksModule();
    public static final TabModule TAB = new TabModule();
    // Przydatne
    public static final AutoGGModule AUTO_GG = new AutoGGModule();
    public static final BetterChatModule BETTER_CHAT = new BetterChatModule();
    public static final FreecamModule FREECAM = new FreecamModule();
    // HUD (nowe)
    public static final MinimapModule MINIMAP = new MinimapModule();
    public static final SpeedModule SPEED = new SpeedModule();
    public static final ClockModule CLOCK = new ClockModule();
    // Kosmetyki
    public static final CosmeticsModule COSMETICS = new CosmeticsModule();

    public static final List<Module> ALL = List.of(
            // HUD
            FPS, CPS, KEYSTROKES, ARMOR, POTIONS, COORDS, PING, TARGET_HUD, REACH, COMBO, ITEMS,
            MINIMAP, SPEED, CLOCK, FIGHT,
            // PvP
            HIT_COLOR, HIT_SOUND, LOW_HP, RECORDING, CLIP,
            // Wygląd
            CROSSHAIR, BLOCK_OVERLAY, FULLBRIGHT, LOW_FIRE, NAMETAGS, WORLD, MOTION_BLUR, OLD_ANIM, ITEM_PHYSICS, MENU,
            HITBOXES, SCOREBOARD, HUD_TWEAKS, TAB,
            // Przydatne
            ZOOM, FREELOOK, FREECAM, TOGGLE_SPRINT, AUTO_GG, BETTER_CHAT,
            // Kosmetyki
            COSMETICS);

    public static List<HudModule> hud() {
        List<HudModule> list = new ArrayList<>();
        for (Module m : ALL) if (m instanceof HudModule h) list.add(h);
        return list;
    }

    public static void load() {
        GruzConfig cfg = GruzConfig.get();
        for (Module m : ALL) {
            JsonObject o = cfg.modules.get(m.id);
            if (o != null) {
                try { m.load(o); } catch (Exception ignored) { /* uszkodzony wpis – zostają domyślne */ }
            }
        }
        COSMETICS.applyLauncherChoice();
    }

    public static void save() {
        GruzConfig cfg = GruzConfig.get();
        for (Module m : ALL) cfg.modules.put(m.id, m.save());
        GruzConfig.save();
    }

    /** Klawisz centrum modów (edytor HUD) – domyślnie prawy Shift */
    public static int HUB_KEY = org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
    private static boolean hubWasDown;

    public static void tick() {
        for (Module m : ALL) if (m.isEnabled()) m.tick();
        Minecraft mc = Minecraft.getInstance();
        boolean down = keyDown(HUB_KEY);
        if (down && !hubWasDown && mc.level != null) mc.setScreen(new pl.gruzclient.screen.HudEditorScreen(null));
        hubWasDown = down;
    }

    /** Rysowanie elementów HUD w trakcie gry */
    public static void renderHud(GuiGraphics c) {
        Minecraft mc = Minecraft.getInstance();
        pl.gruzclient.status.Benchmark.frame(c);
        if (mc.options.hideGui || mc.player == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.screen instanceof pl.gruzclient.screen.HudEditorScreen) return; // edytor rysuje HUD sam, ponad rozmyciem
        int w = c.guiWidth(), h = c.guiHeight();
        for (HudModule m : hud()) if (m.isEnabled()) m.render(c, w, h, false);
        MINIMAP.renderCompass(c);
    }

    /** Czy klawisz jest wciśnięty (tylko gdy nie ma otwartego ekranu) */
    public static boolean keyDown(int code) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || code <= 0) return false;
        return InputConstants.isKeyDown(
                //#if MC >= 26.3
                //#elif MC >= 1.21.9
                mc.getWindow(),
                //#else
                mc.getWindow().getWindow(),
                //#endif
                code);
    }
}
