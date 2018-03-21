# Overview
### Description
JMeter ElasticSearch Backend Listener is a JMeter plugin enabling you to send test results to an ElasticSearch engine. It is meant as an alternative live-monitoring tool to the built-in "InfluxDB" backend listener of JMeter. 

### Features

* ElasticSearch low-level REST client
  * Using the low-level client makes the plugin compatible with any ElasticSearch version
* Bulk requests
  * By making bulk requests, there are practically no impacts on the performance of the tests themselves. 
* Filters
  * Only send the samples you want by using Filters! Simply type them as follows in the appropriate field : ``filter1;filter2;filter3`` or ``sampleLabel_must_contain_this``. *Leave it empty if you don't want any filter!
* Verbose, semi-verbose, and quiet mode
  * debug : Send request/response information of all requests (headers, body, etc.)
  * info : Only send the request/response information of failed requests (headers, body, etc.)
  * quiet : Only send the response time, bytes, and other metrics
* Use either Kibana or Grafana to vizualize your results!

### Maven
```xml
<dependency>
  <groupId>io.github.delirius325</groupId>
  <artifactId>jmeter.backendlistener.elasticsearch</artifactId>
  <version>2.2.2</version>
</dependency>
```

## Contributing
Feel free to contribute by branching and making pull requests, or simply by suggesting ideas through the "Issues" tab.

## Screenshots
### Configuration
![screnshot1](https://image.ibb.co/kDH3YH/Screen_Shot_2018_02_23_at_2_18_03_PM.png "Screenshot of configuration")
### Very simple example of Kibana dashboard
![screnshot1](https://image.ibb.co/bA1oYH/Screen_Shot_2018_02_23_at_2_37_46_PM.png "Example Kibana dashboard")

## Sample ElasticSearch Index

```javascript
{
    "template": "jmeter",
    "mappings": {
        "SampleResult": {
            "properties": {
                "AllThreads": {
                    "type": "long"
                },
                "Assertions": {
                    "properties": {
                        "Failure": {
                            "type": "boolean"
                        },
                        "FailureMessage": {
                            "type": "text",
                            "index": false
                        },
                        "Name": {
                            "type": "text",
                            "index": false
                        }
                    }
                },
                "BodySize": {
                    "type": "long"
                },
                "Bytes": {
                    "type": "long"
                },
                "ConnectTime": {
                    "type": "long",
                    "index": false
                },
                "ContentType": {
                    "type": "text",
                    "index": false
                },
                "DataType": {
                    "type": "text",
                    "index": false
                },
                "EndTime": {
                    "type": "date",
                    "format": "dateOptionalTime"
                },
                "ErrorCount": {
                    "type": "long"
                },
                "GrpThreads": {
                    "type": "long",
                    "index": false
                },
                "IdleTime": {
                    "type": "long"
                },
                "Latency": {
                    "type": "long"
                },
                "ResponseCode": {
                    "type": "text"
                },
                "ResponseMessage": {
                    "type": "text",
                    "index": false
                },
                "ResponseTime": {
                    "type": "long"
                },
                "SampleCount": {
                    "type": "long"
                },
                "SampleLabel": {
                    "type": "keyword"
                },
                "StartTime": {
                    "type": "date",
                    "format": "dateOptionalTime"
                },
                "Success": {
                    "type": "text",
                    "index": false
                },
                "ThreadName": {
                    "type": "keyword",
                    "index": false
                },
                "URL": {
                    "type": "keyword"
                },
                "Timestamp": {
                    "type": "date",
                    "format": "dateOptionalTime"
                },
                "NormalizedTimestamp": {
                    "type": "date",
                    "format": "dateOptionalTime",
                    "index": false
                },
                "BuildNumber": {
                  "type": "long"
                },
                "ElapsedTime": {
                  "type": "date"
                }
            }
        }
    }
}
```
