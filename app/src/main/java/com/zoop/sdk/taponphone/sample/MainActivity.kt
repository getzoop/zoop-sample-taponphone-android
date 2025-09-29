package com.zoop.sdk.taponphone.sample

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zoop.sdk.core.i18n.MessageEvent
import com.zoop.sdk.plugin.taponphone.api.BackButtonConfiguration
import com.zoop.sdk.plugin.taponphone.api.CardAnimationArrangement
import com.zoop.sdk.plugin.taponphone.api.ErrorCodeTextStyle
import com.zoop.sdk.plugin.taponphone.api.ErrorMessageTextStyle
import com.zoop.sdk.plugin.taponphone.api.ErrorScreenConfiguration
import com.zoop.sdk.plugin.taponphone.api.InitializationRequest
import com.zoop.sdk.plugin.taponphone.api.InitializationStatus
import com.zoop.sdk.plugin.taponphone.api.MessagesEventStatus
import com.zoop.sdk.plugin.taponphone.api.PaymentType
import com.zoop.sdk.plugin.taponphone.api.PinPadType
import com.zoop.sdk.plugin.taponphone.api.TapOnPhoneTheme
import com.zoop.sdk.taponphone.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    private val paymentViewModel: PaymentViewModel by viewModels()
    private lateinit var alertDialogBuilder:  AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alertDialogBuilder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.config_details_title))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }

        alertDialogBuilder.create()

        binding.apply {
            marketplaceTextInput.setText(BuildConfig.MARKETPLACE.ifEmpty { "" })
            sellerTextInput.setText(BuildConfig.SELLER.ifEmpty { "" })
            accessKeyTextInput.setText(BuildConfig.API_KEY.ifEmpty { "" })

            buttonPay.setOnClickListener(::onButtonPayClicked)

            buttonCancel.setOnClickListener {
                containerCredential.visibility = View.GONE
            }

            buttonTimeout.setOnClickListener {
                binding.textViewPaymentResult.text = ""
                binding.textViewPaymentResult.visibility = View.GONE
                binding.discoveryTimeoutTextInput.setText(paymentViewModel.timeoutConfig.discoveryTimeout.toString())
                binding.processingTimeoutTextInput.setText(paymentViewModel.timeoutConfig.processingTimeout.toString())
                binding.networkTimeoutTextInput.setText(paymentViewModel.timeoutConfig.networkTimeout.toString())
                binding.totalElapsedTimeoutTextInput.setText(paymentViewModel.timeoutConfig.totalElapsedTimeout.toString())
                binding.containerTimeout.visibility = View.VISIBLE
                binding.buttonTimeout.visibility = View.GONE
            }

            buttonInitialize.setOnClickListener {
                initialize()
            }

            buttonTimeoutCancel.setOnClickListener {
                binding.containerTimeout.visibility = View.GONE
                binding.buttonTimeout.visibility = View.VISIBLE
            }

            buttonTimeoutConfirm.setOnClickListener {
                val discoveryTimeout = discoveryTimeoutTextInput.text.toString().toIntOrNull() ?: 120_000
                val processingTimeout = processingTimeoutTextInput.text.toString().toIntOrNull() ?: 30_000
                val networkTimeout = networkTimeoutTextInput.text.toString().toIntOrNull() ?: 45_000
                val totalElapsedTimeout = totalElapsedTimeoutTextInput.text.toString().toIntOrNull() ?: 180_000

                paymentViewModel.configureTimeout(
                    discoveryTimeout = discoveryTimeout,
                    processingTimeout = processingTimeout,
                    networkTimeout = networkTimeout,
                    totalElapsedTimeout = totalElapsedTimeout
                )

                binding.containerTimeout.visibility = View.GONE
                binding.buttonTimeout.visibility = View.VISIBLE
            }

            buttonConfirm.setOnClickListener {
                val marketplace = marketplaceTextInput.text.toString()
                val seller = sellerTextInput.text.toString()
                val accessKey = accessKeyTextInput.text.toString()

                setCredentials(marketplace = marketplace, seller = seller, accessKey = accessKey)
                binding.containerCredential.visibility = View.GONE
            }


            buttonShowConfigDetails.setOnClickListener {
                alertDialogBuilder.show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.uiState.collect {
                    updateDetailsConfig(it.detailsConfig)
                    displayPaymentResult(it)
                    displayInitializationInfo(it)
                    handleInitializationProgress(it.initializationStatus)
                }
            }
        }
    }

    private fun updateDetailsConfig(configDetails: String?) {
        alertDialogBuilder.setMessage(configDetails)
    }

    private fun handleInitializationProgress(initializationStatus: InitializationStatus) {
        when (initializationStatus) {
            InitializationStatus.FAILED,
            InitializationStatus.PROCESSING -> {
                binding.buttonInitialize.isEnabled = true
                binding.buttonTimeout.isEnabled = true
                binding.buttonShowConfigDetails.isEnabled = true
                binding.textViewInitializationInfo.text = ""
            }
            InitializationStatus.PROCESSING -> {
                binding.buttonPay.isEnabled = false
                binding.buttonInitialize.isEnabled = false
                binding.buttonShowConfigDetails.isEnabled = false
                binding.buttonTimeout.isEnabled = false
                binding.textViewInitializationInfo.text = "Inicializando..."
            }
            InitializationStatus.SUCCESS -> {
                binding.buttonPay.isEnabled = true
                binding.buttonInitialize.isEnabled = true
                binding.buttonShowConfigDetails.isEnabled = true
                binding.buttonTimeout.isEnabled = false
                binding.textViewInitializationInfo.text = ""
            }

            InitializationStatus.NOT_INITIALIZED -> {
                binding.buttonPay.isEnabled = false
                binding.buttonInitialize.isEnabled = true
                binding.buttonShowConfigDetails.isEnabled = true
                binding.buttonTimeout.isEnabled = true
                binding.textViewInitializationInfo.text = ""
            }
        }
    }

    private fun setCredentials(marketplace: String, seller: String, accessKey: String) {
        if (marketplace.isEmpty() || seller.isEmpty() || accessKey.isEmpty()) {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.message_fill_all_input_data),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        paymentViewModel.credentials = InitializationRequest.Credentials(
            clientId = BuildConfig.CLIENT_ID.ifEmpty { "" },
            clientSecret = BuildConfig.CLIENT_SECRET.ifEmpty { "" },
            marketplace,
            seller,
            accessKey
        )
    }

    private fun displayPaymentResult(uiState: PaymentViewModel.UiState) {
        var status = when (uiState.paymentStatus) {
            PaymentStatus.Processing -> return
            PaymentStatus.Success -> {
                binding.fragmentContainer.visibility = View.VISIBLE
                binding.textViewPaymentResult.visibility = View.VISIBLE
                binding.textViewPaymentResult.setTextColor(Color.GREEN)
                "PAGAMENTO APROVADO!"
            }

            PaymentStatus.Fail -> {
                binding.fragmentContainer.visibility = View.GONE
                binding.textViewPaymentResult.visibility = View.VISIBLE
                binding.textViewPaymentResult.setTextColor(Color.RED)
                "PAGAMENTO NEGADO!"
            }
        }

        status = when (uiState.initializationStatus) {
            InitializationStatus.FAILED -> {
                binding.textViewPaymentResult.visibility = View.VISIBLE
                binding.textViewPaymentResult.setTextColor(Color.RED)
                "ERRO AO INICIALIZAR!"
            }

            else -> status
        }

        if (status.isEmpty()) return
        binding.textViewPaymentResult.text = status
        binding.textViewPaymentInfo.text =
            "${uiState.errorMessage ?: ""}\n\nID:${uiState.transactionId ?: ""}"
    }

    private fun displayInitializationInfo(uiState: PaymentViewModel.UiState) {
        if (uiState.initializationStatus == InitializationStatus.FAILED) {
            binding.textViewPaymentResult.visibility = View.VISIBLE
            binding.textViewPaymentResult.text = uiState.errorMessage ?: "Erro ao inicializar"
        }
    }

    private fun initialize() {
        paymentViewModel.initialize(
            theme = getTapOnPhoneTheme()
        )
    }

    private fun onButtonPayClicked(view: View) {
        val amount = binding.editTextAmount.text.toString().toLongOrNull() ?: 0L
        val paymentType = when (binding.radioGroupPaymentType.checkedRadioButtonId) {
            R.id.radioButtonCredit -> PaymentType.CREDIT
            R.id.radioButtonDebit -> PaymentType.DEBIT
            R.id.radioButtonPix -> PaymentType.PIX
            else -> PaymentType.CREDIT
        }

        val installments = binding.editTextInstallments.text.toString().toIntOrNull()

        val referenceId = binding.editTextReferenceId.text.toString()

        paymentViewModel.apply {
            if (paymentType == PaymentType.PIX) {
                payByPix(amount, referenceId)
            } else {
                pay(amount, paymentType, installments, referenceId)
            }
        }
    }

    private fun getTapOnPhoneTheme(): TapOnPhoneTheme {
        return TapOnPhoneTheme(
            logo = AppCompatResources.getDrawable(this, R.drawable.baseline_android_24),
            backgroundColor = Color.argb(255, 255, 255, 255),
            marginTopDPStatusMessages = 40f,
            marginTopDPAmount = 0f,
            marginTopDPPaymentType = 8f,
            amountTextColor = Color.parseColor("#000000"),
            paymentTypeTextColor = Color.parseColor("#FFFF0000"),
            statusTextColor = 0xFF000000.toInt(),
            cardAnimationArrangement = CardAnimationArrangement.MIDDLE,
            cardAnimationSize = null,
            pinPadType = PinPadType.STANDARD,
            errorScreenConfiguration = ErrorScreenConfiguration(
                screenBackgroundColor = Color.parseColor("#FFBFBFBF"),
                errorCodeTextStyle = ErrorCodeTextStyle(
                    textColor = Color.parseColor("#FF8B0000"),
                    fontSize = 26
                ),
                errorMessageTextStyle = ErrorMessageTextStyle(
                    textColor = Color.parseColor("#FF000000"),
                    fontSize = 24
                ),
                backButtonConfiguration = BackButtonConfiguration(
                    isVisible = true,
                    text = getString(R.string.return_to_home),
                    containerColor = Color.parseColor("#FF8B0000"),
                    contentColor = Color.parseColor("#FFFFFFFF")
                ),
            ),
            messagesEventStatus = mapOf(
                MessagesEventStatus.StartPaymentProcess to MessageEvent(
                    title = "Iniciando pagamento",
                    subtitle = "Aguarde..."
                ),

                MessagesEventStatus.HoldCard to MessageEvent(
                    title = "Mantenha o cartão nessa posição",
                    subtitle = "Mantenha assim por alguns segundos"
                ),

                MessagesEventStatus.StartCardReading to MessageEvent(
                    title = "Aproxime o cartão",
                    subtitle = "Aproxime o cartão no leitor"
                ),

                MessagesEventStatus.StartCardReadingAgain to MessageEvent(
                    title = "Reaproxime o cartão, por favor",
                    subtitle = ""
                ),

                MessagesEventStatus.CompletePaymentProcess to MessageEvent(
                    title = "Processando pagamento",
                    subtitle = "Aguarde um instante..."
                ),

                MessagesEventStatus.AuthorisingPleaseWait to MessageEvent(
                    title = "Autorizando",
                    subtitle = "Aguarde, por favor"
                ),

                MessagesEventStatus.StartPinInput to MessageEvent(
                    title = "Inserir a senha do cartão",
                    subtitle = ""
                ),
            ),
            headerMessagesEventStatus = mapOf(
                MessagesEventStatus.StartCardReading to MessageEvent(
                    title = "Aproxime o cartão header",
                    subtitle = "Aproxime o cartão no leitor header"
                ),
            ),
            brandBackgroundColor = "#F68427",
            topCancelIcon = AppCompatResources.getDrawable(this, R.drawable.ic_button_close),
            statusBarColor =  Color.parseColor("#000000")
        )
    }
}