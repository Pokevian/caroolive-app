package com.pokevian.app.smartfleet.ui.setup;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.SignOutActivity;
import com.pokevian.app.smartfleet.ui.common.WithdrawalActivity;
import com.pokevian.app.smartfleet.ui.setup.AccountInfoFragment.UpdateAccountCallback;

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-08-05.
 */
public class AccountInfoActivity extends BaseActivity implements UpdateAccountCallback, AlertDialogCallbacks {

    public static final String TAG = "AccountInfo";
    private static final int REQUEST_SIGN_OUT = 5;
    private static final int REQUEST_WITHDRAW = 6;
    public static final String EXTRA_GOTO_INTRO = "extra.GOTO-INTRO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fragment);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(false);
        bar.setDisplayShowTitleEnabled(false);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(AccountInfoFragment.TAG);
        if (fragment == null) {
            fragment = AccountInfoFragment.newInstance();
            fm.beginTransaction().replace(R.id.container, fragment, AccountInfoFragment.TAG)
                    .commit();
        }
    }

    @Override
    public void onUpdate(Account newAccount) {
        Logger.getLogger(TAG).trace("onUpdate" + newAccount.getNickName());

        SettingsStore settingsStore = SettingsStore.getInstance();
        settingsStore.storeAccountName(newAccount.getNickName());

        settingsStore.storeAccount(newAccount);

        new AlertDialogFragment().newInstance(getString(R.string.notice), getString(R.string.message_update_success), null, getString(R.string.btn_ok))
                .show(getSupportFragmentManager(), "UPDATE");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.action_sign_out == id) {
            LogoutWarningDialogFragment.newInstance().show(getSupportFragmentManager(), LogoutWarningDialogFragment.TAG);
            return true;
        } else if (R.id.action_withdrawal == id) {
            WithdrawalWarningDialogFragment.newInstance().show(getSupportFragmentManager(), WithdrawalWarningDialogFragment.TAG);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGN_OUT) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_GOTO_INTRO, true);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this, R.string.main_failed_to_sign_out, Toast.LENGTH_LONG).show();
            }
        } else if (REQUEST_WITHDRAW == requestCode) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.message_withdrawal_success, Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.putExtra(EXTRA_GOTO_INTRO, true);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this, R.string.message_withdrawal_fail, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        if ("UPDATE".equals(fragment.getTag())) {
            finish();
        }
    }

    private void startLogOutActivity() {
        Intent intent = new Intent(this, SignOutActivity.class);
        startActivityForResult(intent, REQUEST_SIGN_OUT);
    }

    private void startWithdrawalActivity() {
        Intent intent = new Intent(this, WithdrawalActivity.class);
        startActivityForResult(intent, REQUEST_WITHDRAW);
    }


    public static class LogoutWarningDialogFragment extends DialogFragment {
        public static final String TAG = "LOGOUT-WARNING";

        public static LogoutWarningDialogFragment newInstance() {
            LogoutWarningDialogFragment fragment = new LogoutWarningDialogFragment();
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()/*, R.style.AppTheme_Light_Dialog*/)
                    .setTitle(R.string.action_logout)
                    .setMessage(R.string.dialog_message_logout)
                    .setNegativeButton(R.string.btn_no, null)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((AccountInfoActivity) getActivity()).startLogOutActivity();
                        }
                    })
                    .create();
        }

    }

    public static class WithdrawalWarningDialogFragment extends DialogFragment {
        public static final String TAG = "WITHDRAW-WARNING";

        public static WithdrawalWarningDialogFragment newInstance() {
            WithdrawalWarningDialogFragment fragment = new WithdrawalWarningDialogFragment();
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()/*, R.style.AppTheme_Light_Dialog*/)
                    .setTitle(R.string.action_withdrawal)
                    .setMessage(R.string.dialog_message_withdrawal)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_withdraw, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((AccountInfoActivity) getActivity()).startWithdrawalActivity();
                        }
                    })
                    .create();
        }

    }

}
