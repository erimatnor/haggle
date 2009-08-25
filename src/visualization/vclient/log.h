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

#ifndef LOG_H
#define LOG_H

#define LOG_DEBUG	1
#define LOG_INFO	2
#define LOG_WARN	4
#define LOG_ERROR	8

#define LOG_ALL (LOG_DEBUG | LOG_INFO | LOG_WARN | LOG_ERROR)

#define LOG(l, ...)	_print_log(l, __FILE__, __LINE__, __VA_ARGS__);

void _print_log(int level, const char *file, int line, char *fmt, ...);
int get_log_level();
int set_log_level(int log_level);
void toggle_debug();

#endif /* LOG_H */

/* vim:set sw=2 ts=8 expandtab: */
