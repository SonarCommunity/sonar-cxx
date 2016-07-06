/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2011-2016 SonarOpenCommunity
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
package org.sonar.cxx.checks;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.cxx.CxxAstScanner;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class XPathCheckTest {

  @Test
  public void xpathWithoutFilePattern() throws UnsupportedEncodingException, IOException {
    XPathCheck check = new XPathCheck();
    check.xpathQuery = "//declaration";
    check.message = "Avoid declarations!! ";

    String fileName = "src/test/resources/checks/xpath.cc";
    SensorContextTester sensorContext = SensorContextTester.create(new File("."));
    String content = new String(Files.readAllBytes(new File(sensorContext.fileSystem().baseDir(), fileName).toPath()), "UTF-8");
    sensorContext.fileSystem().add(new DefaultInputFile("myProjectKey", fileName).initMetadata(content));
    InputFile cxxFile = sensorContext.fileSystem().inputFile(sensorContext.fileSystem().predicates().hasPath(fileName));
    
    SourceFile file = CxxAstScanner.scanSingleFile(cxxFile, sensorContext, check); 
    
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1).withMessage(check.message)
      .noMore();
  }

  @Test
  public void xpathWithFilePattern1() throws IOException {
    XPathCheck check = new XPathCheck();
    check.matchFilePattern = "/**/*.cc"; // all files with .cc file extension
    check.xpathQuery = "//declaration";
    check.message = "Avoid declarations!! ";

    String fileName = "src/test/resources/checks/xpath.cc";
    SensorContextTester sensorContext = SensorContextTester.create(new File("."));
    String content = new String(Files.readAllBytes(new File(sensorContext.fileSystem().baseDir(), fileName).toPath()), "UTF-8");
    sensorContext.fileSystem().add(new DefaultInputFile("myProjectKey", fileName).initMetadata(content));
    InputFile cxxFile = sensorContext.fileSystem().inputFile(sensorContext.fileSystem().predicates().hasPath(fileName));
    
    SourceFile file = CxxAstScanner.scanSingleFile(cxxFile, sensorContext, check); 
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1).withMessage(check.message)
      .noMore();
  }

  @Test
  public void xpathWithFilePattern2() throws UnsupportedEncodingException, IOException {
    XPathCheck check = new XPathCheck();
    check.matchFilePattern = "/**/test/**/xpath.cc"; // all files with filename xpath.cc in a subdirectory with name test
    check.xpathQuery = "//declaration";
    check.message = "Avoid declarations!! ";

    String fileName = "src/test/resources/checks/xpath.cc";
    SensorContextTester sensorContext = SensorContextTester.create(new File("."));
    String content = new String(Files.readAllBytes(new File(sensorContext.fileSystem().baseDir(), fileName).toPath()), "UTF-8");
    sensorContext.fileSystem().add(new DefaultInputFile("myProjectKey", fileName).initMetadata(content));
    InputFile cxxFile = sensorContext.fileSystem().inputFile(sensorContext.fileSystem().predicates().hasPath(fileName));
    
    SourceFile file = CxxAstScanner.scanSingleFile(cxxFile, sensorContext, check); 
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1).withMessage(check.message)
      .noMore();
  }

  @Test
  public void xpathWithFilePattern3() throws IOException {
    XPathCheck check = new XPathCheck();
    check.matchFilePattern = "/**/*.xxx"; // all files with .xxx file extension
    check.xpathQuery = "//declaration";
    check.message = "Avoid declarations!! ";

    String fileName = "src/test/resources/checks/xpath.cc";
    SensorContextTester sensorContext = SensorContextTester.create(new File("."));
    String content = new String(Files.readAllBytes(new File(sensorContext.fileSystem().baseDir(), fileName).toPath()), "UTF-8");
    sensorContext.fileSystem().add(new DefaultInputFile("myProjectKey", fileName).initMetadata(content));
    InputFile cxxFile = sensorContext.fileSystem().inputFile(sensorContext.fileSystem().predicates().hasPath(fileName));
    
    SourceFile file = CxxAstScanner.scanSingleFile(cxxFile, sensorContext, check); 
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();
  }

  @Test
  public void xpathWithFilePatternInvert() throws IOException {
    XPathCheck check = new XPathCheck();
    check.matchFilePattern = "/**/*.xxx"; // all files with not .xxx file extension
    check.invertFilePattern = true;
    check.xpathQuery = "//declaration";
    check.message = "Avoid declarations!! ";

    String fileName = "src/test/resources/checks/xpath.cc";
    SensorContextTester sensorContext = SensorContextTester.create(new File("."));
    String content = new String(Files.readAllBytes(new File(sensorContext.fileSystem().baseDir(), fileName).toPath()), "UTF-8");
    sensorContext.fileSystem().add(new DefaultInputFile("myProjectKey", fileName).initMetadata(content));
    InputFile cxxFile = sensorContext.fileSystem().inputFile(sensorContext.fileSystem().predicates().hasPath(fileName));
    
    SourceFile file = CxxAstScanner.scanSingleFile(cxxFile, sensorContext, check); 
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1).withMessage(check.message)
      .noMore();
  }
}
