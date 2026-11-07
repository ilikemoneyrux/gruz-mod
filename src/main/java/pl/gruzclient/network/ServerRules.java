package pl.gruzclient.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import pl.gruzclient.compat.Compat;
import pl.gruzclient.config.GruzConfig;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

/**
 * Tryb zgodny z serwerami (0.5.4): serwer Gruz wysyła listę „adres serwera MC → zablokowane moduły”
 * (ustawianą w panelu admina). Na pasującym serwerze te moduły są wyłączone – ustawienie gracza zostaje
 * zapisane i wraca po wyjściu z serwera. Lista jest zapamiętana w configu, więc działa też bez połączenia.
 */
public final class ServerRules {
    private ServerRules() {}

    /** końcówka adresu (np. hypixel.net) -> id modułów */
    private static final Map<String, Set<String>> RULES = new LinkedHashMap<>();
    private static String host = null;
    private static boolean dirty = true;
    private static Set<String> active = Set.of();
    private static Component pending;
    private static int ticks;

    /** Wczytanie zapamiętanej listy (przy starcie gry) */
    public static void load() {
        RULES.clear();
        Map<String, List<String>> saved = GruzConfig.get().serverRules;
        if (saved != null) for (Map.Entry<String, List<String>> e : saved.entrySet()) put(e.getKey(), e.getValue());
        dirty = true;
    }

    /** Nowa lista z serwera Gruz: [{match, block:[…]}] */
    public static void update(JsonArray arr) {
        RULES.clear();
        if (arr != null) for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (!o.has("match") || !o.has("block") || !o.get("block").isJsonArray()) continue;
            List<String> ids = new ArrayList<>();
            for (JsonElement b : o.getAsJsonArray("block")) ids.add(b.getAsString());
            put(o.get("match").getAsString(), ids);
        }
        Map<String, List<String>> save = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : RULES.entrySet()) save.put(e.getKey(), new ArrayList<>(e.getValue()));
        GruzConfig.get().serverRules = save;
        GruzConfig.save();
        dirty = true;
    }

    private static void put(String match, List<String> ids) {
        String k = norm(match);
        if (k.isEmpty() || ids == null || ids.isEmpty()) return;
        Set<String> set = RULES.computeIfAbsent(k, x -> new LinkedHashSet<>());
        for (String id : ids) if (id != null) set.add(id.toLowerCase(Locale.ROOT));
    }

    /** „Play.Hypixel.net:25565” -> „play.hypixel.net” */
    static String norm(String s) {
        if (s == null) return "";
        String h = s.trim().toLowerCase(Locale.ROOT).replaceFirst("^[a-z]+://", "");
        int colon = h.indexOf(':');
        if (colon >= 0 && h.indexOf(':', colon + 1) < 0) h = h.substring(0, colon); // port (nie IPv6)
        int slash = h.indexOf('/');
        if (slash >= 0) h = h.substring(0, slash);
        while (h.endsWith(".")) h = h.substring(0, h.length() - 1);
        return h;
    }

    static Set<String> match(String h) {
        if (h == null || h.isEmpty()) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> e : RULES.entrySet()) {
            String k = e.getKey();
            if (h.equals(k) || h.endsWith("." + k)) out.addAll(e.getValue());
        }
        return out;
    }

    private static String currentHost() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isLocalServer()) return null;
        ServerData s = mc.getCurrentServer();
        return s == null ? null : norm(s.ip);
    }

    /** Co tick: po wejściu na inny serwer albo nowej liście – blokujemy/odblokowujemy moduły */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (pending != null && mc.player != null) { Compat.chat(mc.player, pending, false); pending = null; }
        if (++ticks % 10 != 0 && !dirty) return;
        String h = currentHost();
        boolean changed = !java.util.Objects.equals(h, host);
        if (!changed && !dirty) return;
        host = h;
        dirty = false;
        Set<String> next = match(h);
        boolean newBlocks = !next.isEmpty() && (changed || !next.equals(active));
        active = next;
        List<String> names = new ArrayList<>();
        for (Module m : Modules.ALL) {
            boolean b = next.contains(m.id.toLowerCase(Locale.ROOT));
            if (b && m.wantsEnabled()) names.add(m.name());
            m.setBlocked(b);
        }
        if (newBlocks && !names.isEmpty()) {
            pending = GruzNetwork.prefix().append(Component.literal(Lang.t("rules.blocked")
                    .replace("{server}", h).replace("{list}", String.join(", ", names))).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Czy moduł jest zablokowany na obecnym serwerze */
    public static boolean blocked(String id) { return active.contains(id.toLowerCase(Locale.ROOT)); }
}
