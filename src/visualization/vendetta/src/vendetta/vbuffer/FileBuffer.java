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

import vendetta.Vendetta.NetworkType;

import vendetta.gui.FileModePanel;

import vendetta.util.log.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


/**
 * A playback buffer for replaying events from a file.
 *
 * This is used if Vendetta is run in file mode.
 *
 * @version $Id: FileBuffer.java 1520 2008-06-02 14:53:23Z frhe4063 $
 */
public class FileBuffer extends VBuffer {
    private static final Log LOG = Log.getInstance("FileBuffer");
    private FileModePanel panel = null;
    private BufferedReader infile = null;

    /**
     * currentTime - starts at 0 and increases by speed
     */
    private long currentTime;

    /**
     * number of millis since the log was created
     */
    private long logStartTime;

    /**
     * number of millis since the log was created
     */
    private long logEndTime;

    /**
     * number of millis since the log was created
     */
    private long offset;

    /**
     * Playback speed, maybe 100 == 1-1 ratio
     */
    private long speed = 50;
    private String last_log = null;
    private long last_time = 0;
    private boolean step = false;

    public FileBuffer() {
        super();
        paused = true;
    }

    public void setFileModePanel(FileModePanel panel, long initTime,
        long endTime) {
        this.panel = panel;
        this.currentTime = 0;
        this.logStartTime = initTime;
        this.logEndTime = endTime;
    }

    public void loadFile(String path) throws Exception {
        currentTime = 0;
        Vendetta.clearAllNodes();
        infile = new BufferedReader(new FileReader(path));
    }

    /**
     * Loads events, returns true at EOF
     */
    private boolean loadLogEvents() {
        if (infile == null) {
            LOG.error("No log file loaded.");
            paused = true;

            return true;
        }

        // Did we skip a message the last call?
        if (last_log != null) {
            // Is it still to far away in the future?
            if (last_time > (currentTimeMillis() + bufferWindow)) {
                return false;
            }

            // No, let's add it
            addEvent(last_log, last_time, NetworkType.NA);
            last_log = null;
        }

        String line = "";
        long time = 0;

        while (true) {
            try {
                line = infile.readLine();
            } catch (IOException e) {
                LOG.error("Cannot load log event.", e);
                line = null;
            }

            if (line == null) {
                return true;
            }

            time = Long.parseLong(line.split(" ", 2)[0]);

            // Shall we continue to add events?
            if (offset > time) {
            } else if (time > (currentTimeMillis() + bufferWindow)) {
                // remember this log entry
                last_log = line;
                last_time = time;

                break;
            }

            // add the event
            addEvent(line, time, NetworkType.NA);
        }

        return false;
    }

    /**
     * Resume after a pause
     */
    public synchronized void resume(long offset) {
        if (!paused) {
            return;
        }

        paused = false;
        step = false;
        this.offset = logStartTime + offset;

        if (head == null) {
            notify();
        } else {
            thread.interrupt();
        }
    }

    /**
     * Resume until next event, then pause
     */
    public synchronized void step(long offset) {
        step = true;

        if (!paused) {
            return;
        }

        this.offset = logStartTime + offset;
        paused = false;

        if (head == null) {
            notify();
        } else {
            thread.interrupt();
        }
    }

    private long currentTimeMillis() {
        return currentTime + logStartTime;
    }

    public void run() {
        while (cont) {
            try {
                if (paused) {
                    synchronized (this) {
                        if (panel != null) {
                            panel.pauseButtons();
                        }

                        wait();
                    }

                    // We either pressed start or "quit"
                } else {
                    // Add some events
                    loadLogEvents();

                    if ((step == false) &&
                            (offset < (logStartTime + currentTime))) {
                        Thread.sleep(speed);
                    }

                    currentTime += speed;
                    panel.setSliderValue(currentTime);

                    // Handle the event
                    if ((head != null) && cont && !paused &&
                            (currentTimeMillis() >= head.time)) {
                        System.out.println("(FileBuffer-Event) " + head.log);
                        Vendetta.handleLogEvent(head.log);
                        head = head.q_next;

                        if (currentTime > logEndTime) {
                            paused = true;

                            continue;
                        } else if (step) {
                            paused = true;
                        }
                    }
                }
            } catch (InterruptedException e) {
                // An event was probably added
            }
        }

        debug("Stopped");
    }

    /**
     * Add a log event to the buffer
     */
    public void addEvent(String log, long time, Vendetta.NetworkType type) {
        Event event = new Event(log, time);

        // Empty queue?
        if (head == null) {
            head = event;

            // We need to stop the waiting
            stopWaiting();
        } else if (event.time < head.time) {
            event.q_next = head;
            head = event;

            // We need to set a new delay
            stopSleeping();
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
    }

    protected void debug(String m) {
        LOG.debug(m);
    }
}
