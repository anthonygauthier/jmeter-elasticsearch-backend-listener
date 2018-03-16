package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchBackend extends AbstractBackendListenerClient {
    private static final String BUILD_NUMBER        = "BuildNumber";
    private static final String ES_SCHEME           = "es.scheme";
    private static final String ES_HOST             = "es.host";
    private static final String ES_PORT             = "es.port";
    private static final String ES_INDEX            = "es.index";
    private static final String ES_TIMESTAMP        = "es.timestamp";
    private static final String ES_BULK_SIZE        = "es.bulk.size";
    private static final String ES_TIMEOUT_MS       = "es.timout.ms";
    private static final String ES_SAMPLE_FILTER    = "es.sample.filter";
    private static final String ES_TEST_MODE        = "es.test.mode";
    private static final String ES_TRANSPORT_CLIENT = "es.transport.client";
    private static final long DEFAULT_TIMEOUT_MS = 200L;
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchBackend.class);

    private List<String> bulkRequestList;
    private List<String> filters;
    private RestClient client;
    private String index;
    private int buildNumber;
    private int bulkSize;
    private long timeoutMs;

    @Override
    public Arguments getDefaultParameters() {
        Arguments parameters = new Arguments();
        parameters.addArgument(ES_SCHEME, "http");
        parameters.addArgument(ES_HOST, null);
        parameters.addArgument(ES_PORT, "9200");
        parameters.addArgument(ES_INDEX, null);
        parameters.addArgument(ES_TIMESTAMP, "yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        parameters.addArgument(ES_BULK_SIZE, "100");
        parameters.addArgument(ES_TIMEOUT_MS, Long.toString(DEFAULT_TIMEOUT_MS));
        parameters.addArgument(ES_SAMPLE_FILTER, null);
        parameters.addArgument(ES_TEST_MODE, "info");
        //TODO. In future version - add the support for TransportClient as well for the possibility to choose the ElasticSearch version
        //parameters.addArgument(ES_TRANSPORT_CLIENT, "false");
        //parameters.addArgument(ES_TRANSPORT_VERSION, "6.2.0");
        return parameters;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        try {
            String host = context.getParameter(ES_HOST);
            this.filters = new LinkedList<String>();
            int port = Integer.parseInt(context.getParameter(ES_PORT));
            this.index = context.getParameter(ES_INDEX);
            this.bulkSize = Integer.parseInt(context.getParameter(ES_BULK_SIZE));
            this.timeoutMs = JMeterUtils.getPropDefault(ES_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
            this.buildNumber  = (JMeterUtils.getProperty(ElasticsearchBackend.BUILD_NUMBER) != null && JMeterUtils.getProperty(ElasticsearchBackend.BUILD_NUMBER).trim() != "")
                                    ? Integer.parseInt(JMeterUtils.getProperty(ElasticsearchBackend.BUILD_NUMBER)) : 0;
            this.client = RestClient.builder(new HttpHost(context.getParameter(ES_HOST), port, context.getParameter(ES_SCHEME)))
                    .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000)
                            .setSocketTimeout((int) timeoutMs))
                    .setFailureListener(new RestClient.FailureListener() {
                        @Override
                        public void onFailure(HttpHost host) {
                            throw new IllegalStateException();
                        }
                    })
                    .setMaxRetryTimeoutMillis(60000)
                    .build();
            this.bulkRequestList = new LinkedList<String>();
            String[] filterArray = (context.getParameter(ES_SAMPLE_FILTER).contains(";")) ? context.getParameter(ES_SAMPLE_FILTER).split(";") : new String[] {context.getParameter(ES_SAMPLE_FILTER)};
            if(filterArray.length >= 1 && filterArray[0].trim() != "") {
                for (String filter : filterArray) {
                    this.filters.add(filter);
                }
            }
            super.setupTest(context);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to connect to the ElasticSearch engine", e);
        }
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {
        for(SampleResult sr : results) {
            boolean validSample = false;
            String sampleLabel = sr.getSampleLabel().toLowerCase().trim();

            if(this.filters.size() == 0) {
                validSample = true;
            } else {
                for(String filter : this.filters) {
                    if(filter.toLowerCase().trim().equals(sampleLabel) || sampleLabel.contains(filter.toLowerCase().trim())) {
                        validSample = true;
                        break;
                    }
                }
            }

            if(validSample) {
                Gson gson = new Gson();
                String json = gson.toJson(this.getElasticData(sr, context));
                this.bulkRequestList.add(json);
            }
        }

        if(this.bulkRequestList.size() >= this.bulkSize) {
            try {
                sendRequest(this.bulkRequestList);
            } catch (Exception e) {
                logger.error("Error occured while sending bulk request.", e);
            } finally {
                this.bulkRequestList.clear();
            }
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        if(this.bulkRequestList.size() > 0) {
            sendRequest(this.bulkRequestList);
        }
        IOUtils.closeQuietly(client);
        super.teardownTest(context);
    }
    
    private void sendRequest(List<String> bulkList) throws IOException {
        String actionMetaData = String.format("{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\" } }%n", this.index, "SampleResult");

        StringBuilder bulkRequestBody = new StringBuilder();
        for (String bulkItem : bulkList) {
            bulkRequestBody.append(actionMetaData);
            bulkRequestBody.append(bulkItem);
            bulkRequestBody.append("\n");
        }

        HttpEntity entity = new NStringEntity(bulkRequestBody.toString(), ContentType.APPLICATION_JSON);
        try {
            Response response = client.performRequest("POST", "/"+ this.index +"/SampleResult/_bulk", Collections.emptyMap(), entity);
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                if(logger.isErrorEnabled()) {
                    logger.error("ElasticSearch Backend Listener failed to write results for index {}", this.index);
                }
            }
        } catch (Exception e) {
            if(logger.isErrorEnabled()) {
                logger.error("ElasticSearch Backend Listener was unable to perform request to the ElasticSearch engine. Request reached timeout.");
            }
        }
    }

    public Map<String, Object> getElasticData(SampleResult sr, BackendListenerContext context) {
        HashMap<String, Object> jsonObject = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat(context.getParameter(ES_TIMESTAMP));

        //add all the default SampleResult parameters
        jsonObject.put("AllThreads", sr.getAllThreads());
        jsonObject.put("BodySize", sr.getBodySizeAsLong());
        jsonObject.put("Bytes", sr.getBytesAsLong());
        jsonObject.put("SentBytes", sr.getSentBytes());
        jsonObject.put("ConnectTime", sr.getConnectTime());
        jsonObject.put("ContentType", sr.getContentType());
        jsonObject.put("DataType", sr.getDataType());
        jsonObject.put("ErrorCount", sr.getErrorCount());
        jsonObject.put("GrpThreads", sr.getGroupThreads());
        jsonObject.put("IdleTime", sr.getIdleTime());
        jsonObject.put("Latency", sr.getLatency());
        jsonObject.put("ResponseTime", sr.getTime());
        jsonObject.put("SampleCount", sr.getSampleCount());
        jsonObject.put("SampleLabel", sr.getSampleLabel());
        jsonObject.put("StartTime", sdf.format(new Date(sr.getStartTime())));
        jsonObject.put("EndTime", sdf.format(new Date(sr.getEndTime())));
        jsonObject.put("ThreadName", sr.getThreadName());
        jsonObject.put("URL", sr.getURL());
        jsonObject.put("Timestamp", sdf.format(new Date(sr.getTimeStamp())));
        jsonObject.put("StartTimeInMs", sr.getStartTime());
        jsonObject.put("EndTimeInMs", sr.getEndTime());
        jsonObject.put("ElapsedTimeInMs", System.currentTimeMillis() - sr.getStartTime());
        jsonObject.put(ElasticsearchBackend.BUILD_NUMBER, this.buildNumber);

        // Add the details according to the mode that is set
        switch(context.getParameter(ES_TEST_MODE).trim()) {
            case "debug":
                jsonObject = addDetails(sr, jsonObject);
                break;
            case "info":
                if(!sr.isSuccessful()) {
                    jsonObject = addDetails(sr, jsonObject);
                }
                break;
            case "quiet":
                break;
            default:
                logger.warn("The parameter \"es.test.mode\" isn't set properly. Three modes are allowed: debug ,info, and quiet.");
                logger.warn(" -- \"debug\": sends request and response details to ElasticSearch. Info only sends the details if the response has an error.");
                logger.warn(" -- \"info\": should be used in production");
                logger.warn(" -- \"quiet\": should be used if you don't care to have the details.");
                break;
        }

        //all assertions
        AssertionResult[] assertionResults = sr.getAssertionResults();
        if(assertionResults != null) {
            Map<String, Object>[] assertionArray = new HashMap[assertionResults.length];
            Integer i = 0;
            for(AssertionResult assertionResult : assertionResults) {
                HashMap<String, Object> assertionMap = new HashMap<>();
                boolean failure = assertionResult.isFailure() || assertionResult.isError();
                assertionMap.put("failure", failure);
                assertionMap.put("failureMessage", assertionResult.getFailureMessage());
                assertionMap.put("name", assertionResult.getName());
                assertionArray[i] = assertionMap;
                i++;
            }
            jsonObject.put("AssertionResults", assertionArray);
        }

        // If built from Jenkins, add the hard-c                                                                                                                                                                                                                                                                                                                                                                                                                                                               oded version to be able to compare response time
        // of two builds over the elapsed time
        if(this.buildNumber != 0) {
            Date elapsedTimeComparison = getElapsedTime(true);
            if(elapsedTimeComparison != null)
                jsonObject.put("ElapsedTimeComparison", elapsedTimeComparison);
        }

        Date elapsedTime = getElapsedTime(false);
        if(elapsedTime != null)
            jsonObject.put("ElapsedTime", elapsedTime);
        jsonObject.put("ResponseCode", (sr.getResponseCode()));

        return jsonObject;
    }

    public HashMap<String, Object> addDetails(SampleResult sr, HashMap<String, Object> jsonObject) {
        jsonObject.put("RequestHeaders", sr.getRequestHeaders());
        jsonObject.put("ResponseHeaders", sr.getResponseHeaders());
        jsonObject.put("ResponseBody", sr.getResponseDataAsString());
        jsonObject.put("ResponseMessage", sr.getResponseMessage());
        return jsonObject;
    }

    public Date getElapsedTime(boolean forBuildComparison) {
        String sElapsed;
        //Calculate the elapsed time (Starting from midnight on a random day - enables us to compare of two loads over their duration)
        long start = JMeterContextService.getTestStartTime();
        long end = System.currentTimeMillis();
        long elapsed = (end - start);
        long minutes = (elapsed / 1000) / 60;
        long seconds = (elapsed / 1000) % 60;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); //If there is more than an hour of data, the number of minutes/seconds will increment this
        cal.set(Calendar.MINUTE, (int) minutes);
        cal.set(Calendar.SECOND, (int) seconds);

        if(forBuildComparison) {
            sElapsed = String.format("2017-01-01 %02d:%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND));
        } else {
            sElapsed = String.format("%s %02d:%02d:%02d",
                    DateTimeFormatter.ofPattern("yyyy-mm-dd").format(LocalDateTime.now()),
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND));
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        try {
            return formatter.parse(sElapsed);
        } catch (ParseException e) {
            logger.error("Unexpected error occured computing elapsed date", e);
            return null;
        }

    }
}
