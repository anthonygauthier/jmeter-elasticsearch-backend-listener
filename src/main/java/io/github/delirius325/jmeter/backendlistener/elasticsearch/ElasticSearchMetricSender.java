package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.*;

public class ElasticSearchMetricSender {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchMetricSender.class);

    private RestClient client;
    private String esIndex;
    private List<String> metricList;

    public ElasticSearchMetricSender(RestClient cli, String index) {
        this.client = cli;
        this.esIndex = index;
        this.metricList = new LinkedList<String>();
    }

    /**
     * This method returns the current size of the ElasticSearch documents list
     * @return integer representing the size of the ElasticSearch documents list
     */
    public int getListSize() { return this.metricList.size(); }

    /**
     * This method clears the ElasticSearch documents list
     */
    public void closeConnection() throws IOException { this.client.close(); }

    /**
     * This method closes the REST client
     */
    public void clearList() { this.metricList.clear(); }

    /**
     * This method adds a metric to the list (metricList).
     * @param metric String parameter representing a JSON document for ElasticSearch
     */
    public void addToList(String metric) { this.metricList.add(metric); }

    /**
     * This method creates the ElasticSearch index.
     * Throws an exception if index already exists.
     */
    public void createIndex() {
        try {
            this.client.performRequest("PUT", "/"+ this.esIndex);
        } catch (Exception e) {
            logger.info("Index already exists!");
        }
    }

    /**
     * This method sends the ElasticSearch documents for each document present in the list (metricList).
     * All is being sent through the low-level ElasticSearch REST Client.
     */
    public void sendRequest() {
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
