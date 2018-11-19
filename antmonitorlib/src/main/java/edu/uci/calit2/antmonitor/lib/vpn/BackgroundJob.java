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
package edu.uci.calit2.antmonitor.lib.vpn;

/**
 * Combines a {@link Thread} and a {@link Runnable} such that one may access the
 * {@code Runnable}, i.e. the <i>job</i>, carried out by the {@code Thread} given only the thread
 * instance.
 * @param <JOB> The type of {@link Runnable} that can be run on this {@code BackgroundJob}
 *
 * @author Simon Langhoff, Janus Varmarken
 */
class BackgroundJob<JOB extends Runnable> extends Thread {

    /**
     * The job carried out by this thread.
     */
    private final JOB mJob;

    /**
     * Creates a new {@code BackgroundJob} designated to carry out the given job.
     * @param job The job/task that this thread should carry out.
     */
    public BackgroundJob(JOB job) {
        super(job);
        this.mJob = job;
    }

    /**
     * Creates a new {@code BackgroundJob} designated to carry out the given job and named according to the given name.
     * @param job The job/task that this thread should carry out.
     * @param threadName The name of the thread carrying out the {@code job}.
     */
    public BackgroundJob(JOB job, String threadName) {
        super(job, threadName);
        this.mJob = job;
    }

    /**
     * Gets a reference to the job/task carried out by this thread.
     * @return the job/task carried out by this thread.
     */
    public JOB getJob() {
        return mJob;
    }

}
