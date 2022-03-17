package com.twosixlabs.dart.reader.output.exceptions

class MissingPropertyException( property : String ) extends Exception( s"Missing configuration property: ${property}" ) {}

class UnableToCreateDocumentException( fileName : String ) extends Exception( s"Unable To create ${fileName} on local disk" ) {}

class UnableToRetrieveDocumentException( fileName : String ) extends Exception( s"Unable To retrieve ${fileName} from storage" ) {}

class UnableToSaveDocumentException( fileName : String ) extends Exception( s"Unable To save ${fileName} to local disk" ) {}

class UnableToGetDocIdsByTenant( tenantId : String ) extends Exception( s"Unable get document Ids for tenant: ${tenantId}" ) {}

class UnableToGetAllTenants( ) extends Exception( s"Unable get tenants" ) {}

class InvalidMetadataException( problem : String ) extends Exception( s"invalid metadata: ${problem}" )
