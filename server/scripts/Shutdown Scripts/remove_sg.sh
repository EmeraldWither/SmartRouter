#!/bin/bash
IP=$(curl -s  http://checkip.amazonaws.com)

GROUP_ID_PROXY="_______"
GROUP_ID_PANEL="_______"

aws ec2 revoke-security-group-ingress \
  --group-id "$GROUP_ID_PANEL" \
  --protocol all \
  --cidr "$IP/32"


aws ec2 revoke-security-group-ingress \
  --group-id "$GROUP_ID_PROXY" \
  --protocol all \
  --cidr "$IP/32"
