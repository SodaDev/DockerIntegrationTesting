package com.soda;

import com.soda.docker.test.config.DockerIntegrationTest;
import com.soda.docker.test.manager.DockerTestManager;
import com.soda.docker.test.manager.DockerTestManagerBuilder;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test execution listener responsible for test lifecycle handling
 */
public class DockerIntegrationTestExecutionListener extends AbstractTestExecutionListener {
    private DockerTestManager dockerTestManager;
    private DockerIntegrationTest testConfig;

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        if(!testContext.getTestClass().isAnnotationPresent(DockerIntegrationTest.class)) {
            throw new RuntimeException(DockerIntegrationTest.class.getSimpleName()
                    + " is mandatory with "
                    + DockerIntegrationTestExecutionListener.class.getSimpleName()
            );
        }
        testConfig = testContext.getTestClass().getAnnotation(DockerIntegrationTest.class);
        this.dockerTestManager = DockerTestManagerBuilder.builder()
                .withDockerRepoConfig(testConfig.config())
                .withImages(testConfig.images())
                .build();

        if (testConfig.singleInstance()) {
            dockerTestManager.startContainers();
        }

        super.beforeTestClass(testContext);
    }

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        if (!testConfig.singleInstance()) {
            dockerTestManager.startContainers();
        }

        super.prepareTestInstance(testContext);
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        if (!testConfig.singleInstance()) {
            dockerTestManager.clearAfterTest();
        }
        super.afterTestMethod(testContext);
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        try {
            dockerTestManager.clearAfterTest();
        } finally {
            dockerTestManager.close();
        }

        super.afterTestClass(testContext);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }


}
