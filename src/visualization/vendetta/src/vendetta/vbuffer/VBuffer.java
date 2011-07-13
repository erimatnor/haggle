/* Copyright (c) 2008 Uppsala Universitet.
 * All rights reserved.
 *
 * This file is part of Vendetta.
 *
 * Vendetta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Vendetta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vendetta.  If not, see <http://www.gnu.org/licenses/>.
 */
package vendetta.vbuffer;

import vendetta.Vendetta;


/**
 * The base class for playback buffers.
 *
 * A playback buffer is a source of log events, eventually handled to
 * Vendetta. Using playback buffer allows restoring the order of events
 * that have been received over the network out-of-order.
 * It also eases playing back from files.
 *
 * @see NetBuffer FileBuffer
 * @todo Use priority queue instead of linked list for O(log(n)) insertion time.
 * @todo Comment
 * @version $Id: VBuffer.java 1534 2008-07-11 09:12:25Z frhe4063 $
 */
abstract public class VBuffer implements Runnable {
    protected Thread thread;
    protected volatile boolean cont = false;
    protected volatile boolean paused = false;
    protected Event head = null;
    public long startTime = 0;

    /**
     * The time window of this buffer in milliseconds.
     *
     * Each log event is played out with a delay of this value to be
     * able to restore the order of events that have been received out
     * of order.
     */
    protected long bufferWindow = 1000;

    public VBuffer() {
        startTime = System.currentTimeMillis();

        //		thread = Thread.currentThread();
        thread = new Thread(this);
        cont = true;
    }

    /**
     * The "main loop".
     */
    abstract public void run();

    /**
     * Add a log event to the buffer.
     */
    abstract public void addEvent(String log, long time,
        Vendetta.NetworkType type);

    /**
     * Pause the execution.
     */
    public synchronized void pause() {
        if (paused) {
            return;
        }

        paused = true;

        if (head == null) {
            notify();
        } else {
            thread.interrupt();
        }
    }

    /**
     * Resume after a pause
     */
    public synchronized void resume() {
        if (!paused) {
            return;
        }

        paused = false;

        if (head == null) {
            notify();
        } else {
            thread.interrupt();
        }
    }

    public void clear() {
        head = null;
    }

    public synchronized void stop() {
        cont = false;

        if (head == null) {
            notify();
        } else {
            thread.interrupt();
        }
    }

    protected synchronized void stopSleeping() {
        thread.interrupt();
    }

    protected synchronized void stopWaiting() {
        notify();
    }

    public void start() {
        thread.start();
    }

    /**
     * An event to be added or played out.
     */
    protected class Event {
        /**
         * The log event message.
         */
        public String log;

        /**
         * The time the event was received in milliseconds since UNIX epoch.
         */
        public long time = 0;

        /**
         * q_next - is used when this LogEvent is in the eventqueue
         */
        public Event q_next;

        public Event(String log, long time) {
            this.log = log;
            this.time = time;
        }
    }
}
