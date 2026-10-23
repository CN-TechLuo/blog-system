# 备份容器：MySQL 8 客户端 + gzip + OpenSSL（AES-256-CBC）
FROM mysql:8.0

RUN microdnf install -y gzip openssl \
    && microdnf clean all

COPY ops/backup.sh /ops/backup.sh

ENTRYPOINT ["/bin/bash", "-c"]
CMD ["while true; do bash /ops/backup.sh /backup || echo '[backup] failed'; sleep 86400; done"]
