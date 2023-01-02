FROM eclipse-temurin:17.0.5_8-jre-alpine as build

ARG VERSION=""

ADD target/text2confl${VERSION}-dist/text2confl /opt/app

RUN find /opt/app -exec touch -t 200001010101 {} \;

FROM eclipse-temurin:17.0.5_8-jre-alpine

ARG PLANTUML="1.2022.14"

ADD https://github.com/plantuml/plantuml/releases/download/v$PLANTUML/plantuml.jar /usr/local/bin/plantuml.jar
ADD scripts/plantuml /usr/local/bin/plantuml
RUN chmod 755 /usr/local/bin/plantuml.jar /usr/local/bin/plantuml

RUN apk add --no-cache graphviz

COPY --from=build /opt/app/lib /opt/text2confl/lib
COPY --from=build /opt/app/app /opt/text2confl/app
COPY --from=build /opt/app/text2confl /opt/text2confl/text2confl

RUN adduser -DH user \
    && ln -s /opt/text2confl/text2confl /usr/local/bin/text2confl

USER user

ENTRYPOINT ["/opt/text2confl/text2confl"]
