# PlanEat Server

The PlanEat server, implemented in Rust, is designed to facilitate linking two user accounts within the PlanEat application. It enables secure sharing of agendas and shopping lists between users, providing a synchronized experience for meal planning. This server acts as a communication bridge between linked accounts, ensuring that updates made by one user are reflected on the other account.

## Build

```bash
cargo build --release
```

Or

```bash
docker build -t planeat_server -f docker/Dockerfile .
```

## Running

```bash
RUST_LOG=info cargo run
```

OR

```bash
docker run -p 9001:8080 -v $(pwd)/planeat_server_data:/usr/src/app/data -d --restart unless-stopped docker.io/amarok1412/planeat_server
```

This will start the server on port 9001, and it will persist data in the mounted directory $(pwd)/planeat_server_data.

## How it works

The PlanEat server provides secure endpoints to facilitate account linking and data synchronization. Here's a high-level overview of how it functions:

### Client part

The PlanEat client will generate a ed25519 on the device and will show a QRCode containing the public key. Second user will scan and generates a shared secret to encrypt messages. Once linked, both users can share a common agenda and shopping list.

### Server part

When server receives messages, it will prepare the inboxes for accounts with syncing messages/actions.
Basically, it will store messages to sync into a database under $sha256(public_key)/sync.db

Database got 2 keys, **id**, **action**.

**Id** must be used by client to check if message was already handled. **action** contains the message to handle.

For now, **action** can be:

For link/unlink: (link:true or false)
```json
{"action": "link", "link": false, "key": "OTHER_KEY" }
```

```json
json!({ "action": "sync", "message": "signed(encrypted(message))", "from": "OTHER_KEY" });
```

Message is:

*date* is a timestamp
*remove* is optional

```json
{
    "date": 0,
    "remove": true,
    "recipe": {
        "title": "...",
        "url": "http..."
    }
}
```

So that an account can know when to add and remove events.

## Endpoints

### 1. Link Accounts

- **Endpoint:** `/link`
- **Method:** `POST`
- **Description:** Links two accounts by computing SHA-256 hashes of their keys and inserting linking information into respective SQLite databases.
- **Request Body:**
    ```json
    {
        "key1": "<source_key>",
        "key2": "<target_key>"
    }
    ```
- **Response:**
  - **200 OK:** Successfully linked accounts.
    ```json
    {
        "status": "success",
        "message": "Link created successfully"
    }
    ```
  - **400 Bad Request:** Invalid input, such as improperly formatted JSON.
    ```json
    {
        "status": "error",
        "message": "Invalid JSON in request body"
    }
    ```
  - **500 Internal Server Error:** Issues during linking process, such as database insert failures or directory creation errors.
    ```json
    {
        "status": "error",
        "message": "Failed to insert into database for key1"
    }
    ```

### 2. Unlink Accounts

- **Endpoint:** `/unlink`
- **Method:** `POST`
- **Description:** Unlinks two accounts.
- **Request Body:**
    ```json
    {
        "key1": "<source_key>",
        "key2": "<target_key>"
    }
    ```
- **Response:**
  - **200 OK:** Successfully unlinked accounts.
  - **400 Bad Request:** Invalid input or accounts are not linked.
  - **500 Internal Server Error:** Server error during the unlinking process.

### 3. Outbox

- **Endpoint:** `/outbox`
- **Method:** `POST`
- **Description:** Sends a new message to the outbox for scheduled tasks.
- **Request Body:**
  ```json
  {
      "to": "<recipient_address>",
      "from": "<sender_address>",
      "message": "<message_content>"
  }
  ```
- **Response:**
  - **200 OK:** Successfully retrieved outbox.
    ```json
    {
        "status": "success",
        "message": "Outbox message stored successfully"
    }
    ```
  - **400 Bad Request:** Invalid request body or recipient does not exist.
    ```json
    {
        "status": "error",
        "message": "<error_message>"
    }
    ```
  - **500 Internal Server Error:** Server error during the outbox processing.
    ```json
    {
        "status": "error",
        "message": "Failed to insert into database"
    }
    ```

### 4. Inbox

- **Endpoint:** `/inbox`
- **Method:** `POST`
- **Description:** Retrieves messages from the inbox based on the latest sync ID.
- **Request Body:**
  ```json
    {
        "from": "<sender_address>",
        "latest_sync": <latest_sync_id>
    }
  ```
- **Response:**
  - **200 OK:** Successfully retrieved messages from the inbox.
    ```json
    [
        {
            "id": "<message_id>",
            "action": "<sync_action>"
        },
        ...
    ]
    ```
  - **400 Bad Request:**  Invalid request body or sender does not exist.
    ```json
    {
        "status": "error",
        "message": "<error_message>"
    }
    ```
  - **500 Internal Server Error:** Server error during the inbox retrieval process.
    ```json
    {
        "status": "error",
        "message": "Failed to insert into database"
    }
    ```