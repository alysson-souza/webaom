#!/bin/bash

# Start VNC server
vncserver :1 -geometry 1280x800 -depth 24 &

# Wait for VNC to start
sleep 2

# Start noVNC (browser-based VNC client)
/usr/share/novnc/utils/launch.sh --vnc localhost:5901 --listen 6080 &

echo ""
echo "============================================"
echo " VNC is running!"
echo " Browser: http://localhost:6080/vnc.html"
echo " VNC Client: localhost:5901"
echo "============================================"
echo ""

# Keep running
wait
