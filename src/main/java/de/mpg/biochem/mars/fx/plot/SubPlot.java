package de.mpg.biochem.mars.fx.plot;

import java.util.Set;

import com.jfoenix.controls.JFXBadge;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.ChartPlugin;
import de.mpg.biochem.mars.fx.plot.tools.MarsNumericAxis;
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
