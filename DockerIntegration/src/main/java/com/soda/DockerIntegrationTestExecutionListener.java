package com.soda;

import com.google.common.collect.Sets;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.io.File;
import java.net.URI;
import java.util.*;

public class DockerIntegrationTestExecutionListener extends AbstractTestExecutionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerIntegrationTestExecutionListener.class);
    private static final String PREFIX = "\n\n==================================================\n\n";

    private AuthConfig authConfig;
    private DockerClient dockerClient;
    Set<String> containers = Sets.newConcurrentHashSet();

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        final DockerRepositoryConfig dockerRepoConfig = testContext.getTestClass().getAnnotation(DockerRepositoryConfig.class);
        final DockerTestImages dockerTestImages = testContext.getTestClass().getAnnotation(DockerTestImages.class);

        authConfig = buildAuthConfig(dockerRepoConfig);
        dockerClient = buildDockerClient(dockerRepoConfig, authConfig);

        LOGGER.info(PREFIX + "Pulling images {} for integration test" + PREFIX, dockerTestImages.images());
        pullDockerImages(dockerTestImages);
        LOGGER.info(PREFIX + "Docker images successfully loaded" + PREFIX);

        super.beforeTestClass(testContext);
    }

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        final DockerRepositoryConfig dockerRepoConfig = testContext.getTestClass().getAnnotation(DockerRepositoryConfig.class);
        final DockerTestImages dockerTestImages = testContext.getTestClass().getAnnotation(DockerTestImages.class);

        LOGGER.info(PREFIX + "Starting docker containers: {}" + PREFIX, dockerTestImages.images());
        for (DockerTestImage image : dockerTestImages.images()) {
            startDockerContainer(image);
        }
        LOGGER.info(PREFIX + "Docker containers started" + PREFIX);

        super.prepareTestInstance(testContext);
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        try {
            for (String id : containers) {
                LOGGER.info("Closing container {}", id);
                dockerClient.killContainer(id);
                dockerClient.removeContainer(id);
            }
        } finally {
            dockerClient.close();
        }

        super.afterTestClass(testContext);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void startDockerContainer(DockerTestImage dockerImage) throws DockerCertificateException, DockerException, InterruptedException {
        final Map<String, List<PortBinding>> portBindings = new HashMap<String, List<PortBinding>>();
        for (String port : dockerImage.ports()) {
            List<PortBinding> hostPorts = new ArrayList<PortBinding>();
            hostPorts.add(PortBinding.of("0.0.0.0", port));
            portBindings.put(port, hostPorts);
        }

        // Bind container port 443 to an automatically allocated available host port.
        List<PortBinding> randomPort = new ArrayList<PortBinding>();
        randomPort.add(PortBinding.randomPort("0.0.0.0"));
        portBindings.put("443", randomPort);

        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(dockerImage.name()).exposedPorts(dockerImage.ports())
                .cmd(dockerImage.configCmds())
                .build();

        final ContainerCreation creation = dockerClient.createContainer(containerConfig);
        String id = creation.id();
        final ContainerInfo info = dockerClient.inspectContainer(id);

        LOGGER.info(PREFIX + "Container {} will be started now" + PREFIX, info);
        dockerClient.startContainer(id);
        containers.add(id);
        Thread.sleep(dockerImage.initDelayInMs());
        LOGGER.info(PREFIX + "Container {} started and initializing now" + PREFIX, info);

        for (DockerTestImageInitCmd command : dockerImage.initCmds()) {
            final String execId = dockerClient.execCreate(
                    id, command.cmd(), DockerClient.ExecCreateParam.attachStdout(),
                    DockerClient.ExecCreateParam.attachStderr());
            final LogStream output = dockerClient.execStart(execId);
            LOGGER.debug(PREFIX + output.readFully() + PREFIX);
            Thread.sleep(dockerImage.initDelayInMs());
        }

        LOGGER.info(PREFIX + "Container started successfully" + PREFIX);
    }

    private void pullDockerImages(DockerTestImages dockerTestImages) throws DockerException, InterruptedException, DockerCertificateException {
        for (DockerTestImage dockerImage : dockerTestImages.images()) {
            dockerClient.pull(dockerImage.name());
        }
    }

    private DockerClient buildDockerClient(DockerRepositoryConfig dockerRepoConfig, AuthConfig authConfig) throws DockerCertificateException {
        return createDockerBuilder(authConfig, dockerRepoConfig)
                .authConfig(authConfig)
                .build();
    }

    private DefaultDockerClient.Builder createDockerBuilder(AuthConfig dockerAuthConfig, DockerRepositoryConfig dockerRepoConfig) throws DockerCertificateException {
        DefaultDockerClient.Builder dockerBuilder = null;

        if (System.getenv("DOCKER_HOST") != null) {
            dockerBuilder = DefaultDockerClient.fromEnv();
        } else {
            dockerBuilder = DefaultDockerClient.builder()
                    .uri(URI.create(dockerAuthConfig.serverAddress()))
                    .dockerCertificates(buildDockerCertificatePath(dockerRepoConfig));
        }
        return dockerBuilder;
    }

    private DockerCertificates buildDockerCertificatePath(DockerRepositoryConfig dockerRepoConfig) throws DockerCertificateException {
        File certFile = new File(dockerRepoConfig.certPath());
        if(!certFile.exists()) {
            throw new RuntimeException("Certificate path is not set or not exist in: " + dockerRepoConfig.certPath());
        }
        return DockerCertificates.builder().dockerCertPath(certFile.toPath()).build().get();
    }

    private AuthConfig buildAuthConfig(DockerRepositoryConfig dockerRepoConfig) {
        AuthConfig.Builder authBuilder = AuthConfig.builder()
                .email(dockerRepoConfig.email())
                .username(dockerRepoConfig.username())
                .password(dockerRepoConfig.password());

        if (StringUtils.isNotEmpty(dockerRepoConfig.serverAddress())) {
            authBuilder.serverAddress(dockerRepoConfig.serverAddress());
        }

        return authBuilder.build();
    }
}
