//#swap MC < 1.20.2 | info.getSkin() => info.getSkinLocation()
//#swap MC < 1.20.2 | getInsecureSkin(mc.getGameProfile()) => getInsecureSkinLocation(mc.getUser().getGameProfile())
package pl.gruzclient.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
//#if MC < 26.1
import net.minecraft.client.renderer.PanoramaRenderer;
//#endif
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
//#if MC >= 1.21.6
import net.minecraft.client.renderer.RenderPipelines;
//#elif MC >= 1.21.2
import net.minecraft.client.renderer.RenderType;
//#else
import com.mojang.blaze3d.systems.RenderSystem;
//#endif
import net.minecraft.client.renderer.texture.DynamicTexture;
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
        //#if MC >= 1.21.6
        c.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th, argb);
        //#elif MC >= 1.21.2
        c.blit(RenderType::guiTextured, tex, x, y, u, v, w, h, rw, rh, tw, th, argb);
        //#else
        blend(false);
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
        //#if MC >= 1.21.6
        c.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
        //#elif MC >= 1.21.2
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
        //#if MC >= 1.21.9
        var skin = mc.getSkinManager().createLookup(mc.getGameProfile(), false).get();
        //#else
        var skin = mc.getSkinManager().getInsecureSkin(mc.getGameProfile());
        //#endif
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

    //#if MC < 26.1
    public static void panorama(PanoramaRenderer p, GuiGraphics c, int w, int h, float alpha, float delta) {
        //#if MC >= 1.21.6
        p.render(c, w, h, true);
        //#elif MC >= 1.20.5
        p.render(c, w, h, alpha, delta);
        //#else
        p.render(delta, alpha);
        //#endif
    }
    //#endif

    // ---------------- tekstury ----------------

    /** Tekstura z obrazka; smooth = gładkie skalowanie (tam, gdzie wersja na to pozwala) */
    public static DynamicTexture texture(String name, NativeImage img, boolean smooth) {
        //#if MC >= 1.21.5
        DynamicTexture t = new DynamicTexture(() -> name, img);
        //#else
        DynamicTexture t = new DynamicTexture(img);
        //#endif
        //#if MC < 1.21.6
        if (smooth) t.setFilter(true, false);
        //#endif
        return t;
    }

    /** Pusta tekstura do rysowania pikseli (mapa, peleryny) */
    public static DynamicTexture texture(String name, int w, int h) {
        //#if MC >= 1.21.5
        return new DynamicTexture(() -> name, w, h, true);
        //#else
        return new DynamicTexture(w, h, true);
        //#endif
    }

    /** Ustawienia mieszania kolorów przed rysowaniem (od 1.21.5 robi to pipeline) */
    public static void blend(boolean additive) {
        //#if MC < 1.21.5
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        if (additive) com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        else com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        //#endif
    }

    // ---------------- różne ----------------

    /** Przedmiot do podglądu. Od 26.1 przedmiotów nie da się tworzyć przed wejściem do świata – wtedy pusty. */
    public static net.minecraft.world.item.ItemStack stack(net.minecraft.world.item.Item item) {
        //#if MC >= 26.1
        if (Minecraft.getInstance().level == null) return net.minecraft.world.item.ItemStack.EMPTY;
        //#endif
        return new net.minecraft.world.item.ItemStack(item);
    }

    /** Tekst na czacie, który po kliknięciu wpisuje komendę do pola czatu (ClickEvent to rekordy od 1.21.5) */
    public static net.minecraft.network.chat.MutableComponent suggest(net.minecraft.network.chat.MutableComponent c, String command) {
        //#if MC >= 1.21.5
        return c.withStyle(s -> s.withClickEvent(new net.minecraft.network.chat.ClickEvent.SuggestCommand(command)));
        //#else
        return c.withStyle(s -> s.withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, command)));
        //#endif
    }

    /** Link na czacie (kliknięcie pyta i otwiera przeglądarkę) */
    public static net.minecraft.network.chat.MutableComponent openUrl(net.minecraft.network.chat.MutableComponent c, String url) {
        //#if MC >= 1.21.5
        return c.withStyle(s -> s.withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url))));
        //#else
        return c.withStyle(s -> s.withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, url)));
        //#endif
    }

    /** Wiadomość dla gracza: na czacie albo nad paskiem (overlay) */
    public static void chat(net.minecraft.world.entity.player.Player p, net.minecraft.network.chat.Component c, boolean overlay) {
        //#if MC >= 26.1
        if (overlay) p.sendOverlayMessage(c); else p.sendSystemMessage(c);
        //#else
        p.displayClientMessage(c, overlay);
        //#endif
    }

    /** Pora dnia świata w tickach (od 26.1 czas liczą "zegary") */
    public static long dayTime(Level level) {
        //#if MC >= 26.1
        return level.getDefaultClockTime();
        //#else
        return level.getDayTime();
        //#endif
    }

    public static long windowHandle() {
        //#if MC >= 1.21.9
        return Minecraft.getInstance().getWindow().handle();
        //#else
        return Minecraft.getInstance().getWindow().getWindow();
        //#endif
    }

    /** Styl tekstu z własną czcionką z zasobów */
    public static net.minecraft.network.chat.Style font(ResourceLocation font) {
        //#if MC >= 1.21.9
        return net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(font));
        //#else
        return net.minecraft.network.chat.Style.EMPTY.withFont(font);
        //#endif
    }

    public static boolean isSword(net.minecraft.world.item.ItemStack stack) {
        //#if MC >= 1.21.5
        return stack.is(net.minecraft.tags.ItemTags.SWORDS);
        //#else
        return stack.getItem() instanceof net.minecraft.world.item.SwordItem;
        //#endif
    }

    public static String profileName(com.mojang.authlib.GameProfile p) {
        //#if MC >= 1.21.9
        return p.name();
        //#else
        return p.getName();
        //#endif
    }

    public static java.util.UUID profileId(com.mojang.authlib.GameProfile p) {
        //#if MC >= 1.21.9
        return p.id();
        //#else
        return p.getId();
        //#endif
    }

    /** Potwierdzenie konta w Mojangu (tak jak przy wejściu na serwer) */
    public static void joinServer(Minecraft mc, String serverId) throws Exception {
        var user = mc.getUser();
        //#if MC >= 1.21.9
        mc.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
        //#elif MC >= 1.20.2
        mc.getMinecraftSessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
        //#else
        mc.getMinecraftSessionService().joinServer(user.getGameProfile(), user.getAccessToken(), serverId);
        //#endif
    }

    /** Wyjście ze świata / z serwera */
    public static void leaveWorld(Minecraft mc) {
        if (mc.level == null) return;
        //#if MC >= 1.21.9
        mc.disconnectFromWorld(net.minecraft.network.chat.Component.empty());
        //#elif MC >= 1.20.2
        mc.level.disconnect();
        mc.disconnect();
        //#else
        mc.level.disconnect();
        mc.clearLevel();
        //#endif
    }

    /** Założona zbroja od hełmu do butów */
    public static java.util.List<net.minecraft.world.item.ItemStack> armor(net.minecraft.world.entity.player.Player p) {
        java.util.List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>();
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            out.add(p.getItemBySlot(slot));
        }
        return out;
    }

    /** Wszystkie przedmioty gracza (ekwipunek, zbroja, druga ręka) */
    public static java.util.List<net.minecraft.world.item.ItemStack> allItems(net.minecraft.world.entity.player.Player p) {
        var inv = p.getInventory();
        java.util.List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) out.add(inv.getItem(i));
        return out;
    }

    /** Ikona efektu mikstury */
    public static void effectIcon(GuiGraphics c, MobEffectInstance e, int x, int y, int size) {
        //#if MC >= 1.21.9
        c.blitSprite(RenderPipelines.GUI_TEXTURED, net.minecraft.client.gui.Gui.getMobEffectSprite(e.getEffect()), x, y, size, size);
        //#else
        sprite(c, Minecraft.getInstance().getMobEffectTextures().get(e.getEffect()), x, y, size, size);
        //#endif
    }

    public static void hitboxes(Minecraft mc, boolean on) {
        //#if MC >= 1.21.9
        mc.debugEntries.setStatus(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES,
                on ? net.minecraft.client.gui.components.debug.DebugScreenEntryStatus.ALWAYS_ON : net.minecraft.client.gui.components.debug.DebugScreenEntryStatus.NEVER);
        //#else
        mc.getEntityRenderDispatcher().setRenderHitBoxes(on);
        //#endif
    }

    public static String dimensionId(Level level) {
        //#if MC >= 1.21.11
        return level.dimension().identifier().toString();
        //#else
        return level.dimension().location().toString();
        //#endif
    }

    public static String mcVersion() {
        //#if MC >= 1.21.6
        return net.minecraft.SharedConstants.getCurrentVersion().name();
        //#else
        return net.minecraft.SharedConstants.getCurrentVersion().getName();
        //#endif
    }

    public static void panoramaShot(Minecraft mc, java.io.File dir) {
        //#if MC >= 1.21.6
        mc.grabPanoramixScreenshot(dir);
        //#else
        mc.grabPanoramixScreenshot(dir, 1024, 1024);
        //#endif
    }

    /** Czy gracz próbuje się ruszyć (klawisze ruchu albo skok) */
    public static boolean moving(net.minecraft.client.player.LocalPlayer p) {
        if (p.input == null) return false;
        //#if MC >= 1.21.5
        return p.input.getMoveVector().lengthSquared() > 0 || p.input.keyPresses.jump();
        //#elif MC >= 1.21.2
        return p.input.forwardImpulse != 0 || p.input.leftImpulse != 0 || p.input.keyPresses.jump();
        //#else
        return p.input.forwardImpulse != 0 || p.input.leftImpulse != 0 || p.input.jumping;
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
        //#if MC >= 1.20.2
        return new ServerData(name, address, ServerData.Type.OTHER);
        //#else
        return new ServerData(name, address, false);
        //#endif
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
