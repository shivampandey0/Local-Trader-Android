/*
 * Copyright (c) 2015 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.data.services;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;

/**
 * A bound Service that instantiates the authenticator
 * when started.
 */
public class AuthenticatorService extends Service
{
  
    
    // Instance field that stores the authenticator object
    private Authenticator mAuthenticator;

    private static AccountAuthenticatorImpl sAccountAuthenticator = null;
    
    /*@Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new Authenticator(this);
    }*/

    @Override
    public void onCreate()
    {
        // Create a new authenticator object
        //mAuthenticator = new Authenticator(this);
        sAccountAuthenticator = new AccountAuthenticatorImpl(this);
    }

    /**
     * Obtain a handle to the {@link android.accounts.Account} used for sync in this application.
     *
     * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
     *         has been called)
     */
    public static Account GetAccount()
    {
        /*Account account = new Account(accountName, ACCOUNT_TYPE);
        AccountManager am = AccountManager.get(context);
        am.blockingGetAuthToken(account, )
        boolean accountCreated = am.addAccountExplicitly(account, password, null);

        Bundle extras = getIntent.getExtras();
        if (extras != null) {
            if (accountCreated) {  //Pass the new account back to the account manager
                AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.ACCOUNT_TYPE));
                response.onResult(result);
            }
            finish();
        }
        */
        // This string should *not* be localized. If the user switches locale, we would not be
        // able to locate the old account, and may erroneously register multiple accounts.
        return new Account(Constants.ACCOUNT_NAME, Constants.ACCOUNT_TYPE);
    }

    /**
     * Obtain a handle to the {@link android.accounts.Account} used for sync in this application.
     *
     * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
     *         has been called)
     */
    public static Account GetAccount(String accountName)
    {
        // This string should *not* be localized. If the user switches locale, we would not be
        // able to locate the old account, and may erroneously register multiple accounts.
        return new Account(accountName, Constants.ACCOUNT_TYPE);
    }
    
    private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator
    {
        private Context mContext;

        public AccountAuthenticatorImpl(Context context)
        {
            super(context);
            mContext = context;
        }

        /*
         *  The user has requested to add a new account to the system.  We return an intent that will launch our login screen if the user has not logged in yet,
         *  otherwise our activity will just pass the user's credentials on to the account manager.
         */
        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
                throws NetworkErrorException
        {
            Bundle reply = new Bundle();

            Toast.makeText(mContext, mContext.getString(R.string.warning_no_multiple_account_supports), Toast.LENGTH_SHORT).show();

            Intent i = new Intent(mContext, MainActivity.class);
            //i.setAction("fm.last.android.sync.LOGIN");
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            reply.putParcelable(AccountManager.KEY_INTENT, i);

            return reply;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
            return null;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
            return null;
        }
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    /*@Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }*/

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        return sAccountAuthenticator.getIBinder();
    }

    private AccountAuthenticatorImpl getAuthenticator()
    {
        if (sAccountAuthenticator == null)
            sAccountAuthenticator = new AccountAuthenticatorImpl(this);

        return sAccountAuthenticator;
    }
}