FROM openjdk:11

COPY build/install/kotlin-messenger /opt/kotlin-messenger

EXPOSE 9999

VOLUME ["/opt/kotlin-messenger/data"]

CMD ["/opt/kotlin-messenger/bin/kotlin-messenger"]
