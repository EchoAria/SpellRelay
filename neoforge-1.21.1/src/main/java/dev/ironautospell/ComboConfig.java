package dev.ironautospell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

public final class ComboConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("ironautospell.json");
    public static class Profile {
        public String name = "Combo";
        public List<Integer> slots = new ArrayList<>();
        public SequenceEngine.Policy cooldown = SequenceEngine.Policy.SKIP;
    }
    public List<Profile> profiles = new ArrayList<>();
    public int selected = 0;
    public Profile current() { return profiles.get(selected); }
    public static ComboConfig load() {
        ComboConfig result = new ComboConfig();
        try {
            if (Files.exists(FILE)) result = GSON.fromJson(Files.readString(FILE), ComboConfig.class);
            if (result == null) throw new IOException("Empty config");
            result.validate();
        } catch (Exception e) {
            IronAutoSpell.LOGGER.error("Invalid combo config; preserving original and loading defaults", e);
            try { if (Files.exists(FILE)) Files.copy(FILE, FILE.resolveSibling("ironautospell.json.invalid-" + System.currentTimeMillis())); }
            catch (IOException copyError) { IronAutoSpell.LOGGER.error("Cannot back up invalid config", copyError); }
            result = new ComboConfig(); result.validate();
        }
        return result;
    }
    void validate() {
        if (profiles == null) profiles = new ArrayList<>();
        profiles.removeIf(Objects::isNull);
        if (profiles.isEmpty()) profiles.add(new Profile());
        if (profiles.size() > 32) profiles = new ArrayList<>(profiles.subList(0, 32));
        for (Profile p : profiles) {
            if (p.name == null || p.name.isBlank()) p.name = "Combo";
            if (p.slots == null) p.slots = new ArrayList<>();
            if (p.slots.size() > 64 || p.slots.stream().anyMatch(i -> i == null || i < 1 || i > 256))
                throw new IllegalArgumentException("Slots must be 1..256, at most 64 steps");
            if (p.cooldown == null) p.cooldown = SequenceEngine.Policy.SKIP;
        }
        selected = Math.clamp(selected, 0, profiles.size() - 1);
    }
    public void save() throws IOException {
        validate();
        Files.createDirectories(FILE.getParent());
        Path temp = FILE.resolveSibling("ironautospell.json.tmp");
        Files.writeString(temp, GSON.toJson(this), StandardCharsets.UTF_8);
        try { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING); }
    }
    public static List<Integer> parseSlots(String value) {
        if (value.isBlank()) return new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (String token : value.trim().split("[,，\\s→>]+")) {
            int slot = Integer.parseInt(token);
            if (slot < 1 || slot > 256 || slots.size() >= 64) throw new IllegalArgumentException("Invalid slot/length");
            slots.add(slot);
        }
        return slots;
    }
}
