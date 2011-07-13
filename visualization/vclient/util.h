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

#ifndef UTILH
#define UTILH

// A bunch of utility functions that did not fit in elsewhere.

// Reset all signal handlers to their default value.
void reset_sighandler();

// Write the pid of the running process to the a file.
void write_pid(const char *filename);

// Remove the PID file.
void remove_pid(const char *filename);

int read_file(const char *filename, char *dest, int dest_len);

#endif /* UTILH */

/* vim:set sw=2 ts=8 expandtab: */
