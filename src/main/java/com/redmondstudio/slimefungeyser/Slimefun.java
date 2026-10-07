package com.redmondstudio.slimefungeyser;

import com.redmondstudio.slimefungeyser.model.SlimefunItemDefinition;
import com.redmondstudio.slimefungeyser.registry.CustomItemRegistrar;
import com.redmondstudio.slimefungeyser.registry.SlimefunItemRegistry;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.util.List;

public final class Slimefun implements Extension {
    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        logger().info("Slimefun Geyser Extension starting (v2 custom item API)");
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        List<SlimefunItemDefinition> definitions = SlimefunItemRegistry.load();
        SlimefunItemRegistry.ValidationReport validation = SlimefunItemRegistry.validate(definitions);

        logger().info("Geyser API: " + geyserApi().geyserApiVersion());
        logger().info("Preparing " + validation.definitionCount() + " custom Slimefun items...");
        logger().info("Duplicate definitions: " + validation.duplicateCount());

        if (!validation.valid()) {
            validation.errors().forEach(error -> logger().error("Registry validation: " + error));
            throw new IllegalStateException("Slimefun item registry validation failed with "
                    + validation.errors().size() + " error(s)");
        }

        CustomItemRegistrar.RegistrationResult result = new CustomItemRegistrar(logger())
                .registerAll(event, definitions);
        logger().info("Registered " + result.available() + "/" + result.total() + " custom Slimefun items."
                + (result.reused() == 0 ? "" : " (" + result.reused() + " supplied by existing mappings)"));
        logger().info("Failed definitions: " + result.failed());
    }
}
