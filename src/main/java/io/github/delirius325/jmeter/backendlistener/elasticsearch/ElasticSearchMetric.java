package io.github.delirius325.jmeter.backendlistener.elasticsearch;

import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchMetric {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchMetric.class);

    private SampleResult sampleResult;

    private String esTestMode;

    private String esTimestamp;

    private int ciBuildNumber;

    private HashMap<String, Object> json;

    private boolean allReqHeaders;

    private boolean allResHeaders;

    public ElasticSearchMetric(SampleResult sr, String testMode, String timeStamp, int buildNumber,
            boolean parseReqHeaders, boolean parseResHeaders) {
        this.sampleResult = sr;
        this.esTestMode = testMode.trim();
        this.esTimestamp = timeStamp.trim();
        this.ciBuildNumber = buildNumber;
        this.json = new HashMap<>();
        this.allReqHeaders = parseReqHeaders;
        this.allResHeaders = parseResHeaders;
    }

    /**
     * This method returns the current metric as a Map(String, Object) for the provided sampleResult
     * 
     * @param context
     *            BackendListenerContext
     * @return a JSON Object as Map(String, Object)
     */
    public Map<String, Object> getMetric(BackendListenerContext context) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(this.esTimestamp);

        //add all the default SampleResult parameters
        this.json.put("AllThreads", this.sampleResult.getAllThreads());
        this.json.put("BodySize", this.sampleResult.getBodySizeAsLong());
        this.json.put("Bytes", this.sampleResult.getBytesAsLong());
        this.json.put("SentBytes", this.sampleResult.getSentBytes());
        this.json.put("ConnectTime", this.sampleResult.getConnectTime());
        this.json.put("ContentType", this.sampleResult.getContentType());
        this.json.put("DataType", this.sampleResult.getDataType());
        this.json.put("ErrorCount", this.sampleResult.getErrorCount());
        this.json.put("GrpThreads", this.sampleResult.getGroupThreads());
        this.json.put("IdleTime", this.sampleResult.getIdleTime());
        this.json.put("Latency", this.sampleResult.getLatency());
        this.json.put("ResponseTime", this.sampleResult.getTime());
        this.json.put("SampleCount", this.sampleResult.getSampleCount());
        this.json.put("SampleLabel", this.sampleResult.getSampleLabel());
        this.json.put("ThreadName", this.sampleResult.getThreadName());
        this.json.put("URL", this.sampleResult.getURL());
        this.json.put("ResponseCode", this.sampleResult.getResponseCode());
        this.json.put("StartTime", sdf.format(new Date(this.sampleResult.getStartTime())));
        this.json.put("EndTime", sdf.format(new Date(this.sampleResult.getEndTime())));
        this.json.put("Timestamp", sdf.format(new Date(this.sampleResult.getTimeStamp())));
        this.json.put("InjectorHostname", InetAddress.getLocalHost().getHostName());

        // Add the details according to the mode that is set
        switch (this.esTestMode) {
        case "debug":
            addDetails();
            break;
        case "error":
            addDetails();
            break;
        case "info":
            if (!this.sampleResult.isSuccessful())
                addDetails();
            break;
        default:
            break;
        }

        addAssertions();
        addElapsedTime(sdf);
        addCustomFields(context);
        parseHeadersAsJsonProps(this.allReqHeaders, this.allResHeaders);

        return this.json;
    }

    /**
     * This method adds all the assertions for the current sampleResult
     *
     */
    private void addAssertions() {
        AssertionResult[] assertionResults = this.sampleResult.getAssertionResults();
        if (assertionResults != null) {
            Map<String, Object>[] assertionArray = new HashMap[assertionResults.length];
            Integer i = 0;
            for (AssertionResult assertionResult : assertionResults) {
                HashMap<String, Object> assertionMap = new HashMap<>();
                boolean failure = assertionResult.isFailure() || assertionResult.isError();
                assertionMap.put("failure", failure);
                assertionMap.put("failureMessage", assertionResult.getFailureMessage());
                assertionMap.put("name", assertionResult.getName());
                assertionArray[i] = assertionMap;
                i++;
            }
            this.json.put("AssertionResults", assertionArray);
        }
    }

    /**
     * This method adds the ElapsedTime as a key:value pair in the JSON object. Also, depending on whether or not the
     * tests were launched from a CI tool (i.e Jenkins), it will add a hard-coded version of the ElapsedTime for results
     * comparison purposes
     *
     * @param sdf
     *            SimpleDateFormat
     */
    private void addElapsedTime(SimpleDateFormat sdf) {
        Date elapsedTime;

        if (this.ciBuildNumber != 0) {
            elapsedTime = getElapsedTime(true);
            this.json.put("BuildNumber", this.ciBuildNumber);

            if (elapsedTime != null)
                this.json.put("ElapsedTimeComparison", sdf.format(elapsedTime));
        }

        elapsedTime = getElapsedTime(false);
        if (elapsedTime != null)
            this.json.put("ElapsedTime", sdf.format(elapsedTime));
    }

    /**
     * Methods that add all custom fields added by the user in the Backend Listener's GUI panel
     *
     * @param context
     *            BackendListenerContext
     */
    private void addCustomFields(BackendListenerContext context) {
        Iterator<String> pluginParameters = context.getParameterNamesIterator();
        while (pluginParameters.hasNext()) {
            String parameterName = pluginParameters.next();

            if (!parameterName.startsWith("es.") && !context.getParameter(parameterName).trim().equals("")) {
                String parameter = context.getParameter(parameterName).trim();

                try {
                    this.json.put(parameterName, Long.parseLong(parameter));
                } catch (Exception e) {
                    if (logger.isDebugEnabled())
                        logger.debug("Cannot convert custom field to number");
                    this.json.put(parameterName, context.getParameter(parameterName).trim());
                }
            }
        }
    }

    /**
     * Method that adds the request and response's body/headers
     *
     */
    private void addDetails() {
        this.json.put("RequestHeaders", this.sampleResult.getRequestHeaders());
        this.json.put("RequestBody", this.sampleResult.getSamplerData());
        this.json.put("ResponseHeaders", this.sampleResult.getResponseHeaders());
        this.json.put("ResponseBody", this.sampleResult.getResponseDataAsString());
        this.json.put("ResponseMessage", this.sampleResult.getResponseMessage());
    }

    /**
     * This method will parse the headers and look for custom variables passed through as header. It can also seperate
     * all headers into different ElasticSearch document properties by passing "true" This is a work-around the native
     * behaviour of JMeter where variables are not accessible within the backend listener.
     *
     * @param allReqHeaders
     *            boolean to determine if the user wants to separate ALL request headers into different ES JSON
     *            properties.
     * @param allResHeaders
     *            boolean to determine if the user wants to separate ALL response headers into different ES JSON
     *            properties.
     *
     *            NOTE: This will be fixed as soon as a patch comes in for JMeter to change the behaviour.
     */
    private void parseHeadersAsJsonProps(boolean allReqHeaders, boolean allResHeaders) {
        LinkedList<String[]> headersArrayList = new LinkedList<String[]>();

        if (allReqHeaders) {
            headersArrayList.add(this.sampleResult.getRequestHeaders().split("\n"));
        }

        if (allResHeaders) {
            headersArrayList.add(this.sampleResult.getResponseHeaders().split("\n"));
        }

        for (String[] lines : headersArrayList) {
            for (int i = 0; i < lines.length; i++) {
                String[] header = lines[i].split(":");

                // if not all req headers and header contains special X-tag
                if (header.length > 1) {
                    if (!this.allReqHeaders && header[0].startsWith("X-es-backend")) {
                        this.json.put(header[0].replaceAll("es-", "").trim(), header[1].trim());
                    } else {
                        this.json.put(header[0].replaceAll("es-", "").trim(), header[1].trim());
                    }
                }
            }
        }
    }

    /**
     * This method is meant to return the elapsed time in a human readable format. The purpose of this is mostly for
     * build comparison in Kibana. By doing this, the user is able to set the X-axis of his graph to this date and split
     * the series by build numbers. It allows him to overlap test results and see if there is regression or not.
     *
     * @param forBuildComparison
     *            boolean to determine if there is CI (continuous integration) or not
     * @return The elapsed time in YYYY-MM-dd HH:mm:ss format
     */
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

        if (forBuildComparison) {
            sElapsed = String.format("2017-01-01 %02d:%02d:%02d", cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        } else {
            sElapsed = String.format("%s %02d:%02d:%02d",
                    DateTimeFormatter.ofPattern("yyyy-mm-dd").format(LocalDateTime.now()),
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
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
