# srx-services-ears
**External Authorized Retrieval Service (EARS) for Student Record Exchange operations.**

EARS authorizes requests for xSRE student records, retrieves, filters and returns xSREs with limited data per filter rules defined in the Privacy Rules Service (PRS) for a particular authorized consumer.

***

## Configuration

### Environment Variables
Variable | Description | Example
-------- | ----------- | -------
ENVIRONMENT | Deployment environment name.  | development
LOG_LEVEL | Logging level (info, debug, error). | debug
ROLLBAR_ACCESS_TOKEN | Rollbar access token for error logging. | (see Heroku)
ROLLBAR_URL | URL to Rollbar API. | https://api.rollbar.com/api/1/item/
SERVER_API_ROOT  | Root path for this service. | (typically leave blank)
SERVER_HOST | Host IP for this service. | 127.0.0.1
SERVER_NAME | Server name for this service. | localhost
SERVER_PORT | Port this service listens on. | 8080
SERVER_URL | URL for this service. | http://localhost
SRX_ENVIRONMENT_URL | HostedZone environment URL. | https://psesd.hostedzone.com/svcs/dev/requestProvider
SRX_PRS_SESSION_TOKEN | HostedZone PRS session token assigned to this service. |(see HostedZone configuration)
SRX_PRS_SHARED_SECRET | HostedZone PRS session token assigned to this service. | (see HostedZone configuration)
SRX_SESSION_TOKEN | HostedZone session token assigned to this service. | (see HostedZone configuration)
SRX_SHARED_SECRET | HostedZone shared secret assigned to this service. | (see HostedZone configuration)

### HostedZone

The EARS service (srx-services-ears) must be registered in HostedZone as a new "environment" (application) that provides the following "services" (resources):

 * xSres

Once registered, the supplied HostedZone session token and shared secret should be set in the srx-services-ears host server (Heroku) environment variables (see above).

This EARS service must be further configured in HostedZone as follows:

Service | Zone | Context | Provide | Query | Create | Update | Delete
------- | ---- | ------- | ------- | ----- | ------ | ------ | ------
filters | default | default |   | X |   |  |
filters | [district*] | default |   | X |   |  |
filters | test | default |   | X |   |  |
filters | test | test |   | X |   |  |
masterXsres | default | default |   | X |   |  |
masterXsres | [district*] | default |   | X |   |  |
masterXsres | [district*] | district |   | X |   |  |
masterXsres | test | default |   | X |   |  |
masterXsres | test | district |   | X |   |  |
masterXsres | test | test |   | X |   |  |
srxMessages | default | default |   |   | X |  |
srxMessages | [district*] | default |   |   | X |  |
srxMessages | test | default |   |   | X |  |
srxMessages | test | test |   |   | X |  |
srxZoneConfig | default | default |   | X |   |  |
srxZoneConfig | [district*] | default |   | X |   |  |
srxZoneConfig | test | default |   | X |   |  |
srxZoneConfig | test | test |   | X |   |  |
xSres | default | default | X | X |   |  |
xSres | [district*] | default | X | X |   |  |
xSres | [district*] | CBO | X | X |   |  |
xSres | test | CBO | X | X |   |  |
xSres | test | default | X | X |   |  |
xSres | test | test | X | X |   |  |


[district*] = all district zones utilizing SRX services

## Usage

### xSREs
xSREs are retrieved via a GET request using the following URL format:

```
https://[baseUrl]/[objectType]/[objectId];zoneId=[zoneId];contextId=[contextId]
```

Variable | Description | Example
--------- | ----------- | -------
baseUrl   | URL of the deployment environment hosting the adapter endpoints. |  srx-services-ears-dev.herokuapp.com
objectType | The type of student record to retrieve. Currently only supports xSRE. |  xSres
objectId | District Student ID of the xSRE to retrieve. |  9245017
zoneId    | Zone containing the requested student xSRE record. | seattle
contextId | Client context of request. | CBO

The following required headers must be present in the GET request:

Header | Description | Example
------ | ----------- | -------
authorization | Must be set to a valid HMAC-SHA256 encrypted authorization token. | SIF_HMACSHA256 ZGNlYjgxZmQtNjE5My00NWVkL...
timeStamp | Must be set to a valid date/time in the following format: yyyy-MM-ddTHH:mm:ss:SSSZ | 2016-12-20T18:09:18.539Z
authorizedEntityId | PRS ID for the authorized entity making the request. | 2
districtStudentId | PRS District Student ID of the student to retrieve. | 9245017
externalServiceId | PRS ID of the external service making the request. | 5
objectType | The type of student record to retrieve. Currently only supports xSRE. | xSre

The following optional headers may also be included:

Header | Description | Example
------ | ----------- | -------
personnelId | PRS ID of the authorized personnel making the request. | 1
generatorId | Identification token of the “generator” of this request or event. | testgenerator
messageId | Consumer-generated. If specified, must be set to a valid UUID. | ba74efac-94c1-42bf-af8b-9b149d067816
messageType | If specified, must be set to: REQUEST | REQUEST
requestAction | If specified, must be set to: QUERY | QUERY
requestId | Consumer-generated. If specified, must be set to a valid UUID. | ba74efac-94c1-42bf-af8b-9b149d067816
requestType | If specified, must be set to: IMMEDIATE | IMMEDIATE
serviceType | If specified, must be set to: OBJECT | OBJECT

#### Example EARS GET request
```
GET
https://srx-services-ears-dev.herokuapp.com/xSres/1957207;zoneId=test;contextId=test

authorization: SIF_HMACSHA256 ZGNlYjgxZmQtNjE5My00NWVkL...
timestamp: 2016-12-20T18:09:18.539Z
authorizedEntityId: 2
districtStudentId: 1957207
externalServiceId: 5
objectType: xSre
```

***
#### Example EARS GET response
```
Content-Type: application/xml;charset=UTF-8
Messageid: c91035d5-8aba-49f3-8b77-f64cf5a6a5a8
Messagetype: RESPONSE
Responseaction: QUERY
Responsesource: PROVIDER
Servicetype: OBJECT

<?xml version="1.0" encoding="utf-8"?>
<xSre refId="0f0b1c58-be11-42b9-8b09-ca9fb24bf3bb" xmlns:raw="raw" xmlns:psesd="psesd" xmlns:sif="http://www.sifassociation.org/datamodel/na/3.3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
  <name>
    <familyName>Baratheon</familyName>
    <givenName>Stannis</givenName>
  </name>
  <localId>1957207</localId>
  ...
</xSre>
```