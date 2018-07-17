package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import com.google.gson.Gson;
import org.apache.http.HttpHost;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ElasticsearchBackendClient extends AbstractBackendListenerClient {
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
    private static final String ES_INCLUDE_VARS     = "es.include.all.vars";
    private static final long DEFAULT_TIMEOUT_MS = 200L;
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchBackendClient.class);

    private ElasticSearchMetricSender sender;
    private Set<String> modes;
    private List<String> filters;
    private RestClient client;
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
        parameters.addArgument(ES_INCLUDE_VARS, "false");
        return parameters;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        try {
            this.filters = new LinkedList<String>();
            this.modes = new HashSet<>(Arrays.asList("info","debug","error","quiet"));
            this.bulkSize = Integer.parseInt(context.getParameter(ES_BULK_SIZE));
            this.timeoutMs = JMeterUtils.getPropDefault(ES_TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
            this.buildNumber = (JMeterUtils.getProperty(ElasticsearchBackendClient.BUILD_NUMBER) != null && JMeterUtils.getProperty(ElasticsearchBackendClient.BUILD_NUMBER).trim() != "") ? Integer.parseInt(JMeterUtils.getProperty(ElasticsearchBackendClient.BUILD_NUMBER)) : 0;
            this.client = RestClient.builder(new HttpHost(context.getParameter(ES_HOST), Integer.parseInt(context.getParameter(ES_PORT)), context.getParameter(ES_SCHEME)))
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
            this.sender = new ElasticSearchMetricSender(this.client, context.getParameter(ES_INDEX).toLowerCase());
            this.sender.createIndex();

            checkTestMode(context.getParameter(ES_TEST_MODE));
            
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
            ElasticSearchMetric metric = new ElasticSearchMetric(sr, context.getParameter(ES_TEST_MODE), context.getParameter(ES_TIMESTAMP), this.buildNumber);
            boolean validSample = false;
            String sampleLabel = sr.getSampleLabel().toLowerCase().trim();

            if(this.filters.size() == 0) {
                validSample = (context.getParameter(ES_TEST_MODE).trim().equals("error") && sr.isSuccessful()) ? false : true;
            } else {
                for(String filter : this.filters) {
                    if(filter.toLowerCase().trim().equals(sampleLabel) || sampleLabel.contains(filter.toLowerCase().trim())) {
                        validSample = (context.getParameter(ES_TEST_MODE).trim().equals("error") && sr.isSuccessful()) ? false : true;
                        break;
                    }
                }
            }

            if(validSample) {
                try {
                    this.sender.addToList(new Gson().toJson(metric.getMetric(context)));
                } catch (Exception e) {
                    logger.error("The ElasticSearch Backend Listener was unable to a sampler to the list of samplers to send... More info in JMeter's console.");
                    e.printStackTrace();
                }
            }

        }

        if(this.sender.getListSize() >= this.bulkSize) {
            try {
                this.sender.sendRequest();
            } catch (Exception e) {
                logger.error("Error occured while sending bulk request.", e);
            } finally {
                this.sender.clearList();
            }
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        if(this.sender.getListSize() > 0) {
            this.sender.sendRequest();
        }
        this.sender.closeConnection();
        super.teardownTest(context);
    }

    /**
     * This method checks if the test mode is valid
     * @param mode The test mode as String
     */
    private void checkTestMode(String mode) {
        if(!this.modes.contains(mode)) {
            logger.warn("The parameter \"es.test.mode\" isn't set properly. Three modes are allowed: debug ,info, and quiet.");
            logger.warn(" -- \"debug\": sends request and response details to ElasticSearch. Info only sends the details if the response has an error.");
            logger.warn(" -- \"info\": should be used in production");
            logger.warn(" -- \"quiet\": should be used if you don't care to have the details.");
        }
    }
}
