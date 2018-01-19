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
package org.sonar.cxx.sensors.functionsize;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import java.util.Hashtable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import static org.sonar.cxx.checks.TooManyLinesOfCodeInFunctionCheck.getNumberOfLine;
import org.sonar.cxx.parser.CxxGrammarImpl;
import org.sonar.cxx.sensors.functioncomplexity.FunctionComplexityMetrics;
import org.sonar.cxx.sensors.functioncomplexity.FunctionCount;
import org.sonar.cxx.sensors.squid.SquidSensor;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceFunction;

public class CxxFunctionSizeSquidSensor extends SquidAstVisitor<Grammar> implements SquidSensor  {
  
  private static final Logger LOG = Loggers.get(CxxFunctionSizeSquidSensor.class);
  
  public static final String FUNCTION_SIZE_THRESHOLD_KEY = "funcsize.threshold";  
  
  private int functionsBelowThreshold;
  
  private int sizeThreshold;
  
  public int getFunctionsBelowThreshold(){
    return this.functionsBelowThreshold;
  }
  
  private int functionsOverThreshold;
  
  public int getFunctionsOverThreshold(){
    return this.functionsOverThreshold;
  }  
  
    private Hashtable<SourceFile, FunctionCount> bigFunctionsPerFile = new Hashtable<>();      
  
  public CxxFunctionSizeSquidSensor(CxxLanguage language){
    this.sizeThreshold = language.getIntegerOption(FUNCTION_SIZE_THRESHOLD_KEY).orElse(20);
    LOG.debug("Function size threshold: " + this.sizeThreshold);   
  }
  
    @Override
  public void init() {
    subscribeTo(CxxGrammarImpl.functionBody);
  }

  @Override
  public void leaveNode(AstNode node) {
    SourceFunction sourceFunction = (SourceFunction) getContext().peekSourceCode();
    SourceFile sourceFile = (SourceFile)sourceFunction.getAncestor(SourceFile.class);    
    
    int lineCount = getNumberOfLine(node);
    
    incrementFunctionByThresholdForProject(lineCount);
    incrementFunctionByThresholdForFile(sourceFile, lineCount);
  }
  
  private void incrementFunctionByThresholdForFile(SourceFile sourceFile, int complexity){
    if (!bigFunctionsPerFile.containsKey(sourceFile))
      bigFunctionsPerFile.put(sourceFile, new FunctionCount());
        
    FunctionCount count = bigFunctionsPerFile.get(sourceFile);
    if (complexity > this.sizeThreshold)
        count.functionsOverThreshold++;
    else
        count.functionsBelowThreshold++;        
  }  
  
  private void incrementFunctionByThresholdForProject(int lineCount){
    if (lineCount > sizeThreshold)
      this.functionsOverThreshold++;
    else
      this.functionsBelowThreshold++;
  }


  @Override
  public SquidAstVisitor<Grammar> getVisitor() {
    return this;
  }

  @Override
  public void publishMeasureForFile(InputFile inputFile, SourceFile squidFile, SensorContext context) {
    FunctionCount c = bigFunctionsPerFile.get(squidFile);
    if (c == null) 
      return;
    
    context.<Integer>newMeasure()
      .forMetric(FunctionSizeMetrics.BIG_FUNCTIONS)
      .on(inputFile)
      .withValue(c.functionsOverThreshold)
      .save();

    context.<Double>newMeasure()
      .forMetric(FunctionSizeMetrics.PERC_BIG_FUNCTIONS)
      .on(inputFile)
      .withValue(calculatePercComplexFunctions(c.functionsOverThreshold, c.functionsBelowThreshold))
      .save();    
  }

  @Override
  public void publishMeasureForProject(InputModule module, SensorContext context) {
    context.<Integer>newMeasure()
      .forMetric(FunctionSizeMetrics.BIG_FUNCTIONS)
      .on(module)
      .withValue(functionsOverThreshold)
      .save();
    
    context.<Double>newMeasure()
      .forMetric(FunctionSizeMetrics.PERC_BIG_FUNCTIONS)
      .on(module)
      .withValue(calculatePercComplexFunctions(functionsOverThreshold, functionsBelowThreshold))
      .save();    
  }
  
  private double calculatePercComplexFunctions(int functionsOverThreshold, int functionsBelowThreshold){
    return ((float)functionsOverThreshold * 100.0) / ((float)functionsOverThreshold + (float)functionsBelowThreshold);
  }
  
}
