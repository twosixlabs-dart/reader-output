import sbt._

object Dependencies {
    // logging
    val slf4jVersion = "1.7.20"
    val logbackVersion = "1.2.3"

    // files
    val betterFilesVersion = "3.8.0"

    // webserver
    val scalatraVersion = "2.7.1"
    val jettyWebappVersion = "9.4.18.v20190429"
    val servletApiVersion = "3.1.0"
    val okhttpVersion = "4.1.0"

    // kafka
    val kafkaVersion = "2.2.1"
    val embeddedKafkaVersion = "2.2.0"

    // testing
    val scalaMockVersion = "4.2.0"
    val scalaTestVersion = "3.1.4"
    val embeddedPostgresVersion = "1.2.10"
    val mockitoVersion = "1.16.0"
    val s3MockVersion = "0.2.6"


    // database
    val postgresVersion = "42.2.10"
    val h2Version = "1.4.200"
    val c3p0Version = "0.9.5.1"
    val slickVersion = "3.3.3"
    val slf4jNopVersion = "1.6.4"
    val slickPgVersion = "0.19.4"

    // DART
    val dartCommonsVersion = "3.0.30"
    val jacksonOverrideVersion = "2.10.5"
    val dartRestCommonsVersion = "3.0.4"
    val dartAuthVersion = "3.1.11"
    val arangoDatastoreRepoVersion = "3.0.8"
    val cdr4sVersion = "3.0.9"
    val operationsVersion = "3.0.14"


    val logging = Seq( "org.slf4j" % "slf4j-api" % slf4jVersion,
                       "ch.qos.logback" % "logback-classic" % logbackVersion )

    val betterFiles = Seq( "com.github.pathikrit" %% "better-files" % betterFilesVersion )

    val scalatra = Seq( "org.scalatra" %% "scalatra" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalate" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test",
                        "org.eclipse.jetty" % "jetty-webapp" % jettyWebappVersion,
                        "javax.servlet" % "javax.servlet-api" % servletApiVersion )

    val okhttp = Seq( "com.squareup.okhttp3" % "okhttp" % okhttpVersion,
                      "com.squareup.okhttp3" % "mockwebserver" % okhttpVersion )


    val kafka = Seq( "org.apache.kafka" %% "kafka" % kafkaVersion,
                     "org.apache.kafka" % "kafka-clients" % kafkaVersion,
                     "org.apache.kafka" % "kafka-streams" % kafkaVersion,
                     "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
                     "io.github.embeddedkafka" %% "embedded-kafka" % embeddedKafkaVersion % Test,
                     "io.github.embeddedkafka" %% "embedded-kafka-streams" % embeddedKafkaVersion % Test,
                     "jakarta.ws.rs" % "jakarta.ws.rs-api" % "2.1.2" % Test ) //https://github.com/sbt/sbt/issues/3618

    val testAndMocks = Seq( "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
                            "org.mockito" %% "mockito-scala-scalatest" % mockitoVersion % Test )

    val s3Mock = Seq( "io.findify" %% "s3mock" % s3MockVersion % "test" )

    val database = Seq( "org.postgresql" % "postgresql" % postgresVersion,
                        "com.h2database" % "h2" % h2Version,
                        "com.mchange" % "c3p0" % c3p0Version,
                        "io.zonky.test" % "embedded-postgres" % embeddedPostgresVersion % Test,
                        "com.typesafe.slick" %% "slick" % slickVersion,
//                        "org.slf4j" % "slf4j-nop" % slf4jNopVersion,
                        "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
                        "com.github.tminglei" %% "slick-pg" % slickPgVersion )

    val dartCommons = Seq( "com.twosixlabs.dart" %% "dart-aws" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-cli" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-utils" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-json" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-test-base" % dartCommonsVersion % Test )

    val dartRest = Seq( "com.twosixlabs.dart.rest" %% "dart-scalatra-commons" % dartRestCommonsVersion )

    val dartAuth = Seq( "com.twosixlabs.dart.auth" %% "controllers" % dartAuthVersion )

    val operations = Seq( "com.twosixlabs.dart.operations" %% "status-client" % operationsVersion )

    val arangoDatastoreRepo = Seq( "com.twosixlabs.dart" %% "dart-arangodb-datastore" % arangoDatastoreRepoVersion )

    val cdr4s = Seq( "com.twosixlabs.cdr4s" %% "cdr4s-core" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-ladle-json" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-dart-json" % cdr4sVersion )

}
