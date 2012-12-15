APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi armeabi-v7a
APP_MODULES := \
	libhaggle_jni
APP_PLATFORM := android-10
APP_OPTIM=release
APP_BUILD_SCRIPT=jni/Android.mk
