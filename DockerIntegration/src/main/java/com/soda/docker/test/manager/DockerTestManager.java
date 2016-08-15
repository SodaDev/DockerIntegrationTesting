package com.soda.docker.test.manager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.soda.DockerIntegrationTestExecutionListener;
import com.soda.docker.test.config.DockerTestImage;
import com.soda.docker.test.config.DockerTestImageInitCmd;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.spotify.docker.client.DockerClient.ExecCreateParam.attachStderr;
import static com.spotify.docker.client.DockerClient.ExecCreateParam.attachStdout;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Class wraps up all the docker integration test logic
 */
public class DockerTestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerIntegrationTestExecutionListener.class);
    private final Set<String> containers = Sets.newConcurrentHashSet();
    private final DefaultDockerClient dockerClient;
    private final List<DockerTestImage> images;

    protected DockerTestManager(DefaultDockerClient dockerClient, List<DockerTestImage> images) {
        this.dockerClient = dockerClient;
        this.images = images;
    }

    public void pullImages() throws DockerException, InterruptedException {
        LOGGER.info("Prepare for test: [STARTED] Pulling images for integration test");
        pullDockerImages();
        LOGGER.info("Prepare for test: [FINISHED] Pulling images for integration test");
    }

    public void startContainers() throws InterruptedException, DockerException, DockerCertificateException {
        try {
            LOGGER.info("Run test: [STARTED] Starting containers");
            startDockerContainers();
            LOGGER.info("Run test: [FINISHED] Starting containers");
        } catch (ImageNotFoundException infe) {
            LOGGER.info("Run test: Image was not found locally, pulling image from docker registry");
            pullImages();
            startContainers();
        }
    }

    public void clearAfterTest() throws DockerException, InterruptedException {
        for (String id : containers) {
            LOGGER.info("After test: [STARTED] Kill and remove container {}", id);
            dockerClient.killContainer(id);
            dockerClient.removeContainer(id);
            LOGGER.info("After test: [FINISHED] Kill and remove container {}", id);
        }

        containers.clear();
    }

    public void close() {
        dockerClient.close();
    }

    private void pullDockerImages() throws DockerException, InterruptedException {
        for (DockerTestImage dockerImage : images) {
            LOGGER.info("Prepare for test: [STARTED] Pulling image {} for integration test", dockerImage.name());
            dockerClient.pull(dockerImage.name());
            LOGGER.info("Prepare for test: [FINISHED] Pulling image {} for integration test", dockerImage.name());
        }
    }

    private void startDockerContainers() throws InterruptedException, DockerException, DockerCertificateException {
        for (DockerTestImage dockerImage : images) {
            LOGGER.info("Run test: [STARTED] Starting container {}", dockerImage.name());
            startDockerContainer(dockerImage);
            LOGGER.info("Run test: [FINISHED] Starting container {}", dockerImage.name());
        }
    }

    private void startDockerContainer(DockerTestImage dockerImage) throws DockerCertificateException, DockerException, InterruptedException {
        final HostConfig hostConfig = HostConfig.builder().portBindings(createPortBindings(dockerImage)).build();
        ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(dockerImage.name()).exposedPorts(dockerImage.ports())
                .cmd(asList(dockerImage.configCmds()))
                .build();

        String id = dockerClient.createContainer(containerConfig).id();
        final ContainerInfo info = dockerClient.inspectContainer(id);

        LOGGER.info("Run test: [STARTED] Container will start now: {}", info);
        dockerClient.startContainer(id);
        containers.add(id);
        Thread.sleep(dockerImage.initDelayInMs());
        LOGGER.info("Run test: [FINISHED] Container started and initializing now: {}", info);

        initializeContainer(id, dockerImage);
    }

    private Map<String, List<PortBinding>> createPortBindings(DockerTestImage dockerImage) {
        return new ImmutableMap.Builder<String, List<PortBinding>>()
                .putAll(Stream.of(dockerImage.ports())
                        .collect(toMap(identity(), port -> newArrayList(PortBinding.of("0.0.0.0", port)))))
                .put("443", newArrayList(PortBinding.randomPort("0.0.0.0")))
                .build();
    }

    private void initializeContainer(String id, DockerTestImage dockerImage) throws DockerException, InterruptedException {
        for (DockerTestImageInitCmd command : dockerImage.initCmds()) {
            final String execId = dockerClient.execCreate(id, command.cmd(), attachStdout(), attachStderr());
            dockerClient.execStart(execId);
            Thread.sleep(dockerImage.initDelayInMs());
        }
    }
}
