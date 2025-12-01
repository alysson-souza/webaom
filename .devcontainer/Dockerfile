FROM mcr.microsoft.com/devcontainers/java:21

# Install VNC and lightweight desktop for Swing
RUN apt-get update && apt-get install -y \
    fluxbox \
    fonts-dejavu \
    libxi6 \
    libxrender1 \
    libxtst6 \
    novnc \
    tigervnc-common \
    tigervnc-standalone-server \
    websockify \
    xterm \
    && rm -rf /var/lib/apt/lists/*

# VNC startup script
COPY vnc-startup.sh /usr/local/bin/vnc-startup.sh

# Set VNC password (empty for no password)
RUN chmod +x /usr/local/bin/vnc-startup.sh && \
    mkdir -p /home/vscode/.vnc && \
    echo "" | vncpasswd -f > /home/vscode/.vnc/passwd && \
    chmod 600 /home/vscode/.vnc/passwd && \
    chown -R vscode:vscode /home/vscode/.vnc

EXPOSE 5901 6080
