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

#include <signal.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>

#include "util.h"
#include "log.h"

#ifndef NSIG
#define NSIG 32
#endif

/** Reset all signal handlers to their default value.
 */
void reset_sighandler() {
  int sig;

  for (sig=1;sig<NSIG;sig++) {
    signal(sig, SIG_DFL);
  }
}

/** Write the pid of the running process to the a file.
 */
void write_pid(const char *filename) {
  pid_t pid = getpid();
  FILE *fh;

  fh = fopen(filename, "w");
  if (fh == NULL) {
    LOG(LOG_WARN, "Failed to write PID to file '%s'. Check your permissions.\n", filename);
    return;
  }
  fprintf(fh, "%d", pid);

  fclose(fh);
}

/** Remove the PID file.
 */
void remove_pid(const char *filename) {
  unlink(filename);
}

/** Read the first line from a file.
 *
 * Opens the file, reads the first line and copies it into dest and
 * closes the file again.
 *
 * \param filename Name of the file to read from
 * \param dest Destination buffer
 * \param dest_len Length of destination buffer.
 */
int read_file(const char *filename, char *dest, int dest_len) {
  FILE *f;
  char format[20];

  f = fopen(filename, "r");
  if (!f) {
    LOG(LOG_WARN, "Failed to open file '%s' for reading.\n", filename);
    return -1;
  }

  sprintf(format, "%%%ds", dest_len);
  fscanf(f, format, dest);

  fclose(f);
  return 0;
}

/* vim:set sw=2 ts=8 expandtab: */
