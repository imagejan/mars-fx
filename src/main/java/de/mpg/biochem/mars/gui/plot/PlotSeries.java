/*******************************************************************************
 * Copyright (C) 2019, Karl Duderstadt
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
package de.mpg.biochem.mars.gui.plot;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;

import de.mpg.biochem.mars.table.MARSResultsTable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

public class PlotSeries {
		private ComboBox<String> yColumnField, xColumnField, typeField;
		private ComboBox<Color> colorField, segmentColorField;
		private JFXTextField widthField, segmentsWidthField;
		private JFXCheckBox drawSegmentsColumn;
		
		private MARSResultsTable table;
		
		private String[] columnHeadings;
		
		protected static Color[] colors = {Color.BLACK, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW};
		
		protected static String[] types = {"Line","Scatter","Bar"};
		
		public PlotSeries(MARSResultsTable table, ComboBox<String> xColumnField) {
			this.xColumnField = xColumnField;
			this.columnHeadings = table.getColumnHeadings();
			initComponents();
			load();
			
			//xColumnField.getSelectionModel().select("slice");
			//yColumnField.getSelectionModel().select("x");
			
			//PlotSeries(table, "slice", "x");
		}
		
		public PlotSeries(MARSResultsTable table, String xColumn, String yColumn) {
			this.table = table;
			this.columnHeadings = table.getColumnHeadings();
			initComponents();
			load();
			
			xColumnField.getSelectionModel().select(xColumn);
			yColumnField.getSelectionModel().select(yColumn);
		}
		
		void initComponents() {
			typeField = new ComboBox<>();
			//xColumnField = new ComboBox<>();
			yColumnField = new ComboBox<>();
			colorField = new ComboBox<>();
			widthField = new JFXTextField();

			drawSegmentsColumn = new JFXCheckBox();
			segmentColorField = new ComboBox<>();
			segmentsWidthField = new JFXTextField();
			
			Callback<ListView<Color>, ListCell<Color>> factory = new Callback<ListView<Color>, ListCell<Color>>() {
		        @Override
		        public ListCell<Color> call(ListView<Color> list) {
		            return new ColorRectCell();
		        }
		    };

		    colorField.setCellFactory(factory);
		    colorField.setButtonCell(factory.call(null));
			
		    segmentColorField.setCellFactory(factory);
		    segmentColorField.setButtonCell(factory.call(null));
		}
		
		void load() {
			typeField.getItems().addAll(types);
			
			yColumnField.getItems().addAll(columnHeadings);
			
			colorField.getItems().addAll(colors);
			colorField.setPrefWidth(50);
			
			widthField.setText("1.0");
			widthField.setPrefWidth(50);
			
			segmentColorField.getItems().addAll(colors);
			segmentColorField.setPrefWidth(50);
			
			segmentsWidthField.setText("1.0");
			segmentsWidthField.setPrefWidth(50);
			
			setDefaults();
		}
		
		void setDefaults() {
			typeField.getSelectionModel().select("Line");
			colorField.getSelectionModel().select(Color.BLACK);
			segmentColorField.getSelectionModel().select(Color.RED);
		}
		
		public ComboBox<String> getTypeField() {
			return typeField;
		}
		
		public ComboBox<Color> getColorField() {
			return colorField;
		}
		
		public ComboBox<Color> getSegmentsColorField() {
			return segmentColorField;
		}
		
		public ComboBox<String> yColumnField() {
			return yColumnField;
		}
		
		public ComboBox<String> xColumnField() {
			return xColumnField;
		}
		
		public void setXColumnField(ComboBox<String> xColumnField) {
			this.xColumnField = xColumnField;
		}
					
		public JFXCheckBox getDrawSegmentsField() {
			return drawSegmentsColumn;
		}
		
		public JFXTextField getWidthField() {
			return widthField;
		}
		
		public String getWidth() {
			return widthField.getText();
		}
		
		public String getSegmentsWidth() {
			return segmentsWidthField.getText();
		}
		
		public JFXTextField getSegmentsWidthField() {
			return segmentsWidthField;
		}
		
		public String getType() {
			return typeField.getSelectionModel().getSelectedItem();
		}
		
		public boolean drawSegments() {
			return drawSegmentsColumn.isSelected();
		}
		
		public String getYColumn() {
			return yColumnField.getSelectionModel().getSelectedItem();
		}
		
		public String getXColumn() {
			return xColumnField.getSelectionModel().getSelectedItem();
		}
		
		public Color getColor() {
			return colorField.getSelectionModel().getSelectedItem();
		}
		
		public Color getSegmentsColor() {
			return segmentColorField.getSelectionModel().getSelectedItem();
		}
		
		public MARSResultsTable getDataTable() {
			return table;
		}
		
		static class ColorRectCell extends ListCell<Color> {
		      @Override
		      public void updateItem(Color item, boolean empty) {
		          super.updateItem(item, empty);
		          Rectangle rect = new Rectangle(15,15);
		          if(item != null){
		              rect.setFill(item);
		              setGraphic(rect);
		          }
		      }
		}
}

