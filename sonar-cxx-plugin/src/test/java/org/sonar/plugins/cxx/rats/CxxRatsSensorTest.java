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
package org.sonar.plugins.cxx.rats;

import static org.fest.assertions.Assertions.assertThat;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;
import org.sonar.plugins.cxx.TestUtils;

import org.junit.Before;
import org.junit.Test;

import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

public class CxxRatsSensorTest {

  private CxxRatsSensor sensor;
  private DefaultFileSystem fs;

  @Before
  public void setUp() {
    fs = TestUtils.mockFileSystem();
    Settings settings = new Settings();
    settings.setProperty(CxxRatsSensor.REPORT_PATH_KEY, "rats-reports/rats-result-*.xml");
    sensor = new CxxRatsSensor(settings);
  }

  @Test
  public void shouldReportCorrectViolations() {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());
    context.fileSystem().add(new DefaultInputFile("myProjectKey", "sources/utils/code_chunks.cpp").setLanguage("cpp").initMetadata("asd\nasdas\nasda\n"));
    context.fileSystem().add(new DefaultInputFile("myProjectKey", "report.c").setLanguage("cpp").initMetadata("asd\nasdas\nasda\n"));
    sensor.execute(context);
    assertThat(context.allIssues()).hasSize(5);
  }
}
