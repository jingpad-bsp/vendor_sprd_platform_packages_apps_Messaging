/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.messaging.ui.appsettings;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.core.app.NavUtils;
import androidx.core.text.BidiFormatter;
import androidx.core.text.TextDirectionHeuristicsCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.telephony.SubscriptionInfo;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.SettingsData;
import com.android.messaging.datamodel.data.SettingsData.SettingsDataListener;
import com.android.messaging.datamodel.data.SettingsData.SettingsItem;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Assert;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sprd.messaging.util.SystemAdapter;

/**
 * Shows the "master" settings activity that contains two parts, one for application-wide settings
 * (dubbed "General settings"), and one or more for per-subscription settings (dubbed "Messaging
 * settings" for single-SIM, and the actual SIM name for multi-SIM). Clicking on either item
 * (e.g. "General settings") will open the detail settings activity (ApplicationSettingsActivity
 * in this case).
 */
public class SettingsActivity extends BugleActionBarActivity {
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Directly open the detailed settings page as the top-level settings activity if this is
        // not a multi-SIM device.
        if(!OsUtil.hasPhonePermission()){
            Log.d("SettingsActivity",
                    "=======zhongjihao===onCreate=======");
            OsUtil.requestMissingPermission(this);
        }else{
            if (PhoneUtils.getDefault().getActiveSubscriptionCount() <= 1) {
                UIIntents.get().launchApplicationSettingsActivity(this, true /* topLevel */);
                finish();
            } else {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new SettingsFragment())
                        .commit();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends Fragment implements SettingsDataListener {
        private ListView mListView;
        private SettingsListAdapter mAdapter;
        private final Binding<SettingsData> mBinding = BindingBase.createBinding(this);

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mBinding.bind(DataModel.get().createSettingsData(getActivity(), this));
            mBinding.getData().init(getLoaderManager(), mBinding);
            getActivity().registerReceiver(mSimInOutReceiver, mSimFilter);
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                final Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.settings_fragment, container, false);
            mListView = (ListView) view.findViewById(android.R.id.list);
            mAdapter = new SettingsListAdapter(getActivity());
            mListView.setAdapter(mAdapter);
            return view;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mBinding.unbind();
            getActivity().unregisterReceiver(mSimInOutReceiver);
        }

        //add for bug 610115 start
        private IntentFilter mSimFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        private BroadcastReceiver mSimInOutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("ApplicationSettingsFragment", "receive sim state changed.");
                String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                Log.d("ApplicationSettingsFragment", "sim status is:" + simStatus);
                if (intent.getAction() == TelephonyIntents.ACTION_SIM_STATE_CHANGED){
                    if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simStatus)){
                        getActivity().finish();
                    }
                }
            }
        };
        //add for bug 610115 end

        @Override
        public void onSelfParticipantDataLoaded(SettingsData data) {
            mBinding.ensureBound(data);
            List<SettingsItem> dataset = data.getSettingsItems();
            if (dataset != null) {
                //TelephonyManager tm = (TelephonyManager) getActivity()//2016.8.3 android N update
                //        .getSystemService(Context.TELEPHONY_SERVICE); //2016.8.3 android N update
                int count = dataset.size();
                int phoneId_0 = 0;
                int phoneId_1 = 1;
                for (int i = 0; i < count; i++) {
                    SettingsItem item = dataset.get(i);
                    int phoneId = SystemAdapter.getInstance().getPhoneId(item.getSubId());//tm.getPhoneId(item.getSubId());//2016.8.3 android N update
					if (phoneId == 0) {
                        phoneId_0 = i;
                    } else if (phoneId == 1) {
                        phoneId_1 = i;
                    }
                }
                if (phoneId_1 < phoneId_0) {
                    SettingsItem item_0 = dataset.get(phoneId_0);
                    SettingsItem item_1 = dataset.get(phoneId_1);
                    dataset.set(phoneId_1, item_0);
                    dataset.set(phoneId_0, item_1);
                }

                if(!OsUtil.hasPhonePermission()){
                    Log.d("SettingsActivity",
                            "=======zhongjihao===onSelfParticipantDataLoaded=======");
                    OsUtil.requestMissingPermission(getActivity());
                }else{
                    List<SubscriptionInfo> availableSubInfoList = SystemAdapter.getInstance().getActiveSubInfoList();//SmsManager
                            //.getDefault().getActiveSubInfoList();
                    int actSubCount = 0;
                    if(null != availableSubInfoList) {
                        actSubCount = availableSubInfoList.size();
                    }
                    Log.d("SettingsActivity",
                            "========zhongjihao====onSelfParticipantDataLoaded=====active sub count: "
                                    + count);
                    if (actSubCount == 0) {
                        //modify for bug 610115 start
                        for (int i = dataset.size(); i > 1; i--) {
                            dataset.remove(i-1);
                        }
                        //modify for bug 610115 end
                    }
                    if (actSubCount == 1) {
                        Log.d("SettingsActivity",
                                "========zhongjihao====onSelfParticipantDataLoaded====active=getSubscriptionId: "
                                        + availableSubInfoList.get(0)
                                                .getSubscriptionId());
                        for (int n = 1; n < dataset.size(); n++) {
                            if (availableSubInfoList.get(0).getSubscriptionId() != dataset
                                    .get(n).getSubId())
                                dataset.remove(n);
                        }
                    }
                }
            }
            mAdapter.setSettingsItems(dataset);
        }

        /**
         * An adapter that displays a list of SettingsItem.
         */
        private class SettingsListAdapter extends ArrayAdapter<SettingsItem> {
            public SettingsListAdapter(final Context context) {
                super(context, R.layout.settings_item_view, new ArrayList<SettingsItem>());
            }

            public void setSettingsItems(final List<SettingsItem> newList) {
                clear();
                addAll(newList);
                notifyDataSetChanged();
            }

            @Override
            public View getView(final int position, final View convertView,
                    final ViewGroup parent) {
                View itemView;
                if (convertView != null) {
                    itemView = convertView;
                } else {
                    final LayoutInflater inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    itemView = inflater.inflate(
                            R.layout.settings_item_view, parent, false);
                }
                final SettingsItem item = getItem(position);
                final TextView titleTextView = (TextView) itemView.findViewById(R.id.title);
                final TextView subtitleTextView = (TextView) itemView.findViewById(R.id.subtitle);
                final String summaryText = item.getDisplayDetail();
                titleTextView.setText(item.getDisplayName());

                if (!TextUtils.isEmpty(summaryText)) {
                    if(hasDigit(summaryText)){//bug 1090695
                        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                        final String numberText = bidiFormatter.unicodeWrap(summaryText, TextDirectionHeuristicsCompat.LTR);
                        subtitleTextView.setText(numberText);
                    }else{
                        subtitleTextView.setText(summaryText);
                    }
                    subtitleTextView.setVisibility(View.VISIBLE);
                } else {
                    subtitleTextView.setVisibility(View.GONE);
                }
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switch (item.getType()) {
                            case SettingsItem.TYPE_GENERAL_SETTINGS:
                                UIIntents.get().launchApplicationSettingsActivity(getActivity(),
                                        false /* topLevel */);
                                break;

                            case SettingsItem.TYPE_PER_SUBSCRIPTION_SETTINGS:
                                UIIntents.get().launchPerSubscriptionSettingsActivity(getActivity(),
                                        item.getSubId(), item.getActivityTitle());
                                break;

                            default:
                                Assert.fail("unrecognized setting type!");
                                break;
                        }
                    }
                });
                return itemView;
            }

            private boolean hasDigit(String content) {//bug 1090695
                Pattern p = Pattern.compile(".*\\d+.*");
                Matcher m = p.matcher(content);
                if (m.matches()) {
                    return true;
                }
                return false;
            }
        }
    }
}
