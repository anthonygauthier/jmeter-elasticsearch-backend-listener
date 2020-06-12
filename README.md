[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8a2f2a06171248acb6411a2d870558c8)](https://app.codacy.com/app/antho325/jmeter-elasticsearch-backend-listener?utm_source=github.com&utm_medium=referral&utm_content=delirius325/jmeter-elasticsearch-backend-listener&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/delirius325/jmeter-elasticsearch-backend-listener.svg?branch=master)](https://travis-ci.org/delirius325/jmeter-elasticsearch-backend-listener)


# Overview
### Description
JMeter ElasticSearch Backend Listener is a JMeter plugin enabling you to send test results to an ElasticSearch engine. It is meant as an alternative live-monitoring tool to the built-in "InfluxDB" backend listener of JMeter. 

### Features

* ElasticSearch low-level REST client
  * Using the low-level client makes the plugin compatible with any ElasticSearch version
* X-Pack Authentication!
  * Just supply your crendentials in the specified fields!
* Bulk requests
  * By making bulk requests, there are practically no impacts on the performance of the tests themselves. 
* Filters
  * Only send the samples you want by using Filters! Simply type them as follows in the field ``es.sample.filter`` : ``filter1;filter2;filter3`` or ``sampleLabel_must_contain_this``.
  * You can also choose to exclude certain samplers; `!!exclude_this;filter1;filter2`
* Specific fields ```field1;field2;field3`
  * Specify fields that you want to send to ElasticSearch (possible fields below)
     * AllThreads
     * BodySize
     * Bytes
     * SentBytes
     * ConnectTime
     * ContentType
     * DataType
     * ErrorCount
     * GrpThreads
     * IdleTime
     * Latency
     * ResponseTime
     * SampleCount
     * SampleLabel
     * ThreadName
     * URL
     * ResponseCode
     * TestStartTime
     * SampleStartTime
     * SampleEndTime
     * Timestamp
     * InjectorHostname
* Verbose, semi-verbose, error only, and quiet mode
  * __debug__ : Send request/response information of all samplers (headers, body, etc.)
  * __info__ : Sends all samplers to the ElasticSearch engine, but only sends the headers, body info for the failed samplers.
  * __quiet__ : Only sends the response time, bytes, and other metrics
  * __error__ : Only sends the failing samplers to the ElasticSearch engine (Along with their headers and body information).
* Use either Kibana or Grafana to vizualize your results!
  * [Click here to get a sample Grafana dashboard!](https://github.com/delirius325/jmeter-elasticsearch-backend-listener/wiki/JMeter-Generic-Dashboard) - All you need to do is import it into Grafana and change the data source!
* Continuous Integration support - [Build comparison!](https://github.com/delirius325/jmeter-elasticsearch-backend-listener/wiki/Continuous-Integration---Build-Comparison)
* Send JMeter variables to ElasticSearch! [Refer to this for more info!](https://github.com/delirius325/jmeter-elasticsearch-backend-listener/wiki/Sending-JMeter-variables) 
* New AWS ES parameters introducted in 2.6.0 version which leverage Role based authentication to access Elastic Search managed hosting on AWS

### Maven
```xml
<dependency>
  <groupId>io.github.delirius325</groupId>
  <artifactId>jmeter.backendlistener.elasticsearch</artifactId>
  <version>2.6.10-SNAPSHOT</version>
</dependency>
```

## Contributing
Feel free to contribute by branching and making pull requests, or simply by suggesting ideas through the "Issues" tab.

### Packaging and testing your newly added code
Execute below mvn command. Make sure JAVA_HOME is set properly
```
mvn package
```
Move the resulting JAR to your `JMETER_HOME/lib/ext`.

## Screenshots
### Configuration
![screnshot1](https://cdn-images-1.medium.com/max/2000/1*iVb7mIp2dPg7zE4Ph3PrGQ.png "Screenshot of configuration")

### Sample Grafana dashboard
![screnshot1](https://image.ibb.co/jW6LNx/Screen_Shot_2018_03_21_at_10_21_18_AM.png "Sample Grafana Dashboard")

### For more info
For more information, here's a little [documentation](https://github.com/delirius325/jmeter-elasticsearch-backend-listener/wiki).
