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

import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import org.junit.Test;
import org.sonar.cxx.CxxAstScanner;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class FunctionParameterCountCheckTest {

	@Test
	public void check() {
		FunctionParameterCountCheck check = new FunctionParameterCountCheck();
		check.setMax(3);

		SourceFile file = CxxAstScanner.scanSingleFile(new File("src/test/resources/checks/parametercount.cc"), check);
		CheckMessagesVerifier.verify(file.getCheckMessages())
		.next().atLine(7).next().atLine(12).noMore();
	}
}