package ai.obsidian.agent.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object Envelope {

    private const val WRAP_ALIAS = "obsidian.wrap"
    private const val PREFS = "obsidian.id"
    private const val MAX_SKEW_SECONDS = 600L

    private val random = SecureRandom()

    class Ctx(val deviceId: String, val peerPub: ByteArray)

    fun ensureIdentity(context: Context, deviceId: String, operatorPub: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains("priv")) return
        val keyPair = X25519PrivateKeyParameters(random)
        prefs.edit()
            .putString("priv", wrap(keyPair.encoded))
            .putString("pub", encode(keyPair.generatePublicKey().encoded))
            .putString("device_id", deviceId)
            .putString("op_pub", operatorPub)
            .putLong("seq", 0L)
            .putLong("in_seq", 0L)
            .apply()
    }

    fun ctx(context: Context): Ctx {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val deviceId = requireNotNull(prefs.getString("device_id", null)) { "no identity" }
        val operatorPub = requireNotNull(prefs.getString("op_pub", null)) { "no operator key" }
        return Ctx(deviceId, Base64.decode(operatorPub, Base64.NO_WRAP))
    }

    fun devicePublicKey(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("pub", "").orEmpty()

    private fun nextSeq(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getLong("seq", 0L) + 1L
        prefs.edit().putLong("seq", next).commit()
        return next
    }

    fun seal(context: Context, ctx: Ctx, kind: String, plaintext: ByteArray): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val privateRaw = unwrap(requireNotNull(prefs.getString("priv", null)))
        val devicePrivate = X25519PrivateKeyParameters(privateRaw, 0)
        val peerPublic = X25519PublicKeyParameters(ctx.peerPub, 0)
        val ephemeral = X25519PrivateKeyParameters(random)

        val ikm = ByteArray(64)
        agree(ephemeral, peerPublic).copyInto(ikm, 0)
        agree(devicePrivate, peerPublic).copyInto(ikm, 32)

        val seq = nextSeq(context)
        val timestamp = System.currentTimeMillis() / 1000
        val ephemeralPub = ephemeral.generatePublicKey().encoded
        val nonce = ByteArray(12).also { random.nextBytes(it) }

        val aad = buildHeader(ctx.deviceId, seq, timestamp, ephemeralPub, nonce)
        val key = hkdf(ikm, sha256(aad), "obsidian/v2/${ctx.deviceId}".toByteArray(), 32)
        ikm.fill(0)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)

        return JSONObject()
            .put("v", 1)
            .put("d", ctx.deviceId)
            .put("s", seq)
            .put("t", timestamp)
            .put("e", encode(ephemeralPub))
            .put("n", encode(nonce))
            .put("k", kind)
            .put("c", encode(ciphertext))
            .toString()
    }

    fun open(context: Context, ctx: Ctx, envelope: String): ByteArray {
        val json = JSONObject(envelope)
        require(json.getInt("v") == 1) { "unsupported version" }
        require(json.getString("d") == ctx.deviceId) { "device mismatch" }

        val timestamp = json.getLong("t")
        val now = System.currentTimeMillis() / 1000
        require(Math.abs(now - timestamp) <= MAX_SKEW_SECONDS) { "timestamp skew" }

        val seq = json.getLong("s")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        require(seq > prefs.getLong("in_seq", 0L)) { "replay" }
        prefs.edit().putLong("in_seq", seq).commit()

        val privateRaw = unwrap(requireNotNull(prefs.getString("priv", null)))
        val devicePrivate = X25519PrivateKeyParameters(privateRaw, 0)
        val operatorEphemeral =
            X25519PublicKeyParameters(Base64.decode(json.getString("e"), Base64.NO_WRAP), 0)
        val operatorStatic = X25519PublicKeyParameters(ctx.peerPub, 0)

        val ikm = ByteArray(64)
        agree(devicePrivate, operatorEphemeral).copyInto(ikm, 0)
        agree(devicePrivate, operatorStatic).copyInto(ikm, 32)

        val ephemeralPub = Base64.decode(json.getString("e"), Base64.NO_WRAP)
        val nonce = Base64.decode(json.getString("n"), Base64.NO_WRAP)
        val aad = buildHeader(ctx.deviceId, seq, timestamp, ephemeralPub, nonce)
        val key = hkdf(ikm, sha256(aad), "obsidian/v2/${ctx.deviceId}".toByteArray(), 32)
        ikm.fill(0)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(Base64.decode(json.getString("c"), Base64.NO_WRAP))
    }

    private fun buildHeader(
        deviceId: String,
        seq: Long,
        timestamp: Long,
        ephemeralPub: ByteArray,
        nonce: ByteArray
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write("OBS2".toByteArray())
        out.write(1)
        out.write(deviceId.toByteArray())
        out.write(seq.toString().toByteArray())
        out.write(timestamp.toString().toByteArray())
        out.write(ephemeralPub)
        out.write(nonce)
        return out.toByteArray()
    }

    private fun agree(
        privateKey: X25519PrivateKeyParameters,
        publicKey: X25519PublicKeyParameters
    ): ByteArray {
        val shared = ByteArray(32)
        X25519Agreement().apply { init(privateKey) }.calculateAgreement(publicKey, shared, 0)
        require(shared.any { it != 0.toByte() }) { "low-order peer point" }
        return shared
    }

    private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(1)
        val okm = mac.doFinal().copyOf(length)
        prk.fill(0)
        return okm
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun keyStore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun wrapKey(): SecretKey {
        val store = keyStore()
        if (!store.containsAlias(WRAP_ALIAS)) {
            val builder = KeyGenParameterSpec.Builder(
                WRAP_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
            val generator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            runCatching {
                generator.init(builder.setIsStrongBoxBacked(true).build())
                generator.generateKey()
            }.onFailure {
                generator.init(builder.setIsStrongBoxBacked(false).build())
                generator.generateKey()
            }
        }
        return store.getKey(WRAP_ALIAS, null) as SecretKey
    }

    private fun wrap(data: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrapKey())
        return encode(cipher.iv + cipher.doFinal(data))
    }

    private fun unwrap(blob: String): ByteArray {
        val raw = Base64.decode(blob, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, wrapKey(), GCMParameterSpec(128, raw, 0, 12))
        return cipher.doFinal(raw, 12, raw.size - 12)
    }
}
