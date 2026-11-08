package pl.gruzclient.legacy;

import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Start moda Gruz Client na 1.8.9 */
public class GruzLegacy implements ClientModInitializer {
    public static final Logger LOG = LogManager.getLogger("Gruz Client");

    @Override
    public void onInitializeClient() {
        Mods.load();
        LOG.info("Gruz Client 1.8.9 loaded");
    }
}
