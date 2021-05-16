package de.gesellix.docker.client;

import com.squareup.moshi.Moshi;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.engine.api.Cancellable;
import de.gesellix.docker.engine.api.ContainerApi;
import de.gesellix.docker.engine.api.Frame;
import de.gesellix.docker.engine.api.ImageApi;
import de.gesellix.docker.engine.api.StreamCallback;
import de.gesellix.docker.engine.client.infrastructure.ClientException;
import de.gesellix.docker.engine.client.infrastructure.LoggingExtensionsKt;
import de.gesellix.docker.engine.model.ContainerCreateRequest;
import de.gesellix.docker.engine.model.ContainerCreateResponse;
import de.gesellix.docker.engine.model.ContainerInspectResponse;
import de.gesellix.docker.engine.model.ContainerPruneResponse;
import de.gesellix.docker.engine.model.ContainerTopResponse;
import de.gesellix.docker.engine.model.ContainerUpdateRequest;
import de.gesellix.docker.engine.model.ContainerUpdateResponse;
import de.gesellix.docker.engine.model.RestartPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class ContainerApiIntegrationTest {

  private static final Logger log = LoggingExtensionsKt.logger(ContainerApiIntegrationTest.class.getName()).getValue();

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  ContainerApi containerApi;
  ImageApi imageApi;

  @BeforeEach
  public void setup() {
    containerApi = typeSafeDockerClient.getContainerApi();
    imageApi = typeSafeDockerClient.getImageApi();
  }

  @Test
  public void containerList() {
    List<Map<String, Object>> containers = containerApi.containerList(null, null, null, null);
    assertNotNull(containers);
  }

  @Test
  public void containerCreate() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "create", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        "test:create",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    ContainerCreateResponse container = containerApi.containerCreate(containerCreateRequest, "container-create-test");
    assertTrue(container.getId().matches("\\w+"));
    containerApi.containerDelete("container-create-test", null, null, null);
    imageApi.imageDelete("test:create", null, null);
  }

  @Test
  public void containerDelete() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "delete", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        "test:delete",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-delete-test");
    assertDoesNotThrow(() -> containerApi.containerDelete("container-delete-test", null, null, null));
    assertDoesNotThrow(() -> containerApi.containerDelete("container-delete-missing", null, null, null));
    imageApi.imageDelete("test:delete", null, null);
  }

  @Test
  public void containerInspect() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "inspect", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        "test:inspect",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-inspect-test");
    ContainerInspectResponse container = containerApi.containerInspect("container-inspect-test", false);
    assertEquals("/container-inspect-test", container.getName());
    containerApi.containerDelete("container-inspect-test", null, null, null);
    imageApi.imageDelete("test:inspect", null, null);
  }

  @Test
  public void containerInspectMissing() {
    ClientException clientException = assertThrows(ClientException.class, () -> containerApi.containerInspect("random-" + UUID.randomUUID(), false));
    assertEquals(404, clientException.getStatusCode());
  }

  @Test
  public void containerRename() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "rename", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        "test:rename",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-rename-test");
    assertDoesNotThrow(() -> containerApi.containerRename("container-rename-test", "fancy-name"));
    containerApi.containerDelete("fancy-name", null, null, null);
    imageApi.imageDelete("test:rename", null, null);
  }

  @Test
  public void containerStartStopWait() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-start-test");
    containerApi.containerStart("container-start-test", null);
    ContainerInspectResponse container = containerApi.containerInspect("container-start-test", false);
    assertTrue(container.getState().getRunning());
    containerApi.containerStop("container-start-test", 5);
    containerApi.containerWait("container-start-test", null);
    containerApi.containerDelete("container-start-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerLogsWithoutTty() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-logs-test");
    containerApi.containerStart("container-logs-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogFrameStreamCallback callback = new LogFrameStreamCallback();

    new Thread(() -> containerApi.containerLogs(
        "container-logs-test",
        false, true, true, null, null, null, null,
        callback, timeout.toMillis())).start();

    CountDownLatch wait = new CountDownLatch(1);
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        callback.job.cancel();
        wait.countDown();
      }
    }, 5000);

    try {
      wait.await();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertSame(callback.frames.stream().findAny().get().getStreamType(), Frame.StreamType.STDOUT);

    containerApi.containerStop("container-logs-test", 5);
    containerApi.containerWait("container-logs-test", null);
    containerApi.containerDelete("container-logs-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerLogsWithTty() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        true, null, null,
        null,
        null,
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-logs-with-tty-test");
    containerApi.containerStart("container-logs-with-tty-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogFrameStreamCallback callback = new LogFrameStreamCallback();

    new Thread(() -> containerApi.containerLogs(
        "container-logs-with-tty-test",
        false, true, true, null, null, null, null,
        callback, timeout.toMillis())).start();

    CountDownLatch wait = new CountDownLatch(1);
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        callback.job.cancel();
        wait.countDown();
      }
    }, 5000);

    try {
      wait.await();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertSame(callback.frames.stream().findAny().get().getStreamType(), Frame.StreamType.RAW);

    containerApi.containerStop("container-logs-with-tty-test", 5);
    containerApi.containerWait("container-logs-with-tty-test", null);
    containerApi.containerDelete("container-logs-with-tty-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerUpdate() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-update-test");
    containerApi.containerStart("container-update-test", null);
    ContainerUpdateRequest updateRequest = new ContainerUpdateRequest(
        null, null, null,
        null, null, null, null, null, null,
        null, null, null, null, null, null,
        null, null, null,
        null, null, null, null, null,
        null, null, null, null, null,
        null, null, null, null,
        new RestartPolicy(RestartPolicy.Name.UnlessMinusStopped, null));
    ContainerUpdateResponse updateResponse = containerApi.containerUpdate("container-update-test", updateRequest);
    assertTrue(updateResponse.getWarnings() == null || updateResponse.getWarnings().isEmpty());
    containerApi.containerStop("container-update-test", 5);
    containerApi.containerWait("container-update-test", null);
    containerApi.containerDelete("container-update-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerRestart() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-restart-test");
    containerApi.containerStart("container-restart-test", null);
    assertDoesNotThrow(() -> containerApi.containerRestart("container-restart-test", 5));
    containerApi.containerStop("container-restart-test", 5);
    containerApi.containerWait("container-restart-test", null);
    containerApi.containerDelete("container-restart-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerKill() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-kill-test");
    containerApi.containerStart("container-kill-test", null);
    assertDoesNotThrow(() -> containerApi.containerKill("container-kill-test", null));
    containerApi.containerWait("container-kill-test", null);
    containerApi.containerDelete("container-kill-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerPauseUnpause() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-pause-test");
    containerApi.containerStart("container-pause-test", null);
    assertDoesNotThrow(() -> containerApi.containerPause("container-pause-test"));
    ContainerInspectResponse pausedContainer = containerApi.containerInspect("container-pause-test", false);
    assertTrue(pausedContainer.getState().getPaused());
    assertDoesNotThrow(() -> containerApi.containerUnpause("container-pause-test"));
    ContainerInspectResponse unpausedContainer = containerApi.containerInspect("container-pause-test", false);
    assertFalse(unpausedContainer.getState().getPaused());
    containerApi.containerStop("container-pause-test", 5);
    containerApi.containerWait("container-pause-test", null);
    containerApi.containerDelete("container-pause-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerPrune() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-prune-test");

    Map<String, List<String>> filter = new HashMap<>();
    filter.put("label", singletonList(LABEL_KEY));
    String filterJson = new Moshi.Builder().build().adapter(Map.class).toJson(filter);

    Optional<Map<String, Object>> toBePruned = containerApi.containerList(true, null, null, filterJson).stream().filter((c) -> ((List<String>) c.get("Names")).contains("/container-prune-test")).findFirst();
    assertTrue(toBePruned.isPresent());

    ContainerPruneResponse pruneResponse = containerApi.containerPrune(filterJson);
    assertTrue(pruneResponse.getContainersDeleted().contains(toBePruned.get().get("Id")));

    Optional<Map<String, Object>> shouldBeMissing = containerApi.containerList(true, null, null, filterJson).stream().filter((c) -> ((List<String>) c.get("Names")).contains("/container-prune-test")).findFirst();
    assertFalse(shouldBeMissing.isPresent());

    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  // the api reference v1.41 says: "On Unix systems, this is done by running the ps command. This endpoint is not supported on Windows."
  @Test
  public void containerTop() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-top-test");
    containerApi.containerStart("container-top-test", null);

    ContainerTopResponse processes = containerApi.containerTop("container-top-test", null);
    assertEquals("ping 127.0.0.1", processes.getProcesses().get(0).get(processes.getTitles().indexOf("CMD")));

    containerApi.containerStop("container-top-test", 5);
    containerApi.containerWait("container-top-test", null);
    containerApi.containerDelete("container-top-test", null, null, null);

    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerStatsStream() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-stats-test");
    containerApi.containerStart("container-stats-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogObjectStreamCallback callback = new LogObjectStreamCallback();

    containerApi.containerStats("container-stats-test", null, null, callback, timeout.toMillis());
    assertFalse(callback.elements.isEmpty());

    containerApi.containerStop("container-stats-test", 5);
    containerApi.containerWait("container-stats-test", null);
    containerApi.containerDelete("container-stats-test", null, null, null);

    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  @Test
  public void containerStatsOnce() {
    imageApi.imageCreate("gesellix/testimage", null, null, "os-linux", null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        asList("ping", "127.0.0.1"),
        null,
        null,
        "gesellix/testimage:os-linux",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-stats-test");
    containerApi.containerStart("container-stats-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogObjectStreamCallback callback = new LogObjectStreamCallback();

    containerApi.containerStats("container-stats-test", false, null, callback, timeout.toMillis());
    assertFalse(callback.elements.isEmpty());

    containerApi.containerStop("container-stats-test", 5);
    containerApi.containerWait("container-stats-test", null);
    containerApi.containerDelete("container-stats-test", null, null, null);

    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }

  static class LogFrameStreamCallback implements StreamCallback<Frame> {

    List<Frame> frames = new ArrayList<>();
    Cancellable job = null;

    @Override
    public void onStarting(Cancellable cancellable) {
      job = cancellable;
    }

    @Override
    public void onNext(Frame frame) {
      frames.add(frame);
      log.info("next: {}", frame);
    }
  }

  static class LogObjectStreamCallback implements StreamCallback<Object> {

    List<Object> elements = new ArrayList<>();
    Cancellable job = null;

    @Override
    public void onStarting(Cancellable cancellable) {
      job = cancellable;
    }

    @Override
    public void onNext(Object element) {
      elements.add(element);
      log.info("next: {}", element);
    }
  }
}
