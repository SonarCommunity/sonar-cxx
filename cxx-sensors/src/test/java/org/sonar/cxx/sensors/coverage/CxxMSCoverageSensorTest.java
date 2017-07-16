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
package org.sonar.cxx.sensors.coverage;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.Version;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.sensors.utils.TestUtils;

public class CxxMSCoverageSensorTest {
  private CxxCoverageSensor sensor;
  private DefaultFileSystem fs;
  private SensorContextTester context;
  private CxxLanguage language;

  @Before
  public void setUp() {
    fs = TestUtils.mockFileSystem();

    language = TestUtils.mockCxxLanguage();
    when(language.getStringArrayOption(CxxCoverageSensor.REPORT_PATH_KEY)).thenReturn(new String [] { "coverage-reports/MSCoverage/MSCoverage.xml" });
    when(language.getStringArrayOption(CxxCoverageSensor.REPORT_PATHS_KEY)).thenReturn(new String [] { "coverage-reports/MSCoverage/MSCoverage.xml" });
    when(language.getStringOption(CxxCoverageSensor.REPORT_PATHS_KEY)).thenReturn("coverage-reports/MSCoverage/MSCoverage.xml");
    when(language.getStringOption(CxxCoverageSensor.REPORT_PATH_KEY)).thenReturn("coverage-reports/MSCoverage/MSCoverage.xml");
    when(language.hasKey(CxxCoverageSensor.REPORT_PATH_KEY)).thenReturn(true);
    when(language.hasKey(CxxCoverageSensor.REPORT_PATHS_KEY)).thenReturn(true);
    
    context = SensorContextTester.create(fs.baseDir());
    context.fileSystem().add(new DefaultInputFile("ProjectKey", "source/motorcontroller/motorcontroller.cpp").setLanguage("cpp").initMetadata("asd\nasdas\nasda\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"));
    context.fileSystem().add(new DefaultInputFile("ProjectKey", "source/rootfinder/rootfinder.cpp").setLanguage("cpp").initMetadata("asd\nasdas\nasda\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"));
    
  }

 @Test
  public void shouldReportCorrectCoverage() {
    sensor = new CxxCoverageSensor(new CxxCoverageCache(), language, context);
    sensor.execute(context);
    assertThat(context.lineHits("ProjectKey:source/motorcontroller/motorcontroller.cpp", CoverageType.UNIT, 20)).isEqualTo(1);
    assertThat(context.lineHits("ProjectKey:source/rootfinder/rootfinder.cpp", CoverageType.UNIT, 23)).isEqualTo(1);
  }

}
