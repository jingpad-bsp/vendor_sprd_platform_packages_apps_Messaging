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
package com.android.messaging.ui.conversationlist;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.app.ActivityManager;
import android.util.Log;

import com.android.messaging.R;
import com.android.messaging.ui.SnackBarManager;
import com.android.messaging.util.DebugUtils;

public class ArchivedConversationListActivity extends AbstractConversationListActivity {
    //bug 496495 begin
    private static ArchivedConversationListActivity mLastActivity = null;
    private String TAG = "ArchivedConversationListActivity";
    //bug 496495 end
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //bug 496495 begin
        if(ActivityManager.isUserAMonkey()){
            Log.i(TAG, " onCreate() monkey------>> mLastActivity."+mLastActivity);
            if(mLastActivity != null){
                 mLastActivity.finish();
            }
            mLastActivity = this;
        }
        //bug 496495 end
        if(getConversationListFragment() == null){//Bug 871550
            final ConversationListFragment fragment =
                    ConversationListFragment.createArchivedConversationListFragment();
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment,ConversationListFragment.FRAGMENT_TAG).commit();
        }else{
            Log.e(TAG, "onCreate()",new Exception());
        }//Bug 871550
        invalidateActionBar();
    }

    private ConversationListFragment getConversationListFragment() {//Bug 871550
        return (ConversationListFragment) getFragmentManager().findFragmentByTag(
                ConversationListFragment.FRAGMENT_TAG);
    }


    protected void updateActionBar(ActionBar actionBar) {
        actionBar.setTitle(getString(R.string.archived_activity_title));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(
                getResources().getColor(
                        R.color.archived_conversation_action_bar_background_color_dark)));
        actionBar.show();
        super.updateActionBar(actionBar);
    }

    @Override
    public void onBackPressed() {
        if (isInConversationListSelectMode()) {
            exitMultiSelectState();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        getMenuInflater().inflate(R.menu.archived_conversation_list_menu, menu);
        final MenuItem item = menu.findItem(R.id.action_debug_options);
        if (item != null) {
            final boolean enableDebugItems = DebugUtils.isDebugEnabled();
            item.setVisible(enableDebugItems).setEnabled(enableDebugItems);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.action_debug_options:
                onActionBarDebug();
                return true;
            case android.R.id.home:
                onActionBarHome();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onActionBarHome() {
        onBackPressed();
    }

    @Override
    public boolean isSwipeAnimatable() {
        return false;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(SnackBarManager.sInstance!=null){
            if(SnackBarManager.sInstance.getPopupWindow()!=null){
                SnackBarManager.sInstance.getPopupWindow().dismiss();
            }

        }
    }
}