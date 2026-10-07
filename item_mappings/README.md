# Legacy mapping files removed

The old format-v1 JSON mappings duplicated every item registered by the extension. Installing both the JSON files and the extension made Geyser register the same Bedrock identifiers twice.

Geyser 2.11.3 installations must use the extension JAR only. Remove old Slimefun mapping JSON files from `plugins/Geyser-BungeeCord/custom_mappings/` before starting Geyser.

All 561 authoritative item definitions are retained in `src/main/resources/slimefun-items.tsv` and registered through Geyser's v2 custom item API.
