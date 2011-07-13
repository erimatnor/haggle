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

#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

#include "globals.h"
#include "readln.h"
#include "log.h"

/** Create the FIFO from which the application will read position information
 */
int position_create_fifo() {
  if (mkfifo(POSITION_FIFO, 0666)) {
    LOG(LOG_WARN, "Cannot create position FIFO: %s\n", strerror(errno));
    return -1;
  }

  return 0;
}

/** Delete the position FIFO that was created using position_create_fifo()
 */
int position_delete_fifo() {
  if (unlink(POSITION_FIFO)) {
    LOG(LOG_WARN, "Cannot delete position FIFO: %s\n", strerror(errno));
    return -1;
  }

  return 0;
}

/** Open the FIFO that holds updated position information.
 */
int position_open_fifo() {
  // Note: O_NONBLOCK is required here since otherwise open would block
  // until data is available on the FIFO!
  vcr.position_fifo_fd = open(POSITION_FIFO, O_NONBLOCK, O_RDWR);
  if (vcr.position_fifo_fd < 0) {
	LOG(LOG_ERROR, "Failed to open position FIFO.\n");
	return -1;
  }

  fcntl(vcr.position_fifo_fd, F_SETFD, FD_CLOEXEC);
  return vcr.position_fifo_fd;
}

/** Read updated position information from the FIFO.
 */
void position_read_fifo() {
  static struct readln_state fd_state;
  char buf[MAXLOGLINELEN];
  int status;

  fd_state.fd = vcr.position_fifo_fd;

  status = readln(&fd_state, buf, MAXLOGLINELEN);
  
  if (status > 0) {
	float x, y, z;
	if (sscanf(buf, "%f, %f, %f", &x, &y, &z) == 3) {
		vcr.position.x = x;
		vcr.position.y = y;
		vcr.position.z = z;

		LOG(LOG_DEBUG, "Position updated to (%f, %f, %f) from FIFO.\n",
				x, y, z);
	} else {
		LOG(LOG_WARN, "Malformed position from FIFO.\n");
	}
  } else if (status == -1) {
    LOG(LOG_DEBUG, "Position FIFO was closed, reopening.\n");
	vcr.position_fifo_fd = position_open_fifo();
  }
}

/** Read position information from a file.
 *
 * This function is called on initialization of vclient.
 */
int position_read_from_file(const char *filename) {
  FILE *f;
  float x, y, z;

  f = fopen(filename, "r");
  if (!f) {
    LOG(LOG_WARN, "Cannot open position file '%s' for reading.\n", filename);
    return -1;
  }

  if (fscanf(f, "%f,%f,%f\n", &x, &y, &z) == 3) {
    vcr.position.x = x;
    vcr.position.y = y;
    vcr.position.z = z;
    fclose(f);
  } else {
    LOG(LOG_DEBUG, "Malformed position information in file '%s'.\n", filename);
    fclose(f);
    return -1;
  }

  return 0;  
}

/** Write position information to a file.
 *
 * This function is called on shutdown of vclient.
 */
int position_write_to_file(const char *filename) {
  FILE *f; 

  f = fopen(filename, "w");
  if (!f) {
    LOG(LOG_WARN, "Cannot open position file '%s' for writing.\n", filename);
    return -1;
  }

  fprintf(f, "%f,%f,%f\n", vcr.position.x, vcr.position.y, vcr.position.z);
  fclose(f);

  return 0;
}

/* vim:set sw=2 ts=8 expandtab: */
