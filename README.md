# jmeter-elasticsearch-backend-listener
JMeter plugin that lets you send sample results to an ElasticSearch engine to enable live monitoring of load tests.

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
