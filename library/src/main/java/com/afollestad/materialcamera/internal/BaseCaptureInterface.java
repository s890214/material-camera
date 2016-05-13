package com.afollestad.materialcamera.internal;

import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface BaseCaptureInterface {

    void onRetry(@Nullable String outputUri);

    void onShowPreview(@Nullable String outputUri, boolean countdownIsAtZero);

    void setRecordingStart(long start);

    void setRecordingEnd(long end);

    long getRecordingStart();

    long getRecordingEnd();

    boolean hasLengthLimit();

    boolean countdownImmediately();

    long getLengthLimit();

    void setCameraPosition(int position);

    void toggleCameraPosition();

    Object getCurrentCameraId();

    @BaseCaptureActivity.CameraPosition
    int getCurrentCameraPosition();

    void setFrontCamera(Object id);

    void setBackCamera(Object id);

    Object getFrontCamera();

    Object getBackCamera();

    void useVideo(String uri);

    boolean shouldAutoSubmit();

    boolean allowRetry();

    void setDidRecord(boolean didRecord);

    boolean didRecord();

    boolean restartTimerOnRetry();

    boolean continueTimerInPlayback();

    int videoEncodingBitRate(int defaultVal);

    int audioEncodingBitRate(int defaultVal);

    int videoFrameRate(int defaultVal);

    int videoPreferredHeight();

    float videoPreferredAspect();

    long maxAllowedFileSize();

    int qualityProfile();

    @DrawableRes
    int iconRecord();

    @DrawableRes
    int iconStop();

    @DrawableRes
    int iconFrontCamera();

    @DrawableRes
    int iconRearCamera();

    @DrawableRes
    int iconPlay();

    @DrawableRes
    int iconPause();

    @DrawableRes
    int iconRestart();

    @StringRes
    int labelRetry();

    @StringRes
    int labelUseVideo();
}