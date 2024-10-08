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
use crate::Config;

// TODO: need -lsqlite3
use actix_web::{
    web::{Bytes, Data},
    HttpRequest, HttpResponse, Responder,
};
use log::error;
use rusqlite::{params, Connection, Result as SqlResult};
use serde::{Deserialize};
use serde_json::json;
use std::fs;
use std::path::Path;
use sha2::{Digest, Sha256};
use std::sync::{Arc, Mutex};

// Struct for the incoming `inbox` request
#[derive(Debug, Deserialize)]
pub struct InboxRequest {
    pub from: String,      // The sender's public key
    pub latest_sync: i64,   // The ID of the last synchronized message
}

// Struct to handle the incoming `outbox` requests
#[derive(Debug, Deserialize)]
pub struct OutboxRequest {
    pub to: String,      // Key recipient
    pub from: String,      // Key from sender
    pub message: String, // Message content
}

#[derive(Debug, Deserialize)]
pub struct LinkRequest {
    pub key1: String,
    pub key2: String,
}

#[derive(Debug, Clone)]
pub struct Server {
    pub config: Config,
}

pub struct ServerData {
    pub server: Arc<Mutex<Server>>,
    pub config: Config,
}

impl Server {


    pub async fn link(
        data: Data<ServerData>,
        bytes: Bytes,
        _req: HttpRequest,
    ) -> impl Responder {
        // Step 1: Parse the body as a UTF-8 string
        let body = match String::from_utf8(bytes.to_vec()) {
            Ok(body) => body,
            Err(e) => {
                error!("Failed to parse request body as UTF-8: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid UTF-8 in request body" }));
            }
        };

        // Step 2: Deserialize the JSON body into a `LinkRequest`
        let info: LinkRequest = match serde_json::from_str(&body) {
            Ok(info) => info,
            Err(e) => {
                error!("Failed to deserialize request body: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid JSON in request body" }));
            }
        };

        log::info!("POST Link request: {} with {}", info.key1, info.key2);

        // Step 3: Compute SHA-256 of `key1` and `key2`
        let sha_key1 = format!("{:x}", Sha256::digest(info.key1.as_bytes()));
        let sha_key2 = format!("{:x}", Sha256::digest(info.key2.as_bytes()));
        log::info!("SHA_KEY_1: {}, SHA_KEY_2: {}", sha_key1, sha_key2);

        // Step 4: Prepare the JSON payloads
        let json_key1 = json!({"action": "link", "link": true, "key": info.key2 });
        let json_key2 = json!({"action": "link", "link": true, "key": info.key1 });

        // Step 5: Construct the database paths
        let db_path1 = format!("data/{}/sync.db", sha_key1);
        let db_path2 = format!("data/{}/sync.db", sha_key2);

        // Step 6: Create directories if they don't exist
        if let Err(e) = fs::create_dir_all(Path::new(&*format!("data/{}", sha_key1))) {
            error!("Failed to create directory for SHA_KEY_1: {}", e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to create directory for key1" }));
        }
        if let Err(e) = fs::create_dir_all(Path::new(&*format!("data/{}", sha_key2))) {
            error!("Failed to create directory for SHA_KEY_2: {}", e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to create directory for key2" }));
        }

        // Step 7: Insert into the respective SQLite databases
        if let Err(e) = Self::insert_into_db(&db_path1, &json_key1.to_string()) {
            error!("Failed to insert into database for SHA_KEY_1: {}", e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to insert into database for key1" }));
        }
        if let Err(e) = Self::insert_into_db(&db_path2, &json_key2.to_string()) {
            error!("Failed to insert into database for SHA_KEY_2: {}", e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to insert into database for key2" }));
        }

        // Step 8: Return the HTTP response
        let activity_pub_mime = match "application/activity+json".parse::<mime::Mime>() {
            Ok(mime) => mime,
            Err(e) => {
                error!("Failed to parse MIME type: {}", e);
                return HttpResponse::InternalServerError()
                    .json(json!({ "status": "error", "message": "Failed to set MIME type" }));
            }
        };

        HttpResponse::Ok()
            .content_type(activity_pub_mime)
            .json(json!({ "status": "success", "message": "Link created successfully" }))
    }


    pub async fn unlink(
        data: Data<ServerData>,
        bytes: Bytes,
        _req: HttpRequest,
    ) -> impl Responder {
        // Step 1: Parse the body as a UTF-8 string
        let body = match String::from_utf8(bytes.to_vec()) {
            Ok(body) => body,
            Err(e) => {
                error!("Failed to parse request body as UTF-8: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid UTF-8 in request body" }));
            }
        };

        // Step 2: Deserialize the JSON body into a `LinkRequest`
        let info: LinkRequest = match serde_json::from_str(&body) {
            Ok(info) => info,
            Err(e) => {
                error!("Failed to deserialize request body: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid JSON in request body" }));
            }
        };

        log::info!("POST UnLink request: {} with {}", info.key1, info.key2);

        // Step 3: Compute SHA-256 of `key1` and `key2`
        let sha_key1 = format!("{:x}", Sha256::digest(info.key1.as_bytes()));
        let sha_key2 = format!("{:x}", Sha256::digest(info.key2.as_bytes()));
        log::info!("SHA_KEY_1: {}, SHA_KEY_2: {}", sha_key1, sha_key2);

        // Step 4: Prepare the JSON payloads
        let json_key1 = json!({"action": "link", "link": false, "key": info.key2 });
        let json_key2 = json!({"action": "link", "link": false, "key": info.key1 });

        // Step 5: Construct the database paths
        let db_path1 = format!("data/{}/sync.db", sha_key1);
        let db_path2 = format!("data/{}/sync.db", sha_key2);
        let p = format!("data/{}", sha_key1);
        let dir_path1 = Path::new(&p);

        if !dir_path1.exists() {
            error!("Directory for SHA_KEY: {} does not exist", sha_key1);
            return HttpResponse::BadRequest()
                .json(json!({ "status": "error", "message": "Recipient does not exist" }));
        }
        let p = format!("data/{}", sha_key2);
        let dir_path2 = Path::new(&p);

        if !dir_path2.exists() {
            error!("Directory for SHA_KEY: {} does not exist", sha_key2);
            return HttpResponse::BadRequest()
                .json(json!({ "status": "error", "message": "Recipient does not exist" }));
        }

        // Step 7: Insert into the respective SQLite databases
        if let Err(e) = Self::insert_into_db(&db_path1, &json_key1.to_string()) {
            error!("Failed to insert into database for SHA_KEY_1: {}", e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to insert into database for key1" }));
        }
        if let Err(e) = Self::insert_into_db(&db_path2, &json_key2.to_string()) {
            error!("Failed to insert into database for SHA_KEY_2: {}", e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to insert into database for key2" }));
        }

        // Step 8: Return the HTTP response
        let activity_pub_mime = match "application/activity+json".parse::<mime::Mime>() {
            Ok(mime) => mime,
            Err(e) => {
                error!("Failed to parse MIME type: {}", e);
                return HttpResponse::InternalServerError()
                    .json(json!({ "status": "error", "message": "Failed to set MIME type" }));
            }
        };

        HttpResponse::Ok()
            .content_type(activity_pub_mime)
            .json(json!({ "status": "success", "message": "Link removed successfully" }))
    }

    // Outbox method to handle the new POST request
    pub async fn outbox(
        _data: Data<ServerData>,
        bytes: Bytes,
        _req: HttpRequest,
    ) -> impl Responder {
        // TODO verify signature message is from key
        // Step 1: Parse the body as a UTF-8 string
        let body = match String::from_utf8(bytes.to_vec()) {
            Ok(body) => body,
            Err(e) => {
                error!("Failed to parse request body as UTF-8: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid UTF-8 in request body" }));
            }
        };

        // Step 2: Deserialize the JSON body into an `OutboxRequest`
        let info: OutboxRequest = match serde_json::from_str(&body) {
            Ok(info) => info,
            Err(e) => {
                error!("Failed to deserialize request body: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid JSON in request body" }));
            }
        };

        log::info!("POST Outbox request: to={}, message={}", info.to, info.message);

        // Step 3: Compute SHA-256 of the `to` key
        let sha_key = format!("{:x}", Sha256::digest(info.to.as_bytes()));
        log::info!("SHA_KEY: {}", sha_key);

        // Step 4: Prepare the JSON payload for the outbox action
        let json_payload = json!({ "action": "sync", "message": info.message, "from": info.from });

        // Step 4: Construct the database path and check if the directory exists
        let db_path = format!("data/{}/sync.db", sha_key);
        let p = format!("data/{}", sha_key);
        let dir_path = Path::new(&p);

        if !dir_path.exists() {
            error!("Directory for recipient {} (SHA_KEY: {}) does not exist", info.to, sha_key);
            return HttpResponse::BadRequest()
                .json(json!({ "status": "error", "message": "Recipient does not exist" }));
        }

        // Step 7: Insert the JSON payload into the SQLite database
        if let Err(e) = Self::insert_into_db(&db_path, &json_payload.to_string()) {
            error!("Failed to insert into database for SHA_KEY: {}", e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to insert into database" }));
        }

        // Step 8: Return the HTTP response indicating success
        HttpResponse::Ok()
            .json(json!({ "status": "success", "message": "Outbox message stored successfully" }))
    }

    // Inbox method to handle the new POST request
    pub async fn inbox(
        _data: Data<ServerData>,
        bytes: Bytes,
        _req: HttpRequest,
    ) -> impl Responder {
        // TODO verify signature from
        // Step 1: Parse the body as a UTF-8 string
        let body = match String::from_utf8(bytes.to_vec()) {
            Ok(body) => body,
            Err(e) => {
                error!("Failed to parse request body as UTF-8: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid UTF-8 in request body" }));
            }
        };

        // Step 2: Deserialize the JSON body into an `InboxRequest`
        let info: InboxRequest = match serde_json::from_str(&body) {
            Ok(info) => info,
            Err(e) => {
                error!("Failed to deserialize request body: {}", e);
                return HttpResponse::BadRequest()
                    .json(json!({ "status": "error", "message": "Invalid JSON in request body" }));
            }
        };

        log::info!("POST Inbox request: from={}, latest_sync={}", info.from, info.latest_sync);

        // Step 3: Compute SHA-256 of the `from` key
        let sha_key = format!("{:x}", Sha256::digest(info.from.as_bytes()));
        log::info!("SHA_KEY: {}", sha_key);

        // Step 4: Construct the database path and check if the directory exists
        let db_path = format!("data/{}/sync.db", sha_key);
        let p = format!("data/{}", sha_key);
        let dir_path = Path::new(&p);

        if !dir_path.exists() {
            error!("Directory for sender {} (SHA_KEY: {}) does not exist", info.from, sha_key);
            return HttpResponse::BadRequest()
                .json(json!({ "status": "error", "message": "Sender does not exist" }));
        }

        // Step 5: Retrieve messages from the database where `id > latest_sync`
        let messages = match Self::retrieve_messages(&db_path, info.latest_sync) {
            Ok(messages) => messages,
            Err(e) => {
                error!("Failed to retrieve messages for SHA_KEY {}: {}", sha_key, e);
                return HttpResponse::InternalServerError()
                    .json(json!({ "status": "error", "message": "Failed to retrieve messages" }));
            }
        };

        // Step 6: Delete messages with `id <= latest_sync` to clean up old data
        if let Err(e) = Self::delete_old_messages(&db_path, info.latest_sync) {
            error!("Failed to delete old messages for SHA_KEY {}: {}", sha_key, e);
            return HttpResponse::InternalServerError()
                .json(json!({ "status": "error", "message": "Failed to clean up old messages" }));
        }

        // Step 7: Return the formatted messages as a JSON response
        HttpResponse::Ok().json(messages)
    }

    // Helper function to insert data into the SQLite database with error handling
    fn insert_into_db(db_path: &str, action: &str) -> SqlResult<()> {
        let conn = Connection::open(db_path)?;
        conn.execute(
            "CREATE TABLE IF NOT EXISTS sync (id INTEGER PRIMARY KEY AUTOINCREMENT, action TEXT NOT NULL)",
            params![],
        )?;

        conn.execute(
            "INSERT INTO sync (action) VALUES (?1)",
            params![action],
        )?;

        log::info!("Data inserted into {}: {}", db_path, action);
        Ok(())
    }

    // Helper function to retrieve messages with ID greater than `latest_sync`
    fn retrieve_messages(db_path: &str, latest_sync: i64) -> SqlResult<Vec<serde_json::Value>> {
        let conn = Connection::open(db_path)?;
        let mut stmt = conn.prepare("SELECT id, action FROM sync WHERE id > ?1")?;
        let rows = stmt.query_map(params![latest_sync], |row| {
            Ok(json!({ "id": row.get::<_, i64>(0)?, "action": row.get::<_, String>(1)? }))
        })?;

        let mut messages = Vec::new();
        for message in rows {
            messages.push(message?);
        }
        Ok(messages)
    }

    // Helper function to delete messages with ID less than or equal to `latest_sync`
    fn delete_old_messages(db_path: &str, latest_sync: i64) -> SqlResult<()> {
        let conn = Connection::open(db_path)?;
        conn.execute("DELETE FROM sync WHERE id < ?1", params![latest_sync])?;
        log::info!("Old messages deleted from {}", db_path);
        Ok(())
    }
}