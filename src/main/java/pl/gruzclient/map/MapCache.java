package pl.gruzclient.map;

import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import pl.gruzclient.GruzClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Pamięć odkrytego terenu: kolor każdego bloku z góry (jak mapa w grze), zapisywany per świat/serwer
 * i wymiar w config/gruzclient/maps. Z tego rysuje się minimapa i pełna mapa świata (M).
 */
public final class MapCache {
    private MapCache() {}

    /** chunk (long) -> 256 kolorów ARGB (0 = nieznany) */
    private static final Map<Long, int[]> CHUNKS = new HashMap<>();
    private static String key = "";
    private static boolean dirty = false;
    private static int scanIndex = 0;

    public static String worldKey() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return "";
        String base = mc.isInSingleplayer() && mc.getServer() != null
                ? "sp_" + mc.getServer().getSaveProperties().getLevelName()
                : "mp_" + (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "unknown");
        String dim = mc.world.getRegistryKey().getValue().toString();
        return (base + "_" + dim).replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static Path file(String k) {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/gruzclient/maps/" + k + ".bin");
    }

    /** Przełączenie świata/wymiaru: zapisz stary, wczytaj nowy */
    public static void ensureWorld() {
        String k = worldKey();
        if (k.equals(key)) return;
        save();
        CHUNKS.clear();
        key = k;
        if (k.isEmpty()) return;
        Path f = file(k);
        if (!Files.exists(f)) return;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(f)))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                long pos = in.readLong();
                int[] c = new int[256];
                for (int j = 0; j < 256; j++) c[j] = in.readInt();
                CHUNKS.put(pos, c);
            }
        } catch (Exception e) {
            GruzClient.LOG.warn("Mapa: nie udało się wczytać {}", f, e);
        }
    }

    public static void save() {
        if (!dirty || key.isEmpty()) return;
        dirty = false;
        Path f = file(key);
        try {
            Files.createDirectories(f.getParent());
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(f)))) {
                out.writeInt(CHUNKS.size());
                for (Map.Entry<Long, int[]> e : CHUNKS.entrySet()) {
                    out.writeLong(e.getKey());
                    for (int v : e.getValue()) out.writeInt(v);
                }
            }
        } catch (Exception e) {
            GruzClient.LOG.warn("Mapa: nie udało się zapisać", e);
        }
    }

    /** Skanuje kilka chunków wokół gracza na tick (spiralnie od środka), żeby nie zacinać gry */
    public static void tick(int radiusChunks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) return;
        ensureWorld();
        int pcx = mc.player.getChunkPos().x, pcz = mc.player.getChunkPos().z;
        int side = radiusChunks * 2 + 1, total = side * side;
        for (int n = 0; n < 6; n++) {
            scanIndex = (scanIndex + 1) % total;
            // kolejność od środka: najpierw bliskie chunki częściej
            int i = n < 2 ? (scanIndex % 9) : scanIndex;
            int s = n < 2 ? 3 : side;
            int cx = pcx + (i % s) - s / 2, cz = pcz + (i / s) - s / 2;
            if (!w.getChunkManager().isChunkLoaded(cx, cz)) continue;
            scanChunk(w, cx, cz);
        }
    }

    private static void scanChunk(ClientWorld w, int cx, int cz) {
        int[] colors = CHUNKS.computeIfAbsent(ChunkPos.toLong(cx, cz), p -> new int[256]);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        boolean nether = w.getDimension().hasCeiling();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int bx = (cx << 4) + x, bz = (cz << 4) + z;
                int y = nether ? Math.min(100, w.getTopY(Heightmap.Type.WORLD_SURFACE, bx, bz)) : w.getTopY(Heightmap.Type.WORLD_SURFACE, bx, bz);
                pos.set(bx, y - 1, bz);
                BlockState st = w.getBlockState(pos);
                int guard = 0;
                while (st.getMapColor(w, pos) == MapColor.CLEAR && pos.getY() > w.getBottomY() && guard++ < 64) {
                    pos.move(0, -1, 0);
                    st = w.getBlockState(pos);
                }
                MapColor mc = st.getMapColor(w, pos);
                if (mc == MapColor.CLEAR) { colors[z * 16 + x] = 0xFF000000; continue; }
                // cieniowanie jak na mapie: wyżej = jaśniej, niżej = ciemniej
                int north = (z == 0) ? pos.getY() : HEIGHTS[(z - 1) * 16 + x];
                MapColor.Brightness b = pos.getY() > north ? MapColor.Brightness.HIGH : pos.getY() < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                if (st.getFluidState().isStill() && !st.getFluidState().isEmpty()) b = MapColor.Brightness.NORMAL;
                int abgr = mc.getRenderColor(b);
                int r = abgr & 255, g = (abgr >> 8) & 255, bl = (abgr >> 16) & 255;
                colors[z * 16 + x] = 0xFF000000 | (r << 16) | (g << 8) | bl;
                HEIGHTS[z * 16 + x] = pos.getY();
            }
        }
        dirty = true;
    }

    /** wysokości aktualnie skanowanego chunka (do cieniowania względem bloku od północy) */
    private static final int[] HEIGHTS = new int[256];

    /** Kolor bloku na mapie (ARGB) albo 0, gdy nieodkryty */
    public static int colorAt(int bx, int bz) {
        int[] c = CHUNKS.get(ChunkPos.toLong(bx >> 4, bz >> 4));
        return c == null ? 0 : c[(bz & 15) * 16 + (bx & 15)];
    }

    public static boolean hasChunk(int cx, int cz) { return CHUNKS.containsKey(ChunkPos.toLong(cx, cz)); }
}
