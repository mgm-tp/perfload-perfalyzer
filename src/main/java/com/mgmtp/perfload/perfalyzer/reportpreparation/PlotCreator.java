/*
 * Copyright (c) 2013-2014 mgm technology partners GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mgmtp.perfload.perfalyzer.reportpreparation;

import static com.google.common.io.Files.createParentDirs;
import static com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils.selectDisplayData;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;

import com.mgmtp.perfload.perfalyzer.annotations.IntFormat;

/**
 * @author rnaegele
 */
@Singleton
public class PlotCreator {

	private static final StandardChartTheme CHART_THEME = new StandardChartTheme("JFree");

	static {
		CHART_THEME.setDrawingSupplier(new PerfAlyzerDrawingSupplier());
	}

	public static enum AxisType {
		LINEAR,
		LOGARITHMIC
	}

	public static enum RendererType {
		LINES {
			@Override
			XYItemRenderer createRenderer() {
				return new XYLineAndShapeRenderer(true, false);
			}
		},
		SHAPES {
			@Override
			XYItemRenderer createRenderer() {
				return new XYLineAndShapeRenderer(false, true);
			}
		},
		LINES_AND_SHAPES {
			@Override
			XYItemRenderer createRenderer() {
				return new XYLineAndShapeRenderer(true, true);
			}
		};

		abstract XYItemRenderer createRenderer();
	}

	public static enum ChartDimensions {
		DEFAULT(640, 400),
		LARGE(1125, 800),
		WIDE(1125, 400);

		private final int width;
		private final int height;

		private ChartDimensions(final int width, final int height) {
			this.width = width;
			this.height = height;
		}

		/**
		 * @return the width
		 */
		public int getWidth() {
			return width;
		}

		/**
		 * @return the height
		 */
		public int getHeight() {
			return height;
		}
	}

	private final NumberFormat numberFormat;
	private final ResourceBundle resourceBundle;
	private final List<DisplayData> displayDataList;

	@Inject
	public PlotCreator(@IntFormat final NumberFormat numberFormat, final ResourceBundle resourceBundle,
			final List<DisplayData> displayDataList) {
		this.numberFormat = numberFormat;
		this.resourceBundle = resourceBundle;
		this.displayDataList = displayDataList;
	}

	public JFreeChart createPlot(final AxisType xAxisType, final AxisType yAxisType, final RendererType rendererType,
			final DisplayData displayData, final NumberDataSet... dataSets) {

		NumberAxis xAxis = createAxis(xAxisType, resourceBundle.getString(displayData.getUnitX()));

		Plot plot;
		if (dataSets.length == 1) {
			NumberAxis yAxis = createAxis(yAxisType, resourceBundle.getString(displayData.getUnitY()));
			plot = new XYPlot(dataSets[0], xAxis, yAxis, rendererType.createRenderer());
		} else {
			CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(xAxis);
			for (int i = 0; i < dataSets.length; ++i) {
				NumberDataSet dataSet = dataSets[i];
				NumberAxis yAxis = createAxis(yAxisType, resourceBundle.getString(displayData.getUnitYList().get(i)));
				XYPlot subPlot = new XYPlot(dataSet, null, yAxis, rendererType.createRenderer());
				combinedPlot.add(subPlot);
				formatPlot(subPlot);
			}
			plot = combinedPlot;
		}

		JFreeChart chart = new JFreeChart(plot);
		CHART_THEME.apply(chart);

		formatPlot(plot);

		LegendTitle legend = chart.getLegend();
		legend.setBackgroundPaint(new Color(229, 229, 229));
		legend.setFrame(new LineBorder(new Color(213, 213, 213), new BasicStroke(1.0f), legend.getFrame().getInsets()));

		return chart;
	}

	private NumberAxis createAxis(final AxisType axisType, final String axisLabel) {
		NumberAxis numberAxis = axisType.equals(AxisType.LOGARITHMIC) ? new LogarithmicAxis(axisLabel)
				: new NumberAxis(axisLabel);
		numberAxis.setNumberFormatOverride((NumberFormat) numberFormat.clone());
		return numberAxis;
	}

	private void formatPlot(final Plot plot) {
		plot.setBackgroundPaint(new GradientPaint(0f, 0f, new Color(213, 213, 213), 0f, 0f, new Color(156, 156, 156)));
		plot.setOutlinePaint(new Color(221, 221, 221));
	}

	public void writePlotFile(final File file, final AxisType xAxisType, final AxisType yAxisType,
			final RendererType rendererType, final DisplayData displayData, final ChartDimensions dimensions,
			final NumberDataSet... dataSets) throws IOException {
		createParentDirs(file);
		for (NumberDataSet dataSet : dataSets) {
			dataSet.sortSeries();
		}
		JFreeChart chart = createPlot(xAxisType, yAxisType, rendererType, displayData, dataSets);
		BufferedImage chartImage = chart.createBufferedImage(dimensions.getWidth(), dimensions.getHeight());
		ImageIO.write(chartImage, "png", file);
	}

	public void writePlotFile(final File file, final AxisType xAxisType, final AxisType yAxisType,
			final RendererType rendererType, final ChartDimensions dimensions, final NumberDataSet... dataSets)
			throws IOException {
		DisplayData displayData = selectDisplayData(file, displayDataList);
		writePlotFile(file, xAxisType, yAxisType, rendererType, displayData, dimensions, dataSets);
	}

	static class PerfAlyzerDrawingSupplier extends DefaultDrawingSupplier {

		public PerfAlyzerDrawingSupplier() {
			super(DEFAULT_PAINT_SEQUENCE, DEFAULT_FILL_PAINT_SEQUENCE, DEFAULT_OUTLINE_PAINT_SEQUENCE,
					new Stroke[] { new BasicStroke(1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL) },
					DEFAULT_OUTLINE_STROKE_SEQUENCE, createStandardSeriesShapes());
		}

		/**
		 * Creates an array of standard shapes to display for the items in series on charts.
		 * 
		 * @return The array of shapes.
		 */
		public static Shape[] createStandardSeriesShapes() {

			Shape[] result = new Shape[10];

			double size = 4.0;
			double delta = size / 2.0;
			int[] xpoints = null;
			int[] ypoints = null;

			// square
			result[0] = new Rectangle2D.Double(-delta, -delta, size, size);
			// circle
			result[1] = new Ellipse2D.Double(-delta, -delta, size, size);

			// up-pointing triangle
			xpoints = intArray(0.0, delta, -delta);
			ypoints = intArray(-delta, delta, delta);
			result[2] = new Polygon(xpoints, ypoints, 3);

			// diamond
			xpoints = intArray(0.0, delta, 0.0, -delta);
			ypoints = intArray(-delta, 0.0, delta, 0.0);
			result[3] = new Polygon(xpoints, ypoints, 4);

			// horizontal rectangle
			result[4] = new Rectangle2D.Double(-delta, -delta / 2, size, size / 2);

			// down-pointing triangle
			xpoints = intArray(-delta, +delta, 0.0);
			ypoints = intArray(-delta, -delta, delta);
			result[5] = new Polygon(xpoints, ypoints, 3);

			// horizontal ellipse
			result[6] = new Ellipse2D.Double(-delta, -delta / 2, size, size / 2);

			// right-pointing triangle
			xpoints = intArray(-delta, delta, -delta);
			ypoints = intArray(-delta, 0.0, delta);
			result[7] = new Polygon(xpoints, ypoints, 3);

			// vertical rectangle
			result[8] = new Rectangle2D.Double(-delta / 2, -delta, size / 2, size);

			// left-pointing triangle
			xpoints = intArray(-delta, delta, delta);
			ypoints = intArray(0.0, -delta, +delta);
			result[9] = new Polygon(xpoints, ypoints, 3);

			return result;

		}

		/**
		 * Helper method to avoid lots of explicit casts in getShape(). Returns an array containing
		 * the provided doubles cast to ints.
		 * 
		 * @param a
		 *            x
		 * @param b
		 *            y
		 * @param c
		 *            z
		 * 
		 * @return int[3] with converted params.
		 */
		private static int[] intArray(final double a, final double b, final double c) {
			return new int[] { (int) a, (int) b, (int) c };
		}

		/**
		 * Helper method to avoid lots of explicit casts in getShape(). Returns an array containing
		 * the provided doubles cast to ints.
		 * 
		 * @param a
		 *            x
		 * @param b
		 *            y
		 * @param c
		 *            z
		 * @param d
		 *            t
		 * 
		 * @return int[4] with converted params.
		 */
		private static int[] intArray(final double a, final double b, final double c, final double d) {
			return new int[] { (int) a, (int) b, (int) c, (int) d };
		}
	}

}
