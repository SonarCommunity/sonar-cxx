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
package org.sonar.plugins.cxx;

import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Arrays;

import org.apache.tools.ant.DirectoryScanner;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;

public class TestUtils {

  private final static String OS = System.getProperty("os.name").toLowerCase();
  private final static boolean upperCaseRoot = Character.isUpperCase(System.getProperty("java.home").charAt(0));

  public static File loadResource(String resourceName) {
    URL resource = TestUtils.class.getResource(resourceName);
    File resourceAsFile = null;
    try {
      resourceAsFile = new File(resource.toURI());
    } catch (URISyntaxException e) {
      System.out.println("Cannot load resource: " + resourceName);
    }

    return resourceAsFile;
  }

  /**
   * Mocks the filesystem given the root directory of the project
   *
   * @param baseDir project root directory
   * @return mocked filesystem
   */
  public static DefaultFileSystem mockFileSystem(File baseDir) {
    return mockFileSystem(baseDir, null, null);
  }

  /**
   * Mocks the filesystem given the root directory of the project
   *
   * @param baseDir project root directory
   * @return mocked filesystem
   */
  public static DefaultFileSystem mockFileSystem() {
    return mockFileSystem(TestUtils.loadResource("/org/sonar/plugins/cxx/reports-project"), Arrays.asList(new File(".")), null);
  }

  /**
   * Mocks the filesystem given the root directory and lists of source and tests
   * directories. The latter are given just as in sonar-project.properties
   *
   * @param baseDir project root directory
   * @param sourceDirs List of source directories, relative to baseDir.
   * @param testDirs List of test directories, relative to baseDir.
   * @return mocked filesystem
   */
  public static DefaultFileSystem mockFileSystem(File baseDir,
    List<File> sourceDirs,
    List<File> testDirs) {
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    fs.setEncoding(Charset.forName("UTF-8"));
    scanDirs(fs, sourceDirs, Type.MAIN);
    scanDirs(fs, testDirs, Type.TEST);
    return fs;
  }

  public static CxxLanguage mockCxxLanguage() {
    return new CxxLanguage(new Settings());
  }

  public static boolean isWindows() {
    return OS.contains("win");
  }

  private static void scanDirs(DefaultFileSystem fs, List<File> dirs, Type ftype) {
    if (dirs == null) {
      return;
    }

    String[] suffixes = mockCxxLanguage().getFileSuffixes();
    String[] includes = new String[suffixes.length];
    for (int i = 0; i < includes.length; ++i) {
      includes[i] = "**/*" + suffixes[i];
    }

    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setIncludes(includes);
    String relpath;
    for (File dir : dirs) {
      scanner.setBasedir(new File(fs.baseDir(), dir.getPath()));
      scanner.scan();
      for (String path : scanner.getIncludedFiles()) {
        relpath = new File(dir, path).getPath();
        fs.add(new DefaultInputFile("foo", relpath)
          .setLanguage(CxxLanguage.KEY)
          .setType(ftype));
      }
    }
  }
}
