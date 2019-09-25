package de.mpg.biochem.mars.fx.plot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.AbstractValueIndicator;
import de.gsi.chart.plugins.XRangeIndicator;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YRangeIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.dataset.spi.DoubleDataSet;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.PositionOfInterest;
import de.mpg.biochem.mars.molecule.RegionOfInterest;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Cursor;

public abstract class AbstractMoleculeSubPlot<M extends Molecule> extends AbstractSubPlot implements MoleculeSubPane {
	
	protected M molecule;
	
	//Keep track of axes for which indicators have already been added.
	protected HashSet<String> namesOfActiveRegions, namesOfActivePositions;
	
	public AbstractMoleculeSubPlot(PlotPane plotPane, String plotTitle) {
		super(plotPane, plotTitle);
		
		namesOfActiveRegions = new HashSet<String>();
		namesOfActivePositions = new HashSet<String>();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   if (e.getEventType().getName().equals("UPDATE_PLOT_AREA")) {
					   	removeIndicators();
						update();
						e.consume();
				   }
			   } 
			});
	}
	
	protected DoubleDataSet addLine(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();

		DoubleDataSet dataset = new DoubleDataSet(plotSeries.getXColumn() + " vs " + plotSeries.getYColumn());
		
		//List<Double> xValueList = new ArrayList<>();
		//List<Double> yValueList = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y)) {
				dataset.add(x, y);
			}
		}
		
		//final double[] xValues = new double[xValueList.size()];
		//final double[] yValues = new double[yValueList.size()];
		//for (int row=0;row<xValueList.size();row++) {
		//	xValues[row] = xValueList.get(row);
		//	yValues[row] = yValueList.get(row);
		//}
		
		//If the columns are entirely NaN values. Don't add he plot
		//if (xValueList.size() == 0)
		//	return null;
		
		addRegionsOfInterest(xColumn, yColumn);
		//addPositionsOfInterest(xColumn, yColumn);
		
		return dataset;
	}
	
	protected DoubleDataSet addScatter(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();

		DoubleDataSet dataset = new DoubleDataSet(plotSeries.getXColumn() + " vs " + plotSeries.getYColumn());
		
		//List<Double> xValueList = new ArrayList<>();
		//List<Double> yValueList = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y)) {
				dataset.add(x, y);
			}
		}

		dataset.setStyle("markerType=circle;");
		
		return dataset;
	}
	
	/*
	protected XYChart<Number, Number> addLine(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();
		
		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		//resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);
		
		MarsTable segmentsTable = null;
		if (molecule.hasSegmentsTable(xColumn, yColumn))
			segmentsTable = molecule.getSegmentsTable(xColumn, yColumn);
		
		LineChartWithSegments lineChart = new LineChartWithSegments(segmentsTable, plotSeries, xAxis, yAxis);
		lineChart.setCreateSymbols(false);
		lineChart.setAnimated(false);
		
		List<Data<Number, Number>> data = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y))
				data.add(new Data<>(x, y));
		}
		
		//If the columns are entirely NaN values. Don't add he plot
		if (data.size() == 0)
			return null;
		
		ObservableList<Data<Number, Number>> sourceData = FXCollections.observableArrayList(data);
		
		DataReducingObservableList<Number, Number> reducedData = new DataReducingObservableList<>(xAxis, sourceData);
		reducedData.maxPointsCountProperty().bind(plotPane.maxPointsCount());
		
		Series<Number, Number> series = new Series<>(plotSeries.getYColumn(), reducedData);
		lineChart.getData().add(series);
			
		lineChart.updateStyle(plotPane.getStyleSheetUpdater());
		
		chartPane.getOverlayCharts().add(lineChart);
		
		addRegionsOfInterest(xColumn, yColumn);
		addPositionsOfInterest(xColumn, yColumn);
		
		return lineChart;
	}
	
	
	
	protected XYChart<Number, Number> addScatter(PlotSeries plotSeries) {
		String xColumn = plotSeries.getXColumn();
		String yColumn = plotSeries.getYColumn();

		NumericAxis xAxis = createAxis();
		NumericAxis yAxis = createAxis();
		
		//resetXYZoom();
		
		resetXAxis(xAxis);
		resetYAxis(yAxis);
		
		MarsTable segmentsTable = null;
		if (molecule.hasSegmentsTable(xColumn, yColumn))
			segmentsTable = molecule.getSegmentsTable(xColumn, yColumn);

		ScatterChartWithSegments scatterChart = new ScatterChartWithSegments(segmentsTable, plotSeries, xAxis, yAxis);
		scatterChart.setAnimated(false);
		
		List<Data<Number, Number>> data = new ArrayList<>();
		for (int row=0;row<getDataTable().getRowCount();row++) {
			double x = getDataTable().getValue(xColumn, row);
			double y = getDataTable().getValue(yColumn, row);
			
			if (!Double.isNaN(x) && !Double.isNaN(y))
				data.add(new Data<>(x, y));
		}
		
		//If the columns are entirely NaN values. Don't add he plot
		if (data.size() == 0)
			return null;
		
		ObservableList<Data<Number, Number>> sourceData = FXCollections.observableArrayList(data);
		
		DataReducingObservableList<Number, Number> reducedData = new DataReducingObservableList<>(xAxis, sourceData);
		reducedData.maxPointsCountProperty().bind(plotPane.maxPointsCount());
		
		Series<Number, Number> series = new Series<>(plotSeries.getYColumn(), reducedData);
		scatterChart.getData().add(series);
		
		scatterChart.updateStyle(plotPane.getStyleSheetUpdater());
		
		chartPane.getOverlayCharts().add(scatterChart);
		
		addRegionsOfInterest(xColumn, yColumn);
		addPositionsOfInterest(xColumn, yColumn);
		
		return scatterChart;
	}
	*/
	
	protected void addRegionsOfInterest(String xColumn, String yColumn) {
		for (String regionName : molecule.getRegionNames()) {
			if (molecule.getRegion(regionName).getColumn().equals(xColumn) && !namesOfActiveRegions.contains(regionName)) {
				RegionOfInterest roi = molecule.getRegion(regionName);
				XRangeIndicator xRangeIndicator = new XRangeIndicator(this.globalXAxis, roi.getStart(), roi.getEnd(), roi.getName());
				xRangeIndicator.setLabelVerticalPosition(0.2);
				namesOfActiveRegions.add(regionName);
				chartPane.getPlugins().add(xRangeIndicator);
			} else if (molecule.getRegion(regionName).getColumn().equals(yColumn) && !namesOfActiveRegions.contains(regionName)) {
				RegionOfInterest roi = molecule.getRegion(regionName);
				YRangeIndicator yRangeIndicator = new YRangeIndicator(this.globalYAxis, roi.getStart(), roi.getEnd(), roi.getName());
				yRangeIndicator.setLabelVerticalPosition(0.2);
				namesOfActiveRegions.add(regionName);
				chartPane.getPlugins().add(yRangeIndicator);
			}
		}
	}
	
	protected void addPositionsOfInterest(String xColumn, String yColumn) {
		for (String positionName : molecule.getPositionNames()) {
			if (molecule.getPosition(positionName).getColumn().equals(xColumn) && !namesOfActivePositions.contains(positionName)) {
				PositionOfInterest poi = molecule.getPosition(positionName);
				XValueIndicator xValueIndicator = new XValueIndicator(this.globalXAxis, poi.getPosition(), poi.getName());
				xValueIndicator.setLabelPosition(0.2);
				namesOfActivePositions.add(positionName);
				chartPane.getPlugins().add(xValueIndicator);
			} else if (molecule.getPosition(positionName).getColumn().equals(yColumn) && !namesOfActivePositions.contains(positionName)) {
				PositionOfInterest poi = molecule.getPosition(positionName);
				YValueIndicator yValueIndicator = new YValueIndicator(this.globalYAxis, poi.getPosition(), poi.getName());
				yValueIndicator.setLabelPosition(0.2);
				namesOfActivePositions.add(positionName);
				chartPane.getPlugins().add(yValueIndicator);
			}
		}
	}
	
	@Override
	public void removeIndicators() {
		ArrayList<Object> indicators = new ArrayList<Object>();
    	for (ChartPlugin plugin : chartPane.getPlugins())
			if (plugin instanceof AbstractValueIndicator)
				indicators.add(plugin);
		chartPane.getPlugins().removeAll(indicators);
		namesOfActiveRegions.clear();
		namesOfActivePositions.clear();
	}
	
	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		removeIndicators();
		getDatasetOptionsPane().setTable(molecule.getDataTable());
		for (ChartPlugin plugin : chartPane.getPlugins())
			if (plugin instanceof MarsMoleculePlotPlugin)
				((MarsMoleculePlotPlugin) plugin).setMolecule(molecule);
		update();
		resetXYZoom();
	}

	@Override
	protected MarsTable getDataTable() {
		if (molecule != null) {
			return molecule.getDataTable();
		} else 
			return null;
	}
}
