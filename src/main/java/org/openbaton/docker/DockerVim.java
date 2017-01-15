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
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

/** Created by sakib on 24/08/16. */
public class DockerVim extends VimDriver {
  private static final Logger log = LoggerFactory.getLogger(DockerVim.class);

  private String dockerCertPath = properties.getProperty("docker.cert.path");
  private String vimInstanceAuthUrl = properties.getProperty("vim.instance.auth.url");
  private boolean tlsVerify = Boolean.parseBoolean(properties.getProperty("tls.verify"));
  private String type = properties.getProperty("type");

  private DockerClient dockerClient;
  private VimInstance vimInstance;

  public DockerVim() {
    vimInstance = new VimInstance();
    vimInstance.setAuthUrl(vimInstanceAuthUrl);
    dockerClient = createClient();
  }

  public DockerClient getDockerClient() {
    return this.dockerClient;
  }

  public VimInstance getVimInstance() {
    return this.vimInstance;
  }

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
  }

  public DockerClient createClient() {
    DockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(vimInstanceAuthUrl)
            .withDockerTlsVerify(tlsVerify)
            .withDockerCertPath(dockerCertPath)
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

  public Server launchInstance(
      VimInstance vimInstance,
      String containerName,
      String imageName,
      List<String> exposedPortsToHost,
      List<String> environmentVariables)
      throws VimDriverException {
    String containerHostname = containerName;
    environmentVariables.add("TERM=xterm");
    List<String> cmd = new ArrayList<>();
    cmd.add("sleep");
    cmd.add("99999");

    if (!imageExist(imageName)) {
      log.info("Image " + imageName + " currently not available. Pulling " + imageName);
      pullImage(vimInstance, imageName);
    }

    // Binding ports
    Ports portBindings = new Ports();
    List<ExposedPort> exposedPortList = new ArrayList<>();
    List<Integer> portNumbers = new ArrayList<>();
    for (String exposedPort : exposedPortsToHost) {
      int portNumber = Integer.parseInt(exposedPort);
      if (portNumbers.contains(portNumber)) {
        exposedPortList.add(new ExposedPort(portNumber, InternetProtocol.UDP));
        portBindings.bind(ExposedPort.udp(portNumber), Ports.Binding.bindPort(portNumber));
      } else {
        // In ExposedPort DEFAULT protocol type is TCP
        exposedPortList.add(new ExposedPort(portNumber));
        portNumbers.add(portNumber);
        portBindings.bind(ExposedPort.tcp(portNumber), Ports.Binding.bindPort(portNumber));
      }
    }

    CreateContainerResponse container =
        dockerClient
            .createContainerCmd(imageName)
            .withEnv(environmentVariables)
            .withHostName(containerHostname)
            .withName(containerName)
            .withCmd(cmd)
            .withExposedPorts(exposedPortList)
            .withPortBindings(portBindings)
            //.withUser("root")
            .exec();

    log.debug("Instance created successfully");
    dockerClient.startContainerCmd(container.getId()).exec();

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

  public Server launchInstanceAndWait(VimInstance vimInstance, String name, String image)
      throws VimDriverException {
    String hostName = "Openbaton";
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
    InspectContainerResponse inspectContainerResponse =
        dockerClient.inspectContainerCmd(container.getId()).exec();
    server.setName(containerName);
    server.setHostName(containerHostname);
    server.setImage(
        convertDockerImageToNfvImage(convertStringToImageObject(vimInstance, imageName)));

    server.setIps(getNfvIps(inspectContainerResponse));

    server = setServerCreationDate(server, inspectContainerResponse);
    server.setStatus(inspectContainerResponse.getState().getStatus());

    return server;
  }

  private Map<String, List<String>> getNfvIps(InspectContainerResponse inspectContainerResponse) {
    Map<String, List<String>> ips = new HashMap<>();
    Map<String, ContainerNetwork> dockerNetworks;
    dockerNetworks = inspectContainerResponse.getNetworkSettings().getNetworks();
    for (String s : dockerNetworks.keySet()) {
      List<String> addresses = new ArrayList<>();
      addresses.add(dockerNetworks.get(s).getIpAddress());
      ips.put(s, addresses);
    }
    return ips;
  }

  private Server setServerCreationDate(
      Server server, InspectContainerResponse inspectContainerResponse) {
    String dockerCreationDate = inspectContainerResponse.getCreated();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    Date serverCreationDate = new Date();
    try {
      serverCreationDate = dateFormat.parse(dockerCreationDate);
    } catch (Exception e) {
      log.debug(
          "Error occured while converting container creation date to NFV Server creation date");
    }
    server.setCreated(serverCreationDate);
    return server;
  }

  public void restartServer(VimInstance vimInstance, String serverId) {
    try {
      dockerClient.restartContainerCmd(serverId).withtTimeout(1).exec();
    } catch (Exception e) {
      log.debug(e.toString());
    }
  }

  private Image convertStringToImageObject(VimInstance vimInstance, String imageName) {
    Image image = null;
    List<Image> dockerImages = dockerClient.listImagesCmd().withShowAll(true).exec();
    for (Image containerImage : dockerImages) {
      //System.out.println("Image without tag" +
      // imageName.equals(containerImage.getRepoTags()[0].split(":")[0]));
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
    List<Image> dockerImages = dockerClient.listImagesCmd().withShowAll(true).exec();
    for (Image image : dockerImages) {
      // System.out.println("Image Name:" + image.getRepoTags()[0].split(":")[0]);
      NFVImage nfvImage = convertDockerImageToNfvImage(image);
      images.add(nfvImage);
    }
    return images;
  }

  @Override
  public List<Server> listServer(VimInstance vimInstance) throws VimDriverException {
    // This update all the Servers with current condition and networks of the container
    List<Server> servers = new ArrayList<>();
    List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
    if (containers.size() > 0) {
      for (Container container : containers) {
        Server server = new Server();
        server.setId(container.getId());
        server.setName(container.getNames()[0].substring(1));
        server.setImage(
            convertDockerImageToNfvImage(
                convertStringToImageObject(vimInstance, container.getImage())));
        InspectContainerResponse inspectContainerResponse =
            dockerClient.inspectContainerCmd(container.getId()).exec();
        server.setIps(getNfvIps(inspectContainerResponse));
        server.setStatus(inspectContainerResponse.getState().getStatus());
        server = setServerCreationDate(server, inspectContainerResponse);
        servers.add(server);
      }
    }
    return servers;
  }

  @Override
  public List<Network> listNetworks(VimInstance vimInstance) throws VimDriverException {
    List<Network> networks = new ArrayList<>();
    List<com.github.dockerjava.api.model.Network> dockerNetworks =
        dockerClient.listNetworksCmd().exec();
    for (com.github.dockerjava.api.model.Network dockerNetwork : dockerNetworks) {
      /*
       * Default docker networks cannot be converted into NfvNetwork object using
       * 'convertDockerNetworkToNfvNetwork()' method. So they are filtered here while
       * listing networks. Otherwise 'convertDockerNetworkToNfvNetwork()'
       * throws NoSuchElement exception*/
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
    dockerClient.removeContainerCmd(id).withForce(true).exec();
    log.debug("Server deleted successfully");
  }

  @Override
  public Network createNetwork(VimInstance vimInstance, Network networkName)
      throws VimDriverException {
    return null;
  }

  public Network createDockerNetwork(VimInstance vimInstance, String networkName)
      throws VimDriverException, DockerException {
    networkName = networkName.replaceAll(" ", "");
    CreateNetworkResponse createNetworkResponse =
        dockerClient.createNetworkCmd().withName(networkName).withCheckDuplicate(true).exec();
    com.github.dockerjava.api.model.Network dockerNetwork =
        dockerClient.inspectNetworkCmd().withNetworkId(createNetworkResponse.getId()).exec();
    Network nfvNetwork = convertDockerNetworkToNfvNetwork(dockerNetwork);
    log.debug("Network successfully created");
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
    dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback()).awaitSuccess();

    List<Image> dockerImages = dockerClient.listImagesCmd().withShowAll(true).exec();
    for (Image currImage : dockerImages) {
      //System.out.println("Current Image: " + currImage.getRepoTags()[0].split(":")[0]);
      String currentImage = currImage.getRepoTags()[0];
      if (imageName.equals(currentImage)) {
        Image dockerImage = convertStringToImageObject(vimInstance, currentImage);
        nfvImage = convertDockerImageToNfvImage(dockerImage);
        break;
      }
    }
    return nfvImage;
  }

  private boolean imageExist(String imageName) {
    List<Image> dockerImages = dockerClient.listImagesCmd().withShowAll(true).exec();
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
    try {
      dockerClient.removeImageCmd(image.getId()).exec();
      log.info("Image '" + image + "' deleted successfully");
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
  public boolean deleteNetwork(VimInstance vimInstance, String networkId)
      throws VimDriverException {
    boolean success;
    try {
      dockerClient.removeNetworkCmd(networkId).exec();
      log.info("Network '" + networkId + "' deleted successfully");
      success = true;
    } catch (Exception e) {
      success = false;
    }
    return success;
  }

  @Override
  public Network getNetworkById(VimInstance vimInstance, String id) throws VimDriverException {
    Network nfvNetwork = null;
    try {
      com.github.dockerjava.api.model.Network dockerNetwork =
          dockerClient.inspectNetworkCmd().withNetworkId(id).exec();
      nfvNetwork = convertDockerNetworkToNfvNetwork(dockerNetwork);
    } catch (Exception e) {
      log.debug(e.toString());
    }
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
    try {
      dockerClient
          .connectToNetworkCmd()
          .withNetworkId(networkId)
          .withContainerId(containerId)
          .withContainerNetwork(new ContainerNetwork().withIpamConfig(new ContainerNetwork.Ipam()))
          .exec();
      log.info("Container connected to the Network Successfully");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void disconnectContainerFromNetwork(
      VimInstance vimInstance, String containerId, String networkId) throws VimDriverException {
    try {
      dockerClient
          .disconnectFromNetworkCmd()
          .withNetworkId(networkId)
          .withContainerId(containerId)
          .exec();
      log.info("Container disconnected to the Network Successfully");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void createVolume(VimInstance vimInstance, String volumeName) {
    try {
      CreateVolumeResponse createVolumeResponse =
          dockerClient.createVolumeCmd().withName(volumeName).withDriver("local").exec();
      log.info("Volume '" + volumeName + "' created successfully");
    } catch (Exception e) {
      e.printStackTrace();
    }
    //System.out.println("Volume Name: " + createVolumeResponse.getName());
  }

  public void deleteVolume(VimInstance vimInstance, String volumeName) {
    try {
      dockerClient.removeVolumeCmd(volumeName).exec();
      log.info("Volume '" + volumeName + "' deleted successfully");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean copyArchiveToContainer(
      VimInstance vimInstance, String containerId, String pathToarchive, String remotePath)
      throws IOException {
    Boolean success = false;
    File scriptsFolder = new File(pathToarchive);
    File[] scripts = scriptsFolder.listFiles();
    if (scripts != null) {
      for (File script : scripts) {
        if (script.isFile()) {
          if (script.getName().substring(script.getName().length() - 3).equals(".sh")
              || script.getName().substring(script.getName().length() - 3).equals(".py")) {
            script.setExecutable(true, false);
          }
        }
      }
    } else {
      File script = new File(pathToarchive);
      if (script.getName().substring(script.getName().length() - 3).equals(".sh")
          || script.getName().substring(script.getName().length() - 3).equals(".py")) {
        script.setExecutable(true, false);
      }
    }

    if (remotePath == null) {
      remotePath = "/";
    }

    try {
      dockerClient
          .copyArchiveToContainerCmd(containerId)
          .withHostResource(pathToarchive)
          .withRemotePath(remotePath)
          .exec();
      log.info("Archive '" + pathToarchive + "' copied successfully to container '" + containerId);
      success = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    clearHostMachine();
    return success;
  }

  public boolean copyArchiveFromContainer(
      VimInstance vimInstance, String containerId, String pathToArchive, String hostPath)
      throws IOException {
    Boolean success = false;

    try {
      InputStream response =
          dockerClient
              .copyArchiveFromContainerCmd(containerId, pathToArchive)
              .withHostPath(hostPath)
              .exec();
      log.info("Archive '" + pathToArchive + "' is copied successfully");
      success = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    clearHostMachine();
    return success;
  }

  public boolean setEnvironmentVariable(
      VimInstance vimInstance, String containerID, List<String> envVariables) throws IOException {
    String variables = "";
    boolean success = false;
    for (String envVariable : envVariables) {
      variables += (envVariable + "\n");
    }
    String scriptContent =
        "#!/bin/bash \n\n"
            + "echo \""
            + variables
            + "\" >> /etc/bash.bashrc \n"
            + "echo \""
            + variables
            + "\" >> /root/.bashrc \n"
            + "source /etc/bash.bashrc\n"
            + "source /root/.bashrc\n";
    File script = new File("/tmp/setVnfrEnvironemnt.sh");
    try (Writer writer = new BufferedWriter(new FileWriter(script))) {
      writer.write(scriptContent);
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      copyArchiveToContainer(vimInstance, containerID, "/tmp/setVnfrEnvironemnt.sh", "/");
      execCommand(vimInstance, containerID, "/setVnfrEnvironemnt.sh");
      restartServer(vimInstance, containerID);
      success = true;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    script.delete();
    return success;
  }

  public Map<String, String> getEnvVariableFromContainer(
      VimInstance vimInstance, String containerID) throws IOException {
    Map<String, String> envVariables = new HashMap<>();
    String scriptContent = "#!/bin/bash \n\n" + "set > /envVariablesList";
    File script = new File("/tmp/getEnvVariables.sh");
    try (Writer writer = new BufferedWriter(new FileWriter(script))) {
      writer.write(scriptContent);
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      copyArchiveToContainer(vimInstance, containerID, "/tmp/getEnvVariables.sh", "/");
      execCommand(vimInstance, containerID, "/getEnvVariables.sh");
      ProcessBuilder getEnv =
          new ProcessBuilder(
              "/bin/bash",
              "-c",
              "docker cp "
                  + containerID
                  + ":/envVariablesList"
                  + " /tmp/openbaton/dockerVNFM/currentEnvList");
      Process execute = null;
      int exitStatus = -1;
      try {
        execute = getEnv.redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        exitStatus = execute.waitFor();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    script.delete();
    return envVariables;
  }

  public boolean execCommand(VimInstance vimInstance, String containerId, String... scriptCmd)
      throws InterruptedException {
    boolean success = false;

    try {
      ExecCreateCmdResponse execCreateCmdResponse =
          dockerClient
              .execCreateCmd(containerId)
              .withAttachStdin(true)
              .withAttachStdout(true)
              .withTty(true)
              .withCmd(scriptCmd)
              .withUser("root")
              .exec();

      dockerClient
          .execStartCmd(execCreateCmdResponse.getId())
          .exec(new ExecStartResultCallback(System.out, System.err))
          .awaitCompletion();
      success = true;
      log.info("Command run successfully in Container", containerId);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return success;
  }

  private void clearHostMachine() {
    ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "rm /tmp/docker-java*");
    Process execute = null;
    int exitStatus = -1;
    try {
      execute = pb.redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
      exitStatus = execute.waitFor();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    if (exitStatus == 0) {
      log.info("Successfully cleared gurbage files in /tmp directory in host machine");
    }
  }
}
