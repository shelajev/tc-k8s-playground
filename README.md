# Run Kubernetes clusters with Testcontainers 

This is a collection of unit tests demonstrating how you can run Kubernetes clusters using [Testcontainers](https://www.testcontainers.com/).

## Prerequisites
* Install Java 17+
* Install a compatible Docker environment

## How to run tests?
Run the tests from your IDE, the tests are made to hang to let you explore the Kubernetes setup.

Run `docker ps` to find the ID of the container running k8s cluster for you.
Run `docker exec -ti $containerID /bin/sh` to enter the shell of the container.
Explore the cluster with `kubectl`. 