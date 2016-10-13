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
  final val AuthorizedEntityIdParameter = "authorizedEntityId"
  final val ContextIdParameter = "contextId"
  final val DistrictStudentIdParameter = "districtStudentId"
  final val ExternalServiceIdParameter = "externalServiceId"
  final val ObjectNameParameter = "objectName"
  final val ObjectTypeParameter = "objectType"
  final val PersonnelIdParameter = "personnelId"
  final val PrsFiltersResource = "filters"
  final val ZoneIdParameter = "zoneId"

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

      val authorizedEntityIdParam = parameters.find(p => p.key.toLowerCase == AuthorizedEntityIdParameter.toLowerCase)
      authorizedEntityId = if (authorizedEntityIdParam.isDefined && !authorizedEntityIdParam.get.value.isNullOrEmpty) authorizedEntityIdParam.get.value.toInt else throw new ArgumentInvalidException(AuthorizedEntityIdParameter + " parameter")

      val contextIdParam = parameters.find(p => p.key.toLowerCase == ContextIdParameter.toLowerCase)
      contextId = if (contextIdParam.isDefined && !contextIdParam.get.value.isNullOrEmpty) contextIdParam.get.value else throw new ArgumentInvalidException(ContextIdParameter + " parameter")

      val districtStudentIdParam = parameters.find(p => p.key.toLowerCase == DistrictStudentIdParameter.toLowerCase)
      districtStudentId = if (districtStudentIdParam.isDefined && !districtStudentIdParam.get.value.isNullOrEmpty) districtStudentIdParam.get.value else throw new ArgumentInvalidException(DistrictStudentIdParameter + " parameter")

      val externalServiceIdParam = parameters.find(p => p.key.toLowerCase == ExternalServiceIdParameter.toLowerCase)
      externalServiceId = if (externalServiceIdParam.isDefined && !externalServiceIdParam.get.value.isNullOrEmpty) externalServiceIdParam.get.value.toInt else throw new ArgumentInvalidException(ExternalServiceIdParameter + " parameter")

      val objectNameParam = parameters.find(p => p.key.toLowerCase == ObjectNameParameter.toLowerCase)
      objectName = if (objectNameParam.isDefined && !objectNameParam.get.value.isNullOrEmpty) objectNameParam.get.value else throw new ArgumentInvalidException(ObjectNameParameter + " parameter")

      val objectTypeParam = parameters.find(p => p.key.toLowerCase == ObjectTypeParameter.toLowerCase)
      objectType = if (objectTypeParam.isDefined && !objectTypeParam.get.value.isNullOrEmpty) objectTypeParam.get.value else throw new ArgumentInvalidException(ObjectTypeParameter + " parameter")

      val personnelIdParam = parameters.find(p => p.key.toLowerCase == PersonnelIdParameter.toLowerCase)
      personnelId = if (personnelIdParam.isDefined && !personnelIdParam.get.value.isNullOrEmpty) personnelIdParam.get.value else null

      val zoneIdParam = parameters.find(p => p.key.toLowerCase == ZoneIdParameter.toLowerCase)
      zoneId = if (zoneIdParam.isDefined && !districtStudentIdParam.get.value.isNullOrEmpty) zoneIdParam.get.value else throw new ArgumentInvalidException(ZoneIdParameter + " parameter")

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
          if (sifObjectResponse.exceptions.isEmpty) {
            if (sifObjectResponse.body.isDefined) {
              // return filtered SIF object
              transformSifObject(sifObjectResponse.body.get, prsFilterResponse.body.get)
            } else {
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(objectName))
            }
          } else {
            SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, sifObjectResponse.exceptions.head)
          }
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

    // TODO: remove legacy PRS credentials after new SRX PRS service has been deployed and configured in HostedZone
    val srxPrsProvider = new SifProvider(
      Environment.srxEnvironmentUrl,
      SifProviderSessionToken(Environment.getProperty("SRX_PRS_SESSION_TOKEN")),
      SifProviderSharedSecret(Environment.getProperty("SRX_PRS_SHARED_SECRET")),
      SifAuthenticationMethod.SifHmacSha256
    )

    val sifRequest = new SifRequest(srxPrsProvider, PrsFiltersResource, SifZone(zoneId), SifContext(contextId))
    sifRequest.requestId = Some(SifMessageId().toString)
    sifRequest.contentType = Some(SifContentType.Xml)
    sifRequest.accept = Some(SifContentType.Xml)
    sifRequest.serviceType = Some(SifServiceType.Object)
    sifRequest.messageType = Some(SifMessageType.Request)
    sifRequest.requestType = Some(SifRequestType.Immediate)

    sifRequest.addHeader(AuthorizedEntityIdParameter, authorizedEntityId.toString)
    sifRequest.addHeader(DistrictStudentIdParameter, districtStudentId.toString)
    sifRequest.addHeader(ExternalServiceIdParameter, externalServiceId.toString)
    sifRequest.addHeader(ObjectTypeParameter, objectType.toString)
    if (!personnelId.isNullOrEmpty) {
      sifRequest.addHeader(PersonnelIdParameter, personnelId.toString)
    }

    new SifConsumer().query(sifRequest)
  }

  private def getSifObject(
                            contextId: String,
                            objectId: String,
                            objectType: String,
                            zoneId: String
                          ): SifResponse = {
    // TODO: use objectType vs "sres"
    val resource = "%s/%s".format("sres", objectId)

    // TODO: remove legacy PRS credentials after new SRX PRS service has been deployed and configured in HostedZone
    val srxPrsProvider = new SifProvider(
      Environment.srxEnvironmentUrl,
      SifProviderSessionToken(Environment.getProperty("SRX_PRS_SESSION_TOKEN")),
      SifProviderSharedSecret(Environment.getProperty("SRX_PRS_SHARED_SECRET")),
      SifAuthenticationMethod.SifHmacSha256
    )

    val sifRequest = new SifRequest(srxPrsProvider, resource, SifZone(zoneId), SifContext(contextId))
    sifRequest.requestId = Some(SifMessageId().toString)
    sifRequest.contentType = Some(SifContentType.Xml)
    sifRequest.accept = Some(SifContentType.Xml)
    sifRequest.serviceType = Some(SifServiceType.Object)
    sifRequest.messageType = Some(SifMessageType.Request)
    sifRequest.requestType = Some(SifRequestType.Immediate)

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

}
