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
package org.sonar.plugins.cxx.drmemory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.sonar.plugins.cxx.drmemory.DrMemoryParser.DrMemoryError;

public class DrMemoryParserTest {

	@Test
	public void shouldParseTheWholeFile() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("org/sonar/plugins/cxx/reports-project/drmemory-reports/results.txt").getFile());
        List<DrMemoryError> drMemoryErrors = DrMemoryParser.parse(file);
        Assert.assertEquals(733, drMemoryErrors.size());
	}
}
