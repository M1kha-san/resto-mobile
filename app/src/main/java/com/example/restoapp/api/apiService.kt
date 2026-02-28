package com.example.restoapp.api

import android.util.Log
import androidx.navigation.NavController
import com.example.restoapp.models.LoginResponse
import com.example.restoapp.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Service API untuk fitur autentikasi (login pegawai).
 *
 * Catatan: Modul ini hanya dipakai oleh pegawai yang login
 * melalui aplikasi mobile. Customer tidak perlu login.
 */
object ApiService {

    // Alamat dasar API backend (10.0.2.2 = localhost dari emulator Android)
    private const val BASE_URL = "https://10.0.2.2:7154"

    // Token dan data user yang sedang login
    var token = ""
    var currentUser: User? = null

    /**
     * Menonaktifkan validasi sertifikat SSL.
     * HANYA untuk keperluan development, JANGAN dipakai di production.
     */
    private fun trustAllCertificates() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal setup SSL trust: ${e.message}")
        }
    }

    /**
     * Proses login ke API backend.
     *
     * Mengirim username & password, lalu mengarahkan pengguna
     * ke halaman yang sesuai berdasarkan role-nya.
     */
    suspend fun login(
        username: String,
        password: String,
        navController: NavController
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {

        trustAllCertificates()

        val url = "$BASE_URL/api/Auth/login"
        Log.d(TAG, "Menghubungkan ke: $url")

        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Accept-Charset", "UTF-8")
        connection.doOutput = true
        connection.doInput = true

        return@withContext try {
            // Siapkan data JSON untuk dikirim
            val json = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            // Kirim data login ke server
            connection.outputStream.use { out ->
                out.write(json.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")

            // Tangani redirect dari server
            if (responseCode in listOf(301, 302, 307)) {
                val redirectUrl = connection.getHeaderField("Location")
                Log.e(TAG, "Server redirect ke: $redirectUrl")
                return@withContext Result.failure(
                    Exception("Server redirect (kode: $responseCode). Periksa URL backend.")
                )
            }

            // Baca respons dari server
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: connection.inputStream.bufferedReader().use { it.readText() }
            }

            Log.d(TAG, "Response: $response")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Login gagal: $response"))
            }

            // Parse data user dari respons
            val jsonResponse = JSONObject(response)
            val userObj = if (jsonResponse.has("data")) {
                jsonResponse.getJSONObject("data")
            } else {
                jsonResponse
            }

            val userId = userObj.optInt("userId", 0)
            val usernameResp = userObj.optString("username", username)
            val nama = userObj.optString("nama", username)
            val email = userObj.optString("email", "")
            val role = userObj.optString("role", "User")
            val alamat = userObj.optString("alamat", "")
            val noTelp = userObj.optString("noTelp", "")

            token = username
            currentUser = User(
                id = userId,
                username = usernameResp,
                nama = nama,
                email = email,
                role = role,
                alamat = alamat,
                noTelp = noTelp
            )

            Log.d(TAG, "Login berhasil! User: $usernameResp, Role: $role")

            // Arahkan ke halaman sesuai role
            withContext(Dispatchers.Main) {
                when (currentUser?.role?.lowercase()) {
                    "pegawai", "admin", "kasir", "staff" -> {
                        navController.navigate("pegawai_dashboard") {
                            popUpTo("welcome") { inclusive = false }
                        }
                    }
                    "anggota", "user", "pengunjung" -> {
                        navController.navigate("pengunjung_home") {
                            popUpTo("welcome") { inclusive = false }
                        }
                    }
                    else -> {
                        // Default: arahkan ke dashboard pegawai
                        navController.navigate("pegawai_dashboard") {
                            popUpTo("welcome") { inclusive = false }
                        }
                    }
                }
            }

            Result.success(LoginResponse(username, currentUser!!))

        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Result.failure(Exception("Kesalahan jaringan: ${e.message}"))
        } finally {
            connection.disconnect()
        }
    }

    // Tag untuk logging
    private const val TAG = "ApiService"

    // Proses logout - hapus token dan data user
    fun logout() {
        token = ""
        currentUser = null
    }
}

