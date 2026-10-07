package com.redmondstudio.slimefungeyser.model;

public record SlimefunItemDefinition(
        String name,
        String javaItem,
        int customModelData,
        int legacyTextureSize,
        String bedrockIdentifier,
        String icon
) {
    public String javaMappingKey() {
        return javaItem + "|" + customModelData;
    }

    public String canonicalLine() {
        return String.join("\t", name, javaItem, Integer.toString(customModelData),
                Integer.toString(legacyTextureSize), bedrockIdentifier, icon);
    }
}
