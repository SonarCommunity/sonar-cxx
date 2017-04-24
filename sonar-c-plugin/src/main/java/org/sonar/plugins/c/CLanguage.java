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
package org.sonar.plugins.c;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.config.Settings;
import org.sonar.api.internal.apachecommons.lang.builder.HashCodeBuilder;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.checks.BooleanEqualityComparisonCheck;
import org.sonar.cxx.checks.ClassComplexityCheck;
import org.sonar.cxx.checks.CollapsibleIfCandidateCheck;
import org.sonar.cxx.checks.CommentRegularExpressionCheck;
import org.sonar.cxx.checks.CommentedCodeCheck;
import org.sonar.cxx.checks.CycleBetweenPackagesCheck;
import org.sonar.cxx.checks.DuplicatedIncludeCheck;
import org.sonar.cxx.checks.FileComplexityCheck;
import org.sonar.cxx.checks.FileEncodingCheck;
import org.sonar.cxx.checks.FileHeaderCheck;
import org.sonar.cxx.checks.FileRegularExpressionCheck;
import org.sonar.cxx.checks.FixmeTagPresenceCheck;
import org.sonar.cxx.checks.FunctionComplexityCheck;
import org.sonar.cxx.checks.HardcodedAccountCheck;
import org.sonar.cxx.checks.HardcodedIpCheck;
import org.sonar.cxx.checks.LineRegularExpressionCheck;
import org.sonar.cxx.checks.MagicNumberCheck;
import org.sonar.cxx.checks.MissingCurlyBracesCheck;
import org.sonar.cxx.checks.MissingIncludeFileCheck;
import org.sonar.cxx.checks.MissingNewLineAtEndOfFileCheck;
import org.sonar.cxx.checks.NestedStatementsCheck;
import org.sonar.cxx.checks.NoSonarCheck;
import org.sonar.cxx.checks.ParsingErrorCheck;
import org.sonar.cxx.checks.ParsingErrorRecoveryCheck;
import org.sonar.cxx.checks.ReservedNamesCheck;
import org.sonar.cxx.checks.SafetyTagCheck;
import org.sonar.cxx.checks.StringLiteralDuplicatedCheck;
import org.sonar.cxx.checks.SwitchLastCaseIsDefaultCheck;
import org.sonar.cxx.checks.TabCharacterCheck;
import org.sonar.cxx.checks.TodoTagPresenceCheck;
import org.sonar.cxx.checks.TooLongLineCheck;
import org.sonar.cxx.checks.TooManyLinesOfCodeInFileCheck;
import org.sonar.cxx.checks.TooManyLinesOfCodeInFunctionCheck;
import org.sonar.cxx.checks.TooManyParametersCheck;
import org.sonar.cxx.checks.TooManyStatementsPerLineCheck;
import org.sonar.cxx.checks.UndocumentedApiCheck;
import org.sonar.cxx.checks.UnnamedNamespaceInHeaderCheck;
import org.sonar.cxx.checks.UseCorrectIncludeCheck;
import org.sonar.cxx.checks.UseCorrectTypeCheck;
import org.sonar.cxx.checks.UselessParenthesesCheck;
import org.sonar.cxx.checks.UsingNamespaceInHeaderCheck;
import org.sonar.cxx.checks.XPathCheck;
import org.sonar.cxx.checks.naming.ClassNameCheck;
import org.sonar.cxx.checks.naming.FileNameCheck;
import org.sonar.cxx.checks.naming.FunctionNameCheck;
import org.sonar.cxx.checks.naming.MethodNameCheck;

/**
 *
 * @author jocs
 */
public class CLanguage extends CxxLanguage {
  public static final String DEFAULT_SOURCE_SUFFIXES = ".c";
  public static final String DEFAULT_HEADER_SUFFIXES = ".h";
  public static final String DEFAULT_C_FILES = "*.c,*.C";
  public static final String KEY = "c";
  public static final String PROPSKEY = "c";
  public static final String REPOSITORY_KEY = "c";

  public static final String DEFAULT_PROFILE = "Sonar way";

  private final String[] sourceSuffixes;
  private final String[] headerSuffixes;
  private final String[] fileSuffixes;
  
  public CLanguage(Settings settings) {
    super("c", "c", settings);
    
    sourceSuffixes = createStringArray(settings.getStringArray(CPlugin.SOURCE_FILE_SUFFIXES_KEY), DEFAULT_SOURCE_SUFFIXES);
    headerSuffixes = createStringArray(settings.getStringArray(CPlugin.HEADER_FILE_SUFFIXES_KEY), DEFAULT_HEADER_SUFFIXES);
    fileSuffixes = mergeArrays(sourceSuffixes, headerSuffixes);    
  }
  
  @Override
  public int hashCode() {
      return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
          appendSuper(super.hashCode()).
          append(getKey()).
          toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() == obj.getClass()) {
      return getKey().equals(((CxxLanguage) obj).getKey()); 
    } else {
      return false;
    }
  }

  @Override
  public String[] getFileSuffixes() {
    return fileSuffixes;
  }

  @Override
  public String[] getSourceFileSuffixes() {
    return sourceSuffixes;
  }

  @Override
  public String getRepositorySuffix() {
    return "-c";
  }
  
  @Override
  public String[] getHeaderFileSuffixes() {
    return headerSuffixes;
  }

  @Override
  public String getPropertiesKey() {
    return PROPSKEY;
  }

  private String[] createStringArray(String[] values, String defaultValues) {
    if (values == null || values.length == 0) {
      return defaultValues.split(",");
    }
    return values;
  }  
  
  private String[] mergeArrays(String[] array1, String[] array2) {
    String[] result = new String[array1.length + array2.length];
    System.arraycopy(sourceSuffixes, 0, result, 0, array1.length);
    System.arraycopy(headerSuffixes, 0, result, array1.length, array2.length);
    return result;
  }  
  
  public List<Class> getChecks() {
    return new ArrayList<Class>(Arrays.asList(
      CollapsibleIfCandidateCheck.class,
      CommentedCodeCheck.class,
      CommentRegularExpressionCheck.class,
      CycleBetweenPackagesCheck.class,
      DuplicatedIncludeCheck.class,
      FileComplexityCheck.class,
      ClassComplexityCheck.class,
      FileHeaderCheck.class,
      FileEncodingCheck.class,
      FileRegularExpressionCheck.class,
      LineRegularExpressionCheck.class,
      FixmeTagPresenceCheck.class,
      FunctionComplexityCheck.class,
      HardcodedAccountCheck.class,
      HardcodedIpCheck.class,
      MagicNumberCheck.class,
      MissingCurlyBracesCheck.class,
      MissingIncludeFileCheck.class,
      MissingNewLineAtEndOfFileCheck.class,
      NoSonarCheck.class,
      ParsingErrorCheck.class,
      ParsingErrorRecoveryCheck.class,
      ReservedNamesCheck.class,
      StringLiteralDuplicatedCheck.class,
      SwitchLastCaseIsDefaultCheck.class,
      TabCharacterCheck.class,
      TodoTagPresenceCheck.class,
      TooLongLineCheck.class,
      TooManyLinesOfCodeInFileCheck.class,
      TooManyStatementsPerLineCheck.class,
      UndocumentedApiCheck.class,
      UnnamedNamespaceInHeaderCheck.class,
      UselessParenthesesCheck.class,
      UseCorrectTypeCheck.class,
      UsingNamespaceInHeaderCheck.class,
      SafetyTagCheck.class,
      UseCorrectIncludeCheck.class,
      BooleanEqualityComparisonCheck.class,
      NestedStatementsCheck.class,
      TooManyParametersCheck.class,
      TooManyLinesOfCodeInFunctionCheck.class,
      // name checks
      ClassNameCheck.class,
      FileNameCheck.class,
      FunctionNameCheck.class,
      MethodNameCheck.class,
      // XPath
      XPathCheck.class
    ));
  }

  @Override
  public String getRepositoryKey() {
    return REPOSITORY_KEY;
  }  
}
