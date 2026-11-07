package pl.gruzclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import pl.gruzclient.GruzClient;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ustawienia moda w pliku config/gruzclient.json */
public class GruzConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("gruzclient.json");
    private static GruzConfig instance;

    public String language = "auto";
    /** Ostatni serwer (przycisk "Wróć na serwer" w menu głównym) */
    public String lastServer = "";
    public String lastServerName = "";
    /** Pozycje elementów gry przesuwanych w edytorze HUD (tablica wyników, pasek bossa…): id -> {x, y, scale} */
    public Map<String, JsonObject> elements = new LinkedHashMap<>();
    /** id modułu -> jego zapisany stan (włączony, ustawienia, pozycja HUD) */
    public Map<String, JsonObject> modules = new LinkedHashMap<>();
    /** Tryb zgodny z serwerami: adres serwera -> zablokowane moduły (kopia listy z serwera Gruz) */
    public Map<String, java.util.List<String>> serverRules = new LinkedHashMap<>();

    public static GruzConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                    instance = GSON.fromJson(r, GruzConfig.class);
                }
            }
        } catch (Exception e) {
            GruzClient.LOG.warn("Nie udało się wczytać gruzclient.json – używam domyślnych", e);
        }
        if (instance == null) instance = new GruzConfig();
        if (instance.modules == null) instance.modules = new LinkedHashMap<>();
        if (instance.language == null) instance.language = "auto";
        if (instance.elements == null) instance.elements = new LinkedHashMap<>();
        if (instance.lastServer == null) instance.lastServer = "";
        if (instance.lastServerName == null) instance.lastServerName = "";
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(get(), w);
            }
        } catch (Exception e) {
            GruzClient.LOG.warn("Nie udało się zapisać gruzclient.json", e);
        }
    }
}
