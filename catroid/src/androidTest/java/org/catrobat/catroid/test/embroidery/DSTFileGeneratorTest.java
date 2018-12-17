/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2018 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.test.embroidery;

import android.content.Context;
import android.graphics.PointF;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.embroidery.DSTFileGenerator;
import org.catrobat.catroid.embroidery.EmbroideryList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DSTFileGeneratorTest {
	final String projectName = "testProject";
	DSTFileGenerator fileGenerator;
	EmbroideryList stitchpoints = new EmbroideryList();
	File file;
	Context context;
	final int[] conversion = {-81, 81, -27, 27, -9, 9, -3, 3, -1, 1};

	@Before
	public void setUp() {
		context = InstrumentationRegistry.getTargetContext().getApplicationContext();
		Project project = new Project(InstrumentationRegistry.getTargetContext(), projectName);
		ProjectManager.getInstance().setCurrentProject(project);
		fileGenerator = new DSTFileGenerator(context.getCacheDir());
		file = new File(context.getCacheDir() + "/" + projectName + ".dst");
	}

	@Test
	public void testConversionTable() {
		for (int element = 0; element < DSTFileGenerator.CONVERSTIONTABLE.length; element++) {
			int mask = 0x200;
			int value = 0;
			for (int i = 0; i < conversion.length; i++) {
				if ((DSTFileGenerator.CONVERSTIONTABLE[element] & (mask >>> i)) != 0) {
					value += conversion[i];
				}
			}
			if (element > 121) {
				assertEquals((element - 121) * (-1), value);
			} else {
				assertEquals(element, value);
			}
		}
	}

	@Test
	public void testSimpleDSTFile() {
		final int expectedFileLength = 521;
		stitchpoints.clear();
		stitchpoints.add(new PointF(1, 1));
		stitchpoints.add(new PointF(2, 2));

		fileGenerator.createDSTFile(stitchpoints);
		long dstFileLength = file.length();

		assertEquals(expectedFileLength, dstFileLength);
	}

	@Test
	public void testStitchSplittingAllDirections() {
		final int expectedFileLength = 542;

		stitchpoints.clear();
		stitchpoints.add(new PointF(0, 0));
		stitchpoints.add(new PointF(250, 0));
		stitchpoints.add(new PointF(250, 250));
		stitchpoints.add(new PointF(0, 250));
		stitchpoints.add(new PointF(0, 0));

		fileGenerator.createDSTFile(stitchpoints);
		long dstFileLength = file.length();

		assertEquals(expectedFileLength, dstFileLength);
	}

	@Test
	public void testMultiSplit() {
		final int minExpectedFileLength = 524;

		stitchpoints.clear();
		stitchpoints.add(new PointF(0, 0));
		stitchpoints.add(new PointF(400, 0));

		fileGenerator.createDSTFile(stitchpoints);
		long dstFileLength = file.length();

		assertTrue(dstFileLength >= minExpectedFileLength);
	}
}
