package com.utem.nougat.campuscycle

object SessionManager {
    // Flag untuk bagitau sistem kita tengah logout (supaya tak kick)
    var isLoggingOut = false

    // Flag BARU: Bagitau sistem kita tengah proses login (supaya tak kick awal sangat)
    var isLoggingIn = false
}