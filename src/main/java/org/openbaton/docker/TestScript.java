package org.openbaton.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import org.openbaton.catalogue.nfvo.Server;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.exceptions.VimDriverException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sakib on 11/6/16.
 */
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
