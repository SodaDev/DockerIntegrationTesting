package com.soda;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
            ports = { "8091", "8092", "8093", "8094", "11210", "11211" },
            initCmds = {
                    //http://developer.couchbase.com/documentation/server/4.5/install/init-setup.html http://developer.couchbase.com/documentation/server/current/cli/cbcli/cluster-init.html
                    @DockerTestImageInitCmd(cmd = {"bash", "-c", "couchbase-cli cluster-init -c 192.168.99.100 --cluster-init-username=Administrator --cluster-init-password=password --cluster-init-ramsize=600 -u admin -p password"}),
                    @DockerTestImageInitCmd(cmd = {"bash", "-c", "couchbase-cli bucket-create -c 192.168.99.100 --bucket=default --bucket-type=couchbase --bucket-port=11211 --bucket-ramsize=600 --bucket-replica=1 -u Administrator -p password"})
            }
    )
)
public class CouchbaseIntegrationTest {

    @Test
    public void shouldPullDockerImage() throws Exception {
        Cluster cluster = CouchbaseCluster.create("192.168.99.100");
        Bucket bucket = cluster.openBucket("default");

        // Create a JSON Document
        JsonObject arthur = JsonObject.create()
                .put("name", "Arthur")
                .put("email", "kingarthur@couchbase.com")
                .put("interests", JsonArray.from("Holy Grail", "African Swallows"));

        // Store the Document
        bucket.upsert(JsonDocument.create("u:king_arthur", arthur));

        // Load the Document and print it
        // Prints Content and Metadata of the stored Document
        System.out.println(bucket.get("u:king_arthur"));

        // Create a N1QL Primary Index (but ignore if it exists)
        bucket.bucketManager().createN1qlPrimaryIndex(true, false);

        // Perform a N1QL Query
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("SELECT name FROM default WHERE $1 IN interests",
                        JsonArray.from("African Swallows"))
        );

        // Print each found Row
        for (N1qlQueryRow row : result) {
            // Prints {"name":"Arthur"}
            System.out.println(row);
        }
    }
}
//couchbase-cli cluster-init -c 192.168.99.100 --cluster-init-username=Administrator --cluster-init-password=password --cluster-init-ramsize=600 -u admin -p password
//couchbase-cli bucket-create -c 192.168.99.100 --bucket=default --bucket-type=couchbase --bucket-port=11211 --bucket-ramsize=600 --bucket-replica=1 -u Administrator -p password