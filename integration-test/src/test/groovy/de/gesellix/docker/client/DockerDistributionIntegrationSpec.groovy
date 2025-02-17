package de.gesellix.docker.client

import de.gesellix.docker.engine.DockerVersion
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Specification

@Slf4j
@Requires({ LocalDocker.available() })
class DockerDistributionIntegrationSpec extends Specification {

  static DockerClient dockerClient

  def setupSpec() {
    dockerClient = new DockerClientImpl()
  }

  @Requires({ LocalDocker.dockerVersion >= DockerVersion.parseDockerVersion("17.06") })
  def "can retrieve an image descriptor for alpine:edge"() {
    when:
    def alpineDescriptor = dockerClient.descriptor("alpine:edge")

    then:
    alpineDescriptor.content.descriptor.mediaType in [
        "application/vnd.docker.distribution.manifest.list.v2+json",
        "application/vnd.oci.image.index.v1+json"]
    alpineDescriptor.content.descriptor.digest =~ "sha256:[a-f\\d]{64}"
    alpineDescriptor.content.descriptor.propertySize =~ "\\d{3,4}"
    alpineDescriptor.content.platforms.find { it.architecture == "amd64" && it.os == "linux" }
  }

  @Requires({ LocalDocker.dockerVersion >= DockerVersion.parseDockerVersion("17.06") })
  def "can retrieve an image descriptor for debian:latest"() {
    when:
    def debianDescriptor = dockerClient.descriptor("debian:latest")

    then:
    debianDescriptor.content.descriptor.mediaType in [
        "application/vnd.docker.distribution.manifest.list.v2+json",
        "application/vnd.oci.image.index.v1+json"]
    debianDescriptor.content.descriptor.digest =~ "sha256:[a-f\\d]{64}"
    debianDescriptor.content.descriptor.propertySize =~ "\\d{3,4}"
    debianDescriptor.content.platforms.find { it.architecture == "amd64" && it.os == "linux" }
  }
}
