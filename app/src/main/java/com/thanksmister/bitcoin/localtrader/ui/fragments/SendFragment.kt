/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.PinCodeActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.ScanQrCodeActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.SendActivity
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.WalletViewModel
import com.thanksmister.bitcoin.localtrader.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_send.*
import timber.log.Timber
import javax.inject.Inject

class SendFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: WalletViewModel

    private var address: String? = null
    private var amount: String? = null
    private var balance: String? = null
    private var rate: String? = null
    private var confirming: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            address = arguments!!.getString(EXTRA_ADDRESS)
            amount = arguments!!.getString(EXTRA_AMOUNT)
        } else if (savedInstanceState != null) {
            address = savedInstanceState.getString(SendActivity.EXTRA_QR_ADDRESS)
            amount = savedInstanceState.getString(SendActivity.EXTRA_QR_AMOUNT)
        }

        setHasOptionsMenu(true)
    }

    private fun observeViewModel(viewModel: WalletViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(requireActivity(), it.message)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(requireActivity(), it)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            message?.let {
                dialogUtils.hideProgressDialog()
                dialogUtils.toast(message)
            }
        })
        viewModel.getPendingMessage().observe(this, Observer { value ->
            value?.let {
                if(it) {
                    dialogUtils.showProgressDialog(requireActivity(), getString(R.string.progress_sending_transaction))
                } else {
                    dialogUtils.hideProgressDialog()
                    dialogUtils.toast(R.string.toast_transaction_success)
                    clearForm()
                }
            }
        })
        viewModel.getShowProgressBar().observe(this, Observer { value ->
            if(value.isNullOrEmpty()) {
                dialogUtils.hideProgressDialog()
            } else {
                dialogUtils.showProgressDialog(requireActivity(), value)
            }
        })
        viewModel.getShowProgress().observe(this, Observer { show ->
            show?.let {
                if(it) {
                    dialogUtils.showProgressDialog(requireActivity(), getString(R.string.progress_sending_transaction))
                } else if (confirming) {
                    dialogUtils.hideProgressDialog()
                }
            }
        })
        disposable.add(viewModel.getWalletData()
                .applySchedulersIo()
                .subscribe( { data ->
                    if (data != null) {
                        rate = data.rate
                        balance = data.sendable
                        setWallet()
                    } else {
                        computeBalance(0.0)
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SendActivity.EXTRA_QR_ADDRESS, address)
        outState.putString(SendActivity.EXTRA_QR_AMOUNT, amount)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("onActivityResult: requestCode $requestCode")
        Timber.d("onActivityResult: resultCode $resultCode")
        if (requestCode == PinCodeActivity.REQUEST_CODE) {
            if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
                val pinCode = intent!!.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE)
                val address = intent.getStringExtra(PinCodeActivity.EXTRA_ADDRESS)
                val amount = intent.getStringExtra(PinCodeActivity.EXTRA_AMOUNT)
                pinCodeEvent(pinCode, address, amount)
            }
        } else if (requestCode == ScanQrCodeActivity.SCAN_INTENT) {
            if(resultCode == ScanQrCodeActivity.SCAN_SUCCESS) {
                address = intent?.getStringExtra(PinCodeActivity.EXTRA_ADDRESS)
                amount = intent?.getStringExtra(PinCodeActivity.EXTRA_AMOUNT)
                address?.let {
                    setBitcoinAddress(it)
                }
                amount?.let {
                    setAmount(it)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.send, menu)
        val itemLen = menu.size()
        for (i in 0 until itemLen) {
            val drawable = menu.getItem(i).icon
            if (drawable != null) {
                drawable.mutate()
                drawable.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP)
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_paste -> {
                setAddressFromClipboard()
                return true
            }
            R.id.action_scan -> {
                startBarCodeScanner()
                return true
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_send, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)

        if (!TextUtils.isEmpty(amount)) {
            amountText.setText(amount)
        }
        if (!TextUtils.isEmpty(address)) {
            addressText.setText(address)
        }
        sendDescription.text = Html.fromHtml(getString(R.string.pin_code_send))
        sendDescription.movementMethod = LinkMovementMethod.getInstance()
        addressText.setOnTouchListener { _, _ ->
            if (TextUtils.isEmpty(addressText.text.toString())) {
                setAddressFromClipboardTouch()
            }
            false
        }
        amountText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
                amount = charSequence.toString()
            }
            override fun afterTextChanged(editable: Editable) {
                if (amountText.hasFocus()) {
                    val bitcoin = editable.toString()
                    calculateCurrencyAmount(bitcoin)
                }
            }
        })
        fiatEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (fiatEditText.hasFocus()) {
                    val amount = editable.toString()
                    calculateBitcoinAmount(amount)
                }
            }
        })
        balanceText.setOnClickListener {
            val balanceAmount = Conversions.convertToDouble(balance)
            amountText.setText(balanceAmount.toString())
            calculateCurrencyAmount(balanceAmount.toString())
            Toast.makeText(requireContext(), getString(R.string.text_add_spendable_amount), Toast.LENGTH_SHORT).show()
        }
        sendButton.setOnClickListener {
            validateForm()
        }
        val currency = preferences.exchangeCurrency
        currencyText.text = currency

        computeBalance(0.0)

        observeViewModel(viewModel)
    }

    private fun startBarCodeScanner() {
        startActivityForResult(ScanQrCodeActivity.createStartIntent(requireActivity()), ScanQrCodeActivity.SCAN_INTENT)
    }

    private fun clearForm() {
        confirming = false
        amountText.setText("")
        addressText.setText("")
        fiatEditText.setText("")
    }

    private fun setWallet() {
        computeBalance(0.0);
        if (TextUtils.isEmpty(amountText.text.toString())) {
            calculateCurrencyAmount("0.00")
        } else {
            calculateCurrencyAmount(amountText.text.toString())
        }
    }

    private fun validateForm() {
        if (TextUtils.isEmpty(amountText.text.toString())) {
            dialogUtils.toast(getString(R.string.error_missing_address_amount))
            return
        }
        amount = Conversions.formatBitcoinAmount(amountText.text.toString())
        address = addressText.text.toString();
        if (TextUtils.isEmpty(address)) {
            dialogUtils.toast(getString(R.string.error_missing_address_amount))
            return
        }
        if (TextUtils.isEmpty(amount) || !WalletUtils.validAmount(amount)) {
            dialogUtils.toast(getString(R.string.toast_invalid_btc_amount))
            return
        }
        if(isAdded && activity != null) {
            disposable.add(
                    validateBitcoinAddress(address!!)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ valid ->
                                activity?.runOnUiThread {
                                    if (valid) {
                                        promptForPin(address!!, amount!!)
                                    } else {
                                        dialogUtils.toast(getString(R.string.toast_invalid_address))
                                    }
                                }
                            }, { error ->
                                Timber.e(error.message)
                            }))
        }
    }

    private fun promptForPin(bitcoinAddress: String, bitcoinAmount: String) {
        val intent = PinCodeActivity.createStartIntent(activity!!, bitcoinAddress, bitcoinAmount);
        startActivityForResult(intent, PinCodeActivity.REQUEST_CODE) // be sure to do this from fragment context
    }

    private fun setAddressFromClipboardTouch() {
        val clipText = getClipboardText()
        if (TextUtils.isEmpty(clipText)) {
            return
        }
        val bitcoinAddress = WalletUtils.parseBitcoinAddress(clipText)
        disposable.add(
                validateBitcoinAddress(bitcoinAddress)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( { valid ->
                            activity?.runOnUiThread {
                                if (valid) {
                                    setAddressFromClipboard()
                                } else {
                                    dialogUtils.toast(getString(R.string.toast_invalid_address))
                                }
                            }
                        }, { error ->
                            Timber.e(error.message)
                        }))
    }

    private fun setAddressFromClipboard() {
        val clipText = getClipboardText()
        if (TextUtils.isEmpty(clipText)) {
            dialogUtils.toast(R.string.toast_clipboard_empty)
            return
        }
        val bitcoinAddress = WalletUtils.parseBitcoinAddress(clipText)
        val bitcoinAmount = WalletUtils.parseBitcoinAmount(clipText)
        disposable.add(
                validateBitcoinAddress(bitcoinAddress)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( { valid ->
                            activity?.runOnUiThread {
                                if (valid) {
                                    setBitcoinAddress(bitcoinAddress)
                                    bitcoinAmount?.let {
                                        if (WalletUtils.validAmount(it)) {
                                            setAmount(it)
                                        } else {
                                            dialogUtils.toast(getString(R.string.toast_invalid_btc_amount))
                                        }
                                    }
                                } else {
                                    dialogUtils.toast(getString(R.string.toast_invalid_address))
                                }
                            }
                        }, { error ->
                            Timber.e(error.message)
                        }))
    }

    private fun setBitcoinAddress(bitcoinAddress: String?) {
        bitcoinAddress?.let {
            address = it
            addressText.setText(it)
        }
    }

    private fun setAmount(bitcoinAmount: String?) {
        bitcoinAmount?.let {
            amount = it
            amountText.setText(it)
            calculateCurrencyAmount(it)
        }
    }

    private fun getClipboardText(): String {
        var clipText = ""
        val clipboardManager = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboardManager.primaryClip
        if (clip != null) {
            val item = clip.getItemAt(0)
            if (item.text != null)
                clipText = item.text.toString()
        }
        return clipText
    }

    private fun pinCodeEvent(pinCode: String, address: String, amount: String) {
        this.address = address
        this.amount = amount
        val confirmTitle = getString(R.string.text_confirm_transaction)
        val confirmDescription = getString(R.string.send_confirmation_description, amount, address)
        dialogUtils.showAlertDialog(activity!!, confirmTitle, confirmDescription, DialogInterface.OnClickListener { dialog, which ->
            confirmedPinCodeSend(pinCode, address, amount)
        }, DialogInterface.OnClickListener { dialog, which ->
            // na-da
        })
    }

    private fun confirmedPinCodeSend(pinCode: String, address: String, amount: String) {
        if (!confirming) {
            confirming = true
            viewModel.sendBitcoin(pinCode, address, amount)
        }
    }

    private fun calculateCurrencyAmount(bitcoin: String) {

        try {
            if (bitcoin.toDouble() == 0.0) {
                fiatEditText.setText("")
                computeBalance(0.0)
                return
            }
        } catch (e: Exception) {
            Timber.e(e.message)
            return
        }
        try {
            computeBalance(bitcoin.toDouble())
            if(rate != null) {
                val value = Calculations.computedValueOfBitcoin(rate, bitcoin)
                fiatEditText.setText(value)
            }
        } catch (e: Exception) {
            Timber.e(e.message)
        }
    }

    private fun computeBalance(btcAmount: Double) {
        Timber.d("computeBalance")
        if(balance != null) {
            val balanceAmount = Conversions.convertToDouble(balance)
            val btcBalance = Conversions.formatBitcoinAmount(balanceAmount - btcAmount)
            if (balanceAmount < btcAmount) {
                balanceText.text = getString(R.string.form_balance_negative, btcBalance)
            } else {
                balanceText.text = getString(R.string.form_balance_positive, btcBalance)
            }
        } else {
            balanceText.text = getString(R.string.form_balance_positive, btcAmount.toString())
        }
    }

    private fun calculateBitcoinAmount(fiat: String) {
        try {
            if (Doubles.convertToDouble(fiat) == 0.0) {
                computeBalance(0.0)
                amount = ""
                amountText.setText(amount)
                return
            }
        } catch (e: Exception) {
            Timber.e(e.message)
            return
        }
        if(rate!= null) {
            val btc = Math.abs(Doubles.convertToDouble(fiat) / Doubles.convertToDouble(rate))
            amount = Conversions.formatBitcoinAmount(btc)
            amountText.setText(amount) // set bitcoin amount
            computeBalance(btc)
        }
    }

    private fun validateBitcoinAddress(address: String?): Observable<Boolean> {
        return Observable.create { subscriber ->
            try {
                val bitcoinAddress = WalletUtils.parseBitcoinAddress(address)
                val valid = WalletUtils.validBitcoinAddress(bitcoinAddress)
                subscriber.onNext(valid)
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    companion object {
        const val EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS"
        const val EXTRA_AMOUNT = "com.thanksmister.extra.EXTRA_AMOUNT"
        fun newInstance(address: String?, amount: String?): SendFragment {
            val fragment = SendFragment()
            val args = Bundle()
            if(address != null) {
                args.putString(EXTRA_ADDRESS, address)
            }
            if(amount != null) {
                args.putString(EXTRA_AMOUNT, amount)
            }
            fragment.arguments = args
            return fragment
        }
    }
}