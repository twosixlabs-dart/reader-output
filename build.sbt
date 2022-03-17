import Dependencies._
import sbt._

organization in ThisBuild := "com.worldmodelers"
name := "reader-output"
scalaVersion in ThisBuild := "2.12.7"

resolvers in ThisBuild ++= Seq( "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
                                "Maven Central" at "https://repo1.maven.org/maven2/",
                                "JCenter" at "https://jcenter.bintray.com" )

lazy val root = ( project in file( "." ) ).settings( libraryDependencies ++= logging
                                                                             ++ betterFiles
                                                                             ++ scalatra
                                                                             ++ okhttp
                                                                             ++ kafka
                                                                             ++ database
                                                                             ++ testAndMocks
                                                                             ++ operations
                                                                             ++ dartCommons
                                                                             ++ arangoDatastoreRepo
                                                                             ++ cdr4s
                                                                             ++ dartRest
                                                                             ++ dartAuth
                                                                             ++ s3Mock,
                                                                             // ++ dartAuth,
                                                     excludeDependencies ++= Seq( ExclusionRule( "org.slf4j", "slf4j-log4j12" ),
                                                                                  ExclusionRule( "org.slf4j", "log4j-over-slf4j" ),
                                                                                  ExclusionRule( "log4j", "log4j" ),
                                                                                  ExclusionRule( "org.apache.logging.log4j", "log4j-core" ) ),
                                                     dependencyOverrides ++= Seq( // "com.google.guava" % "guava" % "19.0",
                                                                                  "com.fasterxml.jackson.core" % "jackson-core" % jacksonOverrideVersion,
                                                                                  "com.fasterxml.jackson.core" % "jackson-annotation" % jacksonOverrideVersion,
                                                                                  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.5",
                                                                                  "com.arangodb" %% "velocypack-module-scala" % "1.2.0",
                                                                                  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.10.5",
                                                                                  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5" ) )

mainClass in(Compile, run) := Some( "Main" )

enablePlugins( JavaAppPackaging )

// don't run tests when build the fat jar, use sbt test instead for that (takes too long when building the image)
test in assembly := {}

parallelExecution in Test := false

assemblyMergeStrategy in assembly := {
    case PathList( "META-INF", "MANIFEST.MF" ) => MergeStrategy.discard
    case PathList( "reference.conf" ) => MergeStrategy.concat
    case x => MergeStrategy.last
}
