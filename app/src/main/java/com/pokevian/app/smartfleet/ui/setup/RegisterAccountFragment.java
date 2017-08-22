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

package com.pokevian.app.smartfleet.ui.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.model.Code;
import com.pokevian.app.smartfleet.model.CodeList;
import com.pokevian.app.smartfleet.request.SignUpRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.ui.setup.LoadCountryCodeDialogFragment.LoadCountryCodeCallbacks;
import com.pokevian.app.smartfleet.ui.setup.LoadRegionCodeDialogFragment.LoadRegionCodeCallbacks;
import com.pokevian.app.smartfleet.ui.tripmonitor.Settings;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;
import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.caroo.common.model.code.JoinRouteCd;
import com.pokevian.caroo.common.model.code.LocalKR;
import com.pokevian.caroo.common.model.code.SexCd;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class RegisterAccountFragment extends Fragment implements OnClickListener, OnItemClickListener {

    public static final String TAG = "RegisterAccountFragment";
    final Logger logger = Logger.getLogger(TAG);

    private AuthTarget mAuthTarget;
    private String mAccountId;
    private String mLoginId;

    private ListView mList;
    private ListAdapter mAdapter;

    private Button mPrevBtn;
    private Button mNextBtn;

    private RegisterAccountCallbacks mCallbacks;

    public static RegisterAccountFragment newInstance(AuthTarget authTarget, String accountId, String loginId) {
        RegisterAccountFragment fragment = new RegisterAccountFragment();
        Bundle args = new Bundle();
        args.putSerializable("auth_target", authTarget);
        args.putString("account_id", accountId);
        args.putString("login_id", loginId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (RegisterAccountCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement RegisterAccountCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (RegisterAccountCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement RegisterAccountCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null) {
                mAuthTarget = (AuthTarget) args.getSerializable("auth_target");
                mAccountId = args.getString("account_id");
                mLoginId = args.getString("login_id");
            }
        } else {
            mAuthTarget = (AuthTarget) savedInstanceState.getSerializable("auth_target");
            mAccountId = savedInstanceState.getString("account_id");
            mLoginId = savedInstanceState.getString("login_id");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("auth_target", mAuthTarget);
        outState.putString("account_id", mAccountId);
        outState.putString("login_id", mLoginId);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_account, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mList = (ListView) view.findViewById(R.id.list);

        ArrayList<AccountItem> items = new ArrayList<AccountItem>();
        for (int i = 0; i < ITEM_COUNT; i++) {
            items.add(new AccountItem());
        }


        mAdapter = new ListAdapter(getActivity(), items);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        AccountItem item = mAdapter.getItem(ITEM_EMAIL);
        item.value = mLoginId;

        mPrevBtn = (Button) view.findViewById(R.id.prev_btn);
        mPrevBtn.setText(R.string.btn_previous);
        mPrevBtn.setOnClickListener(this);

        mNextBtn = (Button) view.findViewById(R.id.next_btn);
        mNextBtn.setText(R.string.btn_next);
        mNextBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.prev_btn) {
            getActivity().onBackPressed();
        } else if (id == R.id.next_btn) {
            if (validate()) {
                Account account = new Account();
                account.setAuthTarget(mAuthTarget);
                account.setAccountId(mAccountId);
                account.setLoginId(mLoginId);
                account.setJoinCode(authTargetToCode(mAuthTarget));
                account.setActiveCode(TwoState.Y.name());

                AccountItem item = mAdapter.getItem(ITEM_NICK_NAME);
                account.setNickName(item.value);

                item = mAdapter.getItem(ITEM_BIRTHDAY);
                account.setBirthday(item.date);

//                item = mAdapter.getItem(ITEM_COUNTRY);
//                account.setCountryCode(item.code);

                item = mAdapter.getItem(ITEM_REGION);
                account.setRegionCode(item.code);

                item = mAdapter.getItem(ITEM_SEX);
                account.setSexCode(item.code);

                // Register account!
                DialogFragment fragment = RegisterAccountProcessDialogFragment.newInstance(account);
                fragment.show(getChildFragmentManager(), RegisterAccountProcessDialogFragment.TAG);
            }
        }
    }

    private String authTargetToCode(AuthTarget authTarget) {
        switch (authTarget) {
            default:
            case GOOGLE:
                return JoinRouteCd.google.name();
            case FACEBOOK:
                return JoinRouteCd.facebook.name();
        }
    }

    private String getRegionName(String code) {
        if (LocalKR.seoul.name().equals(code)) {
            return getString(R.string.account_region_seoul);
        }
        if (LocalKR.daejeon.name().equals(code)) {
            return getString(R.string.account_region_daejeon);
        }
        if (LocalKR.gwangju.name().equals(code)) {
            return getString(R.string.account_region_gwangju);
        }
        if (LocalKR.busan.name().equals(code)) {
            return getString(R.string.account_region_busan);
        }
        if (LocalKR.ulsan.name().equals(code)) {
            return getString(R.string.account_region_ulsan);
        }
        if (LocalKR.daegu.name().equals(code)) {
            return getString(R.string.account_region_daegu);
        }
        if (LocalKR.incheon.name().equals(code)) {
            return getString(R.string.account_region_incheon);
        }
        if (LocalKR.gyeonggi.name().equals(code)) {
            return getString(R.string.account_region_gyeonggi);
        }
        if (LocalKR.chungnam.name().equals(code)) {
            return getString(R.string.account_region_chungnam);
        }
        if (LocalKR.chungbuk.name().equals(code)) {
            return getString(R.string.account_region_chungbuk);
        }
        if (LocalKR.jeonnam.name().equals(code)) {
            return getString(R.string.account_region_jeonnam);
        }
        if (LocalKR.jeonbuk.name().equals(code)) {
            return getString(R.string.account_region_jeonbuk);
        }
        if (LocalKR.gyeongnam.name().equals(code)) {
            return getString(R.string.account_region_gyeongnam);
        }
        if (LocalKR.gyeongbuk.name().equals(code)) {
            return getString(R.string.account_region_gyeongbuk);
        }
        if (LocalKR.gangwon.name().equals(code)) {
            return getString(R.string.account_region_gangwon);
        }
        if (LocalKR.jeju.name().equals(code)) {
            return getString(R.string.account_region_jeju);
        }

        return null;
    }

    private boolean validate() {
        AccountItem item = mAdapter.getItem(ITEM_NICK_NAME);
        if (TextUtils.isEmpty(item.value)) {
            Toast.makeText(getActivity(), R.string.sign_up_input_nick_name, Toast.LENGTH_LONG).show();
            return false;
        }
        item = mAdapter.getItem(ITEM_BIRTHDAY);
        if (TextUtils.isEmpty(item.value)) {
            Toast.makeText(getActivity(), R.string.sign_up_pick_birthday, Toast.LENGTH_LONG).show();
            return false;
        }
//        item = mAdapter.getItem(ITEM_COUNTRY);
//        if (TextUtils.isEmpty(item.value)) {
//            Toast.makeText(getActivity(), R.string.sign_up_pick_country, Toast.LENGTH_LONG).show();
//            return false;
//        }
        item = mAdapter.getItem(ITEM_REGION);
        if (TextUtils.isEmpty(item.value)) {
            Toast.makeText(getActivity(), R.string.sign_up_pick_region, Toast.LENGTH_LONG).show();
            return false;
        }
        item = mAdapter.getItem(ITEM_SEX);
        if (TextUtils.isEmpty(item.value)) {
            Toast.makeText(getActivity(), R.string.sign_up_pick_sex, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == ITEM_NICK_NAME) {
            String nickName = mAdapter.getItem(ITEM_NICK_NAME).value;
            DialogFragment fragment = InputNickNameDialogFragment.newInstance(nickName);
            fragment.show(getChildFragmentManager(), InputNickNameDialogFragment.TAG);
        } else if (position == ITEM_BIRTHDAY) {
            Date birthday = mAdapter.getItem(ITEM_BIRTHDAY).date;
            DialogFragment fragment = PickBirthdayDialogFragment.newInstance(birthday);
            fragment.show(getChildFragmentManager(), PickBirthdayDialogFragment.TAG);
        } /*else if (position == ITEM_COUNTRY) {
            String countryCode = mAdapter.getItem(ITEM_COUNTRY).code;
            DialogFragment fragment = PickCountryDialogFragment.newInstance(countryCode);
            fragment.show(getChildFragmentManager(), PickCountryDialogFragment.TAG);
        }*/ else if (position == ITEM_REGION) {
            /*String countryCode = mAdapter.getItem(ITEM_COUNTRY).code;
            if (TextUtils.isEmpty(countryCode)) {
                Toast.makeText(getActivity(), R.string.sign_up_pick_country_first, Toast.LENGTH_LONG).show();
            } else*/ {
                String regionCode = mAdapter.getItem(ITEM_REGION).code;
                DialogFragment fragment = PickRegionDialogFragment.newInstance(regionCode, /*countryCode*/"KR");
                fragment.show(getChildFragmentManager(), PickRegionDialogFragment.TAG);
            }
        } else if (position == ITEM_SEX) {
            String sexCode = mAdapter.getItem(ITEM_SEX).code;
            DialogFragment fragment = PickSexDialogFragment.newInstance(sexCode);
            fragment.show(getChildFragmentManager(), PickSexDialogFragment.TAG);
        }
    }

    public void onNickNameInput(DialogFragment fragment, String nickName) {
        logger.debug("onNickNameInput(): nickName=" + nickName);

        AccountItem item = mAdapter.getItem(ITEM_NICK_NAME);
        item.value = nickName;
        mAdapter.notifyDataSetInvalidated();
    }

    public void onBirthdaySelect(DialogFragment fragment, Date birthday) {
        logger.debug("onBirthdaySelect(): birthday=" + birthday);

        AccountItem item = mAdapter.getItem(ITEM_BIRTHDAY);
        item.date = birthday;
        item.value = DateFormat.getDateFormat(getActivity()).format(birthday);
        mAdapter.notifyDataSetInvalidated();
    }

    public void onCountrySelect(DialogFragment fragment, Code countryCode) {
        logger.debug("onCountrySelect(): countryCode=" + countryCode);

//        AccountItem item = mAdapter.getItem(ITEM_COUNTRY);
//        item.code = countryCode.getCode();
//        item.value = countryCode.getName();

        // Invalidate region
//        item = mAdapter.getItem(ITEM_REGION);
//        item.code = null;
//        item.value = null;

        mAdapter.notifyDataSetInvalidated();
    }

    public void onRegionSelect(DialogFragment fragment, Code regionCode) {
        logger.debug("onRegionSelect(): regionCode=" + regionCode);

        AccountItem item = mAdapter.getItem(ITEM_REGION);
        item.code = regionCode.getCode();
        item.value = regionCode.getName();
        mAdapter.notifyDataSetInvalidated();
    }

    public void onSexSelect(DialogFragment fragment, Code sexCode) {
        logger.debug("onSexSelect(): sexCod=" + sexCode);

        AccountItem item = mAdapter.getItem(ITEM_SEX);
        item.code = sexCode.getCode();
        item.value = sexCode.getName();
        mAdapter.notifyDataSetInvalidated();
    }

    public void onSignUpSuccess(DialogFragment fragment, Account account) {
        logger.debug("onSignUpSuccess(): account=" + account);

        mCallbacks.onNewAccountRegistered(account);
    }

    public void onSignUpCancel(DialogFragment fragment, Account account) {
        logger.debug("onSignUpCancel()");
    }

    private static final int ITEM_NICK_NAME = 0;
    private static final int ITEM_EMAIL = 1;
    private static final int ITEM_BIRTHDAY = 2;
//    private static final int ITEM_COUNTRY = 2;
    private static final int ITEM_REGION = 3;
    private static final int ITEM_SEX = 4;
    private static final int ITEM_COUNT = 5;

    class ListAdapter extends ArrayAdapter<AccountItem> {

        private final LayoutInflater mInflater;

        ListAdapter(Context context, ArrayList<AccountItem> items) {
            super(context, 0, items);

            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.account_info_list_item, null);
            }

            AccountItem item = getItem(position);

            TextView nameText = (TextView) convertView.findViewById(R.id.name_text);
            nameText.setText(getItemNameRes(position));

            TextView valueText = (TextView) convertView.findViewById(R.id.value_text);
            if (TextUtils.isEmpty(item.value)) {
                valueText.setText(getEmptyValueRes(position));
            } else {
                valueText.setText(item.value);
            }

            if (ITEM_EMAIL == position) {
//                convertView.setVisibility(View.INVISIBLE);
                valueText.setEnabled(false);
            }

            return convertView;
        }

        private int getItemNameRes(int position) {
            switch (position) {
                default:
                case ITEM_NICK_NAME:
                    return R.string.sign_up_nick_name;
                case ITEM_EMAIL:
                    return R.string.sign_up_email;
                case ITEM_BIRTHDAY:
                    return R.string.sign_up_birthday;
//                case ITEM_COUNTRY:
//                    return R.string.sign_up_country;
                case ITEM_REGION:
                    return R.string.sign_up_region;
                case ITEM_SEX:
                    return R.string.sign_up_sex;
            }
        }

        private int getEmptyValueRes(int position) {
            switch (position) {
                case ITEM_NICK_NAME:
                    return R.string.sign_up_please_input;
                default:
                    return R.string.sign_up_please_select;
            }
        }

    }

    class AccountItem {
        String value;
        String code;
        Date date;
    }

    public interface RegisterAccountCallbacks {
        void onNewAccountRegistered(Account newAccount);
    }

    public static class InputNickNameDialogFragment extends DialogFragment {

        public static final String TAG = "input-nick-name-dialog";

        private String mNickName;
        private EditText mEdit;

        public static InputNickNameDialogFragment newInstance(String nickName) {
            InputNickNameDialogFragment fragment = new InputNickNameDialogFragment();
            Bundle args = new Bundle();
            args.putString("nick_name", nickName);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mNickName = args.getString("nick_name");
            } else {
                mNickName = savedInstanceState.getString("nick_name");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("nick_name", mNickName);
            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_text, null);

            mEdit = (EditText) view.findViewById(R.id.edit);
            mEdit.setText(mNickName);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.sign_up_nick_name)
                    .setView(view)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);

                            String nickName = mEdit.getText().toString().trim();
                            ((RegisterAccountFragment) getParentFragment())
                                    .onNickNameInput(InputNickNameDialogFragment.this, nickName);
                        }
                    })
                    .create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

    }

    public static class PickBirthdayDialogFragment extends DialogFragment implements OnDateSetListener {

        public static final String TAG = "pick-birthday-dialog";

        private Date mBirthday;

        public static PickBirthdayDialogFragment newInstance(Date birtyday) {
            PickBirthdayDialogFragment fragment = new PickBirthdayDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("birthday", birtyday);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mBirthday = (Date) args.getSerializable("birthday");
            } else {
                mBirthday = (Date) savedInstanceState.getSerializable("birthday");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable("birthday", mBirthday);

            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Calendar birthday = Calendar.getInstance();
            if (mBirthday != null) {
                birthday.setTime(mBirthday);
            } else {
                birthday.add(Calendar.YEAR, -30);
                birthday.set(Calendar.MONTH, Calendar.JANUARY);
                birthday.set(Calendar.DAY_OF_MONTH, 1);
            }
            int year = birthday.get(Calendar.YEAR);
            int month = birthday.get(Calendar.MONTH);
            int day = birthday.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
            return dialog;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            Calendar birthday = Calendar.getInstance();
            birthday.set(year, monthOfYear, dayOfMonth, 0, 0, 0);

            ((RegisterAccountFragment) getParentFragment()).onBirthdaySelect(this, birthday.getTime());
        }

    }

    public static class PickCountryDialogFragment extends DialogFragment implements LoadCountryCodeCallbacks {

        public static final String TAG = "pick-country-dialog";

        private String mCountryCode;
        private ListAdapter mAdapter;

        public static PickCountryDialogFragment newInstance(String countryCode) {
            PickCountryDialogFragment fragment = new PickCountryDialogFragment();
            Bundle args = new Bundle();
            args.putString("country_code", countryCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mCountryCode = args.getString("country_code");
            } else {
                mCountryCode = savedInstanceState.getString("country_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("country_code", mCountryCode);

            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mAdapter = new ListAdapter(getActivity(), new ArrayList<Code>());

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.sign_up_country)
                    .setSingleChoiceItems(mAdapter, 0, null)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int position = ((AlertDialog) getDialog()).getListView().getCheckedItemPosition();
                            Code code = mAdapter.getItem(position);
                            ((RegisterAccountFragment) getParentFragment())
                                    .onCountrySelect(PickCountryDialogFragment.this, code);
                        }
                    })
                    .create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            DialogFragment fragment = LoadCountryCodeDialogFragment.newInstance();
            fragment.show(getChildFragmentManager(), LoadCountryCodeDialogFragment.TAG);
        }

        @Override
        public void onLoadCountryCodeSuccess(CodeList codeList) {
            if (codeList != null) {
                mAdapter.clear();

                ArrayList<Code> codes = codeList.getList();
                mAdapter.addAll(codes);

                for (Code code : codeList.getList()) {
                    Logger.getLogger("resgister").debug(">>>" + code.getCode() + "#" + code.getName());
                }


                int size = codes.size();
                for (int i = 0; i < size; i++) {
                    Code code = codes.get(i);
                    if (code.getCode().equals(mCountryCode)) {
                        ((AlertDialog) getDialog()).getListView().setSelection(i);
                        ((AlertDialog) getDialog()).getListView().setItemChecked(i, true);
                        break;
                    }
                }

                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadCountryCodeFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        class ListAdapter extends ArrayAdapter<Code> {

            private LayoutInflater mInflater;

            ListAdapter(Context context, ArrayList<Code> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                Code item = getItem(position);

                CheckedTextView countryText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                countryText.setText(item.getName());

                return convertView;
            }

        }

    }

    public static class PickRegionDialogFragment extends DialogFragment implements LoadRegionCodeCallbacks {

        public static final String TAG = "pick-region-dialog";

        private String mRegionCode;
        private String mCountryCode;
        private ListAdapter mAdapter;

        public static PickRegionDialogFragment newInstance(String regionCode, String countryCode) {
            PickRegionDialogFragment fragment = new PickRegionDialogFragment();
            Bundle args = new Bundle();
            args.putString("region_code", regionCode);
            args.putString("country_code", countryCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mRegionCode = args.getString("region_code");
                mCountryCode = args.getString("country_code");
            } else {
                mRegionCode = savedInstanceState.getString("region_code");
                mCountryCode = savedInstanceState.getString("country_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("region_code", mRegionCode);
            outState.putString("country_code", mCountryCode);

            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mAdapter = new ListAdapter(getActivity(), new ArrayList<Code>());

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.sign_up_region)
                    .setSingleChoiceItems(mAdapter, 0, null)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int position = ((AlertDialog) getDialog()).getListView().getCheckedItemPosition();
                            Code code = mAdapter.getItem(position);
                            ((RegisterAccountFragment) getParentFragment())
                                    .onRegionSelect(PickRegionDialogFragment.this, code);
                        }
                    })
                    .create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            DialogFragment fragment = LoadRegionCodeDialogFragment.newInstance(mCountryCode);
            fragment.show(getChildFragmentManager(), LoadRegionCodeDialogFragment.TAG);
        }

        @Override
        public void onLoadRegionCodeSuccess(CodeList codeList) {
            if (codeList != null) {
                mAdapter.clear();

                ArrayList<Code> codes = codeList.getList();
                mAdapter.addAll(codes);

                for (Code code : codes) {
                    Logger.getLogger("register").debug(code.getCode() + "#" + code.getName());
                    Logger.getLogger("register").debug(LocalKR.busan.name() + "#" + LocalKR.seoul.fullName());
                }

                int size = codes.size();
                for (int i = 0; i < size; i++) {
                    Code code = codes.get(i);
                    if (code.getCode().equals(mRegionCode)) {
                        ((AlertDialog) getDialog()).getListView().setSelection(i);
                        ((AlertDialog) getDialog()).getListView().setItemChecked(i, true);
                        break;
                    }
                }

                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadRegionCodeFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        class ListAdapter extends ArrayAdapter<Code> {

            private LayoutInflater mInflater;

            ListAdapter(Context context, ArrayList<Code> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                Code item = getItem(position);

                CheckedTextView regionText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                regionText.setText(item.getName());

                return convertView;
            }

        }

    }

    public static class PickSexDialogFragment extends DialogFragment {

        public static final String TAG = "pick-sex-dialog";

        private String mSexCode;
        private ListAdapter mAdapter;

        public static PickSexDialogFragment newInstance(String sexCode) {
            PickSexDialogFragment fragment = new PickSexDialogFragment();
            Bundle args = new Bundle();
            args.putString("sex_code", sexCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mSexCode = args.getString("sex_code");
            } else {
                mSexCode = savedInstanceState.getString("sex_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("sex_code", mSexCode);

            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ArrayList<Code> items = new ArrayList<Code>();
            items.add(new Code(SexCd.male.name(), getString(R.string.sign_up_male)));
            items.add(new Code(SexCd.female.name(), getString(R.string.sign_up_female)));

            int checkedItem = 0;
            int size = items.size();
            for (int i = 0; i < size; i++) {
                Code code = items.get(i);
                if (code.getCode().equals(mSexCode)) {
                    checkedItem = i;
                    break;
                }
            }

            mAdapter = new ListAdapter(getActivity(), items);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.sign_up_sex)
                    .setSingleChoiceItems(mAdapter, checkedItem, null)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int position = ((AlertDialog) getDialog()).getListView().getCheckedItemPosition();
                            Code code = mAdapter.getItem(position);
                            ((RegisterAccountFragment) getParentFragment())
                                    .onSexSelect(PickSexDialogFragment.this, code);
                        }
                    })
                    .create();
        }

        class ListAdapter extends ArrayAdapter<Code> {

            private LayoutInflater mInflater;

            ListAdapter(Context context, ArrayList<Code> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                Code item = getItem(position);

                CheckedTextView sexText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                sexText.setText(item.getName());

                return convertView;
            }
        }
    }

    public static class RegisterAccountProcessDialogFragment extends DialogFragment implements AlertDialogCallbacks {

        public static final String TAG = "register-account-process-dialog";

        private Account mAccount;
        private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();

        public static RegisterAccountProcessDialogFragment newInstance(Account account) {
            RegisterAccountProcessDialogFragment fragment = new RegisterAccountProcessDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("account", account);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mAccount = (Account) args.getSerializable("account");
            } else {
                mAccount = (Account) savedInstanceState.getSerializable("account");
            }
        }

        @Override
        public void onDestroy() {
            mRequestQueue.cancelAll(TAG);

            super.onDestroy();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable("account", mAccount);

            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new WaitForDialog(getActivity());
            setCancelable(false);
            return dialog;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            SignUpRequest request = new SignUpRequest(mAccount, new SignUpListener());
            request.setTag(TAG);
            mRequestQueue.add(request);
        }

        @Override
        public void onDialogButtonClick(DialogFragment fragment, int which) {
            String tag = fragment.getTag();

            if ("sign-up-failure-dialog".equals(tag)) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    SignUpRequest request = new SignUpRequest(mAccount,
                            new SignUpListener());
                    request.setTag(TAG);
                    mRequestQueue.add(request);
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    ((RegisterAccountFragment) getParentFragment())
                            .onSignUpCancel(RegisterAccountProcessDialogFragment.this, mAccount);
                    dismiss();
                }
            }
        }

        private class SignUpListener extends VolleyListener<String> {

            @Override
            public void onResponse(String accountId) {
                mAccount.setAccountId(accountId);
                ((RegisterAccountFragment) getParentFragment())
                        .onSignUpSuccess(RegisterAccountProcessDialogFragment.this, mAccount);
                dismiss();
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof ServerError) {
                    FragmentManager fm = getChildFragmentManager();
                    DialogFragment fragment = AlertDialogFragment.newInstance(
                            getString(R.string.dialog_title_server_error),
                            getString(R.string.dialog_message_server_error),
                            getString(R.string.btn_no),
                            getString(R.string.btn_yes));
                    fm.beginTransaction().add(fragment, "sign-up-failure-dialog")
                            .commitAllowingStateLoss();
                } else {
                    FragmentManager fm = getChildFragmentManager();
                    DialogFragment fragment = AlertDialogFragment.newInstance(
                            getString(R.string.dialog_title_network_error),
                            getString(R.string.dialog_message_network_error),
                            getString(R.string.btn_no),
                            getString(R.string.btn_yes));
                    fm.beginTransaction().add(fragment, "sign-up-failure-dialog")
                            .commitAllowingStateLoss();
                }
            }

        }

    }

}
