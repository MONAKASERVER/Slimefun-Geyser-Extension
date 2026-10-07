package com.redmondstudio.slimefungeyser.registry;

import com.redmondstudio.slimefungeyser.model.SlimefunItemDefinition;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinitionRegisterException;
import org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate;
import org.geysermc.geyser.api.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CustomItemRegistrar {
    private final ExtensionLogger logger;

    public CustomItemRegistrar(ExtensionLogger logger) {
        this.logger = logger;
    }

    public RegistrationResult registerAll(GeyserDefineCustomItemsEvent event,
                                          List<SlimefunItemDefinition> definitions) {
        List<PreparedDefinition> prepared = definitions.stream().map(this::prepare).toList();
        List<String> conflicts = new ArrayList<>();
        List<PreparedDefinition> alreadyPresent = new ArrayList<>();

        for (PreparedDefinition candidate : prepared) {
            ExistingMapping mapping = inspectExisting(event.customItemDefinitions(), candidate);
            if (mapping.status() == ExistingStatus.EQUIVALENT) {
                alreadyPresent.add(candidate);
                logger.warning("Equivalent custom mapping already exists; not registering it twice: "
                        + describe(candidate.source()));
            } else if (mapping.status() == ExistingStatus.CONFLICT) {
                conflicts.add(describe(candidate.source()) + "; " + mapping.reason());
            }
        }

        if (!conflicts.isEmpty()) {
            conflicts.forEach(conflict -> logger.error("Custom item conflict: " + conflict));
            throw new IllegalStateException("Refusing partial registration because " + conflicts.size()
                    + " conflicting custom item definitions already exist");
        }

        int registered = 0;
        int failed = 0;
        for (PreparedDefinition candidate : prepared) {
            if (alreadyPresent.contains(candidate)) {
                continue;
            }
            try {
                event.register(candidate.javaItem(), candidate.customItem());
                registered++;
            } catch (CustomItemDefinitionRegisterException | IllegalArgumentException exception) {
                failed++;
                logger.error("Failed custom item: " + describe(candidate.source())
                        + "; cause=" + exception.getMessage(), exception);
            }
        }

        RegistrationResult result = new RegistrationResult(definitions.size(), registered, alreadyPresent.size(), failed);
        if (failed > 0) {
            throw new IllegalStateException("Failed to register " + failed + " Slimefun custom item definitions");
        }
        return result;
    }

    private PreparedDefinition prepare(SlimefunItemDefinition source) {
        Identifier javaItem = Identifier.of(source.javaItem());
        CustomItemDefinition customItem = CustomItemDefinition.builder(
                        Identifier.of(source.bedrockIdentifier()), javaItem)
                .displayName(source.name())
                .bedrockOptions(CustomItemBedrockOptions.builder()
                        .icon(source.icon()))
                .predicate(ItemRangeDispatchPredicate.legacyCustomModelData(source.customModelData()))
                .build();
        return new PreparedDefinition(source, javaItem, customItem);
    }

    private ExistingMapping inspectExisting(Map<Identifier, Collection<CustomItemDefinition>> existing,
                                            PreparedDefinition candidate) {
        for (Map.Entry<Identifier, Collection<CustomItemDefinition>> entry : existing.entrySet()) {
            for (CustomItemDefinition registered : entry.getValue()) {
                boolean sameBedrockIdentifier = registered.bedrockIdentifier()
                        .equals(candidate.customItem().bedrockIdentifier());
                boolean sameJavaMapping = entry.getKey().equals(candidate.javaItem())
                        && registered.model().equals(candidate.customItem().model())
                        && registered.predicates().equals(candidate.customItem().predicates())
                        && registered.predicateStrategy().equals(candidate.customItem().predicateStrategy());

                if (sameJavaMapping) {
                    if (Objects.equals(registered.icon(), candidate.customItem().icon())) {
                        return new ExistingMapping(ExistingStatus.EQUIVALENT,
                                "provided by " + registered.bedrockIdentifier());
                    }
                    return new ExistingMapping(ExistingStatus.CONFLICT,
                            "same Java item and CustomModelData already use icon " + registered.icon()
                                    + " via " + registered.bedrockIdentifier());
                }
                if (sameBedrockIdentifier) {
                    return new ExistingMapping(ExistingStatus.CONFLICT,
                            "Bedrock identifier is already used for " + entry.getKey()
                                    + " / model " + registered.model());
                }
            }
        }
        return new ExistingMapping(ExistingStatus.NONE, "");
    }

    private String describe(SlimefunItemDefinition definition) {
        return "name=" + definition.name()
                + ", java=" + definition.javaItem()
                + ", CustomModelData=" + definition.customModelData()
                + ", bedrock=" + definition.bedrockIdentifier();
    }

    private record PreparedDefinition(SlimefunItemDefinition source, Identifier javaItem,
                                      CustomItemDefinition customItem) {
    }

    private record ExistingMapping(ExistingStatus status, String reason) {
    }

    private enum ExistingStatus {
        NONE,
        EQUIVALENT,
        CONFLICT
    }

    public record RegistrationResult(int total, int registered, int reused, int failed) {
        public int available() {
            return registered + reused;
        }
    }
}
