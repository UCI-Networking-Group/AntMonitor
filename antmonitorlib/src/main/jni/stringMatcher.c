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
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

jobjectArray search (JNIEnv* env, const char *nativeString, unsigned int length);
void lower_case (char * s, unsigned int l);

// Global variables
jobject list;
jmethodID list_method_add, list_method_clear;

/* Method to be called from Java */
jobject
Java_edu_uci_calit2_antmonitor_lib_util_AhoCorasickInterface_searchNative(
        JNIEnv* env, jobject thiz, jobject buffer, jint packetSize )
{
    char *buf = (char*) (*env)->GetDirectBufferAddress(env, buffer);

    return search(env, buf, packetSize);
}

/* Method to be called from Java */
jboolean
Java_edu_uci_calit2_antmonitor_lib_util_AhoCorasickInterface_init(
        JNIEnv* env, jobject thiz, jobjectArray jstringArray )
{
    // Prepare global references
    jclass listClass = (*env)->FindClass(env, "java/util/ArrayList");
    if(listClass == NULL)
    {
        return JNI_FALSE;
    }

    jmethodID init = (*env)->GetMethodID(env, listClass, "<init>", "()V");
    // Create the list object
    jobject local_list = (*env)->NewObject(env, listClass, init);
    list_method_add = (*env)->GetMethodID(env, listClass, "add",
                                          "(Ljava/lang/Object;)Z"); // Z means boolean returned
    list_method_clear = (*env)->GetMethodID(env, listClass, "clear", "()V");

    // create the global reference for list
    list = (jclass) (*env)->NewGlobalRef(env, local_list);
    // delete the local reference
    (*env)->DeleteLocalRef(env, local_list);

    return JNI_FALSE;
}

jobject search (JNIEnv* env, const char *nativeString, unsigned int length)
{
    // Clear existing list
    (*env)->CallVoidMethod(env, list, list_method_clear);

    return list;
}

/* Converts given string to lower characters */
void lower_case (char * s, unsigned int l)
{
    unsigned int i;
    for(i=0; i<l; i++)
        s[i] = tolower(s[i]);
}