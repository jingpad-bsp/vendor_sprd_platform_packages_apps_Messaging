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
package com.android.messaging.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessagePartData;

/**
 * Shows a view for a "person" - could be a contact or a participant. This always shows a
 * contact icon on the left, and the person's display name on the right.
 *
 * This view is always bound to an abstract PersonItemData class, so to use it for a specific
 * scenario, all you need to do is to create a concrete PersonItemData subclass that bridges
 * between the underlying data (e.g. ParticipantData) and what the UI wants (e.g. display name).
 */
public class OtherItemView extends LinearLayout {
    public interface OtherItemViewListener {
        void onCalendarClicked();

        boolean onCalendarLongClicked();
    }

    private TextView mNameTextView;
    private TextView mDetailsTextView;
    private ImageView mOthermarkView;
    private OtherItemViewListener mListener;

    public OtherItemView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(getContext()).inflate(R.layout.other_item_view,
                this, true);
    }

    @Override
    protected void onFinishInflate() {
        mNameTextView = (TextView) findViewById(R.id.name);
        //mDetailsTextView = (TextView) findViewById(R.id.details);
        mOthermarkView = (ImageView) findViewById(R.id.other_checkmark);
    }

    public void bindMessagePartData(final MessagePartData messagePartData,
            final boolean incoming) {
        // Assert.isTrue(messagePartData == null ||
        // ContentType.isOtherType(messagePartData.getContentType()));
        // final Uri contentUri = (messagePartData == null) ? null :
        // messagePartData.getContentUri();
        // bind(contentUri, incoming);
        // setListener(this);
    }

    public void setListener(final OtherItemViewListener listener) {
        mListener = listener;
        if (mListener == null) {
            return;
        }
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mListener != null) {
                    mListener.onCalendarClicked();
                }
            }
        });
        final OnLongClickListener onLongClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    return mListener.onCalendarLongClicked();
                }
                return false;
            }
        };
        setOnLongClickListener(onLongClickListener);
    }

}
