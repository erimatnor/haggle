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

#ifndef READLNH
#define READLNH

#define READLN_BUFSIZE  512

/** A buffer for storing excessive read data from a file descriptor.
 */
struct readln_state {
  /** The file descriptor that will be read/has been read from.
   */
  int fd;
  /** The buffer with excessive data that has not been processed yet.
   */
  char buf[READLN_BUFSIZE];
  /** Length of the buffer.
   */
  int buf_len;
  /** A flag indicating if the fd has been set to non-blocking mode yet.
   */
  int nonblocking;
};

int readln(struct readln_state *state, char *dest, int dest_len);

#endif /* READLNH */

/* vim:set sw=2 ts=8 expandtab: */
