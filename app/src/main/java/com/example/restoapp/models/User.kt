package com.example.restoapp.models

/**
 * Model data untuk fitur autentikasi pengguna (login).
 *
 * Catatan: Modul auth ini hanya dipakai oleh pegawai (kasir, manajer, owner)
 * yang login melalui desktop. Customer tidak perlu login.
 */

// Permintaan login ke API
data class LoginRequest(
    val username: String,
    val password: String
)

// Respons login dari API
data class LoginResponse(
    val token: String,
    val user: User
)

// Data pengguna yang berhasil login
data class User(
    val id: Int,
    val username: String,
    val nama: String,
    val email: String,
    val role: String,           // "Owner", "Manager", "Kasir"
    val alamat: String? = null,
    val noTelp: String? = null
)

