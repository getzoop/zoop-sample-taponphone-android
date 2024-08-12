package com.zoop.sdk.taponphone.sample

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zoop.sdk.taponphone.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import com.zoop.sdk.plugin.taponphone.TapOnPhoneTheme
import com.zoop.sdk.type.Option

const val TAG = "MainActivityLog"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val paymentViewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            marketplaceTextInput.setText(BuildConfig.MARKETPLACE.ifEmpty { "" })
            sellerTextInput.setText(BuildConfig.SELLER.ifEmpty { "" })
            accessKeyTextInput.setText(BuildConfig.API_KEY.ifEmpty { "" })

            radioGroupPaymentType.setOnCheckedChangeListener(::onPaymentTypeChanged)
            buttonPay.setOnClickListener(::onButtonPayClicked)

            buttonDefineCredential.setOnClickListener {
                binding.containerCredential.visibility = View.VISIBLE
                binding.buttonDefineCredential.visibility = View.GONE
            }

            buttonCancel.setOnClickListener {
                containerCredential.visibility = View.GONE
                binding.buttonDefineCredential.visibility = View.VISIBLE
            }

            buttonConfirm.setOnClickListener {
                val marketplace = marketplaceTextInput.text.toString()
                val seller = sellerTextInput.text.toString()
                val accessKey = accessKeyTextInput.text.toString()

                setCredentials(marketplace = marketplace, seller = seller, accessKey = accessKey)

                binding.buttonDefineCredential.visibility = View.VISIBLE
                binding.containerCredential.visibility = View.GONE
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                paymentViewModel.uiState.collect {
                    displayPaymentResult(it)
                }
            }
        }
        initialize()
    }

    private fun setCredentials(marketplace: String, seller: String, accessKey: String) {
        if (marketplace.isEmpty() || seller.isEmpty() || accessKey.isEmpty()) {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.message_fill_all_input_data),
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }

        /*
        val credentials = InitializationRequest.Credentials(
            clientId = BuildConfig.CLIENT_ID.ifEmpty { "" },
            clientSecret = BuildConfig.CLIENT_SECRET.ifEmpty { "" },
            marketplace,
            seller,
            accessKey
        )
        paymentViewModel.setCredential(credentials)

         */
    }

    private fun onPaymentTypeChanged(group: RadioGroup, checkedId: Int) {
        when (checkedId) {
            R.id.radioButtonCredit -> {
                binding.editTextInstallments.visibility = View.VISIBLE
            }

            R.id.radioButtonDebit -> {
                binding.editTextInstallments.visibility = View.GONE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayPaymentResult(uiState: PaymentViewModel.UiState) {
        val status = when (uiState.paymentStatus) {
            PaymentStatus.Processing -> return
            PaymentStatus.Success -> {
                binding.textViewPaymentResult.setTextColor(Color.GREEN)
                "PAGAMENTO APROVADO!"
            }

            PaymentStatus.Fail -> {
                binding.textViewPaymentResult.setTextColor(Color.RED)
                "PAGAMENTO NEGADO!"
            }
        }
        binding.textViewPaymentResult.text = status
        binding.textViewPaymentInfo.text =
            "${uiState.errorMessage ?: ""}\n\nID:${uiState.transactionId ?: ""}"
    }

    private fun initialize() {
        paymentViewModel.initialize(
            context = this,
            useExternalScreen = false,
            arrangement = TapOnPhoneTheme.Arrangement.DEFAULT
        )
    }

    private fun onButtonPayClicked(view: View) {
        val amount = binding.editTextAmount.text.toString().toLongOrNull() ?: 0L
        val paymentType = when (binding.radioGroupPaymentType.checkedRadioButtonId) {
            R.id.radioButtonCredit -> Option.CREDIT
            R.id.radioButtonDebit -> Option.DEBIT
            else -> Option.CREDIT
        }

        val installments = binding.editTextInstallments.text.toString().toIntOrNull()
        paymentViewModel.pay(
            amount = amount,
            paymentType = paymentType,
            installments = installments ?: 1,
            onApplicationEvent = {
                Log.d(TAG, "onApplicationEvent:$it ")
            },
            onPaymentSuccess = {
                Log.d(TAG, "onPaymentSuccess:$it ")
            },
            onPaymentError = {
                Log.d(TAG, "onPaymentError:$it ")
            }
        )
    }

    /*
    private fun getTapOnPhoneTheme(): TapOnPhoneTheme {
        return TapOnPhoneTheme(
            logo = getDrawable(R.drawable.baseline_android_24),
            backgroundColor = null, // Default is android:background from theme.xml
            animationColor = null, // Default is colorPrimary from theme.xml
            amountTextColor = null, // Default is android:textColor from theme.xml
            paymentTypeTextColor = null, // Default is android:textColor from theme.xml
            statusTextColor = null // Default is android:textColor from theme.xml
        )
    }

     */
}