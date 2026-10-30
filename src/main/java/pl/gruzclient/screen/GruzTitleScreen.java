package pl.gruzclient.screen;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pl.gruzclient.GruzClient;
import pl.gruzclient.config.GruzConfig;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.MenuModule;
import pl.gruzclient.network.NewsFeed;
import pl.gruzclient.render.Ui;

import java.util.ArrayList;
import java.util.List;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Menu główne Gruz Client w stylu nowoczesnych klientów:
 * rozmyta panorama, logo, kolumna dużych przycisków z ikonami, pasek ikon u góry z graczem,
 * karty z newsami i skrótami po lewej, "Wyjdź z gry" na dole.
 */
public class GruzTitleScreen extends Screen {
    private static boolean firstOpen = true;
    private static final CrystalField CRYSTALS = new CrystalField(26);
    private static RotatingCubeMapRenderer ownPanorama;

    private final List<UiButton> buttons = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();
    private final boolean slowIntro;
    private long last = 0;
    private int tipIndex = (int) (System.currentTimeMillis() / 1000 % 4);

    public GruzTitleScreen() {
        super(Text.literal("Gruz Client"));
        this.slowIntro = firstOpen;
        firstOpen = false;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private int logoTop() { return Math.max(10, Math.round(this.height * 0.12f)); }
    private int logoSize() { return Math.round(Math.min(64f, this.height * 0.17f)); }

    @Override
    protected void init() {
        buttons.clear();
        int bw = Math.min(190, this.width - 40), bh = 24, gap = 6;
        int cx = this.width / 2;
        int top = logoTop() + logoSize() + 30;
        int y = top;

        add(new UiButton(cx - bw / 2, y, bw, bh, Ui.Icon.USER, Lang.t("menu.singleplayer"),
                b -> this.client.setScreen(new SelectWorldScreen(this))).big());
        y += bh + gap;
        add(new UiButton(cx - bw / 2, y, bw, bh, Ui.Icon.USERS, Lang.t("menu.multiplayer"),
                b -> this.client.setScreen(new MultiplayerScreen(this))).big());
        y += bh + gap;
        add(new UiButton(cx - bw / 2, y, bw, bh, Ui.Icon.GRID, Lang.t("menu.mods"),
                b -> this.client.setScreen(new GruzModsScreen(this))).big());
        y += bh + gap;
        add(new UiButton(cx - bw / 2, y, bw, bh, Ui.Icon.SHIRT, Lang.t("menu.cosmetics"),
                b -> this.client.setScreen(new GruzModsScreen(this, Module.Category.COSMETICS))).big());
        y += bh + gap;

        // akcent: szybki powrót na ostatni serwer
        String last = GruzConfig.get().lastServer;
        if (last != null && !last.isBlank()) {
            String name = GruzConfig.get().lastServerName == null || GruzConfig.get().lastServerName.isBlank() ? last : GruzConfig.get().lastServerName;
            add(new UiButton(cx - bw / 2, y, bw, bh + 2, Ui.Icon.STAR, Lang.t("menu.rejoin").replace("{name}", name),
                    b -> rejoin(last, name)).kind(UiButton.Kind.ACCENT).big());
            y += bh + 2 + gap;
        }

        // wyjście, niżej i węższe
        int qw = Math.min(130, bw - 40);
        int qy = Math.min(this.height - 54, y + 16);
        add(new UiButton(cx - qw / 2, qy, qw, 22, Ui.Icon.POWER, Lang.t("menu.quitGame"),
                b -> this.client.scheduleStop()).kind(UiButton.Kind.DANGER).big());

        // pasek ikon (prawy górny róg)
        int s = 22, ix = this.width - 10 - s;
        add(new UiButton(ix, 10, s, s, Ui.Icon.GLOBE, null, b -> { Lang.cycle(); this.clearAndInit(); })
                .kind(UiButton.Kind.ICON).badge(Lang.badge().replace("*", "")));
        ix -= s + 5;
        add(new UiButton(ix, 10, s, s, Ui.Icon.GEAR, null, b -> this.client.setScreen(new OptionsScreen(this, this.client.options))).kind(UiButton.Kind.ICON));
        ix -= s + 5;
        add(new UiButton(ix, 10, s, s, Ui.Icon.CAMERA, null,
                b -> Util.getOperatingSystem().open(this.client.runDirectory.toPath().resolve("screenshots").toFile())).kind(UiButton.Kind.ICON));
        ix -= s + 5;
        add(new UiButton(ix, 10, s, s, Ui.Icon.GRID, null, b -> this.client.setScreen(new GruzModsScreen(this))).kind(UiButton.Kind.ICON));
        chipRight = ix - 6;
    }

    private int chipRight;

    private void rejoin(String address, String name) {
        // typ "zwykły serwer" (ostatni w enumie: LAN, REALM, OTHER)
        ServerInfo.ServerType[] types = ServerInfo.ServerType.values();
        ServerInfo info = new ServerInfo(name, address, types[types.length - 1]);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(address), info, false, null);
    }

    private UiButton add(UiButton b) {
        buttons.add(b);
        return this.addDrawableChild(b);
    }

    private float reveal(long delayMs, long durMs) {
        float scale = slowIntro ? 1f : 0.35f;
        long base = slowIntro ? 900 : 0;
        float t = (System.currentTimeMillis() - openedAt - base - delayMs * scale) / (durMs * scale);
        return outExpo(clamp01(t));
    }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        int w = this.width, h = this.height;
        float bg = reveal(0, 1400);

        if (Modules.MENU.usePanorama()) {
            if (ownPanorama == null) ownPanorama = new RotatingCubeMapRenderer(new CubeMapRenderer(MenuModule.PANORAMA));
            ownPanorama.render(c, w, h, 1f, delta);
        } else if (Modules.MENU.crystals()) {
            c.fill(0, 0, w, h, 0xFF000000);
        } else {
            this.renderPanoramaBackground(c, delta);
        }
        if (!Modules.MENU.crystals()) {
            this.applyBlur(delta);
        }
        // przyciemnienie + delikatny kolor motywu od dołu
        c.fill(0, 0, w, h, argb(0.45f, 0x000000));
        c.fillGradient(0, h / 3, w, h, 0x00000000, argb(0.35f, NEON_DEEP));
        c.fillGradient(0, 0, w, h / 4, argb(0.5f, 0x000000), 0x00000000);

        // poświata za logo + kryształy (tylko w trybie "Kryształy")
        float ls = logoSize();
        float lcx = w / 2f, lcy = logoTop() + ls / 2f;
        float breathe = 0.5f + 0.5f * (float) Math.sin(seconds() * 1.4f);
        float gs = ls * (4.2f + 0.2f * breathe);
        sprite(c, GLOW, 256, lcx - gs / 2f, lcy - gs / 2f, gs, gs, bg * (0.28f + 0.08f * breathe), true, 0f);
        if (Modules.MENU.isEnabled() && Modules.MENU.seasonal.value) pl.gruzclient.render.SeasonFx.render(c, w, h, dt, bg);
        if (Modules.MENU.crystals()) CRYSTALS.render(c, w, h, potato() ? 0f : dt, bg * 0.8f);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        for (int i = 0; i < buttons.size(); i++) buttons.get(i).setReveal(reveal(400 + i * 60L, 800));
        this.renderBackground(c, mouseX, mouseY, delta);
        int w = this.width, h = this.height;
        float t = seconds();

        // logo + napis
        float rl = reveal(0, 1200);
        float ls = logoSize();
        float s = ls * (0.88f + 0.12f * rl) * (1f + 0.012f * (float) Math.sin(t * 1.6f));
        sprite(c, LOGO, 256, w / 2f - s / 2f, logoTop() + ls / 2f - s / 2f, s, s, rl, false, (float) Math.sin(t * 0.5f) * 0.035f);
        float rt = reveal(200, 900);
        if (textVisible(rt)) {
            Ui.drawScaled(c, Ui.bold("GRUZ CLIENT"), w / 2f, logoTop() + ls + 6 + (1f - rt) * 5f, 1.6f, argb(rt, 0xFFFFFF), true);
        }

        for (var child : this.children()) if (child instanceof UiButton b) b.render(c, mouseX, mouseY, delta);

        drawPlayerChip(c, reveal(600, 900));
        drawCards(c, reveal(700, 900));

        float rv = reveal(900, 900);
        if (textVisible(rv)) {
            Text v = Ui.text("Gruz Client " + GruzClient.version() + "  ·  Minecraft " + SharedConstants.getGameVersion().getName() + "  ·  Fabric");
            Ui.drawCentered(c, v, w / 2, h - 14, argb(rv * 0.7f, MUTE));
            Text cr = Ui.text("Copyright Mojang AB. Do not distribute!");
            Ui.draw(c, cr, w - Ui.width(cr) - 8, h - 14, argb(rv * 0.45f, MUTE));
        }
    }

    /** Karta gracza obok ikon (głowa + nick) */
    private void drawPlayerChip(DrawContext c, float a) {
        if (!textVisible(a) || this.client.getGameProfile() == null) return;
        Text name = Ui.bold(this.client.getSession().getUsername());
        int cw = Ui.width(name) + 34, x = chipRight - cw, y = 10;
        Ui.round(c, x, y, cw, 22, 4, argb(a * 0.8f, 0x0A0810));
        Ui.roundBorder(c, x, y, cw, 22, 4, argb(a * 0.1f, 0xFFFFFF));
        SkinTextures skin = this.client.getSkinProvider().getSkinTextures(this.client.getGameProfile());
        c.setShaderColor(1f, 1f, 1f, a);
        PlayerSkinDrawer.draw(c, skin, x + 5, y + 4, 14);
        c.setShaderColor(1f, 1f, 1f, 1f);
        c.drawText(Ui.tr(), name, x + 25, y + 7, argb(a, 0xFFFFFF), false);
    }

    /** Karty po lewej: news z serwera Gruz (albo wskazówka) i skróty klawiszowe */
    private void drawCards(DrawContext c, float a) {
        if (!textVisible(a) || this.width < 420) return;
        int x = 10 - Math.round((1f - a) * 12), y = 10, cw = Math.min(150, this.width / 4);

        NewsFeed.Item n = NewsFeed.latest();
        String season = pl.gruzclient.cosmetics.CosmeticSet.season();
        if (n == null && season != null) {
            n = new NewsFeed.Item(Lang.t("season." + season), Lang.t("season.cardTitle." + season),
                    Lang.t("season.cardBody").replace("{d}", String.valueOf(pl.gruzclient.render.SeasonFx.daysLeft())));
        }
        String tag = n != null ? n.tag() : Lang.t("menu.tipTag");
        String title = n != null ? n.title() : Lang.t("menu.tip" + tipIndex + ".title");
        String body = n != null ? n.body() : Lang.t("menu.tip" + tipIndex);
        var lines = Ui.tr().wrapLines(Ui.text(body), cw - 16);
        int lines2 = Math.min(3, lines.size());
        int ch = 34 + lines2 * 10;
        Ui.panel(c, x, y, cw, ch, a);
        c.fill(x + 8, y + 8, x + 10, y + 16, argb(a, NEON));
        Ui.draw(c, Ui.bold(tag.toUpperCase()), x + 14, y + 8, argb(a, NEON_HOT));
        Ui.draw(c, Ui.fit(title, cw - 16, true), x + 8, y + 20, argb(a, 0xFFFFFF));
        for (int i = 0; i < lines2; i++) c.drawText(Ui.tr(), lines.get(i), x + 8, y + 32 + i * 10, argb(a * 0.85f, MUTE), false);
        y += ch + 6;

        String[][] keys = {
                {Lang.t("menu.key.mods"), "R-Shift"}, {Lang.t("menu.key.emotes"), "B"}, {Lang.t("menu.key.map"), "M"}, {Lang.t("menu.key.zoom"), "C"},
        };
        int kh = 22 + keys.length * 12;
        Ui.panel(c, x, y, cw, kh, a);
        Ui.icon(c, Ui.Icon.LAYOUT, x + 8, y + 7, 9, argb(a, NEON_HOT));
        Ui.draw(c, Ui.bold(Lang.t("menu.shortcuts")), x + 21, y + 8, argb(a, 0xFFFFFF));
        for (int i = 0; i < keys.length; i++) {
            int ky = y + 22 + i * 12;
            Ui.draw(c, Ui.text(keys[i][0]), x + 8, ky, argb(a * 0.9f, 0xD6CCE6));
            Text k = Ui.bold(keys[i][1]);
            int kw = Ui.width(k) + 8;
            Ui.round(c, x + cw - 8 - kw, ky - 2, kw, 11, 2, argb(a * 0.9f, 0x1E1630));
            c.drawText(Ui.tr(), k, x + cw - 4 - kw, ky, argb(a, NEON_HOT), false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // klik w kartę z wskazówką przełącza na następną
        if (NewsFeed.latest() == null && this.width >= 420 && Ui.inside(mx, my, 10, 10, Math.min(150, this.width / 4), 64)) {
            tipIndex = (tipIndex + 1) % 4;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
