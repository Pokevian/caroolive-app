package com.pokevian.app.smartfleet.ui.rank;

import android.support.v4.app.DialogFragment;

import com.pokevian.app.smartfleet.model.ScoreRank;

import java.util.List;

/**
 * Created by ian on 2016-02-21.
 */
public interface GetRankingsDialogCallback {
    void onSuccess(DialogFragment fragment, List<ScoreRank> list);
    void onFailure(DialogFragment fragment);
}
