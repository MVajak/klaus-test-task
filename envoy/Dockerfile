FROM envoyproxy/envoy:v1.12.2

COPY ./envoy.yml /etc/envoy/envoy.yml

CMD /usr/local/bin/envoy -c /etc/envoy/envoy.yml