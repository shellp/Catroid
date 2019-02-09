/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2019 The Catrobat Team
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
import android.support.annotation.IdRes;
import android.support.test.InstrumentationRegistry;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.embroidery.DSTFileGenerator;
import org.catrobat.catroid.embroidery.EmbroideryList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.assertEquals;

@RunWith(Parameterized.class)
public class DSTFileGeneratorSampleFileTest {
	final String projectName = "testProject";
	DSTFileGenerator fileGenerator;
	EmbroideryList stitchpoints = new EmbroideryList();
	File file;
	Context context;
	final int[] conversion = {-81, 81, -27, 27, -9, 9, -3, 3, -1, 1};

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{512 + 6 + 3, Arrays.asList(new PointF(0,0), new PointF(500,0)), 1, 500, 0, 0, 0},
				{512 + 15 + 3, Arrays.asList(new PointF(0,0), new PointF(500,0), new PointF(500,500), new
						PointF(0,500), new PointF(0,0)), 1, 500, 500, 0, 0},
				{512 + 12 + 3, Arrays.asList(new PointF(0,0), new PointF(500,0), new PointF(250,500), new
						PointF(0,0)), 1, 500, 500, 0, 0}
		});
	}

	@Parameterized.Parameter
	public @IdRes int fileLength;

	@Parameterized.Parameter(1)
	public @IdRes List<PointF> stitchPoints;

	@Parameterized.Parameter(2)
	public @IdRes int colorChanges;

	@Parameterized.Parameter(3)
	public @IdRes int maxX;

	@Parameterized.Parameter(4)
	public @IdRes int maxY;

	@Parameterized.Parameter(5)
	public @IdRes int minX;

	@Parameterized.Parameter(6)
	public @IdRes int minY;

	@Before
	public void setUp() {
		context = InstrumentationRegistry.getTargetContext().getApplicationContext();
		Project project = new Project(InstrumentationRegistry.getTargetContext(), projectName);
		ProjectManager.getInstance().setCurrentProject(project);
		fileGenerator = new DSTFileGenerator(context.getCacheDir());
		file = new File(context.getCacheDir() + "/" + projectName + ".dst");
	}

	private PointF getNextPoint(PointF previousPoint, char b0, char b1, char b2) {
		if (previousPoint == null)
			return new PointF(0,0);
		int xValue = (b0 & 0x3) | ((b1 << 2) & 0xC) | ((b0 & 0xC) << 2) | ((b1 & 0xC) << 4) | ((b2 & 0xC) << 6);
		int yValue = ((b0 >>> 7) & 0x1) | ((b0 >>> 5) & 0x2) | ((b1 >>> 5) & 0x4) | ((b1 >>> 3) & 0x8)
				| ((b0 >>> 1) & 0x10) | ((b0 << 1) & 0x20) | ((b1 << 1) & 0x40) | ((b1 << 3) & 0x80)
				| ((b2 << 3) & 0x100) | ((b2 << 5) & 0x200);
		int mask = 0x200;
		float xChange = 0;
		float yChange = 0;
		for (int i = 0; i < conversion.length; i++) {
			if ((xValue & (mask >>> i)) != 0) {
				xChange += conversion[i];
			}
			if ((yValue & (mask >>> i)) != 0) {
				yChange += conversion[i];
			}
		}
		float x = previousPoint.x + (xChange / DSTFileGenerator.STEPSIZEINMM);
		float y = previousPoint.y + (yChange / DSTFileGenerator.STEPSIZEINMM);
		return new PointF(x, y);
	}

	private byte[] getExpectedHeader(List<PointF> points) {
		PointF firstStitch = points.get(0);
		PointF lastStitch = points.get(points.size() - 1);
		String header =  String.format(DSTFileGenerator.DST_HEADER_LABEL, ProjectManager.getInstance().getCurrentProject().getName()) +
				String.format(Locale.getDefault(), DSTFileGenerator.DST_HEADER, points.size(), colorChanges,
						(int) (maxX * DSTFileGenerator.STEPSIZEINMM), (int) (minX * DSTFileGenerator.STEPSIZEINMM),
						(int) (maxY * DSTFileGenerator.STEPSIZEINMM), (int) (minY * DSTFileGenerator.STEPSIZEINMM),
						(int) ((lastStitch.x - firstStitch.x) * DSTFileGenerator.STEPSIZEINMM),
						(int) ((lastStitch.y - firstStitch.y) * DSTFileGenerator.STEPSIZEINMM), 0, 0, "*****")
						.replace(' ', '\0');
		return header.getBytes();
	}

	@Test
	public void testSimpleLongLine() throws IOException {
		final int expectedFileLength = fileLength;
		stitchpoints.clear();
		List<PointF> points = stitchPoints;
		for (PointF point : points) {
			stitchpoints.add(point);
		}
		fileGenerator.createDSTFile(stitchpoints);

		assertEquals(expectedFileLength, file.length());

		byte[] fileBytes = new byte[expectedFileLength];
		FileInputStream fis = new FileInputStream(file);
		fis.read(fileBytes);
		fis.close();

		byte[] expectedHeaderBytes =  getExpectedHeader(points);
		for (int i = 0; i < 512; i++) {
			if (i < expectedHeaderBytes.length) {
				assertEquals(expectedHeaderBytes[i], fileBytes[i]);
			} else {
				assertEquals(' ', (char)fileBytes[i]);
			}
		}

		List<PointF> pointsFromFile = new ArrayList<>();
		PointF previousPoint = null;
		for (int i = 512; i < fileBytes.length-3; i += 3) {
			PointF nextPoint = getNextPoint(previousPoint, (char) fileBytes[i], (char) fileBytes[i+1], (char)
					fileBytes[i+2]);
			pointsFromFile.add(nextPoint);
			previousPoint = nextPoint;
		}

		assertEquals(points, pointsFromFile);
		assertEquals((byte) 0x0, fileBytes[fileBytes.length-3]);
		assertEquals((byte) 0x0, fileBytes[fileBytes.length-2]);
		assertEquals((byte) 0xF3, fileBytes[fileBytes.length-1]);
	}

}
