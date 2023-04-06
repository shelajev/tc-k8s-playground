package com.example.k8sdemo;

import com.dajudge.kindcontainer.K3sContainer;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class TouchingK8sDemoTest1 {
    private static final Logger log = LoggerFactory.getLogger(TouchingK8sDemoTest1.class);

    @Test
    public void myTest() throws IOException {
        K3sContainer<?> k8s = new K3sContainer<>();

        k8s.start();

        // obtain a kubeconfig file which allows us to connect to k3s
        String kubeConfigYaml = k8s.getKubeconfig();

        Config config = Config.fromKubeconfig(kubeConfigYaml);

        KubernetesClient client = new DefaultKubernetesClient(config);

        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("devnexus")
                .endMetadata().build();
        client.namespaces().create(ns);

        var selectors = Map.of("app", "devnexus");
        createDeployment(client, selectors);

        // Don't do this in your tests!
        System.in.read();
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
}
