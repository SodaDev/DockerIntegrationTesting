package com.soda;

import com.soda.config.DockerRepositoryConfig;
import com.soda.config.DockerTestImage;
import com.soda.config.DockerTestImages;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.messages.AuthConfig;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;

public final class DockerTestManagerBuilder {

    private DockerRepositoryConfig repoConfig;
    private List<DockerTestImage> images;

    private DockerTestManagerBuilder() { }

    public static DockerTestManagerBuilder builder() {
        return new DockerTestManagerBuilder();
    }

    public DockerTestManagerBuilder withDockerRepoConfig(DockerRepositoryConfig repoConfig) {
        this.repoConfig = repoConfig;
        return this;
    }

    public DockerTestManagerBuilder withImages(DockerTestImages dockerImages) {
        images = asList(dockerImages.images());
        return this;
    }

    public DockerTestManager build() throws DockerCertificateException {
        DefaultDockerClient dockerClient = createDockerBuilder().build();
        return new DockerTestManager(dockerClient, images);
    }

    private DefaultDockerClient.Builder createDockerBuilder() throws DockerCertificateException {
        if (isRepoConfigEmpty()) {
            return DefaultDockerClient.fromEnv();
        }
        return DefaultDockerClient.builder()
                .uri(URI.create(repoConfig.serverAddress()))
                .dockerCertificates(buildDockerCertificatePath())
                .authConfig(buildAuthConfig());
    }

    private AuthConfig buildAuthConfig() {
        AuthConfig.Builder authBuilder = AuthConfig.builder()
                .email(repoConfig.email())
                .username(repoConfig.username())
                .password(repoConfig.password());

        if (StringUtils.isNotEmpty(repoConfig.serverAddress())) {
            authBuilder.serverAddress(repoConfig.serverAddress());
        }

        return authBuilder.build();
    }

    private DockerCertificates buildDockerCertificatePath() throws DockerCertificateException {
        File certFile = new File(repoConfig.certPath());
        if(!certFile.exists()) {
            throw new RuntimeException("Certificate path is not set or not exist in: " + repoConfig.certPath());
        }
        return DockerCertificates.builder().dockerCertPath(certFile.toPath()).build().get();
    }

    private boolean isRepoConfigEmpty() {
        return repoConfig.serverAddress().isEmpty()
                && repoConfig.email().isEmpty()
                && repoConfig.username().isEmpty()
                && repoConfig.password().isEmpty()
                && repoConfig.certPath().isEmpty();
    }
}
