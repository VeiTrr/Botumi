package dev.behindthescenery.botumi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("./config/botumi.json");

    public static BConfig INSTANCE = new BConfig();

    public String protectedStructureId = "vt:town";
    public boolean protectInStructure = true;

    public static void setTestStructureId(String id) {
        Identifier rl = Identifier.tryParse(id);
        if (rl != null) {
            BConfig.INSTANCE.protectedStructureId = rl.toString();
            BConfig.save();
        }
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, BConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
