package com.afollestad.materialcamera.internal;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialcamera.R;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDTintHelper;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.EMVideoView;

/**
 * @author Aidan Follestad (afollestad)
 */
public class PlaybackVideoFragment extends Fragment implements CameraUriInterface,
        ProgressHandler.ProgressCallback, View.OnClickListener, OnPreparedListener, OnErrorListener,
        OnCompletionListener, OnBufferUpdateListener {

    private TextView mPosition;
    private SeekBar mPositionSeek;
    private TextView mDuration;
    private ImageButton mPlayPause;
    private View mRetry;
    private View mUseVideo;
    private View mControlsFrame;
    private EMVideoView mStreamer;
    private TextView mPlaybackContinueCountdownLabel;

    private String mOutputUri;
    private boolean mWasPlaying;
    private BaseCaptureInterface mInterface;
    private boolean mFinishedPlaying;

    private ProgressHandler mProgressHandler;

    private Handler mCountdownHandler;
    private final Runnable mCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlaybackContinueCountdownLabel != null && mPlaybackContinueCountdownLabel.getVisibility() == View.VISIBLE) {
                long diff = mInterface.getRecordingEnd() - System.currentTimeMillis();
                if (diff < 3 && mPlayPause != null) {
                    mRetry.setEnabled(false);
                    mPlayPause.setEnabled(false);
                    mUseVideo.setEnabled(false);
                }
                if (diff <= 0) {
                    useVideo();
                    return;
                }
                mPlaybackContinueCountdownLabel.setText(String.format("-%s", CameraUtil.getDurationString(diff)));
                if (mCountdownHandler != null)
                    mCountdownHandler.postDelayed(mCountdownRunnable, 200);
            }
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mInterface = (BaseCaptureInterface) activity;
    }

    public static PlaybackVideoFragment newInstance(String outputUri, boolean allowRetry, int primaryColor) {
        PlaybackVideoFragment fragment = new PlaybackVideoFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("output_uri", outputUri);
        args.putBoolean(CameraIntentKey.ALLOW_RETRY, allowRetry);
        args.putInt(CameraIntentKey.PRIMARY_COLOR, primaryColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mProgressHandler != null)
            mProgressHandler.dispose();
        if (mStreamer != null) {
            mStreamer.stopPlayback();
            mStreamer.release();
            mStreamer.reset();
            mStreamer = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.mcam_fragment_videoplayback, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPosition = (TextView) view.findViewById(R.id.position);
        mPositionSeek = (SeekBar) view.findViewById(R.id.positionSeek);
        mDuration = (TextView) view.findViewById(R.id.duration);
        mPlayPause = (ImageButton) view.findViewById(R.id.playPause);
        mRetry = view.findViewById(R.id.retry);
        mUseVideo = view.findViewById(R.id.useVideo);
        mControlsFrame = view.findViewById(R.id.controlsFrame);
        mStreamer = (EMVideoView) view.findViewById(R.id.playbackView);
        mPlaybackContinueCountdownLabel = (TextView) view.findViewById(R.id.playbackContinueCountdownLabel);

        view.findViewById(R.id.playbackFrame).setOnClickListener(this);
        mRetry.setOnClickListener(this);
        mPlayPause.setOnClickListener(this);
        mUseVideo.setOnClickListener(this);

        int primaryColor = CameraUtil.darkenColor(getArguments().getInt(CameraIntentKey.PRIMARY_COLOR));
        primaryColor = Color.argb((int) (255 * 0.75f), Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
        mControlsFrame.setBackgroundColor(primaryColor);

        mRetry.setVisibility(getArguments().getBoolean(CameraIntentKey.ALLOW_RETRY, true) ? View.VISIBLE : View.GONE);
        mPositionSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mFinishedPlaying) {
                        mFinishedPlaying = false;
                        mStreamer.restart();
                        mProgressHandler.start();
                    }
                    if (progress < seekBar.getMax())
                        mFinishedPlaying = false;
                    else if (progress >= seekBar.getMax())
                        mFinishedPlaying = true;
                    mStreamer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mWasPlaying = mStreamer.isPlaying();
                mStreamer.pause();
                mProgressHandler.stop();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mWasPlaying) {
                    mStreamer.start();
                    mProgressHandler.start();
                }
            }
        });
        MDTintHelper.setTint(mPositionSeek, Color.WHITE);
        mOutputUri = getArguments().getString("output_uri");

        if (mInterface.hasLengthLimit() && mInterface.shouldAutoSubmit() &&
                mInterface.continueTimerInPlayback()) {
            mPlaybackContinueCountdownLabel.setVisibility(View.VISIBLE);
            final long diff = mInterface.getRecordingEnd() - System.currentTimeMillis();
            mPlaybackContinueCountdownLabel.setText(String.format("-%s", CameraUtil.getDurationString(diff)));
            startCountdownTimer();
        } else {
            mPlaybackContinueCountdownLabel.setVisibility(View.GONE);
        }

        mProgressHandler = new ProgressHandler(mStreamer, this);

        mStreamer.setOnPreparedListener(this);
        mStreamer.setOnErrorListener(this);
        mStreamer.setOnCompletionListener(this);
        mStreamer.setVideoURI(Uri.parse(mOutputUri));
        mStreamer.setOnBufferUpdateListener(this);

        if (mStreamer.isPlaying())
            mPlayPause.setImageDrawable(VC.get(this, R.drawable.mcam_action_pause));
        else
            mPlayPause.setImageDrawable(VC.get(this, R.drawable.mcam_action_play));
    }

    private void startCountdownTimer() {
        if (mCountdownHandler == null)
            mCountdownHandler = new Handler();
        else mCountdownHandler.removeCallbacks(mCountdownRunnable);
        mCountdownHandler.post(mCountdownRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mCountdownHandler != null) {
            mCountdownHandler.removeCallbacks(mCountdownRunnable);
            mCountdownHandler = null;
        }
        mPosition = null;
        mPositionSeek = null;
        mDuration = null;
        mPlayPause = null;
        mRetry = null;
        mUseVideo = null;
        mControlsFrame = null;
        mStreamer = null;
        mPlaybackContinueCountdownLabel = null;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playbackFrame) {
            mControlsFrame.animate().cancel();
            final float targetAlpha = mControlsFrame.getAlpha() == 1f ? 0f :
                    mControlsFrame.getAlpha() == 0f ? 1f :
                            mControlsFrame.getAlpha() > 0.5f ? 0f : 1f;
            mControlsFrame.animate().alpha(targetAlpha).start();
        } else if (v.getId() == R.id.playPause) {
            if (mStreamer != null) {
                if (mStreamer.isPlaying()) {
                    ((ImageButton) v).setImageDrawable(VC.get(this, R.drawable.mcam_action_play));
                    mStreamer.pause();
                    mProgressHandler.stop();
                } else {
                    if (mFinishedPlaying)
                        mStreamer.restart();
                    mFinishedPlaying = false;
                    ((ImageButton) v).setImageDrawable(VC.get(this, R.drawable.mcam_action_pause));
                    mStreamer.start();
                    mProgressHandler.start();
                }
            }
        } else if (v.getId() == R.id.retry) {
            mInterface.onRetry(mOutputUri);
        } else if (v.getId() == R.id.useVideo) {
            useVideo();
        }
    }

    private void useVideo() {
        if (mStreamer != null) {
            mStreamer.stopPlayback();
            mStreamer.release();
            mStreamer.reset();
            mStreamer = null;
        }
        if (mInterface != null)
            mInterface.useVideo(mOutputUri);
    }

    @Override
    public String getOutputUri() {
        return getArguments().getString("output_uri");
    }

    @Override
    public void onPrepared() {
        final int durationMs = mStreamer.getDuration();
        mPositionSeek.setMax(durationMs);
        mDuration.setText(String.format("-%s", CameraUtil.getDurationString(durationMs)));
        mPlayPause.setEnabled(true);
        mRetry.setEnabled(true);
        mUseVideo.setEnabled(true);
    }

    @Override
    public boolean onError() {
//        if (what == -38) {
//            // Error code -38 happens on some Samsung devices
//            // Just ignore it
//            return false;
//        }
//        String errorMsg = "Preparation/playback error: ";
//        switch (what) {
//            case MediaPlayer.MEDIA_ERROR_IO:
//                errorMsg += "I/O error";
//                break;
//            case MediaPlayer.MEDIA_ERROR_MALFORMED:
//                errorMsg += "Malformed";
//                break;
//            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
//                errorMsg += "Not valid for progressive playback";
//                break;
//            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
//                errorMsg += "Server died";
//                break;
//            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
//                errorMsg += "Timed out";
//                break;
//            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
//                errorMsg += "Unsupported";
//                break;
//        }
        new MaterialDialog.Builder(getActivity())
                .title("Playback Error")
                .content("A playback error has occurred.")
                .positiveText(android.R.string.ok)
                .show();
        return false;
    }

    @Override
    public void onCompletion() {
        mFinishedPlaying = true;
        if (mPlayPause != null)
            mPlayPause.setImageDrawable(VC.get(this, R.drawable.mcam_action_play));
        if (mPositionSeek != null) {
            mPositionSeek.setProgress(mStreamer.getDuration());
            mPosition.setText(CameraUtil.getDurationString(mStreamer.getDuration()));
        }

        if (mControlsFrame.getAlpha() < 1f) {
            mControlsFrame.animate().cancel();
            mControlsFrame.animate().alpha(1f).start();
        }
    }

    @Override
    public void onProgressUpdate(int position, int duration) {
        if (position == duration)
            mFinishedPlaying = true;
        try {
            if (mPosition != null)
                mPosition.setText(CameraUtil.getDurationString(position));
            if (mPositionSeek != null)
                mPositionSeek.setProgress(position);
            if (mDuration != null)
                mDuration.setText(String.format("-%s", CameraUtil.getDurationString(duration - position)));
        } catch (Throwable t) {
            if (mPosition != null)
                mPosition.setText(CameraUtil.getDurationString(0));
            if (mPositionSeek != null)
                mPositionSeek.setProgress(mPositionSeek.getMax());
        }
    }

    @Override
    public void onBufferingUpdate(@IntRange(from = 0L, to = 100L) int percent) {
        if (percent < 100) {
            if (mPositionSeek != null)
                mPositionSeek.setSecondaryProgress(percent);
        } else {
            mPositionSeek.setSecondaryProgress(0);
        }
    }
}