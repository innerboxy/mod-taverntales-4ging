package ibx.taverntales.forging;

import ibx.taverntales.forging.client.screen.EquipmentForgeScreen;
import ibx.taverntales.forging.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = TavernTalesEquipmentForge.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TavernTalesEquipmentForge.MODID, value = Dist.CLIENT)
public class TavernTalesEquipmentForgeClient {

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.EQUIPMENT_FORGE.get(), EquipmentForgeScreen::new);
    }
}
