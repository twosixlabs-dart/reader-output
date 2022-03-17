package com.twosixlabs.dart.reader.output.services.notification

import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors._
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputSubmissionResult
import com.twosixlabs.dart.reader.output.services.notification.kafka.{KafkaNotificationService, UploadResultsSerde}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.utilities.TestUtils
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{Logger, LoggerFactory}

class KafkaNotificationServiceTestSuite
  extends StandardTestBase3x with StandardCliConfig with BeforeAndAfterEach with ScalatraSuite with EmbeddedKafka with MockFactory {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    private val config = TestUtils.getConfig

    implicit val kafkaConfig : EmbeddedKafkaConfig =
        EmbeddedKafkaConfig( kafkaPort = 6308, zooKeeperPort = 2111 )

    val valueSerde : Serde[ ReaderOutputSubmissionResult ] = new UploadResultsSerde
    implicit val serializer : Serializer[ ReaderOutputSubmissionResult ] = valueSerde.serializer
    implicit val deserializer : Deserializer[ ReaderOutputSubmissionResult ] = valueSerde.deserializer

    "KafkaNotification::notify" should "pass event through" in {
        val notificationService = config.build[ KafkaNotificationService ]

        val topic = config.getString( "notification.kafka.topic" )

        withRunningKafka {
            val testUploadResults = Right( ReaderOutputSubmissionResult( "1", "2", "3", "4", Some( "5" ), Some( Set( "1" ) ) ) )
            notificationService.notify( testUploadResults )

            // check Kafka processing
            val result : ReaderOutputSubmissionResult = consumeFirstMessageFrom( topic )
            result.identity shouldBe "1"
            result.version shouldBe "2"
            result.documentId shouldBe "3"
            result.storageKey shouldBe "4"
            result.outputVersion shouldBe Some( "5" )
            result.labels shouldBe Some( Set( "1" ) )
        }
    }

    override def header = null
}
