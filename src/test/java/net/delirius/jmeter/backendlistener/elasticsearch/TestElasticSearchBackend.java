package net.delirius.jmeter.backendlistener.elasticsearch;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class TestElasticSearchBackend {
    private DummyElasticSearchBackend instance = null;
    private JMeterContext context;

    class DummyElasticSearchBackend extends ElasticsearchBackend {
        public DummyElasticSearchBackend() throws Exception {
            super();
        }
    }

    @Before
    public void setUp() throws Exception {
        this.context = JMeterContextService.getContext();
        this.instance = new DummyElasticSearchBackend();
    }

    @Test
    public void testGetElapsedTime() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-mm-dd HH:mm:ss");
        Date testDate = this.instance.getElapsedDate();
        assertNotNull("testDate = " + sdf.format(testDate), sdf.format(testDate));
    }
}
