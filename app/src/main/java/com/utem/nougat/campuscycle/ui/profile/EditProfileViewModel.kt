package com.utem.nougat.campuscycle.ui.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EditProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // --- FUNGSI UPLOAD GAMBAR (UPDATED) ---
    // Terima 'folderName' supaya boleh asingkan folder Profile & QR
    fun uploadImage(uri: Uri, folderName: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    // 1. Simpan ikut folder yang diminta (profile_images / qr_codes)
                    val filename = "$folderName/${uid}_${UUID.randomUUID()}.jpg"
                    val ref = storage.reference.child(filename)

                    // 2. Upload file
                    ref.putFile(uri).await()

                    // 3. Dapatkan Download URL
                    val downloadUrl = ref.downloadUrl.await().toString()
                    Log.d("DEBUG_UPLOAD", "Success Upload to $folderName: $downloadUrl")

                    // 4. LOGIC KHAS: Kalau upload PROFILE IMAGE, kita auto-save URL ke Firestore
                    // supaya kalau user tukar gambar tapi tak tekan "Save Changes", gambar tetap bertukar.
                    if (folderName == "profile_images") {
                        val data = hashMapOf("photoUrl" to downloadUrl)
                        db.collection("users").document(uid)
                            .set(data, SetOptions.merge())
                            .await()
                    }

                    // Kalau QR Code, kita tak save ke DB terus. Kita tunggu user tekan butang "Save Changes".
                    // Kita cuma return URL kepada UI untuk preview.

                    onSuccess(downloadUrl)
                } catch (e: Exception) {
                    Log.e("DEBUG_ERROR", "Error: ${e.message}")
                    onError(e.message ?: "Gagal upload gambar")
                }
            }
        } else {
            onError("User tak jumpa (Sila login semula)")
        }
    }

    // --- FUNGSI UPDATE DATA TEKS (UPDATED) ---
    // Tambah parameter 'newQrUrl'
    fun updateProfile(
        newName: String,
        newPhone: String,
        newLink: String,
        newQrUrl: String, // <--- TAMBAH INI
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    val updates = hashMapOf(
                        "fullName" to newName,
                        "phoneNumber" to newPhone,
                        "businessLink" to newLink,
                        "qrCodeUrl" to newQrUrl // Simpan QR URL ke Firestore
                    )

                    db.collection("users").document(uid)
                        .set(updates, SetOptions.merge())
                        .await()

                    onSuccess()
                } catch (e: Exception) {
                    onError(e.message ?: "Gagal update profile")
                }
            }
        }
    }
}