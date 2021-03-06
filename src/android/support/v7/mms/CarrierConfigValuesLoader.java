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

package androidx.appcompat.mms;

import android.os.Bundle;

/**
 * Loader for carrier dependent configuration values
 */
public interface CarrierConfigValuesLoader {
    /**
     * Get the carrier config values in a bundle
     *
     * @param subId the associated subscription ID for the carrier configuration
     * @return a bundle of all the values
     */
    Bundle get(int subId);

    // Configuration keys and default values

    /** Boolean value: if MMS is enabled */
    public static final String CONFIG_ENABLED_MMS = "enabledMMS";
    public static final boolean CONFIG_ENABLED_MMS_DEFAULT = true;
    /**
     * Boolean value: if transaction ID should be appended to
     * the download URL of a single segment WAP push message
     */
    public static final String CONFIG_ENABLED_TRANS_ID = "enabledTransID";
    public static final boolean CONFIG_ENABLED_TRANS_ID_DEFAULT = false;
    /**
     * Boolean value: if acknowledge or notify response to a download
     * should be sent to the WAP push message's download URL
     */
    public static final String CONFIG_ENABLED_NOTIFY_WAP_MMSC = "enabledNotifyWapMMSC";
    public static final boolean CONFIG_ENABLED_NOTIFY_WAP_MMSC_DEFAULT = false;
    /**
     * Boolean value: if phone number alias can be used
     */
    public static final String CONFIG_ALIAS_ENABLED = "aliasEnabled";
    public static final boolean CONFIG_ALIAS_ENABLED_DEFAULT = false;
    /**
     * Boolean value: if audio is allowed in attachment
     */
    public static final String CONFIG_ALLOW_ATTACH_AUDIO = "allowAttachAudio";
    public static final boolean CONFIG_ALLOW_ATTACH_AUDIO_DEFAULT = true;
    /**
     * Boolean value: if true, long sms messages are always sent as multi-part sms
     * messages, with no checked limit on the number of segments. If false, then
     * as soon as the user types a message longer than a single segment (i.e. 140 chars),
     * the message will turn into and be sent as an mms message or separate,
     * independent SMS messages (dependent on CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES flag).
     * This feature exists for carriers that don't support multi-part sms.
     */
    public static final String CONFIG_ENABLE_MULTIPART_SMS = "enableMultipartSMS";
    public static final boolean CONFIG_ENABLE_MULTIPART_SMS_DEFAULT = true;
    /**
     * Boolean value: if SMS delivery report is supported
     */
    public static final String CONFIG_ENABLE_SMS_DELIVERY_REPORTS = "enableSMSDeliveryReports";
    public static final boolean CONFIG_ENABLE_SMS_DELIVERY_REPORTS_DEFAULT = true;
    /**
     * Boolean value: if group MMS is supported
     */
    public static final String CONFIG_ENABLE_GROUP_MMS = "enableGroupMms";
    public static final boolean CONFIG_ENABLE_GROUP_MMS_DEFAULT = true;
    /**
     * Boolean value: if the content_disposition field of an MMS part should be parsed
     * Check wap-230-wsp-20010705-a.pdf, chapter 8.4.2.21. Most carriers support it except some.
     */
    public static final String CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION =
            "supportMmsContentDisposition";
    public static final boolean CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION_DEFAULT = true;
    /**
     * Boolean value: if the sms app should support a link to the system settings
     * where amber alerts are configured.
     */
    public static final String CONFIG_CELL_BROADCAST_APP_LINKS = "config_cellBroadcastAppLinks";
    public static final boolean CONFIG_CELL_BROADCAST_APP_LINKS_DEFAULT = true;
    /**
     * Boolean value: if multipart SMS should be sent as separate SMS messages
     */
    public static final String CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES =
            "sendMultipartSmsAsSeparateMessages";
    public static final boolean CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES_DEFAULT = false;
    /**
     * Boolean value: if MMS read report is supported
     */
    public static final String CONFIG_ENABLE_MMS_READ_REPORTS = "enableMMSReadReports";
    public static final boolean CONFIG_ENABLE_MMS_READ_REPORTS_DEFAULT = false;
    /**
     * Boolean value: if MMS delivery report is supported
     */
    public static final String CONFIG_ENABLE_MMS_DELIVERY_REPORTS = "enableMMSDeliveryReports";
    public static final boolean CONFIG_ENABLE_MMS_DELIVERY_REPORTS_DEFAULT = false;
    /**
     * Boolean value: if "charset" value is supported in the "Content-Type" HTTP header
     */
    public static final String CONFIG_SUPPORT_HTTP_CHARSET_HEADER = "supportHttpCharsetHeader";
    public static final boolean CONFIG_SUPPORT_HTTP_CHARSET_HEADER_DEFAULT = false;
    /**
     * Integer value: maximal MMS message size in bytes
     */
    public static final String CONFIG_MAX_MESSAGE_SIZE = "maxMessageSize";
    public static final int CONFIG_MAX_MESSAGE_SIZE_DEFAULT = 300 * 1024;
    /**
     * Integer value: maximal MMS image height in pixels
     */
    public static final String CONFIG_MAX_IMAGE_HEIGHT = "maxImageHeight";
    public static final int CONFIG_MAX_IMAGE_HEIGHT_DEFAULT = 480;
    /**
     * Integer value: maximal MMS image width in pixels
     */
    public static final String CONFIG_MAX_IMAGE_WIDTH = "maxImageWidth";
    public static final int CONFIG_MAX_IMAGE_WIDTH_DEFAULT = 640;
    /**
     * Integer value: limit on recipient list of an MMS message
     */
    public static final String CONFIG_RECIPIENT_LIMIT = "recipientLimit";
    public static final int CONFIG_RECIPIENT_LIMIT_DEFAULT = Integer.MAX_VALUE;
    /**
     * Integer value: HTTP socket timeout in milliseconds for MMS
     */
    public static final String CONFIG_HTTP_SOCKET_TIMEOUT = "httpSocketTimeout";
    public static final int CONFIG_HTTP_SOCKET_TIMEOUT_DEFAULT = 60 * 1000;
    /**
     * Integer value: minimal number of characters of an alias
     */
    public static final String CONFIG_ALIAS_MIN_CHARS = "aliasMinChars";
    public static final int CONFIG_ALIAS_MIN_CHARS_DEFAULT = 2;
    /**
     * Integer value: maximal number of characters of an alias
     */
    public static final String CONFIG_ALIAS_MAX_CHARS = "aliasMaxChars";
    public static final int CONFIG_ALIAS_MAX_CHARS_DEFAULT = 48;
    /**
     * Integer value: the threshold of number of SMS parts when an multipart SMS will be
     * converted into an MMS, e.g. if this is "4", when an multipart SMS message has 5
     * parts, then it will be sent as MMS message instead. "-1" indicates no such conversion
     * can happen.
     */
    public static final String CONFIG_SMS_TO_MMS_TEXT_THRESHOLD = "smsToMmsTextThreshold";
    public static final int CONFIG_SMS_TO_MMS_TEXT_THRESHOLD_DEFAULT = 10;
    /**
     * Integer value: the threshold of SMS length when it will be converted into an MMS.
     * "-1" indicates no such conversion can happen.
     */
    public static final String CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD =
            "smsToMmsTextLengthThreshold";
    public static final int CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD_DEFAULT = -1;
    /**
     * Integer value: maximal length in bytes of SMS message
     */
    public static final String CONFIG_MAX_MESSAGE_TEXT_SIZE = "maxMessageTextSize";
    public static final int CONFIG_MAX_MESSAGE_TEXT_SIZE_DEFAULT = -1;
    /**
     * Integer value: maximum number of characters allowed for mms subject
     */
    public static final String CONFIG_MAX_SUBJECT_LENGTH = "maxSubjectLength";
    public static final int CONFIG_MAX_SUBJECT_LENGTH_DEFAULT = 40;
    /**
     * String value: name for the user agent profile HTTP header
     */
    public static final String CONFIG_UA_PROF_TAG_NAME = "mUaProfTagName";
    public static final String CONFIG_UA_PROF_TAG_NAME_DEFAULT = "x-wap-profile";
    /**
     * String value: additional HTTP headers for MMS HTTP requests.
     * The format is
     * header_1:header_value_1|header_2:header_value_2|...
     * Each value can contain macros.
     */
    public static final String CONFIG_HTTP_PARAMS = "httpParams";
    public static final String CONFIG_HTTP_PARAMS_DEFAULT = null;
    /**
     * String value: number of email gateway
     */
    public static final String CONFIG_EMAIL_GATEWAY_NUMBER = "emailGatewayNumber";
    public static final String CONFIG_EMAIL_GATEWAY_NUMBER_DEFAULT = null;
    /**
     * String value: suffix for the NAI HTTP header value, e.g. ":pcs"
     * (NAI is used as authentication in HTTP headers for some carriers)
     */
    public static final String CONFIG_NAI_SUFFIX = "naiSuffix";
    public static final String CONFIG_NAI_SUFFIX_DEFAULT = null;

    /**
     * Boolean value: if SMS Retry Times is supported
     */
    public static final String CONFIG_ENABLE_SMS_RETRY_TIMES = "enableSMSRetryTimes";
    public static final boolean CONFIG_ENABLE_SMS_RETRY_TIMES_DEFAULT = false;

    /**
     * Integer value: maximal MMS txt file size in bytes
     */
    public static final String CONFIG_MAX_TXT_FILE_SIZE = "maxTxtFileSize";
    public static final int CONFIG_MAX_TXT_FILE_SIZE_DEFAULT = 3 * 1024;

    /* And by SPRD for Bug:530742 2016.02.02 Start */
    /**
     * Integer value: limit on shared image list of an MMS message
     */
    public static final String CONFIG_SHARED_IMAGE_LIMIT = "sharedImageLimit";
    public static final int CONFIG_SHARED_IMAGE_LIMIT_DEFAULT = 20;
    /* And by SPRD for Bug:530742 2016.02.02 End */

    /* Sprd add for sms merge forward begin */
    /**
     * Integer value: Sms merger forward max count limite
     */
    public static final String CONFIG_SMS_MERGE_FORWARD_MAX_TIMES = "smsmergeforwardMaxItem";
    public static final int CONFIG_SMS_MERGE_FORWARD_MAX_DEFAULT = 30;
    /**
     * Integer value: Sms merger forward min count limite
     */
    public static final String CONFIG_SMS_MERGE_FORWARD_MIN_TIMES = "smsmergeforwardMinItem";
    public static final int CONFIG_SMS_MERGE_FORWARD_MIN_DEFAULT = 2;
    /* Sprd add for sms merge forward end */
    // sprd #542214 start
    public static final String CONFIG_ENABLE_SMS_SAVE_TO_SIM = "enableSmsSaveSim";
    public static final boolean CONFIG_ENABLE_SMS_SAVE_TO_SIM_DEFAULT = true;
    // sprd #542214 end

    // sprd #596503 start
    public static final String SAVE_ATTACHMENTS_TO_EXTERNAL = "save_attachments_to_external";
    public static final boolean SAVE_ATTACHMENTS_TO_EXTERNAL_DEFAULT = false;
    // sprd #596503 end
    
    // sprd #601442 start
    public static final String FORWARD_MESSAGE_USING_SMIL = "forward_message_using_smil";
    public static final boolean FORWARD_MESSAGE_USING_SMIL_DEFAULT = false;

    public static final String BEEP_ON_CALL_STATE = "beep_oncall_state";
    public static final boolean BEEP_ON_CALL_STATE_DEFAULT = false;

    public static final String CONTENT_EDIT_ENABLED = "content_edit_enabled";
    public static final boolean CONTENT_EDIT_ENABLED_DEFAULT = false;

    public static final String IS_CMCC_PARAM = "is_cmcc_param";
    public static final boolean IS_CMCC_PARAM_DEFAULT = false;

    public static final String INTERNAL_MEMORY_USAGE_ENABLED = "internal_memory_usage_enabled";
    public static final boolean INTERNAL_MEMORY_USAGE_ENABLED_DEFAULT = false;

    public static final String SMSC_EDITEABLE = "smsc_editable";
    public static final boolean SMSC_EDITEABLE_DEFAULT = true;

    public static final String ENCODETYPE_PREFE_STATUS = "encodetype_prefe_status";
    public static final boolean ENCODETYPE_PREFE_STATUS_DEFAULT = false;
    // sprd #601442 end

    public static final String SEND_EMPTY_MESSAGE = "send_empty_message";
    public static final int SEND_EMPTY_MESSAGE_DEFAULT = 0;

    public static final String SELECT_MMS_SIZE = "select_mms_size";
    public static final boolean SELECT_MMS_SIZE_DEFAULT = false;

    //bug 740202 begin
    public static final String UNREAD_SMS_LIGHT_COLOR = "unread_sms_light_color";
    public static final String UNREAD_SMS_LIGHT_COLOR_DEFAULT = "blue";
    //bug 740202 end

    //change ro property to xml
    public static final String ENABLE_FDN_FILTER = "enable_fdn_filter";
    public static final boolean ENABLE_FDN_FILTER_DEFAULT = true;

    public static final String MESSAGING_OPERATOR = "messaging_operator";
    public static final String MESSAGING_OPERATOR_DEFAULT = "";

}
