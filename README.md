# api-gw

GitOps config repo https://github.com/sadhal/multi-tenancy-team0-api-gw

```bash
API_GW=localhost:8080
API_GW=api-gw-tenant1-team0-api-gw.apps-crc.testing

curl $API_GW/anything/segm -H "Authorization: Bearer supersecure" -v -d @Dockerfile -H "Content-Type: application/json"

curl $API_GW/uuid -H "Authorization: Bearer supersecure" -v 


```