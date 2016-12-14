package org.openbaton.docker;

import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.exceptions.VimDriverException;

/** Created by sakib on 11/6/16. */
public class TestScript {

  public static void main(String[] args) throws VimDriverException, InterruptedException {
    VimInstance vimInstance = new VimInstance();
    DockerVim dockerVim = new DockerVim();
    vimInstance.setAuthUrl("tcp://localhost:2375");
    //Server iperf3_server = dockerVim.launchInstance(vimInstance, "MyContainer", "hostName", "ubuntu", cmd);
    //String serverScript = "/openbaton/script/" + "iperf3_server.sh";
    //dockerVim.execScript(vimInstance, "server", serverScript);
    String scriptName = "/openbaton/script/" + "iperf3_client.sh";
    dockerVim.execCommand(vimInstance, "client", scriptName, "172.17.0.2");
  }
}
