/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2020 SonarOpenCommunity
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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.sensors.coverage.bullseye.BullseyeParser;
import org.sonar.cxx.sensors.coverage.cobertura.CoberturaParser;
import org.sonar.cxx.sensors.coverage.ctc.TestwellCtcTxtParser;
import org.sonar.cxx.sensors.coverage.vs.VisualStudioParser;
import org.sonar.cxx.sensors.utils.CxxReportSensor;
import org.sonar.cxx.sensors.utils.CxxUtils;
import org.sonar.cxx.sensors.utils.EmptyReportException;

/**
 * {@inheritDoc}
 */
public class CxxCoverageSensor extends CxxReportSensor {

  // Configuration properties before SQ 6.2
  public static final String REPORT_PATH_KEY = "sonar.cxx.coverage.reportPaths";

  private static final Logger LOG = Loggers.get(CxxCoverageSensor.class);

  private final List<CoverageParser> parsers = new LinkedList<>();
  private final CxxCoverageCache cache;

  /**
   * {@inheritDoc}
   *
   * @param cache for all coverage data
   */
  public CxxCoverageSensor(CxxCoverageCache cache) {
    this.cache = cache;
    parsers.add(new CoberturaParser());
    parsers.add(new BullseyeParser());
    parsers.add(new VisualStudioParser());
    parsers.add(new TestwellCtcTxtParser());
  }

  public static List<PropertyDefinition> properties() {
    return Collections.unmodifiableList(Arrays.asList(
      PropertyDefinition.builder(REPORT_PATH_KEY)
        .name("Coverage report(s)")
        .description("List of paths to reports containing coverage data, relative to projects root."
                       + " The values are separated by commas."
                       + " See <a href='https://github.com/SonarOpenCommunity/sonar-cxx/wiki/Get-code-coverage-metrics'>"
                     + "here</a> for supported formats.")
        .category("CXX External Analyzers")
        .subCategory("Coverage")
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build()
    ));
  }

  /**
   * @param parser
   * @param report
   * @param measuresTotal
   * @return true if report was parsed and results are available otherwise false
   */
  private static void parseCoverageReport(CoverageParser parser, File report,
                                          Map<String, CoverageMeasures> measuresTotal) {
    var measuresForReport = new HashMap<String, CoverageMeasures>();
    try {
      parser.parse(report, measuresForReport);
    } catch (XMLStreamException e) {
      throw new EmptyReportException("Coverage report " + report + " cannot be parsed by " + parser, e);
    }

    if (measuresForReport.isEmpty()) {
      throw new EmptyReportException("Coverage report " + report + " result is empty (parsed by " + parser + ")");
    }

    measuresTotal.putAll(measuresForReport);
    LOG.info("Added coverage report '{}' (parsed by: {})", report, parser);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("CXX coverage report import")
      .onlyOnLanguage("cxx")
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PATH_KEY));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void executeImpl() {
    if (context.config().hasKey(REPORT_PATH_KEY)) {
      List<File> reports = getReports(REPORT_PATH_KEY);
      Map<String, CoverageMeasures> coverageMeasures = processReports(reports, this.cache.unitCoverageCache());
      saveMeasures(coverageMeasures);
    }
  }

  private Map<String, CoverageMeasures> processReports(List<File> reports,
                                                       Map<String, Map<String, CoverageMeasures>> cacheCov) {
    Map<String, CoverageMeasures> measuresTotal = new HashMap<>();

    for (var report : reports) {
      if (!cacheCov.containsKey(report.getAbsolutePath())) {
        for (var parser : parsers) {
          try {
            parseCoverageReport(parser, report, measuresTotal);
            LOG.debug("cached measures for '{}' : current cache content data = '{}'", report.getAbsolutePath(),
                      cacheCov.size());

            cacheCov.put(report.getAbsolutePath(), measuresTotal);
            // Only use first coverage parser which handles the data correctly
            break;
          } catch (EmptyReportException e) {
            LOG.debug("Report is empty {}", e.getMessage());
          }
        }
        if (cacheCov.get(report.getAbsolutePath()) != null) {
          measuresTotal.putAll(cacheCov.get(report.getAbsolutePath()));
        }
      } else {
        measuresTotal = cacheCov.get(report.getAbsolutePath());
        LOG.debug("Processing report '{}' skipped - already in cache", report);
      }
    }
    return measuresTotal;
  }

  private void saveMeasures(Map<String, CoverageMeasures> coverageMeasures) {
    for (var entry : coverageMeasures.entrySet()) {
      final String filePath = PathUtils.sanitize(entry.getKey());
      if (filePath != null) {
        InputFile cxxFile = getInputFileIfInProject(filePath);
        LOG.debug("save coverage measure for file: '{}' cxxFile = '{}'", filePath, cxxFile);

        if (cxxFile != null) {

          NewCoverage newCoverage = context.newCoverage().onFile(cxxFile);

          Collection<CoverageMeasure> measures = entry.getValue().getCoverageMeasures();
          LOG.debug("Saving '{}' coverage measures for file '{}'", measures.size(), filePath);

          measures.forEach((CoverageMeasure measure) -> checkCoverage(newCoverage, measure));

          try {
            newCoverage.save();
            LOG.debug("Saved '{}' coverage measures for file '{}'", measures.size(), filePath);
          } catch (RuntimeException e) {
            var msg = "Cannot save coverage measures for file '" + filePath + "'";
            CxxUtils.validateRecovery(msg, e, context.config());
          }
        } else {
          LOG.debug("Cannot find the file '{}', ignoring coverage measures", filePath);
          if (filePath.startsWith(context.fileSystem().baseDir().getAbsolutePath())) {
            LOG.warn("Cannot find the file '{}', ignoring coverage measures", filePath);
          }
        }
      } else {
        LOG.debug("Cannot sanitize file path '{}'", entry.getKey());
      }
    }
  }

  /**
   * @param newCoverage
   * @param measure
   */
  private void checkCoverage(NewCoverage newCoverage, CoverageMeasure measure) {
    try {
      newCoverage.lineHits(measure.getLine(), measure.getHits());
      newCoverage.conditions(measure.getLine(), measure.getConditions(), measure.getCoveredConditions());
      LOG.debug("line '{}' Hits '{}' Conditions '{}:{}'", measure.getLine(), measure.getHits(),
                measure.getConditions(), measure.getCoveredConditions());
    } catch (RuntimeException e) {
      var msg = "Cannot save Conditions Hits for Line '" + measure.getLine() + "'";
      CxxUtils.validateRecovery(msg, e, context.config());
    }
  }

}
