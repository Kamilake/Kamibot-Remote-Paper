package kamibot;

import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import kamibot.KamibotLib.Async;
import com.mojang.logging.LogUtils;

public class EventDispatcher {

  JsonObject event = new JsonObject();

  public EventDispatcher set(String key, Object value) {
    event.addProperty(key, new Gson().toJson(value));
    return this;
  }

  public EventDispatcher set(String key, String value) {
    event.addProperty(key, value);
    return this;
  }

  public EventDispatcher set(String key, int value) {
    event.addProperty(key, value);
    return this;
  }

  public EventDispatcher set(String key, long value) {
    event.addProperty(key, value);
    return this;
  }

  private static final Logger LOGGER = LogUtils.getLogger();

  public void send() {
    Async.run(() -> {
      int uuid = Config.kamibotRemoteUuid;
      event.addProperty("uuid", uuid);
      // json 형식으로 이벤트 생성
      String json = new Gson().toJson(event);
      if (Config.enableVerboseLogging)
        LOGGER.info("CommonEvent: " + json);
      WebsocketSender.sendMessage(json);
    });
  }
}
