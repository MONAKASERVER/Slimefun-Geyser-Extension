package com.redmondstudio.slimefungeyser.registry;

import com.redmondstudio.slimefungeyser.model.SlimefunItemDefinition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class SlimefunItemRegistry {
    static final String RESOURCE = "/slimefun-items.tsv";
    private static final Pattern ITEM_NAME = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern JAVA_ITEM = Pattern.compile("minecraft:[a-z0-9_./-]+");
    private static final Pattern BEDROCK_IDENTIFIER = Pattern.compile("(?:slimefun|geyser_custom):[a-z0-9_.-]+");

    private SlimefunItemRegistry() {
    }

    public static List<SlimefunItemDefinition> load() {
        InputStream stream = SlimefunItemRegistry.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing bundled item registry: " + RESOURCE);
        }

        List<SlimefunItemDefinition> definitions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length != 6) {
                    throw new IllegalStateException("Invalid registry row at line " + lineNumber + ": expected 6 columns");
                }
                try {
                    definitions.add(new SlimefunItemDefinition(
                            columns[0], columns[1], Integer.parseInt(columns[2]),
                            Integer.parseInt(columns[3]), columns[4], columns[5]
                    ));
                } catch (NumberFormatException exception) {
                    throw new IllegalStateException("Invalid number in registry at line " + lineNumber, exception);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read bundled item registry", exception);
        }
        return List.copyOf(definitions);
    }

    public static ValidationReport validate(List<SlimefunItemDefinition> definitions) {
        Set<String> errors = new LinkedHashSet<>();
        Map<String, SlimefunItemDefinition> names = new HashMap<>();
        Map<String, SlimefunItemDefinition> bedrockIdentifiers = new HashMap<>();
        Map<String, SlimefunItemDefinition> javaMappings = new HashMap<>();

        for (SlimefunItemDefinition definition : definitions) {
            if (definition.name() == null || !ITEM_NAME.matcher(definition.name()).matches()) {
                errors.add("Invalid item name: " + definition.name());
            }
            if (definition.javaItem() == null || !JAVA_ITEM.matcher(definition.javaItem()).matches()) {
                errors.add("Invalid Java item for " + definition.name() + ": " + definition.javaItem());
            }
            if (definition.customModelData() <= 0) {
                errors.add("Invalid CustomModelData for " + definition.name() + ": " + definition.customModelData());
            }
            if (definition.icon() == null || !ITEM_NAME.matcher(definition.icon()).matches()) {
                errors.add("Invalid texture icon for " + definition.name() + ": " + definition.icon());
            }
            if (definition.bedrockIdentifier() == null
                    || !BEDROCK_IDENTIFIER.matcher(definition.bedrockIdentifier()).matches()) {
                errors.add("Invalid Bedrock identifier for " + definition.name() + ": "
                        + definition.bedrockIdentifier());
            }
            if (definition.legacyTextureSize() != 16 && definition.legacyTextureSize() != 32) {
                errors.add("Unexpected legacy texture size for " + definition.name() + ": " + definition.legacyTextureSize());
            }

            addUnique(names, definition.name(), definition, "item name", errors);
            addUnique(bedrockIdentifiers, definition.bedrockIdentifier(), definition, "Bedrock identifier", errors);
            addUnique(javaMappings, definition.javaMappingKey(), definition, "Java item + CustomModelData", errors);
        }

        return new ValidationReport(definitions.size(), List.copyOf(errors));
    }

    private static void addUnique(Map<String, SlimefunItemDefinition> values, String key,
                                  SlimefunItemDefinition definition, String kind, Set<String> errors) {
        SlimefunItemDefinition previous = values.putIfAbsent(key, definition);
        if (previous != null) {
            errors.add("Duplicate " + kind + ": " + key + " (" + previous.name() + ", " + definition.name() + ")");
        }
    }

    public record ValidationReport(int definitionCount, List<String> errors) {
        public boolean valid() {
            return errors.isEmpty();
        }

        public int duplicateCount() {
            return (int) errors.stream().filter(error -> error.startsWith("Duplicate ")).count();
        }
    }
}
