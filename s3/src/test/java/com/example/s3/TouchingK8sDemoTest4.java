package com.example.s3;

import com.dajudge.kindcontainer.K3sContainer;
import com.github.terma.javaniotcpproxy.StaticTcpProxyConfig;
import com.github.terma.javaniotcpproxy.TcpProxy;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.util.Map;

public class TouchingK8sDemoTest4 {
    private static final Logger log = LoggerFactory.getLogger(TouchingK8sDemoTest4.class);

    @Test
    public void myTest() throws IOException {
        Network network = Network.newNetwork();

        PostgreSQLContainer<?> postgreSQLContainer =
                new PostgreSQLContainer<>("postgres:11.5-alpine")
                        .withNetwork(network)
                        .withNetworkAliases("postgres");
        postgreSQLContainer.start();

        createPostgresProxy(postgreSQLContainer);

        K3sContainer<?> k8s = new K3sContainer<>()
                .withNetwork(network);
        k8s.addExposedPorts(30001);
        // postgres://username:password@hostname:port/database-name
        String pqConnect = "postgres://%s:%s@postgres:5432/%s?sslmode=disable".formatted(
                postgreSQLContainer.getUsername(),
                postgreSQLContainer.getPassword(),
                postgreSQLContainer.getDatabaseName()
        );
        k8s.withEnv("K3S_DATASTORE_ENDPOINT", pqConnect);


        k8s.start();
        // obtain a kubeconfig file which allows us to connect to k3s
        String kubeConfigYaml = k8s.getKubeconfig();

        Config config = Config.fromKubeconfig(kubeConfigYaml);

        KubernetesClient client = new DefaultKubernetesClient(config);

        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("alex")
                .endMetadata().build();
        client.namespaces().create(ns);

        var selectors = Map.of("app", "alex");
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
                .withNodePort(30001)
                .withPort(80)
                .withTargetPort(new IntOrString(80))
                .endPort()
                .withSelector(selectors)
                .withType("NodePort")
                .endSpec()
                .withNewMetadata()
                .withName("alex")
                .endMetadata()
                .build();

        client.services().inNamespace("alex").create(service);
    }

    private static void createDeployment(KubernetesClient client, Map<String, String> selectors) {
        Deployment d = new DeploymentBuilder()
                .withNewMetadata()
                .withName("alex")
                .withLabels(selectors)
                .endMetadata()
                .withNewSpec()
                .withReplicas(2)
                .withNewTemplate()
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
                .endTemplate()
                .withNewSelector()
                .withMatchLabels(selectors)
                .endSelector()
                .endSpec()
                .build();

        client.apps().deployments().inNamespace("alex").create(d);
    }

    static TcpProxy tcpProxy;

    @AfterAll
    public static void stopProxy() {
        if (tcpProxy != null)
            tcpProxy.shutdown();
    }

    private static void createPostgresProxy(PostgreSQLContainer<?> postgreSQLContainer) {
        StaticTcpProxyConfig config = new StaticTcpProxyConfig(
                5900,
                postgreSQLContainer.getHost(),
                postgreSQLContainer.getFirstMappedPort()
        );
        config.setWorkerCount(1);
        tcpProxy = new TcpProxy(config);
        tcpProxy.start();
    }

}
