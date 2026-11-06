//#only MC == 1.21.1 || MC >= 1.21.11
package pl.gruzclient.cosmetics;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Jedyne miejsce, które dotyka klas Player Animation Library. Ładowane TYLKO gdy biblioteka jest
 * zainstalowana (EmoteAnim sprawdza to wcześniej), więc bez niej gra się nie wywali.
 * Wersje: 1.21.1 (PAL 1.1.x) oraz 1.21.11 / 26.x (PAL 1.1.12 / 1.2.x) – to samo API.
 */
final class EmoteAnimPal {
    private EmoteAnimPal() {}

    private static final ResourceLocation LAYER = id("emotes");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gruzclient", path); }

    /** Warstwa animacji Gruz na każdym graczu; bez własnej animacji w spoczynku (STOP), gra tylko to, co uruchomimy */
    static void init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000,
                player -> new PlayerAnimationController(player, (controller, data, setter) -> PlayState.STOP));
    }

    private static PlayerAnimationController controller(AbstractClientPlayer p) {
        IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(p, LAYER);
        return layer instanceof PlayerAnimationController c ? c : null;
    }

    /** true = biblioteka ma tę animację i ją uruchomiła */
    static boolean play(AbstractClientPlayer p, String emote) {
        PlayerAnimationController c = controller(p);
        return c != null && c.triggerAnimation(id(emote));
    }

    static void stop(AbstractClientPlayer p) {
        PlayerAnimationController c = controller(p);
        if (c != null) c.stop();
    }
}
