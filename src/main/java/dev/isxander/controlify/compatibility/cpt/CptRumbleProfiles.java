package dev.isxander.controlify.compatibility.cpt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.isxander.controlify.utils.CUtil;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class CptRumbleProfiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Profile FALLBACK = new Profile(0.35f, 0.25f, 3, 0.16f, 0.22f, 3, 5.0f);

    private static Map<String, Profile> profiles = Map.of();
    private static Profile defaultProfile = FALLBACK;
    private static Set<String> knownAmmoIds = Set.of();
    private static FileTime lastModified;
    private static long lastFileCheckNanos;

    private CptRumbleProfiles() {}

    public static Profile forAmmo(String ammoId) {
        reloadIfChanged();
        return ammoId == null ? defaultProfile : profiles.getOrDefault(ammoId, defaultProfile);
    }

    /** Merge ammo IDs discovered in CPT's live ammo wheel into the editable config. */
    public static synchronized void observeAmmoIds(Collection<String> ammoIds) {
        reloadIfChanged(false);
        boolean hasNewId = ammoIds.stream().filter(id -> id != null && !id.isBlank())
                .anyMatch(id -> !knownAmmoIds.contains(id));
        if (hasNewId) reloadIfChanged(true);

        Set<String> updated = new TreeSet<>(knownAmmoIds);
        ammoIds.stream().filter(id -> id != null && !id.isBlank()).forEach(updated::add);
        if (updated.equals(knownAmmoIds)) return;

        knownAmmoIds = Set.copyOf(updated);
        try {
            writeConfig(configFile());
            lastModified = Files.getLastModifiedTime(configFile());
            lastFileCheckNanos = System.nanoTime();
        } catch (Exception e) {
            CUtil.LOGGER.warn("Could not update the CPT ammo ID list", e);
        }
    }

    private static synchronized void reloadIfChanged() {
        reloadIfChanged(false);
    }

    private static synchronized void reloadIfChanged(boolean force) {
        long now = System.nanoTime();
        if (!force && lastModified != null && now - lastFileCheckNanos < 500_000_000L) return;
        lastFileCheckNanos = now;

        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) writeConfig(file);

            FileTime modified = Files.getLastModifiedTime(file);
            if (modified.equals(lastModified)) return;
            lastModified = modified;

            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Profile parsedDefault = parseProfile(root.get("default"), FALLBACK);
                Map<String, Profile> parsedProfiles = new HashMap<>();
                JsonElement ammoElement = root.get("ammo");
                if (ammoElement != null && ammoElement.isJsonObject()) {
                    for (var entry : ammoElement.getAsJsonObject().entrySet()) {
                        parsedProfiles.put(entry.getKey(), parseProfile(entry.getValue(), parsedDefault));
                    }
                }

                Set<String> parsedIds = new TreeSet<>();
                JsonElement idsElement = root.get("knownAmmo");
                if (idsElement != null && idsElement.isJsonArray()) {
                    for (JsonElement id : idsElement.getAsJsonArray()) {
                        if (id.isJsonPrimitive() && id.getAsJsonPrimitive().isString()) {
                            parsedIds.add(id.getAsString());
                        }
                    }
                }

                defaultProfile = parsedDefault;
                profiles = Map.copyOf(parsedProfiles);
                knownAmmoIds = Set.copyOf(parsedIds);
            }
        } catch (Exception e) {
            CUtil.LOGGER.warn("Could not load CPT rumble profiles; using the last valid settings", e);
        }
    }

    private static Path configFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config/controlify/cpt-rumble.json");
    }

    private static Profile parseProfile(JsonElement element, Profile fallback) {
        if (element == null || !element.isJsonObject()) return fallback;
        JsonObject json = element.getAsJsonObject();
        return new Profile(
                readFloat(json, "strong", fallback.strong()),
                readFloat(json, "weak", fallback.weak()),
                readInt(json, "durationTicks", fallback.durationTicks()),
                readFloat(json, "triggerLeft", fallback.triggerLeft()),
                readFloat(json, "triggerRight", fallback.triggerRight()),
                readInt(json, "triggerDurationTicks", fallback.triggerDurationTicks()),
                readFloat(json, "damageReference", fallback.damageReference())
        ).validated();
    }

    private static float readFloat(JsonObject json, String key, float fallback) {
        try {
            return json.has(key) ? json.get(key).getAsFloat() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static void writeConfig(Path file) throws Exception {
        JsonObject root = new JsonObject();
        root.add("default", toJson(defaultProfile));
        JsonObject ammo = new JsonObject();
        profiles.forEach((id, profile) -> ammo.add(id, toJson(profile)));
        root.add("ammo", ammo);
        JsonArray ids = new JsonArray();
        new TreeSet<>(knownAmmoIds).forEach(ids::add);
        root.add("knownAmmo", ids);

        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(root, writer);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonObject toJson(Profile profile) {
        JsonObject json = new JsonObject();
        json.addProperty("strong", profile.strong());
        json.addProperty("weak", profile.weak());
        json.addProperty("durationTicks", profile.durationTicks());
        json.addProperty("triggerLeft", profile.triggerLeft());
        json.addProperty("triggerRight", profile.triggerRight());
        json.addProperty("triggerDurationTicks", profile.triggerDurationTicks());
        json.addProperty("damageReference", profile.damageReference());
        return json;
    }

    public record Profile(
            float strong,
            float weak,
            int durationTicks,
            float triggerLeft,
            float triggerRight,
            int triggerDurationTicks,
            float damageReference
    ) {
        private Profile validated() {
            return new Profile(
                    clamp(strong, 0f, 1f),
                    clamp(weak, 0f, 1f),
                    Math.max(1, Math.min(40, durationTicks)),
                    clamp(triggerLeft, 0f, 1f),
                    clamp(triggerRight, 0f, 1f),
                    Math.max(1, Math.min(40, triggerDurationTicks)),
                    clamp(damageReference, 0.1f, 100f)
            );
        }

        private static float clamp(float value, float min, float max) {
            if (!Float.isFinite(value)) return min;
            return Math.max(min, Math.min(max, value));
        }
    }
}
