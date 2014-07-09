<?php
 
$userGuid = "YOUR_USER_GUID";
$apiKey = "YOUR_API_KEY";
$connectorGuid = "YOUR_CONNECTOR_GUID";
$connectorDomain = "YOUR_CONNECTOR_DOMAIN";
$username = "YOUR_SITE_USERNAME";
$password = "YOUR_SITE_PASSWORD";
 
function query($connectorGuid, $input, $userGuid, $apiKey, $additionalInput, $login) {
 
  $url = "https://api.import.io/store/connector/" . $connectorGuid . "/_query?_user=" . urlencode($userGuid) . "&_apikey=" . urlencode($apiKey);
 
  $data = array();
  if ($input) {
    $data["input"] = $input;
  }
  if ($additionalInput) {
    $data["additionalInput"] = $additionalInput;
  }
  if ($login) {
    $data["loginOnly"] = true;
  }
 
  $ch = curl_init($url);
  curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
  curl_setopt($ch, CURLOPT_POSTFIELDS,  json_encode($data));
  curl_setopt($ch, CURLOPT_POST, 1);
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
  curl_setopt($ch, CURLOPT_HEADER, 0);
  $result = curl_exec($ch);
  curl_close($ch);
 
  return json_decode($result);
}
 
$creds = array();
$creds[$connectorDomain] = array(
  "username" => $username,
  "password" => $password
);
 
$login = query($connectorGuid, false, $userGuid, $apiKey, array(
  $connectorGuid => array(
    "domainCredentials" => $creds
  )
), false);
 
$result = query($connectorGuid, array(
  "search" => "google",
), $userGuid, $apiKey, array(
  $connectorGuid => array(
    "cookies" => $login->cookies
  )
), false);
 
var_dump($result);