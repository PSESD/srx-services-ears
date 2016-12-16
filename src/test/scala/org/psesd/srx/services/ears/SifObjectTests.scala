package org.psesd.srx.services.ears

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class SifObjectTests extends FunSuite {

  test("query null parameters") {
    val result = SifObject.query(null)
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The parameters collection cannot be null."))
  }

  test("query invalid authorizedEntityId") {
    val result = SifObject.query(List[SifRequestParameter]())
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The authorizedEntityId parameter is invalid."))
  }

  test("query invalid contextId") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The contextId parameter is invalid."))
  }

  test("query invalid districtStudentId") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("contextId", "1")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The districtStudentId parameter is invalid."))
  }

  test("query invalid externalServiceId") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("contextId", "1"),
      SifRequestParameter("districtStudentId", "1")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The externalServiceId parameter is invalid."))
  }

  test("query invalid objectName") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("contextId", "1"),
      SifRequestParameter("districtStudentId", "1"),
      SifRequestParameter("externalServiceId", "1")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The objectName parameter is invalid."))
  }

  test("query invalid objectType") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("contextId", "1"),
      SifRequestParameter("districtStudentId", "1"),
      SifRequestParameter("externalServiceId", "1"),
      SifRequestParameter("objectName", "1")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The objectType parameter is invalid."))
  }

  test("query invalid zoneId") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("contextId", "1"),
      SifRequestParameter("districtStudentId", "1"),
      SifRequestParameter("externalServiceId", "1"),
      SifRequestParameter("objectName", "1"),
      SifRequestParameter("objectType", "1")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage().equals("The zoneId parameter is invalid."))
  }

  test("query invalid filter parameters") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "2"),
      SifRequestParameter("contextId", "DEFAULT"),
      SifRequestParameter("districtStudentId", "xsreSample1"),
      SifRequestParameter("externalServiceId", "5"),
      SifRequestParameter("objectName", "xSre"),
      SifRequestParameter("objectType", "xSre"),
      SifRequestParameter("zoneId", "foo")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.InternalServerError)
    assert(result.exceptions.head.getMessage().contains("Service[filters] not found in Domain[psesd], Solution[dev], Zone[foo], Context[DEFAULT]"))
  }

  ignore("query valid") {
    val result = SifObject.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "2"),
      SifRequestParameter("contextId", "DEFAULT"),
      SifRequestParameter("districtStudentId", "11362710"),
      SifRequestParameter("externalServiceId", "5"),
      SifRequestParameter("objectName", "xSre"),
      SifRequestParameter("objectType", "xSre"),
      SifRequestParameter("zoneId", "seattle")
    ))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val body = result.toXml.get.toXmlString
    assert(body.contains("<localId>11362710</localId>"))
  }

}
