package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import static io.github.delirius325.jmeter.backendlistener.elasticsearch.ElasticSearchRequests.SEND_BULK_REQUEST;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchMetricSender {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchMetricSender.class);
    private RestClient client;
    private String esIndex;
    private String host;
    private List<String> metricList;
    private String authUser;
    private String authPwd;
    private String awsEndpoint;

    public ElasticSearchMetricSender(RestClient cli, String host, String index, String user, String pwd, String endpoint) {
        this.client = cli;
        this.esIndex = index;
        this.metricList = new LinkedList<String>();
        this.authUser = user.trim();
        this.authPwd = pwd.trim();
        this.awsEndpoint = endpoint;
        this.host = host;
    }

    /**
     * This method returns the current size of the ElasticSearch documents list
     * 
     * @return integer representing the size of the ElasticSearch documents list
     */
    public int getListSize() {
        return this.metricList.size();
    }

    /**
     * This method closes the REST client
     */
    public void closeConnection() throws IOException {
        this.client.close();
    }

    /**
     * This method clears the ElasticSearch documents list
     */
    public void clearList() {
        this.metricList.clear();
    }

    /**
     * This method adds a metric to the list (metricList).
     * 
     * @param metric
     *            String parameter representing a JSON document for ElasticSearch
     */
    public void addToList(String metric) {
        this.metricList.add(metric);
    }

    /**
     * This method sets the Basic Authorization header to requests
     */
    private Request setHeaders(Request request) {
        RequestOptions.Builder options = request.getOptions().toBuilder();
        if (this.awsEndpoint.equals("") && !this.authPwd.equals("")) {
            String encodedCredentials = Base64.getEncoder()
                    .encodeToString((this.authUser + ":" + this.authPwd).getBytes());
            options.addHeader("Authorization", "Basic " + encodedCredentials);
            
        }
        options.addHeader("Host", host);
        request.setOptions(options);
        return request;
    }

    /**
     * This method creates the ElasticSearch index.
     */
    public void createIndex() {
        try {
            this.client.performRequest(setHeaders(new Request("PUT", "/" + this.esIndex)));
        } catch (IOException e) {
            logger.info("Index already exists!");
        }
    }
    
    public int getElasticSearchVersion() {
    	Request request = new Request("GET", "/" );
    	int elasticSearchVersion = -1;
    	 try {
             Response response = this.client.performRequest(setHeaders(request));
             if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && logger.isErrorEnabled()) {
                 logger.error("Unable to perform request to ElasticSearch engine for index {}. Response status: {}",
                              this.esIndex, response.getStatusLine().toString());
             }else {
            	 String responseBody = EntityUtils.toString(response.getEntity());
     			 JSONObject elasticSearchConfig = new JSONObject(responseBody);
     			 JSONObject version  = (JSONObject) elasticSearchConfig.get("version");
     			 String elasticVersion =  version.get("number").toString();
     			 elasticSearchVersion = Integer.parseInt(elasticVersion.split("\\.")[0]);
                 logger.info("ElasticSearch Version : "  + Integer.toString(elasticSearchVersion));
             }
         } catch (Exception e) {
             if (logger.isErrorEnabled()) {
                 logger.error("Exception" + e);
                 logger.error("ElasticSearch Backend Listener was unable to perform request to the ElasticSearch engine. Check your JMeter console for more info.");
             }
         }
    	 return elasticSearchVersion;
    }
    

    /**
     * This method sends the ElasticSearch documents for each document present in the list (metricList). All is being
     * sent through the low-level ElasticSearch REST Client.
     */
    public void sendRequest(int elasticSearchVersionPrefix) {
    	Request request;
    	StringBuilder bulkRequestBody = new StringBuilder();
    	String actionMetaData;
    	if(elasticSearchVersionPrefix < 7) {
    		 request = new Request("POST", "/" + this.esIndex + "/SampleResult/_bulk");
 			 actionMetaData = String.format(SEND_BULK_REQUEST, this.esIndex, "SampleResult");
    	}
    	else {
    		 request = new Request("POST", "/" + this.esIndex + "/_bulk");
    		 actionMetaData = String.format(SEND_BULK_REQUEST, this.esIndex);
    	}
    		
        for (String metric : this.metricList) {
            bulkRequestBody.append(actionMetaData);
            bulkRequestBody.append(metric);
            bulkRequestBody.append("\n");
        }

        request.setEntity(new NStringEntity(bulkRequestBody.toString(), ContentType.APPLICATION_JSON));

        try {

            Response response = this.client.performRequest(setHeaders(request));

            if (logger.isErrorEnabled()) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    logger.error("ElasticSearch Backend Listener failed to write results for index {}. Response status: {}",
                                 this.esIndex, response.getStatusLine().toString());
                } else {
                    logger.debug("ElasticSearch Backend Listener has successfully written to ES instance [{}] _bulk request {}",
                                 client.getNodes().iterator().next().getHost().toHostString(), request.toString());
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Exception" + e);
                logger.error("Elastic Search Request End Point: " + request.getEndpoint());
                logger.error("ElasticSearch Backend Listener was unable to perform request to the ElasticSearch engine. Check your JMeter console for more info.");
            }
        }
    }

}
