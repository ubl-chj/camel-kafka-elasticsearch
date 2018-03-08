FROM openjdk:8-jre

MAINTAINER Christopher Johnson <christopher_hanna.johnson@uni-leipzig.de>
LABEL description = "Provides a Kafka Stream Indexer"

ENV INDEXER_RUNTIME /opt

WORKDIR ${INDEXER_RUNTIME}
COPY *.tar .
RUN tar -xvf *.tar && \
mv activitystream-elasticsearch-${version} activitystream-elasticsearch && \
chmod +x ${INDEXER_RUNTIME}/activitystream-elasticsearch/bin/activitystream-elasticsearch
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh
ENTRYPOINT [ "./entrypoint.sh" ]