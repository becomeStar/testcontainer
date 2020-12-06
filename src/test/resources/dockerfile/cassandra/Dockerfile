FROM cassandra:3.11.2

ENV CASSANDRA_KEYSPACE test
ENV TABLE item

COPY entrypoint-wrap.sh /entrypoint-wrap.sh
RUN ["chmod", "+x", "/entrypoint-wrap.sh"]

ENTRYPOINT ["/entrypoint-wrap.sh"]

CMD ["cassandra", "-f"]