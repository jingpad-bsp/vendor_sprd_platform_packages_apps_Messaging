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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;
import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.action.DeleteConversationAction;
import com.android.messaging.datamodel.action.DeleteConversationAction.DeleteConversationActionListener;
import com.android.messaging.datamodel.action.BugleActionToasts;
import com.android.messaging.datamodel.action.UpdateConversationArchiveStatusAction;
import com.android.messaging.datamodel.action.UpdateConversationOptionsAction;
import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.SnackBar;
import com.android.messaging.ui.SnackBarInteraction;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.ui.contact.AddContactsConfirmationDialog;
import com.android.messaging.ui.conversationlist.ConversationListFragment.ConversationListFragmentHost;
import com.android.messaging.ui.conversationlist.MultiSelectActionModeCallback.SelectedConversation;
import com.android.messaging.util.BugleGservices;
import com.android.messaging.util.BugleGservicesKeys;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.Trace;
import com.android.messaging.util.UiUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
//add for bug 587201 begin
import android.view.inputmethod.InputMethodManager;
import com.android.messaging.ui.conversation.ConversationActivity;
//add for bug 587201 end
//import android.app.PerformanceManagerInternal;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;

/**
 * Base class for many Conversation List activities. This will handle the common actions of multi
 * select and common launching of intents.
 */
public abstract class AbstractConversationListActivity  extends BugleActionBarActivity
    implements ConversationListFragmentHost, MultiSelectActionModeCallback.Listener {
    private static final String TAG = "AbstractConversationListActivity";
    private static final int REQUEST_SET_DEFAULT_SMS_APP = 1;
    private static final Uri SMSMMS_SEARCH_CONTENT_URI = Uri.parse("content://mms-sms/sqlite_sequence");

    protected ConversationListFragment mConversationListFragment;
    private Context mContext;
    private Dialog mWaitingDialog;
    private AlertDialog mCancelDialog = null;
    @Override
    public void onAttachFragment(final Fragment fragment) {
        Trace.beginSection("AbstractConversationListActivity.onAttachFragment");
        // Fragment could be debug dialog
        if (fragment instanceof ConversationListFragment) {
            mConversationListFragment = (ConversationListFragment) fragment;
            mConversationListFragment.setHost(this);
            mContext = fragment.getContext();
        }
        Trace.endSection();
    }

    @Override
    public void onBackPressed() {
        // If action mode is active dismiss it
        if (getActionMode() != null) {
            dismissActionMode();
            return;
        }
        if(mConversationListFragment !=null && mConversationListFragment.isResumed()){
            super.onBackPressed();
        }
    }

    protected void startMultiSelectActionMode() {
        startActionMode(new MultiSelectActionModeCallback(this));
    }

    protected void exitMultiSelectState() {
        mConversationListFragment.showFab();
        dismissActionMode();
        mConversationListFragment.updateUi();
    }

    protected boolean isInConversationListSelectMode() {
        return getActionModeCallback() instanceof MultiSelectActionModeCallback;
    }

    @Override
    public boolean isSelectionMode() {
        return isInConversationListSelectMode();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    }

    protected long getConversationMaxCount(){
        long conversationCnt= 0;
        Cursor cursor = null;
        try {
            final ContentResolver resolver = Factory.get().getApplicationContext().getContentResolver();
            cursor = resolver.query(SMSMMS_SEARCH_CONTENT_URI,
                    null ,
                    "select _id from threads",
                    null,
                    null,
                    null);
            if (cursor != null) {
                    conversationCnt = cursor.getCount();
                    LogUtil.i(TAG,"jessica add  : onActionBarDelete ,conversationCnt is " + conversationCnt);
                cursor.close();
                cursor = null;
            }
          } catch (Exception e) {
              LogUtil.i(TAG,"Exception in isBloacked() ,Exception is " + e);
          } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return conversationCnt;
    }

    protected String[] convertSelectedConversationToArray(final Collection<SelectedConversation> conversations){
        String[] szConversations = new String[conversations.size()];
        int szIndex = 0;
        for (final SelectedConversation conversation : conversations) {
            szConversations[szIndex] = conversation.conversationId;
            szIndex++;
        }
        return szConversations;
    }

    protected long getSmsMmsMaxCount(String szArg){
        long MaxCnt= 0;
        Cursor cursor = null;
        try {

            final ContentResolver resolver = Factory.get().getApplicationContext().getContentResolver();
            cursor = resolver.query(SMSMMS_SEARCH_CONTENT_URI,
                    null ,
                   /* "select seq from sqlite_sequence where name =? ",*//*Bug modified for 791793 start*/
                    "select max(_id) from " + szArg +";",
                    null,
                    //new String[] { szArg, },
                    null,
                    null);
            if (cursor != null) {
                LogUtil.i(TAG," getSmsMmsMaxCount  cursor.getCount()" + cursor.getCount());
                while (cursor.moveToNext()) {
                    //MaxCnt = cursor.getInt(cursor.getColumnIndex("seq"));
                    MaxCnt = cursor.getInt(0);
                    LogUtil.i(TAG,"jessica add  : onActionBarDelete ,szArg is " + szArg + " MaxCnt = "+MaxCnt);
                    /*Bug modified for 791793 end*/
                }
                cursor.close();
                cursor = null;
            }
          } catch (Exception e) {
              LogUtil.i(TAG,"Exception in isBloacked() ,Exception is " + e);
          } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return MaxCnt;
    }

    long conversationCnt = 0;
    long smsMaxCnt = 0;
    long mmsMaxCnt = 0;
   static  Object mObj = new Object();
    class MyRunnable implements Runnable
    {
        @Override
        public void run() {
            synchronized( mObj){
           LogUtil.i(TAG,"3.1==>>jessica add  : onActionBarDelete run");
           final long oldMsecs = System.currentTimeMillis();
           conversationCnt= getConversationMaxCount();
           String szSms = "sms";
           String szMms = "pdu";
           smsMaxCnt = getSmsMmsMaxCount(szSms);
           mmsMaxCnt = getSmsMmsMaxCount(szMms);
           LogUtil.i(TAG,"3.2==>>jessica add  : onActionBarDelete smsMaxCnt = "
           + smsMaxCnt + "mmsMaxCnt = " + mmsMaxCnt);
           final long nowMsecs = System.currentTimeMillis();
           final long allMsecs = nowMsecs - oldMsecs;
           LogUtil.e(TAG, "3.3==>>jessica out onActionBarDelete allMsecs = " + allMsecs + " oldMsecs = " + oldMsecs + " nowMsecs = " + nowMsecs);
       }
        }
    }

    @Override
    public void onActionBarDelete(final Collection<SelectedConversation> conversations) {
        LogUtil.i(TAG,"4.1==>>jessica add  : onActionBarDelete enther");
        final Thread thread = new Thread(new MyRunnable());
        thread.start();
        if (!PhoneUtils.getDefault().isDefaultSmsApp()) {
            // TODO: figure out a good way to combine this with the implementation in
            // ConversationFragment doing similar things
            final Activity activity = this;
            UiUtils.showSnackBarWithCustomAction(this,
                    getWindow().getDecorView().getRootView(),
                    getString(R.string.requires_default_sms_app),
                    SnackBar.Action.createCustomAction(new Runnable() {
                            @Override
                            public void run() {
                                final Intent intent =
                                        UIIntents.get().getChangeDefaultSmsAppIntent(activity);
                                startActivityForResult(intent, REQUEST_SET_DEFAULT_SMS_APP);
                            }
                        },
                        getString(R.string.requires_default_sms_change_button)),
                    null /* interactions */,
                    null /* placement */);
            return;
        }

        mCancelDialog=new AlertDialog.Builder(this)
                .setTitle(getResources().getQuantityString(
                        R.plurals.delete_conversations_confirmation_dialog_title,
                        conversations.size()))
                .setPositiveButton(R.string.delete_conversation_confirmation_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                    final int button) {
                                //PerformanceManagerInternal.getDefault().removeApplcationSnapShot(AbstractConversationListActivity.this.getPackageName());/*add for Bug 843192 */   //only form compile by weicn
                                //Bug 983721 begin
                                mWaitingDialog = createLoadingDialog(mContext);
                                deleteThreadsTimeoutHandler.sendEmptyMessage(DELETETHREADS_START_TAG);
                                DeleteConversationActionObsever deleteConversationActionObsever = new DeleteConversationActionObsever(mWaitingDialog);
                                //Bug 983721 end
                                LogUtil.i(TAG,"4.3==>>jessica add  : onActionBarDelete onClick conversationCnt = " + conversationCnt);
                                LogUtil.e(TAG,"4.4==>>jessica add  : onActionBarDelete onClick smsMaxCnt = "
                                        + smsMaxCnt + " mmsMaxCnt = " + mmsMaxCnt + " SystemTime = " + System.currentTimeMillis());
                                String[] szConversations = new String[conversations.size()];
                                szConversations =convertSelectedConversationToArray(conversations);
                                LogUtil.i(TAG,"4.5==>>jessica add  : onActionBarDelete ,conversations.size() = " + conversations.size());
                                if(conversations.size() > 1){
                                            if (conversations.size() < conversationCnt){
                                                DeleteConversationAction.deleteConversation(
                                                        szConversations,
                                                        0,
                                                        smsMaxCnt,
                                                        mmsMaxCnt,
                                                        deleteConversationActionObsever); //Bug 983721
                                            }else if(conversations.size() >= conversationCnt){/*modified for Bug 729129*/
                                                szConversations = new String[] {""};
                                                DeleteConversationAction.deleteConversation(
                                                        szConversations,
                                                        0,
                                                        smsMaxCnt,
                                                        mmsMaxCnt,
                                                        deleteConversationActionObsever); //Bug 983721
                                            }/*deleted for Bug 729129 start*//*else
                                            {
                                                LogUtil.e(TAG,"4.6==>>jessica add  : onActionBarDelete ,SelectedConversation > conversations.size()  error outside!!!!");
                                                return;
                                            }*//*deleted for Bug 729129 end*/
                                        }else if(conversations.size() > 0){
                                            for (final SelectedConversation conversation : conversations) {
                                                DeleteConversationAction.deleteConversation(
                                                        conversation.conversationId,
                                                        conversation.timestamp,
                                                        deleteConversationActionObsever //Bug 983721
                                                        );
                                            }
                                        }else{
                                            LogUtil.e(TAG,"4.7==>>jessica add  : onActionBarDelete , conversations.size()<0  error outside!!!!");
                                            return;
                                        }
                                Log.d("AbstractConversationListActivity","4.8==>>=======jihao=====Deleted local conversation===done===");
                                conversationCnt = 0;
                                smsMaxCnt = 0;
                                mmsMaxCnt = 0;
                                exitMultiSelectState();
                            }
                })
                .setNegativeButton(R.string.delete_conversation_decline_button, null)
                .show();
    }

    //Bug 983721 begin
    private static class DeleteConversationActionObsever implements DeleteConversationAction.DeleteConversationActionListener {
        Dialog mWaitingDialog;
        DeleteConversationActionObsever(Dialog dialog) {
            mWaitingDialog = dialog;
        }

        @Override
        public void onDeleteConversationActionAction(final boolean success) {
            if (success) {
                Log.d(TAG, "deleteConversationAction successfully.");
            } else {
                Log.d(TAG, "deleteConversationAction failed.");
            }
            if (null !=  mWaitingDialog) {
                if(mWaitingDialog.isShowing()) {
                    mWaitingDialog.dismiss();
                }
                mWaitingDialog = null;
            }
        }
    }
    //Bug 983721 begin

    @Override
    public void onActionBarArchive(final Iterable<SelectedConversation> conversations,
            final boolean isToArchive) {
        final ArrayList<String> conversationIds = new ArrayList<String>();
        for (final SelectedConversation conversation : conversations) {
            final String conversationId = conversation.conversationId;
            conversationIds.add(conversationId);
        }
            if (isToArchive) {
            UpdateConversationArchiveStatusAction.archiveSprdConversation(conversationIds);
            } else {
            UpdateConversationArchiveStatusAction.unarchiveSprdConversation(conversationIds);
        }

        final Runnable undoRunnable = new Runnable() {
            @Override
            public void run() {
               /*for (final String conversationId : conversationIds) {
                    if (isToArchive) {
                        UpdateConversationArchiveStatusAction.unarchiveConversation(conversationId);
                    } else {
                        UpdateConversationArchiveStatusAction.archiveConversation(conversationId);
                    }
                }*/
                if (isToArchive) {
                    UpdateConversationArchiveStatusAction.unarchiveSprdConversation(conversationIds);
                } else {
                    UpdateConversationArchiveStatusAction.archiveSprdConversation(conversationIds);
                }
            }
        };

        final int textId =
                isToArchive ? R.string.archived_toast_message : R.string.unarchived_toast_message;
        final String message = getResources().getString(textId, conversationIds.size());
        UiUtils.showSnackBar(this, findViewById(android.R.id.list), message, undoRunnable,
                SnackBar.Action.SNACK_BAR_UNDO,
                mConversationListFragment.getSnackBarInteractions());
        exitMultiSelectState();
    }

    @Override
    public void onActionBarNotification(final Iterable<SelectedConversation> conversations,
            final boolean isNotificationOn) {

        /* Add by SPRD for Bug:509855 2015.12.14 Start */
        final ArrayList<String> conversationIds = new ArrayList<>();
        /* Add by SPRD for Bug:509855 2015.12.14 End */

        for (final SelectedConversation conversation : conversations) {
            UpdateConversationOptionsAction.enableConversationNotifications(
                    conversation.conversationId, isNotificationOn);

            /* Add by SPRD for Bug:509855 2015.12.14 Start */
            conversationIds.add(conversation.conversationId);
            /* Add by SPRD for Bug:509855 2015.12.14 End */

        }

        /* Add by SPRD for Bug:509855 2015.12.14 Start */
        final Runnable undoRunnable = new Runnable() {
            @Override
            public void run() {
                for (String conversationId : conversationIds) {
                    UpdateConversationOptionsAction.enableConversationNotifications(
                            conversationId, !isNotificationOn);
                }
            }
        };
        /* And by SPRD for Bug:509855 2015.12.14 End */

        final int textId = isNotificationOn ?
                R.string.notification_on_toast_message : R.string.notification_off_toast_message;
        final String message = getResources().getString(textId, 1);

        /* Modify by SPRD for Bug:509855 2015.12.14 Start */
//        UiUtils.showSnackBar(this, findViewById(android.R.id.list), message,
//            null /* undoRunnable */,
//            SnackBar.Action.SNACK_BAR_UNDO, mConversationListFragment.getSnackBarInteractions());
        UiUtils.showSnackBar(this, findViewById(android.R.id.list), message,
                undoRunnable, SnackBar.Action.SNACK_BAR_UNDO,
                mConversationListFragment.getSnackBarInteractions());
        /* Modify by SPRD for Bug:509855 2015.12.14 End */

        exitMultiSelectState();
    }

    //Bug 996662 begin
    @Override
    protected void onDestroy() {
        if (null !=  mWaitingDialog) {
                mWaitingDialog.dismiss();
                 mWaitingDialog = null;
        }
        if (null !=  mCancelDialog) {
                mCancelDialog.dismiss();
                 mCancelDialog = null;
        }

        super.onDestroy();
        Log.d(TAG, "onDestory.");
    }
    //Bug 996662 end

    @Override
    public void onActionBarAddContact(final SelectedConversation conversation) {
        final Uri avatarUri;
        if (conversation.icon != null) {
            avatarUri = Uri.parse(conversation.icon);
        } else {
            avatarUri = null;
        }
        final AddContactsConfirmationDialog dialog = new AddContactsConfirmationDialog(
                this, avatarUri, conversation.otherParticipantNormalizedDestination);
        dialog.show();
        exitMultiSelectState();
    }

    @Override
    public void onActionBarBlock(final SelectedConversation conversation) {
        final Resources res = getResources();
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance(); //by 1229545
        final String otherParticipantNormalizedDestination = bidiFormatter.unicodeWrap(conversation.otherParticipantNormalizedDestination, TextDirectionHeuristics.LTR); //by 1229545
        new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.block_confirmation_title,
                        otherParticipantNormalizedDestination))
                .setMessage(res.getString(R.string.block_confirmation_message))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface arg0, final int arg1) {
                        final Context context = AbstractConversationListActivity.this;
                        final View listView = findViewById(android.R.id.list);
                        final List<SnackBarInteraction> interactions =
                                mConversationListFragment.getSnackBarInteractions();
                        final UpdateDestinationBlockedAction.UpdateDestinationBlockedActionListener
                                undoListener =
                                        new UpdateDestinationBlockedActionSnackBar(
                                                context, listView, null /* undoRunnable */,
                                                interactions);
                        final Runnable undoRunnable = new Runnable() {
                            @Override
                            public void run() {
                                UpdateDestinationBlockedAction.updateDestinationBlocked(
                                        conversation.otherParticipantNormalizedDestination, false,
                                        conversation.conversationId,
                                        undoListener);
                            }
                        };
                        final UpdateDestinationBlockedAction.UpdateDestinationBlockedActionListener
                              listener = new UpdateDestinationBlockedActionSnackBar(
                                      context, listView, undoRunnable, interactions);
                        UpdateDestinationBlockedAction.updateDestinationBlocked(
                                conversation.otherParticipantNormalizedDestination, true,
                                conversation.conversationId,
                                listener);
                        exitMultiSelectState();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onConversationClick(final ConversationListData listData,
                                    final ConversationListItemData conversationListItemData,
                                    final boolean isLongClick,
                                    final ConversationListItemView conversationView) {
        if (isLongClick && !isInConversationListSelectMode()) {
            // add for bug 587201 begin
            // hide inputMethod when longClick conversation
            hideInputMethod();
            // add for bug 587201 end
            startMultiSelectActionMode();
        }

        if (isInConversationListSelectMode()) {
            final MultiSelectActionModeCallback multiSelectActionMode =
                    (MultiSelectActionModeCallback) getActionModeCallback();
            multiSelectActionMode.toggleSelect(listData, conversationListItemData);
            mConversationListFragment.updateUi();
        } else {
            final String conversationId = conversationListItemData.getConversationId();
            Bundle sceneTransitionAnimationOptions = null;
            boolean hasCustomTransitions = false;

            UIIntents.get().launchConversationActivity(
                    this, conversationId, null,
                    sceneTransitionAnimationOptions,
                    hasCustomTransitions);
        }
    }

    @Override
    public void onCreateConversationClick() {
        UIIntents.get().launchCreateNewConversationActivity(this, null);
    }


    @Override
    public boolean isConversationSelected(final String conversationId) {
        return isInConversationListSelectMode() &&
                ((MultiSelectActionModeCallback) getActionModeCallback()).isSelected(
                        conversationId);
    }

    // fix for bug 547780 begin
    @Override
    public void updateSelectedAllOrNoneIconState() {
        if (isInConversationListSelectMode()) {
            ((MultiSelectActionModeCallback) getActionModeCallback()).updateSelectedAllOrNoneIconState();
        }
    }
    // fix for bug 547780 end

    public void onActionBarDebug() {
        DebugUtils.showDebugOptions(this);
    }

    private static class UpdateDestinationBlockedActionSnackBar
            implements UpdateDestinationBlockedAction.UpdateDestinationBlockedActionListener {
        private final Context mContext;
        private final View mParentView;
        private final Runnable mUndoRunnable;
        private final List<SnackBarInteraction> mInteractions;

        UpdateDestinationBlockedActionSnackBar(final Context context,
                @NonNull final View parentView, @Nullable final Runnable undoRunnable,
                @Nullable List<SnackBarInteraction> interactions) {
            mContext = context;
            mParentView = parentView;
            mUndoRunnable = undoRunnable;
            mInteractions = interactions;
        }

        @Override
        public void onUpdateDestinationBlockedAction(
            final UpdateDestinationBlockedAction action,
            final boolean success, final boolean block,
            final String destination) {
            if (success) {
                final int messageId = block ? R.string.blocked_toast_message
                        : R.string.unblocked_toast_message;
                final String message = mContext.getResources().getString(messageId, 1);
                UiUtils.showSnackBar(mContext, mParentView, message, mUndoRunnable,
                        SnackBar.Action.SNACK_BAR_UNDO, mInteractions);
            }
        }
    }
    // fix for bug 547780 begin
    @Override
    public ConversationListAdapter getCurrentAdapter(){
        if (mConversationListFragment != null) {
            return mConversationListFragment.getConversationListAdapter();
        }
        return null;
    }
    // fix for bug 547780 end
    // add for bug 587201 begin
    private void hideInputMethod() {
        Log.d(TAG, "enter hideInputMethod() when longClick conversation");
        InputMethodManager imm = (InputMethodManager) getSystemService(ConversationActivity.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
    // add for bug 587201 end

    //Bug 983721 begin
    public static Dialog createLoadingDialog(Context context) {
        ProgressBar progressBar = new ProgressBar(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.addView(progressBar, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final AlertDialog showAlertDialog = new android.app.AlertDialog.Builder(context, R.style.dialogWithoutBg).create();
        showAlertDialog.setCancelable(false);
        showAlertDialog.setView(linearLayout);
        showAlertDialog.show();
        Window dialogWindow = showAlertDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.alpha = 0.8f;
        lp.dimAmount = 0.2f;
        dialogWindow.setAttributes(lp);
        dialogWindow.setGravity(Gravity.CENTER);
        return showAlertDialog;
    }

    private final static int DELETETHREADS_START_TAG = 0;

    private final static int DELETETHREADS_TIMEOUT_MS = 60*1000;

    private Handler deleteThreadsTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DELETETHREADS_START_TAG:
                    deleteThreadsTimeoutHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "delete threads timeout!!");
                            if (null !=  mWaitingDialog) {
                                if(mWaitingDialog.isShowing()) {
                                    mWaitingDialog.dismiss();
                                }
                                mWaitingDialog = null;
                            }
                        }
                    }, DELETETHREADS_TIMEOUT_MS);
                    break;
                default:
                    Log.e(TAG, "Unkown message, message.what " + msg.what);
                    break;
            }
        };
    };
    //Bug 983721 begin
}
