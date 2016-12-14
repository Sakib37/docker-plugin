/*
 * Copyright (c) 2016 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.openbaton.catalogue.mano.common.DeploymentFlavour;
import org.openbaton.catalogue.nfvo.NFVImage;
import org.openbaton.catalogue.nfvo.Network;
import org.openbaton.catalogue.nfvo.Quota;
import org.openbaton.catalogue.nfvo.Server;
import org.openbaton.catalogue.nfvo.Subnet;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.catalogue.security.Key;
import org.openbaton.exceptions.VimDriverException;
import org.openbaton.plugin.PluginStarter;
import org.openbaton.vim.drivers.interfaces.VimDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by gca on 24/08/16. */
public class DockerVim extends VimDriver {
  private static final Logger log = LoggerFactory.getLogger(DockerVim.class);

  public static void main(String[] args)
      throws NoSuchMethodException, IOException, InstantiationException, TimeoutException,
          IllegalAccessException, InvocationTargetException, InterruptedException {
    if (args.length <= 1)
      PluginStarter.registerPlugin(DockerVim.class, "docker", "localhost", 5672, 3);
    else
      PluginStarter.registerPlugin(
          DockerVim.class,
          args[0],
          args[1],
          Integer.parseInt(args[2]),
          Integer.parseInt(args[3]),
          args[4],
          args[5]);

    VimInstance vimInstance = new VimInstance();
    vimInstance.setAuthUrl("tcp://Thesis:2376");

    /*DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(vimInstance.getAuthUrl())
            .withDockerTlsVerify(true)
            .withDockerCertPath("/home/sakib/.docker")
            .build();
    DockerClient docker = DockerClientBuilder.getInstance(config).build();
    //System.out.println(docker);

    CreateContainerResponse container =
            dockerClient.createContainerCmd("sakib/iperf3")
            .withName("iperf_client")
            .withCmd(cmd)
            .exec();
    dockerClient.startContainerCmd(container.getId()).exec();
    //Thread.sleep((long) 12000.0);
    dockerClient.startContainerCmd(container.getId()).exec();

    try {
        List<NFVImage> images = dockerVim.listImages(vimInstance);
        for (NFVImage image : images) {
            System.out.println(image);
        }
    } catch (VimDriverException e) {
        e.printStackTrace();
    }*/
  }

  private DockerClient createClient(String endpoint) {
    DockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(endpoint)
            .withDockerTlsVerify(true)
            .withDockerCertPath("/home/sakib/.docker")
            .build();
    return DockerClientBuilder.getInstance(config).build();
  }

  @Override
  public Server launchInstance(
      VimInstance vimInstance,
      String name,
      String image,
      String flavor,
      String keypair,
      Set<String> network,
      Set<String> secGroup,
      String userData)
      throws VimDriverException {
    return null;
  }

  public Server launchInstance(VimInstance vimInstance, String containerName, String imageName)
      throws VimDriverException {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    String containerHostname = "Openbaton";
    String cmd = "top";
    /*List<String> cmd = new ArrayList<String>();
    cmd.add("sleep");
    cmd.add("99999");*/
    if (!imageExist(vimInstance, imageName)) {
      log.info("Image " + imageName + " currently not available. Pulling " + imageName);
      pullImage(vimInstance, imageName);
    }

    CreateContainerResponse container =
        dockerClient
            .createContainerCmd(imageName)
            .withHostName(containerHostname)
            .withName(containerName)
            .withUser("root")
            .withCmd(cmd)
            .exec();

    //System.out.println("Created container :  " + container);
    log.debug("Instance created successfully");
    dockerClient.startContainerCmd(container.getId()).exec();
    //System.out.println("Created container Image :  " + );

    Server server =
        convertContainerToServer(
            vimInstance, container, containerName, containerHostname, imageName);
    return server;
  }

  @Override
  public Server launchInstanceAndWait(
      VimInstance vimInstance,
      String hostname,
      String image,
      String extId,
      String keyPair,
      Set<String> networks,
      Set<String> securityGroups,
      String s,
      Map<String, String> floatingIps,
      Set<Key> keys)
      throws VimDriverException {
    return null;
  }

  @Override
  public Server launchInstanceAndWait(
      VimInstance vimInstance,
      String name,
      String image,
      String extId,
      String keyPair,
      Set<String> networks,
      Set<String> securityGroups,
      String s)
      throws VimDriverException {
    return null;
  }

  public Server launchInstanceAndWait(
      VimInstance vimInstance, String name, String image, String hostName)
      throws VimDriverException {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());

    CreateContainerResponse container =
        dockerClient.createContainerCmd(image).withHostName(hostName).withName(name).exec();

    log.info("Container created successfully");
    dockerClient.startContainerCmd(container.getId()).exec();
    dockerClient
        .waitContainerCmd(container.getId())
        .exec(new WaitContainerResultCallback())
        .awaitStatusCode();

    Server server = convertContainerToServer(vimInstance, container, name, hostName, image);
    return server;
  }

  private Server convertContainerToServer(
      VimInstance vimInstance,
      CreateContainerResponse container,
      String containerName,
      String containerHostname,
      String imageName) {
    Server server = new Server();
    server.setId(container.getId());
    server.setName(containerName);
    server.setHostName(containerHostname);
    server.setImage(
        convertDockerImageToNfvImage(convertStringToImageObject(vimInstance, imageName)));
    return server;
  }

  private Image convertStringToImageObject(VimInstance vimInstance, String imageName) {
    Image image = null;
    List<Image> dockerImages =
        this.createClient(vimInstance.getAuthUrl()).listImagesCmd().withShowAll(true).exec();
    for (Image containerImage : dockerImages) {
      System.out.println("Finding launched container Image: " + containerImage.getRepoTags()[0]);
      //System.out.println("Image: " + imageName);
      System.out.println(
          "Image without tag" + imageName.equals(containerImage.getRepoTags()[0].split(":")[0]));
      //System.out.println(imageName.equals(containerImage.getRepoTags()));
      if (containerImage.getRepoTags()[0].equals(imageName)
          || containerImage.getRepoTags()[0].split(":")[0].equals(imageName)) {
        image = containerImage;
        break;
      }
    }
    return image;
  }

  private NFVImage convertDockerImageToNfvImage(Image dockerImage) {
    NFVImage nfvImage = new NFVImage();
    nfvImage.setName(String.valueOf(dockerImage.getRepoTags()[0]));
    nfvImage.setId(dockerImage.getId());
    nfvImage.setContainerFormat("docker");
    nfvImage.setCreated(new Date(dockerImage.getCreated()));
    nfvImage.setIsPublic(true);
    log.debug("Found a docker image, transformed into a NFV Image " + nfvImage);
    return nfvImage;
  }

  @Override
  public List<NFVImage> listImages(VimInstance vimInstance) throws VimDriverException {
    List<NFVImage> images = new ArrayList<>();
    DockerClient docker = this.createClient(vimInstance.getAuthUrl());
    List<Image> dockerImages = docker.listImagesCmd().withShowAll(true).exec();
    for (Image image : dockerImages) {
      // System.out.println("Image Name:" + image.getRepoTags()[0].split(":")[0]);
      NFVImage nfvImage = convertDockerImageToNfvImage(image);
      images.add(nfvImage);
    }
    return images;
  }

  @Override
  public List<Server> listServer(VimInstance vimInstance) throws VimDriverException {
    List<Server> servers = new ArrayList<>();
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
    if (containers.size() > 0) {
      for (Container container : containers) {
        Server server = new Server();
        server.setId(container.getId());
        server.setName(container.getNames()[0].substring(1));
        //System.out.println("Container image" + container.getImage());
        server.setImage(
            convertDockerImageToNfvImage(
                convertStringToImageObject(vimInstance, container.getImage())));
        servers.add(server);
      }
    }
    return servers;
  }

  @Override
  public List<Network> listNetworks(VimInstance vimInstance) throws VimDriverException {
    List<Network> networks = new ArrayList<>();
    DockerClient docker = this.createClient(vimInstance.getAuthUrl());
    //DockerClient docker = DockerClientBuilder.getInstance("http://127.0.0.1:2375").build();
    List<com.github.dockerjava.api.model.Network> dockerNetworks = docker.listNetworksCmd().exec();
    for (com.github.dockerjava.api.model.Network dockerNetwork : dockerNetworks) {
      /*
       * Default docker networks cannot be converted into NfvNetwork object using
       * 'convertDockerNetworkToNfvNetwork()' method. So they are filtered here while
       * listing networks. Otherwise 'convertDockerNetworkToNfvNetwork()' throws NoSuchElement exception*/
      if (dockerNetwork.getName().indexOf("host") < 0
          && dockerNetwork.getName().indexOf("bridge") < 0
          && dockerNetwork.getName().indexOf("none") < 0) {
        Network net;
        net = convertDockerNetworkToNfvNetwork(dockerNetwork);
        log.debug("Found a docker network, transformed into a NFV network " + net);
        networks.add(net);
      }
    }
    return networks;
  }

  @Override
  public List<DeploymentFlavour> listFlavors(VimInstance vimInstance) throws VimDriverException {
    // flavors don't exist in docker - invalid method
    List<DeploymentFlavour> dummyFlavoursList = new ArrayList<>();
    DeploymentFlavour dummyFlavour = new DeploymentFlavour();
    dummyFlavour.setVersion(1);
    dummyFlavour.setDisk(1);
    dummyFlavour.setFlavour_key("m1.small");
    dummyFlavour.setVcpus(1);
    dummyFlavour.setRam(1);
    dummyFlavoursList.add(dummyFlavour);
    return dummyFlavoursList;
  }

  @Override
  public void deleteServerByIdAndWait(VimInstance vimInstance, String id)
      throws VimDriverException {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    //dockerClient.startContainerCmd(id).exec();
    //dockerClient.waitContainerCmd(id).exec(new WaitContainerResultCallback()).awaitStatusCode();
    //dockerClient.stopContainerCmd(id).withTimeout(2).exec();
    dockerClient.removeContainerCmd(id).withForce(true).exec();
    log.debug("Server deleted successfully");
  }

  @Override
  public Network createNetwork(VimInstance vimInstance, Network network) throws VimDriverException {
    return null;
  }

  public Network createNetwork(
      VimInstance vimInstance, String networkName, String subnet, String gateway, String ipRange)
      throws VimDriverException, DockerException {
    networkName = networkName.replaceAll(" ", "");
    subnet = subnet.replaceAll(" ", "");
    gateway = gateway.replaceAll(" ", "");
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    com.github.dockerjava.api.model.Network.Ipam ipam;
    if (ipRange != null) {
      ipRange = ipRange.replaceAll(" ", "");
      ipam =
          new com.github.dockerjava.api.model.Network.Ipam()
              .withConfig(
                  new com.github.dockerjava.api.model.Network.Ipam.Config()
                      .withSubnet(subnet)
                      .withGateway(gateway)
                      .withIpRange(ipRange));
    } else {
      ipam =
          new com.github.dockerjava.api.model.Network.Ipam()
              .withConfig(
                  new com.github.dockerjava.api.model.Network.Ipam.Config()
                      .withSubnet(subnet)
                      .withGateway(gateway));
    }
    CreateNetworkResponse createNetworkResponse =
        dockerClient
            .createNetworkCmd()
            .withName(networkName)
            .withCheckDuplicate(true)
            .withIpam(ipam)
            .exec();
    com.github.dockerjava.api.model.Network dockerNetwork =
        dockerClient.inspectNetworkCmd().withNetworkId(createNetworkResponse.getId()).exec();
    Network nfvNetwork = convertDockerNetworkToNfvNetwork(dockerNetwork);
    log.debug("Network successfully created");
    // System.out.println("NFV Network" + nfvNetwork);
    return nfvNetwork;
  }

  private Network convertDockerNetworkToNfvNetwork(
      com.github.dockerjava.api.model.Network dockerNetwork) {
    Network nfvNetwork = new Network();
    nfvNetwork.setName(dockerNetwork.getName());
    nfvNetwork.setId(dockerNetwork.getId());
    Subnet subnetElement = new Subnet();
    subnetElement.setNetworkId(dockerNetwork.getIpam().getConfig().iterator().next().getSubnet());
    subnetElement.setGatewayIp(dockerNetwork.getIpam().getConfig().iterator().next().getGateway());
    subnetElement.setName(dockerNetwork.getDriver());
    Set<Subnet> subnet = new HashSet<>();
    subnet.add(subnetElement);
    nfvNetwork.setSubnets(subnet);
    return nfvNetwork;
  }

  @Override
  public DeploymentFlavour addFlavor(VimInstance vimInstance, DeploymentFlavour deploymentFlavour)
      throws VimDriverException {
    return null;
  }

  @Override
  public NFVImage addImage(VimInstance vimInstance, NFVImage image, byte[] imageFile)
      throws VimDriverException {
    return null;
  }

  @Override
  public NFVImage addImage(VimInstance vimInstance, NFVImage image, String image_url)
      throws VimDriverException {
    return null;
  }

  public NFVImage pullImage(VimInstance vimInstance, String imageName) throws VimDriverException {
    NFVImage nfvImage = new NFVImage();
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback()).awaitSuccess();

    List<Image> dockerImages = dockerClient.listImagesCmd().withShowAll(true).exec();
    for (Image currImage : dockerImages) {
      //System.out.println("Current Image: " + currImage.getRepoTags()[0].split(":")[0]);
      String currentImage = currImage.getRepoTags()[0];
      // System.out.println("CURRENT IMAGE: " + currentImage);
      if (imageName.equals(currentImage)) {
        Image dockerImage = convertStringToImageObject(vimInstance, currentImage);
        nfvImage = convertDockerImageToNfvImage(dockerImage);
        System.out.println("NFV image id: " + nfvImage.getId().getClass());
        // System.out.println("NFV Image: " + nfvImage);
        break;
      }
    }
    return nfvImage;
  }

  public boolean imageExist(VimInstance vimInstance, String imageName) {
    DockerClient docker = this.createClient(vimInstance.getAuthUrl());
    List<Image> dockerImages = docker.listImagesCmd().withShowAll(true).exec();
    for (Image dockerImage : dockerImages) {
      if (dockerImage.getRepoTags()[0].equals(imageName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public NFVImage updateImage(VimInstance vimInstance, NFVImage image) throws VimDriverException {
    return null;
  }

  @Override
  public NFVImage copyImage(VimInstance vimInstance, NFVImage image, byte[] imageFile)
      throws VimDriverException {
    return null;
  }

  @Override
  public boolean deleteImage(VimInstance vimInstance, NFVImage image) throws VimDriverException {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    try {
      System.out.println(image.getId());
      dockerClient.removeImageCmd(image.getId()).exec();
      // System.out.println("Image deleted");
    } catch (Exception e) {
      log.debug(e.toString());
    }
    return false;
  }

  @Override
  public DeploymentFlavour updateFlavor(
      VimInstance vimInstance, DeploymentFlavour deploymentFlavour) throws VimDriverException {
    return null;
  }

  @Override
  public boolean deleteFlavor(VimInstance vimInstance, String extId) throws VimDriverException {
    return false;
  }

  @Override
  public Subnet createSubnet(VimInstance vimInstance, Network createdNetwork, Subnet subnet)
      throws VimDriverException {
    return null;
  }

  @Override
  public Network updateNetwork(VimInstance vimInstance, Network network) throws VimDriverException {
    return null;
  }

  @Override
  public Subnet updateSubnet(VimInstance vimInstance, Network updatedNetwork, Subnet subnet)
      throws VimDriverException {
    return null;
  }

  @Override
  public List<String> getSubnetsExtIds(VimInstance vimInstance, String network_extId)
      throws VimDriverException {
    return null;
  }

  @Override
  public boolean deleteSubnet(VimInstance vimInstance, String existingSubnetExtId)
      throws VimDriverException {
    return false;
  }

  @Override
  public boolean deleteNetwork(VimInstance vimInstance, String Id) throws VimDriverException {
    try {
      DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
      dockerClient.removeNetworkCmd(Id).exec();
      log.debug("Network deleted successfully");
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Network getNetworkById(VimInstance vimInstance, String id) throws VimDriverException {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    com.github.dockerjava.api.model.Network dockerNetwork =
        dockerClient.inspectNetworkCmd().withNetworkId(id).exec();
    Network nfvNetwork = convertDockerNetworkToNfvNetwork(dockerNetwork);
    return nfvNetwork;
  }

  @Override
  public Quota getQuota(VimInstance vimInstance) throws VimDriverException {
    Quota dummyQuaota = new Quota();
    dummyQuaota.setId("dummy");
    dummyQuaota.setVersion(1);
    dummyQuaota.setCores(1);
    dummyQuaota.setRam(1);
    dummyQuaota.setKeyPairs(1);
    return dummyQuaota;
  }

  @Override
  public String getType(VimInstance vimInstance) throws VimDriverException {
    return "docker";
  }

  public void connectContainerToNetwork(
      VimInstance vimInstance, String containerId, String networkId) throws VimDriverException {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    dockerClient.startContainerCmd(containerId);
    dockerClient
        .connectToNetworkCmd()
        .withNetworkId(networkId)
        .withContainerId(containerId)
        .withContainerNetwork(
            new ContainerNetwork()
                .withIpamConfig(new ContainerNetwork.Ipam())
                .withIpv4Address("172.19.0.2"))
        .exec();
    com.github.dockerjava.api.model.Network updatedNetwork =
        dockerClient.inspectNetworkCmd().withNetworkId(networkId).exec();

    com.github.dockerjava.api.model.Network.ContainerNetworkConfig containerNetworkConfig =
        updatedNetwork.getContainers().get(containerId);
    log.info("Container connected to the Network Successfully");
  }

  public void disconnectContainerFromNetwork(
      VimInstance vimInstance, String containerId, String networkId) throws VimDriverException {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    dockerClient
        .disconnectFromNetworkCmd()
        .withNetworkId(networkId)
        .withContainerId(containerId)
        .exec();
    log.info("Container disconnected to the Network Successfully");
  }

  public void createVolume(VimInstance vimInstance, String volumeName) {
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    CreateVolumeResponse createVolumeResponse =
        dockerClient.createVolumeCmd().withName(volumeName).withDriver("local").exec();

    //System.out.println("Volume Name: " + createVolumeResponse.getName());
    //System.out.println("Volume Driver: " + createVolumeResponse.getDriver());
    //System.out.println("volume in mountpoint : " + createVolumeResponse.getMountpoint());
  }

  public void deleteVolume(VimInstance vimInstance, String volumeName) {
    try {
      DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
      dockerClient.removeVolumeCmd(volumeName).exec();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean copyArchiveToContainer(
      VimInstance vimInstance, String containerId, String pathToarchive) throws IOException {
    Boolean success = false;
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    File scriptsFolder = new File(pathToarchive);
    File[] scripts = scriptsFolder.listFiles();
    for (File script : scripts) {
      if (script.isFile()) {
        if (script.getName().substring(script.getName().length() - 3).equals(".sh")
            || script.getName().substring(script.getName().length() - 3).equals(".py")) {
          script.setExecutable(true, false);
        }
      }
    }
    dockerClient.startContainerCmd(containerId).exec();
    try {
      dockerClient
          .copyArchiveToContainerCmd(containerId)
          .withHostResource(pathToarchive)
          .withRemotePath("/")
          .exec();
      success = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return success;
  }

  public boolean execCommand(VimInstance vimInstance, String containerId, String... scriptCmd)
      throws InterruptedException {
    boolean success = false;
    DockerClient dockerClient = this.createClient(vimInstance.getAuthUrl());
    dockerClient.startContainerCmd(containerId).exec();

    try {
      ExecCreateCmdResponse execCreateCmdResponse =
          dockerClient.execCreateCmd(containerId).withCmd("touch", "file.log").exec();

      /*ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
      .withAttachStdout(true)
      .withCmd(scriptCmd)
      .withUser("root")
      .exec();*/

      dockerClient
          .execStartCmd(execCreateCmdResponse.getId())
          .exec(new ExecStartResultCallback(System.out, System.err))
          .awaitCompletion();
      System.out.println("Exec response: " + execCreateCmdResponse);
      success = true;
      log.info("Command run successfully in Container", containerId);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Success in exec method: " + success);
    return success;
  }
}
