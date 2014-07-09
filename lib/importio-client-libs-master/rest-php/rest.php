<?php

// Put your User GUID here
$userGuid = "";
// Put your API key here
$apiKey = "";

function query($connectorGuid, $input, $userGuid, $apiKey) {

	$url = "https://query.import.io/store/connector/" . $connectorGuid . "/_query?_user=" . urlencode($userGuid) . "&_apikey=" . urlencode($apiKey);

	$ch = curl_init($url);
	curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
	curl_setopt($ch, CURLOPT_POSTFIELDS,  json_encode(array("input" => $input)));
	curl_setopt($ch, CURLOPT_POST, 1);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($ch, CURLOPT_HEADER, 0);
	$result = curl_exec($ch);
	curl_close($ch);

	return json_decode($result);
}

// Example of doing a query
$result = query("39df3fe4-c716-478b-9b80-bdbee43bfbde", array(
	"input" => "query",
), $userGuid, $apiKey);

var_dump($result);
