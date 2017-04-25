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
package org.sonar.cxx.sensors.other;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.sensors.utils.CxxMetrics;
import org.sonar.cxx.sensors.utils.CxxUtils;
import org.sonar.cxx.sensors.utils.CxxReportSensor;
import org.sonar.cxx.sensors.utils.StaxParser;

/**
 * Custom Rule Import, all static analysis are supported.
 *
 * @author jorge costa, stefan weiser
 */
public class CxxOtherSensor extends CxxReportSensor {
  public static final Logger LOG = Loggers.get(CxxOtherSensor.class);
  public static final String REPORT_PATH_KEY = "other.reportPath";
  public static final String KEY = "other";
  public static final String OTHER_XSLT_KEY = KEY + ".xslt.";
  public static final String STYLESHEET_KEY = ".stylesheet";
  public static final String SOURCE_KEY = ".inputs";
  public static final String OUTPUT_KEY = ".outputs";
  private CxxLanguage cxxLanguage;

  /**
   * {@inheritDoc}
   */
  public CxxOtherSensor(CxxLanguage language) {
    super(language);
    this.cxxLanguage = language;
  }

  @Override
  protected String reportPathKey() {
    return REPORT_PATH_KEY;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(this.language.getKey()).name(language.getName() + " ExternalRulesSensor");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(SensorContext context) {
    transformFiles(context.fileSystem().baseDir());
    super.execute(context);
  }

  @Override
  public void processReport(final SensorContext context, File report) throws XMLStreamException, IOException, URISyntaxException, TransformerException {
    LOG.debug("Parsing 'other' format");

    StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

      /**
       * {@inheritDoc}
       */
      @Override
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();

        SMInputCursor errorCursor = rootCursor.childElementCursor("error");
        while (errorCursor.getNext() != null) {
          String file = errorCursor.getAttrValue("file");
          String line = errorCursor.getAttrValue("line");
          String id = errorCursor.getAttrValue("id");
          String msg = errorCursor.getAttrValue("msg");

          saveUniqueViolation(context, CxxOtherRepository.KEY, file, line, id, msg);
        }
      }
    });

    parser.parse(report);
  }

  @Override
  protected String getSensorKey() {
    return KEY;
  }  

  public void transformFiles(final File baseDir) {
    Boolean goOn = true;
    for (int i = 1; (i < 10) && goOn; i++) {
      String stylesheetKey = OTHER_XSLT_KEY + i + STYLESHEET_KEY;
      String sourceKey = OTHER_XSLT_KEY + i + SOURCE_KEY;
      String outputKey = OTHER_XSLT_KEY + i + OUTPUT_KEY;

      String stylesheet = resolveFilename(baseDir.getAbsolutePath(), cxxLanguage.getStringOption(stylesheetKey));
      List<File> sources = getReports(cxxLanguage, baseDir, sourceKey);
      String outputStrings[] = language.getStringArrayOption(outputKey);
      List<String> outputs = Arrays.asList((outputStrings != null) ? outputStrings : new String[] {});

      if (sources.size() != outputs.size()) {
        LOG.error("Number of source XML files is not equal to the the number of output files.");
        goOn = false;
      } else if ((stylesheet != null) ||
        ((sources != null) && (sources.size() > 0)) ||
        ((outputs != null) && (outputs.size() > 0))) {
        if (stylesheet == null) {
          LOG.error(stylesheetKey + " is not defined.");
          goOn = false;
        } else if (sources == null) {
          LOG.error(sourceKey + " file is not defined.");
          goOn = false;
        } else if (outputs == null) {
          LOG.error(outputKey + " is not defined.");
          goOn = false;
        } else {
          LOG.debug("Converting " + stylesheet + " with " + sources.toString() + " to " + outputs.toString() + ".");
          File stylesheetFile = new File(stylesheet);
          if (stylesheetFile.isAbsolute()) {
            transformFileList(baseDir.getAbsolutePath(), stylesheetFile, sources, outputs);
          }
        }
      } else {
          // No keys found.
          goOn = false;
      }
    }
  }

  private void transformFileList(final String baseDir, File stylesheetFile, List<File> sources, List<String> outputs) {
    for (int j = 0; j < sources.size(); j++) {
      try {
        String normalizedOutputFilename = resolveFilename(baseDir, outputs.get(j));
        CxxUtils.transformFile(new StreamSource(stylesheetFile), sources.get(j), new File(normalizedOutputFilename));
      } catch (Exception e) {
        String msg = new StringBuilder()
          .append("Cannot transform report files: '")
          .append(e)
          .append("'")
          .toString();
        LOG.error(msg);
        CxxUtils.validateRecovery(e, cxxLanguage);
      }
    }
  }
}
