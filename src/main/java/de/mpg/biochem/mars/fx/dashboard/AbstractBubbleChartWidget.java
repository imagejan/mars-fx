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

import de.gsi.chart.XYChart;
import de.gsi.chart.marker.DefaultMarker;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.Histogram;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.molecule.DashboardTab;
import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTracker;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.metadata.MarsMetadata;
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

import de.gsi.chart.plugins.DataPointTooltip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.Initializable;

import org.apache.commons.lang3.ArrayUtils;

public abstract class AbstractBubbleChartWidget extends AbstractScriptableWidget
		implements MarsDashboardWidget, Initializable {

	protected XYChart bubbleChart;
	protected MarsNumericAxis xAxis, yAxis;

	protected ErrorDataSetRenderer renderer;

	protected ArrayList<DefaultErrorDataSet> datasets;

	protected ArrayList<String> requiredGlobalFields = new ArrayList<String>(
			Arrays.asList("xlabel", "ylabel", "title"));

	@Override
	public void initialize() {
		super.initialize();

		xAxis = new MarsNumericAxis("");
		xAxis.minorTickVisibleProperty().set(false);
		xAxis.setAutoRanging(true);
		xAxis.setAutoRangeRounding(false);

		yAxis = new MarsNumericAxis("");
		yAxis.setMinorTickVisible(false);
		yAxis.setAutoRanging(true);
		yAxis.setAutoRangeRounding(false);

		bubbleChart = new XYChart(xAxis, yAxis);
		bubbleChart.getPlugins().add(new MarsDataPointTracker());
		bubbleChart.setAnimated(false);
		bubbleChart.getRenderers().clear();

		bubbleChart.getPlugins().add(new MarsDataPointTracker());

		datasets = new ArrayList<DefaultErrorDataSet>();

		renderer = new ErrorDataSetRenderer();
		renderer.setMarkerSize(5);
		renderer.setPolyLineStyle(LineStyle.NONE);
		renderer.setErrorType(ErrorStyle.NONE);
		renderer.setDrawMarker(true);
		renderer.setAssumeSortedData(false);
		renderer.pointReductionProperty().set(false);

		bubbleChart.getRenderers().add(renderer);
		bubbleChart.setLegend(null);
		bubbleChart.horizontalGridLinesVisibleProperty().set(false);
		bubbleChart.verticalGridLinesVisibleProperty().set(false);
		
		bubbleChart.setTriggerDistance(0);

		StackPane stack = new StackPane();
		stack.setPadding(new Insets(10, 10, 10, 10));
		stack.getChildren().add(bubbleChart);
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

		if (outputs == null) {
			writeToLog("No outputs were generated by this script.");
			return;
		}

		for (String field : requiredGlobalFields)
			if (!outputs.containsKey(field)) {
				writeToLog("required output " + field + " is missing.");
				return;
			}

		datasets.clear();

		String xlabel = (String) outputs.get("xlabel");
		String ylabel = (String) outputs.get("ylabel");
		String title = (String) outputs.get("title");

		// Check if an x-range was provided
		final boolean autoXRanging;
		if (outputs.containsKey("xmin") && outputs.containsKey("xmax")) {
			autoXRanging = false;
		} else if (outputs.containsKey("xmin")) {
			writeToLog("required output xmax is missing.");
			return;
		} else if (outputs.containsKey("xmax")) {
			writeToLog("required output xmin is missing.");
			return;
		} else {
			autoXRanging = true;
		}

		// Check if a y-range was provided
		final boolean autoYRanging;
		if (outputs.containsKey("ymin") && outputs.containsKey("ymax")) {
			autoYRanging = false;
		} else if (outputs.containsKey("ymin")) {
			writeToLog("required output xmax is missing.");
			return;
		} else if (outputs.containsKey("ymax")) {
			writeToLog("required output xmin is missing.");
			return;
		} else {
			autoYRanging = true;
		}

		LinkedHashSet<String> seriesSet = new LinkedHashSet<String>();
		for (String outputName : outputs.keySet()) {
			if (outputName.startsWith("series")) {
				int index = outputName.indexOf("_");
				seriesSet.add(outputName.substring(0, index));
			}
		}

		List<String> series = new ArrayList<String>(seriesSet);
		Collections.sort(series);

		for (String seriesName : series) {
			DefaultErrorDataSet dataset = buildDataSet(outputs, seriesName);
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
				if (autoXRanging) {
					xAxis.setAutoRanging(true);
				} else {
					xAxis.setAutoRanging(false);
					xAxis.setMin((Double) outputs.get("xmin"));
					xAxis.setMax((Double) outputs.get("xmax"));
				}

				yAxis.setName(ylabel);
				if (autoYRanging) {
					yAxis.setAutoRanging(true);
				} else {
					yAxis.setAutoRanging(false);
					yAxis.setMin((Double) outputs.get("ymin"));
					yAxis.setMax((Double) outputs.get("ymax"));
				}

				bubbleChart.setTitle(title);

				renderer.getDatasets().clear();
				renderer.getDatasets().addAll(datasets);
				
				bubbleChart.getXAxis().forceRedraw();
				bubbleChart.getYAxis().forceRedraw();
				
				bubbleChart.requestLayout();
				
				//Is this needed??
				bubbleChart.layout();
				
				bubbleChart.layoutChildren();
			}
		});
	}

	protected DefaultErrorDataSet buildDataSet(Map<String, Object> outputs, String seriesName) {
		DefaultErrorDataSet dataset = new DefaultErrorDataSet(seriesName);

		int dataPointCount = 0;

		if (outputs.containsKey(seriesName + "_xvalues") && outputs.containsKey(seriesName + "_yvalues")) {
			Double[] xvalues = (Double[]) outputs.get(seriesName + "_xvalues");
			Double[] yvalues = (Double[]) outputs.get(seriesName + "_yvalues");

			if (xvalues == null)
				System.out.println("xvalues == null");

			if (xvalues.length == 0) {
				writeToLog(seriesName + "_xvalues have zero values.");
				return null;
			}

			dataPointCount = xvalues.length;

			if (xvalues.length != yvalues.length) {
				writeToLog(seriesName + "_xvalues and " + seriesName + "_yvalues do not have the same dimensions.");
				return null;
			}

			for (int index = 0; index < xvalues.length; index++)
				dataset.add(xvalues[index], yvalues[index]);
		} else if (outputs.containsKey(seriesName + "_xvalues")) {
			writeToLog("required output " + seriesName + "_yvalues" + " is missing");
			return null;
		} else if (outputs.containsKey(seriesName + "_yvalues")) {
			writeToLog("required output " + seriesName + "_xvalues" + " is missing");
			return null;
		}

		if (outputs.containsKey(seriesName + "_label")) {
			String[] label = (String[]) outputs.get(seriesName + "_label");

			if (dataPointCount != label.length) {
				writeToLog("The length of " + seriesName + "_label does not match that of " + seriesName
						+ "_xvalues and " + seriesName + "_yvalues.");
				return null;
			}

			for (int index = 0; index < label.length; index++)
				dataset.addDataLabel(index, label[index]);
		}

		String[] styleString = new String[dataPointCount];
		for (int index = 0; index < dataPointCount; index++)
			styleString[index] = "";

		if (outputs.containsKey(seriesName + "_color")) {
			String[] color = (String[]) outputs.get(seriesName + "_color");

			if (dataPointCount != color.length) {
				writeToLog("The length of " + seriesName + "_color does not match that of " + seriesName
						+ "_xvalues and " + seriesName + "_yvalues. Will assume single color input.");
				for (int index = 0; index < dataPointCount; index++)
					styleString[index] += "markerColor=" + color[0] + "; ";
			} else {
				for (int index = 0; index < dataPointCount; index++)
					styleString[index] += "markerColor=" + color[index] + "; ";
			}
		} else if (outputs.containsKey(seriesName + "_markerColor")) {
			for (int index = 0; index < dataPointCount; index++)
				styleString[index] += "markerColor=" + (String) outputs.get(seriesName + "_markerColor") + "; ";
		}

		if (outputs.containsKey(seriesName + "_size")) {
			Double[] size = (Double[]) outputs.get(seriesName + "_size");

			if (dataPointCount != size.length) {
				writeToLog("The length of " + seriesName + "_size does not match that of " + seriesName
						+ "_xvalues and " + seriesName + "_yvalues. Will assume single color input.");
				for (int index = 0; index < dataPointCount; index++)
					styleString[index] += "markerSize=" + size[0] + "; ";
			} else {
				for (int index = 0; index < dataPointCount; index++)
					styleString[index] += "markerSize=" + size[index] + "; ";
			}
		} else if (outputs.containsKey(seriesName + "_markerSize")) {
			for (int index = 0; index < dataPointCount; index++)
				styleString[index] += "markerSize=" + (String) outputs.get(seriesName + "_markerSize") + "; ";
		}

		for (int index = 0; index < dataPointCount; index++)
			dataset.addDataStyle(index, styleString[index] + "markerType=circle;");

		return dataset;
	}

	@Override
	public Node getIcon() {
		Region xychartIcon = new Region();
		xychartIcon.getStyleClass().add("bubblechartIcon");
		return xychartIcon;
	}
}
