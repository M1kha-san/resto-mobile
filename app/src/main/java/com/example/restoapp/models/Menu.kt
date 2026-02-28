package com.example.restoapp.models

/**
 * Model data untuk fitur pelanggan (customer).
 *
 * Berisi semua model yang berkaitan dengan menu, meja, transaksi,
 * dan keranjang belanja untuk alur pemesanan customer.
 */

// ===== MODEL MENU =====

// Data menu dari API
data class Menu(
    val menuId: Int,
    val namaMenu: String,
    val kategori: String,
    val harga: Double,
    val ketersediaan: Boolean,
    val stok: Int,
    val deskripsi: String? = null,
    val gambarUrl: String? = null
)

// Data kategori menu
data class KategoriMenu(
    val kategoriId: Int,
    val namaKategori: String
)

// ===== MODEL MEJA =====

// Data meja restoran dari API
data class Meja(
    val mejaId: Int,
    val nomorMeja: String,
    val kapasitas: Int,
    val status: String          // "Tersedia", "Terisi", "Dipesan"
)

// ===== ENUM STATUS & METODE =====

// Metode pembayaran yang tersedia
enum class MetodePembayaran(val value: Int) {
    CASH(0),
    TRANSFER(1),
    QRIS(2);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: CASH
    }
}

// Metode pesanan (makan di tempat / bawa pulang)
enum class MetodePesanan(val value: Int, val displayName: String) {
    DINE_IN(0, "Dine In"),
    TAKE_AWAY(1, "Take Away");

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: DINE_IN
    }
}

// Status transaksi keseluruhan
enum class StatusTransaksi(val value: Int, val displayName: String) {
    MENUNGGU(0, "Menunggu"),
    DIPROSES(1, "Diproses"),
    SELESAI(2, "Selesai"),
    DIBATALKAN(3, "Dibatalkan");

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: MENUNGGU
    }
}

// Status per item dalam transaksi
enum class StatusDetailTransaksi(val value: Int, val displayName: String) {
    MENUNGGU(0, "Menunggu"),
    DIMASAK(1, "Dimasak"),
    SIAP(2, "Siap"),
    DISAJIKAN(3, "Disajikan");

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: MENUNGGU
    }
}

// Tipe transaksi (dine in / take away)
enum class TipeTransaksi(val value: Int, val displayName: String) {
    DINE_IN(0, "Dine In"),
    TAKE_AWAY(1, "Take Away");

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: DINE_IN
    }
}

// ===== MODEL REQUEST (untuk kirim data ke API) =====

// Request buat transaksi dine in → POST /api/Transaksi/dine-in
data class CreateDineInRequest(
    val namaKonsumen: String,
    val idMeja: Int
)

// Request buat transaksi take away → POST /api/Transaksi/take-away
data class CreateTakeAwayRequest(
    val namaKonsumen: String
)

// Request tambah item ke transaksi → POST /api/Transaksi/{id}/items
data class AddItemRequest(
    val idMenu: Int,
    val jumlah: Int,
    val catatan: String? = null
)

// Request update item di transaksi → PATCH /api/Transaksi/{transaksiId}/items/{itemId}
data class UpdateItemRequest(
    val jumlah: Int,
    val catatan: String? = null
)

// Request konfirmasi transaksi → POST /api/Transaksi/{id}/confirm
data class ConfirmTransaksiRequest(
    val idKasir: Int? = null,
    val pembayaran: Int = MetodePembayaran.CASH.value
)

// Request update status transaksi → PATCH /api/Transaksi/{id}/status
data class UpdateStatusRequest(
    val status: String
)

// ===== MODEL RESPONSE (data dari API) =====

// Data transaksi dari API
data class Transaksi(
    val idTransaksi: Int,
    val namaKonsumen: String,
    val tipe: TipeTransaksi = TipeTransaksi.DINE_IN,
    val totalBayar: Double,
    val tanggalTransaksi: String,
    val status: StatusTransaksi,
    val idUser: Int? = null,        // ID kasir yang mengurus (diisi saat konfirmasi)
    val idMeja: Int? = null,        // Null jika take away
    val pembayaran: MetodePembayaran,
    val catatan: String? = null,
    val detailTransaksi: List<DetailTransaksi> = emptyList()
)

// Data detail per item dalam transaksi
data class DetailTransaksi(
    val idDetailTransaksi: Int,
    val idTransaksi: Int,
    val idMenu: Int,
    val jumlah: Int,
    val subtotal: Double,
    val metode: MetodePesanan,
    val catatan: String? = null,
    val status: StatusDetailTransaksi,
    val namaMenu: String? = null
)

// ===== MODEL KERANJANG (lokal, tidak dikirim ke API) =====

// Item dalam keranjang belanja customer
data class CartItem(
    val menu: Menu,
    val quantity: Int,
    val catatan: String = "",
    val metodePesanan: MetodePesanan = MetodePesanan.DINE_IN
) {
    // Hitung subtotal otomatis dari harga x jumlah
    val subtotal: Double get() = menu.harga * quantity
}

