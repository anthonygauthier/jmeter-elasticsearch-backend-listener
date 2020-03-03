package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.core.config.Configurator;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Running SSL protected elasticsearch needed")
public class TestElasticSearchBackendHTTPsConnection {
    private static RestClient client;

    private ElasticSearchMetricSender sender;

    @Before
    public void setUp() throws Exception {
        Configurator.initialize(null, "config/log4j2.xml");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        String SSL_TRUSTSTORE_PATH = "certs/truststore.jks";
        String SSL_TRUSTSTORE_PW = "changeit";
        String SSL_KEYSTORE_PATH = "certs/jmeter-keystore.jks";
        String SSL_KEYSTORE_PW = "changeit";

        System.setProperty("javax.net.ssl.keyStore", SSL_KEYSTORE_PATH);
        System.setProperty("javax.net.ssl.keyStorePassword", SSL_KEYSTORE_PW);
        System.setProperty("javax.net.ssl.keyStoreType",
                FilenameUtils.getExtension(SSL_KEYSTORE_PATH).toLowerCase().equals("jks") ? "JKS" : "PKCS12");

        System.setProperty("javax.net.ssl.trustStore", SSL_TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", SSL_TRUSTSTORE_PW);
        System.setProperty("javax.net.ssl.trustStoreType",
                FilenameUtils.getExtension(SSL_TRUSTSTORE_PATH).toLowerCase().equals("jks") ? "JKS" : "PKCS12");

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
    public void createIndex() {
        sender.createIndex();
    }
}
