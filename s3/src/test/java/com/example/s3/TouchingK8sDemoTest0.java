package com.example.s3;

import com.dajudge.kindcontainer.K3sContainer;
import com.github.terma.javaniotcpproxy.StaticTcpProxyConfig;
import com.github.terma.javaniotcpproxy.TcpProxy;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.util.Map;

public class TouchingK8sDemoTest0 {
    private static final Logger log = LoggerFactory.getLogger(TouchingK8sDemoTest0.class);

    @Test
    public void myTest() throws IOException {
        K3sContainer<?> k8s = new K3sContainer<>()
                .withLogConsumer(new Slf4jLogConsumer(log));

        k8s.start();

        // obtain a kubeconfig file which allows us to connect to k3s
        String kubeConfigYaml = k8s.getKubeconfig();

        Config config = Config.fromKubeconfig(kubeConfigYaml);

        KubernetesClient client = new DefaultKubernetesClient(config);

        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("alex")
                .endMetadata().build();
        client.namespaces().create(ns);

        // Don't do this in your tests!
        System.in.read();
    }

}
