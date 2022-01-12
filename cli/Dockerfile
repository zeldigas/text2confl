FROM openjdk:17-alpine3.14 as build

ADD target/cli-dist/text2confl /opt/app

RUN find /opt/app -exec touch -t 200001010101 {} \;

FROM openjdk:17-alpine3.14

COPY --from=build /opt/app/lib /opt/text2confl/lib
COPY --from=build /opt/app/app /opt/text2confl/app
COPY --from=build /opt/app/text2confl /opt/text2confl/text2confl

RUN adduser -DH user

USER user

ENTRYPOINT ["/opt/text2confl/text2confl"]
