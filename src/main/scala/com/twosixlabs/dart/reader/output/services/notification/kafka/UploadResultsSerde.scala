package com.twosixlabs.dart.reader.output.services.notification.kafka

import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputSubmissionResult
import com.twosixlabs.dart.json.JsonFormat
import org.apache.kafka.common.serialization.Serdes.WrapperSerde
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import java.util.{Map => JMap}

class UploadResultsSerializer extends Serializer[ ReaderOutputSubmissionResult ] {

    override def configure( configs : JMap[ String, _ ], isKey : Boolean ) : Unit = {}

    override def serialize( topic : String, data : ReaderOutputSubmissionResult ) : Array[ Byte ] = JsonFormat.marshalFrom( data ).get.getBytes

    override def close( ) : Unit = {}
}


class UploadResultsDeserializer extends Deserializer[ ReaderOutputSubmissionResult ] {

    override def configure( configs : JMap[ String, _ ], isKey : Boolean ) : Unit = {}

    override def deserialize( topic : String, data : Array[ Byte ] ) : ReaderOutputSubmissionResult = JsonFormat.unmarshalTo( new String( data ), classOf[ ReaderOutputSubmissionResult ] ).get

    override def close( ) : Unit = {}
}

class UploadResultsSerde extends WrapperSerde[ ReaderOutputSubmissionResult ]( new UploadResultsSerializer, new UploadResultsDeserializer )
