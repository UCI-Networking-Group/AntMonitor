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
#include <poll.h>
#include <jni.h>
#include <unistd.h>
#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Read from tun */
jint Java_edu_uci_calit2_antmonitor_lib_vpn_TunNativeInterface_pollRead
(JNIEnv *env, jobject thisObj, jint fd, jlong timeout, jobject buffer, jint bufferHeaderSize) {
    int ERROR_VALUE = -1;
    int TIMEOUT_VALUE = 0;

    // Prepare the poll structure
    struct pollfd arrayfds[1];
    arrayfds[0].fd = (int) fd;
    arrayfds[0].events = POLLIN;

    // Perform the poll
    int retval;
    retval = poll(arrayfds, 1, (long) timeout);
    if (retval == -1) { // error
        return ERROR_VALUE;
    } else if (retval == 0) { // timeout
        return TIMEOUT_VALUE;
    } else {
        if (arrayfds[0].revents & POLLIN) { // there is data to read

            char *buf = (char*) env->GetDirectBufferAddress(buffer);
            long capacity = (long) env->GetDirectBufferCapacity(buffer);

            // Sanity check
            int headerSize = (int) bufferHeaderSize;
            if (headerSize > capacity) {
                return ERROR_VALUE;
            }

            // Advance pointer to skip the header
            int i;
            for (i=0; i<headerSize; i++) {
                buf++;
            }

            // Read up to the remaining capacity
            long remainCapacity = capacity - (long) headerSize;
            int bytesRead = read((int) fd, buf, (size_t) remainCapacity);

            return bytesRead;


        } else { // other events Error (POLLERR), Hangup (POLLHUP), Invalid request (POLLNVAL)
            return ERROR_VALUE;
        }
    }
}

/* Write to tun */
jint Java_edu_uci_calit2_antmonitor_lib_vpn_TunNativeInterface_write
(JNIEnv *env, jobject thisObj, jint fd, jobject buffer, jint length) {

    char *buf = (char*) env->GetDirectBufferAddress(buffer);
    int bytesWritten = write((int) fd, buf, (size_t) length);
    return bytesWritten;
}

#ifdef __cplusplus
}
#endif
