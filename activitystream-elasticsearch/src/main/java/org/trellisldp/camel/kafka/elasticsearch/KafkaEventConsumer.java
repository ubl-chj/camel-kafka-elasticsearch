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

import static org.apache.camel.LoggingLevel.INFO;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.camel.kafka.elasticsearch.ElasticsearchHighLevelClientImpl.getDocumentId;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.main.MainSupport;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.trellisldp.camel.ActivityStreamProcessor;

/**
 * KafkaEventConsumer.
 *
 * @author christopher-johnson
 */
public class KafkaEventConsumer {
    private static final Logger LOGGER = getLogger(KafkaEventConsumer.class);

    private static RestHighLevelClient client;

    /**
     * @param args args
     * @throws Exception Exception
     */
    public static void main(final String[] args) throws Exception {
        final KafkaEventConsumer kafkaConsumer = new KafkaEventConsumer();
        kafkaConsumer.init();
    }

    private void init() throws Exception {
        final Main main = new Main();
        main.addRouteBuilder(new KafkaEventRoute());
        main.addMainListener(new Events());
        main.run();
    }

    public static class Events extends MainListenerSupport {

        @Override
        public void afterStart(final MainSupport main) {
            System.out.println("KafkaEventConsumer with Camel is now started!");
        }

        @Override
        public void beforeStop(final MainSupport main) {
            System.out.println("KafkaEventConsumer with Camel is now being stopped!");
        }
    }

    public static class KafkaEventRoute extends RouteBuilder {

        /**
         * Configure the event route.
         */
        public void configure() {
            final PropertiesComponent pc = getContext().getComponent("properties", PropertiesComponent.class);
            pc.setLocation("classpath:cfg/org.trellisldp.camel.kafka.elasticsearch.cfg");

            from("kafka:{{consumer.topic}}?brokers={{kafka.host}}:{{kafka.port}}" + "&maxPollRecords={{consumer" + "" +
                    ".maxPollRecords}}" + "&consumersCount={{consumer.consumersCount}}" + "&seekTo={{consumer" + "" +
                    ".seekTo}}" + "&groupId={{consumer.group}}").routeId("kafkaConsumer")
                    .unmarshal()
                    .json(JsonLibrary.Jackson)
                    .process(new ActivityStreamProcessor())
                    .marshal()
                    .json(JsonLibrary.Jackson, true)
                    .log(INFO, LOGGER, "Serializing ActivityStreamMessage to JSONLD")
                    .setHeader("event.index.name", constant("{{event.index.name}}"))
                    .setHeader("event.index.type", constant("{{event.index.type}}"))
                    //.to("file://{{serialization.log}}");
                    .to("direct:index");
            from("direct:index").process(exchange -> {
                final String docId = getDocumentId();
                final IndexRequest request = new IndexRequest(exchange.getIn()
                        .getHeader("event.index.name")
                        .toString(), exchange.getIn()
                        .getHeader("event.index.type")
                        .toString(), docId);
                final String jsonString = exchange.getIn()
                        .getBody(String.class);
                LOGGER.debug("Indexing Message Body {}", jsonString);
                request.source(jsonString, XContentType.JSON);
                final IndexResponse indexResponse = client.index(request);
                LOGGER.info("Document ID {} Indexing Status: {}", docId, indexResponse.status());
            });
        }
    }
}