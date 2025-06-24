SERVER_ID=$(curl http://survivalcraft.emeraldcraft.org:25564/serverid)
echo "$SERVER_ID"
curl "https://panel.emeraldcraft.org/api/client/servers/$SERVER_ID/power" \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer ptlc_YBaW6TNeT7IyawVjtC5NPOxAhl0cP0oX9f92gb4z1cd' \
  -X POST \
  -d '{
  "signal": "start"
}'
