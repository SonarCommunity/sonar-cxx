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
package org.sonar.plugins.cxx.valgrind;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.plugins.cxx.TestUtils;

public class ValgrindReportParserTest {

  private ValgrindReportParser parser;
  private DefaultFileSystem fs;

  @Before
  public void setUp() {
    parser = new ValgrindReportParser();
    fs = TestUtils.mockFileSystem();
  }

  @Test
  public void shouldParseCorrectNumberOfErrors() throws javax.xml.stream.XMLStreamException {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());
    File report = TestUtils.loadResource("reports-project/valgrind-reports/valgrind-result-SAMPLE.xml");
    Set<ValgrindError> valgrindErrors = parser.processReport(context, report);
    assertEquals(valgrindErrors.size(), 6);
  }

  @Test(expected = javax.xml.stream.XMLStreamException.class)
  public void shouldThrowWhenGivenAnIncompleteReport_1() throws javax.xml.stream.XMLStreamException {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());
    // error contains no kind-tag
    File report = TestUtils.loadResource("reports-project/valgrind-reports/incorrect-valgrind-result_1.xml");
    parser.processReport(context, report);
  }

  @Test(expected = javax.xml.stream.XMLStreamException.class)
  public void shouldThrowWhenGivenAnIncompleteReport_2() throws javax.xml.stream.XMLStreamException {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());
    // error contains no what- or xwhat-tag
    File report = TestUtils.loadResource("reports-project/valgrind-reports/incorrect-valgrind-result_2.xml");
    parser.processReport(context, report);
  }

  @Test(expected = javax.xml.stream.XMLStreamException.class)
  public void shouldThrowWhenGivenAnIncompleteReport_3() throws javax.xml.stream.XMLStreamException {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());
    // error contains no stack-tag
    File report = TestUtils.loadResource("reports-project/valgrind-reports/incorrect-valgrind-result_3.xml");
    parser.processReport(context, report);
  }
}
