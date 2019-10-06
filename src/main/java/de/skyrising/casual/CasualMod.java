package de.skyrising.casual;

import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import net.fabricmc.api.ModInitializer;

public class CasualMod implements ModInitializer {
    @Override
    public void onInitialize() {
        InitializationHandler.getInstance().registerInitializationHandler(this::malilibInit);
    }

    private void malilibInit() {
        RenderEventHandler.getInstance().registerWorldLastRenderer(new MarkerRenderer());
    }
}
