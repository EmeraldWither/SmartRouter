API_TOKEN=_______
EMAIL=_______
DOMAIN_ZONE=____
NAME=______
COMMENT="_______"
# <!> EDIT THESE #


SUBDOMAIN="$NAME._________"
IP=$(curl --silent https://checkip.amazonaws.com/)


echo API Token is $API_TOKEN
echo Email is $EMAIL
echo Domain Zone is $DOMAIN_ZONE
echo This servers name is $NAME
echo This servers subdomain is $SUBDOMAIN
echo "IP is \"$IP\""
echo "Server Comment is \"$COMMENT\""
echo
echo


# Get all DNS records for the zone
RESULT=$(curl --silent --request GET \
  --url "https://api.cloudflare.com/client/v4/zones/$DOMAIN_ZONE/dns_records" \
  --header "Content-Type: application/json" \
  --header "X-Auth-Email: $EMAIL" \
  --header "Authorization: Bearer $API_TOKEN")


# Filter the result to get the domain ID for the specific subdomain
DOMAIN_ID=$(echo "$RESULT" | jq -r --arg SUBDOMAIN "$SUBDOMAIN" '.result[] | select(.name == $SUBDOMAIN) | .id')

echo "Obtained Domain ID for $SUBDOMAIN: $DOMAIN_ID"

# update the domain
RESULT=$(curl --silent --request PUT \
  --url https://api.cloudflare.com/client/v4/zones/$DOMAIN_ZONE/dns_records/$DOMAIN_ID \
  --header 'Content-Type: application/json' \
  --header "X-Auth-Email: $EMAIL" \
  --header "Authorization: Bearer $API_TOKEN" \
  --data "{
  \"content\": \"$IP\",
  \"name\": \"$NAME\",
  \"proxied\": false,
  \"type\": \"A\",
  \"comment\": \"$COMMENT\",
  \"ttl\": 60,
  \"zone_id\": \"$DOMAIN_ZONE\"
}")

# Suppress output of the jq success
if [ "$(jq .success <<< $RESULT)" == "true" ]; then
        echo "Successfully updated the domain"
        exit 0
else
        echo "Could not update the domain. Here is the result of the response"
        jq . <<< $RESULT
        exit 1
fi
