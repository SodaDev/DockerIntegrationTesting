package com.soda;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestExecutionListeners({
        DockerIntegrationTestExecutionListener.class
})
@DockerRepositoryConfig(
        serverAddress = "https://192.168.99.100:2376",
        email = "sodkiewicz.m@gmail.com",
        username = "sodkiewiczm",
        password = "",
        certPath = "/Users/soda/.docker/machine/machines/default"
)
@DockerTestImages(
    images = @DockerTestImage(
            name = "couchbase:latest",
            ports = { "8091", "8092", "8093", "8094", "11207", "11210", "11211", "18091", "18092", "18093" },
            initCmds = {
                    // http://developer.couchbase.com/documentation/server/4.5/install/init-setup.html
                    // http://developer.couchbase.com/documentation/server/current/cli/cbcli/cluster-init.html
                    @DockerTestImageInitCmd(cmd = {"bash", "-c", "couchbase-cli cluster-init -c 192.168.99.100 --cluster-init-username=Administrator --cluster-init-password=password --cluster-init-ramsize=600 --services=data -u admin -p password"}),
                    @DockerTestImageInitCmd(cmd = {"bash", "-c", "couchbase-cli bucket-create -c 192.168.99.100:8091 --bucket=default --bucket-type=couchbase --bucket-port=11211 --bucket-ramsize=600 --bucket-replica=1 -u Administrator -p password"})
            },
            initDelayInMs = 7500
    )
)
public class CouchbaseIntegrationTest {
    private CouchbaseCluster cluster;
    private Bucket bucket;

    @Before
    public void setUp() throws Exception {
        cluster = CouchbaseCluster.create("192.168.99.100");
        bucket = cluster.openBucket("default", 30, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        bucket.close();
        cluster.disconnect();
    }

    @Test
    public void shouldPullDockerImage() throws Exception {
        // GIVEN any json document
        JsonObject arthur = JsonObject.create()
                .put("name", "Arthur")
                .put("email", "kingarthur@couchbase.com")
                .put("interests", JsonArray.from("Holy Grail", "African Swallows"));

        // WHEN document is persisted
        bucket.upsert(JsonDocument.create("u:king_arthur", arthur), 30, TimeUnit.SECONDS);

        // THEN document should be persisted correctly
        JsonDocument result = bucket.get("u:king_arthur");

        System.out.println(result);
        assertEquals(arthur, result.content());
    }
}