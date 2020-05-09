/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.dashboard;

import org.scijava.plugin.Plugin;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.Histogram;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsCategoryAxis;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.molecule.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import de.gsi.chart.renderer.ErrorStyle;

import static de.jensd.fx.glyphs.octicons.OctIcon.BEAKER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.Initializable;

public abstract class AbstractHistogramWidget extends AbstractScriptableWidget
		implements MarsDashboardWidget, Initializable {

	protected XYChart histChart;
	protected MarsNumericAxis xAxis, yAxis;

	protected ErrorDataSetRenderer outlineHistogramRenderer;

	protected ArrayList<DefaultErrorDataSet> datasets;

	protected ArrayList<String> requiredGlobalFields = new ArrayList<String>(
			Arrays.asList("xlabel", "ylabel", "title", "bins", "xmin", "xmax"));

	@Override
	public void initialize() {
		super.initialize();

		xAxis = new MarsNumericAxis("");
		// xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);
		xAxis.minorTickVisibleProperty().set(false);
		xAxis.setAutoRangeRounding(false);
		// xAxis.setAutoRanging(true);
		yAxis = new MarsNumericAxis("");
		yAxis.setMinorTickVisible(false);
		yAxis.setForceZeroInRange(true);
		yAxis.setAutoRanging(true);
		yAxis.setAutoRangeRounding(false);

		histChart = new XYChart(xAxis, yAxis);
		histChart.setAnimated(false);
		histChart.getRenderers().clear();

		outlineHistogramRenderer = new ErrorDataSetRenderer();
		outlineHistogramRenderer.setPolyLineStyle(LineStyle.HISTOGRAM);
		outlineHistogramRenderer.setErrorType(ErrorStyle.NONE);
		outlineHistogramRenderer.pointReductionProperty().set(false);

		datasets = new ArrayList<DefaultErrorDataSet>();

		histChart.getRenderers().add(outlineHistogramRenderer);
		histChart.legendVisibleProperty().set(false);
		histChart.horizontalGridLinesVisibleProperty().set(false);
		histChart.verticalGridLinesVisibleProperty().set(false);

		StackPane stack = new StackPane();
		stack.setPadding(new Insets(10, 10, 10, 10));
		stack.getChildren().add(histChart);
		stack.setPrefSize(250, 250);

		BorderPane chartPane = new BorderPane();
		chartPane.setCenter(stack);
		setContent(getIcon(), chartPane);

		rootPane.setMinSize(250, 250);
		rootPane.setMaxSize(250, 250);
	}

	@Override
	public void run() {
		Map<String, Object> outputs = runScript();

		if (outputs == null)
			return;

		for (String field : requiredGlobalFields)
			if (!outputs.containsKey(field)) {
				writeToLog("required output " + field + " is missing.");
				return;
			}

		datasets.clear();

		String ylabel = (String) outputs.get("ylabel");
		String xlabel = (String) outputs.get("xlabel");
		String title = (String) outputs.get("title");
		Integer bins = (Integer) outputs.get("bins");
		Double xmin = (Double) outputs.get("xmin");
		Double xmax = (Double) outputs.get("xmax");

		Set<String> series = new HashSet<String>();
		for (String outputName : outputs.keySet()) {
			if (outputName.startsWith("series")) {
				int index = outputName.indexOf("_");
				series.add(outputName.substring(0, index));
			}
		}

		for (String seriesName : series) {
			DefaultErrorDataSet dataset = buildDataSet(outputs, seriesName, bins.intValue(), xmin.doubleValue(), xmax.doubleValue());
			if (dataset != null)
				datasets.add(dataset);
			else {
				return;
			}
		}

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				xAxis.setName(xlabel);
				xAxis.setAutoRanging(false);
				xAxis.setMin(xmin);
				xAxis.setMax(xmax);

				yAxis.setName(ylabel);
				// Check if a y-range was provided
				if (outputs.containsKey("ymin") && outputs.containsKey("ymax")) {
					yAxis.setAutoRanging(false);
					yAxis.setMin((Double) outputs.get("ymin"));
					yAxis.setMax((Double) outputs.get("ymax"));
				} else if (outputs.containsKey("ymax")) {
					yAxis.setAutoRanging(false);
					yAxis.setMin(0.0);
					yAxis.setMax((Double) outputs.get("ymax"));
				} else if (outputs.containsKey("ymin")) {
					yAxis.setAutoRanging(true);
				}

				histChart.setTitle(title);

				outlineHistogramRenderer.getDatasets().clear();
				outlineHistogramRenderer.getDatasets().addAll(datasets);
			}
		});
	}
	
	protected DefaultErrorDataSet buildDataSet(Map<String, Object> outputs, String seriesName, int bins, double minX, double maxX) {
		DefaultErrorDataSet dataset = new DefaultErrorDataSet(seriesName);
		
		double binWidth = (maxX - minX) / bins;

		double[] yvalues = new double[bins];
		double[] xvalues = new double[bins];
		
		for (int bin=0; bin<bins; bin++) {
			yvalues[bin] = 0;
			xvalues[bin] = minX + (0.5 + bin)*binWidth;
		}
		
		if (outputs.containsKey(seriesName + "_" + "values")) {
			Double[] values = (Double[]) outputs.get(seriesName + "_" + "values");

			for (double value : values) {
				for (int bin=0; bin<bins; bin++) {
					if (value >= minX + bin*binWidth && value < minX + (bin + 1)*binWidth) {
						yvalues[bin]++;
						break;
					}
				}
			}
		} else {
			writeToLog("Required field " + seriesName + "_values is missing.");
			return null;
		}

		for (int index = 0; index < yvalues.length; index++)
			dataset.add(xvalues[index], yvalues[index]);
		
		String styleString = "";
		if (outputs.containsKey(seriesName + "_" + "strokeColor"))
			styleString += "strokeColor=" + (String) outputs.get(seriesName + "_" + "strokeColor") + "; ";
		if (outputs.containsKey(seriesName + "_" + "strokeWidth"))
			styleString += "strokeWidth=" + ((Integer) outputs.get(seriesName + "_" + "strokeWidth")).intValue();
		
		
		dataset.setStyle(styleString);
		
		return dataset;
	}
	
	@Override
	public Node getIcon() {
		Region barchartIcon = new Region();
		barchartIcon.getStyleClass().add("barchartIcon");
		return barchartIcon;
	}
}
