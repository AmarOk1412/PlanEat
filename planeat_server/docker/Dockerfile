FROM rust:latest

RUN apt-get update && apt-get install -y sqlite3 libsqlite3-dev
WORKDIR /usr/src/app

COPY . .

RUN cargo build --release

ENV RUST_LOG=info

RUN cargo build --release

EXPOSE 8080

CMD ["cargo", "run", "--release"]
