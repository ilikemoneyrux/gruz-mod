package pl.gruzclient.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import pl.gruzclient.GruzClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Punkty na mapie (waypointy), osobno dla każdego świata/serwera i wymiaru. */
public final class Waypoints {
    private Waypoints() {}

    public static final class Point {
        public String name;
        public int x, y, z;
        public int color;
        public boolean death;
        public long created = System.currentTimeMillis();

        public Point(String name, int x, int y, int z, int color, boolean death) {
            this.name = name; this.x = x; this.y = y; this.z = z; this.color = color; this.death = death;
        }
    }

    public static final int[] COLORS = {0xB45CFF, 0x5CE1FF, 0x7DFFB2, 0xFFD35C, 0xFF5C8A, 0xFFFFFF};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, List<Point>> all;

    private static Path file() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/gruzclient/waypoints.json");
    }

    private static Map<String, List<Point>> data() {
        if (all != null) return all;
        try {
            if (Files.exists(file())) all = GSON.fromJson(Files.readString(file(), StandardCharsets.UTF_8), new TypeToken<Map<String, List<Point>>>() {}.getType());
        } catch (Exception e) {
            GruzClient.LOG.warn("Waypointy: błąd odczytu", e);
        }
        if (all == null) all = new HashMap<>();
        return all;
    }

    public static List<Point> current() {
        String k = MapCache.worldKey();
        if (k.isEmpty()) return new ArrayList<>();
        return data().computeIfAbsent(k, x -> new ArrayList<>());
    }

    public static Point add(String name, int x, int y, int z, boolean death) {
        List<Point> list = current();
        if (death) list.removeIf(p -> p.death); // tylko ostatnie miejsce śmierci
        Point p = new Point(name, x, y, z, death ? 0xFF5C8A : COLORS[list.size() % COLORS.length], death);
        list.add(p);
        save();
        return p;
    }

    public static void remove(Point p) {
        current().remove(p);
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(data()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            GruzClient.LOG.warn("Waypointy: błąd zapisu", e);
        }
    }
}
