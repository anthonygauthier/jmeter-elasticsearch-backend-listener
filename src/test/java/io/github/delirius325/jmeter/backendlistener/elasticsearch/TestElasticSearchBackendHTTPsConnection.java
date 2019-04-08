package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.core.config.Configurator;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Test;

public class TestElasticSearchBackendHTTPsConnection {
    private static RestClient client;

    private ElasticSearchMetricSender sender;

    @Before
    public void setUp() throws Exception {
        Configurator.initialize(null, "config/log4j2.xml");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        client = RestClient.builder(new HttpHost("localhost", Integer.parseInt("9200"), "https"))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000)
                        .setSocketTimeout((int) 200L))
                .setFailureListener(new RestClient.FailureListener() {
                    @Override
                    public void onFailure(Node node) {
                        System.err.println("Error with node: " + node.toString());
                    }
                }).setMaxRetryTimeoutMillis(60000).build();
        sender = new ElasticSearchMetricSender(client, "test_" + sdf.format(new Date()), "logstashTest",
                "logstashTest", "");
    }

    @Test
    public void createIndex() throws Exception {
        sender.createIndex();
    }
}
