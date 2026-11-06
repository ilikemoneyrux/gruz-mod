package pl.gruzclient.cosmetics;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import pl.gruzclient.compat.Compat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.CosmeticsModule;
import pl.gruzclient.network.GruzNetwork;

import java.util.UUID;

/**
 * Emotki we dwoje (od 0.5.2, płatne – kod emote:highfive / emote:hug / emote:duodance).
 * Wybierasz emotkę w kole patrząc na gracza Gruz obok → serwer wysyła mu zaproszenie → on wciska klawisz
 * (domyślnie Y) → serwer startuje animację u obu i u innych graczy Gruz na tym serwerze. Obaj obracają się do siebie.
 */
public final class Duo {
    private Duo() {}

    private static final double REACH = 4.0;
    private static UUID inviteFrom;
    private static String inviteName = "";
    private static Emote inviteEmote;
    private static long inviteUntil;
    private static boolean wasKey;

    /** Gracz, na którego patrzysz (albo najbliższy w zasięgu 4 kratek) */
    private static Player target() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        Entity look = mc.crosshairPickEntity;
        if (look instanceof Player p && p != mc.player && p.distanceTo(mc.player) <= REACH) return p;
        Player best = null;
        double bestD = REACH;
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            double dist = p.distanceTo(mc.player);
            if (dist <= bestD) { bestD = dist; best = p; }
        }
        return best;
    }

    private static void say(String key, String name, ChatFormatting color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String text = Lang.t(key).replace("{name}", name).replace("{emote}", inviteEmote != null ? Lang.t("emote." + inviteEmote.id) : "");
        Compat.chat(mc.player, Component.literal("◆ Gruz ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(text).withStyle(color)), false);
    }

    /** Wybór emotki we dwoje z koła */
    public static void invite(Emote e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!GruzNetwork.connected()) { say("duo.offline", "", ChatFormatting.RED); return; }
        Player p = target();
        if (p == null) { say("duo.noTarget", "", ChatFormatting.RED); return; }
        if (!Cosmetics.isGruzUser(p.getUUID())) { say("duo.notGruz", p.getName().getString(), ChatFormatting.RED); return; }
        // posiadanie sprawdza serwer (kod mógł być wpisany już w trakcie gry)
        GruzNetwork.sendDuoInvite(p.getUUID(), e.id);
        say("duo.sent", p.getName().getString(), ChatFormatting.GRAY);
    }

    /** Zaproszenie od gracza obok */
    public static void onInvite(UUID from, String name, String emote) {
        Emote e = Emote.byId(emote);
        if (e == null || !e.duo) return;
        inviteFrom = from;
        inviteName = name;
        inviteEmote = e;
        inviteUntil = System.currentTimeMillis() + 18_000;
        String key = Modules.COSMETICS.duoKey.display();
        say("duo.invited", name, ChatFormatting.YELLOW);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) Compat.chat(mc.player, Component.literal(Lang.t("duo.press").replace("{key}", key)).withStyle(ChatFormatting.GOLD), true);
    }

    /** Co tick: klawisz przyjęcia zaproszenia */
    public static void tick(CosmeticsModule module) {
        boolean down = Modules.keyDown(module.duoKey.code);
        if (down && !wasKey && inviteFrom != null) {
            if (System.currentTimeMillis() <= inviteUntil) GruzNetwork.sendDuoAccept(inviteFrom);
            inviteFrom = null;
        }
        wasKey = down;
        if (inviteFrom != null && System.currentTimeMillis() > inviteUntil) inviteFrom = null;
    }

    /** Start u obu: a = zapraszający, b = przyjmujący */
    public static void onStart(UUID a, UUID b, String emote) {
        Emote e = Emote.byId(emote);
        if (e == null) return;
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        for (UUID who : new UUID[]{a, b}) {
            Emote.Playing play = new Emote.Playing(e, now);
            Cosmetics.EMOTES.put(who, play);
            String anim = e == Emote.DUODANCE ? (who.equals(a) ? "duodance_a" : "duodance_b") : e.id;
            EmoteAnim.start(who, play, anim);
        }
        if (mc.player == null || mc.level == null) return;
        UUID me = mc.player.getUUID();
        if (!me.equals(a) && !me.equals(b)) return;
        // obracamy się przodem do partnera
        Player other = mc.level.getPlayerByUUID(me.equals(a) ? b : a);
        if (other != null) {
            double dx = other.getX() - mc.player.getX(), dz = other.getZ() - mc.player.getZ();
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            mc.player.setYRot(yaw);
            mc.player.setYHeadRot(yaw);
            mc.player.yBodyRot = yaw;
        }
        Modules.COSMETICS.emoteStarted();
    }
}
