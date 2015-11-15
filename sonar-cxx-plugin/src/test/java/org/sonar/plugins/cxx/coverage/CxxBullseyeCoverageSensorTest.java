/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010 Neticoa SAS France
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cxx.coverage;

import java.nio.file.Paths;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.plugins.cxx.TestUtils;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.sonar.plugins.cxx.utils.CxxUtils;

public class CxxBullseyeCoverageSensorTest {
  
  private CxxCoverageSensor sensor;
  private SensorContext context;
  private Project project;
  private DefaultFileSystem fs;
  private Issuable issuable;
  private ResourcePerspectives perspectives;
    
  @Before
  public void setUp() {
    project = TestUtils.mockProject();
    issuable = TestUtils.mockIssuable();
    perspectives = TestUtils.mockPerspectives(issuable);
    fs = TestUtils.mockFileSystem();
    context = mock(SensorContext.class);
  }

  @Test
  public void shouldReportCorrectCoverage() {
    Settings settings = new Settings();
    if (TestUtils.isWindows()) {
      settings.setProperty(CxxCoverageSensor.REPORT_PATH_KEY, "coverage-reports/bullseye/coverage-result-bullseye-win.xml");
      settings.setProperty(CxxCoverageSensor.IT_REPORT_PATH_KEY, "coverage-reports/bullseye/coverage-result-bullseye-win.xml");
      settings.setProperty(CxxCoverageSensor.OVERALL_REPORT_PATH_KEY, "coverage-reports/bullseye/coverage-result-bullseye-win.xml");
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/src/testclass.h"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/src/testclass.cpp"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/main.cpp"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/testclass.h"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/testclass.cpp"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/source_1.cpp"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/src/testclass.h"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/src/testclass.cpp"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/main.cpp"), Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("c:/home/path/TESTCOV/testclass.h"), Paths.get("c:/home/path/"));
    } else {
      settings.setProperty(CxxCoverageSensor.REPORT_PATH_KEY, "coverage-reports/bullseye/coverage-result-bullseye-linux.xml");
      settings.setProperty(CxxCoverageSensor.IT_REPORT_PATH_KEY, "coverage-reports/bullseye/coverage-result-bullseye-linux.xml");
      settings.setProperty(CxxCoverageSensor.OVERALL_REPORT_PATH_KEY, "coverage-reports/bullseye/coverage-result-bullseye-linux.xml");
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/src/testclass.h", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/src/testclass.cpp", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/main.cpp", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/testclass.h", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/testclass.cpp", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/source_1.cpp", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/src/testclass.h", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/src/testclass.cpp", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/main.cpp", Paths.get("c:/home/path/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/home/path/TESTCOV/testclass.h", Paths.get("c:/home/path/"));
    }
    sensor = new CxxCoverageSensor(settings, fs, TestUtils.mockReactor());
    sensor.analyse(project, context);
    verify(context, times(90)).saveMeasure((InputFile) anyObject(), any(Measure.class));
  }

  @Test
  public void shoulParseTopLevelFiles() {
    Settings settings = new Settings();
    if (TestUtils.isWindows()) {
      settings.setProperty(CxxCoverageSensor.REPORT_PATH_KEY, "coverage-reports/bullseye/bullseye-coverage-report-data-in-root-node-win.xml");
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/randomfoldernamethatihopeknowmachinehas/anotherincludeattop.h"), Paths.get("C:/randomfoldernamethatihopeknowmachinehas/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/randomfoldernamethatihopeknowmachinehas/test/test.c"), Paths.get("C:/randomfoldernamethatihopeknowmachinehas/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/randomfoldernamethatihopeknowmachinehas/test2/test2.c"), Paths.get("C:/randomfoldernamethatihopeknowmachinehas/"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/randomfoldernamethatihopeknowmachinehas/main.c"), Paths.get("C:/randomfoldernamethatihopeknowmachinehas/"));
    } else {
      settings.setProperty(CxxCoverageSensor.REPORT_PATH_KEY, "coverage-reports/bullseye/bullseye-coverage-report-data-in-root-node-linux.xml");
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/test/anotherincludeattop.h", Paths.get("c:/test/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/test/test/test.c", Paths.get("c:/test/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/test/test2/test2.c", Paths.get("c:/test/"));
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/test/main.c", Paths.get("c:/test/"));
    }
    sensor = new CxxCoverageSensor(settings, fs, TestUtils.mockReactor());
    sensor.analyse(project, context);
    verify(context, times(28)).saveMeasure((InputFile) anyObject(), any(Measure.class));
  }

  @Test
  public void shoulCorrectlyHandleDriveLettersWithoutSlash() {
    Settings settings = new Settings();
    if (TestUtils.isWindows()) {
      settings.setProperty(CxxCoverageSensor.REPORT_PATH_KEY, "coverage-reports/bullseye/bullseye-coverage-drive-letter-without-slash-win.xml");
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/main.c"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/randomfoldernamethatihopeknowmachinehas/test.c"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/randomfoldernamethatihopeknowmachinehas2/test2.c"));
      TestUtils.addInputFile(fs, perspectives, issuable, CxxUtils.normalizePath("C:/anotherincludeattop.h"));
    }
    else {
      settings.setProperty(CxxCoverageSensor.REPORT_PATH_KEY, "coverage-reports/bullseye/bullseye-coverage-drive-letter-without-slash-linux.xml");
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/main.c");
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/test/test.c");
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/test2/test2.c");
      TestUtils.addInputFile(fs, perspectives, issuable, "/c/anotherincludeattop.h");      
    }
    sensor = new CxxCoverageSensor(settings, fs, TestUtils.mockReactor());
    sensor.analyse(project, context);
    verify(context, times(28)).saveMeasure((InputFile) anyObject(), any(Measure.class));
  }
}
