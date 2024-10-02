/**
 *  Copyright (c) 2022-2023, Sébastien Blin <sebastien.blin@enconn.fr>
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/
mod config;
mod server;

use crate::config::Config;
use crate::server::{Server, ServerData};

use actix_web::{web, web::Data, App, HttpServer};
use std::fs;
use std::sync::{Arc, Mutex};

fn main() {
    // Init logging
    env_logger::init();
    // Run actix_web with tokio to allow both incoming and outgoing requests
    actix_web::rt::System::with_tokio_rt(|| {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .worker_threads(8)
            .thread_name("main-tokio")
            .build()
            .unwrap()
    })
    .block_on(run_server());
}

async fn run_server() {
    let config_str = fs::read_to_string("config.json");
    let config = serde_json::from_str::<Config>(&config_str.unwrap()).unwrap();
    let server = Arc::new(Mutex::new(Server {
        config: config.clone(),
    }));
    let data = Data::new(ServerData {
        server: server.clone(),
        config: config.clone(),
    });
    log::info!("Launching server on: {}", config.bind_address);
    HttpServer::new(move || {
        App::new()
            .app_data(data.clone())
            .route("/link", web::post().to(Server::link))
            .route("/unlink", web::post().to(Server::unlink))
            .route("/outbox", web::post().to(Server::outbox))
            .route("/inbox", web::post().to(Server::inbox))
    })
    .bind(&*config.bind_address)
    .unwrap()
    .run()
    .await
    .unwrap()
}
