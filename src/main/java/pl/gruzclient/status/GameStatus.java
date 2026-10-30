package pl.gruzclient.status;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

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
        Minecraft mc = Minecraft.getInstance();
        JsonObject o = new JsonObject();
        if (mc.level == null) {
            o.addProperty("state", "menu");
        } else if (mc.isLocalServer()) {
            o.addProperty("state", "singleplayer");
        } else {
            o.addProperty("state", "multiplayer");
            ServerData s = mc.getCurrentServer();
            if (s != null) {
                pl.gruzclient.config.GruzConfig cfg = pl.gruzclient.config.GruzConfig.get();
                if (!s.ip.equals(cfg.lastServer)) {
                    cfg.lastServer = s.ip;
                    cfg.lastServerName = s.name;
                    pl.gruzclient.config.GruzConfig.save();
                }
                o.addProperty("server", s.ip);
                o.addProperty("serverName", s.name);
            }
        }
        o.addProperty("fps", mc.getFps());
        o.addProperty("kills", pl.gruzclient.combat.Combat.kills);
        String json = o.toString();
        if (json.equals(lastJson)) return;
        lastJson = json;
        try {
            Path file = mc.gameDirectory.toPath().resolve("gruz-status.json");
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // brak zapisu nie może przeszkadzać w grze
        }
    }
}
