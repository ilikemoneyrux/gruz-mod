package pl.gruzclient.render;

import pl.gruzclient.compat.Compat;
//#if MC < 26.3
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
//#endif
import org.lwjgl.system.MemoryUtil;
import pl.gruzclient.GruzClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

/** Logo Gruz jako ikona okna gry (pasek zadań i Alt+Tab) zamiast ikony Minecrafta. */
public final class WindowIcon {
    private WindowIcon() {}

    private static boolean done = false;

    public static void apply() {
        if (done) return;
        done = true;
        if (Util.getPlatform() == Util.OS.OSX) return; // macOS bierze ikonę z aplikacji
        long handle = Compat.windowHandle();
        List<ByteBuffer> buffers = new ArrayList<>();
        try (InputStream in = WindowIcon.class.getResourceAsStream("/assets/gruzclient/textures/gui/logo.png")) {
            if (in == null) return;
            NativeImage src = NativeImage.read(in);
            int[] sizes = {16, 32, 48, 64, 128};
            //#if MC >= 26.3
            // 26.3: okno na SDL3 – jedna powierzchnia 128 px + mniejsze jako obrazy zastępcze
            org.lwjgl.sdl.SDL_Surface main = null;
            List<org.lwjgl.sdl.SDL_Surface> alts = new ArrayList<>();
            //#else
            GLFWImage.Buffer icons = GLFWImage.malloc(sizes.length);
            //#endif
            for (int i = 0; i < sizes.length; i++) {
                int s = sizes[i];
                NativeImage scaled = new NativeImage(s, s, true);
                src.resizeSubRectTo(0, 0, src.getWidth(), src.getHeight(), scaled);
                ByteBuffer buf = MemoryUtil.memAlloc(s * s * 4);
                buffers.add(buf);
                for (int y = 0; y < s; y++) {
                    for (int x = 0; x < s; x++) {
                        int abgr = Compat.getPixelABGR(scaled, x, y);
                        buf.put((byte) (abgr & 255)).put((byte) ((abgr >> 8) & 255)).put((byte) ((abgr >> 16) & 255)).put((byte) ((abgr >>> 24) & 255));
                    }
                }
                buf.flip();
                scaled.close();
                //#if MC >= 26.3
                org.lwjgl.sdl.SDL_Surface surf = org.lwjgl.sdl.SDLSurface.SDL_CreateSurfaceFrom(s, s, org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ABGR8888, buf, s * 4);
                if (surf != null) alts.add(surf);
                //#else
                icons.position(i).width(s).height(s).pixels(buf);
                //#endif
            }
            //#if MC >= 26.3
            if (!alts.isEmpty()) {
                main = alts.remove(alts.size() - 1);
                for (org.lwjgl.sdl.SDL_Surface a : alts) org.lwjgl.sdl.SDLSurface.SDL_AddSurfaceAlternateImage(main, a);
                org.lwjgl.sdl.SDLVideo.SDL_SetWindowIcon(handle, main);
                for (org.lwjgl.sdl.SDL_Surface a : alts) org.lwjgl.sdl.SDLSurface.SDL_DestroySurface(a);
                org.lwjgl.sdl.SDLSurface.SDL_DestroySurface(main);
            }
            //#else
            icons.position(0);
            GLFW.glfwSetWindowIcon(handle, icons);
            icons.free();
            //#endif
            src.close();
        } catch (Throwable t) {
            GruzClient.LOG.warn("Nie udało się ustawić ikony okna", t);
        } finally {
            for (ByteBuffer b : buffers) MemoryUtil.memFree(b);
        }
    }
}
