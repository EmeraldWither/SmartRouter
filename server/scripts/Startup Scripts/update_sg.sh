IP=$(curl -s  http://checkip.amazonaws.com)

GROUP_ID_PROXY="sg-08aa0a2fa783098ee"
GROUP_ID_PANEL="sg-0ba5040a87d7886a3"

aws ec2 authorize-security-group-ingress \
  --group-id "$GROUP_ID_PANEL" \
  --protocol all \
  --cidr "$IP/32"


aws ec2 authorize-security-group-ingress \
  --group-id "$GROUP_ID_PROXY" \
  --protocol all \
  --cidr "$IP/32"
