package io.github.delirius325.jmeter.backendlistener.elasticsearch;

public class ElasticSearchRequests {
    /**
     * Request to send metrics (JMeter/Percentiles) as ElasticSearch documents
     */
    public static String SEND_BULK_REQUEST = "{ \"index\" : { \"_index\" : \"%s\" } }%n";
}
