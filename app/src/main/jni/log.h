#pragma once

#include <android/log.h>

extern bool g_debug_enabled;

#define LOG_TAG "mpv"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGV(...)                                                          \
    do                                                                      \
    {                                                                       \
        if (g_debug_enabled)                                                \
            __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__); \
    } while (0)

__attribute__((noreturn)) void die(const char *msg);

#define CHECK_MPV_INIT()                      \
    do                                        \
    {                                         \
        if (__builtin_expect(!g_mpv, 0))      \
            die("libmpv is not initialized"); \
    } while (0)
