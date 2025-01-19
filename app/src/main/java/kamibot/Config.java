package kamibot;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class Config {

    private static FileConfiguration config;

    public static String kamibotSocketUrl;
    public static int kamibotRemoteUuid;
    public static boolean enableVerboseLogging;

    public static void loadConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        kamibotSocketUrl = config.getString("kamibotSocketUrl", "wss://kamibot.kami.live/minecraftForge/ws");
        kamibotRemoteUuid = config.getInt("uuid", new Random().nextInt(Integer.MAX_VALUE));
        enableVerboseLogging = config.getBoolean("enableVerboseLogging", false);

        config.set("kamibotSocketUrl", kamibotSocketUrl);
        config.set("uuid", kamibotRemoteUuid);
        config.set("enableVerboseLogging", enableVerboseLogging);
        
        plugin.saveConfig();
    }

    public static void reloadConfig(JavaPlugin plugin) {
        plugin.reloadConfig();
        config = plugin.getConfig();

        kamibotSocketUrl = config.getString("kamibotSocketUrl");
        kamibotRemoteUuid = config.getInt("uuid");
        enableVerboseLogging = config.getBoolean("enableVerboseLogging");
    }
}