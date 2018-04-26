FROM maprtech/pacc:6.0.0_4.0.0_centos7

# ElasticSearch
EXPOSE 9300

# Create a directory for MapR Application and copy the Application
RUN mkdir -p /usr/share/mapr-apps/ojai-search-plugin

COPY ./search-plugin/target/ojai-search-plugin.jar /usr/share/mapr-apps/ojai-search-plugin/

COPY ./bin/run.sh /usr/share/mapr-apps/ojai-search-plugin/run.sh
RUN chmod +x /usr/share/mapr-apps/ojai-search-plugin/run.sh

# Install ElasticSeach
WORKDIR /usr/share/mapr-apps/ojai-search-plugin/
RUN curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.6.1.tar.gz
RUN tar -xvf elasticsearch-5.6.1.tar.gz
RUN echo 'http.cors.enabled: true' >> elasticsearch-5.6.1/config/elasticsearch.yml
RUN echo 'http.cors.allow-origin: "*"' >> elasticsearch-5.6.1/config/elasticsearch.yml

CMD ["/usr/share/mapr-apps/ojai-search-plugin/run.sh"]
