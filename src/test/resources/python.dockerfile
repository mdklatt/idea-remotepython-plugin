FROM linuxserver/openssh-server:version-9.3_p2-r0
RUN \
    apk add python3 && \
    python3 -m venv /opt/venv && \
    . /opt/venv/bin/activate && \
    python3 -m pip install cowsay
