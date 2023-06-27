
# Stage 1: Build Redis
FROM debian:buster-slim AS redis-builder

# Install build dependencies
RUN apt-get update && apt-get install -y build-essential tcl

# Download and compile Redis
WORKDIR /redis
RUN apt-get install -y wget && \
    wget http://download.redis.io/redis-stable.tar.gz && \
    tar xvzf redis-stable.tar.gz && \
    cd redis-stable && \
    make

# Stage 2: Final image with Redis and PHP

FROM php:7.4-cli

# Install Redis from the previous stage
COPY --from=redis-builder /redis/redis-stable/src/redis-server /usr/local/bin/
COPY --from=redis-builder /redis/redis-stable/src/redis-cli /usr/local/bin/
# RUN apt-get update && apt-get install timeout

COPY ./php /php

EXPOSE 8000
WORKDIR /php
ENTRYPOINT ["php", "-S", "0.0.0.0:8000"]

