package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jmeter.threads.JMeterContextService;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.*;

import static io.github.delirius325.jmeter.backendlistener.elasticsearch.ElasticSearchRequests.SEND_BULK_REQUEST;

public class ElasticSearchMetricSender {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchMetricSender.class);

    private RestClient client;
    private String esIndex;
    private List<String> metricList;
    private String authUser;
    private String authPwd;
    private String awsendpoint;

    public ElasticSearchMetricSender(RestClient cli, String index, String user, String pwd, String endpoint) {
        this.client = cli;
        this.esIndex = index;
        this.metricList = new LinkedList<String>();
        this.authUser = user.trim();
        this.authPwd = pwd.trim();
        this.awsendpoint = endpoint;
    }

    /**
     * This method returns the current size of the ElasticSearch documents list
     * @return integer representing the size of the ElasticSearch documents list
     */
    public int getListSize() { return this.metricList.size(); }

    /**
     * This method closes the REST client
     */
    public void closeConnection() throws IOException { this.client.close(); }

    /**
     * This method clears the ElasticSearch documents list
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
            this.client.performRequest(new Request("PUT", "/" + this.esIndex));
        } catch (Exception e) {
            logger.info("Index already exists!");
        }
    }

    /**
     * This method sends the ElasticSearch documents for each document present in the list (metricList).
     * All is being sent through the low-level ElasticSearch REST Client.
     */
    public void sendRequest() {
        Request request = new Request("POST", "/" + this.esIndex + "/SampleResult/_bulk");
        StringBuilder bulkRequestBody = new StringBuilder();
        String actionMetaData = String.format(SEND_BULK_REQUEST, this.esIndex, "SampleResult");

        for (String metric : this.metricList) {
            bulkRequestBody.append(actionMetaData);
            bulkRequestBody.append(metric);
            bulkRequestBody.append("\n");
        }

        request.setEntity(new NStringEntity(bulkRequestBody.toString(), ContentType.APPLICATION_JSON));

       try {

           if(this.awsendpoint.equals("") && !this.authPwd.equals("") ) {
               String encodedCredentials = Base64.getEncoder().encodeToString((this.authUser + ":" + this.authPwd).getBytes());
               RequestOptions.Builder options = request.getOptions().toBuilder();
               options.addHeader("Authorization", "Basic " + encodedCredentials);
               request.setOptions(options);
           }

           Response response = this.client.performRequest(request);

            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && logger.isErrorEnabled()) {
                logger.error("ElasticSearch Backend Listener failed to write results for index {}", this.esIndex);
           }
        } catch (Exception e) {
           if(logger.isErrorEnabled()) {
                logger.error("Exception" + e);
             logger.error("ElasticSearch Backend Listener was unable to perform request to the ElasticSearch engine. Request reached timeout.");
            }
        }
    }
}
