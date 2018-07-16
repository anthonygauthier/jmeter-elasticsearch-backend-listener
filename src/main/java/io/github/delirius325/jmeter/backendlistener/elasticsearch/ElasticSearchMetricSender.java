package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.*;

public class ElasticSearchMetricSender {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchBackendClient.class);

    private RestClient client;
    private String esIndex;
    private List<String> metricList;

    public ElasticSearchMetricSender(RestClient cli, String index) {
        this.client = cli;
        this.esIndex = index;
        this.metricList = new LinkedList<String>();
    }

    public int getListSize() {
        return this.metricList.size();
    }

    public void clearList() {
        this.metricList.clear();
    }

    public void addToList(String metric) {
        this.metricList.add(metric);
    }

    public void createIndex() {
        try {
            this.client.performRequest("PUT", "/"+ this.esIndex);
        } catch (Exception e) {
            logger.info("Index already exists!");
        }
    }

    public void sendRequest() throws IOException {
        String actionMetaData = String.format("{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\" } }%n", this.esIndex, "SampleResult");

        StringBuilder bulkRequestBody = new StringBuilder();
        for (String metric : this.metricList) {
            bulkRequestBody.append(actionMetaData);
            bulkRequestBody.append(metric);
            bulkRequestBody.append("\n");
        }

        HttpEntity entity = new NStringEntity(bulkRequestBody.toString(), ContentType.APPLICATION_JSON);
        try {
            Response response = this.client.performRequest("POST", "/"+ this.esIndex +"/SampleResult/_bulk", Collections.emptyMap(), entity);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                if(logger.isErrorEnabled()) {
                    logger.error("ElasticSearch Backend Listener failed to write results for index {}", this.esIndex);
                }
            }
        } catch (Exception e) {
            if(logger.isErrorEnabled()) {
                logger.error("ElasticSearch Backend Listener was unable to perform request to the ElasticSearch engine. Request reached timeout.");
            }
        }
    }
}
