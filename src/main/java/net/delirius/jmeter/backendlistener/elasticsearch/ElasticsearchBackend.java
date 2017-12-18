package net.delirius.jmeter.backendlistener.elasticsearch;

import org.apache.commons.lang.StringUtils;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author: Delirius325
 * @inspired_by: korteke & zumo64
 * @source_1: https://github.com/korteke/JMeter_ElasticsearchBackendListener
 * @source_2: https://github.com/zumo64/ELK_POC
 */
public class ElasticsearchBackend extends AbstractBackendListenerClient {
    private static final String ES_PROTOCOL     = "es.protocol";
    private static final String ES_HOST         = "es.host";
    private static final String ES_PORT         = "es.transport.port";
    private static final String ES_INDEX        = "es.index";
    private static final String ES_INDEX_TYPE   = "es.indexType";
    private static final String ES_TIMESTAMP    = "es.timestamp";
    private static final String ES_STATUS_CODE  = "es.status.code";
    private static final String ES_CLUSTER      = "es.cluster";
    private static final String ES_BULK_SIZE    = "es.bulk.size";

    private Client client;
    private Settings settings;
    private String index;
    private String indexType;
    private String host;
    private Integer port;
    private Integer buildNumber;
    private Integer bulkSize;
    private BulkRequestBuilder bulkRequest;

    @Override
    public Arguments getDefaultParameters() {
        Arguments parameters = new Arguments();
        parameters.addArgument(ES_PROTOCOL, "https");
        parameters.addArgument(ES_HOST, null);
        parameters.addArgument(ES_PORT, "9300");
        parameters.addArgument(ES_INDEX, null);
        parameters.addArgument(ES_INDEX_TYPE, "SampleResult");
        parameters.addArgument(ES_TIMESTAMP, "yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        parameters.addArgument(ES_STATUS_CODE, "531");
        parameters.addArgument(ES_CLUSTER, "elasticsearch");
        parameters.addArgument(ES_BULK_SIZE, "100");
        return parameters;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        try {
            this.index        = context.getParameter(ES_INDEX);
            this.indexType    = context.getParameter(ES_INDEX_TYPE);
            this.host         = context.getParameter(ES_HOST);
            this.bulkSize     = Integer.parseInt(context.getParameter(ES_BULK_SIZE));
            this.port         = Integer.parseInt(context.getParameter(ES_PORT));
            this.buildNumber  = (JMeterUtils.getProperty("BuildNumber") != null && JMeterUtils.getProperty("BuildNumber").trim() != "") ? Integer.parseInt(JMeterUtils.getProperty("BuildNumber")) : 0;
            this.settings     = Settings.builder().put("cluster.name", context.getParameter(ES_CLUSTER)).build();
            this.client       = new PreBuiltTransportClient(this.settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(this.host), this.port));
            this.bulkRequest  = this.client.prepareBulk();
            super.setupTest(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {
        for(SampleResult sr : results) {
            this.bulkRequest.add(this.client.prepareIndex(this.index, this.indexType).setSource(this.getElasticData(sr, context), XContentType.JSON));
        }

        if(this.bulkRequest.numberOfActions() >= this.bulkSize) {
            this.bulkRequest.get();
            this.bulkRequest = this.client.prepareBulk();
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        if(this.bulkRequest.numberOfActions() > 0)
            this.bulkRequest.get();

        this.client.close();
        super.teardownTest(context);
    }

    public HashMap<String, Object> getElasticData(SampleResult sr, BackendListenerContext context) {
        HashMap<String, Object> jsonObject = new HashMap<String, Object>();
        SimpleDateFormat sdf = new SimpleDateFormat(context.getParameter(ES_TIMESTAMP));

        //add all the default SampleResult parameters
        jsonObject.put("AllThreads", sr.getAllThreads());
        jsonObject.put("BodySize", sr.getBodySize());
        jsonObject.put("Bytes", sr.getBytes());
        jsonObject.put("ConnectTime", sr.getConnectTime());
        jsonObject.put("ContentType", sr.getContentType());
        jsonObject.put("DataType", sr.getDataType());
        jsonObject.put("ErrorCount", sr.getErrorCount());
        jsonObject.put("GrpThreads", sr.getGroupThreads());
        jsonObject.put("IdleTime", sr.getIdleTime());
        jsonObject.put("Latency", sr.getLatency());
        jsonObject.put("ResponseTime", (sr.getEndTime() - sr.getStartTime()));
        jsonObject.put("SampleCount", sr.getSampleCount());
        jsonObject.put("SampleLabel", sr.getSampleLabel());
        jsonObject.put("StartTime", sdf.format(new Date(sr.getStartTime())));
        jsonObject.put("EndTime", sdf.format(new Date(sr.getEndTime())));
        jsonObject.put("ThreadName", sr.getThreadName());
        jsonObject.put("URL", sr.getURL());
        jsonObject.put("Timestamp", sdf.format(new Date(sr.getTimeStamp())));
        jsonObject.put("BuildNumber", this.buildNumber);
        jsonObject.put("ElapsedTime", getElapsedDate());
        jsonObject.put("ResponseCode", (sr.isResponseCodeOK() && StringUtils.isNumeric(sr.getResponseCode())) ? sr.getResponseCode() : context.getParameter(ES_STATUS_CODE));

        //all assertions
        AssertionResult[] assertionResults = sr.getAssertionResults();
        if(assertionResults != null) {
            HashMap<String, Object> [] assertionArray = new HashMap[assertionResults.length];
            Integer i = 0;
            for(AssertionResult assertionResult : assertionResults) {
                HashMap<String, Object> assertionMap = new HashMap<String, Object>();
                boolean failure = assertionResult.isFailure() || assertionResult.isError();
                assertionMap.put("failure", failure);
                assertionMap.put("failureMessage", assertionResult.getFailureMessage());
                assertionMap.put("name", assertionResult.getName());
                assertionArray[i] = assertionMap;
                i++;
            }
        }

        return jsonObject;
    }

    public Date getElapsedDate() {
        //Calculate the elapsed time (Starting from midnight on a random day - enables us to compare of two loads over their duration)
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("YYYY-mm-dd HH:mm:ss");
            long start = JMeterContextService.getTestStartTime();
            long end = System.currentTimeMillis();
            long elapsed = (end - start);
            long minutes = (elapsed / 1000) / 60;
            long seconds = (elapsed / 1000) % 60;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); //If there is more than an hour of data, the number of minutes/seconds will increment this
            cal.set(Calendar.MINUTE, (int) minutes);
            cal.set(Calendar.SECOND, (int) seconds);
            String sElapsed = String.format("2017-01-01 %02d:%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
            Date elapsedDate = formatter.parse(sElapsed);
            return elapsedDate;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
