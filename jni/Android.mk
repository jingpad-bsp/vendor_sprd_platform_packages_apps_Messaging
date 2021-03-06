#  Copyright (C) 2015 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_STATIC_LIBRARIES += libgif-ex

# Link to Android logging (liblog.so) and dynamic linker (libdl.so) libraries
LOCAL_LDFLAGS := -llog -ldl

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libs/giflib

LOCAL_MODULE    := libgiftranscode

ifeq ($(strip $(TARGET_ARCH)),x86_64)
LOCAL_PRODUCT_MODULE := true
endif

LOCAL_SRC_FILES := GifTranscoder.cpp

LOCAL_CFLAGS += -Wall -Wno-unused-parameter -Wno-switch

LOCAL_SDK_VERSION := 8
LOCAL_NDK_STL_VARIANT := c++_static # LLVM libc++

include $(BUILD_SHARED_LIBRARY)

ifdef IDPT_MESSAGING
include $(call all-makefiles-under, $(LOCAL_PATH)/../libs)
endif