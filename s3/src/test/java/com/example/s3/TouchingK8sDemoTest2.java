package com.example.s3;

import com.github.terma.javaniotcpproxy.StaticTcpProxyConfig;
import com.github.terma.javaniotcpproxy.TcpProxy;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TouchingK8sDemoTest2 {
  private static final Logger log = LoggerFactory.getLogger(TouchingK8sDemoTest2.class);

  @Test
  public void myTest() {
    Network network = Network.newNetwork();

    PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:11.5-alpine").withNetwork(network)
        .withNetworkAliases("postgres");
    postgreSQLContainer.start();

    String postgresUrl = "postgres://%s:%s@postgres:5432/%s?sslmode=disable".formatted(postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword(), postgreSQLContainer.getDatabaseName());

    K3sContainer k8s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
      .withLogConsumer(new Slf4jLogConsumer(log)).withNetwork(network);

    k8s.withEnv("K3S_DATASTORE_ENDPOINT", postgresUrl);
    k8s.start();

// obtain a kubeconfig file which allows us to connect to k3s
    String kubeConfigYaml = k8s.getKubeConfigYaml();

// requires io.fabric8:kubernetes-client:5.11.0 or higher
    Config config = Config.fromKubeconfig(kubeConfigYaml);
//KubernetesClient client = new KubernetesClientBuilder().build();
    KubernetesClient client = new DefaultKubernetesClient(config);

    Namespace ns = new NamespaceBuilder().withNewMetadata().withName("oleg")
      .endMetadata().build();
    client.namespaces().create(ns);


    Assertions.assertThat(client.namespaces().list().getItems().get(0).getMetadata().getName()).isEqualTo("oleg");
  }

}
