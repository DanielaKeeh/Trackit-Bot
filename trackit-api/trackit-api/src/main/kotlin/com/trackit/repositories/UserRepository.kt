package com.trackit.repositories

import com.trackit.models.Users
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class UserRecord(val id: Long, val username: String, val passwordHash: String, val salt: String)

/**
 * Hashing con PBKDF2 (incluido en el JDK, sin dependencias externas).
 * Equivalente en propósito a usar bcrypt, adaptado para no añadir otra
 * librería más al proyecto académico.
 */
object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun hash(password: String, saltB64: String): String {
        val salt = Base64.getDecoder().decode(saltB64)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun newSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun verify(password: String, saltB64: String, expectedHash: String): Boolean =
        hash(password, saltB64) == expectedHash
}

class UserRepository {

    suspend fun findByUsername(username: String): UserRecord? = newSuspendedTransaction(Dispatchers.IO) {
        Users.selectAll().where { Users.username eq username }
            .limit(1)
            .map { it.toRecord() }
            .firstOrNull()
    }

    suspend fun findById(id: Long): UserRecord? = newSuspendedTransaction(Dispatchers.IO) {
        Users.selectAll().where { Users.id eq id }
            .limit(1)
            .map { it.toRecord() }
            .firstOrNull()
    }

    suspend fun create(username: String, rawPassword: String): UserRecord = newSuspendedTransaction(Dispatchers.IO) {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash(rawPassword, salt)
        val id = Users.insertAndGetId {
            it[Users.username] = username
            it[Users.passwordHash] = hash
            it[Users.salt] = salt
            it[Users.createdAt] = LocalDateTime.now()
        }
        UserRecord(id.value, username, hash, salt)
    }

    private fun ResultRow.toRecord() = UserRecord(
        id = this[Users.id].value,
        username = this[Users.username],
        passwordHash = this[Users.passwordHash],
        salt = this[Users.salt]
    )
}
