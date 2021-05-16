package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.engine.api.ContainerApi;
import de.gesellix.docker.engine.api.ExecApi;
import de.gesellix.docker.engine.api.ImageApi;
import de.gesellix.docker.engine.model.ContainerCreateRequest;
import de.gesellix.docker.engine.model.ExecConfig;
import de.gesellix.docker.engine.model.ExecInspectResponse;
import de.gesellix.docker.engine.model.ExecStartConfig;
import de.gesellix.docker.engine.model.IdResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DockerEngineAvailable
class ExecApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  ExecApi execApi;
  ContainerApi containerApi;
  ImageApi imageApi;

  @BeforeEach
  public void setup() {
    execApi = typeSafeDockerClient.getExecApi();
    containerApi = typeSafeDockerClient.getContainerApi();
    imageApi = typeSafeDockerClient.getImageApi();
  }

  @Test
  public void containerExec() {
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
    containerApi.containerCreate(containerCreateRequest, "container-exec-test");
    containerApi.containerStart("container-exec-test", null);

    IdResponse exec = execApi.containerExec(
        "container-exec-test",
        new ExecConfig(null, true, true, null, null,
                       null,
                       asList("echo", "'aus dem Wald'"),
                       null, null, null));
    assertNotNull(exec.getId());

    execApi.execStart(exec.getId(), new ExecStartConfig(false, null));

    ExecInspectResponse execInspect = execApi.execInspect(exec.getId());
    assertFalse(execInspect.getRunning());

    containerApi.containerStop("container-exec-test", 5);
    containerApi.containerWait("container-exec-test", null);
    containerApi.containerDelete("container-exec-test", null, null, null);
    imageApi.imageDelete("gesellix/testimage:os-linux", null, null);
  }
}
