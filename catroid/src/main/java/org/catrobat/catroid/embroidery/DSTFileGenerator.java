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

package org.catrobat.catroid.embroidery;

import android.graphics.PointF;
import android.util.Log;

import org.catrobat.catroid.ProjectManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Locale;

public class DSTFileGenerator {
	/* https://edutechwiki.unige.ch/en/Embroidery_format_DST
	/* http://www.achatina.de/sewing/main/TECHNICL.HTM
	DST Encoding
	BYTE  |  7  |  6  |  5  |  4  ||  3  |  2  |  1  |  0
		  |------------------------------------------------
		1 | y+1 | y-1 | y+9 | y-9 || x-9 | x+9 | x-1 | x+1
		2 | y+3 | y-3 | y+27| y-27|| x-27| x+27| x-3 | x+3
		3 | c2  | c1  | y+81| y-81|| x-81| x+81| set | set
	*/
	private static final String TAG = DSTFileGenerator.class.getSimpleName();
	private static final int MAXDISTANCE = 121;
	private static final int HEADERMAXBYTE = 512;
	public static final float STEPSIZEINMM = 0.2f;
	public static final String DST_HEADER_LABEL = "LA:%-15s\n" + (char)0x1A;
	public static final String DST_HEADER = "ST:%-6d\n" + (char)0x1A + "CO:%-2d\n" + (char)0x1A
			+ "+X:%-4d\n" + (char)0x1A + "-X:%-4d\n" + (char)0x1A
			+ "+Y:%-4d\n" + (char)0x1A + "-Y:%-4d\n" + (char)0x1A
			+ "AX:%-5d\n" + (char)0x1A + "AY:%-5d\n" + (char)0x1A
			+ "MX:%-5d\n" + (char)0x1A + "MY:%-5d\n" + (char)0x1A
			+ "PD:%-5s\n" + (char)0x1A;
	public static final int[] CONVERSTIONTABLE = {0x0, 0x1, 0x6, 0x4, 0x5, 0x1a, 0x18, 0x19, 0x12, 0x10, 0x11, 0x16,
			0x14, 0x15, 0x6a, 0x68, 0x69, 0x62, 0x60, 0x61, 0x66, 0x64, 0x65, 0x4a, 0x48, 0x49, 0x42, 0x40, 0x41,
			0x46, 0x44, 0x45, 0x5a, 0x58, 0x59, 0x52, 0x50, 0x51, 0x56, 0x54, 0x55, 0x1aa, 0x1a8, 0x1a9, 0x1a2,
			0x1a0, 0x1a1, 0x1a6, 0x1a4, 0x1a5, 0x18a, 0x188, 0x189, 0x182, 0x180, 0x181, 0x186, 0x184, 0x185, 0x19a,
			0x198, 0x199, 0x192, 0x190, 0x191, 0x196, 0x194, 0x195, 0x12a, 0x128, 0x129, 0x122, 0x120, 0x121, 0x126,
			0x124, 0x125, 0x10a, 0x108, 0x109, 0x102, 0x100, 0x101, 0x106, 0x104, 0x105, 0x11a, 0x118, 0x119, 0x112,
			0x110, 0x111, 0x116, 0x114, 0x115, 0x16a, 0x168, 0x169, 0x162, 0x160, 0x161, 0x166, 0x164, 0x165, 0x14a,
			0x148, 0x149, 0x142, 0x140, 0x141, 0x146, 0x144, 0x145, 0x15a, 0x158, 0x159, 0x152, 0x150, 0x151, 0x156,
			0x154, 0x155, 0x2, 0x9, 0x8, 0xa, 0x25, 0x24, 0x26, 0x21, 0x20, 0x22, 0x29, 0x28, 0x2a, 0x95, 0x94, 0x96,
			0x91, 0x90, 0x92, 0x99, 0x98, 0x9a, 0x85, 0x84, 0x86, 0x81, 0x80, 0x82, 0x89, 0x88, 0x8a, 0xa5, 0xa4,
			0xa6, 0xa1, 0xa0, 0xa2, 0xa9, 0xa8, 0xaa, 0x255, 0x254, 0x256, 0x251, 0x250, 0x252, 0x259, 0x258, 0x25a,
			0x245, 0x244, 0x246, 0x241, 0x240, 0x242, 0x249, 0x248, 0x24a, 0x265, 0x264, 0x266, 0x261, 0x260, 0x262,
			0x269, 0x268, 0x26a, 0x215, 0x214, 0x216, 0x211, 0x210, 0x212, 0x219, 0x218, 0x21a, 0x205, 0x204, 0x206,
			0x201, 0x200, 0x202, 0x209, 0x208, 0x20a, 0x225, 0x224, 0x226, 0x221, 0x220, 0x222, 0x229, 0x228, 0x22a,
			0x295, 0x294, 0x296, 0x291, 0x290, 0x292, 0x299, 0x298, 0x29a, 0x285, 0x284, 0x286, 0x281, 0x280, 0x282,
			0x289, 0x288, 0x28a, 0x2a5, 0x2a4, 0x2a6, 0x2a1, 0x2a0, 0x2a2, 0x2a9, 0x2a8, 0x2aa};

	private float minX;
	private float maxX;
	private float minY;
	private float maxY;
	private File dstFile;
	private String filename;
	private FileOutputStream fileStream;

	ArrayList<PointF> stitchPoints = null;

	public DSTFileGenerator(File cache) {
		filename = ProjectManager.getInstance().getCurrentProject().getName() + ".dst";
		dstFile = new File(cache, filename);

		if (dstFile.exists()) {
			dstFile.delete();
		}

		try {
			dstFile.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}

		stitchPoints = new ArrayList<>();
	}

	public File createDSTFile(ArrayList<PointF> stitchPoints) {
		if (stitchPoints.size() > 1) {
			prepareFileWriting(stitchPoints);
			writeDSTFile();
			reset();
			return dstFile;
		}
		return null;
	}

	private void prepareFileWriting(ArrayList<PointF> stitchPoints) {
		convertStitchPointsUnit(stitchPoints);
		validateStitches();
		calculateBoundingBox();
	}

	private void writeDSTFile() {
		try {
			writeHeader();
			writeStitches();
			writeEndFile();
		} catch (IllegalArgumentException ex) {
			Log.e(TAG, "Remaining stitch distance should be zero!");
		} catch (IOException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}

	private void convertStitchPointsUnit(ArrayList<PointF> stitchPoints) {
		for (PointF stitch: stitchPoints) {
			this.stitchPoints.add(new PointF(stitch.x * STEPSIZEINMM, stitch.y * STEPSIZEINMM));
		}
	}

	private void validateStitches() {
		int xDistance;
		int yDistance;

		for (int index = 0; index < stitchPoints.size() - 1; index++) {
			PointF currentPoint = stitchPoints.get(index);
			PointF nextPoint = stitchPoints.get(index + 1);

			xDistance = (int) (nextPoint.x - currentPoint.x);
			yDistance = (int) (nextPoint.y - currentPoint.y);

			if ((xDistance < -MAXDISTANCE) || (xDistance > MAXDISTANCE)
					|| (yDistance < -MAXDISTANCE) || (yDistance > MAXDISTANCE)) {
				splitStitchAtIndex(index);
			}
		}
	}

	private void splitStitchAtIndex(int index) {
		PointF lastValidPoint = stitchPoints.get(index);
		PointF nextPoint = stitchPoints.get(index + 1);

		float xDistance = nextPoint.x - lastValidPoint.x;
		float yDistance = nextPoint.y - lastValidPoint.y;
		float maxDistance = Math.max(Math.abs(xDistance), Math.abs(yDistance));

		int splitCount = (int) Math.ceil(maxDistance / MAXDISTANCE);

		for (int count = 1; count < splitCount; count++) {
			float splitFactor = (float) count / splitCount;

			PointF newStitch = new PointF();
			newStitch.x = interpolate(nextPoint.x, lastValidPoint.x, splitFactor);
			newStitch.y = interpolate(nextPoint.y, lastValidPoint.y, splitFactor);
			this.stitchPoints.add(index + 1, newStitch);
		}
	}

	private float interpolate(float endValue, float startValue, float percentage) {
		return endValue + percentage * (startValue - endValue);
	}

	private void calculateBoundingBox() {
		initBoundingBox(stitchPoints.get(0));
		for (PointF point: stitchPoints) {
			if (minX > point.x) {
				minX = point.x;
			}
			if (maxX < point.x) {
				maxX = point.x;
			}
			if (minY > point.y) {
				minY = point.y;
			}
			if (maxY < point.y) {
				maxY = point.y;
			}
		}
	}

	private void initBoundingBox(PointF point) {
		minX = point.x;
		maxX = point.x;
		minY = point.y;
		maxY = point.y;
	}

	private void writeHeader() throws IOException {
		String header = createHeaderString();

		String headerFill = "";
		for (int index = header.getBytes().length; index < HEADERMAXBYTE; index++) {
			headerFill += " ";
		}

		fileStream = new FileOutputStream(dstFile);
		fileStream.write(header.getBytes());
		fileStream.write(headerFill.getBytes());
	}

	private String createHeaderString() {
		final int mx = 0;
		final int my = 0;
		final String pd = "*****";
		final int colorChanges = 1;
		PointF firstStitch = stitchPoints.get(0);
		PointF lastStitch = stitchPoints.get(stitchPoints.size() - 1);
		return String.format(DST_HEADER_LABEL, ProjectManager.getInstance().getCurrentProject().getName()) + String
				.format(Locale.getDefault(), DST_HEADER, stitchPoints.size(), colorChanges,
				(int) maxX, (int) minX, (int) maxY, (int) minY, (int) (lastStitch.x - firstStitch.x),
				(int) (lastStitch.y - firstStitch.y), mx, my, pd).replace(' ', '\0');
	}

	private char[] getBytesForStitchPoint(int x, int y) {
		char yPart = (char) ((((y & 0x1) << 3) | ((y & 0x2) << 1) | ((y & 0x10) >>> 3)
				| ((y & 0x20) >>> 5)) << 4);
		char xPart = (char) (((x >>> 2) & 0xC) | (x & 0x3));
		char byte0 = (char) (yPart | xPart);

		yPart = (char) ((((y & 0x4) << 1) | ((y & 0x8) >>> 1) | ((y & 0x40) >>> 5)
				| ((y & 0x80) >>> 7)) << 4);
		xPart = (char) (((x >>> 4) & 0xC) | ((x >>> 2) & 0x3));
		char byte1 = (char) (yPart | xPart);

		yPart = (char) (((y >>> 5) & 0x10) | ((y >>> 3) & 0x20));
		xPart = (char) ((x >>> 6) & 0xC);
		char byte2 = (char) (yPart | xPart | 0x03);

		return new char[]{byte0, byte1, byte2};
	}

	private void writeStitches() throws IOException {
		int xChange;
		int yChange;

		ListIterator<PointF> iterator = stitchPoints.listIterator();
		PointF currentPoint = stitchPoints.get(0);
		while (iterator.hasNext()) {
			PointF nextPoint = iterator.next();
			xChange = (int) (nextPoint.x - currentPoint.x);
			yChange = (int) (nextPoint.y - currentPoint.y);
			int xValue = (xChange < 0 ? CONVERSTIONTABLE[(xChange * (-1)) + 121] : CONVERSTIONTABLE[xChange]);
			int yValue = (yChange < 0 ? CONVERSTIONTABLE[(yChange * (-1)) + 121] : CONVERSTIONTABLE[yChange]);
			char[] bytes = getBytesForStitchPoint(xValue, yValue);

			writeBytes(bytes[0], bytes[1], bytes[2]);
			currentPoint = nextPoint;
		}
	}

	private void writeEndFile() throws IOException {
		char byte0 = 0;
		char byte1 = 0;
		char byte2 = 0xF3;

		writeBytes(byte0, byte1, byte2);
		fileStream.flush();
		fileStream.close();
	}

	private void writeBytes(char byte0, char byte1, char byte2) throws IOException {
		fileStream.write(byte0);
		fileStream.write(byte1);
		fileStream.write(byte2);
	}

	private void reset() {
		stitchPoints = new ArrayList<>();
		this.fileStream = null;
	}
}
