package de.gesellix.docker.client;

import com.squareup.moshi.Moshi;
import de.gesellix.docker.client.builder.BuildContextBuilder;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.DockerRegistry;
import de.gesellix.docker.client.testutil.HttpTestServer;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.NetworkInterfaces;
import de.gesellix.docker.engine.api.ContainerApi;
import de.gesellix.docker.engine.api.ImageApi;
import de.gesellix.docker.engine.model.BuildPruneResponse;
import de.gesellix.docker.engine.model.ContainerCreateRequest;
import de.gesellix.docker.engine.model.ContainerCreateResponse;
import de.gesellix.docker.engine.model.HistoryResponseItem;
import de.gesellix.docker.engine.model.IdResponse;
import de.gesellix.docker.engine.model.Image;
import de.gesellix.docker.engine.model.ImageDeleteResponseItem;
import de.gesellix.docker.engine.model.ImageSearchResponseItem;
import de.gesellix.docker.engine.model.ImageSummary;
import de.gesellix.testutil.ResourceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static java.nio.file.Files.readAttributes;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class ImageApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  ImageApi imageApi;
  ContainerApi containerApi;

  @BeforeEach
  public void setup() {
    imageApi = typeSafeDockerClient.getImageApi();
    containerApi = typeSafeDockerClient.getContainerApi();
  }

  @Test
  public void buildPrune() {
    BuildPruneResponse response = imageApi.buildPrune(null, null, null);
    assertTrue(response.getSpaceReclaimed() >= 0);
  }

  @Test
  public void imageBuild() throws IOException {
    String dockerfile = "/images/builder/Dockerfile";
    File inputDirectory = ResourceReader.getClasspathResourceAsFile(dockerfile, ImageApi.class).getParentFile();
    InputStream buildContext = newBuildContext(inputDirectory);
    assertDoesNotThrow(() -> imageApi.imageBuild(null, "test:build", null, null, null, null, null, null,
                                                 null, null, null, null, null, null, null,
                                                 null, null, null, null, null, null, null,
                                                 null, null, null, null, buildContext));
  }

  InputStream newBuildContext(File baseDirectory) throws IOException {
    ByteArrayOutputStream buildContext = new ByteArrayOutputStream();
    BuildContextBuilder.archiveTarFilesRecursively(baseDirectory, buildContext);
    return new ByteArrayInputStream(buildContext.toByteArray());
  }

  @Test
  public void imageCreatePullFromRemote() {
    assertDoesNotThrow(() -> imageApi.imageCreate("alpine", null, null, "edge", null, null, null, null, null));
  }

  @Test
  public void imageCreateImportFromUrl() throws IOException {
    URL importUrl = getClass().getResource("/images/importUrl/import-from-url.tar");
    HttpTestServer server = new HttpTestServer();
    InetSocketAddress serverAddress = server.start("/images/", new HttpTestServer.FileServer(importUrl));
    int port = serverAddress.getPort();
    List<String> addresses = new NetworkInterfaces().getInet4Addresses();
    String url = String.format("http://%s:%s/images/%s", addresses.get(0), port, importUrl.getPath());

    assertDoesNotThrow(() -> imageApi.imageCreate(null, url, "test", "from-url", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, null));

    server.stop();
    imageApi.imageDelete("test:from-url", null, null);
  }

  @Test
  public void imageCreateImportFromInputStream() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      assertDoesNotThrow(() -> imageApi.imageCreate(null, "-", "test", "from-stream", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source));
    }
    imageApi.imageDelete("test:from-stream", null, null);
  }

  @Test
  public void imageCommit() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      assertDoesNotThrow(() -> imageApi.imageCreate(null, "-", "test", "commit", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source));
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
        "test:commit",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    ContainerCreateResponse container = containerApi.containerCreate(containerCreateRequest, "container-commit-test");
    IdResponse image = imageApi.imageCommit(container.getId(), "test", "commited", null, null, null, null, null);
    assertTrue(image.getId().matches("sha256:\\w+"));
    imageApi.imageDelete("test:commited", null, null);
    containerApi.containerDelete("container-commit-test", null, null, null);
    imageApi.imageDelete("test:commit", null, null);
  }

  @Test
  public void imageList() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "list", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }
    List<ImageSummary> images = imageApi.imageList(null, null, null);
    assertEquals(1, images.stream().filter((i) -> i.getRepoTags() != null && i.getRepoTags().stream().filter((t) -> t.equals("test:list")).count() > 0).count());
    imageApi.imageDelete("test:list", null, null);
  }

  @Test
  public void imageDelete() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "delete", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }
    List<ImageDeleteResponseItem> deletedImages = imageApi.imageDelete("test:delete", null, null);
    assertEquals(1, deletedImages.stream().filter((e) -> e.getDeleted() != null).count());
  }

  @Test
  public void imagePrune() throws IOException {
    Map<String, List<String>> filter = new HashMap<>();
    filter.put("label", singletonList(LABEL_KEY));
    String filterJson = new Moshi.Builder().build().adapter(Map.class).toJson(filter);

    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "prune", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }
    assertDoesNotThrow(() -> imageApi.imagePrune(filterJson));
  }

  @Test
  public void imageGet() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "export", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }
    File exportedImage = imageApi.imageGet("test:export");
    assertEquals("16896", readAttributes(exportedImage.toPath(), "size", NOFOLLOW_LINKS).get("size").toString());

    imageApi.imageDelete("test:export", null, null);
  }

  @Test
  public void imageGetAll() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "export-all-1", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "export-all-2", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }

    File exportedImages = imageApi.imageGetAll(asList("test:export-all-1", "test:export-all-2"));
    assertEquals("22016", readAttributes(exportedImages.toPath(), "size", NOFOLLOW_LINKS).get("size").toString());

    imageApi.imageDelete("test:export-all-1", null, null);
    imageApi.imageDelete("test:export-all-2", null, null);
  }

  @Test
  public void imageLoad() {
    File tarFile = new File(getClass().getResource("/images/loadImage/load-from-file.tar").getPath());
    assertDoesNotThrow(() -> imageApi.imageLoad(false, tarFile));
    assertEquals(LABEL_VALUE, imageApi.imageInspect("test:load-image").getConfig().getLabels().get(LABEL_KEY));
    this.imageApi.imageDelete("test:load-image", null, null);
  }

  @Test
  public void imageHistory() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "history", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }

    List<HistoryResponseItem> history = imageApi.imageHistory("test:history");
    assertEquals(1, history.size());
    assertEquals("Imported from -", history.get(0).getComment());

    imageApi.imageDelete("test:history", null, null);
  }

  @Test
  public void imageInspect() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "inspect", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }

    Image image = imageApi.imageInspect("test:inspect");
    assertEquals("Imported from -", image.getComment());

    imageApi.imageDelete("test:inspect", null, null);
  }

  @Test
  public void imageSearch() {
    List<ImageSearchResponseItem> searchResult = imageApi.imageSearch("alpine", 1, null);
    assertEquals(1, searchResult.size());
    assertEquals("alpine", searchResult.get(0).getName());
  }

  @Test
  public void imageTag() throws IOException {
    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "tag", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }
    imageApi.imageTag("test:tag", "test/image", "test-tag");
    Image image1 = imageApi.imageInspect("test:tag");
    Image image2 = imageApi.imageInspect("test/image:test-tag");
    assertFalse(image1.getId().isEmpty());
    assertEquals(image1.getId(), image2.getId());

    imageApi.imageDelete("test:tag", null, null);
    imageApi.imageDelete("test/image:test-tag", null, null);
  }

  @Test
  public void imagePushToCustomRegistry() throws IOException {
    DockerRegistry registry = new DockerRegistry(typeSafeDockerClient);
    registry.run();
    String registryUrl = registry.url();

    try (InputStream source = getClass().getResourceAsStream("/images/importUrl/import-from-url.tar")) {
      imageApi.imageCreate(null, "-", "test", "push", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source);
    }
    imageApi.imageTag("test:push", registryUrl + "/test", "push");

    imageApi.imagePush(registryUrl + "/test", "", "push");

    registry.rm();

    imageApi.imageDelete("test:push", null, null);
    imageApi.imageDelete(registryUrl + "/test:push", null, null);
  }
}
