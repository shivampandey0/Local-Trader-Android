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

import androidx.lifecycle.*
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.network.api.model.Contact
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import com.thanksmister.bitcoin.localtrader.ui.activities.ContactActivity
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchActivity
import com.thanksmister.bitcoin.localtrader.ui.adapters.ContactsAdapter
import com.thanksmister.bitcoin.localtrader.ui.components.ItemClickSupport
import com.thanksmister.bitcoin.localtrader.ui.viewmodels.ContactsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_dashboard_items.*
import timber.log.Timber
import javax.inject.Inject

class ContactsFragment : BaseFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var viewModel: ContactsViewModel

    private val contactsAdapter: ContactsAdapter by lazy {
        ContactsAdapter(requireActivity(), object : ContactsAdapter.OnItemClickListener {
            override fun onSearchButtonClicked() {
                showSearchScreen()
            }
            override fun onAdvertiseButtonClicked() {
                createAdvertisementScreen()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contactsList.apply {
            setHasFixedSize(true)
            val linearLayoutManager = LinearLayoutManager(activity)
            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
            layoutManager = linearLayoutManager
            adapter = contactsAdapter
        }
        ItemClickSupport.addTo(contactsList).setOnItemClickListener { recyclerView, position, v ->
            showContact(contactsAdapter.getItemAt(position))
        }
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ContactsViewModel::class.java)
        observeViewModel(viewModel)
    }

    private fun setupList(items: List<Contact>) {
        contactsAdapter.replaceWith(items)
    }

    private fun observeViewModel(viewModel: ContactsViewModel) {
        viewModel.getAlertMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.showAlertDialog(requireActivity(), message)
            }
        })
        viewModel.getToastMessage().observe(this, Observer { message ->
            if (message != null && activity != null) {
                dialogUtils.toast(message)
            }
        })
        disposable.add(viewModel.getActiveContacts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { data ->
                    if(data != null && data.size > 0) {
                        setupList(data)
                    }
                }, { error ->
                    Timber.e(error.message)
                }))
    }

    private fun showContact(contact: Contact?) {
        if (contact != null && contact.contactId != 0 && activity != null) {
            val intent = ContactActivity.createStartIntent(requireActivity(), contact.contactId)
            startActivity(intent)
        } else {
            dialogUtils.toast(getString(R.string.toast_contact_not_exist))
        }
    }

    private fun createAdvertisementScreen() {
        if(activity != null && isAdded) {
            dialogUtils.showAlertDialog(requireActivity(), getString(R.string.dialog_edit_advertisements),
                    DialogInterface.OnClickListener { _, _ ->
                        try {
                            startActivity( Intent(Intent.ACTION_VIEW, Uri.parse(Constants.ADS_URL)));
                        } catch (ex: ActivityNotFoundException) {
                            Toast.makeText(requireActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show();
                        }
                    }, DialogInterface.OnClickListener { _, _ ->
                // na-da
            })
        }
    }

    private fun showSearchScreen() {
        if(activity != null && isAdded) {
            val intent = SearchActivity.createStartIntent(requireActivity())
            startActivity(intent)
        }
    }

    companion object {
        fun newInstance(): ContactsFragment {
            return ContactsFragment()
        }
    }
}