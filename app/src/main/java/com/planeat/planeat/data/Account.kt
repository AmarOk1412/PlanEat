package com.planeat.planeat.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Account(private val context: Context) {
    var id: String = ""

    companion object {
        private const val KEY_ALIAS = "PlanEat"
    }

    fun generateECCKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
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
            Log.e("PlanEat", "@@@ Generating new account and storing keys")
            generateAccount()
        } else {
            // Load the existing public key from file
            val publicKeyBytes = publicKeyFile.readBytes()
            val publicKey = loadPublicKey(publicKeyBytes)
            id = sha256(publicKey.encoded)
            Log.e("PlanEat", "@@@ Account Loaded: ID = $id")
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
        Log.e("PlanEat", "@@@ generateAccount")
        val keyPair = generateECCKeyPair()

        val accountDir = File(context.filesDir, "account")
        if (!accountDir.exists()) accountDir.mkdirs()

        // Save the generated public key to a file
        File(accountDir, "pub.key").writeBytes(keyPair.public.encoded)

        id = sha256(keyPair.public.encoded)
        Log.e("PlanEat", "@@@ generateAccount $id")
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
    fun decrypt(encryptedData: ByteArray): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        val privateKey = getPrivateKey()

        // Use the device's public key to establish the shared secret
        val publicKeyBytes = File(context.filesDir, "account/pub.key").readBytes()
        val publicKey = loadPublicKey(publicKeyBytes)

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        return aesDecrypt(encryptedData, sharedSecret)
    }

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
        val iv = encryptedData.copyOfRange(0, 16)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
        return cipher.doFinal(encryptedData.copyOfRange(16, encryptedData.size))
    }
}
