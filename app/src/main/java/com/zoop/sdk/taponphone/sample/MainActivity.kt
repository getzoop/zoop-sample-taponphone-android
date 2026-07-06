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
import com.zoop.sdk.plugin.taponphone.api.Credentials
import com.zoop.sdk.plugin.taponphone.api.ErrorCodeTextStyle
import com.zoop.sdk.plugin.taponphone.api.ErrorMessageTextStyle
import com.zoop.sdk.plugin.taponphone.api.ErrorScreenConfiguration
import com.zoop.sdk.plugin.taponphone.api.MessagesEventStatus
import com.zoop.sdk.plugin.taponphone.api.PaymentType
import com.zoop.sdk.plugin.taponphone.api.PinPadType
import com.zoop.sdk.plugin.taponphone.api.TapOnPhoneTheme
import com.zoop.sdk.taponphone.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var alertDialogBuilder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel.setConfig(applicationContext, getTapOnPhoneTheme())

        alertDialogBuilder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.config_details_title))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }

        alertDialogBuilder.create()

        binding.apply {
            marketplaceTextInput.setText(BuildConfig.MARKETPLACE.ifEmpty { "" })
            sellerTextInput.setText(BuildConfig.SELLER.ifEmpty { "" })
            accessKeyTextInput.setText(BuildConfig.API_KEY.ifEmpty { "" })

            buttonPay.setOnClickListener(::onButtonPayClicked)

            buttonCancel.setOnClickListener {
                containerCredential.visibility = View.GONE
            }

            buttonTimeoutCancel.setOnClickListener {
                containerTimeout.visibility = View.GONE
            }

            buttonTimeoutConfirm.setOnClickListener {
                val discoveryTimeout = discoveryTimeoutTextInput.text.toString().toIntOrNull() ?: 120_000
                val processingTimeout = processingTimeoutTextInput.text.toString().toIntOrNull() ?: 30_000
                val networkTimeout = networkTimeoutTextInput.text.toString().toIntOrNull() ?: 45_000
                val totalElapsedTimeout = totalElapsedTimeoutTextInput.text.toString().toIntOrNull() ?: 180_000

                mainViewModel.configureTimeout(
                    discoveryTimeout = discoveryTimeout,
                    processingTimeout = processingTimeout,
                    networkTimeout = networkTimeout,
                    totalElapsedTimeout = totalElapsedTimeout,
                )

                containerTimeout.visibility = View.GONE
            }

            buttonConfirm.setOnClickListener {
                val marketplace = marketplaceTextInput.text.toString()
                val seller = sellerTextInput.text.toString()
                val accessKey = accessKeyTextInput.text.toString()
                setCredentials(marketplace = marketplace, seller = seller, accessKey = accessKey)
                containerCredential.visibility = View.GONE
            }

        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.uiState.collect {
                    updateDetailsConfig(it.detailsConfig)
                    displayPaymentResult(it)
                    displayInitializationInfo(it)
                }
            }
        }
    }

    private fun updateDetailsConfig(configDetails: String?) {
        alertDialogBuilder.setMessage(configDetails)
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

        mainViewModel.credentials = Credentials(
            clientId = BuildConfig.CLIENT_ID.ifEmpty { "" },
            clientSecret = BuildConfig.CLIENT_SECRET.ifEmpty { "" },
            marketplace = marketplace,
            seller = seller,
            accessKey = accessKey,
        )
        mainViewModel.setConfig(applicationContext, getTapOnPhoneTheme())
    }

    private fun displayPaymentResult(uiState: MainViewModel.UiState) {
        val status = when (uiState.paymentStatus) {
            PaymentStatus.Processing,
            PaymentStatus.SessionActivationStarted -> return

            PaymentStatus.Success,
            PaymentStatus.Complete,
            PaymentStatus.SessionActivated -> {
                binding.fragmentContainer.visibility = View.VISIBLE
                binding.textViewPaymentResult.visibility = View.VISIBLE
                binding.textViewPaymentResult.setTextColor(Color.GREEN)
                "PAGAMENTO APROVADO!"
            }

            PaymentStatus.QRCode -> {
                binding.textViewPaymentResult.visibility = View.VISIBLE
                binding.textViewPaymentResult.setTextColor(Color.BLUE)
                "QR CODE GERADO"
            }

            PaymentStatus.Fail,
            PaymentStatus.SessionActivationFail -> {
                binding.fragmentContainer.visibility = View.GONE
                binding.textViewPaymentResult.visibility = View.VISIBLE
                binding.textViewPaymentResult.setTextColor(Color.RED)
                if (uiState.initializationStatus == InitializationStatus.Error) {
                    "ERRO AO INICIALIZAR!"
                } else {
                    "PAGAMENTO NEGADO!"
                }
            }
        }

        binding.textViewPaymentResult.text = status
        binding.textViewPaymentInfo.text =
            "${uiState.errorMessage ?: ""}\n\nID:${uiState.transactionId ?: ""}"
    }

    private fun displayInitializationInfo(uiState: MainViewModel.UiState) {
        if (uiState.initializationStatus == InitializationStatus.Error) {
            binding.textViewPaymentResult.visibility = View.VISIBLE
            binding.textViewPaymentResult.text = uiState.errorMessage ?: "Erro ao inicializar"
        }
    }

    private fun getTapOnPhoneTheme() = TapOnPhoneTheme(
        logo = AppCompatResources.getDrawable(this, R.drawable.baseline_android_24),
        backgroundColor = Color.argb(255, 255, 255, 255),
        amountTextColor = Color.parseColor("#000000"),
        paymentTypeTextColor = Color.parseColor("#FFFF0000"),
        statusTextColor = 0xFF000000.toInt(),
        cardAnimationArrangement = CardAnimationArrangement.MIDDLE,
        pinPadType = PinPadType.STANDARD,
        brandBackgroundColor = "#F68427",
        topCancelIcon = AppCompatResources.getDrawable(this, R.drawable.ic_button_close),
        statusBarColor = Color.parseColor("#000000"),
        errorScreenConfiguration = ErrorScreenConfiguration(
            screenBackgroundColor = Color.parseColor("#FFBFBFBF"),
            errorCodeTextStyle = ErrorCodeTextStyle(
                textColor = Color.parseColor("#FF8B0000"),
                fontSize = 26,
            ),
            errorMessageTextStyle = ErrorMessageTextStyle(
                textColor = Color.parseColor("#FF000000"),
                fontSize = 24,
            ),
            backButtonConfiguration = BackButtonConfiguration(
                isVisible = true,
                text = getString(R.string.return_to_home),
                containerColor = Color.parseColor("#FF8B0000"),
                contentColor = Color.parseColor("#FFFFFFFF"),
            ),
        ),
        messagesEventStatus = mapOf(
            MessagesEventStatus.TerminalActivationStarted to MessageEvent(
                title = "Ativando o terminal",
                subtitle = "Por favor, aguarde...",
            ),
            MessagesEventStatus.PaymentProcessStarted to MessageEvent(
                title = "Iniciando pagamento",
                subtitle = "Aguarde...",
            ),
            MessagesEventStatus.CardReadingStarted to MessageEvent(
                title = "Aproxime o cartão",
                subtitle = "Aproxime o cartão no leitor",
            ),
            MessagesEventStatus.CardReadingRetry to MessageEvent(
                title = "Reaproxime o cartão, por favor",
                subtitle = "",
            ),
            MessagesEventStatus.HoldCardSteady to MessageEvent(
                title = "Mantenha o cartão nessa posição",
                subtitle = "Mantenha assim por alguns segundos",
            ),
            MessagesEventStatus.PaymentProcessFinished to MessageEvent(
                title = "Processando pagamento",
                subtitle = "Aguarde um instante...",
            ),
            MessagesEventStatus.AuthorisingPleaseWait to MessageEvent(
                title = "Autorizando",
                subtitle = "Aguarde, por favor",
            ),
            MessagesEventStatus.PinInputStarted to MessageEvent(
                title = "Inserir a senha do cartão",
                subtitle = "",
            ),
        ),
    )

    private fun onButtonPayClicked(view: View) {
        val amount = binding.editTextAmount.text.toString().toLongOrNull() ?: 0L
        val paymentType = when (binding.radioGroupPaymentType.checkedRadioButtonId) {
            R.id.radioButtonCredit -> PaymentType.CREDIT
            R.id.radioButtonDebit -> PaymentType.DEBIT
            R.id.radioButtonPix -> PaymentType.PIX
            else -> PaymentType.CREDIT
        }
        val installments = binding.editTextInstallments.text.toString().toIntOrNull()
        val referenceId = binding.editTextReferenceId.text.toString().ifEmpty { null }

        if (paymentType == PaymentType.PIX) {
            mainViewModel.payByPix(amount, referenceId)
        } else {
            mainViewModel.pay(amount, paymentType, installments, referenceId)
        }
    }
}
