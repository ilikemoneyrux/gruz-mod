package pl.gruzclient.replay;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Zapis klipu jako AVI z klatkami JPEG (MJPEG). Ten format otwiera VLC i odtwarzacz Windows,
 * a launcher przerabia go na MP4 (16:9 albo 9:16). Bez zależności od Minecrafta.
 */
public final class Avi {
    private Avi() {}

    public static void write(Path file, List<byte[]> frames, int width, int height, int fps) throws IOException {
        int n = frames.size(), max = 0;
        long movi = 4;
        for (byte[] f : frames) { movi += 8 + f.length + (f.length & 1); max = Math.max(max, f.length); }
        long riff = 4 + 200 + (8 + movi) + (8 + 16L * n);
        if (riff > 0xFFFFFFFFL) throw new IOException("klip za duży na AVI");
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".part");
        try (OutputStream o = new BufferedOutputStream(Files.newOutputStream(tmp), 1 << 16)) {
            fourcc(o, "RIFF"); u32(o, riff); fourcc(o, "AVI ");
            fourcc(o, "LIST"); u32(o, 192); fourcc(o, "hdrl");
            fourcc(o, "avih"); u32(o, 56);
            u32(o, 1_000_000 / fps); u32(o, (long) max * fps); u32(o, 0); u32(o, 0x10); // 0x10 = AVIF_HASINDEX
            u32(o, n); u32(o, 0); u32(o, 1); u32(o, max); u32(o, width); u32(o, height);
            u32(o, 0); u32(o, 0); u32(o, 0); u32(o, 0);
            fourcc(o, "LIST"); u32(o, 116); fourcc(o, "strl");
            fourcc(o, "strh"); u32(o, 56);
            fourcc(o, "vids"); fourcc(o, "MJPG"); u32(o, 0); u16(o, 0); u16(o, 0); u32(o, 0);
            u32(o, 1); u32(o, fps); u32(o, 0); u32(o, n); u32(o, max); u32(o, 0xFFFFFFFFL); u32(o, 0);
            u16(o, 0); u16(o, 0); u16(o, width); u16(o, height);
            fourcc(o, "strf"); u32(o, 40);
            u32(o, 40); u32(o, width); u32(o, height); u16(o, 1); u16(o, 24); fourcc(o, "MJPG");
            u32(o, (long) width * height * 3); u32(o, 0); u32(o, 0); u32(o, 0); u32(o, 0);
            fourcc(o, "LIST"); u32(o, movi); fourcc(o, "movi");
            for (byte[] f : frames) {
                fourcc(o, "00dc"); u32(o, f.length); o.write(f);
                if ((f.length & 1) == 1) o.write(0); // fragmenty RIFF mają parzystą długość
            }
            fourcc(o, "idx1"); u32(o, 16L * n);
            long off = 4; // od początku "movi"
            for (byte[] f : frames) {
                fourcc(o, "00dc"); u32(o, 0x10); u32(o, off); u32(o, f.length); // 0x10 = klatka kluczowa
                off += 8 + f.length + (f.length & 1);
            }
        }
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void fourcc(OutputStream o, String s) throws IOException { o.write(s.getBytes(StandardCharsets.US_ASCII)); }
    private static void u16(OutputStream o, int v) throws IOException { o.write(v & 255); o.write((v >> 8) & 255); }
    private static void u32(OutputStream o, long v) throws IOException {
        o.write((int) (v & 255)); o.write((int) ((v >> 8) & 255)); o.write((int) ((v >> 16) & 255)); o.write((int) ((v >> 24) & 255));
    }
}
