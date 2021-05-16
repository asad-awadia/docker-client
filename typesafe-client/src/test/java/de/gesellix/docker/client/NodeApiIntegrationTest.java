package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.engine.api.NodeApi;
import de.gesellix.docker.engine.model.LocalNodeState;
import de.gesellix.docker.engine.model.Node;
import de.gesellix.docker.engine.model.NodeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DockerEngineAvailable(requiredSwarmMode = LocalNodeState.Active)
class NodeApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  NodeApi nodeApi;

  @BeforeEach
  public void setup() {
    nodeApi = typeSafeDockerClient.getNodeApi();
  }

  @Test
  public void nodeList() {
    List<Node> nodes = nodeApi.nodeList(null);
    assertFalse(nodes.isEmpty());
    Node firstNode = nodes.get(0);
    assertEquals(NodeState.Ready, firstNode.getStatus().getState());
  }

  @Test
  public void nodeInspect() {
    List<Node> nodes = nodeApi.nodeList(null);
    Node firstNode = nodes.get(0);
    Node node = nodeApi.nodeInspect(firstNode.getID());
    assertEquals(firstNode.getID(), node.getID());
  }
}
