package com.example.k8sdemo;

import com.dajudge.kindcontainer.K3sContainer;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TouchingK8sDemoTest0 {

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

        // Don't do this in your tests!
        System.in.read();
    }

}
