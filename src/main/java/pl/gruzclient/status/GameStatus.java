package pl.gruzclient.status;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Zapisuje do pliku gruz-status.json (w folderze gry), co aktualnie robi gracz:
 * menu / świat jednoosobowy / serwer + adres. Launcher czyta ten plik i ustawia status na Discordzie.
 */
public final class GameStatus {
    private GameStatus() {}

    private static int ticks = 0;
    private static String lastJson = "";

    public static void tick() {
        if (++ticks % 100 != 0) return; // co 5 sekund
        MinecraftClient mc = MinecraftClient.getInstance();
        JsonObject o = new JsonObject();
        if (mc.world == null) {
            o.addProperty("state", "menu");
        } else if (mc.isInSingleplayer()) {
            o.addProperty("state", "singleplayer");
        } else {
            o.addProperty("state", "multiplayer");
            ServerInfo s = mc.getCurrentServerEntry();
            if (s != null) {
                pl.gruzclient.config.GruzConfig cfg = pl.gruzclient.config.GruzConfig.get();
                if (!s.address.equals(cfg.lastServer)) {
                    cfg.lastServer = s.address;
                    cfg.lastServerName = s.name;
                    pl.gruzclient.config.GruzConfig.save();
                }
                o.addProperty("server", s.address);
                o.addProperty("serverName", s.name);
            }
        }
        o.addProperty("fps", mc.getCurrentFps());
        o.addProperty("kills", pl.gruzclient.combat.Combat.kills);
        String json = o.toString();
        if (json.equals(lastJson)) return;
        lastJson = json;
        try {
            Path file = mc.runDirectory.toPath().resolve("gruz-status.json");
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // brak zapisu nie może przeszkadzać w grze
        }
    }
}
