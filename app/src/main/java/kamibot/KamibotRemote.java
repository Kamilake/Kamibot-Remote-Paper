package kamibot;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

public class KamibotRemote extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    Config.loadConfig(this);
    new WebsocketSender(URI.create(Config.kamibotSocketUrl), this);

    Logger logger = getLogger();
    logger.info("====Kamibot Remote====");
    logger.info("Connecting " + Config.kamibotSocketUrl + "...");
    logger.info("UUID: " + Config.kamibotSocketUrl);

    Server server = getServer();
    server.getPluginManager().registerEvents(this, this);
    server.getPluginCommand("kamibot-info").setExecutor(this);
    server.getPluginCommand("kamibot-register").setExecutor(this);
    server.getPluginCommand("discord").setExecutor(this);

    new EventDispatcher()
        .set("eventType", "ServerStartingEvent")
        .set("motd", ((TextComponent) getServer().motd()).content())
        .send();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("kamibot-info")) {
      String output = "====Kamibot Remote 정보====\n";
      output += "서버: " + ((TextComponent) getServer().motd()).content() + "\n";
      output += "UUID: " + Config.kamibotRemoteUuid + "\n";
      output += "소켓: " + Config.kamibotSocketUrl + "\n";
      long delay = measureHttpGetDelay();
      output += "소켓 연결 상태: " + (WebsocketSender.instance == null
          ? "소켓 없음"
          : WebsocketSender.instance.isOpen() ? "연결됨, Https (" + (delay == -1 ? "측정 불가능" : delay + "ms") + ")"
              : "연결 끊김");
      sender.sendMessage(output);
      return true;
    } else if (command.getName().equalsIgnoreCase("kamibot-register")) {
      if (args.length < 1) {
        sender.sendMessage("사용법: /kamibot-register <채널 링크>\n연동하고 싶은 Discord 채널을 우클릭한 다음 '링크 복사하기'를 누르세요.");
        return true;
      }
      String url = args[0];
      String[] urlParts = url.split("/");
      if (urlParts.length < 6 || !urlParts[2].equals("discord.com") || !urlParts[3].equals("channels")) {
        sender.sendMessage(
            "올바른 Discord 채널 링크를 입력하세요.\n 예시: /kamibot-register https://discord.com/channels/1277440737939554376/1330761715306201188");
        return true;
      }
      String guildId = urlParts[4];
      String channelId = urlParts[5];

      Player player = sender instanceof Player ? (Player) sender : null;
      String playerName = player != null ? player.getName() : null;
      String playerUUID = player != null ? player.getUniqueId().toString() : null;

      new EventDispatcher()
          .set("eventType", "RegisterChannelEvent")
          .set("guildId", guildId)
          .set("channelId", channelId)
          .set("serverName", ((TextComponent) getServer().motd()).content())
          .set("uuid", Config.kamibotRemoteUuid)
          .set("playerName", playerName)
          .set("playerUUID", playerUUID)
          .send();

      sender.sendMessage("등록 요청에 성공했어요! 해당 채널에서 카미봇이 보낸 메세지를 확인해보세요.");
      return true;
    } else if (command.getName().equalsIgnoreCase("discord")) {
      if (args.length < 1) {
        sender.sendMessage(
            "====Kamibot Remote 도움말====\n\n내 Minecraft 계정과 Discord 계정을 같은 이름으로 표시해주는 기능이에요.\n사용법: /discord <내 디스코드 계정>\n이렇게 하면 여러분의 메세지가 Discord 채널에서 올바른 이름으로 표시돼요!\n예시: /discord kamilake\n예시: /discord 1019061779357245521");
        return true;
      }
      String id = args[0];
      if (!(sender instanceof Player)) {
        sender.sendMessage("이 명령어는 인게임에서만 사용할 수 있어요.");
        return true;
      }
      Player player = (Player) sender;
      new EventDispatcher()
          .set("eventType", "DiscordIntegrationEvent")
          .set("discordId", id)
          .set("serverName", ((TextComponent) getServer().motd()).content())
          .set("serverUuid", Config.kamibotRemoteUuid)
          .set("playerName", player.getName())
          .set("playerUUID", player.getUniqueId().toString())
          .send();

      sender.sendMessage("등록 요청에 성공했어요! 카미봇의 DM에서 카미봇이 보낸 메세지를 확인해보세요. (시간 제한 5분)");
      return true;
    }
    return false;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.addAttachment(this, "kamibot.discord", true);
    player.addAttachment(this, "kamibot.info", true);

    new EventDispatcher()
        .set("eventType", "PlayerLoggedInEvent")
        .set("playerName", player.getName())
        .set("playerUUID", player.getUniqueId().toString())
        .set("playerCount", Bukkit.getOnlinePlayers().size())
        .set("maxPlayerCount", getServer().getMaxPlayers())
        .send();
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    new EventDispatcher()
        .set("eventType", "PlayerLoggedOutEvent")
        .set("playerName", player.getName())
        .set("playerUUID", player.getUniqueId().toString())
        .set("playerCount", Bukkit.getOnlinePlayers().size() - 1)
        .set("maxPlayerCount", getServer().getMaxPlayers())
        .send();
  }

  @EventHandler
  public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
    // 레벨 변경 이벤트 처리
  }

  @EventHandler
  public void onPlayerChat(AsyncChatEvent event) {
    TextComponent message = (TextComponent) event.message();
    new EventDispatcher()
        .set("eventType", "ServerChatEvent")
        .set("message", message.content())
        .set("playerName", event.getPlayer().getName())
        .set("playerUUID", event.getPlayer().getUniqueId().toString())
        .send();
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    String killer = player.getKiller() != null ? player.getKiller().getName() : "environment";
    EntityDamageEvent damageEvent = player.getLastDamageCause();
    DamageSource damageSource = damageEvent.getDamageSource();
    String playerPosString = String.format("%.2f, %.2f, %.2f", player.getLocation().getX(), player.getLocation().getY(),
        player.getLocation().getZ());
    String deathMessage = player.getName() + " was killed by " + killer;
    new EventDispatcher()
        .set("result", "DEATH")
        .set("eventType", "PlayerDeathEvent")
        .set("playerName", player.getName())
        .set("killerName", killer)
        .set("damageSource", damageSource.getDamageType().getTranslationKey())
        .set("playerPos", playerPosString)
        .set("playerUUID", player.getUniqueId().toString())
        .set("deathMessage", deathMessage)
        .send();
  }

  public long measureHttpGetDelay() {
    try {
      URL url = URI.create("https://kamibot.app/api").toURL();
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      connection.setRequestMethod("GET");
      connection.setConnectTimeout(1000); // Set timeout to 1000ms
      connection.setReadTimeout(1000); // Set read timeout to 1000ms

      long startTime = System.currentTimeMillis();
      int responseCode = connection.getResponseCode();
      long endTime = System.currentTimeMillis();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        return endTime - startTime;
      } else {
        return -1;
      }
    } catch (Exception e) {
      return -1;
    }
  }
}
