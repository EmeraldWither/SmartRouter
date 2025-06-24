#!/bin/bash
"/home/ubuntu/Startup Scripts/update_dns.sh"
"/home/ubuntu/Startup Scripts/update_sg.sh"
echo Starting the Server
sleep 60
"/home/ubuntu/Startup Scripts/start_server.sh"