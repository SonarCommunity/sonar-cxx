/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010 Neticoa SAS France
 * dev@sonar.codehaus.org
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
package org.sonar.plugins.cxx.utils;

import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;

import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cxx.TestUtils;

public class CxxReportSensorTest {
  private final String VALID_REPORT_PATH = "cppcheck-reports/cppcheck-result-*.xml";
  private final String INVALID_REPORT_PATH = "something";
  private final String REPORT_PATH_PROPERTY_KEY = "cxx.reportPath";

  private class CxxReportSensorImpl extends CxxReportSensor {
    public CxxReportSensorImpl(Settings settings, FileSystem fs){
      super(settings, fs);
    }

    @Override
    public void analyse(Project p, SensorContext sc) {
    }
  };

  private CxxReportSensor sensor;
  private File baseDir;
  private Settings settings;
  private static FileSystem fs;

  @Before
  public void init() {
    settings = new Settings();
    fs = TestUtils.mockFileSystem();
    sensor = new CxxReportSensorImpl(settings, fs);
    try {
      baseDir = new File(getClass().getResource("/org/sonar/plugins/cxx/reports-project/").toURI());
    } catch (java.net.URISyntaxException e) {
      System.out.println(e);
    }
  }

  @Test
  public void shouldntThrowWhenInstantiating() {
    new CxxReportSensorImpl(settings, fs);
  }


  @Test
  public void getReports_shouldFindSomethingIfThere() {
    List<File> reports = sensor.getReports(settings, baseDir.getPath(),
        "", VALID_REPORT_PATH);
    assertFound(reports);
  }

  @Test
  public void getReports_shouldFindNothingIfNotThere() {
    List<File> reports = sensor.getReports(new Settings(), baseDir.getPath(),
        "", INVALID_REPORT_PATH);
    assertNotFound(reports);
  }

  @Test
  public void getReports_shouldUseConfigurationWithHigherPriority() {
    // we'll detect this condition by passing something not existing as config property
    // and something existing as default. The result is 'found nothing' because the
    // config has been used
    settings.setProperty(REPORT_PATH_PROPERTY_KEY, INVALID_REPORT_PATH);

    List<File> reports = sensor.getReports(settings, baseDir.getPath(),
        REPORT_PATH_PROPERTY_KEY, VALID_REPORT_PATH);
    assertNotFound(reports);
  }

  @Test
  public void getReports_shouldFallbackToDefaultIfNothingConfigured() {
    List<File> reports = sensor.getReports(settings, baseDir.getPath(),
        REPORT_PATH_PROPERTY_KEY, VALID_REPORT_PATH);
    assertFound(reports);
  }

  @Test
  public void savesACorrectLineLevelViolation() {
    // assert(sensor.saveViolation(??, ??, rulerepokey, "existingfile",
    //                             "1", "existingruleid", "somemessage"))
  }

  @Test
  public void savesACorrectFileLevelViolation() {
    //TDB
  }

  @Test
  public void savesACorrectProjectLevelViolation() {
    //TDB
  }

  ///// negative testcases for saveViolation ////////////
  @Test
  public void savesOnProjectLevelIfFilenameIsEmpty() {
    //TDB
  }

  @Test
  public void doesNotSaveIfLineNumberCannotBeParsed() {
    //TDB
  }

  @Test
  public void doesNotSaveIfRuleCannotBeFound() {
    //TDB
  }

  @Test
  public void doesNotSaveIfResourceCannotBeFoundInSonar() {
    //TDB
  }

  private void assertFound(List<File> reports) {
    assert (reports != null);
    assert (reports.get(0).exists());
    assert (reports.get(0).isAbsolute());
  }

  private void assertNotFound(List<File> reports) {
    assert (reports != null);
  }

}
