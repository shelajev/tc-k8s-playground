package com.example.s3;

import com.dajudge.kindcontainer.K3sContainer;
import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;
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
import java.util.HashMap;
import java.util.Map;

public class TouchingK8sDemoTest3 {
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

        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("alex")
                .endMetadata().build();
        client.namespaces().create(ns);

        var selectors = Map.of("app", "alex");
        var cm = createConfigMap(client);
        createDeployment(client, selectors, cm);
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
                .withName("alex")
                .endMetadata()
                .build();

        client.services().inNamespace("alex").create(service);
    }

    private static void createDeployment(KubernetesClient client, Map<String, String> selectors, ConfigMap cm) {
        Deployment d = new DeploymentBuilder()
                .withNewMetadata()
                .withName("alex")
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
                                .withVolumeMounts(new VolumeMountBuilder()
                                        .withName("static-resources")
                                        .withMountPath("/usr/share/nginx/html")
                                        .build())
                                .endContainer()
                                .withVolumes(new VolumeBuilder()
                                        .withName("static-resources")
                                        .withConfigMap(new ConfigMapVolumeSourceBuilder()
                                                .withName(cm.getMetadata().getName())
                                                .build())
                                        .build())
                                .endSpec()
                                .build())
                        .withNewSelector()
                        .withMatchLabels(selectors)
                        .endSelector()
                        .build())
                .build();

        client.apps().deployments().inNamespace("alex").create(d);
    }

    private static ConfigMap createConfigMap(final KubernetesClient client) {
        return client.configMaps().inNamespace("alex").create(new ConfigMapBuilder()
                .withNewMetadata()
                .withName("alex")
                .endMetadata()
                .withData(new HashMap<>() {{
                    put("index.html", "<h1>Hello, JavaLand!</h1>");
                }})
                .build());
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
