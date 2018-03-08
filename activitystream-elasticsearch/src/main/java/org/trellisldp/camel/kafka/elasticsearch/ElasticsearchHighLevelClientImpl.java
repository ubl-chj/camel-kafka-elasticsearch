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

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * @author christopher-johnson
 */
class ElasticsearchHighLevelClientImpl {

    private ElasticsearchHighLevelClientImpl() {
    }

    static String getDocumentId() {
        return UUID.randomUUID()
                   .toString();
    }

    static Boolean createIndexRequest(final RestHighLevelClient client) {
        final CreateIndexRequest request = new CreateIndexRequest("trellis");
        final String mapping = "{\"activity-stream\":{\"properties\":{\"@context\":{\"type\":\"text\"}," +
                "\"id\":{\"type\":\"text\"},\"type\":{\"type\":\"text\"}," +
                "\"object\":{\"properties\":{\"id\":{\"type\":\"text\"},\"type\":{\"type\":\"text\"}}}," +
                "\"published\":{\"type\":\"date\",\"format\":\"strict_date_optional_time||epoch_millis\"}}}}";
        request.mapping("activity-stream", mapping, XContentType.JSON);
        CreateIndexResponse createIndexResponse = null;
        try {
            createIndexResponse = client.indices()
                                        .create(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(createIndexResponse)
                      .isAcknowledged();
    }

    static Boolean deleteIndexRequest(final RestHighLevelClient client) {
        final DeleteIndexRequest request = new DeleteIndexRequest("trellis");
        DeleteIndexResponse deleteIndexResponse = null;
        try {
            deleteIndexResponse = client.indices()
                                        .delete(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(deleteIndexResponse)
                      .isAcknowledged();
    }
}
