# Overview
### Description
JMeter ElasticSearch Backend Listener is a JMeter plugin enabling you to send test results to an ElasticSearch engine. It is meant as an alternative live-monitoring tool to the built-in "InfluxDB" backend listener of JMeter. 

### Features

* The plugin uses the ElasticSearch low-level REST client meaning that it works with any version of ElasticSearch.
* The plugin makes use of ElasticSearch's ability to make bulk requests. 
By doing this, we limit the amount of HTTP calls made to the ES engine, therefore minimally impacting the tests performance.
* Only send the samples you want by using Filters! Simply type them as follows in the appropriate field : ``filter1;filter2;filter3``. *Leave it empty if you don't want any filter!
* You can use either Kibana or Grafana to vizualize your results!

## Contributing
Feel free to contribute by branching and making pull requests, or simply by suggesting ideas through the "Issues" tab.

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
