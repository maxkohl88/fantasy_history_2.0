package main

import "fmt"
import "encoding/json"
import "io/ioutil"
import "net/http"
import "net/url"
import "strings"
import _ "crypto/sha256"
import _ "crypto/sha512"

// Does a login
func login(username string, password string, domain string, connectorGuid string, client *http.Client, userguid string, apikey string) []interface{} {

  inputString,_ := json.Marshal(map[string]interface{}{
    "loginOnly": true,
    "additionalInput": map[string]interface{}{
      connectorGuid: map[string]interface{}{
        "domainCredentials": map[string]interface{}{
            domain: map[string]interface{}{
              "username": username,
              "password": password,
            },
        },
      },
    },
  })

  Url,_ := url.Parse("https://api.import.io/store/connector/" + connectorGuid + "/_query")
  parameters := url.Values{}
  parameters.Add("_user",userguid)
  parameters.Add("_apikey",apikey)
  Url.RawQuery = parameters.Encode()

  request, _ := http.NewRequest("POST", Url.String(), strings.NewReader(string(inputString)))
  request.Header.Add("Content-Type","application/json")
  resp,_ := client.Do(request)

  defer resp.Body.Close()
  body,_ := ioutil.ReadAll(resp.Body)
  var data map[string]interface{}
  json.Unmarshal(body, &data)
  return data["cookies"].([]interface{})
}

// Does a single query
func query(input map[string]interface{}, connectorGuid string, client *http.Client, userguid string, apikey string) {

  inputString,_ := json.Marshal(input)

  Url,_ := url.Parse("https://api.import.io/store/connector/" + connectorGuid + "/_query")
  parameters := url.Values{}
  parameters.Add("_user",userguid)
  parameters.Add("_apikey",apikey)
  Url.RawQuery = parameters.Encode()

  request, _ := http.NewRequest("POST", Url.String(), strings.NewReader(string(inputString)))
  request.Header.Add("Content-Type","application/json")
  resp,_ := client.Do(request)

  defer resp.Body.Close()
  body,_ := ioutil.ReadAll(resp.Body)

  fmt.Printf(string(body[:]))
    
}

func main() {

  client := &http.Client{}

  userguid := "YOUR_USER_GUID"
  apikey := "YOUR_API_KEY"
  connectorGuid := "YOUR_CONNECTOR_GUID"
  connectorDomain := "YOUR_CONNECTOR_DOMAIN"
  connectorUsername := "YOUR_CONNECTOR_USERNAME"
  connectorPassword := "YOUR_CONNECTOR_PASSWORD"

  cookies := login(connectorUsername, connectorPassword, connectorDomain, connectorGuid, client, userguid, apikey)

  query(map[string]interface{}{
    "input": map[string]interface{}{
      "???": "???",
    },
    "additionalInput": map[string]interface{}{
        connectorGuid: map[string]interface{}{
            "cookies": cookies,
        },
    },
  }, connectorGuid, client, userguid, apikey)

}