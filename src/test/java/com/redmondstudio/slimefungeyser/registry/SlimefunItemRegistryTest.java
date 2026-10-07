package com.redmondstudio.slimefungeyser.registry;

import com.redmondstudio.slimefungeyser.model.SlimefunItemDefinition;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlimefunItemRegistryTest {
    private static final int EXPECTED_DEFINITION_COUNT = 561;
    private static final String EXPECTED_REGISTRY_SHA256 =
            "ff55fa9d0421a8f93f5ecf4c20e5155fa82e54277704e9569aa3e3606803af37";

    @Test
    void preservesEveryLegacyDefinitionWithoutDuplicates() {
        List<SlimefunItemDefinition> definitions = SlimefunItemRegistry.load();
        SlimefunItemRegistry.ValidationReport report = SlimefunItemRegistry.validate(definitions);

        assertEquals(EXPECTED_DEFINITION_COUNT, definitions.size());
        assertTrue(report.valid(), () -> String.join(System.lineSeparator(), report.errors()));
        assertEquals(0, report.duplicateCount());
        assertEquals(EXPECTED_REGISTRY_SHA256, registrySha256(definitions));
    }

    @Test
    void keepsLegacyTextureMetadataAndKnownMappings() {
        List<SlimefunItemDefinition> definitions = SlimefunItemRegistry.load();

        assertEquals(438, definitions.stream().filter(item -> item.legacyTextureSize() == 16).count());
        assertEquals(123, definitions.stream().filter(item -> item.legacyTextureSize() == 32).count());
        assertTrue(definitions.stream().anyMatch(item -> item.name().equals("carbon")
                && item.javaItem().equals("minecraft:player_head")
                && item.customModelData() == 2200113
                && item.bedrockIdentifier().equals("slimefun:carbon")
                && item.icon().equals("carbon")));
    }

    @Test
    void usesResourcePackAliasesForMissingLegacyKeys() {
        List<SlimefunItemDefinition> definitions = SlimefunItemRegistry.load();

        assertTrue(definitions.stream().anyMatch(item -> item.name().equals("wiki")
                && item.icon().equals("slimefun_guide")));
        assertTrue(definitions.stream().anyMatch(item -> item.name().equals("ui_background_2")
                && item.icon().equals("background")));
        Set<String> attachableIdentifiers = definitions.stream()
                .filter(item -> item.bedrockIdentifier().startsWith("geyser_custom:"))
                .map(SlimefunItemDefinition::bedrockIdentifier)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "geyser_custom:cactus_boots", "geyser_custom:cactus_chestplate",
                "geyser_custom:cactus_helmet", "geyser_custom:cactus_leggings",
                "geyser_custom:damascus_steel_boots", "geyser_custom:damascus_steel_chestplate",
                "geyser_custom:damascus_steel_helmet", "geyser_custom:damascus_steel_leggings",
                "geyser_custom:gilded_iron_boots", "geyser_custom:gilded_iron_chestplate",
                "geyser_custom:gilded_iron_helmet", "geyser_custom:gilded_iron_leggings",
                "geyser_custom:hazmat_chestplate", "geyser_custom:hazmat_leggings",
                "geyser_custom:rubber_boots", "geyser_custom:scuba_helmet"
        ), attachableIdentifiers);
    }

    private String registrySha256(List<SlimefunItemDefinition> definitions) {
        String canonical = String.join("\n", definitions.stream()
                .map(SlimefunItemDefinition::canonicalLine)
                .toList());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
