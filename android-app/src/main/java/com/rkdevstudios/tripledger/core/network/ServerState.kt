package com.rkdevstudios.tripledger.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ServerStatus {
    object Online : ServerStatus()
    data class WakingUp(val message: String = "Server is waking up. This may take a moment…") : ServerStatus()
    object Offline : ServerStatus()
}

object ServerState {
    private val _status = MutableStateFlow<ServerStatus>(ServerStatus.Online)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    fun setWakingUp(message: String = "Server is waking up. This may take a moment…") {
        _status.value = ServerStatus.WakingUp(message)
    }

    fun setOnline() {
        _status.value = ServerStatus.Online
    }

    fun setOffline() {
        _status.value = ServerStatus.Offline
    }
}
