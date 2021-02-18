FROM java:8u111
VOLUME /tmp
ADD *.jar app.jar
COPY *.so /usr/lib/
EXPOSE 80
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9999","-jar","/app.jar"]
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime