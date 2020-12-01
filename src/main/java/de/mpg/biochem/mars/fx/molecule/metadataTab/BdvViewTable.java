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
package de.mpg.biochem.mars.fx.molecule.metadataTab;
import java.io.File;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.CustomTextField;

import com.jfoenix.controls.JFXCheckBox;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataEventHandler;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import javafx.stage.FileChooser;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.cell.TextFieldTableCell;

import javafx.scene.layout.FlowPane;

public class BdvViewTable implements MetadataEventHandler {
    
	protected MarsMetadata marsImageMetadata;
	
	protected BorderPane rootPane;
	
    protected CustomTextField addBdvSourceNameField;
    protected TableView<MarsBdvSource> bdvTable;
    protected ObservableList<MarsBdvSource> bdvRowList = FXCollections.observableArrayList();
    
    protected Button typeButton;
    protected int buttonType = 0;

    public BdvViewTable() {        
    	bdvTable = new TableView<MarsBdvSource>();
    	addBdvSourceNameField = new CustomTextField();
    	
    	TableColumn<MarsBdvSource, MarsBdvSource> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<MarsBdvSource, MarsBdvSource>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(MarsBdvSource pRow, boolean empty) {
                super.updateItem(pRow, empty);

                if (pRow == null) {
                    setGraphic(null);
                    return;
                }
                
                removeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS, "1.0em"));
        		removeButton.setCenterShape(true);
        		removeButton.setStyle(
                        "-fx-background-radius: 5em; " +
                        "-fx-min-width: 18px; " +
                        "-fx-min-height: 18px; " +
                        "-fx-max-width: 18px; " +
                        "-fx-max-height: 18px;"
                );
        		
                setGraphic(removeButton);
                removeButton.setOnAction(e -> {
        			marsImageMetadata.removeBdvSource(pRow.getName());
        			loadBdvSources();
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
    	bdvTable.getColumns().add(deleteColumn);

        TableColumn<MarsBdvSource, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> { 
        	String newRegionName = event.getNewValue();
        	if (!marsImageMetadata.hasBdvSource(newRegionName)) {
        		MarsBdvSource bdvSource = event.getRowValue();
        		String oldName = bdvSource.getName();
        		marsImageMetadata.removeBdvSource(oldName);
        		
        		bdvSource.setName(newRegionName);
        		marsImageMetadata.putBdvSource(bdvSource);
        	} else {
        		((MarsBdvSource) event.getTableView().getItems()
        	            .get(event.getTablePosition().getRow())).setName(event.getOldValue());
        		bdvTable.refresh();
        	}
        });
        nameColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(bdvSource.getValue().getName())
        );
        nameColumn.setSortable(false);
        nameColumn.setPrefWidth(100);
        nameColumn.setMinWidth(100);
        nameColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        bdvTable.getColumns().add(nameColumn);
        
        TableColumn<MarsBdvSource, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        typeColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>((bdvSource.getValue().isN5()) ? "N5" : "HD5")
        );
        typeColumn.setSortable(false);
        typeColumn.setPrefWidth(100);
        typeColumn.setMinWidth(100);
        typeColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        bdvTable.getColumns().add(typeColumn);
        
        TableColumn<MarsBdvSource, String> m00Column = buildAffineColumn("m00", 0, 0);
        bdvTable.getColumns().add(m00Column);
        TableColumn<MarsBdvSource, String> m01Column = buildAffineColumn("m01", 0, 1);
        bdvTable.getColumns().add(m01Column);
        TableColumn<MarsBdvSource, String> m02Column = buildAffineColumn("m02", 0, 3);
        bdvTable.getColumns().add(m02Column);
        TableColumn<MarsBdvSource, String> m10Column = buildAffineColumn("m10", 1, 0);
        bdvTable.getColumns().add(m10Column);
        TableColumn<MarsBdvSource, String> m11Column = buildAffineColumn("m11", 1, 1);
        bdvTable.getColumns().add(m11Column);
        TableColumn<MarsBdvSource, String> m12Column = buildAffineColumn("m12", 1, 3);
        bdvTable.getColumns().add(m12Column);
        
        TableColumn<MarsBdvSource, MarsBdvSource> driftCorrectColumn = new TableColumn<>("Drift Correct");
        driftCorrectColumn.setPrefWidth(40);
        driftCorrectColumn.setMinWidth(40);
        driftCorrectColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        driftCorrectColumn.setCellFactory(param -> new TableCell<MarsBdvSource, MarsBdvSource>() {
            private final JFXCheckBox checkbox = new JFXCheckBox();

            @Override
            protected void updateItem(MarsBdvSource pRow, boolean empty) {
                super.updateItem(pRow, empty);

                if (pRow == null) {
                    setGraphic(null);
                    return;
                }
                
                checkbox.setSelected(pRow.getCorrectDrift());
                
                setGraphic(checkbox);
                checkbox.setOnAction(e -> {
        			if (checkbox.isSelected())
        				pRow.setCorrectDrift(true);
        			else 
        				pRow.setCorrectDrift(false);
        		});
            }
        });
    	driftCorrectColumn.setStyle("-fx-alignment: CENTER;");
    	driftCorrectColumn.setSortable(false);
    	bdvTable.getColumns().add(driftCorrectColumn);

    	TableColumn<MarsBdvSource, String> channelColumn = buildEntryFieldColumn("C");
    	channelColumn.setOnEditCommit(event -> { 
        	try {
    			int num = Integer.valueOf(event.getNewValue());
    			event.getRowValue().setChannel(num);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
    	channelColumn.setCellValueFactory(bdvSource -> {
    		String str = String.valueOf(bdvSource.getValue().getChannel());
    		return new ReadOnlyObjectWrapper<>(str);
    	});
    	bdvTable.getColumns().add(channelColumn);
    	
    	TableColumn<MarsBdvSource, String> datasetColumn = buildEntryFieldColumn("N5 Dataset");
    	datasetColumn.setOnEditCommit(event -> event.getRowValue().setN5Dataset(event.getNewValue()));
    	datasetColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(String.valueOf(bdvSource.getValue().getN5Dataset()))
        );
        bdvTable.getColumns().add(datasetColumn);
    	
        TableColumn<MarsBdvSource, String> pathColumn = buildEntryFieldColumn("Path");
        pathColumn.setOnEditCommit(event -> event.getRowValue().setPath(event.getNewValue()));
        pathColumn.setCellValueFactory(bdvSource ->
                new ReadOnlyObjectWrapper<>(String.valueOf(bdvSource.getValue().getPath()))
        );
        bdvTable.getColumns().add(pathColumn);
        
        bdvTable.setItems(bdvRowList);
        bdvTable.setEditable(true);

		Button addButton = new Button();
		addButton.setGraphic(FontAwesomeIconFactory.get().createIcon(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS, "1.0em"));
		addButton.setCenterShape(true);
		addButton.setStyle(
                "-fx-background-radius: 5em; " +
                "-fx-min-width: 18px; " +
                "-fx-min-height: 18px; " +
                "-fx-max-width: 18px; " +
                "-fx-max-height: 18px;"
        );
		addButton.setOnAction(e -> {
			if (!addBdvSourceNameField.getText().equals("") && !marsImageMetadata.hasBdvSource(addBdvSourceNameField.getText())) {
				MarsBdvSource bdvSource = new MarsBdvSource(addBdvSourceNameField.getText());
				
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

				File path = fileChooser.showOpenDialog(getNode().getScene().getWindow());
				
				if (path != null) {
					bdvSource.setPath(path.getAbsolutePath());

				switch (this.buttonType) {
					case 0:
						bdvSource.setN5(true);
			    		break;
					case 1:
						bdvSource.setHD5(true);
						break;
				}
					marsImageMetadata.putBdvSource(bdvSource);
					loadBdvSources();
				}
			}
		});
		
		addBdvSourceNameField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (addBdvSourceNameField.getText().isEmpty()) {
        		addBdvSourceNameField.setRight(new Label(""));
        	} else {
        		addBdvSourceNameField.setRight(addButton);
        	}
        });
		addBdvSourceNameField.setStyle(
                "-fx-background-radius: 2em; "
        );
		
		typeButton = new Button();
        typeButton.setText("N5");
        typeButton.setCenterShape(true);
        typeButton.setStyle(
                "-fx-background-radius: 2em; " +
                "-fx-min-width: 60px; " +
                "-fx-min-height: 30px; " +
                "-fx-max-width: 60px; " +
                "-fx-max-height: 30px;"
        );
        typeButton.setOnAction(e -> {
        	buttonType++;
        	if (buttonType > 1)
        		buttonType = 0;
        	
			switch (buttonType) {
				case 0:
					typeButton.setText("N5");
					typeButton.setGraphic(null);
					break;
				case 1:
					typeButton.setText("HD5");
					typeButton.setGraphic(null);
					break;
			}
		});
        
        BorderPane bomttomPane = new BorderPane();
        bomttomPane.setCenter(addBdvSourceNameField);
        bomttomPane.setLeft(typeButton);

        rootPane = new BorderPane();
        Insets insets = new Insets(5);
        
        rootPane.setCenter(bdvTable);
        rootPane.setBottom(addBdvSourceNameField);
        BorderPane.setMargin(bomttomPane, insets);
        
        getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
    }
    
    protected TableColumn<MarsBdvSource, String> buildAffineColumn(String name, int rowIndex, int columnIndex) {
    	TableColumn<MarsBdvSource, String> column = buildEntryFieldColumn(name);
    	column.setOnEditCommit(event -> { 
        	try {
    			double num = Double.valueOf(event.getNewValue());
    			event.getRowValue().getAffineTransform3D().set(num, rowIndex, columnIndex);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
        });
    	column.setCellValueFactory(bdvSource -> {
    		String str = "";
    		if (bdvSource.getValue().getAffineTransform3D() != null)
    			str = String.valueOf(bdvSource.getValue().getAffineTransform3D().get(rowIndex, columnIndex));
    		
    		return new ReadOnlyObjectWrapper<>(str);
    	});
    	return column;
    }
    
    protected TableColumn<MarsBdvSource, String> buildEntryFieldColumn(String name) {
    	TableColumn<MarsBdvSource, String> column = new TableColumn<>(name);
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setSortable(false);
        column.setPrefWidth(100);
        column.setMinWidth(100);
        column.setEditable(true);
        column.setStyle( "-fx-alignment: CENTER-LEFT;");
        return column;
    }
    
    public Node getNode() {
    	return rootPane;
    }
    
    public void loadBdvSources() {
    	bdvRowList.setAll(marsImageMetadata.getBdvSourceNames().stream().map(name -> marsImageMetadata.getBdvSource(name)).collect(Collectors.toList()));
	}

	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMetadataSelectionChangedEvent(MarsMetadata marsImageMetadata) {
		this.marsImageMetadata = marsImageMetadata;
		loadBdvSources();
	}
}
