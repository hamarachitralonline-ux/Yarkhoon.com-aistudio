package com.example.data

sealed class AuthResult {
    data class Success(val uid: String, val email: String, val displayName: String?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
