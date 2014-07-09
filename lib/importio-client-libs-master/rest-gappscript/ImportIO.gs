/**
* Import.io example for Google Sheets with Google Apps Script
*
* This file allows you to get a single page of import.io data from a single source using the REST
* API and insert it into a Google Sheets document.
*
* Dependencies: None
*
* @author: dev@import.io
* @source: https://github.com/import-io/importio-client-libs/tree/master/rest-gappscript
*
* Thanks to contributions from Martin Hawksey for starting and open-sourcing a previous version of this script
*/

/**
* This method is responsible for executing a query against import.io
*
* @param {string} the GUID of the data source to query from https://import.io/data/mine
* @param {object} a JSON object mapping input names to values
* @param {string} the import.io user's GUID from https://import.io/data/account
* @param {string} the import.io user's API key from https://import.io/data/account
*/
function query(connectorGuid, input, userGuid, apiKey) {
  
  // Configure the HTTP Request options
  var options = {
    "method": "post",
    "contentType": "application/json",
    "payload": JSON.stringify(input)
  };
  
  // Construct the URL that we are going to request
  var url = "https://api.import.io/store/connector/" + connectorGuid + "/_query?_user=" + userGuid + "&_apikey=" + encodeURIComponent(apiKey);
  
  // Request the data from import.io
  var resp = UrlFetchApp.fetch(url, options);  
  
  // Parse the response data into a standard object
  var data = JSON.parse(resp.getContentText()); 
  
  // Here we convert the data from import.io into a 2D array suitable for use in the spreadsheet
  var tbl = [];
  // Push in the header row
  tbl.push(Object.keys(data.results[0]));
  for (var i = 0; i < data.results.length; ++i){
    // Build up a row of results from each of the keys
    var row = [];
    for (r in data.results[i]){
      row.push(data.results[i][r]);
    }
    // Add the row to the table
    tbl.push(row);
  }
 
  // Use the spreadsheet service to write the data to a spreadsheet
  SpreadsheetApp.getActiveSpreadsheet()
                .getSheetByName("Sheet1")
                .getRange(1, 1, tbl.length, tbl[0].length)
                .setValues(tbl);
}