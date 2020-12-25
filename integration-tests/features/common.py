#!/usr/bin/env python
# -*- mode: python; coding: iso-8859-1 -*-

# Sonar C++ Plugin (Community)
# Copyright (C) Waleri Enns
# dev@sonar.codehaus.org

# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.

# You should have received a copy of the GNU Lesser General Public
# License along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02

import re
import os
import sys
import requests
import json
import time

from requests.auth import HTTPBasicAuth

SONAR_ERROR_RE = re.compile(".* ERROR .*")
SONAR_WARN_RE = re.compile(".* WARN .*")
SONAR_WARN_TO_IGNORE_RE = re.compile(".*H2 database should.*|.*Starting search|.*Starting web")
SONAR_LOG_FOLDER = "logs"

RED = ""
YELLOW = ""
GREEN = ""
RESET = ""
RESET_ALL = ""
BRIGHT = ""
try:
    import colorama
    colorama.init()
    RED = colorama.Fore.RED
    YELLOW = colorama.Fore.YELLOW
    GREEN = colorama.Fore.GREEN
    RESET = colorama.Fore.RESET
    BRIGHT = colorama.Style.BRIGHT
    RESET_ALL = colorama.Style.RESET_ALL
    if os.environ.get("APPVEYOR"):
        # AppVeyor does handle the escape sequences
        colorama.deinit()    
except ImportError:
    pass

INDENT = "    "    
SONAR_URL = "http://localhost:9000"

def get_sonar_log_folder(sonarhome):
    return os.path.join(sonarhome, SONAR_LOG_FOLDER)

def get_sonar_log_file(sonarhome):
    SONAR_LOG_FILE = "sonar-" + time.strftime("%Y%m%d") + ".log"
    return os.path.join(get_sonar_log_folder(sonarhome), SONAR_LOG_FILE)

def sonar_analysis_finished(logpath):
    url = ""

    print(BRIGHT + "    Read Log : " + logpath + RESET_ALL)

    try:
        with open(logpath, "r") as log:
            lines = log.readlines()
            url = get_url_from_log(lines)
    except IOError, e:
        pass

    print(BRIGHT + "     Get Analysis In Background : " + url + RESET_ALL)

    if url == "":
        return ""

    status = ""
    while True:
        time.sleep(1)
        response = requests.get(url, auth=HTTPBasicAuth('admin', 'admin'))
        if not response.text:
            print(BRIGHT + "     CURRENT STATUS : no response" + RESET_ALL)
            continue
        task = json.loads(response.text).get("task", None)
        if not task:
            print(BRIGHT + "     CURRENT STATUS : ?" + RESET_ALL)
            continue
        print(BRIGHT + "     CURRENT STATUS : " + task["status"] + RESET_ALL)
        if task["status"] == "IN_PROGRESS" or task["status"] == "PENDING":
            continue

        if task["status"] == "SUCCESS":
            break
        if task["status"] == "FAILED":
            status = "BACKGROUND TASK AS FAILED. CHECK SERVER : " + logpath + ".server"
            break

    serverlogurl = url.replace("task?id", "logs?taskId")
    r = requests.get(serverlogurl, auth=HTTPBasicAuth('admin', 'admin'), timeout=10)

    writepath = logpath + ".server"
    f = open(writepath, 'w')
    f.write(r.text)
    f.close()

#    print(BRIGHT + " LOG: " + r.text + RESET_ALL)

    return status

def cleanup_logs(sonarhome):
    sys.stdout.write(INDENT + "cleaning logs ... ")
    sys.stdout.flush()
    try:
        logpath = get_sonar_log_folder(sonarhome)
        filelist = [ f for f in os.listdir(logpath) if f.endswith(".log") ]
        for f in filelist:
            os.remove(os.path.join(logpath, f))
    except OSError:
        pass
    sys.stdout.write(GREEN + "OK\n" + RESET)

def print_logs(sonarhome):
    sys.stdout.write(INDENT + "print logs ... \n")
    sys.stdout.flush()
    try:
        logpath = get_sonar_log_folder(sonarhome)
        filelist = [ f for f in os.listdir(logpath) if f.endswith(".log") ]
        for f in filelist:
            sys.stdout.write("\n--- " + f + " ---\n")
            with open(os.path.join(logpath, f), 'r') as file:
                sys.stdout.write(file.read());
    except OSError:
        pass
    sys.stdout.write("\n")

def analyse_log(logpath, toignore=None):
    badlines = []
    errors = warnings = 0

    try:
        with open(logpath, "r") as log:
            lines = log.readlines()
            badlines, errors, warnings = analyse_log_lines(lines, toignore)
    except IOError, e:
        badlines.append(str(e) + "\n")

    return badlines, errors, warnings

def get_url_from_log(lines):
    url = ""
    for line in lines:
        if "INFO: More about the report processing at" in line:
            url = line.split("INFO: More about the report processing at")[1].strip()

        if "INFO  - More about the report processing at" in line:
            url = line.split("INFO  - More about the report processing at")[1].strip()

    return url

def analyse_log_lines(lines, toignore=None):
    badlines = []
    errors = warnings = 0
    toingore_re = None if toignore is None else re.compile(toignore)
    for line in lines:
        if is_sonar_error(line, toingore_re):
            badlines.append(line)
            errors += 1
        elif is_sonar_warning(line, toingore_re):
            badlines.append(line)
            warnings += 1

    return badlines, errors, warnings

def is_sonar_error(line, toignore_re):
    return (SONAR_ERROR_RE.match(line)
            and (toignore_re is None or not toignore_re.match(line)))

def is_sonar_warning(line, toignore_re):
    return (SONAR_WARN_RE.match(line)
            and not SONAR_WARN_TO_IGNORE_RE.match(line)
            and (toignore_re is None or not toignore_re.match(line)))

def build_regexp(multiline_str):
    lines = [line for line in multiline_str.split("\n") if line != '']
    return re.compile("|".join(lines))
