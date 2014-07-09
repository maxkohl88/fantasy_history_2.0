/**
* Import.io example for Google Sheets with Google Apps Script
*
* This file shows you how to use the import.io library in Google Apps
*
* Dependencies: None
*
* @author: dev@import.io
* @source: https://github.com/import-io/importio-client-libs/tree/master/rest-gappscript
*
* Thanks to contributions from Martin Hawksey for starting and open-sourcing a previous version of this script
*/

function execute() {
  // This is the connector GUID you are querying - get it from My Data at https://import.io/data/mine
  var connectorGuid = "caff10dc-3bf8-402e-b1b8-c799a77c3e8c";
  // This is the input object, a map of input names to properties
  var input = {
    "input": {
      "searchterm": "avengers"
    }
  }
  // Fill in your user GUID here, from https://import.io/data/account
  var userGuid = "YOUR_USER_GUID";
  // Fill in your API key here, from https://import.io/data/account
  var apiKey = "YOUR_API_KEY";
  
  // Call the method to get the data
  return ImportIO.query(connectorGuid, input, userGuid, apiKey);
}
