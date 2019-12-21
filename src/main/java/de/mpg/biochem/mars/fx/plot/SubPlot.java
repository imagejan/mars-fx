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
package de.mpg.biochem.mars.fx.plot;

import java.util.Set;

import com.jfoenix.controls.JFXBadge;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.ChartPlugin;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
import de.mpg.biochem.mars.molecule.JsonConvertibleRecord;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Node;

public interface SubPlot {
	public MarsNumericAxis getXAxis();
	public void setXLabel(String xAxisLabel);
	public MarsNumericAxis getYAxis();
	public void setYLabel(String yAxisLabel);
	public DatasetOptionsPane getDatasetOptionsPane();
	public ObservableList<PlotSeries> getPlotSeriesList();
	public JFXBadge getDatasetOptionsButton();
	public void setTool(ChartPlugin plugin, Cursor cursor);
	public void removeTools();
	public XYChart getChart();
	public void removeIndicators();
	public void addIndicators(Set<String> AxisList, Set<String> yAxisList);
	public void addDataSet(PlotSeries plotSeries);
	public Node getNode();
	public void fireEvent(Event event);
	public void update();
}
