package com.afollestad.materialcamera.internal;

import android.os.Handler;

import com.devbrackets.android.exomedia.EMAudioPlayer;
import com.devbrackets.android.exomedia.EMVideoView;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class ProgressHandler {

    public interface ProgressCallback {
        void onProgressUpdate(long position, long duration);
    }

    private final static int UPDATE_INTERVAL = 100;

    private Handler mHandler;
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            long position = 0;
            long duration = 0;

            if (mVideoView != null) {
                position = mVideoView.getCurrentPosition();
                duration = mVideoView.getDuration();
            } else if (mAudioPlayer != null) {
                position = mAudioPlayer.getCurrentPosition();
                duration = mAudioPlayer.getDuration();
            }

            if (position > duration)
                position = duration;

            if (mCallback != null)
                mCallback.onProgressUpdate(position, duration);
            if (mHandler != null)
                mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    private ProgressCallback mCallback;
    private EMVideoView mVideoView;
    private EMAudioPlayer mAudioPlayer;

    ProgressHandler(ProgressCallback callback) {
        mHandler = new Handler();
        mCallback = callback;
    }

    public ProgressHandler(EMVideoView videoView, ProgressCallback callback) {
        this(callback);
        mVideoView = videoView;
    }

    public ProgressHandler(EMAudioPlayer audioPlayer, ProgressCallback callback) {
        this(callback);
        mAudioPlayer = audioPlayer;
    }

    public void start() {
        stop();
        mHandler.post(mUpdateRunnable);
    }

    public void stop() {
        if (mHandler != null)
            mHandler.removeCallbacks(mUpdateRunnable);
    }

    public void dispose() {
        stop();
        mVideoView = null;
        mAudioPlayer = null;
        mHandler = null;
        mUpdateRunnable = null;
    }
}
