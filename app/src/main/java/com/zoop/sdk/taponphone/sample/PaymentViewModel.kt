package com.zoop.sdk.taponphone.sample

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zoop.sdk.core.exception.KernelException
import com.zoop.sdk.core.exception.ZoopException
import com.zoop.sdk.core.type.BeepVolumeConfig
import com.zoop.sdk.plugin.taponphone.api.InitializationRequest
import com.zoop.sdk.plugin.taponphone.api.InitializationStatus
import com.zoop.sdk.plugin.taponphone.api.PaymentApprovedResponse
import com.zoop.sdk.plugin.taponphone.api.PaymentErrorResponse
import com.zoop.sdk.plugin.taponphone.api.PaymentRequest
import com.zoop.sdk.plugin.taponphone.api.PaymentType
import com.zoop.sdk.plugin.taponphone.api.TapOnPhone
import com.zoop.sdk.plugin.taponphone.api.TapOnPhoneError
import com.zoop.sdk.plugin.taponphone.api.TapOnPhoneTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.concurrent.thread

class PaymentViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(
        UiState(
            paymentStatus = PaymentStatus.Processing
        )
    )
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val paymentStatus: PaymentStatus,
        val errorMessage: String? = null,
        val transactionId: String? = null
    )

    private val tapOnPhone = TapOnPhone(app.applicationContext)

    private val beepVolumeConfig = BeepVolumeConfig(
        beepVolume = 1f
    )

    lateinit var credentials: InitializationRequest.Credentials

    fun initialize(
        theme: TapOnPhoneTheme,
        onError: (TapOnPhoneError) -> Unit,
        onSuccess: () -> Unit
    ) {
        val credentials = InitializationRequest.Credentials(
            clientId = BuildConfig.CLIENT_ID.ifEmpty { "" },
            clientSecret = BuildConfig.CLIENT_SECRET.ifEmpty { "" },
            marketplace = BuildConfig.MARKETPLACE.ifEmpty { "" },
            seller = BuildConfig.SELLER.ifEmpty { "" },
            accessKey = BuildConfig.API_KEY.ifEmpty { "" },
        )

        val initializationRequest = InitializationRequest(
            theme = theme,
            credentials = credentials,
            beepVolume = beepVolumeConfig
        )

        lateinit var status: InitializationStatus

        thread {
            try {
                status = tapOnPhone.initialize(initializationRequest)
            } catch (e: KernelException) {
                // KernelException()
                // val kernelError: KernelError = e.kernelError
            } catch (e: ZoopException) {
                // Tratar exceção
            } finally {
                Log.d("isInitialized", status.name)
            }
        }
    }

    fun pay(
        amount: Long,
        paymentType: PaymentType,
        installments: Int?
    ) {
        viewModelScope.launch {
            tapOnPhone.pay(
                payRequest = PaymentRequest(
                    referenceId = UUID.randomUUID().toString(),
                    amount = amount,
                    paymentType = paymentType,
                    installments = installments
                ),
                onApproved = { response ->
                    onPaymentSuccess(response)
                },
                onError = { error ->
                    onPaymentError(error)
                }
            )
        }
    }

    private fun onPaymentSuccess(response: PaymentApprovedResponse) {
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Success,
                transactionId = response.transactionId,
                errorMessage = null
            )
        }
    }

    private fun onPaymentError(error: PaymentErrorResponse) {
        _uiState.update {
            it.copy(
                paymentStatus = PaymentStatus.Fail,
                errorMessage = "Erro: ${error.message}\nCódigo: ${error.code}\nDescrição: ${error.description}",
                transactionId = error.transactionId
            )
        }
    }
}