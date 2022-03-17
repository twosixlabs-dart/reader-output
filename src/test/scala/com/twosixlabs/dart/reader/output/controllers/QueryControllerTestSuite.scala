package com.twosixlabs.dart.reader.output.controllers

import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.auth.groups.ProgramManager
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.reader.output.models.tables.Schema
import com.twosixlabs.dart.reader.output.models.tables.Schema.{readerOutputTableInsert, readerOutputTableInsertWithoutLabels}
import com.twosixlabs.dart.reader.output.services.db.ArangoDartDatastore
import com.twosixlabs.dart.reader.output.services.query.PgArangoReaderSearchService
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.utilities.TestUtils
import com.twosixlabs.dart.test.utilities.TestUtils.FutureImplicits
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{Logger, LoggerFactory}
import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._
import com.twosixlabs.dart.test.tags.WipTest

import javax.servlet.http.HttpServletRequest

class QueryControllerTestSuite
    extends StandardTestBase3x with BeforeAndAfterEach with ScalatraSuite with MockFactory {

    val futureImplicits = new FutureImplicits( 5000, 250 )

    import futureImplicits._

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val arangoDartDatastore : ArangoDartDatastore = stub[ ArangoDartDatastore ]

    val config = TestUtils.getConfig

    import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors._

    private val authDeps = config.build[ AuthDependencies ]
    private val db       = config.build[ Database ]

    val searchDependencies : PgArangoReaderSearchService.Dependencies =
        PgArangoReaderSearchService.Dependencies( arangoDartDatastore,
                                                  db )

    val pgArangoReaderSearch : PgArangoReaderSearchService =
        new PgArangoReaderSearchService( searchDependencies )

    val controllerDeps : QueryController.Dependencies =
        QueryController.Dependencies( pgArangoReaderSearch, authDeps )

    val controller = new QueryController( controllerDeps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser =
            DartUser( "test-user", Set( ProgramManager ) )
    }

    addServlet( controller, "/*" )

    val endpoint = "/"

    val testIdentity   = "testreader"
    val testVersion    = "1.3.3.7"
    val testDocumentId = "d41d8cd98f00b204e9800998ecf8427e"
    val testStorageKey = "123321.txt"
    val outputVersion  = "1.2.3"
    val labels         = Set( "test", "spec" )

    override def beforeAll( ) : Unit = {
        super.beforeAll()
        db.run( Schema.readerOutputTableQuery.schema.dropIfExists ).awaitWrite
        db.run( Schema.readerOutputTableQuery.schema.create ).awaitWrite
        db.run( Schema.readerOutputTableQuery.schema.truncate ).awaitWrite
        ( arangoDartDatastore.getAllDocsAndTenants _ ).when().returns( Map( testDocumentId -> Set( "tenant-1", "tenant-2" ) ) )
    }

    override def beforeEach( ) : Unit = {
        super.beforeEach()
    }

    override def afterEach( ) : Unit = {

        db.run(
            Schema.readerOutputTableQuery.schema.truncate
            ).awaitWrite

        super.afterEach()
    }

    override def afterAll( ) : Unit = {
        super.afterAll()
        db.run( Schema.readerOutputTableQuery.schema.truncate ).awaitWrite
    }


    "POST to /query" should "return results and 200 on successful query" taggedAs ( WipTest) in {
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, outputVersion, Some( labels.toArray ) )

        val mockedQuery      = s"""{ "readers": ["${testIdentity}"] }"""
        val expectedResponse =
            s"""{"records":[{"identity":"${testIdentity}","version":"${testVersion}","document_id":"${testDocumentId}","storage_key":"${testStorageKey}","output_version":"1.2.3","tenants":["tenant-1","tenant-2"],"labels":["test","spec"]}]}""".stripMargin

        submitMultipart(
            method = "POST",
            path = endpoint,
            params = Array( ("metadata", mockedQuery) )
            ){
            status shouldBe 200
            body shouldBe expectedResponse
        }
    }

    "POST to /query" should "return results and 200 on successful query with non required fields are null" in {
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, outputVersion, None )
        ( arangoDartDatastore.getAllDocsAndTenants _ ).when.returns( Map( "d41d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-1", "tenant-2" ) ) )

        val mockedQuery      = s"""{ "readers": ["${testIdentity}"] }"""
        val expectedResponse =
            s"""{"records":[{"identity":"${testIdentity}","version":"${testVersion}","document_id":"${testDocumentId}","storage_key":"${testStorageKey}","output_version":"${outputVersion}","tenants":["tenant-1","tenant-2"],"labels":[]}]}""".stripMargin

        submitMultipart(
            method = "POST",
            path = endpoint,
            params = Array( ("metadata", mockedQuery) )
            ){
            status shouldBe 200
            body shouldBe expectedResponse
        }
    }

    "POST to /query" should "return results and 200 on successful query using one label and return only one record" in {
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, outputVersion, Some( labels.toArray ) )
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, "spec_output_version", Some( Array( "another_spec" ) ) )
        ( arangoDartDatastore.getAllDocsAndTenants _ ).when.returns( Map( "d41d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-1", "tenant-2" ) ) )

        val mockedQuery      = s"""{ "labels": ["spec"] }"""
        val expectedResponse =
            s"""{"records":[{"identity":"${testIdentity}","version":"${testVersion}","document_id":"${testDocumentId}","storage_key":"${testStorageKey}","output_version":"1.2.3","tenants":["tenant-1","tenant-2"],"labels":["test","spec"]}]}""".stripMargin

        submitMultipart(
            method = "POST",
            path = endpoint,
            params = Array( ("metadata", mockedQuery) )
            ){
            status shouldBe 200
            body shouldBe expectedResponse
        }
    }

    "POST to /query" should "return results and 200 on successful query using multiple labels" in {
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, outputVersion, Some( labels.toArray ) )
        ( arangoDartDatastore.getAllDocsAndTenants _ ).when.returns( Map( "d41d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-1", "tenant-2" ) ) )

        val mockedQuery      = s"""{ "labels": ["spec","test"] }"""
        val expectedResponse =
            s"""{"records":[{"identity":"${testIdentity}","version":"${testVersion}","document_id":"${testDocumentId}","storage_key":"${testStorageKey}","output_version":"1.2.3","tenants":["tenant-1","tenant-2"],"labels":["test","spec"]}]}""".stripMargin

        submitMultipart(
            method = "POST",
            path = endpoint,
            params = Array( ("metadata", mockedQuery) )
            ){
            status shouldBe 200
            body shouldBe expectedResponse
        }
    }

    "POST to /query" should "return multiple results and 200 on successful query using multiple labels and reader_id" in {
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, outputVersion, Some( labels.toArray ) )
        updateTable( "testreader", "1.3.3.7", "d41d8cd98f00b204e9800998ecf8427e", testStorageKey, "1.2.4", Some( Array( "test", "qwerty" ) ) )
        ( arangoDartDatastore.getAllDocsAndTenants _ ).when.returns( Map( "d41d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-1", "tenant-2" ) ) )

        val mockedQuery      = s"""{"readers": ["${testIdentity}"], "labels": ["test"] }"""
        val expectedResponse =
            s"""{"records":[{"identity":"${testIdentity}","version":"${testVersion}","document_id":"${testDocumentId}","storage_key":"${testStorageKey}","output_version":"1.2.3","tenants":["tenant-1","tenant-2"],"labels":["test","spec"]},{"identity":"${testIdentity}","version":"${testVersion}","document_id":"${testDocumentId}","storage_key":"${testStorageKey}","output_version":"1.2.4","tenants":["tenant-1","tenant-2"],"labels":["test","qwerty"]}]}"""
                .stripMargin

        submitMultipart(
            method = "POST",
            path = endpoint,
            params = Array( ("metadata", mockedQuery) )
            ){
            status shouldBe 200
            body shouldBe expectedResponse
        }
    }

    "POST to /query" should "return only the document Ids which are part of the specified tenant Id" in {
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, outputVersion, Some( labels.toArray ) )
        updateTable( "testreader", "1.3.3.7", "d31d8cd98f00b204e9800998ecf8427e", testStorageKey, "1.2.4", Some( Array( "test", "qwerty" ) ) )
        ( arangoDartDatastore.getAllDocsAndTenants _ ).when.returns( Map( "d41d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-1", "tenant-2" ),
                                                                          "d31d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-3" ) ) )

        val mockedQuery      = s"""{"readers": ["${testIdentity}"], "tenant_id": "tenant-3" }"""
        val expectedResponse =
            s"""{"records":[{"identity":"${testIdentity}","version":"${testVersion}","document_id":"d31d8cd98f00b204e9800998ecf8427e","storage_key":"${testStorageKey}","output_version":"1.2.4","tenants":["tenant-3"],"labels":["test","qwerty"]}]}""".stripMargin

        submitMultipart(
            method = "POST",
            path = endpoint,
            params = Array( ("metadata", mockedQuery) )
            ){
            status shouldBe 200
            body shouldBe expectedResponse
        }
    }

    "POST to /query" should "return only documents which are part of the tenant with only tenant Id provided as a query parameter" in {
        updateTable( testIdentity, testVersion, testDocumentId, testStorageKey, outputVersion, Some( labels.toArray ) )
        updateTable( "testreader", "1.3.3.7", "d31d8cd98f00b204e9800998ecf8427e", testStorageKey, "1.2.4", Some( Array( "test", "qwerty" ) ) )
        ( arangoDartDatastore.getAllDocsAndTenants _ ).when.returns( Map( "d41d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-1", "tenant-2" ),
                                                                          "d31d8cd98f00b204e9800998ecf8427e" -> Set( "tenant-3" ) ) )

        val mockedQuery      = s"""{"tenant_id": "tenant-3" }"""
        val expectedResponse =
            s"""{"records":[{"identity":"${testIdentity}","version":"${testVersion}","document_id":"d31d8cd98f00b204e9800998ecf8427e","storage_key":"${testStorageKey}","output_version":"1.2.4","tenants":["tenant-3"],"labels":["test","qwerty"]}]}""".stripMargin

        submitMultipart(
            method = "POST",
            path = endpoint,
            params = Array( "metadata" -> mockedQuery )
            ){
            status shouldBe 200
            body shouldBe expectedResponse
        }
    }

    def updateTable(
        testIdentity : String, testVersion : String, testDocumentId : String, testStorageKey : String, outputVersion : String,
        labels : Option[ Array[ String ] ] ) : Int = {

        db.run(
            labels match {
                case Some( ls ) => readerOutputTableInsert += ( (testIdentity, testVersion, testDocumentId, testStorageKey, Some( outputVersion ), ls.toList) )
                case None => readerOutputTableInsertWithoutLabels += (testIdentity, testVersion, testDocumentId, Some( outputVersion ), testStorageKey)
            }
            ).awaitWrite

    }

    override def header = null
}
