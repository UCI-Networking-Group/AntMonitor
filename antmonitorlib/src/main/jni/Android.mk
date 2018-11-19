LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := antMonitorNative
LOCAL_SRC_FILES := stringMatcher.c \
                    tunReadWrite.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_CFLAGS := -Wall -g

include $(BUILD_SHARED_LIBRARY)
