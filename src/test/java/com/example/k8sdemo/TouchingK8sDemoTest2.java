package com.example.k8sdemo;

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
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.Map;

public class TouchingK8sDemoTest2 {
    public static final int NODE_PORT = 30001;

    @Test
    public void myTest() throws IOException {
        K3sContainer<?> k8s = new K3sContainer<>();
        k8s.addExposedPorts(NODE_PORT); // 30001

        k8s.start();
        createNginxProxy(k8s);
        // obtain a kubeconfig file which allows us to connect to k3s
        String kubeConfigYaml = k8s.getKubeconfig();

        Config config = Config.fromKubeconfig(kubeConfigYaml);

        KubernetesClient client = new DefaultKubernetesClient(config);

        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("devnexus")
                .endMetadata().build();
        client.namespaces().create(ns);

        var selectors = Map.of("app", "devnexus");
        createDeployment(client, selectors);
        createService(client, selectors);

        // Don't do this in your tests!
        System.in.read();
    }

    private static void createService(KubernetesClient client, Map<String, String> selectors) {
        Service service = new ServiceBuilder()
                .withNewSpec()
                .addNewPort()
                .withName("http")
                .withNodePort(NODE_PORT)
                .withPort(80)
                .withTargetPort(new IntOrString(80))
                .endPort()
                .withSelector(selectors)
                .withType("NodePort")
                .endSpec()
                .withNewMetadata()
                .withName("devnexus")
                .endMetadata()
                .build();

        client.services().inNamespace("devnexus").create(service);
    }

    private static void createDeployment(KubernetesClient client, Map<String, String> selectors) {
        Deployment d = new DeploymentBuilder()
                .withNewMetadata()
                .withName("devnexus")
                .withLabels(selectors)
                .endMetadata()
                .withSpec(new DeploymentSpecBuilder()
                        .withReplicas(2)
                        .withTemplate(new PodTemplateSpecBuilder()
                                .withNewMetadata()
                                .withLabels(selectors)
                                .endMetadata()
                                .withNewSpec()
                                .addNewContainer()
                                .withName("nginx")
                                .withImage("nginx:1.23.1")
                                .addNewPort().withContainerPort(80).endPort()
                                .endContainer()
                                .endSpec()
                                .build())
                        .withNewSelector()
                        .withMatchLabels(selectors)
                        .endSelector()
                        .build())
                .build();

        client.apps().deployments().inNamespace("devnexus").create(d);
    }

    static TcpProxy tcpProxy;

    @AfterAll
    public static void stopProxy() {
        if (tcpProxy != null)
            tcpProxy.shutdown();
    }

    private static void createNginxProxy(GenericContainer<?> k8s) {
        StaticTcpProxyConfig config = new StaticTcpProxyConfig(
                8080,
                k8s.getHost(),
                k8s.getMappedPort(NODE_PORT)
        );
        config.setWorkerCount(1);
        tcpProxy = new TcpProxy(config);
        tcpProxy.start();
    }
}
