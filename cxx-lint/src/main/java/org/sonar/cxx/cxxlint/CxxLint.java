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
package org.sonar.cxx.cxxlint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sonar.sslr.api.Grammar;
import java.beans.Statement;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.cxx.CxxAstScanner;
import org.sonar.cxx.CxxConfiguration;
import org.sonar.cxx.CxxVCppBuildLogParser;
import org.sonar.cxx.checks.CheckList;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

/**
 *
 * @author jocs
 */
public class CxxLint {

  private static Options CreateCommandLineOptions() {
    Options options = new Options();
    options.addOption("s", true, "settings file");
    options.addOption("f", true, "file to analyse - required");
    return options;
  }

  public static String readFile(String filename) throws IOException {
    String content = null;
    File file = new File(filename); //for ex foo.txt
    try (FileReader reader = new FileReader(file)) {
      char[] chars = new char[(int) file.length()];
      reader.read(chars);
      content = new String(chars);
      reader.close();
    } catch (IOException e) {
    }
    return content;
  }

  /**
   * @param args the command line arguments
   * @throws java.lang.InstantiationException
   * @throws java.lang.IllegalAccessException
   * @throws java.io.IOException
   */
  public static void main(String[] args)
          throws InstantiationException, IllegalAccessException, IOException, Exception {

    CommandLineParser commandlineParser = new DefaultParser();
    Options options = CreateCommandLineOptions();
    CommandLine parsedArgs;
    String settingsFile = "";
    String fileToAnalyse = "";
    CxxConfiguration configuration = new CxxConfiguration();

    try {
      parsedArgs = commandlineParser.parse(CreateCommandLineOptions(), args);
      if (!parsedArgs.hasOption("f")) {
        throw new ParseException("f option mandatory");
      } else {
        fileToAnalyse = parsedArgs.getOptionValue("f");
        File f = new File(fileToAnalyse);
        if (!f.exists()) {
          throw new ParseException("file to analysis not found");
        }
      }

      if (parsedArgs.hasOption("s")) {
        settingsFile = parsedArgs.getOptionValue("s");
        File f = new File(settingsFile);
        if (!f.exists()) {
          throw new ParseException("optional settings file given with -s, however file was not found");
        }
      }
    } catch (ParseException exp) {
      System.err.println("Parsing Command line Failed.  Reason: " + exp.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar CxxLint-<sersion>.jar -f filetoanalyse", options);
      throw exp;
    }

    HashMap<String, CheckerData> rulesData = new HashMap<>();
    if (!"".equals(settingsFile)) {
      JsonParser parser = new JsonParser();
      String fileContent = readFile(settingsFile);
      
      // get basic information
      String platformToolset = GetJsonStringValue(parser, fileContent, "platformToolset");     
      String platform = GetJsonStringValue(parser, fileContent, "platform");
      String projectFile = GetJsonStringValue(parser, fileContent, "projectFile");       
      
      JsonElement rules = parser.parse(fileContent).getAsJsonObject().get("rules");
      if (rules != null) {
        for (JsonElement rule : rules.getAsJsonArray()) {
          JsonObject data = rule.getAsJsonObject();
          String ruleId = data.get("ruleId").getAsString();
          String enabled = data.get("status").getAsString();
          if (rulesData.containsKey(ruleId)) {
            continue;
          }

          CheckerData check = new CheckerData();
          check.id = ruleId;
          check.enabled = enabled.equals("Enabled");
          JsonElement region = data.get("properties");
          if (region != null) {
            for (Entry parameter : region.getAsJsonObject().entrySet()) {
              JsonElement elem = (JsonElement) parameter.getValue();
              check.parameterData.put(parameter.getKey().toString(), elem.getAsString());
            }
          }

          rulesData.put(ruleId, check);
        }
      }
      
      JsonElement includes = parser.parse(fileContent).getAsJsonObject().get("includes");
      if (includes != null) {
        for (JsonElement include : includes.getAsJsonArray()) {
          configuration.addOverallIncludeDirectory(include.getAsString());
        }
      }

      JsonElement defines = parser.parse(fileContent).getAsJsonObject().get("defines");
      if (defines != null) {
        for (JsonElement define : defines.getAsJsonArray()) {
          configuration.addOverallDefine(define.getAsString());
        }
      }
      
      JsonElement additionalOptions = parser.parse(fileContent).getAsJsonObject().get("additionalOptions");
      String elementsOfAdditionalOptions = "";
      if (additionalOptions != null) {
        for (JsonElement option : additionalOptions.getAsJsonArray()) {
          elementsOfAdditionalOptions = elementsOfAdditionalOptions + " " + option.getAsString();
        }
      }
      
      HandleVCppAdditionalOptions(platformToolset, platform, elementsOfAdditionalOptions + " ", projectFile, fileToAnalyse, configuration);
    }

    List<Class> checks = CheckList.getChecks();
    List<SquidAstVisitor<Grammar>> visitors = new ArrayList<>();
    HashMap<String, String> KeyData = new HashMap<String, String>();

    for (Class check : checks) {
      Rule rule = (Rule) check.getAnnotation(Rule.class);
      if (rule == null) {
        continue;
      }

      SquidAstVisitor<Grammar> element = (SquidAstVisitor<Grammar>) check.newInstance();

      KeyData.put(check.getCanonicalName(), rule.key());
      
      if (!parsedArgs.hasOption("s")) {
        visitors.add(element);
        continue;
      }
      
      if (!rulesData.containsKey(rule.key())) {
        continue;
      }

      CheckerData data = rulesData.get(rule.key());

      if (!data.enabled) {
        continue;
      }

      for (Field f : check.getDeclaredFields()) {
        for (Annotation a : f.getAnnotations()) {
          RuleProperty ruleProp = (RuleProperty) a;
          if (ruleProp != null) {
            if (data.parameterData.containsKey(ruleProp.key())) {
              if (f.getType().equals(int.class)) {
                String cleanData = data.parameterData.get(ruleProp.key());
                int value = Integer.parseInt(cleanData);
                if (f.toString().startsWith("public ")) {
                  f.set(element, value);
                } else {
                  char first = Character.toUpperCase(ruleProp.key().charAt(0));
                  Statement stmt = new Statement(element, "set" + first + ruleProp.key().substring(1), new Object[]{value});
                  stmt.execute();
                }
              }

              if (f.getType().equals(String.class)) {
                String cleanData = data.parameterData.get(ruleProp.key());

                if (f.toString().startsWith("public ")) {
                  f.set(element, cleanData);
                } else {
                  char first = Character.toUpperCase(ruleProp.key().charAt(0));
                  Statement stmt = new Statement(element, "set" + first + ruleProp.key().substring(1), new Object[]{cleanData});
                  stmt.execute();
                }
              }
            }
          }
        }
      }
      visitors.add(element);
    }

    System.out.println("Analyse with : " + visitors.size() + " checks");
    
    String fileName = new File(fileToAnalyse).getName();
    SensorContextTester sensorContext = SensorContextTester.create(new File(fileToAnalyse).getParentFile().toPath());
    String content = new String(Files.readAllBytes(new File(fileToAnalyse).toPath()), "UTF-8");
    sensorContext.fileSystem().add(new DefaultInputFile("myProjectKey", fileName).initMetadata(content));
    InputFile cxxFile = sensorContext.fileSystem().inputFile(sensorContext.fileSystem().predicates().hasPath(fileName));
    
    SourceFile file = CxxAstScanner.scanSingleFileConfig(
            cxxFile,
            configuration,
            sensorContext,
            visitors.toArray(new SquidAstVisitor[visitors.size()]));
    
    for (CheckMessage message : file.getCheckMessages()) {
      String key = KeyData.get(message.getCheck().getClass().getCanonicalName());      
      // E:\TSSRC\Core\Common\libtools\tool_archive.cpp(390): Warning : sscanf can be ok, but is slow and can overflow buffers.  [runtime/printf-5] [1]
      System.out.println(message.getSourceCode() + "(" + message.getLine() + "): Warning : " + message.formatDefaultMessage() + " [" + key + "]");
    }
  }

  private static String GetJsonStringValue(JsonParser parser, String fileContent, String id) throws JsonSyntaxException {
    JsonElement element = parser.parse(fileContent).getAsJsonObject().get(id);
    if (element != null) {
      return element.getAsString();
    }
    return "";
  }

  private static void HandleVCppAdditionalOptions(String platformToolset, String platform, String elementsOfAdditionalOptions, String project, String fileToAnalyse, CxxConfiguration configuration) {
    if(platformToolset.equals("V100") ||
            platformToolset.equals("V110") ||
            platformToolset.equals("V120") ||
            platformToolset.equals("V140")) {
      
      HashMap<String, List<String>> uniqueIncludes = new HashMap<>();
      HashMap<String, Set<String>> uniqueDefines = new HashMap<>();
      uniqueDefines.put(fileToAnalyse, new HashSet<String>());
      uniqueIncludes.put(fileToAnalyse, new ArrayList<String>());
      CxxVCppBuildLogParser lineOptionsParser = new CxxVCppBuildLogParser(uniqueIncludes, uniqueDefines);
      lineOptionsParser.setPlatform(platform);
      lineOptionsParser.setPlatformToolset(platformToolset);
      lineOptionsParser.parseVCppLine(elementsOfAdditionalOptions, project, fileToAnalyse);
      for(String define : uniqueDefines.get(fileToAnalyse)) {
        configuration.addOverallDefine(define);
      }
    }
  }
}
