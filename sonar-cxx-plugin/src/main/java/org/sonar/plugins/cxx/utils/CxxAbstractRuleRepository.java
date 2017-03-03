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
import java.io.FileReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

import org.sonar.api.config.Settings;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.cxx.CxxLanguage;

/**
 * {@inheritDoc}
 */
public abstract class CxxAbstractRuleRepository implements RulesDefinition {

  public static final Logger LOG = Loggers.get(CxxAbstractRuleRepository.class);

  private final ServerFileSystem fileSystem;
  public final Settings settings;
  private final RulesDefinitionXmlLoader xmlRuleLoader;
  protected final String repositoryKey;
  protected final String repositoryName;
  protected final String customRepositoryKey;

  /**
   * {@inheritDoc}
   */
  public CxxAbstractRuleRepository(ServerFileSystem fileSystem, RulesDefinitionXmlLoader xmlRuleLoader, Settings settings, String key, String name, String customKey) {
    this.fileSystem = fileSystem;
    this.xmlRuleLoader = xmlRuleLoader;
    this.repositoryKey = key;
    this.repositoryName = name;
    this.customRepositoryKey = customKey;
    this.settings = settings;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(repositoryKey, CxxLanguage.KEY).setName(repositoryName);

    RulesDefinitionXmlLoader xmlLoader = new RulesDefinitionXmlLoader();
    if (!"".equals(fileName())) {
      InputStream xmlStream = getClass().getResourceAsStream(fileName());
      xmlLoader.load(repository, xmlStream, "UTF-8");

      for (File userExtensionXml : getExtensions(repositoryKey, "xml")) {
        try {
          FileReader reader = new FileReader(userExtensionXml);
          xmlRuleLoader.load(repository, reader);
        } catch (Exception ex) {
          LOG.info("Cannot Load XML '{}'", ex);
        }
      }
    }

    String customRules = settings.getString(this.customRepositoryKey);
    if (customRules != null && !customRules.trim().isEmpty()) {
      xmlRuleLoader.load(repository, new StringReader(customRules));
    }

    repository.done();
  }

  public List<File> getExtensions(String dirName, String... suffixes) {
    File dir = new File(fileSystem.getHomeDir(), "extensions/rules/" + dirName);
    List<File> files = new ArrayList<>();
    if (dir.exists() && dir.isDirectory()) {
      if (suffixes != null && suffixes.length > 0) {
        files.addAll(FileUtils.listFiles(dir, suffixes, false));
      } else {
        files.addAll(FileUtils.listFiles(dir, null, false));
      }
    }
    return files;
  }

  protected abstract String fileName();
}
