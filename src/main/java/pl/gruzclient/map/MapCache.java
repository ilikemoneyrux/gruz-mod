package pl.gruzclient.map;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.GruzClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "";
        String base = mc.isLocalServer() && mc.getSingleplayerServer() != null
                ? "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName()
                : "mp_" + (mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "unknown");
        String dim = mc.level.dimension().location().toString();
        return (base + "_" + dim).replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static Path file(String k) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/gruzclient/maps/" + k + ".bin");
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
        Minecraft mc = Minecraft.getInstance();
        ClientLevel w = mc.level;
        if (w == null || mc.player == null) return;
        ensureWorld();
        int pcx = mc.player.chunkPosition().x, pcz = mc.player.chunkPosition().z;
        int side = radiusChunks * 2 + 1, total = side * side;
        for (int n = 0; n < 6; n++) {
            scanIndex = (scanIndex + 1) % total;
            // kolejność od środka: najpierw bliskie chunki częściej
            int i = n < 2 ? (scanIndex % 9) : scanIndex;
            int s = n < 2 ? 3 : side;
            int cx = pcx + (i % s) - s / 2, cz = pcz + (i / s) - s / 2;
            if (!w.getChunkSource().hasChunk(cx, cz)) continue;
            scanChunk(w, cx, cz);
        }
    }

    private static void scanChunk(ClientLevel w, int cx, int cz) {
        int[] colors = CHUNKS.computeIfAbsent(ChunkPos.asLong(cx, cz), p -> new int[256]);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean nether = w.dimensionType().hasCeiling();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int bx = (cx << 4) + x, bz = (cz << 4) + z;
                int y = nether ? Math.min(100, w.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz)) : w.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
                pos.set(bx, y - 1, bz);
                BlockState st = w.getBlockState(pos);
                int guard = 0;
                while (st.getMapColor(w, pos) == MapColor.NONE && pos.getY() > Compat.minY(w) && guard++ < 64) {
                    pos.move(0, -1, 0);
                    st = w.getBlockState(pos);
                }
                MapColor mc = st.getMapColor(w, pos);
                if (mc == MapColor.NONE) { colors[z * 16 + x] = 0xFF000000; continue; }
                // cieniowanie jak na mapie: wyżej = jaśniej, niżej = ciemniej
                int north = (z == 0) ? pos.getY() : HEIGHTS[(z - 1) * 16 + x];
                MapColor.Brightness b = pos.getY() > north ? MapColor.Brightness.HIGH : pos.getY() < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                if (st.getFluidState().isSource() && !st.getFluidState().isEmpty()) b = MapColor.Brightness.NORMAL;
                int abgr = Compat.mapColorABGR(mc, b);
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
        int[] c = CHUNKS.get(ChunkPos.asLong(bx >> 4, bz >> 4));
        return c == null ? 0 : c[(bz & 15) * 16 + (bx & 15)];
    }

    public static boolean hasChunk(int cx, int cz) { return CHUNKS.containsKey(ChunkPos.asLong(cx, cz)); }
}
