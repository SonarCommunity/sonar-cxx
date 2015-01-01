#!/usr/bin/env python
# -*- mode: python; coding: iso-8859-1 -*-

# SonarQube Python Plugin
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

import sys
import os
import re
import json
import requests
import subprocess
import shutil
from behave import given, when, then, model
from common import analyselog, build_regexp, sonarlog, analyseloglines


TESTDATADIR = os.path.normpath(os.path.join(os.path.realpath(__file__),
                                            "..", "..", "..", "testdata"))
SONAR_URL = "http://localhost:9000"


@given(u'the project "{project}"')
def step_impl(context, project):
    assert os.path.isdir(os.path.join(TESTDATADIR, project))
    context.project = project


@when(u'I run "{command}"')
def step_impl(context, command):
    context.log = "_%s_.log" % context.project

    sonarhome = os.environ.get("SONARHOME", None)
    if sonarhome:
        context.serverlog = sonarlog(sonarhome)
        if getattr(context, "serverlogfd", None) is not None:
            context.serverlogfd.close()
        context.serverlogfd = open(context.serverlog, "r")
        context.serverlogfd.seek(0, 2)
    else:
        context.serverlogfd = None

    projecthome = os.path.join(TESTDATADIR, context.project)
    with open(context.log, "w") as logfile:
        rc = subprocess.call(command,
                             cwd=projecthome,
                             stdout=logfile, stderr=logfile,
                             shell=True)
    context.rc = rc


@then(u'the analysis finishes successfully')
def step_impl(context):
    assert context.rc == 0, "Exit code is %i, but should be zero" % context.rc


@then(u'the analysis log contains no error/warning messages except those matching')
def step_impl(context):
    ignore_re = build_regexp(context.text)
    badlines, _errors, _warnings = analyselog(context.log, ignore_re)

    assert len(badlines) == 0,\
        ("Found following errors and/or warnings lines in the logfile:\n"
         + "".join(badlines)
         + "For details see %s" % context.log)


@then(u'the analysis log contains no error/warning messages')
def step_impl(context):
    badlines, _errors, _warnings = analyselog(context.log)

    assert len(badlines) == 0,\
        ("Found following errors and/or warnings lines in the logfile:\n"
         + "".join(badlines)
         + "For details see %s" % context.log)


@then(u'the server log (if locatable) contains no error/warning messages')
def step_impl(context):
    if context.serverlogfd is not None:
        lines = context.serverlogfd.readlines()
        badlines, _errors, _warnings = analyseloglines(lines)

        assert len(badlines) == 0,\
            ("Found following errors and/or warnings lines in the logfile:\n"
             + "".join(badlines)
             + "For details see %s" % context.serverlog)


@then(u'the number of violations fed is {number}')
def step_impl(context, number):
    exp_measures = {"violations": float(number)}
    assert_measures(context.project, exp_measures)


TEST_METRICS_ORDER = [
    "tests",
    "test_failures",
    "test_errors",
    "skipped_tests",
    "test_success_density",
    "test_execution time"
    ]


def _expMeasuresToDict(measures):
    def convertvalue(value):
        return None if value == "None" else float(value)
    res = {}
    if isinstance(measures, model.Table):
        res = {row["metric"]: convertvalue(row["value"]) for row in measures}
    elif isinstance(measures, list):
        assert len(measures) == len(TEST_METRICS_ORDER)
        res = {}
        for i in range(len(measures) - 1):
            res[TEST_METRICS_ORDER[i]] = convertvalue(measures[i])
    return res


def _gotMeasuresToDict(measures):
    return {measure["key"]: measure["val"] for measure in measures}


def _diffMeasures(expected, measured):
    difflist = []
    for metric, value_expected in expected.iteritems():
        value_measured = measured.get(metric, None)
        if value_expected != value_measured:
            difflist.append("\t%s is actually %s" % (metric, str(value_measured)))
    return "\n".join(difflist)


@then(u'the following metrics have following values')
def step_impl(context):
    exp_measures = _expMeasuresToDict(context.table)
    assert_measures(context.project, exp_measures)


def assert_measures(project, measures):
    metrics_to_query = measures.keys()

    try:
        
        url = (SONAR_URL + "/api/resources?resource=" + project + "&metrics="
               + ",".join(metrics_to_query))
               
        sys.stdout.write("URL METRICS: " + url + "\n")
        
        response = requests.get(url)
        got_measures = {}
        json_measures = json.loads(response.text)[0].get("msr", None)
        if json_measures is not None:
            got_measures = _gotMeasuresToDict(json_measures)

        diff = _diffMeasures(measures, got_measures)
    except requests.exceptions.ConnectionError, e:
        assert False, "cannot query the metrics, details: %s" % str(e)

    assert diff == "", "\n" + diff


@then(u'the test related metrics have following values: {values}')
def step_impl(context, values):
    parsed_values = [value.strip() for value in values.split(",")]
    exp_measures = _expMeasuresToDict(parsed_values)
    assert_measures(context.project, exp_measures)


@then(u'the analysis breaks')
def step_impl(context):
    assert context.rc != 0, "Exit code is %i, but should be non zero" % context.rc


@then(u'the analysis log contains a line matching')
def step_impl(context):
    assert contains_line_matching(context.log, context.text)


def contains_line_matching(filepath, pattern):
    pat = re.compile(pattern)
    with open(filepath) as logfo:
        for line in logfo:
            if pat.match(line):
                return True
    return False


@given(u'a report outside the projects directory, e.g. "/tmp/cppcheck-v1.xml"')
def step_impl(context):
    report_fname = "cppcheck-v1.xml"
    source = os.path.join(TESTDATADIR, "cppcheck_project", report_fname)
    target = os.path.join("/tmp", report_fname)
    shutil.copyfile(source, target)
