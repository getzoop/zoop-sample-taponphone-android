package com.zoop.sdk.taponphone.sample

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.zoop.sdk.core.event.ApplicationEvent
import com.zoop.sdk.core.type.BeepVolumeConfig
import com.zoop.sdk.core.type.TimeoutConfig
import com.zoop.sdk.plugin.taponphone.ConfigParameters
import com.zoop.sdk.plugin.taponphone.ErrorResponse
import com.zoop.sdk.plugin.taponphone.TapOnPhone
import com.zoop.sdk.plugin.taponphone.api.Billing
import com.zoop.sdk.plugin.taponphone.api.Credentials
import com.zoop.sdk.plugin.taponphone.api.PaymentRequest
import com.zoop.sdk.plugin.taponphone.api.PaymentType
import com.zoop.sdk.plugin.taponphone.api.PixRequest
import com.zoop.sdk.plugin.taponphone.api.SdkConfig
import com.zoop.sdk.plugin.taponphone.api.TapOnPhoneTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {

    var timeoutConfig = TimeoutConfig(
        discoveryTimeout = 120_000,
        processingTimeout = 30_000,
        networkTimeout = 45_000,
        totalElapsedTimeout = 180_000,
    )

    private val beepVolumeConfig = BeepVolumeConfig(beepVolume = 1f)

    var credentials: Credentials? = null

    private val _uiState = MutableStateFlow(
        UiState(
            paymentStatus = PaymentStatus.Processing,
            initializationStatus = InitializationStatus.Idle,
        )
    )
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val paymentStatus: PaymentStatus,
        val allowLandscape: Boolean = false,
        val sendSms: Boolean = false,
        val errorCode: Int? = null,
        val errorMessage: String? = null,
        val transactionId: String? = null,
        val detailsConfig: String? = null,
        val initializationStatus: InitializationStatus,
        val qrCode: String? = null,
        val applicationEvent: ApplicationEvent? = null,
        val binNumber: String? = null,
        val paymentDevice: String? = null,
        val integrityErrorCode: Int? = null,
        val integrityErrorDescription: String? = null,
        val zoopDescription: String? = null,
        val sessionExpiration: Long? = null,
        val isNfcDisabledFailure: Boolean = false,
        val redactedLogInfo: String? = null,
    )

    fun configureTimeout(
        discoveryTimeout: Int,
        processingTimeout: Int,
        networkTimeout: Int,
        totalElapsedTimeout: Int,
    ) {
        try {
            timeoutConfig = TimeoutConfig(
                discoveryTimeout = discoveryTimeout,
                processingTimeout = processingTimeout,
                networkTimeout = networkTimeout,
                totalElapsedTimeout = totalElapsedTimeout,
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update {
                it.copy(
                    initializationStatus = InitializationStatus.Error,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun setConfig(context: Context, theme: TapOnPhoneTheme) {
        val creds = credentials ?: loadCredentials()
        val sdkConfig = SdkConfig(
            timeout = timeoutConfig,
            theme = theme,
            beepVolume = beepVolumeConfig,
            allowLandscape = _uiState.value.allowLandscape,
        )
        TapOnPhone.setConfig(
            ConfigParameters(
                context = context,
                credentials = creds,
                sdkConfig = sdkConfig,
            )
        )
    }

    fun pay(
        amount: Long,
        paymentType: PaymentType,
        installments: Int?,
        referenceId: String?,
        billing: Billing? = null,
    ) {
        val request = PaymentRequest(
            referenceId = referenceId ?: UUID.randomUUID().toString(),
            amount = amount,
            paymentType = paymentType,
            installments = installments,
            metadata = """{"fee":0.0455,"original_value":$amount,"installments":${installments ?: 1}}""",
            billing = billing,
        )
        TapOnPhone.pay(
            request = request,
            onSuccess = this::onPaymentSuccess,
            onError = this::onError,
            onEvent = this::onEvent,
        )
    }

    fun payByPix(amount: Long, referenceId: String?) {
        val request = PixRequest(
            amount = amount,
            referenceId = referenceId ?: UUID.randomUUID().toString(),
        )
        TapOnPhone.payByPix(
            request = request,
            onQRCode = this::onPixQrCode,
            onSuccess = this::onPixSuccess,
            onError = this::onError,
            onEvent = this::onEvent,
        )
    }

    private fun onPaymentSuccess(response: Any) {
        val transactionId = runCatching {
            response.javaClass.getMethod("getTransactionId").invoke(response) as? String
        }.getOrNull()
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Success,
                transactionId = transactionId,
                errorMessage = null,
                sendSms = true,
                qrCode = null,
            )
        }
    }

    private fun onPixQrCode(qrCode: String) {
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.QRCode,
                errorMessage = null,
                qrCode = qrCode,
            )
        }
    }

    private fun onPixSuccess(response: Any) {
        val transactionId = runCatching {
            response.javaClass.getMethod("getTransactionId").invoke(response) as? String
        }.getOrNull()
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Success,
                transactionId = transactionId,
                sendSms = true,
                errorMessage = null,
                qrCode = null,
            )
        }
    }

    private fun onEvent(event: ApplicationEvent) {
        _uiState.update { it.copy(applicationEvent = event) }
    }

    private fun onError(error: ErrorResponse) {
        when (error) {
            is ErrorResponse.Initialize, is ErrorResponse.Terminal -> {
                val e = (error as? ErrorResponse.Initialize)?.details
                    ?: (error as ErrorResponse.Terminal).details
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.Fail,
                        errorCode = e.code,
                        errorMessage = e.description ?: e.message.toString(),
                        zoopDescription = e.zoopDescription,
                    )
                }
            }

            is ErrorResponse.Session -> {
                val sessionError = error.details
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.Fail,
                        errorCode = sessionError.code,
                        errorMessage = sessionError.message,
                        integrityErrorCode = sessionError.integrityErrorCode?.code,
                        integrityErrorDescription = sessionError.integrityErrorCode?.description,
                    )
                }
            }

            is ErrorResponse.Payment -> {
                val e = error.details
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.Fail,
                        errorCode = e.code,
                        errorMessage = e.message,
                        transactionId = e.transactionId,
                        binNumber = e.binNumber,
                        paymentDevice = e.paymentDevice,
                        integrityErrorCode = e.integrityErrorCode?.code,
                        integrityErrorDescription = e.integrityErrorCode?.description,
                        zoopDescription = e.zoopDescription,
                    )
                }
            }

            is ErrorResponse.Pix, is ErrorResponse.SMS, is ErrorResponse.Default -> {
                _uiState.update {
                    it.copy(
                        paymentStatus = PaymentStatus.Fail,
                        errorMessage = when (error) {
                            is ErrorResponse.Pix -> error.details.message
                            is ErrorResponse.SMS -> error.details.message
                            is ErrorResponse.Default -> error.details.message
                            else -> null
                        },
                    )
                }
            }
        }
    }

    private fun loadCredentials() = Credentials(
        clientId = BuildConfig.CLIENT_ID.ifEmpty { "" },
        clientSecret = BuildConfig.CLIENT_SECRET.ifEmpty { "" },
        marketplace = BuildConfig.MARKETPLACE.ifEmpty { "" },
        seller = BuildConfig.SELLER.ifEmpty { "" },
        accessKey = BuildConfig.API_KEY.ifEmpty { "" },
    )
}
