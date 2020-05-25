/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2020 SonarOpenCommunity
 * http://github.com/SonarOpenCommunity/sonar-cxx
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.cxx.sensors.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.utils.CxxReportIssue;
import org.sonar.cxx.utils.CxxReportLocation;

/**
 * This class is used as base for all sensors which import external reports, which contain issues. It hosts common logic
 * such as saving issues in SonarQube
 */
public abstract class CxxIssuesReportSensor extends CxxReportSensor {

  private static final Logger LOG = Loggers.get(CxxIssuesReportSensor.class);

  private final Set<CxxReportIssue> uniqueIssues = new HashSet<>();

  /**
   * {@inheritDoc}
   */
  protected CxxIssuesReportSensor() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void executeImpl() {
    LOG.debug("Searching reports by relative path with basedir '{}' and search prop '{}'",
              context.fileSystem().baseDir(), getReportPathKey());

    List<File> reports = getReports(getReportPathKey());
    for (var report : reports) {
      executeReport(report);
    }
  }

  /**
   * Saves code violation only if it wasn't already saved
   *
   * Saves a code violation which is detected in the given file/line and has given ruleId and message. Saves it to the
   * given project and context. Project or file-level violations can be saved by passing null for the according
   * parameters ('file' = null for project level, 'line' = null for file-level)
   *
   * @param issue
   */
  public void saveUniqueViolation(CxxReportIssue issue) {
    if (uniqueIssues.add(issue)) {
      try {
        NewIssue newIssue = context.newIssue();
        if (addLocations(newIssue, issue)) {
          addFlow(newIssue, issue);
          newIssue.save();
        }
      } catch (RuntimeException e) {
        var msg = "Cannot save the issue '" + issue + "'";
        CxxUtils.validateRecovery(msg, e, context.config());
      }
    }
  }

  /**
   * @param report to read
   */
  protected void executeReport(File report) {
    try {
      LOG.info("Processing report '{}'", report);
      processReport(report);
      LOG.info("Processing successful '{}'", report);
    } catch (ReportException e) {
      var msg = e.getMessage() + ", report='" + report + "'";
      CxxUtils.validateRecovery(msg, e, context.config());
    }
  }

  private int getLineAsInt(@Nullable String line, int lines) {
    int lineNr = 0;
    if (line != null) {
      try {
        lineNr = Integer.parseInt(line);
        if (lineNr < 1) {
          lineNr = 1;
        } else if (lineNr > lines) { // https://jira.sonarsource.com/browse/SONAR-6792
          lineNr = lines;
        }
      } catch (java.lang.NumberFormatException e) {
        var msg = "Invalid line number: '" + line + "'";
        CxxUtils.validateRecovery(msg, e, context.config());
        lineNr = -1;
      }
    }
    return lineNr;
  }

  @CheckForNull
  private NewIssueLocation createNewIssueLocation(NewIssue newIssue, CxxReportLocation location) {
    InputFile inputFile = getInputFileIfInProject(location.getFile());
    if (inputFile != null) {
      int line = Integer.max(1, getLineAsInt(location.getLine(), inputFile.lines()));
      return newIssue.newLocation()
        .on(inputFile)
        .at(inputFile.selectLine(line))
        .message(location.getInfo());
    }
    return null;
  }

  private boolean addLocations(NewIssue newIssue, CxxReportIssue issue) {
    boolean first = true;
    for (var location : issue.getLocations()) {
      NewIssueLocation newLocation = null;
      if (location.getFile() != null && !location.getFile().isEmpty()) {
        newLocation = createNewIssueLocation(newIssue, location);
      } else {
        newLocation = newIssue.newLocation()
          .on(context.project())
          .message(location.getInfo());
      }

      if (newLocation != null) {
        if (first) {
          newIssue
            .forRule(RuleKey.of(getRuleRepositoryKey(), issue.getRuleId()))
            .at(newLocation);
          first = false;
        } else {
          newIssue.addLocation(newLocation);
        }
      }
    }

    return !first;
  }

  private void addFlow(NewIssue newIssue, CxxReportIssue issue) {
    var newIssueFlow = new ArrayList<NewIssueLocation>();
    for (var location : issue.getFlow()) {
      NewIssueLocation newIssueLocation = createNewIssueLocation(newIssue, location);
      if (newIssueLocation != null) {
        newIssueFlow.add(newIssueLocation);
      } else {
        LOG.debug("Failed to create new issue location from flow location {}", location);
        newIssueFlow.clear();
        break;
      }
    }

    if (!newIssueFlow.isEmpty()) {
      newIssue.addFlow(newIssueFlow);
    }
  }

  protected abstract void processReport(File report) throws ReportException;

  protected abstract String getReportPathKey();

  protected abstract String getRuleRepositoryKey();
}
