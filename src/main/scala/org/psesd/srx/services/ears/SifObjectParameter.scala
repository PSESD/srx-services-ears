package org.psesd.srx.services.ears

import org.psesd.srx.shared.core.extensions.ExtendedEnumeration

/** Enumeration of supported SIF object parameters.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  **/
object SifObjectParameter extends ExtendedEnumeration {
  type SifObjectParameter = Value
  val AuthorizedEntityId = Value("authorizedEntityId")
  val ContextId = Value("contextId")
  val DistrictStudentId = Value("districtStudentId")
  val ExternalServiceId = Value("externalServiceId")
  val ObjectName = Value("objectName")
  val ObjectType= Value("objectType")
  val PersonnelId = Value("personnelId")
  val ZoneId = Value("zoneId")
}