package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.engine.api.DistributionApi;
import de.gesellix.docker.engine.model.DistributionInspectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class DistributionApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  DistributionApi distributionApi;

  @BeforeEach
  public void setup() {
    distributionApi = typeSafeDockerClient.getDistributionApi();
  }

  @Test
  public void distributionInspect() {
    DistributionInspectResponse response = distributionApi.distributionInspect("alpine:3.5");
    assertNotNull(response.getDescriptor().getDigest());
    assertTrue(response.getDescriptor().getDigest().startsWith("sha256:"));
  }
}
