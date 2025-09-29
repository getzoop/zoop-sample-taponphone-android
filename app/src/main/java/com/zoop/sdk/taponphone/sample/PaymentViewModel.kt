package com.zoop.sdk.taponphone.sample

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zoop.sdk.core.exception.KernelException
import com.zoop.sdk.core.exception.ZoopException
import com.zoop.sdk.core.type.BeepVolumeConfig
import com.zoop.sdk.core.type.TimeoutConfig
import com.zoop.sdk.plugin.taponphone.Parameters
import com.zoop.sdk.plugin.taponphone.api.ExternalSeller
import com.zoop.sdk.plugin.taponphone.api.InitializationRequest
import com.zoop.sdk.plugin.taponphone.api.InitializationStatus
import com.zoop.sdk.plugin.taponphone.api.PaymentApprovedResponse
import com.zoop.sdk.plugin.taponphone.api.PaymentErrorResponse
import com.zoop.sdk.plugin.taponphone.api.PaymentRequest
import com.zoop.sdk.plugin.taponphone.api.PaymentType
import com.zoop.sdk.plugin.taponphone.api.PixApprovedResponse
import com.zoop.sdk.plugin.taponphone.api.PixErrorResponse
import com.zoop.sdk.plugin.taponphone.api.PixRequest
import com.zoop.sdk.plugin.taponphone.api.TapOnPhone
import com.zoop.sdk.plugin.taponphone.api.TapOnPhoneTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.concurrent.thread

class PaymentViewModel(app: Application) : AndroidViewModel(app) {
    var timeoutConfig = TimeoutConfig(
        discoveryTimeout = 120_000,
        processingTimeout = 30_000,
        networkTimeout = 45_000,
        totalElapsedTimeout = 180_000
    )

    private val beepVolumeConfig = BeepVolumeConfig(
        beepVolume = 1f
    )

    lateinit var credentials: InitializationRequest.Credentials

    private val _uiState = MutableStateFlow(
        UiState(
            paymentStatus = PaymentStatus.Processing,
            initializationStatus = InitializationStatus.NOT_INITIALIZED
        )
    )
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val paymentStatus: PaymentStatus,
        val sendSms: Boolean = false,
        val errorMessage: String? = null,
        val transactionId: String? = null,
        val detailsConfig: String? = null,
        val initializationStatus: InitializationStatus,
        val qrCode: String? = null
    )

    private val tapOnPhone = TapOnPhone(
        context = app.applicationContext,
        parameters = Parameters.DEFAULT.copy(showErrorScreen = false),
    )

    val instanceId: String?
        get() = tapOnPhone.instanceId

    private fun getDetailsConfig() {
        tapOnPhone.getConfigDetails { configDetails ->
            _uiState.update {
                it.copy(
                    detailsConfig = configDetails.toString()
                )
            }
        }
    }

    fun configureTimeout(
        discoveryTimeout: Int,
        processingTimeout: Int,
        networkTimeout: Int,
        totalElapsedTimeout: Int
    ) {
        try {
            timeoutConfig = TimeoutConfig(
                discoveryTimeout = discoveryTimeout,
                processingTimeout = processingTimeout,
                networkTimeout = networkTimeout,
                totalElapsedTimeout = totalElapsedTimeout
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update {
                it.copy(
                    initializationStatus = InitializationStatus.FAILED,
                    errorMessage = e.message
                )
            }
        }

    }

    fun initialize(theme: TapOnPhoneTheme) {

        if (::credentials.isInitialized.not()) {
            credentials = InitializationRequest.Credentials(
                clientId = BuildConfig.CLIENT_ID.ifEmpty { "" },
                clientSecret = BuildConfig.CLIENT_SECRET.ifEmpty { "" },
                marketplace = BuildConfig.MARKETPLACE.ifEmpty { "" },
                seller = BuildConfig.SELLER.ifEmpty { "" },
                accessKey = BuildConfig.API_KEY.ifEmpty { "" }
            )
        }

        val initializationRequest = InitializationRequest(
            theme = theme,
            credentials = credentials,
            timeout = timeoutConfig,
            beepVolume = beepVolumeConfig
        )

        var kernelException: KernelException? = null
        var zoopException: ZoopException? = null

        thread {
            try {
                tapOnPhone.initialize(initializationRequest)
            } catch (e: KernelException) {
                kernelException = e
            } catch (e: ZoopException) {
                zoopException = e
            }finally {
                getDetailsConfig()
                if (kernelException != null) {
                    _uiState.update {
                        it.copy(
                            initializationStatus = InitializationStatus.FAILED,
                            errorMessage = kernelException?.message
                        )
                    }
                }

                if (zoopException != null) {
                    _uiState.update {
                        it.copy(
                            initializationStatus = InitializationStatus.FAILED,
                            errorMessage = zoopException?.message
                        )
                    }
                }

                if (kernelException == null && zoopException == null) {
                    _uiState.update {
                        it.copy(
                            initializationStatus = InitializationStatus.SUCCESS
                        )
                    }
                }
            }
        }
    }

    fun pay(
        amount: Long,
        paymentType: PaymentType,
        installments: Int?,
        referenceId: String?
    ) {
        viewModelScope.launch {
            tapOnPhone.pay(
                payRequest = PaymentRequest(
                    referenceId = referenceId ?: UUID.randomUUID().toString(),
                    amount = amount,
                    paymentType = paymentType,
                    installments = installments,
                    metadata = ""// """{"demo":"${UUID.randomUUID()}"}""".trimIndent()
                ),
                onApproved = { response ->
                    getDetailsConfig()
                    onPaymentSuccess(response)
                },
                onError = { error ->
                    getDetailsConfig()
                    onPaymentError(error)
                }
            )

        }
    }

    fun payByPix(amount: Long, referenceId: String?) {
        viewModelScope.launch {
            tapOnPhone.payByPix(
                PixRequest(
                    amount = amount,
                    referenceId = referenceId ?: UUID.randomUUID().toString()
                ),
                onApproved = { response ->
                    getDetailsConfig()
                    onPixSuccess(response)
                },
                onQrCode = { qrCode ->
                    onPixQrCode(qrCode)
                },
                onError = { error ->
                    getDetailsConfig()
                    onPixError(error)
                }
            )
        }
    }

    fun cancelPix() {
        tapOnPhone.cancelPix()
    }

    fun clearSms() {
        _uiState.update {
            it.copy(
                sendSms = false,
                paymentStatus = PaymentStatus.Success,
            )
        }
    }

    fun sendSms(phoneNumber: String) {
        fun onError(error: ZoopException) {
            _uiState.update {
                it.copy(
                    sendSms = false,
                    errorMessage = error.message,
                    paymentStatus = PaymentStatus.Success
                )
            }
        }

        viewModelScope.launch {
            tapOnPhone.sendSms(
                phoneNumber = phoneNumber,
                transactionId = _uiState.value.transactionId!!,
                onError = { onError(it) },
                onSuccess = { clearSms() }
            )
        }
    }

    private fun onPixQrCode(qrCode: String) {
        Log.d("PaymentViewModel", "onPixQrCode: $qrCode")
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Success,
                errorMessage = null,
                qrCode = qrCode
            )
        }
    }

    private fun onPixSuccess(response: PixApprovedResponse) {
        Log.d("PaymentViewModel", "onPixSuccess: ${response.transactionId}")
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Success,
                transactionId = response.transactionId,
                sendSms = true,
                errorMessage = null,
                qrCode = null
            )
        }
    }

    private fun onPaymentSuccess(response: PaymentApprovedResponse) {
        Log.d("PaymentViewModel", "onPaymentSuccess: ${response.transactionId}")
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Success,
                transactionId = response.transactionId,
                errorMessage = null,
                sendSms = true,
                qrCode = null
            )
        }
    }

    private fun onPaymentError(error: PaymentErrorResponse) {
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Fail,
                errorMessage = "Erro: ${error.message}\nCódigo: ${error.code}\nDescrição: ${error.description}\nSource: ${error.errorSource}\nkernel:${error.kernel.toString()}",
                transactionId = error.transactionId,
                qrCode = null,
                sendSms = false
            )
        }
    }

    private fun onPixError(error: PixErrorResponse) {
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Fail,
                errorMessage = "Erro: ${error.message}\n",
                transactionId = error.transactionId,
                qrCode = null,
                sendSms = false
            )
        }
    }
}