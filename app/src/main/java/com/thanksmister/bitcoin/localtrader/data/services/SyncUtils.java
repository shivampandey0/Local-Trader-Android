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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.thanksmister.bitcoin.localtrader.constants.Constants;


/**
 * Static helper methods for working with the sync framework.
 */
public class SyncUtils
{
    private static final long SYNC_FREQUENCY = 5 * 60;  // 5 minutes in seconds
    private static final String ACCOUNT_NAME = "Contacts";
    private static final String PREF_SETUP_COMPLETE = "setup_complete";
  
 
    /**
     * Create an entry for this application in the system account list, if it isn't already there.
     *
     * @param context Context
     * @return syncing Boolean
     */
    public static boolean CreateSyncAccount(Context context, String accountName)
    {
        boolean newAccount = false;
        boolean setupComplete = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        //Account account = AuthenticatorService.GetAccount();
        //AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        if(accountName == null || accountName.equals(""))
            accountName = ACCOUNT_NAME;

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = AuthenticatorService.GetAccount(accountName);
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, Constants.AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account,  Constants.AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account,  Constants.AUTHORITY, new Bundle(), SYNC_FREQUENCY);
            newAccount = true;
        }

        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (newAccount || !setupComplete) {
            TriggerRefresh(context, accountName);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_SETUP_COMPLETE, true).commit();
            return true;
        }

        return false;
    }

    @Deprecated
    public static void ClearSyncAccount(Context context)
    {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(PREF_SETUP_COMPLETE).commit();
    }

    public static void ClearSyncAccount(Context context, String accountName)
    {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(PREF_SETUP_COMPLETE).commit();
        Account account = AuthenticatorService.GetAccount(accountName);
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        accountManager.removeAccount(account, null, null);
        //ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 0);
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh(Context context, String accountName)
    {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(AuthenticatorService.GetAccount(accountName), Constants.AUTHORITY, b);  
    }
}
