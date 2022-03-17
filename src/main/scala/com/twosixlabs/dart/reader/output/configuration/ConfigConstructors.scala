package com.twosixlabs.dart.reader.output.configuration

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.twosixlabs.dart.arangodb.{Arango, ArangoConf}
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.reader.output.services.notification.RestNotificationService
import com.twosixlabs.dart.reader.output.services.notification.kafka.KafkaNotificationService
import com.twosixlabs.dart.reader.output.services.storage.{LocalReaderOutputStorageService, ReaderOutputStorageService, S3ReaderOutputStorageService}
import com.twosixlabs.dart.operations.status.client.{PipelineStatusUpdateClient, SqlPipelineStatusUpdateClient}
import com.twosixlabs.dart.sql.SqlClient
import com.typesafe.config.Config
import software.amazon.awssdk.auth.credentials._

import java.net.URI
import java.util.Properties
import scala.util.{Success, Try}


trait ConfigConstructor[ C, T ] {
    def buildFromConfig( config : C ) : T
}

object ConfigConstructors {

    /**
     * The idea here is that if you have a set of implicit ConfigConstructors in scope,
     * you can just call config.build[ Result ] where Result is the type being built
     */
    implicit class FromConfig[ C ]( config : C ) {
        def build[ T ]( implicit constructor : ConfigConstructor[ C, T ] ) : T = constructor.buildFromConfig( config )
    }

    import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._

    implicit object AuthFromConfig extends ConfigConstructor[ Config, AuthDependencies ] {
        override def buildFromConfig( config : Config ) : AuthDependencies = {
            SecureDartController.authDeps( config )
        }
    }

    implicit object DatabaseFromConfig extends ConfigConstructor[ Config, Database ] {
        override def buildFromConfig( config : Config ) : Database = {
            val ds = new ComboPooledDataSource()
            ds.setDriverClass( config.getString( "postgres.driver.class" ) )
            val pgHost = config.getString( "postgres.host" )
            val pgPort = config.getInt( "postgres.port" )
            val pgDb = config.getString( "postgres.database" )
            ds.setJdbcUrl( s"jdbc:postgresql://$pgHost:$pgPort/$pgDb" )
            ds.setUser( config.getString( "postgres.user" ) )
            ds.setPassword( config.getString( "postgres.password" ) )
            Try( config.getInt( "postgres.minPoolSize" )  ).foreach( v => ds.setMinPoolSize( v ) )
            Try( config.getInt( "postgres.acquireIncrement" )  ).foreach( v => ds.setAcquireIncrement( v ) )
            Try( config.getInt( "postgres.maxPoolSize" )  ).foreach( v => ds.setMaxPoolSize( v ) )

            val maxConns = Try( config.getInt( "postgres.max.connections" ) ).toOption

            Database.forDataSource( ds, maxConns )
        }
    }

    implicit object ArangoFromConfig extends ConfigConstructor[ Config, Arango ] {
        override def buildFromConfig( config : Config ) : Arango = {
            new Arango( ArangoConf(
                host = config.getString( "arangodb.host" ) ,
                port = config.getInt( "arangodb.port" ),
                database = config.getString( "arangodb.database" )
            ) )
        }
    }

    implicit object KafkaNotifierFromConfig extends ConfigConstructor[ Config, KafkaNotificationService ] {
        override def buildFromConfig( config : Config ) : KafkaNotificationService = {
            def propsFromConfig(config: Config): Properties = {
                import scala.collection.JavaConverters._

                val props = new Properties()

                val map: Map[String, Object] = config.entrySet().asScala.map({ entry =>
                    entry.getKey -> entry.getValue.unwrapped()
                })( collection.breakOut )

                props.putAll( map.asJava )
                props
            }

            KafkaNotificationService(
                config.getString( "notification.kafka.topic" ),
                propsFromConfig( config.getConfig( "kafka" ) ),
            )
        }
    }

    implicit object RestNotifierFromConfig extends ConfigConstructor[ Config, RestNotificationService ] {
        override def buildFromConfig( config : Config ) : RestNotificationService = {
            Try( config.getLong( "notification.rest.timeout" ) ).toOption match {
                case None => RestNotificationService(
                    config.getString( "notification.rest.url" ),
                )
                case Some( timeout ) => RestNotificationService(
                    config.getString( "notification.rest.url" ),
                    timeout,
                )
            }
        }
    }

    implicit object S3StorageFromConfig extends ConfigConstructor[ Config, S3ReaderOutputStorageService ] {
        override def buildFromConfig( config : Config ) : S3ReaderOutputStorageService = {
            S3ReaderOutputStorageService {
                val bucketName = config.getString( "persistence.bucket.name" )
                val credentialsStringTry = Try( config.getString( "aws.credentials.provider" ) )
                val credentials : AwsCredentialsProvider = {
                     credentialsStringTry match {
                        case Success( "INSTANCE" ) => InstanceProfileCredentialsProvider.create()
                        case Success( "ENVIRONMENT" ) => EnvironmentVariableCredentialsProvider.create()
                        case Success( "TEST" ) => AnonymousCredentialsProvider.create()
                        case _ => SystemPropertyCredentialsProvider.create()
                    }
                }

                val endpoint = ( credentialsStringTry match {
                    case Success( "TEST" ) => Try( config.getString( "aws.test.url" ) ).toOption
                    case _ => None
                } ).map( new URI( _ ) )

                new S3Bucket( bucketName, credentials, System.getProperty( "java.io.tmpdir" ), endpoint )
            }
        }
    }

    implicit object LocalStorageFromConfig extends ConfigConstructor[ Config, LocalReaderOutputStorageService ] {
        override def buildFromConfig( config : Config ) : LocalReaderOutputStorageService = {
            LocalReaderOutputStorageService( config.getString( "persistence.dir" ) )
        }
    }

    implicit object StorageFromConfig extends ConfigConstructor[ Config, ReaderOutputStorageService ] {
        override def buildFromConfig( config : Config ) : ReaderOutputStorageService = {
            config.getString( "persistence.mode" ) match {
                case "aws" => config.build[ S3ReaderOutputStorageService ]
                case _ => config.build[ LocalReaderOutputStorageService ]
            }
        }
    }

    implicit object OperationsFromConfig extends ConfigConstructor[ Config, PipelineStatusUpdateClient ] {
        override def buildFromConfig( config : Config ) : PipelineStatusUpdateClient = {
            val engine = "postgresql"
            val host = config.getString( "postgres.host" )
            val port = config.getInt( "postgres.port" )
            val user = config.getString( "postgres.user" )
            val password = config.getString( "postgres.password" )
            val name = config.getString( "postgres.database" )

            val client = SqlClient.newClient( engine, name, host, port, Some( user ), Some( password ) )

            new SqlPipelineStatusUpdateClient( client, "pipeline_status" )
        }
    }

}
