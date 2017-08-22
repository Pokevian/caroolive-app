package com.pokevian.app.smartfleet.ui.rank;

import android.support.v4.app.DialogFragment;

import com.pokevian.app.smartfleet.model.Rank;

/**
 * Created by ian on 2016-02-21.
 */
public interface GetRankingDialogCallback {
    void onSuccess(DialogFragment fragment, Rank rank);
    void onFailure(DialogFragment fragment);
}
