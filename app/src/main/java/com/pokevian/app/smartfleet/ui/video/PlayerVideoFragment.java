/*
 * Copyright (c) 2014. Pokevian Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pokevian.app.smartfleet.ui.video;

import android.app.Fragment;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.pokevian.app.smartfleet.R;


public class PlayerVideoFragment extends Fragment implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final String ARG_POSITION = "position";

    private View mFragmentView;
    private VideoView mVideoView;

    private String mVideoPath;
    private int mStartPos;

    private OnErrorListener mOnErrorListener;
    private OnCompletionListener mOnCompletionListener;
    private OnPreparedListener mOnPreparedListener;

    public static Fragment newInstance(int position) {
        PlayerVideoFragment frag = new PlayerVideoFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mStartPos = args.getInt(ARG_POSITION, 0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragmentView = inflater.inflate(R.layout.fragment_player_video, container, false);
//        mFragmentView.setId(222);
        mVideoView = (VideoView) mFragmentView.findViewById(R.id.video);
        if (mVideoView != null) {
            mVideoView.setOnErrorListener(this);
            mVideoView.setOnPreparedListener(this);
            mVideoView.setOnCompletionListener(this);
        }

        return mFragmentView;
    }

    public void setStartPos(int startPos) {
        mStartPos = startPos;
    }

    public int getCurrentPosition() {
        int pos = -1;
        if (mVideoView != null) {
            try {
                pos = mVideoView.getCurrentPosition();
                if (pos == 0 && mStartPos != 0) {
                    pos = mStartPos;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return pos;
    }

    public void seekToVideo(int pos) {
        if (mVideoView != null) {
            mVideoView.seekTo(pos);
        }
    }

    public void stopVideo() {
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
    }

    public void pauseVideo() {
        if (mVideoView != null) {
            mVideoView.pause();
        }
    }

    public void startVideo() {
        if (mVideoView != null) {
            mVideoView.start();
        }
    }

    public void setVideoPath(String path) {
        if (mVideoView != null) {
            mVideoPath = path;
            mVideoView.setVideoPath(path);
        }
    }

    public int getVideoDuration() {
        int duration = 0;
        if (mVideoView != null) {
            duration = mVideoView.getDuration();
        }
        return duration;
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return mFragmentView.getLayoutParams();
    }

    public void setVideoErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setVideoCompleteListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setVideoPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(mp, what, extra);
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(mp);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(mp);
        }
    }

}
