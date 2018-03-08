## camel-kafka-elasticsearch

An implementation of [camel-ldp](https://github.com/trellis-ldp/camel-ldp) that consumes an activity stream from Kafka 
and writes the events to an Elasticsearch Index.

### Build
```bash
$ gradle build
$ gradle docker
```
### Configuration

* org.trellisldp.camel.kafka.elasticsearch.cfg

 ### Docker
 ```bash
 $ docker run -ti trellisldp/camel-kafka-elasticsearch
 ```
 
### Use With Trellis:
* Start [trellis-compose](https://github.com/trellis-ldp/trellis-deployment/blob/master/trellis-compose/docker-compose.yml) 

* Start [elasticsearch-compose](https://github.com/ub-leipzig/camel-kafka-elasticsearch/blob/master/docker-compose.yml)

### Kibana
* The Kibana interface can be accessed at `http://localhost:5601`

### Example Query
```bash
$ curl -v "http://localhost:9200/trellis/_search?q=object.type:http\:\/\/www.w3.org\/ns\/ldp#RDFSource"
```

### Elasticsearch Configuration
```bash
$ sudo sysctl -w vm.max_map_count=262144
```

### TODO
* route request to ACTIVITY_STREAM_OBJECT_ID, retrieve body as JSON-LD, and index resource property values.