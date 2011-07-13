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

import vendetta.util.log.Log;


/**
 * A playback buffer for events received over the network.
 *
 * This is where events received over the upstream TCP or UDP connection
 * end up.
 *
 * @todo Comment
 * @todo Do not delay packets that are insanely out of time.
 * @version $Id: NetBuffer.java 1534 2008-07-11 09:12:25Z frhe4063 $
 */
public class NetBuffer extends VBuffer {
    private static final Log LOG = Log.getInstance("NetBuffer");

    /**
     * Create a new NetBuffer
     *
     * @param The buffer window
     */
    public NetBuffer(int buffer) {
        super();
        bufferWindow = buffer;
    }

    public void run() {
        long sleepTime = 100;
        long nextWakeTime = System.currentTimeMillis() + sleepTime;
        Event currentEvent = null;

        while (cont) {
            try {
                long now = System.currentTimeMillis();
                long timeLeft = nextWakeTime - now;

                if (currentEvent == null) {
                    currentEvent = getEvent();
                }

                if (paused) {
                    LOG.debug("PAUSED");
                    wait();
                    LOG.debug("RESUMED");
                } else if (currentEvent == null) {
                    if (timeLeft > 0) {
                        Thread.sleep(timeLeft);
                    }
                } else {
                    // 2000 - ( 1600 - 1500 )
                    long delay = bufferWindow - (now - currentEvent.time);

                    // too long lag or what? 
                    if (delay > timeLeft) {
                        if (timeLeft > 0) {
                            Thread.sleep(timeLeft);
                        }
                    } else {
                        if (delay < 1) {
                            // Vendetta.warning( "(NetBuffer)", "Delay < 1" );
                        } else {
                            Thread.sleep(delay);
                        }

                        // Handle the event
                        if (cont && !paused) {
                            Vendetta.handleLogEvent(currentEvent.log);
                            currentEvent = null;
                        }
                    }
                }

                now = System.currentTimeMillis();

                if (now > nextWakeTime) {
                    while (now > nextWakeTime)
                        nextWakeTime += sleepTime;

                    Vendetta.redraw();
                }
            } catch (InterruptedException e) {
            }
        }

        LOG.info("Stopped.");
    }

    public synchronized Event getEvent() {
        Event retval = head;

        if (head == null) {
            return null;
        }

        head = head.q_next;

        return retval;
    }

    /**
     * Add a log event to the buffer
     */
    public synchronized void addEvent(String log, long time,
        Vendetta.NetworkType type) {
        Event event = new Event(log, System.currentTimeMillis()); //time);

        // Empty queue?
        if (head == null) {
            head = event;
        } else if (event.time < head.time) {
            event.q_next = head;
            head = event;
        } else {
            Event ev = head;

            while (ev != null) {
                if (ev.q_next == null) {
                    // add to the tail
                    ev.q_next = event;

                    break;
                } else if (event.time < ev.q_next.time) {
                    event.q_next = ev.q_next;
                    ev.q_next = event;

                    break;
                }

                ev = ev.q_next;
            }
        }

        stopSleeping();
    }
}
