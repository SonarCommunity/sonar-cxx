/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2017 SonarOpenCommunity
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
package org.sonar.plugins.cxx.utils;

import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import org.sonar.api.config.Settings;
import org.sonar.plugins.cxx.CxxLanguage;
import org.sonar.plugins.cxx.TestUtils;

public class CxxReportSensorTest {

  private final String VALID_REPORT_PATH = "cppcheck-reports/cppcheck-result-*.xml";
  private final String VALID_REPORT_PATH_LIST = "cppcheck-reports/*V1.xml, cppcheck-reports/*V2.xml";
  private final String INVALID_REPORT_PATH = "something";
  private final String REPORT_PATH_PROPERTY_KEY = "cxx.reportPath";
 
  private class CxxReportSensorImpl extends CxxReportSensor {

    public CxxReportSensorImpl(Settings settings, FileSystem fs) {
      super(settings, null);
    }

    @Override
    public void execute(SensorContext sc) {
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
      descriptor.onlyOnLanguage(CxxLanguage.KEY).name("CxxReportSensorTest");
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
  public void getReports_shouldFindNothingIfNoKey() {
    settings.setProperty(REPORT_PATH_PROPERTY_KEY, INVALID_REPORT_PATH);
    List<File> reports = sensor.getReports(settings, baseDir, "");
    assertNotFound(reports);
  }

  @Test
  public void getReports_shouldFindNothingIfNoPath() {
    settings.setProperty(REPORT_PATH_PROPERTY_KEY, "");
    List<File> reports = sensor.getReports(settings, baseDir, REPORT_PATH_PROPERTY_KEY);
    assertNotFound(reports);
  }

  @Test
  public void getReports_shouldFindNothingIfInvalidPath() {
    settings.setProperty(REPORT_PATH_PROPERTY_KEY, INVALID_REPORT_PATH);
    List<File> reports = sensor.getReports(settings, baseDir, REPORT_PATH_PROPERTY_KEY);
    assertNotFound(reports);
  }

  @Test
  public void getReports_shouldFindSomething() {
    settings.setProperty(REPORT_PATH_PROPERTY_KEY, VALID_REPORT_PATH);
    List<File> reports = sensor.getReports(settings, baseDir, REPORT_PATH_PROPERTY_KEY);
    assertFound(reports);
    assert (reports.size() == 6);
  }

  @Test
  public void getReports_shouldFindSomethingList() {
    settings.setProperty(REPORT_PATH_PROPERTY_KEY, VALID_REPORT_PATH_LIST);
    List<File> reports = sensor.getReports(settings, baseDir, REPORT_PATH_PROPERTY_KEY);
    assertFound(reports);
    assert (reports.size() == 5);
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
