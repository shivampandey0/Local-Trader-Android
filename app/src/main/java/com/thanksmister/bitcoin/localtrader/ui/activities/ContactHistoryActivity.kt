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

package com.thanksmister.bitcoin.localtrader.ui.activities

import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.crashlytics.android.Crashlytics
import com.thanksmister.bitcoin.localtrader.BuildConfig
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.managers.ConnectionLiveData
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.network.api.model.Message
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.MessageAdapter
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.ContactsViewModel
import com.thanksmister.bitcoin.localtrader.utils.Conversions
import com.thanksmister.bitcoin.localtrader.utils.Dates
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_contact.*
import timber.log.Timber
import javax.inject.Inject

class ContactHistoryActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var viewModel: ContactsViewModel

    private var connectionLiveData: ConnectionLiveData? = null

    private var detailsEthereumAddress: TextView? = null
    private var detailsSortCode: TextView? = null
    private var detailsBSB: TextView? = null
    private var detailsAccountNumber: TextView? = null
    private var detailsBillerCode: TextView? = null
    private var detailsPhoneNumber: TextView? = null
    private var detailsReceiverEmail: TextView? = null
    private var detailsReceiverName: TextView? = null
    private var detailsIbanName: TextView? = null
    private var detailsSwiftBic: TextView? = null
    private var detailsReference: TextView? = null

    private var onlineOptionsLayout: View? = null
    private var detailsEthereumAddressLayout: View? = null
    private var detailsSortCodeLayout: View? = null
    private var detailsBSBLayout: View? = null
    private var detailsAccountNumberLayout: View? = null
    private var detailsBillerCodeLayout: View? = null
    private var detailsPhoneNumberLayout: View? = null
    private var detailsReceiverEmailLayout: View? = null
    private var detailsReceiverNameLayout: View? = null
    private var detailsIbanLayout: View? = null
    private var detailsSwiftBicLayout: View? = null
    private var detailsReferenceLayout: View? = null
    private var tradePrice: TextView? = null
    private var tradeAmountTitle: TextView? = null
    private var tradeAmount: TextView? = null
    private var tradeReference: TextView? = null
    private var tradeId: TextView? = null
    private var traderName: TextView? = null
    private var tradeFeedback: TextView? = null
    private var tradeCount: TextView? = null
    private var tradeType: TextView? = null
    private var noteText: TextView? = null
    private var lastSeenIcon: View? = null
    private var buttonLayout: View? = null
    private var contactHeaderLayout: View? = null
    private var dealPrice: TextView? = null
    private var contactButton: Button? = null
    private var adapter: MessageAdapter? = null
    private var contactId: Int = 0
    private var contact: Contact? = null
    private var downloadManager: DownloadManager? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                showDownload()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.view_contact)

        contactId = if (savedInstanceState == null) {
            intent.getIntExtra(EXTRA_ID, 0)
        } else {
            savedInstanceState.getInt(EXTRA_ID, 0)
        }

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = ""
        }

        contactSwipeLayout.setOnRefreshListener(this)
        contactSwipeLayout.setColorSchemeColors(resources.getColor(R.color.red))

        val headerView = View.inflate(this, R.layout.view_contact_header, null)
        val messageButton = headerView.findViewById<View>(R.id.messageButton) as ImageButton
        messageButton.setOnClickListener { sendNewMessage() }

        detailsEthereumAddress = headerView.findViewById<View>(R.id.detailsEthereumAddress) as TextView
        detailsSortCode = headerView.findViewById<View>(R.id.detailsSortCode) as TextView
        detailsBSB = headerView.findViewById<View>(R.id.detailsBSB) as TextView
        detailsAccountNumber = headerView.findViewById<View>(R.id.detailsAccountNumber) as TextView
        detailsBillerCode = headerView.findViewById<View>(R.id.detailsBillerCode) as TextView
        detailsPhoneNumber = headerView.findViewById<View>(R.id.detailsPhoneNumber) as TextView
        detailsReceiverEmail = headerView.findViewById<View>(R.id.detailsReceiverEmail) as TextView
        detailsReceiverName = headerView.findViewById<View>(R.id.detailsReceiverName) as TextView
        detailsIbanName = headerView.findViewById<View>(R.id.detailsIbanName) as TextView
        detailsSwiftBic = headerView.findViewById<View>(R.id.detailsSwiftBic) as TextView
        detailsReference = headerView.findViewById<View>(R.id.detailsReference) as TextView

        onlineOptionsLayout = headerView.findViewById(R.id.onlineOptionsLayout)
        detailsEthereumAddressLayout = headerView.findViewById(R.id.detailsEthereumAddressLayout)
        detailsSortCodeLayout = headerView.findViewById(R.id.detailsSortCodeLayout)
        detailsBSBLayout = headerView.findViewById(R.id.detailsBSBLayout)
        detailsAccountNumberLayout = headerView.findViewById(R.id.detailsAccountNumberLayout)
        detailsBillerCodeLayout = headerView.findViewById(R.id.detailsBillerCodeLayout)
        detailsPhoneNumberLayout = headerView.findViewById(R.id.detailsPhoneNumberLayout)
        detailsReceiverEmailLayout = headerView.findViewById(R.id.detailsReceiverEmailLayout)
        detailsReceiverNameLayout = headerView.findViewById(R.id.detailsReceiverNameLayout)
        detailsIbanLayout = headerView.findViewById(R.id.detailsIbanLayout)
        detailsSwiftBicLayout = headerView.findViewById(R.id.detailsSwiftBicLayout)
        detailsReferenceLayout = headerView.findViewById(R.id.detailsReferenceLayout)

        tradeAmountTitle = headerView.findViewById<View>(R.id.tradeAmountTitle) as TextView
        tradePrice = headerView.findViewById<View>(R.id.tradePrice) as TextView
        tradeAmount = headerView.findViewById<View>(R.id.tradeAmount) as TextView
        tradeReference = headerView.findViewById<View>(R.id.tradeReference) as TextView
        tradeId = headerView.findViewById<View>(R.id.tradeId) as TextView
        traderName = headerView.findViewById<View>(R.id.traderName) as TextView
        tradeFeedback = headerView.findViewById<View>(R.id.tradeFeedback) as TextView
        tradeType = headerView.findViewById<View>(R.id.tradeType) as TextView
        noteText = headerView.findViewById<View>(R.id.noteTextContact) as TextView
        tradeCount = headerView.findViewById<View>(R.id.tradeCount) as TextView
        dealPrice = headerView.findViewById<View>(R.id.dealPrice) as TextView
        lastSeenIcon = headerView.findViewById(R.id.lastSeenIcon)
        contactHeaderLayout = headerView.findViewById(R.id.contactHeaderLayout)
        buttonLayout = findViewById(R.id.buttonLayout)
        contactButton = findViewById<View>(R.id.contactButton) as Button
        contactButton!!.visibility = View.GONE

        contactList.addHeaderView(headerView, null, false)
        contactList.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val message = adapterView.adapter.getItem(i) as Message
            setMessageOnClipboard(message)
        }

        contactList.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                val topRowVerticalPosition = if (contactList == null || contactList.childCount == 0) 0 else contactList.getChildAt(0).top
                contactSwipeLayout.isEnabled = firstVisibleItem == 0 && topRowVerticalPosition >= 0
            }
        })

        adapter = MessageAdapter(this)
        setAdapter(adapter!!)

        if (contactId == 0) {
            dialogUtils.showAlertDialog(this@ContactHistoryActivity, getString(R.string.toast_error_contact_data), DialogInterface.OnClickListener { dialog, which ->
                finish();
            })
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ContactsViewModel::class.java)
        observeViewModel(viewModel)
    }

    override fun onStart() {
        super.onStart()
        connectionLiveData = ConnectionLiveData(this)
        connectionLiveData?.observe(this, Observer { connected ->
            if(!connected!!) {
                dialogUtils.toast(getString(R.string.error_network_disconnected))
                onRefreshStop()
            } else {
                dialogUtils.toast(getString(R.string.toast_refreshing_data))
                updateData()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_ID, contactId)
    }

    private fun observeViewModel(viewModel: ContactsViewModel) {
        viewModel.getNetworkMessage().observe(this, Observer { message ->
            if (message?.message != null) {
                onRefreshStop()
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@ContactHistoryActivity, message.message!!)
            }
        })
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null) {
                onRefreshStop()
                dialogUtils.hideProgressDialog()
                dialogUtils.showAlertDialog(this@ContactHistoryActivity, message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if(message != null) {
                onRefreshStop()
                dialogUtils.hideProgressDialog()
                dialogUtils.toast(message)
            }
        })

        viewModel.getContact().observe(this, Observer { contact ->
            if(contact != null) {
                setTitle(contact)
                setContact(contact)
                showOnlineOptions(contact)
                contactList.visibility = View.VISIBLE
            }
        })

        // update contact data
        dialogUtils.showProgressDialog(this@ContactHistoryActivity, getString(R.string.toast_loading_trade))
        updateData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        when (item.itemId) {
            R.id.action_send -> {
                sendNewMessage()
                return true
            }
            R.id.action_profile -> {
                showProfile()
                return true
            }
            R.id.action_advertisement -> {
                showAdvertisement()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.contacthistory, menu)
        return true
    }

    override fun onRefresh() {
        updateData()
    }

    private fun onRefreshStop() {
        contactSwipeLayout.isRefreshing = false
    }

    private fun updateData() {
        disposable.add(viewModel.fetchContactData(contactId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    if(data != null) {
                        setTitle(data.contact)
                        setContact(data.contact)
                        showOnlineOptions(data.contact)
                        contactList.visibility = View.VISIBLE
                        if (!data.messages.isEmpty() && adapter != null) {
                            adapter!!.replaceWith(data.messages)
                        }
                    }
                    dialogUtils.hideProgressDialog()
                    onRefreshStop()
                }, { error ->
                    Timber.e("Data error: $error")
                    dialogUtils.hideProgressDialog()
                    onRefreshStop()
                }))
    }

    private fun setContact(contact: Contact) {
        this.contact = contact
        val date = Dates.parseLocalDateStringAbbreviatedTime(contact.createdAt)
        val amount = contact.amount + " " + contact.currency
        var type = ""

        val adTradeType = TradeType.valueOf(contact.advertisement.tradeType)
        when (adTradeType) {
            TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> {
                var paymentMethod = TradeUtils.getPaymentMethodName(this@ContactHistoryActivity, contact.advertisement.paymentMethod)
                paymentMethod = paymentMethod.replace("_", " ")
                type = if (contact.isBuying) getString(R.string.contact_list_buying_online, amount, paymentMethod, date) else getString(R.string.contact_list_selling_online, amount, paymentMethod, date)
            }
            TradeType.NONE -> TODO()
        }

        tradeType!!.text = Html.fromHtml(type)
        tradePrice!!.text = getString(R.string.trade_price, contact.amount, contact.currency)
        tradeAmount!!.text = contact.amountBtc + " " + getString(R.string.btc)
        tradeReference!!.text = contact.referenceCode
        tradeId!!.text = contact.contactId.toString()
        dealPrice!!.text = Conversions.formatDealAmount(contact.amountBtc, contact.amount) + " " + contact.currency
        tradeAmount!!.text = contact.amountBtc + " " + getString(R.string.btc)
        traderName!!.text = if (contact.isBuying) contact.seller.username else contact.buyer.username
        tradeFeedback!!.text = if (contact.isBuying) contact.seller.feedbackScore.toString() else contact.buyer.feedbackScore.toString()
        tradeCount!!.text = if (contact.isBuying) contact.seller.tradeCount else contact.buyer.tradeCount

        if (contact.isBuying) {
            lastSeenIcon!!.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.seller.lastOnline!!))
        } else if (contact.isSelling) {
            lastSeenIcon!!.setBackgroundResource(TradeUtils.determineLastSeenIcon(contact.buyer.lastOnline!!))
        }

        val buttonTag = TradeUtils.getTradeActionButtonLabel(contact)
        contactButton!!.tag = buttonTag
        if (buttonTag > 0) {
            contactButton!!.text = getString(buttonTag)
        }

        if (buttonTag == R.string.button_cancel || buttonTag == 0) {
            buttonLayout!!.visibility = View.GONE
        } else {
            buttonLayout!!.visibility = View.VISIBLE
        }

        val description = TradeUtils.getContactDescription(contact, this)
        if (!TextUtils.isEmpty(description)) {
            noteText!!.text = Html.fromHtml(description)
        }
        contactHeaderLayout!!.visibility = if (description == null) View.GONE else View.VISIBLE

        if (TradeUtils.isOnlineTrade(contact)) {
            tradeAmountTitle!!.setText(R.string.text_escrow_amount)
            showOnlineOptions(contact)
        }
    }

    private fun showOnlineOptions(contact: Contact?) {
        if (contact != null) {
            onlineOptionsLayout!!.visibility = View.VISIBLE
            if (!TextUtils.isEmpty(contact.accountDetails.bsb)) {
                detailsBSB!!.text = contact.accountDetails.bsb
                detailsBSBLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.iban)) {
                detailsIbanName!!.text = contact.accountDetails.iban
                detailsIbanLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.sortCode)) {
                detailsSortCode!!.text = contact.accountDetails.sortCode
                detailsSortCodeLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.receiverName)) {
                detailsReceiverName!!.text = contact.accountDetails.receiverName
                detailsReceiverNameLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.receiverEmail)) {
                detailsReceiverEmail!!.text = contact.accountDetails.receiverEmail
                detailsReceiverEmailLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.swiftBic)) {
                detailsSwiftBic!!.text = contact.accountDetails.swiftBic
                detailsSwiftBicLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.ethereumAddress)) {
                detailsEthereumAddress!!.text = contact.accountDetails.ethereumAddress
                detailsEthereumAddressLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.reference)) {
                detailsReference!!.text = contact.accountDetails.reference
                detailsReferenceLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.phoneNumber)) {
                detailsPhoneNumber!!.text = contact.accountDetails.phoneNumber
                detailsPhoneNumberLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.billerCode)) {
                detailsBillerCode!!.text = contact.accountDetails.billerCode
                detailsBillerCodeLayout!!.visibility = View.VISIBLE
            }
            if (!TextUtils.isEmpty(contact.accountDetails.accountNumber)) {
                detailsAccountNumber!!.text = contact.accountDetails.accountNumber
                detailsAccountNumberLayout!!.visibility = View.VISIBLE
            }
        }
    }

    private fun setAdapter(adapter: MessageAdapter) {
        contactList.adapter = adapter
    }

    private fun downloadAttachment(message: Message) {
        if (TextUtils.isEmpty(message.attachmentUrl)) {
            dialogUtils.showAlertDialog(this@ContactHistoryActivity, getString(R.string.toast_attachment_empty))
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("message_download", message.attachmentUrl)
                Crashlytics.logException(Exception("Error downloading url: " + message.attachmentUrl!!))
            }
            return
        }
        val token = preferences.getAccessToken()
        val request = DownloadManager.Request(Uri.parse(message.attachmentUrl + "?accessToken=" + token))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        request.setVisibleInDownloadsUi(true)
        request.setMimeType(message.attachmentType)
        request.setTitle(message.attachmentName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        try {
            downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager!!.enqueue(request)
        } catch (e: NullPointerException) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("message_download", message.attachmentUrl)
                Crashlytics.logException(Exception("Error downloading url: " + message.attachmentUrl!!))
            }
        }
    }

    private fun showDownload() {
        try {
            val i = Intent()
            i.action = DownloadManager.ACTION_VIEW_DOWNLOADS
            startActivity(i)
        } catch (exception: ActivityNotFoundException) {
            dialogUtils.showAlertDialog(this@ContactHistoryActivity, getString(R.string.toast_error_no_installed_ativity))
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun setMessageOnClipboard(message: Message) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.message_clipboard_title), message.message)
        clipboard.primaryClip = clip
        if (!TextUtils.isEmpty(message.attachmentName)) {
            downloadAttachment(message)
            dialogUtils.toast(R.string.message_copied_attachment_toast)
        } else {
            dialogUtils.toast(R.string.message_copied_toast)
        }
    }

    private fun sendNewMessage() {
        if (contact != null) {
            val contactName = if (contact!!.isBuying) contact!!.seller.username else contact!!.buyer.username
            startActivityForResult(MessageActivity.createStartIntent(this@ContactHistoryActivity, contactId, contactName!!), MessageActivity.REQUEST_MESSAGE_CODE)
        }
    }

    private fun showProfile() {
        if (contact != null) {
            try {
                val url = "https://localbitcoins.com/accounts/profile/" + (if (contact!!.isBuying) contact!!.seller.username else contact!!.buyer.username) + "/"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (e: SecurityException) {
                dialogUtils.showAlertDialog(this@ContactHistoryActivity, getString(R.string.error_hijack_link) + e.message)
            } catch (e: ActivityNotFoundException) {
                dialogUtils.showAlertDialog(this@ContactHistoryActivity, getString(R.string.toast_error_no_installed_ativity))
            }
        }
    }

    private fun showAdvertisement() {
        if (contact != null && contact!!.advertisement.id != null) {
            disposable.add(viewModel.getAdvertisement(contact!!.advertisement.id!!)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe( { advertisement ->
                        Timber.d("adverisement: $advertisement")
                        if(advertisement == null) {
                            launchAdvertisementLink(contact)
                        } else {
                            loadAdvertisementView(contact)
                        }
                    }, { error ->
                        Timber.e("Advertisement error: $error")
                        launchAdvertisementLink(contact)
                    }))
        }
    }

    private fun loadAdvertisementView(contact: Contact?) {
        if (contact != null) {
            if (contact.advertisement.id == null) {
                dialogUtils.showAlertDialog(this@ContactHistoryActivity, getString(R.string.error_no_advertisement), DialogInterface.OnClickListener { dialog, which ->

                })
            } else {
                val intent = AdvertisementActivity.createStartIntent(this@ContactHistoryActivity, contact.advertisement.id!!)
                startActivity(intent)
            }
        }
    }

    private fun launchAdvertisementLink(contact: Contact?) {
        if (contact != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contact.actions.advertisementPublicView))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                dialogUtils.showAlertDialog(this@ContactHistoryActivity, getString(R.string.toast_error_no_installed_ativity))
            }
        }
    }

    private fun setTitle(contact: Contact?) {
        if (contact != null) {
            val tradeType = TradeType.valueOf(contact.advertisement.tradeType)
            val title = when (tradeType) {
                TradeType.ONLINE_BUY, TradeType.ONLINE_SELL -> if (contact.isBuying) getString(R.string.text_buying_online) else getString(R.string.text_selling_online)
                else -> getString(R.string.text_trade)
            }
            if (supportActionBar != null) {
                supportActionBar!!.title = title
            }
        }
    }

    companion object {
        const val EXTRA_ID = "com.thanksmister.extras.EXTRA_ID"
        fun createStartIntent(context: Context, contactId: Int): Intent {
            val intent = Intent(context, ContactHistoryActivity::class.java)
            intent.putExtra(EXTRA_ID, contactId)
            return intent
        }
    }
}