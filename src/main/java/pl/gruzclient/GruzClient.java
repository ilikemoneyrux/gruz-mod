package pl.gruzclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gruzclient.config.GruzConfig;
import pl.gruzclient.module.Modules;

public class GruzClient implements ClientModInitializer {
    public static final String MOD_ID = "gruzclient";
    public static final Logger LOG = LoggerFactory.getLogger("Gruz Client");

    public static String version() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    @Override
    public void onInitializeClient() {
        GruzConfig.load();
        Modules.load();
        pl.gruzclient.cosmetics.EmoteAnim.init(); // emotki z zginaniem (Player Animation Library, jeśli jest)
        LOG.info("Gruz Client {} wczytany, moduły: {}", version(), Modules.ALL.size());
    }
}
