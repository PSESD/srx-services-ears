package org.psesd.srx.services.ears

import org.http4s._
import org.http4s.dsl._
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.sif._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

/** SRX External Authorized Retrieval Service server.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  **/
object EarsServer extends SrxServer {

  private final val ServerUrlKey = "SERVER_URL"

  private final val DatasourceClassNameKey = "DATASOURCE_CLASS_NAME"
  private final val DatasourceMaxConnectionsKey = "DATASOURCE_MAX_CONNECTIONS"
  private final val DatasourceTimeoutKey = "DATASOURCE_TIMEOUT"
  private final val DatasourceUrlKey = "DATASOURCE_URL"

  val sifProvider: SifProvider = new SifProvider(
    SifProviderUrl(Environment.getProperty(ServerUrlKey)),
    SifProviderSessionToken(Environment.getProperty(Environment.SrxSessionTokenKey)),
    SifProviderSharedSecret(Environment.getProperty(Environment.SrxSharedSecretKey)),
    SifAuthenticationMethod.SifHmacSha256
  )

  val srxService: SrxService = new SrxService(
    new SrxServiceComponent(Build.name, Build.version + "." + Build.buildNumber),
    List[SrxServiceComponent](
      new SrxServiceComponent("java", Build.javaVersion),
      new SrxServiceComponent("scala", Build.scalaVersion),
      new SrxServiceComponent("sbt", Build.sbtVersion)
    )
  )

  override def serviceRouter(implicit executionContext: ExecutionContext) = HttpService {

    case req@GET -> Root =>
      Ok()

    case _ -> Root =>
      NotImplemented()

    case req@GET -> Root / _ if services(req, SrxResourceType.Ping.toString) =>
      Ok(true.toString)

    case req@GET -> Root / _ if services(req, SrxResourceType.Info.toString) =>
      respondWithInfo(getDefaultSrxResponse(req))

    case req@GET -> Root / objectName / _ if services(req, objectName) =>
      executeRequest(req, getSifObjectRequestParameters(req, objectName), objectName, SifObject)

    case req@GET -> Root / objectName / _ =>
      executeRequest(req, getSifObjectRequestParameters(req, objectName), objectName, SifObject)

    case req@POST -> Root / objectName / _ if services(req, objectName) =>
      MethodNotAllowed()

    case req@PUT -> Root / objectName / _ if services(req, objectName) =>
      MethodNotAllowed()

    case req@PUT -> Root / objectName / _ =>
      MethodNotAllowed()

    case req@DELETE -> Root / objectName / _ if services(req, objectName) =>
      MethodNotAllowed()

    case req@DELETE -> Root / objectName / _ =>
      MethodNotAllowed()

    case _ =>
      NotFound()

  }

  private def getSifObjectRequestParameters(req: Request, objectName: String): Option[List[SifRequestParameter]] = {
    val params = ArrayBuffer[SifRequestParameter]()
    params += SifRequestParameter(SifObjectParameter.ObjectName.toString, objectName)
    for (h <- req.headers) {
      val headerName = h.name.value.toLowerCase
      if(headerName == SifObjectParameter.AuthorizedEntityId.toString.toLowerCase) params += SifRequestParameter(SifObjectParameter.AuthorizedEntityId.toString, h.value)
      if(headerName == SifObjectParameter.DistrictStudentId.toString.toLowerCase) params += SifRequestParameter(SifObjectParameter.DistrictStudentId.toString, h.value)
      if(headerName == SifObjectParameter.ExternalServiceId.toString.toLowerCase) params += SifRequestParameter(SifObjectParameter.ExternalServiceId.toString, h.value)
      if(headerName == SifObjectParameter.ObjectType.toString.toLowerCase) params += SifRequestParameter(SifObjectParameter.ObjectType.toString, h.value)
      if(headerName == SifObjectParameter.PersonnelId.toString.toLowerCase) params += SifRequestParameter(SifObjectParameter.PersonnelId.toString, h.value)
    }
    Some(params.toList)
  }

}
