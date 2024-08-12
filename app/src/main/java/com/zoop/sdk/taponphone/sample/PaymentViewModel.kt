package com.zoop.sdk.taponphone.sample

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.zoop.sdk.InitializeData
import com.zoop.sdk.Zoop
import com.zoop.sdk.event.ApplicationEvent
import com.zoop.sdk.plugin.taponphone.TapOnPhonePaymentResponse
import com.zoop.sdk.plugin.taponphone.TapOnPhonePlugin
import com.zoop.sdk.plugin.taponphone.TapOnPhoneTheme
import com.zoop.sdk.plugin.taponphone.driver.mypinpad.MyPinPadCredentials
import com.zoop.sdk.type.Callback
import com.zoop.sdk.type.Currency
import com.zoop.sdk.type.Environment
import com.zoop.sdk.type.Option
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PaymentViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(
        UiState(
            paymentStatus = PaymentStatus.Processing
        )
    )
    val uiState = _uiState.asStateFlow()

    private var defaultCredential: Boolean? = false

    data class UiState(
        val paymentStatus: PaymentStatus,
        val errorMessage: String? = null,
        val transactionId: String? = null
    )

    fun initialize(
        context: Context,
        useExternalScreen: Boolean = false,
        arrangement: TapOnPhoneTheme.Arrangement,
        cancelButton: View? = null,
        infoAreaTextView: TextView? = null
    ) {
        Log.d("TapOnPhonePluginHelper", "initialize using zoop variant!")
        val marketplace = BuildConfig.MARKETPLACE.ifEmpty { "" }
        val seller = BuildConfig.SELLER.ifEmpty { "" }
        val terminal = UUID.randomUUID().toString()
        val accessKey = BuildConfig.API_KEY.ifEmpty { "" }

        val initializer: InitializeData.() -> Unit = {
            credentials {
                this.marketplace = marketplace
                this.seller = seller
                this.terminal = terminal
                this.accessKey = accessKey
            }
        }

        Zoop.initialize(context, initializer)
        Zoop.setEnvironment(Environment.Staging)
        Zoop.setStrict(false)

        val clientId = BuildConfig.CLIENT_ID.ifEmpty { "" }
        val clientSecret = BuildConfig.CLIENT_SECRET.ifEmpty { "" }

        Zoop.findPlugin<TapOnPhonePlugin>()?.let {
            Zoop.unplug(it)
        }

        Zoop.make<TapOnPhonePlugin>()
            .apply {
                configureTheme {
                    window.fullscreen = true
                    playbook.cancel =
                        ContextCompat.getDrawable(context, R.drawable.ic_nubank_button_close)
                    playbook.logo = ContextCompat.getDrawable(context, R.drawable.ic_nubank_logo)
                    playbook.background = ContextCompat.getDrawable(context, R.color.nubank_main)
                    playbook.text.color = ContextCompat.getColor(context, R.color.black)
                    playbook.amountText = TapOnPhoneTheme.Playbook.Text(
                        color = ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    playbook.footer =
                        ContextCompat.getDrawable(context, R.drawable.ic_taponphone_footer)

                    if (arrangement != TapOnPhoneTheme.Arrangement.DEFAULT) {
                        playbook.infoAreaBackground = ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_nubank_card_scheme_borders_blank
                        )
                    }
                    // New properties
                    playbook.presentationAreaBackground = ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_nubank_presentation_background
                    )
                    playbook.arrangement = arrangement
                    playbook.spaceBetween = 120
                    playbook.presentationSpaceBetween = 160
                    playbook.layoutPadding = TapOnPhoneTheme.Padding(
                        top = 48,
                        left = 24,
                        bottom = 48,
                        right = 24
                    )
                    playbook.presentationAreaPadding = TapOnPhoneTheme.Padding(
                        top = 160,
                        left = 24,
                        bottom = 150,
                        right = 24
                    )
                    playbook.infoAreaPadding = TapOnPhoneTheme.Padding(
                        top = 8,
                        left = 8,
                        bottom = 8,
                        right = 8,
                    )
                }
                useMyPinPadDriver(
                    credentials = MyPinPadCredentials(
                        clientId = clientId,
                        clientSecret = clientSecret
                    )
                ) {
                    if (useExternalScreen) {
                        useExternalScreen()
                    }
                }
            }.run(Zoop::plug)
    }

    fun pay(
        amount: Long,
        paymentType: Option,
        installments: Int = 1,
        onApplicationEvent: (ApplicationEvent) -> Unit,
        onPaymentSuccess: (TapOnPhonePaymentResponse) -> Unit,
        onPaymentError: (Throwable) -> Unit
    ) {
        Log.d("TapOnPhonePluginHelper", "pay using nubank variant!")

        val metadata = """
            {
                "client_id": "123456789",
                "client_name": "John Doe"
            }
        """.trimIndent()

        TapOnPhonePlugin.createPaymentRequestBuilder()
            .currency(Currency.BRL)
            .amount(amount)
            .option(paymentType)
            .installments(installments)
            .clientMetadata(metadata)
            .applicationCallback(object : Callback<ApplicationEvent>() {
                override fun onSuccess(response: ApplicationEvent) {
                    onApplicationEvent(response)
                }

                override fun onFail(error: Throwable) {
                    onPaymentError(error)
                }
            })
            .callback(object : Callback<TapOnPhonePaymentResponse>() {
                override fun onSuccess(response: TapOnPhonePaymentResponse) {
                    onPaymentSuccess(response)
                }

                override fun onFail(error: Throwable) {
                    onPaymentError(error)
                }
            })
            .build()
            .apply(Zoop::post)
    }

    /*
    private val tapOnPhone = TapOnPhone(app.applicationContext)
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
            credentials = credentials
        )

        tapOnPhone.initialize(
            request = initializationRequest,
            onSuccess = onSuccess,
            onError = onError,
        )
    }
    */

    /*

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

    fun setCredential(credentials: InitializationRequest.Credentials) {
        tapOnPhone.setCredential(credentials)
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

     */
}