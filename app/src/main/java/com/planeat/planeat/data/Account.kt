package com.planeat.planeat.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RequiresApi(Build.VERSION_CODES.O)
class Account(private val context: Context, private val onMessage: (String)->Unit) {
    var id: String = ""
    var linkedAccounts = mutableListOf<String>()
        private set(value) {
            field = value
            saveLinkedAccounts() // Save to file every time linkedAccounts is updated
        }

    private var latestSync = 0L // To keep track of the latest sync ID
    private val executor = Executors.newSingleThreadScheduledExecutor()

    companion object {
        private const val KEY_ALIAS = "PlanEat"
        private const val LINKED_ACCOUNTS_FILE = "linkedAccounts"
        private const val SYNC_INTERVAL = 10L // Interval in minutes
    }

    fun startSync() {
        // Schedule the sync() method to run every 10 minutes
        executor.scheduleWithFixedDelay({ sync() }, 0, SYNC_INTERVAL, TimeUnit.MINUTES)
    }

    init {
        loadLinkedAccounts() // Load linked accounts on initialization
        loadLatestSync()
        if (linkedAccounts.isNotEmpty()) {
            startSync()
        }
    }

    private fun generateECCKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
        )
        val purposes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        } else {
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        }
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            purposes
        )
            .setKeySize(256)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        keyPairGenerator.initialize(keyGenParameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadAccount() {
        // Load the KeyStore and check if the private key already exists
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val privateKeyExists = keyStore.containsAlias(KEY_ALIAS)

        val accountDir = File(context.filesDir, "account")
        val publicKeyFile = File(accountDir, "pub.key")

        if (!privateKeyExists || !publicKeyFile.exists()) {
            // Either private key or public key does not exist: Generate a new account
            generateAccount()
        } else {
            // Load the existing public key from file
            val publicKeyBytes = publicKeyFile.readBytes()
            val publicKey = loadPublicKey(publicKeyBytes)
            id = sha256(publicKey.encoded)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun exportPublicKey(): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // Retrieve the public key using the alias
        val publicKeyEntry = keyStore.getCertificate(KEY_ALIAS)?.publicKey
            ?: throw NullPointerException("Public key not found in Keystore")

        // Return the public key as a Base64 encoded string
        return Base64.getEncoder().encodeToString(publicKeyEntry.encoded)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateAccount() {
        val keyPair = generateECCKeyPair()

        val accountDir = File(context.filesDir, "account")
        if (!accountDir.exists()) accountDir.mkdirs()

        // Save the generated public key to a file
        File(accountDir, "pub.key").writeBytes(keyPair.public.encoded)

        id = sha256(keyPair.public.encoded)
    }

    private fun loadPublicKey(publicKeyBytes: ByteArray): PublicKey {
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(keySpec)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return Base64.getEncoder().encodeToString(hash)
    }

    // Encryption using recipient's public key (shared secret)
    fun encrypt(data: ByteArray, recipientPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        val privateKey = getPrivateKey()
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(recipientPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        return aesEncrypt(data, sharedSecret)
    }

    // Decrypt with *this account's* private key
    fun decrypt(encryptedData: ByteArray, fromPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        val privateKey = getPrivateKey()

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(fromPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        return aesDecrypt(encryptedData, sharedSecret)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun sync() {
        try {
            // Prepare the JSON payload with the `from` key and the `latest_sync` value
            val payload = JSONObject().apply {
                put("from", exportPublicKey())
                put("latest_sync", latestSync)
            }

            // Send POST request to the inbox endpoint
            val url = URL("https://planeat.cha-cu.it/inbox")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            // Write the payload to the request
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            // Read the response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                handleInboxResponse(JSONArray(response))
            } else {
                Log.e("PlanEat", "Sync failed: HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.e("PlanEat", "Error during sync: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendMessage(recipientPublicKey: String, message: String) {
        try {
            // Create the JSON payload for the request
            val payload = JSONObject().apply {
                put("from", exportPublicKey())
                put("to", recipientPublicKey)
                put("message", message)
            }

            // Send POST request to the outbox endpoint
            val url = URL("https://planeat.cha-cu.it/outbox")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            // Write the payload to the request
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            // Read the response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.i("PlanEat", "Message sent successfully: $response")
            } else {
                Log.e("PlanEat", "Failed to send message: HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.e("PlanEat", "Error during message sending: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleInboxResponse(response: JSONArray) {
        var highestId = latestSync
        Log.i("PlanEat", "Handling inbox response with $response")
        for (i in 0 until response.length()) {
            try {
                val item = response.getJSONObject(i)
                val actionObjString = item.getString("action")
                val actionObj = JSONObject(actionObjString)
                val action = actionObj.getString("action")
                highestId = maxOf(highestId, item.getLong("id"))

                when (action) {
                    "link" -> {
                        val key = actionObj.getString("key")
                        if (!linkedAccounts.contains(key)) {
                            linkedAccounts.add(key)
                            saveLinkedAccounts()
                            Log.i("PlanEat", "New account linked: $key")
                        }
                    }

                    "sync" -> {
                        val fromKey = actionObj.getString("from")
                        val message = actionObj.getString("message")

                        // Check if the sender is a linked account
                        if (!linkedAccounts.contains(fromKey)) {
                            Log.e("PlanEat", "Received message from unknown sender: $fromKey")
                            continue
                        }


                        val senderPublicKeyBytes = Base64.getDecoder().decode(fromKey)
                        val senderPublicKey = loadPublicKey(senderPublicKeyBytes)

                        val messageContent = readMessage(senderPublicKey, message.toString())?: ""

                        Log.i("PlanEat", "Decrypted message: $messageContent")
                        onMessage(messageContent)
                    }

                    else -> {
                        Log.w("PlanEat", "Unhandled action: $action")
                    }
                }
            } catch (e: Exception) {
                Log.e("PlanEat", "Error handling inbox response: ${e.message}")
            }
        }

        if (highestId > latestSync) {
            latestSync = highestId
            saveLatestSync()
        }
    }


    fun stopSync() {
        executor.shutdown()
    }


    // Private
    private fun getPrivateKey(): PrivateKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // Use the alias to fetch the private key
        val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: throw NullPointerException("Private key not found in Keystore")

        return privateKeyEntry.privateKey
    }

    private fun aesEncrypt(data: ByteArray, sharedSecret: ByteArray): ByteArray {
        val aesKey = SecretKeySpec(sharedSecret.take(16).toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
        return iv + cipher.doFinal(data)
    }

    private fun aesDecrypt(encryptedData: ByteArray, sharedSecret: ByteArray): ByteArray {
        val aesKey = SecretKeySpec(sharedSecret.take(16).toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        // Extract the IV (first 16 bytes) and the actual ciphertext
        val iv = encryptedData.copyOfRange(0, 16)
        val cipherText = encryptedData.copyOfRange(16, encryptedData.size)

        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
        return cipher.doFinal(cipherText)
    }


    private fun loadLinkedAccounts() {
        val file = File(context.cacheDir, LINKED_ACCOUNTS_FILE)
        if (file.exists()) {
            val content = file.readText()
            linkedAccounts = content.split(",").filter { it.isNotBlank() }.toMutableList()
        }
    }

    private fun saveLinkedAccounts() {
        val file = File(context.cacheDir, LINKED_ACCOUNTS_FILE)
        file.writeText(linkedAccounts.joinToString(","))
    }

    // Load latest sync ID from app_cache/latestSync
    private fun loadLatestSync() {
        val latestSyncFile = File(context.cacheDir, "latestSync")
        if (latestSyncFile.exists()) {
            latestSync = latestSyncFile.readText().toLongOrNull() ?: 0L
        }
    }

    // Save latest sync ID to app_cache/latestSync
    private fun saveLatestSync() {
        val latestSyncFile = File(context.cacheDir, "latestSync")
        latestSyncFile.writeText(latestSync.toString())
    }

    // Server communication

    @RequiresApi(Build.VERSION_CODES.O)
    fun link(key: String): Boolean {
        if (linkedAccounts.contains(key))
            return true // Already linked
        // Export the public key from the AndroidKeyStore
        val myPublicKey = exportPublicKey()

        // Create the JSON payload for the POST request
        val jsonBody = JSONObject().apply {
            put("key1", myPublicKey)
            put("key2", key)
        }

        val url = URL("https://planeat.cha-cu.it/link") // Use the appropriate server URL
        val connection = url.openConnection() as HttpURLConnection

        return try {
            // Set up the connection properties
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Send the JSON payload
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(jsonBody.toString().toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            // Get the response code and read the response
            val responseCode = connection.responseCode
            val response = StringBuilder()
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(connection.inputStream.reader())
                reader.useLines { lines ->
                    lines.forEach { response.append(it) }
                }
                reader.close()
            } else {
                Log.e("PlanEat", "Failed to link account: HTTP error code $responseCode")
                return false
            }

            // Parse the JSON response
            val jsonResponse = JSONObject(response.toString())
            val status = jsonResponse.optString("status")
            Log.i("PlanEat", "Link status: $status")

            // Check if the status is 'success'
            if (status == "success") {
                linkedAccounts.add(key)
                saveLinkedAccounts()
            }
            status == "success"
        } catch (e: Exception) {
            Log.e("PlanEat", "Error during link operation: ${e.message}")
            false
        } finally {
            connection.disconnect()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun writeMessage(recipientPublicKey: String, message: String): String {
        try {
            val recipientKeyBytes = Base64.getDecoder().decode(recipientPublicKey)
            val recipientKey = loadPublicKey(recipientKeyBytes)

            // Encrypt the message with the recipient's public key
            val encryptedData = encrypt(message.toByteArray(StandardCharsets.UTF_8), recipientKey)

            // Sign the encrypted data using the private key
            val privateKey = getPrivateKey()
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            signature.update(encryptedData)
            val signedData = signature.sign()

            // Create a JSON object with "sig" and "data"
            val messageJson = JSONObject()
            messageJson.put("sig", Base64.getEncoder().encodeToString(signedData))
            messageJson.put("data", Base64.getEncoder().encodeToString(encryptedData))

            return messageJson.toString()
        } catch (e: Exception) {
            Log.e("PlanEat", "Error in writeMessage: ${e.message}")
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun readMessage(senderPublicKey: PublicKey, jsonMessage: String): String? {
        try {
            // Parse the JSON object to extract "sig" and "data"
            val messageJson = JSONObject(jsonMessage)
            val signature = Base64.getDecoder().decode(messageJson.getString("sig"))
            val encryptedData = Base64.getDecoder().decode(messageJson.getString("data"))

            // Verify the signature using the sender's public key
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(senderPublicKey)
            verifier.update(encryptedData)
            if (!verifier.verify(signature)) {
                Log.e("PlanEat", "Signature verification failed")
                return null
            }

            // Decrypt the message using the recipient's private key
            val decryptedData = decrypt(encryptedData, senderPublicKey)
            return String(decryptedData, StandardCharsets.UTF_8)
        } catch (e: BadPaddingException) {
            Log.e("PlanEat", "Decryption failed: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e("PlanEat", "Error in readMessage: ${e.message}")
            throw e
        }
    }

}
