package kamibot.KamibotLib;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Async {
  public static final ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Runs the given command in a separate thread without an error handler. 별도의 스레드에서 주어진 명령을 실행합니다. 에러 핸들러는 제공되지 않습니다.
   *
   * @param command the command to run. 실행할 명령.
   * @return an instance of Async. Async 인스턴스.
   */
  public static Async run(Runnable command) {
    return run(command, null);
  }

  /**
   * Runs the given command in a separate thread with an error handler. 별도의 스레드에서 주어진 명령을 실행합니다. 에러 핸들러가 제공됩니다.
   *
   * @param command the command to run. 실행할 명령.
   * @param onError the error handler. 에러 핸들러.
   * @return an instance of Async. Async 인스턴스.
   */
  public static Async run(Runnable command, Consumer<Exception> onError) {
    Async async = new Async();
    ExecutorService executor = Async.executor;
    executor.execute(() -> {
      try {
        command.run();
      } catch (Exception e) {
        if (onError != null)
          onError.accept(e);
        else
          e.printStackTrace();
      }
    });
    return async;
  };

}
