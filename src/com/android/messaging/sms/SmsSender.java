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

package com.android.messaging.sms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.telephony.TelephonyManager; //sprd add for smsc
import android.util.Log;
import android.widget.Toast;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.receiver.SendStatusReceiver;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.util.Assert;
import com.android.messaging.util.BugleGservices;
import com.android.messaging.util.BugleGservicesKeys;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.GlobleUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that sends chat message via SMS.
 *
 * The interface emulates a blocking sending similar to making an HTTP request.
 * It calls the SmsManager to send a (potentially multipart) message and waits
 * on the sent status on each part. The waiting has a timeout so it won't wait
 * forever. Once the sent status of all parts received, the call returns.
 * A successful sending requires success status for all parts. Otherwise, we
 * pick the highest level of failure as the error for the whole message, which
 * is used to determine if we need to retry the sending.
 */
public class SmsSender {
    private static final String TAG = LogUtil.BUGLE_TAG;

    public static final String EXTRA_PART_ID = "part_id";

    /*
     * A map for pending sms messages. The key is the random request UUID.
     */
    private static ConcurrentHashMap<Uri, SendResult> sPendingMessageMap =
            new ConcurrentHashMap<Uri, SendResult>();

    private static final Random RANDOM = new Random();

    // Whether we should send multipart SMS as separate messages
    private static Boolean sSendMultipartSmsAsSeparateMessages = null;
    //Sprd add for smsc
    public static String SMSC_EMPTY_FAILURE="SmsSender: empty smsc address";

    /**
     * Class that holds the sent status for all parts of a multipart message sending
     */
    public static class SendResult {
        // Failure levels, used by the caller of the sender.
        // For temporary failures, possibly we could retry the sending
        // For permanent failures, we probably won't retry
        public static final int FAILURE_LEVEL_NONE = 0;
        public static final int FAILURE_LEVEL_TEMPORARY = 1;
        public static final int FAILURE_LEVEL_PERMANENT = 2;

        // Tracking the remaining pending parts in sending
        private int mPendingParts;
        // Tracking the highest level of failure among all parts
        private int mHighestFailureLevel;

        public SendResult(final int numOfParts) {
            Assert.isTrue(numOfParts > 0);
            mPendingParts = numOfParts;
            mHighestFailureLevel = FAILURE_LEVEL_NONE;
        }

        // Update the sent status of one part
        public void setPartResult(final int resultCode) {
            mPendingParts--;
            setHighestFailureLevel(resultCode);
        }

        public boolean hasPending() {
            return mPendingParts > 0;
        }

        public int getHighestFailureLevel() {
            return mHighestFailureLevel;
        }

        private int getFailureLevel(final int resultCode) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    return FAILURE_LEVEL_NONE;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    return FAILURE_LEVEL_TEMPORARY;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    return FAILURE_LEVEL_PERMANENT;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    return FAILURE_LEVEL_PERMANENT;
                default: {
                    LogUtil.e(TAG, "SmsSender: Unexpected sent intent resultCode = " + resultCode);
                    return FAILURE_LEVEL_PERMANENT;
                }
            }
        }

        private void setHighestFailureLevel(final int resultCode) {
            final int level = getFailureLevel(resultCode);
            if (level > mHighestFailureLevel) {
                mHighestFailureLevel = level;
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("SendResult:");
            sb.append("Pending=").append(mPendingParts).append(",");
            sb.append("HighestFailureLevel=").append(mHighestFailureLevel);
            return sb.toString();
        }
    }

    public static void setResult(final Uri requestId, final int resultCode,
            final int errorCode, final int partId, int subId) {
        //Add by SPRD for 606190 begin
        SendResult result = null;
        if (requestId != null) {
            result = sPendingMessageMap.get(requestId);
            if (result != null) {
                synchronized (result) {
                    result.setPartResult(resultCode);
                    if (!result.hasPending()) {
                        result.notifyAll();
                    }
                }
            } else {
                LogUtil.e(TAG, "SmsSender: ignoring sent result. " + " requestId=" + requestId
                        + " partId=" + partId + " resultCode=" + resultCode);
            }
        }

        if (resultCode != Activity.RESULT_OK) {
            LogUtil.e(TAG, "SmsSender: failure in sending message part. "
                    + " requestId=" + requestId + " partId=" + partId
                    + " resultCode=" + resultCode + " errorCode=" + errorCode);
            final Context context = Factory.get().getApplicationContext();
            if (errorCode != SendStatusReceiver.NO_ERROR_CODE) {
                UiUtils.showToastAtBottom(getSendErrorToastMessage(context, subId, errorCode));
            }
            //Bug 1004506 begin
            else if (resultCode == SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE) {
                //remove
                LogUtil.d(TAG, "SmsSender: remove toast fdn check");
                //UiUtils.showToastAtBottom(context.getString(R.string.fdn_check_failure));
            }
            //Bug 1004506 end
        } else {
            if(MmsUtils.isEnabelSmsSendReport(subId)) {
                final Context context = Factory.get().getApplicationContext();
                if (result != null && !result.hasPending()) {
                    Toast.makeText(context, context.getString(R.string.host_app_name) + context.getString(R.string.message_outgoing_successful), Toast.LENGTH_LONG).show();
                }
            }
            if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
                LogUtil.v(TAG, "SmsSender: received sent result. " + " requestId=" + requestId
                        + " partId=" + partId + " resultCode=" + resultCode);
            }
        }
        //Add by SPRD for 606190 end
    }

    private static String getSendErrorToastMessage(final Context context, final int subId,
            final int errorCode) {
        final String carrierName = PhoneUtils.get(subId).getCarrierName();
        if (TextUtils.isEmpty(carrierName)) {
            return context.getString(R.string.carrier_send_error_unknown_carrier, errorCode);
        } else {
            return context.getString(R.string.carrier_send_error, carrierName, errorCode);
        }
    }

    // This should be called from a RequestWriter queue thread
    public static SendResult sendMessage(final Context context,  final int subId, String dest,
            String message, final String serviceCenter, final boolean requireDeliveryReport,
            final Uri messageUri) throws SmsException {
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "SmsSender: sending message. " +
                    "dest=" + dest + " message=" + message +
                    " serviceCenter=" + serviceCenter +
                    " requireDeliveryReport=" + requireDeliveryReport +
                    " requestId=" + messageUri);
        }
        Log.i(TAG, "getFinalSendEmptyMessageFlag====>" + MmsConfig.get(subId).getFinalSendEmptyMessageFlag());
        if((MmsConfig.get(subId).getFinalSendEmptyMessageFlag() < 0)){
            if (TextUtils.isEmpty(message)) {
                throw new SmsException("SmsSender: empty text message");
            }
        }
        // Get the real dest and message for email or alias if dest is email or alias
        // Or sanitize the dest if dest is a number
        if (!TextUtils.isEmpty(MmsConfig.get(subId).getEmailGateway()) &&
                (MmsSmsUtils.isEmailAddress(dest) || MmsSmsUtils.isAlias(dest, subId))) {
            // The original destination (email address) goes with the message
            message = dest + " " + message;
            // the new address is the email gateway #
            dest = MmsConfig.get(subId).getEmailGateway();
        } else {
            // remove spaces and dashes from destination number
            // (e.g. "801 555 1212" -> "8015551212")
            // (e.g. "+8211-123-4567" -> "+82111234567")
            dest = PhoneNumberUtils.stripSeparators(dest);
        }
        if (TextUtils.isEmpty(dest)) {
            throw new SmsException("SmsSender: empty destination address");
        }
        //Bug 892217 begin
        // sprd add for smsc begin
        //if (TextUtils.isEmpty(serviceCenter) || serviceCenter == null) {
        //    throw new SmsException(SMSC_EMPTY_FAILURE);
        //}
        // sprd add for smsc end
        //Bug 892217 end
        // Divide the input message by SMS length limit
        final SmsManager smsManager = PhoneUtils.get(subId).getSmsManager();
        /*Add by SPRD for Bug:562203 Encode Type feature feature  Start */
        ArrayList<String> messages = new ArrayList<String>();
       /* if(MmsConfig.get(1).getEncodePrefeStatus() == 3){
            messages = divideMessageFor16Bit(message);
            Log.d(TAG+"-encodeTypeLog", "SmsSender | 16bit encode.");
        }else{*/

          messages = smsManager.divideMessage(message);
          /*  Log.d(TAG+"-encodeTypeLog", "SmsSender | 7bit encode.");
        }*/
        /*Add by SPRD for Bug:562203 Encode Type feature feature  End */
        if ((MmsConfig.get(subId).getFinalSendEmptyMessageFlag() == 1)
                || (MmsConfig.get(subId).getFinalSendEmptyMessageFlag() == 2)) {
            if (messages.size() < 1) {
                messages.add("");
                Log.i("kkkk", "messages.add() is ok");
            }
        } else {
            if (messages == null || messages.size() < 1) {
                throw new SmsException("SmsSender: fails to divide message");
            }
        }
        // Prepare the send result, which collects the send status for each part
        final SendResult pendingResult = new SendResult(messages.size());
        sPendingMessageMap.put(messageUri, pendingResult);

        //fdn feature for sms begin 489520 begin
//        List<String> listContacts = new ArrayList<String>(1);
//        listContacts.add(dest);
//        boolean[] sendFilter = smsManager.checkFdnContacts(listContacts);
//        if(sendFilter[0]){
//           Message msgflag = GlobleUtil.mGlobleHandler.obtainMessage();
//           msgflag.what = GlobleUtil.FDN_TOAST_MSG;
//           msgflag.obj = context.getString(R.string.fdn_check_failure);
//           GlobleUtil.mGlobleHandler.sendMessage(msgflag);
//           return null;
//         /* Add by SPRD for bug 558251 2016/05/11 Start */
//         } else {
//             listContacts.clear();
//             listContacts.add(serviceCenter);
//             sendFilter = smsManager.checkFdnContacts(listContacts);
//             if(sendFilter[0]){
//                 Message msgflag = GlobleUtil.mGlobleHandler.obtainMessage();
//                 msgflag.what = GlobleUtil.FDN_TOAST_MSG;
//                 msgflag.obj = context.getString(R.string.smsc_fdn_check_failure);
//                 GlobleUtil.mGlobleHandler.sendMessage(msgflag);
//                 return null;
//             }
//         }
//        listContacts = null;
//        sendFilter = null;
        /* Add by SPRD for bug 558251 2016/05/11 End */
        //fdn feature for sms begin 489520 end

        // Actually send the sms
        sendInternal(
                context, subId, dest, messages, serviceCenter, requireDeliveryReport, messageUri);
        // Wait for pending intent to come back
        synchronized (pendingResult) {
            final long smsSendTimeoutInMillis = BugleGservices.get().getLong(
                    BugleGservicesKeys.SMS_SEND_TIMEOUT_IN_MILLIS,
                    BugleGservicesKeys.SMS_SEND_TIMEOUT_IN_MILLIS_DEFAULT);
            final long beginTime = SystemClock.elapsedRealtime();
            long waitTime = smsSendTimeoutInMillis;
            // We could possibly be woken up while still pending
            // so make sure we wait the full timeout period unless
            // we have the send results of all parts.
            while (pendingResult.hasPending() && waitTime > 0) {
                try {
                    pendingResult.wait(waitTime);
                } catch (final InterruptedException e) {
                    LogUtil.e(TAG, "SmsSender: sending wait interrupted");
                }
                waitTime = smsSendTimeoutInMillis - (SystemClock.elapsedRealtime() - beginTime);
            }
        }
        // Either we timed out or have all the results (success or failure)
        sPendingMessageMap.remove(messageUri);
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "SmsSender: sending completed. " +
                    "dest=" + dest + " message=" + message + " result=" + pendingResult);
        }
        return pendingResult;
    }

    // Actually sending the message using SmsManager
    private static void sendInternal(final Context context, final int subId, String dest,
            final ArrayList<String> messages, final String serviceCenter,
            final boolean requireDeliveryReport, final Uri messageUri) throws SmsException {
        Assert.notNull(context);
        final SmsManager smsManager = PhoneUtils.get(subId).getSmsManager();
        final int messageCount = messages.size();
        final ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>(messageCount);
        final ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);
        for (int i = 0; i < messageCount; i++) {
            // Make pending intents different for each message part
            final int partId = (messageCount <= 1 ? 0 : i + 1);
            if (requireDeliveryReport && (i == (messageCount - 1))) {
                // TODO we only care about the delivery status of the last part
                // Shall we have better tracking of delivery status of all parts?
                deliveryIntents.add(PendingIntent.getBroadcast(
                        context,
                        partId,
                        getSendStatusIntent(context, SendStatusReceiver.MESSAGE_DELIVERED_ACTION,
                                messageUri, partId, subId),
                        0/*flag*/));
            } else {
                deliveryIntents.add(null);
            }
            sentIntents.add(PendingIntent.getBroadcast(
                    context,
                    partId,
                    getSendStatusIntent(context, SendStatusReceiver.MESSAGE_SENT_ACTION,
                            messageUri, partId, subId),
                    0/*flag*/));
        }
        if (sSendMultipartSmsAsSeparateMessages == null) {
            sSendMultipartSmsAsSeparateMessages = MmsConfig.get(subId)
                    .getSendMultipartSmsAsSeparateMessages();
        }
        try {
            if (sSendMultipartSmsAsSeparateMessages) {
                // If multipart sms is not supported, send them as separate messages
                for (int i = 0; i < messageCount; i++) {
                    smsManager.sendTextMessage(dest,
                            serviceCenter,
                            messages.get(i),
                            sentIntents.get(i),
                            deliveryIntents.get(i));
                }
            } else {
                smsManager.sendMultipartTextMessage(
                        dest, serviceCenter, messages, sentIntents, deliveryIntents);
            }
          /// fdn feature toast end
        }catch(IllegalArgumentException e){
            if ("Invalid FDN destinationAddress".equalsIgnoreCase(e
                    .getMessage())) {
                Log.i("xuexue", "messaging---11---Invalid FDN destinationAddress---->");
                throw new IllegalArgumentException("Invalid FDN destinationAddress" + e);
            }
        }catch (final Exception e) {
            throw new SmsException("SmsSender: caught exception in sending " + e);
        }
    }

    private static Intent getSendStatusIntent(final Context context, final String action,
            final Uri requestUri, final int partId, final int subId) {
        // Encode requestId in intent data
        final Intent intent = new Intent(action, requestUri, context, SendStatusReceiver.class);
        intent.putExtra(SendStatusReceiver.EXTRA_PART_ID, partId);
        intent.putExtra(SendStatusReceiver.EXTRA_SUB_ID, subId);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);//for 812214
        return intent;
    }

    /*Add by SPRD for Bug:562203 Encode Type feature  Start */
    private static ArrayList<String> divideMessageFor16Bit(String text) {
        int msgCount;
        int limit;
        int count = text.length() * 2;
        if (count > SmsMessage.MAX_USER_DATA_BYTES) {
            msgCount = (count + (SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER - 1))
                    / SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER;
        } else {
            msgCount = 1;
        }
        if (msgCount > 1) {
            limit = SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER;
        } else {
            limit = SmsMessage.MAX_USER_DATA_BYTES;
        }
        ArrayList<String> result = new ArrayList<String>(msgCount);
        int pos = 0; // Index in code units.
        int textLen = text.length();
        while (pos < textLen) {
            int nextPos = 0; // Counts code units.

            nextPos = pos + Math.min(limit / 2, textLen - pos);

            if ((nextPos <= pos) || (nextPos > textLen)) {
                Log.e(TAG, "fragmentText failed (" + pos + " >= " + nextPos
                        + " or " + nextPos + " >= " + textLen + ")");
                break;
            }
            result.add(text.substring(pos, nextPos));
            pos = nextPos;
        }
        return result;
    }
    /*Add by SPRD for Bug:562203 Encode Type feature  End */
}
