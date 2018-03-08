/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trellisldp.camel.kafka.elasticsearch;

import static java.util.stream.Collectors.toList;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.builder.PredicateBuilder.in;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_ID;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_TYPE;
import static org.trellisldp.camel.kafka.elasticsearch.ElasticsearchHighLevelClientImpl.getDocumentId;
import static org.trellisldp.camel.kafka.elasticsearch.ProcessorUtils.tokenizePropertyPlaceholder;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trellisldp.camel.ActivityStreamProcessor;

/**
 * org.trellisldp.camel.kafka.elasticsearch.KafkaEventConsumerTest.
 *
 * @author christopher-johnson
 */
public final class KafkaEventConsumerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumerTest.class);

    private static final String HTTP_ACCEPT = "Accept";

    private static RestHighLevelClient client;

    private KafkaEventConsumerTest() {
    }

    private static void initAll() {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    public static void main(final String[] args) throws Exception {
        initAll();
        LOGGER.info("About to run Kafka-camel integration...");

        final CamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new RouteBuilder() {
            public void configure() {
                final PropertiesComponent pc = getContext().getComponent("properties", PropertiesComponent.class);
                pc.setLocation("classpath:application.properties");

                LOGGER.info("About to start route: Kafka Server -> Log ");

                from("kafka:{{consumer.topic}}?brokers={{kafka.host}}:{{kafka.port}}" + "&maxPollRecords={{consumer"
                        + ".maxPollRecords}}" + "&consumersCount={{consumer.consumersCount}}" + "&seekTo={{consumer"
                        + ".seekTo}}" + "&groupId={{consumer.group}}").routeId("FromKafka")
                        .routeId("KafkaConsume")
                        .unmarshal()
                        .json(JsonLibrary.Jackson)
                        .process(new ActivityStreamProcessor())
                        .marshal()
                        .json(JsonLibrary.Jackson, true)
                        .log(INFO, LOGGER, "Serializing ActivityStreamMessage to JSONLD")
                        .setHeader("event.index.name", constant("{{event.index.name}}"))
                        .setHeader("event.index.type", constant("{{event.index.type}}"))
                        .setHeader("document.index.name", constant("{{document.index.name}}"))
                        .setHeader("document.index.type", constant("{{document.index.type}}"))
                        //.to("file://{{serialization.log}}");
                        .to("direct:get");
                from("direct:get").routeId("DocumentGet")
                        .choice()
                        .when(in(tokenizePropertyPlaceholder(getContext(), "{{indexable.types}}", ",").stream()
                                .map(type -> header(ACTIVITY_STREAM_OBJECT_TYPE).contains(type))
                                .collect(toList())))
                        .setHeader(HTTP_METHOD)
                        .constant("GET")
                        .setHeader(HTTP_URI)
                        .header(ACTIVITY_STREAM_OBJECT_ID)
                        .setHeader(HTTP_ACCEPT)
                        .constant("application/ld+json")
                        .to("http4://localhost")
                        .convertBodyTo(String.class)
                        .process(exchange -> {
                            final String jsonString = exchange.getIn()
                                    .getBody(String.class);
                            LOGGER.debug("Getting Document Resource {}", jsonString);
                        })
                        .to("direct:docIndex")
                        .otherwise()
                        .to("direct:eventIndex");
                from("direct:docIndex").routeId("DocIndex")
                        .process(exchange -> {
                            final String docId = getDocumentId();
                            final IndexRequest request = new IndexRequest(exchange.getIn()
                                    .getHeader("document.index.name")
                                    .toString(), exchange.getIn()
                                    .getHeader("document.index.type")
                                    .toString(), docId);
                            final String jsonString = exchange.getIn()
                                    .getBody(String.class);
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            XContentParser parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry
                                    .EMPTY, jsonString);
                            builder.startObject();
                            builder.field("web-anno");
                            builder.copyCurrentStructure(parser);
                            builder.endObject();
                            LOGGER.debug("Indexing Document Body {}", jsonString);
                            request.source(builder);
                            final IndexResponse indexResponse = client.index(request);
                            LOGGER.info("Document ID {} Indexing Status: {}", docId, indexResponse.status());
                        });
                from("direct:eventIndex").routeId("EventIndex")
                        .process(exchange -> {
                            final String docId = getDocumentId();
                            final IndexRequest request = new IndexRequest(exchange.getIn()
                                    .getHeader("event.index.name")
                                    .toString(), exchange.getIn()
                                    .getHeader("event.index.type")
                                    .toString(), docId);
                            final String jsonString = exchange.getIn()
                                    .getBody(String.class);
                            LOGGER.debug("Indexing Event Body {}", jsonString);
                            request.source(jsonString, XContentType.JSON);
                            final IndexResponse indexResponse = client.index(request);
                            LOGGER.info("Event ID {} Indexing Status: {}", docId, indexResponse.status());
                        });
            }
        });
        camelContext.start();

        // let it run for 5 minutes before shutting down
        Thread.sleep(5 * 60 * 1000);

        camelContext.stop();
    }
}