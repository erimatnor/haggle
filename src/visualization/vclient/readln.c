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

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>

#include "log.h"
#include "readln.h"

/**
 * \file readln.c
 *       Reading a complete line from a file descriptor without
 *       blocking and without losing data across subsequent calls.
 * \todo Rewrite using fdopen() and fgetc()/fungetc(). I just
 *       learned they exist :/
 */

/** Try to read a line from a file descriptor.
 *
 * Note that this function will set the file descriptor to
 * non-blocking.
 * 
 * \param state Contains the file descriptor to read from as well
 *              as excessive read data from last call to readln()
 *              are stored in `state'. `state' should be
 *              static in the caller's scope.
 * \param dest The destination buffer.
 * \param dest_len The length of the destination buffer.
 *
 * \return -2 if an error occurred,
 *         -1 if the fd to read from was closed,
 *          0 if not enough input was available, 
 *          1 if one line was read, you may try again.
 */
int readln(struct readln_state *state, char *dest, int dest_len) {
  long flags;
  ssize_t n;
  int i, newline_pos = -1, copy_len;

  if (state->fd < 0) {
    LOG(LOG_DEBUG, "Ignoring invalid fd: %d\n", state->fd);
    return -1;
  }

  if (!state->nonblocking) {
    // Make sure the fd is non-blocking.
    flags = fcntl(state->fd, F_GETFL);
    flags |= O_NONBLOCK;
    if (fcntl(state->fd, F_SETFL, flags) < 0) {
      LOG(LOG_ERROR, "Failed to set fd to non-blocking! The program might lock.\n");
      return -2;
    }
    state->nonblocking = 1;
  }

  if (state->buf_len == READLN_BUFSIZE) {
    LOG(LOG_WARN, "Discarding too long log line!\n");
    state->buf_len = 0;
  }

  n = read(state->fd, state->buf+state->buf_len, READLN_BUFSIZE-state->buf_len);
  if (n < 0) {
    if (errno == EINTR || errno == EAGAIN) {
      // We don't consider these to be errors.
      n = 0;
    } else {
      LOG(LOG_ERROR, "readln() Failed reading: %s.\n", strerror(errno));
      return -2;
    }
  } else if (n == 0) {
    // End of file.
    state->fd = -1;
    return -1;
  }

  state->buf_len += n;

  for (i=0;i<state->buf_len;i++) {
    if (state->buf[i] == '\n') {
      newline_pos = i;
      break;
    }
  }

  if (newline_pos == -1) {
    // No newline yet.
    return 0;
  }

  copy_len = (dest_len-1 < newline_pos+1) ? dest_len-1 : newline_pos+1;
  memcpy(dest, state->buf, copy_len);
  dest[copy_len] = '\0';

  memmove(state->buf, state->buf+copy_len, READLN_BUFSIZE-copy_len);
  state->buf_len -= copy_len;

  return 1;
}

/* vim:set sw=2 ts=8 expandtab: */
