package org.psesd.srx.services.ears

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.xml.transform._
import javax.xml.transform.stream._

import org.json4s.JValue
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif._

import scala.xml.Node

/** Represents a SIF Data Object method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class SifObjectResult(httpStatusCode: Int, resultXml: Option[Node]) extends SrxResourceResult {

  statusCode = httpStatusCode

  def toJson: Option[JValue] = {
    if (statusCode == SifHttpStatusCode.Ok && resultXml.isDefined) {
      Some(resultXml.get.toJsonStringNoRoot.toJson)
    } else {
      None
    }
  }

  def toXml: Option[Node] = {
    if (statusCode == SifHttpStatusCode.Ok) {
      resultXml
    } else {
      None
    }
  }
}

/** SIF Data Object methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object SifObject extends SrxResourceService {
  final val PrsFiltersResource = "filters"
  final val MasterXsresResource = "masterXsres"

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }
      throw new NotImplementedError("EARS CREATE method not implemented.")
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      throw new NotImplementedError("EARS DELETE method not implemented.")
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def query(parameters: List[SifRequestParameter]): SrxResourceResult = {
    var authorizedEntityId: Int = 0
    var contextId: String = null
    var districtStudentId: String = null
    var externalServiceId: Int = 0
    var objectName: String = null
    var objectType: String = null
    var personnelId: String = null
    var zoneId: String = null

    try {
      if (parameters == null) {
        throw new ArgumentNullException("parameters collection")
      }

      val authorizedEntityIdParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.AuthorizedEntityId.toString.toLowerCase)
      authorizedEntityId = if (authorizedEntityIdParam.isDefined && !authorizedEntityIdParam.get.value.isNullOrEmpty) authorizedEntityIdParam.get.value.toInt else throw new ArgumentInvalidException(SifObjectParameter.AuthorizedEntityId.toString + " parameter")

      val contextIdParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.ContextId.toString.toLowerCase)
      contextId = if (contextIdParam.isDefined && !contextIdParam.get.value.isNullOrEmpty) contextIdParam.get.value else throw new ArgumentInvalidException(SifObjectParameter.ContextId.toString + " parameter")

      val districtStudentIdParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.DistrictStudentId.toString.toLowerCase)
      districtStudentId = if (districtStudentIdParam.isDefined && !districtStudentIdParam.get.value.isNullOrEmpty) districtStudentIdParam.get.value else throw new ArgumentInvalidException(SifObjectParameter.DistrictStudentId.toString + " parameter")

      val externalServiceIdParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.ExternalServiceId.toString.toLowerCase)
      externalServiceId = if (externalServiceIdParam.isDefined && !externalServiceIdParam.get.value.isNullOrEmpty) externalServiceIdParam.get.value.toInt else throw new ArgumentInvalidException(SifObjectParameter.ExternalServiceId.toString + " parameter")

      val objectNameParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.ObjectName.toString.toLowerCase)
      objectName = if (objectNameParam.isDefined && !objectNameParam.get.value.isNullOrEmpty) objectNameParam.get.value else throw new ArgumentInvalidException(SifObjectParameter.ObjectName.toString + " parameter")

      val objectTypeParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.ObjectType.toString.toLowerCase)
      objectType = if (objectTypeParam.isDefined && !objectTypeParam.get.value.isNullOrEmpty) objectTypeParam.get.value else throw new ArgumentInvalidException(SifObjectParameter.ObjectType.toString + " parameter")

      val personnelIdParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.PersonnelId.toString.toLowerCase)
      personnelId = if (personnelIdParam.isDefined && !personnelIdParam.get.value.isNullOrEmpty) personnelIdParam.get.value else null

      val zoneIdParam = parameters.find(p => p.key.toLowerCase == SifObjectParameter.ZoneId.toString.toLowerCase)
      zoneId = if (zoneIdParam.isDefined && !districtStudentIdParam.get.value.isNullOrEmpty) zoneIdParam.get.value else throw new ArgumentInvalidException(SifObjectParameter.ZoneId.toString + " parameter")

      try {
        val prsFilterResponse = getPrsFilter(
          authorizedEntityId,
          contextId,
          districtStudentId,
          externalServiceId,
          objectName,
          objectType,
          personnelId,
          zoneId
        )
        if (prsFilterResponse.isValid && prsFilterResponse.statusCode.equals(SifHttpStatusCode.Ok) && prsFilterResponse.body.isDefined) {
          // get unfiltered SIF object
          val sifObjectResponse = getSifObject(contextId, districtStudentId, objectType, zoneId)
          if (sifObjectResponse.exceptions.isEmpty && sifObjectResponse.statusCode.equals(SifHttpStatusCode.Ok)) {
            if (sifObjectResponse.body.isDefined) {
              // return filtered SIF object
              EarsServer.logSuccessMessage(
                SrxResourceType.Xsres.toString,
                SifRequestAction.Query.toString,
                Some(districtStudentId),
                SifRequestParameterCollection(parameters),
                None
              )
              transformSifObject(sifObjectResponse.body.get, prsFilterResponse.body.get)
            } else {
              EarsServer.logNotFoundMessage(
                SrxResourceType.Xsres.toString,
                SifRequestAction.Query.toString,
                Some(districtStudentId),
                SifRequestParameterCollection(parameters),
                None
              )
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(objectName))
            }
          } else {
            if (sifObjectResponse.statusCode.equals(SifHttpStatusCode.NotFound)) {
              EarsServer.logNotFoundMessage(
                SrxResourceType.Xsres.toString,
                SifRequestAction.Query.toString,
                Some(districtStudentId),
                SifRequestParameterCollection(parameters),
                None
              )
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(objectName))
            } else {
              if (sifObjectResponse.exceptions.isEmpty) {
                val sifObjectResponseXml = sifObjectResponse.getBodyXml
                if (sifObjectResponseXml.isDefined) {
                  SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, new Exception((sifObjectResponseXml.get \ "message").text))
                } else {
                  SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, new Exception("Failed to retrieve student '%s'".format(districtStudentId)))
                }
              } else {
                SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, sifObjectResponse.exceptions.head)
              }
            }
          }
        } else {
          if(prsFilterResponse.statusCode.equals(SifHttpStatusCode.NotFound)) {
            EarsServer.logNotFoundMessage(
              SrxResourceType.Xsres.toString,
              SifRequestAction.Query.toString,
              Some(districtStudentId),
              SifRequestParameterCollection(parameters),
              None
            )
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(objectName))
          } else {
            // TODO: submit PRS Filters error to Rollbar
            if (prsFilterResponse.exceptions.nonEmpty) {
              // return any exceptions connecting to or receiving a response from PRS
              SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, prsFilterResponse.exceptions.head)
            } else {
              if (prsFilterResponse.body.isDefined && prsFilterResponse.body.get.contains("<error") && prsFilterResponse.body.get.contains("<message")) {
                // return the original PRS error message received from SIF environment broker
                SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, new Exception((prsFilterResponse.body.get.toXml \ "message").text))
              } else {
                // return a generic message indicating the PRS filter could not be retrieved
                SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, new SrxResourceNotFoundException("%s PRS filter".format(objectName)))
              }
            }
          }
        }
      } catch {
        case e: Exception =>
          SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
      }

    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, e)
    }
  }

  def update(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }
      throw new NotImplementedError("EARS UPDATE method not implemented.")
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  private def getPrsFilter(
                            authorizedEntityId: Int,
                            contextId: String,
                            districtStudentId: String,
                            externalServiceId: Int,
                            objectName: String,
                            objectType: String,
                            personnelId: String,
                            zoneId: String
                          ): SifResponse = {
    val sifRequest = getSifRequest(PrsFiltersResource, zoneId)
    sifRequest.requestId = Some(SifMessageId().toString)
    sifRequest.contentType = Some(SifContentType.Xml)
    sifRequest.accept = Some(SifContentType.Xml)
    sifRequest.serviceType = Some(SifServiceType.Object)
    sifRequest.messageType = Some(SifMessageType.Request)
    sifRequest.requestType = Some(SifRequestType.Immediate)

    sifRequest.addHeader(SifObjectParameter.AuthorizedEntityId.toString, authorizedEntityId.toString)
    sifRequest.addHeader(SifObjectParameter.DistrictStudentId.toString, districtStudentId.toString)
    sifRequest.addHeader(SifObjectParameter.ExternalServiceId.toString, externalServiceId.toString)
    sifRequest.addHeader(SifObjectParameter.ObjectType.toString, objectType.toString)
    if (!personnelId.isNullOrEmpty) {
      sifRequest.addHeader(SifObjectParameter.PersonnelId.toString, personnelId.toString)
    }

    new SifConsumer().query(sifRequest)
  }

  private def getSifObject(
                            contextId: String,
                            objectId: String,
                            objectType: String,
                            zoneId: String
                          ): SifResponse = {
    // TODO: support multiple object types? SRE vs xSRE?
    val resource = "%s/%s".format(MasterXsresResource, objectId)

    val sifRequest = getSifRequest(resource, zoneId)
    sifRequest.requestId = Some(SifMessageId().toString)
    sifRequest.contentType = Some(SifContentType.Xml)
    sifRequest.accept = Some(SifContentType.Xml)
    sifRequest.serviceType = Some(SifServiceType.Object)
    sifRequest.messageType = Some(SifMessageType.Request)
    sifRequest.requestType = Some(SifRequestType.Immediate)
    sifRequest.generatorId = Some(EarsServer.srxService.service.name)

    new SifConsumer().query(sifRequest)
  }

  private def transformSifObject(sifObjectXml: String, xsl: String): SrxResourceResult = {
    try {
      val xslSource = new StreamSource(new ByteArrayInputStream(xsl.getBytes))
      val factory = TransformerFactory.newInstance()
      val template = factory.newTemplates(xslSource)
      val transformer = template.newTransformer()
      val xmlSource = new StreamSource(new ByteArrayInputStream(sifObjectXml.getBytes))
      val result = new StreamResult(new ByteArrayOutputStream())
      transformer.transform(xmlSource, result)
      Some(result.getOutputStream.toString.toXml)
      new SifObjectResult(SifHttpStatusCode.Ok, Some(result.getOutputStream.toString.toXml))
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  private def getSifRequest(resource: String, zoneId: String): SifRequest = {
    new SifRequest(
      new SifProvider(
        SifProviderUrl(Environment.getProperty(Environment.SrxEnvironmentUrlKey)),
        SifProviderSessionToken(Environment.getProperty(Environment.SrxSessionTokenKey)),
        SifProviderSharedSecret(Environment.getProperty(Environment.SrxSharedSecretKey)),
        SifAuthenticationMethod.SifHmacSha256
      ),
      resource,
      SifZone(zoneId),
      SifContext()
    )
  }

}
