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

import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.plugins.DataPointTooltip;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTracker;
import de.mpg.biochem.mars.fx.plot.tools.MarsZoomer;
//import de.mpg.biochem.mars.fx.plot.tools.MarsDataPointTooltip;
//import de.mpg.biochem.mars.fx.plot.tools.MarsPositionSelectionTool;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.StyleSheetUpdater;
import de.mpg.biochem.mars.molecule.AbstractJsonConvertibleRecord;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.PopOver.ArrowLocation;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.beans.value.ChangeListener;
import javafx.scene.input.KeyCode;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;

import javafx.scene.control.TextField;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

public abstract class AbstractPlotPane extends AbstractJsonConvertibleRecord implements PlotPane {
	
	public static final Predicate<MouseEvent> PAN_MOUSE_FILTER = event -> MouseEvents
		    .isOnlyPrimaryButtonDown(event) && MouseEvents.modifierKeysUp(event);
	
	protected ArrayList<SubPlot> charts;
	protected ArrayList<Action> tools;
	protected ToolBar toolBar;
	
	protected BorderPane rootBorderPane;
	
	protected VBox chartsPane;
	
	protected static StyleSheetUpdater styleSheetUpdater;
	
	protected BooleanProperty gridlines = new SimpleBooleanProperty();
	protected BooleanProperty fixXBounds = new SimpleBooleanProperty();
	protected BooleanProperty fixYBounds = new SimpleBooleanProperty();
	
	protected BooleanProperty trackSelected = new SimpleBooleanProperty();
	protected BooleanProperty zoomXYSelected = new SimpleBooleanProperty();
	protected BooleanProperty zoomXSelected = new SimpleBooleanProperty();
	protected BooleanProperty zoomYSelected = new SimpleBooleanProperty();
	protected BooleanProperty panSelected = new SimpleBooleanProperty();
	
	protected DoubleProperty yAxisWidth = new SimpleDoubleProperty();
	
	protected IntegerProperty maxPointsCount = new SimpleIntegerProperty(10_000);
	
	protected PlotOptionsPane plotOptionsPane;
	
	protected ButtonBase propertiesButton;

	public AbstractPlotPane() {
		if (styleSheetUpdater == null)
			styleSheetUpdater = new StyleSheetUpdater();
		
		rootBorderPane = new BorderPane();

		plotOptionsPane = new PlotOptionsPane();
		charts = new ArrayList<SubPlot>();
		tools = new ArrayList<Action>();
		chartsPane = new VBox();
		rootBorderPane.setCenter(chartsPane);
		
		gridlines.setValue(true);
		fixXBounds.setValue(false);
		fixYBounds.setValue(false);
		
		buildTools();
		rootBorderPane.setTop(createToolBar());
	}
	
	protected void buildTools() {
		
		Action trackCursor = new Action("Track", null, CIRCLE_ALT, e -> setTool(trackSelected, () -> new MarsDataPointTracker(), Cursor.DEFAULT), 
				null, trackSelected);
		addTool(trackCursor);
		
		Action zoomXYCursor = new Action("Select XY region", null, ARROWS, e -> setTool(zoomXYSelected, () -> new MarsZoomer(false), Cursor.CROSSHAIR),
				null, zoomXYSelected);
		addTool(zoomXYCursor);
		
		Action zoomXCursor = new Action("Select X region", null, ARROWS_H, e -> setTool(zoomXSelected, () -> new MarsZoomer(AxisMode.X, false), Cursor.H_RESIZE),
				null, zoomXSelected);
		addTool(zoomXCursor);
		
		Action zoomYCursor = new Action("Select Y region", null, ARROWS_V, e -> setTool(zoomYSelected, () -> new MarsZoomer(AxisMode.Y, false), Cursor.V_RESIZE),
				null, zoomYSelected);
		addTool(zoomYCursor);
		
		Action panCursor = new Action("Pan", null, HAND_PAPER_ALT, e -> setTool(panSelected, () -> {
			Panner panner = new Panner();
			panner.setMouseFilter(PAN_MOUSE_FILTER);
			return panner;
		}, Cursor.MOVE), null, panSelected);
		addTool(panCursor);
	}
	
	protected void addTool(Action action) {
		tools.add(action);
	}

	protected Node createToolBar() { 
		ToggleButton[] toolButtons = new ToggleButton[tools.size()];
		ToggleGroup toolGroup = new ToggleGroup();
		
		for (int i = 0; i < tools.size(); i++) {
			 if (tools.get(i) != null) {
				 toolButtons[i] = (ToggleButton) ActionUtils.createToolBarButton(tools.get(i));
				 toolButtons[i].setToggleGroup(toolGroup);
			 }
		}

		toolBar = new ToolBar(toolButtons);
		toolBar.getItems().add(new Separator());

		Action resetXYZoom = new Action("Reset Zoom", null, EXPAND, e -> {
			resetXYZoom();
		});
		toolBar.getItems().add(ActionUtils.createToolBarButton(resetXYZoom));
		
		Action reloadAction = new Action("Reload", null, REFRESH, e -> {
			for (SubPlot subPlot : charts)
				subPlot.update();
			resetXYZoom();
		});

		toolBar.getItems().add(ActionUtils.createToolBarButton(reloadAction));
		
		Action saveImageToDisk = new Action("Save image", null, IMAGE, e -> { 
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			fileChooser.setInitialFileName("snapshot.png");
			
			File outputPath = fileChooser.showSaveDialog(getNode().getScene().getWindow());
			
			if (outputPath == null)
				return;
			
			WritableImage snapshot = pixelScaleAwareCanvasSnapshot(chartsPane, 2);
			
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", outputPath);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		toolBar.getItems().add(ActionUtils.createToolBarButton(saveImageToDisk));
		
		//settings
		propertiesButton = ActionUtils.createToolBarButton(new Action("Settings", "Shortcut+S", COG, e -> {
			PopOver popOver = new PopOver();
			popOver.setTitle("Plot Settings");
			popOver.setHeaderAlwaysVisible(true);
			popOver.setAutoHide(false);
			popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
			
			//Retrieve x and y bounds from first chart
			plotOptionsPane.setXMin(charts.get(0).getChart().getXAxis().getMin());
			plotOptionsPane.setXMax(charts.get(0).getChart().getXAxis().getMax());
			plotOptionsPane.setYMin(charts.get(0).getChart().getYAxis().getMin());
			plotOptionsPane.setYMax(charts.get(0).getChart().getYAxis().getMax());
			
			popOver.setContentNode(plotOptionsPane);
			popOver.show(propertiesButton);				
		}));
		toolBar.getItems().add(propertiesButton);
		
		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolBar.getItems().add(spacer);
		
		ButtonBase addSubPlot = ActionUtils.createToolBarButton(new Action("Add Subplot", null, PLUS, e -> addChart()), "0.6em");
		addSubPlot.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 13px; " +
                "-fx-min-height: 13px; " +
                "-fx-max-width: 13px; " +
                "-fx-max-height: 13px;"
        );
		ButtonBase removeSubPlot = ActionUtils.createToolBarButton(new Action("Remove Subplot", null, MINUS, e -> removeChart()), "0.6em");
		removeSubPlot.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 13px; " +
                "-fx-min-height: 13px; " +
                "-fx-max-width: 13px; " +
                "-fx-max-height: 13px;"
        );
		
		VBox plotManagementPane = new VBox(2);
		plotManagementPane.getChildren().add(addSubPlot);
		plotManagementPane.getChildren().add(removeSubPlot);
		toolBar.getItems().add(plotManagementPane);
		
		return toolBar;
	}
	
	protected void setTool(BooleanProperty selected, Supplier<ChartPlugin> supplier, Cursor cursor) {
		for (SubPlot subPlot : charts) {
			if (selected.get()) {
				subPlot.setTool(supplier.get(), cursor);
			} else {
				subPlot.removeTools();
			}
		}
	}
	
	public void resetXYZoom() {
		for (SubPlot subPlot : charts) {
			if (subPlot.getPlotSeriesList().size() == 0)
				continue;
			
			//Make sure the columns have been picked otherwise do nothing...
			for (int i=0; i < subPlot.getPlotSeriesList().size(); i++) {
				if (subPlot.getPlotSeriesList().get(i).getXColumn() == null || subPlot.getPlotSeriesList().get(i).getYColumn() == null)
					return;
			}
	
			if (fixXBounds.get()) {
				subPlot.getChart().getXAxis().setAutoRanging(false);
				subPlot.getChart().getXAxis().setMin(plotOptionsPane.getXMin());
				subPlot.getChart().getXAxis().setMax(plotOptionsPane.getXMax());
			} else
				subPlot.getChart().getXAxis().setAutoRanging(true);
			
			if (fixYBounds.get()) {
				subPlot.getChart().getYAxis().setAutoRanging(false);
				subPlot.getChart().getYAxis().setMin(plotOptionsPane.getYMin());
				subPlot.getChart().getYAxis().setMax(plotOptionsPane.getYMax());
			} else 
				subPlot.getChart().getYAxis().setAutoRanging(true);
		}
	}
	
	public abstract void addChart();
	
	public void addChart(SubPlot subplot) {
		charts.add(subplot);
		
		VBox.setVgrow(subplot.getNode(), Priority.ALWAYS);
		chartsPane.getChildren().add(subplot.getNode());
		
		subplot.getChart().horizontalGridLinesVisibleProperty().bind(gridlines);
		subplot.getChart().verticalGridLinesVisibleProperty().bind(gridlines);
		
		for (SubPlot otherSubPlot : charts) {
			
			otherSubPlot.getYAxis().setMinWidth(50);
			
			if (subplot.equals(otherSubPlot))
				continue;
			
			subplot.getXAxis().setAutoRanging(false);
			
			if (subplot.getXAxis().getMax() > otherSubPlot.getXAxis().getMin() || subplot.getXAxis().getMin() > otherSubPlot.getXAxis().getMin()) {
				subplot.getXAxis().minProperty().bindBidirectional(otherSubPlot.getXAxis().minProperty());
				subplot.getXAxis().maxProperty().bindBidirectional(otherSubPlot.getXAxis().maxProperty());
			} else {
				subplot.getXAxis().maxProperty().bindBidirectional(otherSubPlot.getXAxis().maxProperty());
				subplot.getXAxis().minProperty().bindBidirectional(otherSubPlot.getXAxis().minProperty());
			}
		}
		
		//Ensures autoranging is turned off when zoom, pan etc are used
		//Then all linked plots moved together. Otherwise, one plot can lock the movement of another..
		subplot.getChart().getCanvas().setOnMousePressed(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent me) {
		      for (SubPlot subPlot : charts) {
		    	  subPlot.getXAxis().setAutoRanging(false);
		    	  subPlot.getYAxis().setAutoRanging(false);
		      }
		    }
		  });
			
		toolBar.getItems().add(toolBar.getItems().size() - 1, subplot.getDatasetOptionsButton());
		
		subplot.getYAxis().widthProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				updatePlotWidths();
			}
	      });
		
		updateSubPlotBadges();
	}
	
	public void updatePlotWidths() {
		if (charts.size() > 1) {
			double globalYAxisWidth = 0;
			for (SubPlot subPlot : charts) {
				double calcWidth = Math.ceil(subPlot.getYAxis().calculateWidth());
				if (calcWidth > globalYAxisWidth)
					globalYAxisWidth = calcWidth;
			}
			
			for (SubPlot subPlot : charts) {
				double width = subPlot.getYAxis().widthProperty().get();
				if (width != globalYAxisWidth)
					subPlot.getYAxis().setMinWidth(globalYAxisWidth);
			}
		}
	}
	
	public void removeChart() {
		if (charts.size() > 1) {
			charts.remove(charts.size() - 1);
			chartsPane.getChildren().remove(chartsPane.getChildren().size() - 1);
			
			toolBar.getItems().remove(toolBar.getItems().size() - 2);
		}
		updateSubPlotBadges();
	}
	
	protected void updateSubPlotBadges() {
		if (charts.size() > 1) {
			for (int num=1; num <= charts.size(); num++) {
				charts.get(num-1).getDatasetOptionsButton().setEnabled(true);
				charts.get(num-1).getDatasetOptionsButton().setText("" + num);
				charts.get(num-1).getDatasetOptionsButton().refreshBadge();
			}
		} else {
			charts.get(0).getDatasetOptionsButton().setEnabled(false);
			charts.get(0).getDatasetOptionsButton().refreshBadge();
		}
	}	
	
	public StyleSheetUpdater getStyleSheetUpdater() {
		return styleSheetUpdater;
	}
	
	public static WritableImage pixelScaleAwareCanvasSnapshot(Node node, double pixelScale) {
	    WritableImage writableImage = new WritableImage((int)Math.rint(pixelScale*node.getBoundsInParent().getWidth()), (int)Math.rint(pixelScale*node.getBoundsInParent().getHeight()));
	    SnapshotParameters spa = new SnapshotParameters();
	    spa.setTransform(Transform.scale(pixelScale, pixelScale));
	    return node.snapshot(spa, writableImage);     
	}

	@Override
	public Node getNode() {
		return rootBorderPane;
	}
	
	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
	
	@Override
	public ArrayList<SubPlot> getCharts() {
		return charts;
	}
	
	public BooleanProperty fixXBoundsProperty() {
		return fixXBounds;
	}
	
	public BooleanProperty fixYBoundsProperty() {
		return fixYBounds;
	}
	
	class PlotOptionsPane extends VBox  {
		private TextField xMinTextField, yMinTextField, xMaxTextField, yMaxTextField;
		
		public PlotOptionsPane() {
			//gridlines control
			BorderPane gridBorderPane = new BorderPane();
			ToggleSwitch gridlineSwitch = new ToggleSwitch();
			gridlineSwitch.selectedProperty().bindBidirectional(gridlines);
			gridBorderPane.setLeft(new Label("Gridlines"));
			gridBorderPane.setRight(gridlineSwitch);
			getChildren().add(gridBorderPane);
			
			//X Bounds
			BorderPane fixXBoundsBorderPane = new BorderPane();
			ToggleSwitch fixXBoundsSwitch = new ToggleSwitch();
			fixXBoundsSwitch.selectedProperty().bindBidirectional(fixXBounds);
			fixXBoundsBorderPane.setLeft(new Label("Fix X Bounds"));
			fixXBoundsBorderPane.setRight(fixXBoundsSwitch);
			getChildren().add(fixXBoundsBorderPane);
			
			EventHandler<KeyEvent> handleXFieldEnter = new EventHandler<KeyEvent>() {
		        @Override
		        public void handle(KeyEvent ke) {
		            if (ke.getCode().equals(KeyCode.ENTER)) {
		            	if (!fixXBounds.get())
		            		fixXBounds.set(true);
		            	resetXYZoom();
		            }
		        }
			};
			
			EventHandler<KeyEvent> handleYFieldEnter = new EventHandler<KeyEvent>() {
		        @Override
		        public void handle(KeyEvent ke) {
		            if (ke.getCode().equals(KeyCode.ENTER)) {
		            	if (!fixYBounds.get())
		            		fixYBounds.set(true);
		            	resetXYZoom();
		            }
		        }
			};
			
			BorderPane xMinBorderPane = new BorderPane();
			xMinTextField = new TextField();
			xMinBorderPane.setLeft(new Label("X Min"));
			xMinBorderPane.setRight(xMinTextField);
			xMinTextField.setOnKeyPressed(handleXFieldEnter);
			getChildren().add(xMinBorderPane);
			
			BorderPane xMaxBorderPane = new BorderPane();
			xMaxTextField = new TextField();
			xMaxBorderPane.setLeft(new Label("X Max"));
			xMaxBorderPane.setRight(xMaxTextField);
			xMaxTextField.setOnKeyPressed(handleXFieldEnter);
			getChildren().add(xMaxBorderPane);
			
			//Y Bounds
			BorderPane fixYBoundsborderPane = new BorderPane();
			ToggleSwitch fixYBoundsSwitch = new ToggleSwitch();
			fixYBoundsSwitch.selectedProperty().bindBidirectional(fixYBounds);
			fixYBoundsborderPane.setLeft(new Label("Fix Y Bounds"));
			fixYBoundsborderPane.setRight(fixYBoundsSwitch);
			getChildren().add(fixYBoundsborderPane);
			
			BorderPane yMinBorderPane = new BorderPane();
			yMinTextField = new TextField();
			yMinBorderPane.setLeft(new Label("Y Min"));
			yMinBorderPane.setRight(yMinTextField);
			yMinTextField.setOnKeyPressed(handleYFieldEnter);
			getChildren().add(yMinBorderPane);
			
			BorderPane yMaxBorderPane = new BorderPane();
			yMaxTextField = new TextField();
			yMaxBorderPane.setLeft(new Label("Y Max"));
			yMaxBorderPane.setRight(yMaxTextField);
			yMaxTextField.setOnKeyPressed(handleYFieldEnter);
			getChildren().add(yMaxBorderPane);
			
			this.setPrefWidth(250);
			this.setSpacing(5);
			this.setPadding(new Insets(10, 10, 10, 10));
		}
		
		void setXMin(double xMin) {
			xMinTextField.setText(String.valueOf(xMin));
		}
		
		double getXMin() {
			return Double.valueOf(xMinTextField.getText());
		}
		
		void setXMax(double xMax) {
			xMaxTextField.setText(String.valueOf(xMax));
		}
		
		double getXMax() {
			return Double.valueOf(xMaxTextField.getText());
		}
		
		void setYMin(double yMin) {
			yMinTextField.setText(String.valueOf(yMin));
		}
		
		double getYMin() {
			return Double.valueOf(yMinTextField.getText());
		}
		
		void setYMax(double yMax) {
			yMaxTextField.setText(String.valueOf(yMax));
		}
		
		double getYMax() {
			return Double.valueOf(yMaxTextField.getText());
		}
	}
}
