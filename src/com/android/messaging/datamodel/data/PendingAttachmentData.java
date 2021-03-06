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
package com.android.messaging.datamodel.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
// Add for bug 563344
import com.android.messaging.util.GlobleUtil;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.SafeAsyncTask;
import com.android.messaging.util.UriUtil;

import android.util.Log;
import com.sprd.messaging.drm.MessagingDrmSession;

/**
 * Represents a "pending" message part that acts as a placeholder for the actual attachment being
 * loaded. It handles the task to load and persist the attachment from a Uri to local scratch
 * folder. This item is not persisted to the database.
 */
public class PendingAttachmentData extends MessagePartData {
    /** The pending state. This is the initial state where we haven't started loading yet */
    public static final int STATE_PENDING = 0;

    /** The state for when we are currently loading the attachment to the scratch space */
    public static final int STATE_LOADING = 1;

    /** The attachment has been successfully loaded and no longer pending */
    public static final int STATE_LOADED = 2;

    /** The attachment failed to load */
    public static final int STATE_FAILED = 3;

    private static final int LOAD_MEDIA_TIME_LIMIT_MILLIS = 100 * 1000;  // 100s

    /** The current state of the pending attachment. Refer to the STATE_* states above */
    private int mCurrentState;

    private static final String TAG = "PendingAttachmentData";

    /**
     * Create a new instance of PendingAttachmentData with an output Uri.
     * @param sourceUri the source Uri of the attachment. The Uri maybe temporary or remote,
     * so we need to persist it to local storage.
     */
    protected PendingAttachmentData(final String caption, final String contentType,
            @NonNull final Uri sourceUri, final int width, final int height,
            final boolean onlySingleAttachment, final double attachmentSize) {
        super(caption, contentType, sourceUri, width, height, onlySingleAttachment, attachmentSize);
        mCurrentState = STATE_PENDING;
    }

    /**
     * Creates a pending attachment data that is able to load from the given source uri and
     * persist the media resource locally in the scratch folder.
     */
    public static PendingAttachmentData createPendingAttachmentData(final String contentType,
            final Uri sourceUri) {
        return createPendingAttachmentData(null, contentType, sourceUri, UNSPECIFIED_SIZE,
                UNSPECIFIED_SIZE);
    }

    public static PendingAttachmentData createPendingAttachmentData(final String caption,
            final String contentType, final Uri sourceUri, final int width, final int height) {
        Assert.isTrue(ContentType.isMediaType(contentType));
        return new PendingAttachmentData(caption, contentType, sourceUri, width, height,
                false /*onlySingleAttachment*/, 0);
    }

    public static PendingAttachmentData createPendingAttachmentData(final String caption,
            final String contentType, final Uri sourceUri, final int width, final int height,
            final boolean onlySingleAttachment) {
        Assert.isTrue(ContentType.isMediaType(contentType));
        return new PendingAttachmentData(caption, contentType, sourceUri, width, height,
                onlySingleAttachment, 0);
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    public void loadAttachmentForDraft(final DraftMessageData draftMessageData,
            final String bindingId) {
        if (mCurrentState != STATE_PENDING) {
            return;
        }
        mCurrentState = STATE_LOADING;

        // Kick off a SafeAsyncTask to load the content of the media and persist it locally.
        // Note: we need to persist the media locally even if it's not remote, because we
        // want to be able to resend the media in case the message failed to send.
        new SafeAsyncTask<Void, Void, MessagePartData>(LOAD_MEDIA_TIME_LIMIT_MILLIS,
                true /* cancelExecutionOnTimeout */) {
            @Override
            protected MessagePartData doInBackgroundTimed(final Void... params) {
                final Uri contentUri = getContentUri();
                final String contentType= getContentType();
                String drmOrigMimetype = null;
                Uri persistedUri = null;
                if (ContentType.isDrmType(contentType)){
	              try{
                         String path = MessagingDrmSession.get().getPath(contentUri);
	                  drmOrigMimetype = MessagingDrmSession.get().getDrmOrigMimeType(path, contentType);
			    persistedUri = contentUri;
	                  Log.d(TAG, "loadAttachmentForDraft  drm: contentUri "+contentUri+" path "+path+" drmOrigMimetype "+drmOrigMimetype);
	              }catch(Exception ex){
	                  Log.d(TAG, "loadAttachmentForDraft  drm: ex "+ex);
	                  persistedUri = contentUri;
	              }
                }else{
                    //-- Add by SPRD for bug 618360  Start
                    //add for bug 774769  start
                    if(ContentType.isVCardType(contentType)){
                        //ContentType.isImageType(contentType)||ContentType.isAudioType(contentType)
                        //                            ||ContentType.isVideoType(contentType)||
                        persistedUri=contentUri;
                    }else{
                        persistedUri =  UriUtil.persistContentToScratchSpace(contentUri);
                    }
                    //add for bug 774769  end
                    //-- Add by SPRD for bug 618360  end --
                     Log.d(TAG, "loadAttachmentForDraft : contentUri "+contentUri+" persistedUri"+persistedUri);
                }
                if (persistedUri != null) {
                    MessagePartData part =  MessagePartData.createMediaMessagePart(
                            getText(),
                            getContentType(),
                            persistedUri,
                            getWidth(),
                            getHeight(),
                            getmAttachmetSize());
	             //if (drmOrigMimetype != null){
                        //part.setDrmOrigContentType(drmOrigMimetype);
                    //}
                    // Add by SPRD for bug 563344
                    GlobleUtil.contentUriMap.put(persistedUri.toString(), contentUri.toString());
                    part.setIsCompressed(1);// add for bug 725726
                    return part;
                }
                return null;
            }

            @Override
            protected void onCancelled() {
                LogUtil.w(LogUtil.BUGLE_TAG, "Timeout while retrieving media");
                mCurrentState = STATE_FAILED;
                if (draftMessageData.isBound(bindingId)) {
                    draftMessageData.removePendingAttachment(PendingAttachmentData.this);
                }
            }

            @Override
            protected void onPostExecute(final MessagePartData attachment) {
                if (attachment != null) {
                    mCurrentState = STATE_LOADED;
                    /* SPRD: modified for bug 503072 begin */
                    /*if (draftMessageData.isBound(bindingId)) {
                        draftMessageData.updatePendingAttachment(attachment,
                                PendingAttachmentData.this);
                    } else {
                        // The draft message data is no longer bound, drop the loaded attachment.
                        attachment.destroyAsync();
                   }*/
                    draftMessageData.updatePendingAttachment(attachment,
                            PendingAttachmentData.this);
                    /* SPRD: modified for bug 503072 end */
                } else {
                    // Media load failed. We already logged in doInBackground() so don't need to
                    // do that again.
                    mCurrentState = STATE_FAILED;
                    if (draftMessageData.isBound(bindingId)) {
                        draftMessageData.onPendingAttachmentLoadFailed(PendingAttachmentData.this);
                        draftMessageData.removePendingAttachment(PendingAttachmentData.this);
                    }
                }
            }
        }.executeOnThreadPool();
    }

    protected PendingAttachmentData(final Parcel in) {
        super(in);
        mCurrentState = in.readInt();
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(mCurrentState);
    }

    public static final Parcelable.Creator<PendingAttachmentData> CREATOR
        = new Parcelable.Creator<PendingAttachmentData>() {
            @Override
            public PendingAttachmentData createFromParcel(final Parcel in) {
                return new PendingAttachmentData(in);
            }

            @Override
            public PendingAttachmentData[] newArray(final int size) {
                return new PendingAttachmentData[size];
            }
        };
}
