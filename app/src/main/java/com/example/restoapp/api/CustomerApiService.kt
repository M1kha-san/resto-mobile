package com.example.restoapp.api

import android.util.Log
import com.example.restoapp.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

 // Service API untuk fitur customer (menu, meja, transaksi)
object CustomerApiService {

    private const val BASE_URL = "https://10.0.2.2:7154/api"
    private const val TAG = "CustomerApiService"
    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 15000

    // Inisialisasi: nonaktifkan validasi SSL untuk development
    init {
        setupTrustAllCertificates()
    }

    // SSL trust untuk development only
    private fun setupTrustAllCertificates() {
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
            Log.e(TAG, "Gagal setup SSL: ${e.message}")
        }
    }

    // === HTTP HELPERS ===

    private suspend fun doGet(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL$endpoint"
        Log.d(TAG, "GET: $url")

        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        try {
            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Result.success(response)
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error tidak diketahui"
                Log.e(TAG, "Error: $error")
                Result.failure(Exception("HTTP $responseCode: $error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request gagal: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun doPost(endpoint: String, jsonBody: JSONObject): Result<String> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL$endpoint"
        Log.d(TAG, "POST: $url")
        Log.d(TAG, "Body: $jsonBody")

        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.doInput = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        try {
            // Kirim data ke server
            connection.outputStream.use { out ->
                out.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")

            val response = if (responseCode in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED)) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: connection.inputStream.bufferedReader().use { it.readText() }
            }
            Log.d(TAG, "Response: $response")

            if (responseCode in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED)) {
                Result.success(response)
            } else {
                Result.failure(Exception("HTTP $responseCode: $response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request gagal: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    // === API MEJA ===

    suspend fun getMeja(): Result<List<Meja>> {
        return try {
            val result = doGet("/Meja")
            result.fold(
                onSuccess = { response ->
                    val mejaList = parseJsonArrayResponse(response) { parseMeja(it) }
                    Result.success(mejaList)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getMeja error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Ambil data meja dengan cek silang transaksi aktif
    suspend fun getMejaWithActiveCheck(): Result<List<Meja>> {
        return try {
            // Langkah 1: Ambil data meja dari API
            val mejaList = getMeja().getOrElse { return Result.failure(it) }

            // Langkah 2: Ambil ID meja dari transaksi yang masih aktif
            val activeMejaIds = getActiveMejaIds()

            if (activeMejaIds.isEmpty()) {
                return Result.success(mejaList)
            }

            // Langkah 3: Timpa status meja jika ada transaksi aktif
            val updatedList = mejaList.map { meja ->
                if (meja.mejaId in activeMejaIds && meja.status.equals("Tersedia", ignoreCase = true)) {
                    meja.copy(status = "Terisi")
                } else {
                    meja
                }
            }

            Result.success(updatedList)
        } catch (e: Exception) {
            Log.e(TAG, "getMejaWithActiveCheck error: ${e.message}", e)
            getMeja() // Fallback ke data meja biasa
        }
    }

    private suspend fun getActiveMejaIds(): Set<Int> {
        val mejaIds = mutableSetOf<Int>()

        try {
            // Coba endpoint /Transaksi/pending terlebih dahulu
            val result = doGet("/Transaksi/pending")

            result.fold(
                onSuccess = { response ->
                    extractMejaIdsFromTransaksiResponse(response, mejaIds)
                },
                onFailure = {
                    // Jika gagal, coba GET /Transaksi lalu filter manual
                    Log.d(TAG, "/Transaksi/pending gagal, coba /Transaksi")
                    doGet("/Transaksi").fold(
                        onSuccess = { response ->
                            extractActiveMejaIdsFromAllTransaksi(response, mejaIds)
                        },
                        onFailure = { /* abaikan */ }
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getActiveMejaIds error: ${e.message}")
        }

        return mejaIds
    }

    // === API MENU ===

    suspend fun getMenu(): Result<List<Menu>> {
        return try {
            val result = doGet("/Menu")
            result.fold(
                onSuccess = { response ->
                    val menuList = parseJsonArrayResponse(response) { parseMenu(it) }
                    Result.success(menuList)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getMenu error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === API TRANSAKSI ===

    suspend fun createDineInTransaksi(namaKonsumen: String, idMeja: Int): Result<Transaksi> {
        return try {
            val jsonBody = JSONObject().apply {
                put("nama_konsumen", namaKonsumen)
                put("id_meja", idMeja)
            }

            doPost("/Transaksi/dine-in", jsonBody).fold(
                onSuccess = { response ->
                    val transaksi = parseTransaksiFromResponse(response)
                    Result.success(transaksi)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "createDineInTransaksi error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createTakeAwayTransaksi(namaKonsumen: String): Result<Transaksi> {
        return try {
            val jsonBody = JSONObject().apply {
                put("nama_konsumen", namaKonsumen)
            }

            doPost("/Transaksi/take-away", jsonBody).fold(
                onSuccess = { response ->
                    val transaksi = parseTransaksiFromResponse(response)
                    Result.success(transaksi)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "createTakeAwayTransaksi error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addItemToTransaksi(
        transaksiId: Int,
        idMenu: Int,
        jumlah: Int,
        catatan: String? = null
    ): Result<String> {
        return try {
            val jsonBody = JSONObject().apply {
                put("id_menu", idMenu)
                put("jumlah", jumlah)
                if (!catatan.isNullOrEmpty()) put("catatan", catatan)
            }

            doPost("/Transaksi/$transaksiId/items", jsonBody).fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "addItemToTransaksi error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getTransaksiById(transaksiId: Int): Result<Transaksi> {
        return try {
            doGet("/Transaksi/$transaksiId").fold(
                onSuccess = { response ->
                    val transaksi = parseTransaksiFromResponse(response)
                    Result.success(transaksi)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getTransaksiById error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cancelTransaksi(transaksiId: Int): Result<String> {
        return try {
            doPost("/Transaksi/$transaksiId/cancel", JSONObject()).fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "cancelTransaksi error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === HELPER: BUAT TRANSAKSI DARI KERANJANG ===

    suspend fun createTransaksiFromCart(
        customerName: String,
        mejaId: Int,
        cartItems: List<CartItem>,
        pembayaran: MetodePembayaran = MetodePembayaran.CASH
    ): Result<Transaksi> {
        val isDineIn = mejaId > 0

        // Buat transaksi
        val createResult = if (isDineIn) {
            createDineInTransaksi(customerName, mejaId)
        } else {
            createTakeAwayTransaksi(customerName)
        }

        val transaksi = createResult.getOrElse { return Result.failure(it) }

        // Tambah semua item dari keranjang
        for (item in cartItems) {
            val addResult = addItemToTransaksi(
                transaksiId = transaksi.idTransaksi,
                idMenu = item.menu.menuId,
                jumlah = item.quantity,
                catatan = item.catatan.ifEmpty { null }
            )
            addResult.getOrElse { error ->
                try { cancelTransaksi(transaksi.idTransaksi) } catch (_: Exception) {}
                return Result.failure(Exception("Gagal menambah '${item.menu.namaMenu}': ${error.message}"))
            }
        }

        // Ambil data transaksi terbaru
        val updatedTransaksi = getTransaksiById(transaksi.idTransaksi).getOrElse { transaksi }
        return Result.success(updatedTransaksi)
    }

    // === JSON PARSERS ===

    private fun <T> parseJsonArrayResponse(response: String, parser: (JSONObject) -> T): List<T> {
        val list = mutableListOf<T>()
        try {
            // Coba parse sebagai objek dengan field "data"
            val jsonResponse = JSONObject(response)
            val jsonArray = if (jsonResponse.has("data")) {
                jsonResponse.getJSONArray("data")
            } else {
                JSONArray(response)
            }
            for (i in 0 until jsonArray.length()) {
                list.add(parser(jsonArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // Fallback: coba parse sebagai array langsung
            try {
                val jsonArray = JSONArray(response)
                for (i in 0 until jsonArray.length()) {
                    list.add(parser(jsonArray.getJSONObject(i)))
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Gagal parse JSON: ${e2.message}")
            }
        }
        return list
    }

    // Parser untuk data meja
    private fun parseMeja(jsonObj: JSONObject): Meja {
        val statusValue = jsonObj.optInt("status", 0)
        val statusStr = when (statusValue) {
            0 -> "Tersedia"
            1 -> "Terisi"
            2 -> "Dipesan"
            else -> jsonObj.optString("status", "Tersedia")
        }
        return Meja(
            mejaId = jsonObj.optInt("id_meja", jsonObj.optInt("mejaId", 0)),
            nomorMeja = jsonObj.optString("no_meja", jsonObj.optString("nomorMeja", "")),
            kapasitas = jsonObj.optInt("kapasitas", 4),
            status = statusStr
        )
    }

    // Parser untuk data menu
    private fun parseMenu(jsonObj: JSONObject): Menu {
        val kategoriObj = jsonObj.optJSONObject("kategori")
        val kategoriNama = kategoriObj?.optString("nama_kategori") ?: "Lainnya"

        // Status menu: 0 = Tersedia, 1 = Habis
        val statusValue = jsonObj.optInt("status", -1)
        val ketersediaan = if (statusValue >= 0) {
            statusValue == 0
        } else {
            jsonObj.optString("status", "Tersedia").equals("Tersedia", ignoreCase = true)
        }

        return Menu(
            menuId = jsonObj.optInt("id_menu", jsonObj.optInt("menuId", 0)),
            namaMenu = jsonObj.optString("nama_menu", jsonObj.optString("namaMenu", "")),
            kategori = kategoriNama,
            harga = jsonObj.optDouble("harga", 0.0),
            ketersediaan = ketersediaan,
            stok = jsonObj.optInt("stok", 0),
            deskripsi = jsonObj.optString("deskripsi", ""),
            gambarUrl = jsonObj.optString("foto_menu", jsonObj.optString("gambarUrl", ""))
        )
    }

    // Parser untuk data transaksi dari respons (bisa berupa objek langsung atau memiliki field "data")
    private fun parseTransaksiFromResponse(response: String): Transaksi {
        val jsonObj = JSONObject(response)
        val dataObj = if (jsonObj.has("data")) jsonObj.getJSONObject("data") else jsonObj
        return parseTransaksi(dataObj)
    }

    // Parser untuk data transaksi
    private fun parseTransaksi(jsonObj: JSONObject): Transaksi {
        val detailArray = jsonObj.optJSONArray("detailTransaksi") ?: JSONArray()
        val detailList = mutableListOf<DetailTransaksi>()
        for (i in 0 until detailArray.length()) {
            detailList.add(parseDetailTransaksi(detailArray.getJSONObject(i)))
        }

        return Transaksi(
            idTransaksi = jsonObj.optInt("id_transaksi", jsonObj.optInt("idTransaksi", 0)),
            namaKonsumen = jsonObj.optString("nama_konsumen", jsonObj.optString("namaKonsumen", "")),
            tipe = TipeTransaksi.fromInt(jsonObj.optInt("tipe", 0)),
            totalBayar = jsonObj.optDouble("total_bayar", jsonObj.optDouble("totalBayar", 0.0)),
            tanggalTransaksi = jsonObj.optString("tanggal_transaksi", jsonObj.optString("tanggalTransaksi", "")),
            status = StatusTransaksi.fromInt(jsonObj.optInt("status", 0)),
            idUser = if (jsonObj.isNull("id_user")) null else jsonObj.optInt("id_user", 0).takeIf { it > 0 },
            idMeja = if (jsonObj.isNull("id_meja")) null else jsonObj.optInt("id_meja", 0).takeIf { it > 0 },
            pembayaran = MetodePembayaran.fromInt(jsonObj.optInt("pembayaran", 0)),
            catatan = if (jsonObj.isNull("catatan")) null else jsonObj.optString("catatan", ""),
            detailTransaksi = detailList
        )
    }

    // Parser untuk detail item transaksi
    private fun parseDetailTransaksi(jsonObj: JSONObject): DetailTransaksi {
        val menuObj = jsonObj.optJSONObject("menu")
        val namaMenu = menuObj?.optString("nama_menu") ?: jsonObj.optString("namaMenu", "")

        return DetailTransaksi(
            idDetailTransaksi = jsonObj.optInt("id_detailtransaksi", jsonObj.optInt("idDetailTransaksi", 0)),
            idTransaksi = jsonObj.optInt("id_transaksi", jsonObj.optInt("idTransaksi", 0)),
            idMenu = jsonObj.optInt("id_menu", jsonObj.optInt("idMenu", 0)),
            jumlah = jsonObj.optInt("jumlah", 0),
            subtotal = jsonObj.optDouble("subtotal", 0.0),
            metode = MetodePesanan.fromInt(jsonObj.optInt("metode", 0)),
            catatan = jsonObj.optString("catatan", ""),
            status = StatusDetailTransaksi.fromInt(jsonObj.optInt("status", 0)),
            namaMenu = namaMenu
        )
    }

    // === HELPER INTERNAL ===

    private fun extractMejaIdsFromTransaksiResponse(response: String, mejaIds: MutableSet<Int>) {
        try {
            val dataArray = try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("data")) jsonResponse.getJSONArray("data") else JSONArray(response)
            } catch (e: Exception) {
                JSONArray(response)
            }

            for (i in 0 until dataArray.length()) {
                val transaksi = dataArray.getJSONObject(i)
                extractMejaIdFromTransaksi(transaksi, mejaIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal parse meja IDs: ${e.message}")
        }
    }

    private fun extractActiveMejaIdsFromAllTransaksi(response: String, mejaIds: MutableSet<Int>) {
        try {
            val dataArray = try {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("data")) jsonResponse.getJSONArray("data") else JSONArray(response)
            } catch (e: Exception) {
                JSONArray(response)
            }

            for (i in 0 until dataArray.length()) {
                val transaksi = dataArray.getJSONObject(i)
                val status = transaksi.optString("status", "")
                val statusInt = transaksi.optInt("status", -1)

                // Hanya ambil transaksi yang aktif (Pending = 0, Diproses = 1)
                val isActive = statusInt == 0 || statusInt == 1
                        || status.equals("Pending", ignoreCase = true)
                        || status.equals("Diproses", ignoreCase = true)

                if (isActive) {
                    extractMejaIdFromTransaksi(transaksi, mejaIds)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal filter transaksi aktif: ${e.message}")
        }
    }

    private fun extractMejaIdFromTransaksi(transaksi: JSONObject, mejaIds: MutableSet<Int>) {
        val mejaObj = transaksi.optJSONObject("meja")
        if (mejaObj != null) {
            val mejaId = mejaObj.optInt("id_meja", 0)
            if (mejaId > 0) mejaIds.add(mejaId)
        } else {
            val mejaId = transaksi.optInt("id_meja", 0)
            if (mejaId > 0) mejaIds.add(mejaId)
        }
    }
}

