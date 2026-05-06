package com.utem.nougat.campuscycle.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- LOGOUT SIMPLE & SELAMAT ---
    fun logoutUser(onSuccess: () -> Unit) {
        // Kita tak kacau database. Cuma keluar dari Auth.
        // ID lama biar je dalam DB.
        try {
            auth.signOut()
            onSuccess()
        } catch (e: Exception) {
            onSuccess()
        }
    }

    // --- LOGIN USER ---
    fun loginUser(
        inputId: String,
        pass: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // 🔥 TAMBAH .uppercase() DI SINI 🔥
        // .trim() buang space kosong depan belakang
        // .uppercase() tukar semua jadi huruf besar (contoh: d03 -> D03)
        val trimmedInput = inputId.trim().uppercase()

        if (trimmedInput.contains("@")) {
            // Kalau email, tak payah uppercase (sebab email memang case sensitive kadang2, tapi biasanya Firebase handle)
            // Tapi untuk selamat, email biasanya lowercase. Tapi fokus kita skrg ID.
            performLogin(inputId.trim(), pass, onSuccess, onError)
        } else {
            // Cari ID (yang dah di-convert jadi HURUF BESAR)
            db.collection("users").whereEqualTo("studentId", trimmedInput).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val realEmail = documents.documents[0].getString("email") ?: ""
                        if (realEmail.isNotEmpty()) {
                            performLogin(realEmail, pass, onSuccess, onError)
                        } else {
                            onError("Akaun wujud tapi email hilang.")
                        }
                    } else {
                        onError("ID Pelajar tidak dijumpai.")
                    }
                }
                .addOnFailureListener { onError("Masalah sambungan.") }
        }
    }

    private fun performLogin(email: String, pass: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc -> onSuccess(doc.getString("role") ?: "student") }
                        .addOnFailureListener { onError("Gagal dapatkan info user.") }
                }
            }
            .addOnFailureListener { onError(it.message ?: "Login Gagal.") }
    }

    // Function registerStudent letak di sini (kekal sama)...
    fun registerStudent(fullName: String, studentId: String, email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("users").whereEqualTo("studentId", studentId).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) onError("ID sudah wujud")
                else {
                    auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { res ->
                        val uid = res.user?.uid
                        if (uid != null) {
                            val data = hashMapOf("fullName" to fullName, "studentId" to studentId, "email" to email, "role" to "student", "phoneNumber" to "", "photoUrl" to "", "businessLink" to "")
                            db.collection("users").document(uid).set(data).addOnSuccessListener { onSuccess() }
                        }
                    }.addOnFailureListener { onError(it.message ?: "Error") }
                }
            }
    }
}