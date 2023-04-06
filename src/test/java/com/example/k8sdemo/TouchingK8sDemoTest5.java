package com.example.k8sdemo;

import com.dajudge.kindcontainer.K3sContainer;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.readiness.Readiness;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class TouchingK8sDemoTest5 {

    @Test
    public void myTest() {
        var k8s = new K3sContainer<>()
                .withCopyToContainer(Transferable.of("you don't know 42".getBytes(UTF_8)), "/super-secret.txt");
        k8s.start();

        // obtain a kubeconfig file which allows us to connect to k3s
        var kubeConfigYaml = k8s.getKubeconfig();

        var config = Config.fromKubeconfig(kubeConfigYaml);

        var client = new DefaultKubernetesClient(config);

        var ns = new NamespaceBuilder().withNewMetadata().withName("devnexus")
                .endMetadata().build();
        client.namespaces().create(ns);

        var cmd = "nsenter --mount=/proc/1/ns/mnt cat /super-secret.txt";
        createDeployment(client, cmd);

        var pods = await()
                .timeout(60, SECONDS)
                .until(() -> client.pods().inNamespace("devnexus").list().getItems(), it -> !it.isEmpty());
        System.out.println("Pod is created");

        var pod = await().until(() -> client.pods()
                        .inNamespace("devnexus")
                        .withName(pods.get(0).getMetadata().getName())
                        .get(),
                Readiness::isPodReady);
        System.out.println("Pod is ready");

        var log = await().until(() -> client.pods()
                        .inNamespace("devnexus")
                        .withName(pod.getMetadata().getName())
                        .inContainer("shell")
                        .getLog(),
                it -> it.contains("42"));

        System.out.println("Your secret is: " + log.trim());
    }

    private static Deployment createDeployment(KubernetesClient client, String command) {
        var script = """
                %s
                echo
                sleep infinity
                """.formatted(command);
        var selectors = Map.of("app", "devnexus");
        var d = new DeploymentBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("devnexus")
                        .build())
                .withSpec(new DeploymentSpecBuilder()
                        .withReplicas(2)
                        .withTemplate(new PodTemplateSpecBuilder()
                                .withMetadata(new ObjectMetaBuilder()
                                        .withLabels(selectors)
                                        .build())
                                .withSpec(new PodSpecBuilder()
                                        .withHostPID()
                                        .addNewContainer()
                                        .withName("shell")
                                        .withImage("busybox")
                                        .withCommand("sh", "-c", script)
                                        .withSecurityContext(new SecurityContextBuilder()
//                                                .withPrivileged()
                                                .build())
                                        .endContainer()
                                        .build())
                                .build())
                        .withSelector(new LabelSelectorBuilder()
                                .withMatchLabels(selectors)
                                .build())
                        .build())
                .build();

        return client.apps().deployments().inNamespace("devnexus").create(d);
    }
}
