# Preferences Frontend Service
# Overview
Preferences frontend is a microservice to allow end users to manage their personal online communication preferences.
It provides a user journey that other services can integrate into their solutions so that they can also use this journey. In addition, it hosts a number of endpoints that can be consumed to provide user contact preference information.

In particular, it is associated with the ability to define how the customer receives information from HMRC. If the user would like to receive information electronically/online, they must also provide an email (for nudge purposes) which must then be verified.

HMRC communication method (online/digital or paper-based) can be updated at any time. Nudge email language can also be set - to either English or Welsh. Note that this language does not set the language for any online communication, only for nudge emails.

The process of requesting digital documents online is called "opting in".

- [Digital Contact Runbook](https://confluence.tools.tax.service.gov.uk/display/DCT/Digital+Contact+Runbook)
- [Preferences Frontend Service on Confluence](https://confluence.tools.tax.service.gov.uk/display/DCT/Preferences+Frontend+Service)
- [Digital Contact Confluence home page](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=DCT&title=Digital+Contact)
- [Digital Contact Slack channel - #team-digital-contact](https://hmrcdigital.slack.com/archives/C0J85LC3W)

# Integration

## Paperless User Journey Integration (redirect)
Permits users to choose whether to receive digital communications from HMRC or paper letters.
If the user chooses paperless, they must also enter a valid email address for nudges.

Redirect to this entrypoint URL, providing a return link and the text to display on that link.
```
${preferences-frontend-host}/paperless/check-settings?returnUrl=$url&returnLinkText=$text
```
| Name                 |          | Description                              |
|----------------------|----------|------------------------------------------|
| `returnUrl`          | Required | The return url                           |
| `returnLinkText`     | Required | The return link text                     |


## API Endpoints

### Description
| Path                          | Supported Methods | Description                                                                                     |
|-------------------------------|-------------------|-------------------------------------------------------------------------------------------------|
| `/paperless/activate`         | `PUT`             | Update T&Cs and return the status [More...](#put-paperlessactivate)                             |
| `/paperless/choose/capture`   | `GET`             | Return redirect response based on the underlying journey [More...](#get-paperlesschoosecapture) |
| `/paperless/choose/precheck`  | `GET`             | Returns the underlying journey [More...](#get-paperlesschooseprecheck)                          |
| `/paperless/status`           | `GET`             | Returns the paperless journey [More...](#get-paperlessstatus)                                   |

### PUT /paperless/activate
The request need to be authenticated.
Update the language of the term and conditions associated to the preference (if found) and then return the status

#### Query Parameters

* Note that the parameters value are encrypted

| Name                 |          | Description                              |
|----------------------|----------|------------------------------------------|
| `returnUrl`          | Required | The return url                           |
| `returnLinkText`     | Required | The return link text                     |
| `termsAndConditions` | Optional | Type of terms and generic and conditions |
| `email`              | Optional | Email                                    |
| `alreadyOptedInUrl`  | Optional | The already optin url                    |
| `regime`             | Optional | The regime                               |

#### Example request :

```
/paperless/activate?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&email=e9zVPuZh0zbnXYIdEw1gz%2FYGFxquqdJgiJJN8WJGzOQ%3D"
```

Responds with status:

* `200`

Response body:
```json
{
  "optedIn": true,
  "verifiedEmail": false
}
```
* `409`
* `412`

Response body:
```json
{
  "redirectUserTo": "someUrl"
}
```

### GET /paperless/choose/capture

Return redirect response based on the underlying journey the preference has.

#### Query Parameters

* Note that the parameters value are encrypted

| Name                 |          | Description                                     |
| -------------------- | -------- |-------------------------------------------------|
| `returnUrl`          | Required | The return url                                  |
| `returnLinkText`     | Required | The return link text                            |
| `regime`             | Required | The regime - should be `itsa`                   |

#### Example request :

```
/paperless/choose/capture?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&regime=KucfrgeglpOjHad59vo1xg%3D%3D"
```

Responds with status:

* `303` - Redirect to the return url and append the entityId

* `400`

Response body:
```
This feature is disabled
```
* `409`

Response body:

```json
{
  "reason": "MULTIPLE_PREFERENCES_FOUND"
}
```

### GET /paperless/choose/precheck

Returns the underlying journey associated to the authenticated user passed on into the header token.
(Note: This endpoint is for test purposes only.)

Responds with status:

* `200`

Response body:

```json
{
  "entityId": "fc607a50-47e8-11ec-a15c-8bdf69d670e8",
  "reason": "",
  "journeyType": "SILENT_REDIRECT"
}
```

* `400`

Response body:
```json
{
  "reason": "reason",
  "email": "email",
  "journeyType": "BOUNCE_EMAIL",
  "_type":"model.BounceEmailJourney"
}
```
* `409`

Response body:

```json
{
  "reason": "MULTIPLE_PREFERENCES_FOUND",
  "journeyType": "CONFLICT"
}
```

### GET /paperless/status

Returns the paperless journey in Json format

#### Query Parameters

* Note that the parameters value are encrypted

| Name                 |          | Description                                     |
| -------------------- | -------- |-------------------------------------------------|
| `returnUrl`          | Required | The return url                                  |
| `returnLinkText`     | Required | The return link text                            |

#### Example request :

```
/paperless/status?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D"
```

Responds with status:

* `200`

Response body:

```json
{
  "status": {
    "name": "ALRIGHT",
    "category": "INFO",
    "text": "You chose to get your Self Assessment tax letters online"
  },
  "url": {
    "link": "http://localhost:9024/paperless/check-settings?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D",
    "text": "Check your settings"
  }
}
```

# Developer Information

## Integration Testing
Prior to running integration tests, ensure the profile `DC_PREFERENCES_FRONTEND_IT` has been started with service manager.


## Run the project locally

`sbt run "9053 -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"`

## SBT tasks
```bash
# Format the code
sbt fmt

# Clean, build test and integration test
sbt clean test it/test

# Run a coverage report
sbt clean coverage test coverageReport
```


## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

