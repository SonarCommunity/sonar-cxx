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
package org.sonar.cxx.sensors.compiler;

import java.io.File;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.sensors.utils.CxxIssuesReportSensor;
import org.sonar.cxx.sensors.utils.InvalidReportException;
import org.sonar.cxx.sensors.utils.ReportException;
import org.sonar.cxx.utils.CxxReportIssue;

/**
 * base class for compiler issues
 */
public abstract class CxxCompilerSensor extends CxxIssuesReportSensor {

  private static final Logger LOG = Loggers.get(CxxCompilerSensor.class);

  @Override
  protected void processReport(File report) throws ReportException {

    final String reportCharset = getCharset();
    final String reportRegEx = getRegex();

    if (reportRegEx.isEmpty()) {
      LOG.error("processReport terminated because of empty custom regular expression");
      return;
    }

    LOG.info("Processing '{}' report '{}', Charset= '{}'", getCompilerKey(), report, reportCharset);

    try ( var scanner = new Scanner(report, reportCharset)) {
      Pattern pattern = Pattern.compile(reportRegEx);
      LOG.info("Using pattern : '{}'", pattern);

      while (scanner.hasNextLine()) {
        Matcher matcher = pattern.matcher(scanner.nextLine());
        if (matcher.find()) {
          String filename = alignFilename(matcher.group("file"));
          String line = alignLine(matcher.group("line"));
          String id = alignId(matcher.group("id"));
          String msg = alignMessage(matcher.group("message"));
          if (isInputValid(filename, line, id, msg)) {
            LOG.debug("Scanner-matches file='{}' line='{}' id='{}' msg={}", filename, line, id, msg);
            var issue = new CxxReportIssue(id, filename, line, msg);
            saveUniqueViolation(issue);
          } else {
            LOG.warn("Invalid compiler warning: '{}''{}'", id, msg);
          }
        }
      }
    } catch (java.io.FileNotFoundException | java.lang.IllegalArgumentException | java.lang.IllegalStateException e) {
      throw new InvalidReportException("The compiler report is invalid", e);
    }
  }

  /**
   * Unique string to identify the compiler
   *
   * @return
   */
  protected abstract String getCompilerKey();

  /**
   * Character set of the report
   *
   * @return
   */
  protected abstract String getCharset();

  /**
   * Regular expression to parse the report
   *
   * @return
   */
  protected abstract String getRegex();

  /**
   * Derived classes can overload this method
   *
   * A valid issue must have an id and, if it has a line number, a filename.
   *
   *
   * @param filename
   * @param line
   * @param id
   * @param msg
   * @return true, if valid
   */
  protected boolean isInputValid(@Nullable String filename, @Nullable String line, @Nullable String id, String msg) {
    if ((id == null) || id.isEmpty()) {
      return false;
    }
    if ((line != null) && !line.isEmpty() && ((filename == null) || filename.isEmpty())) {
      return false;
    }
    return true;
  }

  /**
   * Derived classes can overload this method to align filename
   *
   * @param filename
   * @return
   */
  protected String alignFilename(String filename) {
    return filename;
  }

  /**
   * Derived classes can overload this method to align line number
   *
   * @param line
   * @return
   */
  protected String alignLine(String line) {
    return line;
  }

  /**
   * Derived classes can overload this method to align message id
   *
   * @param id
   * @return
   */
  protected String alignId(String id) {
    return id;
  }

  /**
   * Derived classes can overload this method to align message
   *
   * @param message
   * @return
   */
  protected String alignMessage(String message) {
    return message;
  }

}
