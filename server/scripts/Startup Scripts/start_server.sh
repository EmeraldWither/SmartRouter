SERVER_ID=$(curl http://____________/serverid)
echo "$SERVER_ID"
curl "https://____________/api/client/servers/$SERVER_ID/power" \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer ____________' \
  -X POST \
  -d '{
  "signal": "start"
}'

# Start the java daemon
java -jar ServerDaemon-0.0.1-SNAPSHOT.jar --ptero.apikey=_______ --ptero.panelurl=_____