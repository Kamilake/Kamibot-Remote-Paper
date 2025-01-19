package kamibot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class WebsocketSender extends WebSocketClient {

  public static WebsocketSender instance;
  private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketSender.class);
  private static JavaPlugin plugin;

  public WebsocketSender(URI serverUri, JavaPlugin plugin) {
    super(serverUri);
    WebsocketSender.plugin = plugin;
    instance = this;
  }

  public static void sendMessage(String message) {
    try {
      if (instance == null) {
        instance = new WebsocketSender(URI.create(Config.kamibotSocketUrl), plugin);
        if (Config.enableVerboseLogging)
          LOGGER.info("Connecting to websocket server... (" + Config.kamibotSocketUrl + ")");
        instance.connectBlocking(10, TimeUnit.SECONDS);
        instance.send("{\"eventType\":\"auth\",\"uuid\":" + Config.kamibotRemoteUuid + "}");
      }
      instance.send(message);
    } catch (InterruptedException | WebsocketNotConnectedException e) {
      if (Config.enableVerboseLogging)
        LOGGER.error("Failed to send message: " + message + " because the websocket is not connected.");
      instance = new WebsocketSender(URI.create(Config.kamibotSocketUrl), plugin);
      if (Config.enableVerboseLogging)
        LOGGER.info("Reconnecting to websocket server... (" + Config.kamibotSocketUrl + ")");
      try {
        instance.connectBlocking(10, TimeUnit.SECONDS);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      instance.send("{\"eventType\":\"auth\",\"uuid\":" + Config.kamibotRemoteUuid + "}");
      instance.send(message);
    }
  }

  @Override
  public void onOpen(ServerHandshake handshakedata) {
    if (Config.enableVerboseLogging)
      LOGGER.info("Connected");
  }

  @Override
  public void onMessage(String json) {
    if (Config.enableVerboseLogging)
      LOGGER.info("Received: " + json);
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      JSONObject messageObject = new JSONObject(json);
      String eventType = messageObject.getString("eventType");
      switch (eventType) {
        case "SendChatEvent":
          sendChatToMinecraft(messageObject.getString("message"));
          break;
        case "SendTellEvent":
          sendTellToMinecraft(messageObject.getString("message"), messageObject.getString("user"));
          break;
        default:
          LOGGER.warn("Unknown event type: " + eventType);
      }
    });
  }

  private void sendChatToMinecraft(String content) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(content);
    }
  }

  private void sendTellToMinecraft(String content, String user) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getUniqueId().toString().equals(user) || player.getName().equals(user)) {
        player.sendMessage(content);
      }
    }
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    LOGGER.info("Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
  }

  @Override
  public void onError(Exception ex) {
    ex.printStackTrace();
  }
}