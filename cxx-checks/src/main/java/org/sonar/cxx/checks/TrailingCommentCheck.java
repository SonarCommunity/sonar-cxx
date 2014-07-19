/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2011 Waleri Enns and CONTACT Software GmbH
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.cxx.checks;

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.squid.checks.SquidCheck;

//import org.sonar.squidbridge.checks.SquidCheck;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.sslr.parser.LexerlessGrammar;



import java.util.Set;
import java.util.regex.Pattern;


@Rule(
  key = "TrailingCommentCheck",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class TrailingCommentCheck extends SquidCheck<Grammar> implements AstAndTokenVisitor {


  private static final String DEFAULT_LEGAL_COMMENT_PATTERN = "^\\s*+[^\\s]++$";
  private static final Set<String> EXCLUDED_PATTERNS = ImmutableSet.of("NOSONAR");


  @RuleProperty(
    key = "legalCommentPattern",
    defaultValue = DEFAULT_LEGAL_COMMENT_PATTERN)
  public String legalCommentPattern = DEFAULT_LEGAL_COMMENT_PATTERN;


  private Pattern pattern;
  private int previousTokenLine;


  @Override
  public void visitFile(AstNode astNode) {
    previousTokenLine = -1;
    pattern = Pattern.compile(legalCommentPattern);
  }


  @Override
  public void visitToken(Token token) {
    if (token.getLine() != previousTokenLine) {
      for (Trivia trivia : token.getTrivia()) {
        if (trivia.isComment() && trivia.getToken().getLine() == previousTokenLine) {
          String comment = trivia.getToken().getValue();


          comment = comment.startsWith("//") ? comment.substring(2) : comment.substring(2, comment.length() - 2);
          comment = comment.trim();


          if (!pattern.matcher(comment).matches() && !containsExcludedPattern(comment)) {
            getContext().createLineViolation(this, "Move this trailing comment on the previous empty line.", previousTokenLine);
          }
        }
      }
    }


    previousTokenLine = token.getLine();
  }


  private boolean containsExcludedPattern(String comment) {
    for (String pattern : EXCLUDED_PATTERNS) {
      if (StringUtils.containsIgnoreCase(comment, pattern)) {
        return true;
      }
    }
    return false;
  }
}

