/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *
 *  AntMonitor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  AntMonitor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with AntMonitor. If not, see <http://www.gnu.org/licenses/>.
 */
#include "include/aho_corasick.hpp"
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#define  LOG_TAG    "stringMatcher"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

jobject search (JNIEnv *env, const char *nativeString, int length);
void lower_case (char * s, unsigned int l);

// Global variables
aho_corasick::trie trie;
jobject list;
jmethodID list_method_add, list_method_clear;
const int NUM_BUF_SIZE = 6;
/* Buffer to keep position of found strings - biggest packet size is ~16k -> 5 digits and NULL
 * terminator */
char NUM_BUF[NUM_BUF_SIZE];

/* Method to be called from Java */
jobject
Java_edu_uci_calit2_antmonitor_lib_util_AhoCorasickInterface_searchNative(
        JNIEnv *env, jobject thiz, jobject buffer, jint packetSize )
{
    char *buf = (char*) env->GetDirectBufferAddress(buffer);

    return search(env, buf, packetSize);
}

/* Method to be called from Java */
JNIEXPORT jboolean JNICALL
Java_edu_uci_calit2_antmonitor_lib_util_AhoCorasickInterface_init(
        JNIEnv *env, jobject thiz, jobjectArray jstringArray )
{
    // Fill out Aho-Corasick trie
    int numStrs = env->GetArrayLength(jstringArray);
    for (unsigned int i = 0; i < numStrs; i++) {
        jstring string = (jstring) env->GetObjectArrayElement(jstringArray, i);
        const char *cstring = env->GetStringUTFChars(string, 0);
        // Don't forget to call `ReleaseStringUTFChars` when you're done.
        // Note: this is not being done now since then we can't access the string
        // upon finding the pattern

        trie.insert(cstring);
        //LOGD("Successfully added pattern  '%s'\n", cstring);

        // Cleanup
        env->ReleaseStringUTFChars(string, cstring);
        env->DeleteLocalRef(string);
    }

    // Prepare global references
    jclass listClass = env->FindClass("java/util/ArrayList");
    if(listClass == NULL)
    {
        return JNI_FALSE;
    }

    jmethodID init = env->GetMethodID(listClass, "<init>", "()V");
    // Create the list object
    jobject local_list = env->NewObject(listClass, init);
    // Z means boolean returned
    list_method_add = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    list_method_clear = env->GetMethodID(listClass, "clear", "()V");

    // create the global reference for list
    list = (jclass) env->NewGlobalRef(local_list);
    // delete the local reference
    env->DeleteLocalRef(local_list);

    return JNI_FALSE;
}

jobject search (JNIEnv *env, const char *nativeString, int length)
{
    // Clear existing list
    env->CallVoidMethod(list, list_method_clear);

    auto result = trie.parse_text(nativeString);

    for (const auto &item : result) {
        //LOGD("found: %s at %d", item.get_keyword().c_str(), item.get_end());

        // Add the found string
        jstring key = env->NewStringUTF(item.get_keyword().c_str());
        env->CallBooleanMethod(list, list_method_add, key);
        env->DeleteLocalRef(key);

        // Add the position the string was found at
        // TODO: with the new library, we can directly return starting position
        snprintf(NUM_BUF, NUM_BUF_SIZE, "%ld", item.get_end());
        key = env->NewStringUTF(NUM_BUF);
        env->CallBooleanMethod(list, list_method_add, key);
        env->DeleteLocalRef(key);
    }

    return list;
}

#ifdef __cplusplus
}
#endif