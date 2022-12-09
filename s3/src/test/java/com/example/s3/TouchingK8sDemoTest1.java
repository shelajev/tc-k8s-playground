package com.example.s3;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TouchingK8sDemoTest1 {
  private static final Logger log = LoggerFactory.getLogger(TouchingK8sDemoTest1.class);

  @Test
  public void createCluster() {
    K3sContainer k8s = new K3sContainer(
      DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
      .withLogConsumer(new Slf4jLogConsumer(log));
    k8s.start();

    String kubeConfigYaml = k8s.getKubeConfigYaml();
    Config config = Config.fromKubeconfig(kubeConfigYaml);
    KubernetesClient client = new DefaultKubernetesClient(config);

    Namespace ns = new NamespaceBuilder().withNewMetadata().withName("oleg")
      .endMetadata().build();
    client.namespaces().create(ns);

    List<Namespace> namespaces = client.namespaces().list().getItems();
    Assertions.assertThat(namespaces.get(namespaces.size() - 1) .getMetadata().getName()).isEqualTo("oleg");
  }
}
