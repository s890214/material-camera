package com.afollestad.materialcamera.internal;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;

/**
 * @author Aidan Follestad (afollestad)
 */
class VC {

    public static Drawable get(@NonNull Fragment context, @DrawableRes int vectorRes) {
        return get(context.getActivity(), vectorRes);
    }

    public static Drawable get(@NonNull Context context, @DrawableRes int vectorRes) {
        return VectorDrawableCompat.create(context.getResources(), vectorRes, null);
    }

    private VC() {
    }
}