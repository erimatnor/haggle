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

#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include "log.h"

#ifdef DEBUG
int log_level = LOG_ALL;
#else
int log_level = LOG_INFO | LOG_ERROR | LOG_WARN;
#endif

void _print_log(int level, const char *file, int line, char *fmt, ...) {
  va_list ap;
  FILE *out;
  char datetime[18];
  time_t tim;
  struct tm *tmp;

  if (!(level & log_level))
    return;

  time(&tim);
  tmp = localtime(&tim);
  if (tmp == NULL) {
    strcpy(datetime, "???");
  } else {
    strftime(datetime, sizeof(datetime), "%y/%m/%d %H:%M:%S", tmp);
  }
  
  if ((level & LOG_DEBUG) || (level & LOG_INFO)) {
    out = stdout;
  } else {
    out = stderr;
  }

  switch (level) {
  case LOG_DEBUG:
    fprintf(out, "[%s, DEBUG] ", datetime);
    break;
  case LOG_INFO:
    fprintf(out, "[%s, INFO] ", datetime);
    break;
  case LOG_WARN:
    fprintf(out, "[%s, WARNING] ", datetime);
    break;
  case LOG_ERROR:
    fprintf(out, "[%s, ERROR] ", datetime);
    break;
  }

#ifdef DEBUG
  fprintf(out, "(%s, %d) ", file, line);
#endif

  va_start(ap, fmt);
  vfprintf(out, fmt, ap);

  fflush(out);
}

int get_log_level() {
  return log_level;
}

int set_log_level(int lvl) {
  int old = log_level;

  log_level = lvl;

  return old;
}

/** Toggle vclient's debug output.
 */
void toggle_debug() {
  int lvl;

  lvl = get_log_level();
  if (lvl & LOG_DEBUG) {
    set_log_level(lvl & ~LOG_DEBUG);
    LOG(LOG_INFO, "Debug output disabled.\n");
  } else {
    set_log_level(lvl | LOG_DEBUG);
    LOG(LOG_INFO, "Debug output enabled.\n");
  }
}

/* vim:set sw=2 ts=8 expandtab: */
