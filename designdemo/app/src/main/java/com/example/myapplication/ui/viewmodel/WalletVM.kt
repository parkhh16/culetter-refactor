// app/src/main/java/com/example/myapplication/ui/viewmodel/WalletVM.kt
package com.example.myapplication.ui.viewmodel

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.UserRepository
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WalletVM(
    app: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(app) {

    companion object { private const val TAG = "WalletVM" }

    private val _address = MutableStateFlow<String?>(null)
    val address = _address.asStateFlow()

    private val _connecting = MutableStateFlow(false)
    val connecting = _connecting.asStateFlow()

    private val _patching = MutableStateFlow(false)
    val patching = _patching.asStateFlow()

    private val _patchOk = MutableStateFlow<Boolean?>(null)
    val patchOk = _patchOk.asStateFlow()

    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true

        val meta = Core.Model.AppMetaData(
            name = "MetaMaskDemo",
            description = "WalletConnect (Reown) Integration",
            url = "https://example.com",
            icons = listOf("https://example.com/icon.png"),
            redirect = "myapplication://wc" // Manifest와 동일해야 함
        )

        CoreClient.initialize(
            projectId = "ddd0e7a082736c39ff145c9b4664d8c7", // 필요시 BuildConfig 로 이동
            connectionType = ConnectionType.AUTOMATIC,
            application = getApplication(),
            metaData = meta,
            relay = null,
            keyServerUrl = null,
            networkClientTimeout = null,
            telemetryEnabled = true
        ) { err -> Log.e(TAG, "Core init failed", err.throwable) }

        SignClient.initialize(
            init = Sign.Params.Init(core = CoreClient),
            onSuccess = {},
            onError = { err -> Log.e(TAG, "Sign init failed", err.throwable) }
        )

        SignClient.setDappDelegate(object : SignClient.DappDelegate {
            override fun onSessionApproved(approved: Sign.Model.ApprovedSession) {
                val accounts = approved.namespaces["eip155"]?.accounts.orEmpty()
                val addr = accounts.firstOrNull()?.split(":")?.lastOrNull()
                viewModelScope.launch {
                    _connecting.value = false
                    _address.value = addr
                    _patchOk.value = null // 새 주소 수신 시 결과 초기화
                }
            }

            override fun onSessionRejected(rejected: Sign.Model.RejectedSession) {
                viewModelScope.launch { _connecting.value = false }
            }
            override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) {
                viewModelScope.launch { _connecting.value = false }
            }
            override fun onError(error: Sign.Model.Error) {
                Log.e(TAG, "Sign error", error.throwable)
                viewModelScope.launch { _connecting.value = false }
            }

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {}
            override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {}
            override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {}
            override fun onSessionExtend(session: Sign.Model.Session) {}
            override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {}
            override fun onRequestExpired(expiredRequest: Sign.Model.ExpiredRequest) {}
            override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {}
        })
    }

    fun connect(context: android.content.Context) {
        viewModelScope.launch {
            _connecting.value = true

            val chains  = listOf("eip155:11155111")
            val methods = listOf("eth_sendTransaction", "personal_sign", "eth_signTypedData")
            val events  = listOf("chainChanged", "accountsChanged")
            val namespaces = mapOf("eip155" to Sign.Model.Namespace.Proposal(chains, methods, events))

            val pairing = CoreClient.Pairing.create()
            val wcUri = pairing?.uri
            if (pairing == null || wcUri.isNullOrEmpty()) {
                _connecting.value = false
                Log.e(TAG, "Pairing create/uri failed")
                return@launch
            }

            SignClient.connect(
                connect = Sign.Params.Connect(namespaces = namespaces, pairing = pairing),
                onSuccess = {
                    val deepLink = "metamask://wc?uri=" +
                            URLEncoder.encode(wcUri, StandardCharsets.UTF_8.name())
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { context.startActivity(intent) }
                    catch (_: ActivityNotFoundException) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=io.metamask")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                onError = { _ -> _connecting.value = false }
            )
        }
    }

    /** 주소가 준비됐을 때 서버에 PATCH */
    fun patchMe() {
        val addr = address.value ?: return
        viewModelScope.launch {
            _patching.value = true
            _patchOk.value = userRepository.updateWalletAddress(addr)
            _patching.value = false
        }
    }
}

/** Hilt 없이 Repository를 주입하기 위한 커스텀 Factory */
class WalletVMFactory(
    private val app: Application,
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WalletVM::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return WalletVM(app, repository) as T
    }
}
