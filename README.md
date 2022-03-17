# Reader Output and Notification API

## Overview

This application provides an API that can be used to integrate DART with external NLP systems. DART itself publishes notifications as documents are updated in the system for external systems to listen to. After processing DART data the external system can upload it's output along with metadata via this service. This API includes a search interface that allows systems to browse through ouputs based on the metadata (reader id, reader version, ontology version, etc...). Retrieving the original output is also possible.

This interface also provides functionality for implementing multi-stage NLP pipelines. When an external system uploads new output an optional notification can be published to indicate to downstream systems of the new data. This notification is configurable and supports notifications via Kafka (by default) and REST.

## Dependencies
DART is a Scala/Java application, and as a result has dependencies on other Java libraries developed by DART. In order to build DART these dependencies must be accessible via the local filesystem (in the SBT cache) or over the network via an artifact repository such as [Sonatype Nexus](https://www.sonatype.com/products/repository-oss-download) or [JFrog Artifactory](https://jfrog.com/artifactory). DART requires the following dependencies to be built/installed

| Group ID                       | Artifact ID                     |
|--------------------------------|---------------------------------|
| com.twosixlabs.dart            | dart-aws_2.12                   |
| com.twosixlabs.dart            | dart-cli_2.12                   |
| com.twosixlabs.dart            | dart-utils_2.12                 |
| com.twosixlabs.dart            | dart-json_2.12                  |
| com.twosixlabs.dart            | dart-test-base_2.12             |
| com.twosixlabs.dart            | dart-arangodb-datastore_2.12    |
| com.twosixlabs.dart            | embedded-text-utils_2.12        |
| com.twosixlabs.dart            | dart-auth-commons_2.12          |
| com.twosixlabs.cdr4s           | cdr4s-core_2.12                 |
| com.twosixlabs.cdr4s           | cdr4s-ladle-json_2.12           |
| com.twosixlabs.cdr4s           | cdr4s-dart-json_2.12            |
| com.twosixlabs.dart.operations | status-client_2.12              |
| com.twosixlabs.dart.ontologies | ontology-registry-api_2.12      |
| com.twosixlabs.dart.ontologies | ontology-registry-services_2.12 |
| com.twosixlabs.dart.rest       | dart-scalatra-commons_2.12      |

## Building
This project is built using SBT. For more information on installation and configuration of SBT please [see their documentation](https://www.scala-sbt.org/1.x/docs/)

To build and test the code:
```bash
sbt clean test
````

To create a runnable JAR:
```bash
sbt clean package
```

To create a Docker image of the runnable application:
```bash
make docker-build
```


## Environment Variables
| Name                     | Description                                                                            | Example Values                            |
|--------------------------|----------------------------------------------------------------------------------------|-------------------------------------------|
| KAFKA_BOOTSTRAP_SERVERS  | Connection string to the Kafka broker                                                  | `dart.kafka.com:19092`                    |
| REST_BOOTSTRAP_SERVERS   | the url for sending REST notifications (alias of NOTIFICATION_REST_URLS)               | `http://dart.external:9090/output/notify` |
| DRAGONS_HOARD_PORT       | primary http/https port for accessing the API                                          | `8080`                                    |
| PERSISTENCE_MODE         | configures how the output is stored (ie: AWS S3, local filesystem, etc...)             | `aws`                                     |
| PERSISTENCE_BUCKET_NAME  | if the persistence mode is S3 this is the name of the S3 bucket                        | `dart-reader-output-bucket`               |
| PERSISTENCE_DIR          | if the persistence mode is the local filesystem this is the directory to store outputs | `/opt/app/data/`                          |
| NOTIFICATION_KAFKA_TOPIC | the name of the Kafka topic that output notifications are pushed to                    | `dart.reader.output.notifications`        |
| NOTIFICATION_REST_URLS   | the list of REST endpoints that output notifications are pushed to                     | `http://dart.external:9090/output/notify` |
| ARANGODB_HOST            | the hostname where the primary DART datastore is available                             | `dart.datastore`                          |
| ARANGODB_PORT            | the port number for connecting to the DART datastore                                   | `8529`                                    |
| AWS_CREDENTIALS_PROVIDER | specifies how the application will resolve AWS credentials                             | `INSTANCE`                                |
| FILE_STREAM_THRESHOLD_MB | file size limit for determining if the file upload should be streamed                  | `100000`                                  |
| POSTGRES_HOST            | hostname of the SQL datastore                                                          | `dart.sql.datastore`                      |
| POSTGRES_PORT            | port of the SQL datastore                                                              | `5432`                                    |
| POSTGRES_DB              | the name of the database used for DART                                                 | `dart_db`                                 |
| POSTGRES_USER            | username of the SQL datastore                                                          | `dart`                                    |
| POSTGRES_PASSWORD        | password of the SQL datastore                                                          | `dartsqlpassword`                         |
| DART_AUTH_SECRET         | path to the secret file used for authentication/authorization with Keycloak            | `/opt/app/secret`                         |
| DART_AUTH_BYPASS         | flag for if authentication/authorization should be enabled                             | `true`                                    |
