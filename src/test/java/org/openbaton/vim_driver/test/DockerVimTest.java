package org.openbaton.vim_driver.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openbaton.catalogue.nfvo.NFVImage;
import org.openbaton.catalogue.nfvo.Network;
import org.openbaton.catalogue.nfvo.Server;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.docker.DockerVim;
import org.openbaton.exceptions.VimDriverException;

/** Created by sakib on 9/18/16. */
public class DockerVimTest {

  private DockerVim dockerVim;
  private VimInstance vimInstance;
  private DockerClient dockerClient;

  /*private DockerClient createClient(String endpoint) {
    DockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(endpoint)
            .withDockerTlsVerify(true)
            .withDockerCertPath("/home/sakib/.docker")
            .build();
    return DockerClientBuilder.getInstance(config).build();
  }*/

  @Before
  public void init() throws Exception {
    vimInstance = new VimInstance();
    vimInstance.setAuthUrl("tcp://Thesis:2376");
    dockerVim = new DockerVim();
    dockerClient = dockerVim.createClient(vimInstance.getAuthUrl());

    List<Server> servers = dockerVim.listServer(vimInstance);
    for (Server server : servers) {
      dockerVim.deleteServerByIdAndWait(vimInstance, server.getId());
    }

    List<Network> networks = dockerVim.listNetworks(vimInstance);
    for (Network network : networks) {
      if (network.getName().indexOf("host") < 0
          && network.getName().indexOf("bridge") < 0
          && network.getName().indexOf("none") < 0) {
        dockerVim.deleteNetwork(vimInstance, network.getId());
      }
    }

    ListVolumesResponse listVolumesResponse = dockerClient.listVolumesCmd().exec();
    System.out.println("List of volumes: " + listVolumesResponse.getVolumes());
    if (listVolumesResponse.getVolumes() != null) {
      for (InspectVolumeResponse volume : listVolumesResponse.getVolumes()) {
        dockerVim.deleteVolume(vimInstance, volume.getName());
      }
    }
  }

  @Ignore
  @Test
  public void launchInstanceTest() throws VimDriverException {
    List<String> exposedPortList = new ArrayList<>();
    exposedPortList.add("8084");
    exposedPortList.add("9046");
    List<String> environmentVariables = new ArrayList<>();
    environmentVariables.add("port=8080");
    try {
      Server server =
          dockerVim.launchInstance(
              vimInstance, "MyContainer", "ubuntu:14.04", exposedPortList, environmentVariables);
      // System.out.println("CREATED SERVER : " + server);
      assertEquals("Check server name ", "MyContainer", server.getName());
      assertEquals("Check server hostName ", "Openbaton", server.getHostName());
      assertEquals("Check server image ", "ubuntu:14.04", server.getImage().getName());
      assertEquals("Check Format ", "docker", server.getImage().getContainerFormat());
      InspectContainerResponse inspectContainerResponse =
          dockerClient.inspectContainerCmd(server.getId()).exec();
      assertThat(
          inspectContainerResponse
                  .getHostConfig()
                  .getPortBindings()
                  .getBindings()
                  .get(ExposedPort.tcp(8084))[
              0],
          is(equalTo(Ports.Binding.bindPort(8084))));
      assertThat(
          inspectContainerResponse
                  .getHostConfig()
                  .getPortBindings()
                  .getBindings()
                  .get(ExposedPort.tcp(9046))[
              0],
          is(equalTo(Ports.Binding.bindPort(9046))));
    } catch (VimDriverException e) {
      e.printStackTrace();
    }
  }

  @Ignore
  @Test
  public void createDockerNetworkTest() throws VimDriverException {
    Network createdNetwork = new Network();
    try {
      createdNetwork = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
      //System.out.println(createdNetwork);
    } catch (VimDriverException e) {
      e.printStackTrace();
    } catch (DockerException e) {
      e.printStackTrace();
    }
    System.out.println("Printing created network: " + createdNetwork);
    String networkID = createdNetwork.getId();
    assertEquals("Check Network Name ", "MyNetwork", createdNetwork.getName());

    List<Network> networks = dockerVim.listNetworks(vimInstance);
    for (Network network : networks) {
      if (network.getName().indexOf("host") < 0
          && network.getName().indexOf("bridge") < 0
          && network.getName().indexOf("none") < 0) {
        assertEquals("Verifying Network Name", "MyNetwork", network.getName());
        assertEquals("Verifying Network ID", networkID, network.getId());
        assertEquals(
            "Verifying Network subnet",
            "172.19.0.0/16",
            network.getSubnets().iterator().next().getNetworkId());
        assertEquals(
            "Verifying Network subnet",
            "172.19.0.1",
            network.getSubnets().iterator().next().getGatewayIp());
      }
    }
  }

  @Ignore
  @Test
  public void listImagesTest() throws VimDriverException {
    NFVImage nfvImage = dockerVim.pullImage(vimInstance, "hello-world:latest");
    try {
      List<NFVImage> images = dockerVim.listImages(vimInstance);
      assertEquals("Verify number of images ", 1, images.size());
      // Test will pass if there is no other images and hello-world is the first image of 'images'
      assertEquals("Verify image name ", "hello-world:latest", images.get(0).getName());
    } catch (VimDriverException e) {
      e.printStackTrace();
    }
    dockerVim.pullImage(vimInstance, "ubuntu:latest");
    try {
      List<NFVImage> images = dockerVim.listImages(vimInstance);
      assertEquals("Verify number of images: ", 2, images.size());
      String existingImage = "";
      for (NFVImage image : images) {
        existingImage += image.getName();
      }
      //assertTrue(existingImage.indexOf("hello-world") >= 0);
      //assertTrue(existingImage.indexOf("ubuntu") >= 0);
    } catch (VimDriverException e) {
      e.printStackTrace();
    }
    dockerVim.deleteImage(vimInstance, nfvImage);
  }

  @Ignore
  @Test
  public void listServerTest() throws VimDriverException {
    List<String> exposedPortList = new ArrayList<>();
    exposedPortList.add("8084");
    List<String> environmentVariables = new ArrayList<>();
    environmentVariables.add("port=8080");

    Server createdServer =
        dockerVim.launchInstance(
            vimInstance, "MyContainer", "ubuntu:14.04", exposedPortList, environmentVariables);
    try {
      List<Server> servers = dockerVim.listServer(vimInstance);
      //for (Server server : servers) {
      //    System.out.println("Server : " + server);
      //}
      Server server = servers.get(0);
      assertEquals("Check server Name ", createdServer.getName(), server.getName());
      assertEquals("Check server ID ", createdServer.getId(), server.getId());
    } catch (VimDriverException e) {
      e.printStackTrace();
    }
  }

  @Ignore
  @Test
  public void listNetworksTest() throws VimDriverException {
    Network createdNetwork = new Network();
    try {
      createdNetwork = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
      //System.out.println(createdNetwork);
    } catch (VimDriverException e) {
      e.printStackTrace();
    } catch (DockerException e) {
      e.printStackTrace();
    }
    List<Network> networks = dockerVim.listNetworks(vimInstance);
    //System.out.println("Printing network : " + networks.get(0));
    //System.out.println("No. of network: " + networks.size());
    assertEquals("Checking number of network ", 1, networks.size());
    assertEquals("Checking network name ", createdNetwork.getId(), networks.get(0).getId());
    assertEquals(
        "Checking network subnet ",
        createdNetwork.getSubnets().iterator().next().getNetworkId(),
        networks.get(0).getSubnets().iterator().next().getNetworkId());
  }

  @Ignore
  @Test
  public void deleteServerByIdAndWaitTest() throws VimDriverException {
    List<String> exposedPortList = new ArrayList<>();
    exposedPortList.add("8084");
    List<String> environmentVariables = new ArrayList<>();
    environmentVariables.add("port=8080");

    Server createdServer =
        dockerVim.launchInstance(
            vimInstance, "MyContainer", "ubuntu:14.04", exposedPortList, environmentVariables);
    String serverID = createdServer.getId();
    int numberOfServer = dockerVim.listServer(vimInstance).size();
    dockerVim.deleteServerByIdAndWait(vimInstance, serverID);
    assertEquals(
        "Check number of server after deleting ",
        (numberOfServer - 1),
        dockerVim.listServer(vimInstance).size());
  }

  @Ignore
  @Test
  public void pullImageTest() throws VimDriverException {
    // need to be implemented after getting confirmation
    NFVImage nfvImage = null;
    try {
      List<NFVImage> images = dockerVim.listImages(vimInstance);
      int totalImage = images.size();

      nfvImage = dockerVim.pullImage(vimInstance, "hello-world:latest");
      List<NFVImage> imagesAfterPulling = dockerVim.listImages(vimInstance);
      int totalImageAfterPulling = imagesAfterPulling.size();
      assertEquals(
          "Total image number increased by 1 after pulling",
          (totalImage + 1),
          totalImageAfterPulling);
    } catch (Exception e) {
      //e.printStackTrace();
    }
    dockerVim.deleteImage(vimInstance, nfvImage);
  }

  @Ignore
  @Test
  public void deleteImageTest() throws VimDriverException {
    NFVImage nfvImage = dockerVim.pullImage(vimInstance, "hello-world:latest");
    System.out.println("Printing NfvImage: " + nfvImage);
    int numberOfImages = dockerVim.listImages(vimInstance).size();
    System.out.println("Number of images : " + numberOfImages);
    try {
      dockerVim.deleteImage(vimInstance, nfvImage);
    } catch (VimDriverException e) {
      //e.printStackTrace();
    }
    System.out.println("Number of images After: " + dockerVim.listImages(vimInstance).size());
    assertEquals(
        "Verifying number of images after deleting",
        numberOfImages - 1,
        dockerVim.listImages(vimInstance).size());
  }

  @Ignore
  @Test
  public void deleteNetworkTest() throws VimDriverException {
    Network network = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
    int numberOfNetworks = dockerVim.listNetworks(vimInstance).size();
    System.out.println("Num of networks : " + numberOfNetworks);
    try {
      dockerVim.deleteNetwork(vimInstance, network.getId());
      System.out.println(network);
    } catch (VimDriverException e) {
      //e.printStackTrace();
    }
    System.out.println("Num of networks After : " + dockerVim.listNetworks(vimInstance).size());
    assertEquals(
        "Check number of Network after deleting",
        numberOfNetworks - 1,
        dockerVim.listNetworks(vimInstance).size());
  }

  @Ignore
  @Test
  public void getNetworkByIdTest() throws VimDriverException {
    Network createdNetwork = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
    String networkID = createdNetwork.getId();
    Network network = new Network();
    try {
      network = dockerVim.getNetworkById(vimInstance, createdNetwork.getId());
      System.out.println("NFV Network: " + network);
    } catch (VimDriverException e) {
      e.printStackTrace();
    }
    assertEquals("Verifying Network Name", "MyNetwork", network.getName());
    assertEquals("Verifying Network ID", networkID, network.getId());
    assertEquals(
        "Verifying Network subnet",
        "172.19.0.0/16",
        network.getSubnets().iterator().next().getNetworkId());
    assertEquals(
        "Verifying Network subnet",
        "172.19.0.1",
        network.getSubnets().iterator().next().getGatewayIp());
  }

  /**
   * connectContainerToNetworkTest() is not able to connect container with network if the container
   * is not UP
   */
  //@Ignore
  @Test
  public void connectContainerToNetworkTest() throws VimDriverException {
    Network newNetwork = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
    Network newNetwork2 = dockerVim.createDockerNetwork(vimInstance, "MyNetwork2");
    List<String> exposedPortList1 = new ArrayList<>();
    exposedPortList1.add("8084");
    List<String> environmentVariables = new ArrayList<>();
    environmentVariables.add("port=8080");

    Server server =
        dockerVim.launchInstance(
            vimInstance, "MyContainer", "ubuntu:14.04", exposedPortList1, environmentVariables);

    List<String> exposedPortList2 = new ArrayList<>();
    exposedPortList2.add("8085");
    Server server2 =
        dockerVim.launchInstance(
            vimInstance, "MyContainer2", "ubuntu:14.04", exposedPortList2, environmentVariables);
    try {
      dockerVim.connectContainerToNetwork(vimInstance, server.getId(), newNetwork.getId());
      dockerVim.connectContainerToNetwork(vimInstance, server.getId(), newNetwork2.getId());
      dockerVim.connectContainerToNetwork(vimInstance, server2.getId(), newNetwork.getId());
      dockerVim.connectContainerToNetwork(vimInstance, server2.getId(), newNetwork2.getId());
    } catch (Exception e) {
      e.printStackTrace();
    }
    //System.out.println("newNetwork : " + newNetwork);
    //System.out.println("MyContainer : " + server);
    com.github.dockerjava.api.model.Network updatedDockerNetwork =
        dockerClient.inspectNetworkCmd().withNetworkId(newNetwork.getId()).exec();
    System.out.println("Updated docker Network: " + updatedDockerNetwork.getContainers());
    assertTrue(updatedDockerNetwork.getContainers().containsKey(server.getId()));
    InspectContainerResponse inspectContainerResponse =
        dockerClient.inspectContainerCmd(server.getId()).exec();
    assertNotNull(inspectContainerResponse.getNetworkSettings().getNetworks().get("MyNetwork"));

    /*com.github.dockerjava.api.model.Network.ContainerNetworkConfig containerNetworkConfig =
        updatedDockerNetwork.getContainers().get(server.getId());
    System.out.println("Server IP Address: " + containerNetworkConfig.getIpv4Address());
    System.out.println("Created network address : " + newNetwork.getSubnets().toArray());
    assertEquals(
        "Container IP should be the first address of the network",
        containerNetworkConfig.getIpv4Address(),
        "172.18.0.2/16");*/
  }

  @Ignore
  @Test
  public void disconnectContainerFromNetworkTest() throws VimDriverException {
    List<String> exposedPortList = new ArrayList<>();
    exposedPortList.add("8084");
    Network newNetwork = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
    List<String> environmentVariables = new ArrayList<>();
    environmentVariables.add("port=8080");

    Server server =
        dockerVim.launchInstance(
            vimInstance, "MyContainer", "ubuntu:14.04", exposedPortList, environmentVariables);
    try {
      dockerVim.connectContainerToNetwork(vimInstance, server.getId(), newNetwork.getId());
    } catch (Exception e) {
      //e.printStackTrace();
    }
    com.github.dockerjava.api.model.Network updatedDockerNetwork =
        dockerClient.inspectNetworkCmd().withNetworkId(newNetwork.getId()).exec();
    com.github.dockerjava.api.model.Network.ContainerNetworkConfig containerNetworkConfig =
        updatedDockerNetwork.getContainers().get(server.getId());
    try {
      dockerVim.disconnectContainerFromNetwork(vimInstance, server.getId(), newNetwork.getId());
    } catch (Exception e) {
      e.printStackTrace();
    }
    InspectContainerResponse inspectContainerResponse =
        dockerClient.inspectContainerCmd(server.getId()).exec();
    assertNull(inspectContainerResponse.getNetworkSettings().getNetworks().get("MyNetwork"));
    updatedDockerNetwork =
        dockerClient.inspectNetworkCmd().withNetworkId(newNetwork.getId()).exec();
    assertFalse(updatedDockerNetwork.getContainers().containsKey(server.getId()));
  }

  @Ignore
  @Test
  public void createVolumeTest() {
    String newVolume = "NewVolume";
    dockerVim.createVolume(vimInstance, newVolume);

    InspectVolumeResponse inspectVolumeResponse =
        (InspectVolumeResponse) dockerClient.inspectVolumeCmd(newVolume).exec();
    assertEquals("Verify the newly created volume", inspectVolumeResponse.getName(), newVolume);
  }

  @Ignore
  @Test(expected = NotFoundException.class)
  public void deleteVolumeTest() {
    String newVolume = "NewVolume";
    dockerVim.createVolume(vimInstance, newVolume);

    InspectVolumeResponse inspectVolumeResponse =
        (InspectVolumeResponse) dockerClient.inspectVolumeCmd(newVolume).exec();
    assertEquals("Verify the newly created volume", inspectVolumeResponse.getName(), newVolume);
    dockerVim.deleteVolume(vimInstance, newVolume);
    inspectVolumeResponse = (InspectVolumeResponse) dockerClient.inspectVolumeCmd(newVolume).exec();
    assertEquals("Verify the newly created volume", inspectVolumeResponse.getName(), newVolume);
  }

  @Ignore
  @Test
  public void copyArchiveToContainerTest() throws VimDriverException, IOException {
    List<String> exposedPortList = new ArrayList<>();
    exposedPortList.add("8084");
    Network newNetwork = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
    List<String> environmentVariables = new ArrayList<>();
    environmentVariables.add("port=8080");

    Server server =
        dockerVim.launchInstance(
            vimInstance, "MyContainer", "ubuntu:14.04", exposedPortList, environmentVariables);
    String currectDir = System.getProperties().getProperty("user.dir");
    String pathToArchive = currectDir + "/src/test/data";
    System.out.println(pathToArchive);

    //boolean success = dockerVim.copyArchiveToContainer(vimInstance, server.getId(), pathToArchive);
    assertTrue(dockerVim.copyArchiveToContainer(vimInstance, server.getId(), pathToArchive));
  }

  @Ignore
  @Test
  public void execCommandTest() throws InterruptedException, VimDriverException, IOException {
    List<String> exposedPortList = new ArrayList<>();
    exposedPortList.add("8084");
    Network newNetwork = dockerVim.createDockerNetwork(vimInstance, "MyNetwork");
    List<String> environmentVariables = new ArrayList<>();
    environmentVariables.add("port=8080");

    Server server =
        dockerVim.launchInstance(
            vimInstance, "MyContainer", "ubuntu:14.04", exposedPortList, environmentVariables);
    String currentDir = System.getProperties().getProperty("user.dir");
    String pathToArchive = currentDir + "/src/test/data";
    dockerVim.copyArchiveToContainer(vimInstance, server.getId(), pathToArchive);
    String param1 = "param1";
    assertTrue(dockerVim.execCommand(vimInstance, server.getId(), "/data/testScript.sh", param1));
  }

  @After
  public void after() throws VimDriverException {
    List<Server> servers = dockerVim.listServer(vimInstance);
    for (Server server : servers) {
      dockerVim.deleteServerByIdAndWait(vimInstance, server.getId());
    }

    List<Network> networks = dockerVim.listNetworks(vimInstance);
    for (Network network : networks) {
      if (network.getName().indexOf("host") < 0
          && network.getName().indexOf("bridge") < 0
          && network.getName().indexOf("none") < 0) {
        dockerVim.deleteNetwork(vimInstance, network.getId());
      }
    }

    ListVolumesResponse listVolumesResponse = dockerClient.listVolumesCmd().exec();
    if (listVolumesResponse.getVolumes() != null) {
      for (InspectVolumeResponse volume : listVolumesResponse.getVolumes()) {
        dockerVim.deleteVolume(vimInstance, volume.getName());
      }
    }
  }
}
