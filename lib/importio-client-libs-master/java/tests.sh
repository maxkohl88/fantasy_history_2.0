#!/bin/bash
# Arguments: host, username, password, user GUID, API key
pushd com.import.io.api.clientlite
	mvn test -Dhost="$1" -Dusername="$2" -Dpassword="$3" -DuserGuid="$4" -DapiKey="$5"
	res=$?
popd

exit $res