package com.thanksmister.bitcoin.localtrader.ui;

/**
 * Author: Michael Ritchie
 * Date: 3/26/15
 * Copyright 2013, ThanksMister LLC
 * 
 * https://github.com/rosshambrick/standardlib/blob/master/src/main/java/com/rosshambrick/standardlib/BlockingProgressFragment.java
 */

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.thanksmister.bitcoin.localtrader.R;

import rx.Subscription;

public class BlockingProgressFragment extends DialogFragment
{
    public static final String TAG = BlockingProgressFragment.class.getSimpleName();
    private Subscription subscription;

    public static BlockingProgressFragment newInstance()
    {
        return new BlockingProgressFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_blocking_progress, container, false);
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        super.onCancel(dialog);
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    public void setOnCancelListener(Subscription subscription)
    {
        this.subscription = subscription;
    }
}
