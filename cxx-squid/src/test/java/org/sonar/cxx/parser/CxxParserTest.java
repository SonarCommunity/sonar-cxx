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
package org.sonar.cxx.parser;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.cxx.CxxConfiguration;
import org.sonar.cxx.CxxFileTesterHelper;
import org.sonar.squidbridge.SquidAstVisitorContext;

public class CxxParserTest extends ParserBaseTest {

  String errSources = "/parser/bad/error_recovery_declaration.cc";
  String[] goodFiles = {"own", "VC", "cli", "cuda", "examples"};
  String[] preprocessorFiles = {"preprocessor"};
  String[] cCompatibilityFiles = {"C", "C99"};
  String rootDir = "src/test/resources/parser";
  File erroneousSources = null;

  public CxxParserTest() {
    super();
    try {
      erroneousSources = new File(CxxParserTest.class.getResource(errSources).toURI());
    } catch (java.net.URISyntaxException e) {
    }
  }

  @Test
  public void testParsingOnDiverseSourceFiles() {
    Collection<File> files = listFiles(goodFiles, new String[]{"cc", "cpp", "hpp"});
    for (File file : files) {
      p.parse(file);
      CxxParser.finishedParsing(file);
    }
  }

  @Test
  public void testPreproccessorParsingOnDiverseSourceFiles() {
    conf = new CxxConfiguration(CxxFileTesterHelper.mockCxxLanguage());
    conf.setErrorRecoveryEnabled(false);
    String baseDir = new File("src/test").getAbsolutePath();
    conf.setBaseDir(baseDir);
    conf.setIncludeDirectories(Arrays.asList(
        "C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\INCLUDE",
        "C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\ATLMFC\\INCLUDE",
        "C:\\Program Files (x86)\\Windows Kits\\10\\include\\10.0.10586.0\\ucrt",
        "C:\\Program Files (x86)\\Windows Kits\\NETFXSDK\\4.6.1\\include\\um",
        "C:\\Program Files (x86)\\Windows Kits\\10\\include\\10.0.10586.0\\shared",
        "C:\\Program Files (x86)\\Windows Kits\\10\\include\\10.0.10586.0\\um",
        "C:\\Program Files (x86)\\Windows Kits\\10\\include\\10.0.10586.0\\winrt",
        "C:\\Workspaces\\boost\\boost_1_61_0",
        "resources",
        "resources\\parser\\preprocessor")
        );
    p = CxxParser.create(mock(SquidAstVisitorContext.class), conf, CxxFileTesterHelper.mockCxxLanguage());
    Collection<File> files = listFiles(preprocessorFiles, new String[]{"cc", "cpp", "hpp", "h"});
    for (File file : files) {
      p.parse(file);
      CxxParser.finishedParsing(file);
    }
  }

  @Test
  public void testParsingInCCompatMode() {
    // The C-compatibility replaces c++ keywords, which aren't keywords in C,
    // with non-keyword-strings via the preprocessor.
    // This mode works if such a file causes parsing errors when the mode
    // is switched off and doesn't, if the mode is switched on.

    File cfile = (File) listFiles(cCompatibilityFiles, new String[]{"c"}).toArray()[0];

    SquidAstVisitorContext context = mock(SquidAstVisitorContext.class);
    when(context.getFile()).thenReturn(cfile);

    conf.setCFilesPatterns(new String[]{""});
    p = CxxParser.create(context, conf, CxxFileTesterHelper.mockCxxLanguage());
    try {
      p.parse(cfile);
    } catch (com.sonar.sslr.api.RecognitionException re) {
    }

    conf.setCFilesPatterns(new String[]{"*.c"});
    p = CxxParser.create(context, conf, CxxFileTesterHelper.mockCxxLanguage());
    p.parse(cfile);
  }

  @Test
  public void testParseErrorRecovery() {
    // The error recovery works, if:
    // - a syntacticly incorrect file causes a parse error when recovery is disabled
    // - but doesn't cause such an error if we run with default settings

    try {
      p.parse(erroneousSources);
      fail("Parser could not recognize the syntax error");
    } catch (com.sonar.sslr.api.RecognitionException re) {
    }

    conf.setErrorRecoveryEnabled(true);
    p = CxxParser.create(mock(SquidAstVisitorContext.class), conf, CxxFileTesterHelper.mockCxxLanguage());
    p.parse(erroneousSources); //<-- this shouldn't throw now
  }

  private Collection<File> listFiles(String[] dirs, String[] extensions) {
    List<File> files = new ArrayList<>();
    for (String dir : dirs) {
      files.addAll(FileUtils.listFiles(new File(rootDir, dir), extensions, true));
    }
    return files;
  }

}
