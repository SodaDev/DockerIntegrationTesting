package com.soda;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soda.config.DockerTestImage;
import com.soda.config.DockerTestImageInitCmd;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.spotify.docker.client.DockerClient.ExecCreateParam.attachStderr;
import static com.spotify.docker.client.DockerClient.ExecCreateParam.attachStdout;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class DockerTestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerIntegrationTestExecutionListener.class);
    private static final String PREFIX = "\n\n==================================================\n\n";

    private final DefaultDockerClient dockerClient;
    private final List<DockerTestImage> images;

    private final Set<String> containers = Sets.newConcurrentHashSet();

    protected DockerTestManager(DefaultDockerClient dockerClient, List<DockerTestImage> images) {
        this.dockerClient = dockerClient;
        this.images = images;
    }

    public void pullImages() throws DockerException, InterruptedException {
        LOGGER.info(PREFIX + "Prepare for test: [STARTED] Pulling images for integration test" + PREFIX);
        pullDockerImages();
        LOGGER.info(PREFIX + "Prepare for test: [FINISHED] Pulling images for integration test" + PREFIX);
    }

    public void startContainers() throws InterruptedException, DockerException, DockerCertificateException {
        try {
            LOGGER.info(PREFIX + "Run test: [STARTED] Starting containers" + PREFIX);
            startDockerContainers();
            LOGGER.info(PREFIX + "Run test: [FINISHED] Starting containers" + PREFIX);
        } catch (ImageNotFoundException infe) {
            LOGGER.info(PREFIX + "Run test: Image was not found locally, pulling image from docker registry" + PREFIX);
            pullImages();
            startContainers();
        }
    }

    public void clearAfterTest() throws DockerException, InterruptedException {
        for (String id : containers) {
            LOGGER.info(PREFIX + "After test: [STARTED] Kill and remove container {}" + PREFIX, id);
            dockerClient.killContainer(id);
            dockerClient.removeContainer(id);
            LOGGER.info(PREFIX + "After test: [FINISHED] Kill and remove container {}" + PREFIX, id);
        }

        containers.clear();
    }

    public void close() {
        dockerClient.close();
    }

    private void pullDockerImages() throws DockerException, InterruptedException {
        for (DockerTestImage dockerImage : images) {
            LOGGER.info(PREFIX + "Prepare for test: [STARTED] Pulling image {} for integration test" + PREFIX, dockerImage.name());
            dockerClient.pull(dockerImage.name());
            LOGGER.info(PREFIX + "Prepare for test: [FINISHED] Pulling image {} for integration test" + PREFIX, dockerImage.name());
        }
    }

    private void startDockerContainers() throws InterruptedException, DockerException, DockerCertificateException {
        for (DockerTestImage dockerImage : images) {
            LOGGER.info(PREFIX + "Run test: [STARTED] Starting container {}" + PREFIX, dockerImage.name());
            startDockerContainer(dockerImage);
            LOGGER.info(PREFIX + "Run test: [FINISHED] Starting container {}" + PREFIX, dockerImage.name());
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

        LOGGER.info(PREFIX + "Run test: [STARTED] Container {} will start now" + PREFIX, info);
        dockerClient.startContainer(id);
        containers.add(id);
        Thread.sleep(dockerImage.initDelayInMs());
        LOGGER.info(PREFIX + "Run test: [FINISHED] Container {} started and initializing now" + PREFIX, info);

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
            final LogStream output = dockerClient.execStart(execId);
//            LOGGER.debug(PREFIX + "Run test: Container init with command {} and result {}" + PREFIX, command.cmd(), output.readFully());
            Thread.sleep(dockerImage.initDelayInMs());
        }
    }
}
