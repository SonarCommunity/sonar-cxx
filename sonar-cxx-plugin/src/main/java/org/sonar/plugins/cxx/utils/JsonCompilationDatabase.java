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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxCompilationUnitSettings;
import org.sonar.cxx.CxxConfiguration;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonCompilationDatabase {
  public static final Logger LOG = Loggers.get(JsonCompilationDatabase.class);

  public JsonCompilationDatabase(CxxConfiguration config, File compileCommandsFile)
      throws IOException, JsonParseException, JsonMappingException {
    LOG.debug("Parsing 'JSON Compilation Database' format");

    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY);

    JsonCompilationDatabaseCommandObject[] commandObjects = mapper.readValue(compileCommandsFile,
        JsonCompilationDatabaseCommandObject[].class);

    for (JsonCompilationDatabaseCommandObject commandObject : commandObjects) {

      Path cwd = null;
      Path absPath = null;

      if (commandObject.directory != null) {
        cwd = Paths.get(commandObject.directory);
      } else {
        cwd = Paths.get(".");
      }
      absPath = cwd.resolve(commandObject.file);

      if (commandObject.file.equals("__global__")) {
        CxxCompilationUnitSettings globalSettings = new CxxCompilationUnitSettings();

        parseCommandObject(globalSettings, commandObject);

        config.setGlobalCompilationUnitSettings(globalSettings);
      } else {
        CxxCompilationUnitSettings settings = new CxxCompilationUnitSettings();

        parseCommandObject(settings, commandObject);

        config.addCompilationUnitSettings(absPath.toAbsolutePath().normalize().toString(), settings);
      }
    }
  }

  private void parseCommandObject(CxxCompilationUnitSettings settings,
      JsonCompilationDatabaseCommandObject commandObject) {
    settings.setDefines(commandObject.defines);
    settings.setIncludes(commandObject.includes);

    // No need to parse command lines as we have needed information
    if (commandObject.defines != null || commandObject.includes != null)
      return;

    String cmdLine = null;

    if (commandObject.arguments != null)
      cmdLine = commandObject.arguments;
    else if (commandObject.command != null)
      cmdLine = commandObject.command;
    else
      return;

    String[] args = tokenizeCommandLine(cmdLine);
    boolean nextInclude = false;
    boolean nextDefine = false;
    List<String> includes = new ArrayList<String>();
    HashMap<String, String> defines = new HashMap<String, String>();

    // Capture defines and includes from command line
    for (String arg : args) {
      if (nextInclude == true) {
        nextInclude = false;
        includes.add(arg);
      } else if (nextDefine == true) {
        nextDefine = false;
        String[] define = arg.split("=", 2);
        if (define.length == 1) {
          defines.put(define[0], "");
        } else {
          defines.put(define[0], define[1]);
        }
      } else if (arg.equals("-I")) {
        nextInclude = true;
      } else if (arg.startsWith("-I")) {
        includes.add(arg.substring(2));
      } else if (arg.equals("-D")) {
        nextDefine = true;
      } else if (arg.startsWith("-D")) {
        String[] define = arg.substring(2).split("=", 2);
        if (define.length == 1) {
          defines.put(define[0], "");
        } else {
          defines.put(define[0], define[1]);
        }
      }
    }

    settings.setDefines(defines);
    settings.setIncludes(includes);
  }

  private String[] tokenizeCommandLine(String cmdLine) {
    List<String> args = new ArrayList<String>();
    boolean escape = false;
    char stringOpen = 0;
    StringBuffer sb = new StringBuffer();

    // Tokenize command line with support for escaping
    for (char ch : cmdLine.toCharArray()) {
      if (escape) {
        escape = false;
        sb.append(ch);
      } else {
        if (stringOpen == 0) {
          // String not open
          if (ch == '\\')
            escape = true;
          else if (ch == '\'')
            stringOpen = '\'';
          else if (ch == '\"')
            stringOpen = '\"';
          else if (ch == ' ') {
            if (sb.length() > 0) {
              args.add(sb.toString());
              sb = new StringBuffer();
            }
          }

          if (ch != ' ')
            sb.append(ch);
        } else {
          // String open
          if (ch == '\\')
            escape = true;
          else if (ch == stringOpen)
            stringOpen = 0;

          sb.append(ch);
        }
      }
    }

    if (sb.length() > 0)
      args.add(sb.toString());

    return args.toArray(new String[0]);
  }
}
