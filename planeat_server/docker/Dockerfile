FROM rust:latest

RUN apt-get update && apt-get install -y sqlite3 libsqlite3-dev
WORKDIR /usr/src/app

COPY . .

RUN cargo build --release

ENV RUST_LOG=info

RUN cargo build --release

EXPOSE 8080

# docker run -p9001:8080 -v/home/fedora/planeat_server_data:/usr/src/app/data -d --restart unless-stopped docker.io/amarok1412/planeat_server
CMD ["cargo", "run", "--release"]
