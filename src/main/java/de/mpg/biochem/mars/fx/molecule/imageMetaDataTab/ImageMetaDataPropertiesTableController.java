package de.mpg.biochem.mars.fx.molecule.imageMetaDataTab;

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.textfield.CustomTextField;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.plot.PlotSeries;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

public class ImageMetaDataPropertiesTableController implements ImageMetaDataSubTab {
    
	private MarsImageMetadata meta;
	
	private BorderPane borderPane;
	
    private CustomTextField addParameterField;
    private TableView<ParameterRow> parameterTable;
    private ObservableList<ParameterRow> parameterRowList = FXCollections.observableArrayList();

    public ImageMetaDataPropertiesTableController() {        
        initialize();
    }

    private void initialize() {
    	parameterTable = new TableView<ParameterRow>();
    	addParameterField = new CustomTextField();
    	
    	TableColumn<ParameterRow, ParameterRow> deleteColumn = new TableColumn<>();
    	deleteColumn.setPrefWidth(30);
    	deleteColumn.setMinWidth(30);
    	deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
    	deleteColumn.setCellFactory(param -> new TableCell<ParameterRow, ParameterRow>() {
            private final Button removeButton = new Button();

            @Override
            protected void updateItem(ParameterRow pRow, boolean empty) {
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
        			meta.removeParameter(pRow.getName());
        			loadData();
        		});
            }
        });
    	deleteColumn.setStyle( "-fx-alignment: CENTER;");
    	deleteColumn.setSortable(false);
        parameterTable.getColumns().add(deleteColumn);

        TableColumn<ParameterRow, String> ParameterColumn = new TableColumn<>("Parameter");
        ParameterColumn.setCellValueFactory(parameterRow ->
                new ReadOnlyObjectWrapper<>(parameterRow.getValue().getName())
        );
        ParameterColumn.setSortable(false);
        ParameterColumn.setPrefWidth(100);
        ParameterColumn.setMinWidth(100);
        ParameterColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        parameterTable.getColumns().add(ParameterColumn);
        
        //TODO how to also allow editing of this parameter
        TableColumn<ParameterRow, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(
                event -> event.getRowValue().setValue(event.getNewValue()));
        valueColumn.setCellValueFactory(parameterRow ->
                new ReadOnlyObjectWrapper<>(parameterRow.getValue().getValue())
        );
        valueColumn.setSortable(false);
        valueColumn.setPrefWidth(100);
        valueColumn.setMinWidth(100);
        valueColumn.setEditable(true);
        valueColumn.setStyle( "-fx-alignment: CENTER-LEFT;");
        parameterTable.getColumns().add(valueColumn);
        
        parameterTable.setItems(parameterRowList);
        parameterTable.setEditable(true);

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
			if (!addParameterField.getText().equals(""))
			meta.setParameter(addParameterField.getText(), Double.NaN);
			loadData();
		});
		
        addParameterField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (addParameterField.getText().isEmpty()) {
        		addParameterField.setRight(new Label(""));
        	} else {
        		addParameterField.setRight(addButton);
        	}
        });
        addParameterField.setStyle(
                "-fx-background-radius: 2em; "
        );

        borderPane = new BorderPane();
        Insets insets = new Insets(5);
        
        borderPane.setCenter(parameterTable);
        
        borderPane.setBottom(addParameterField);
        BorderPane.setMargin(addParameterField, insets);
    }
    
    public Node getNode() {
    	return borderPane;
    }
    
    public void loadData() {
    	parameterRowList.clear();

    	for (String parameter : meta.getParameters().keySet()) {
        	parameterRowList.add(new ParameterRow(parameter));
        }
	}
    
    private class ParameterRow {
    	private String parameter;
    	
    	ParameterRow(String parameter) {
    		this.parameter = parameter;
    	}
    	
    	public String getName() {
    		return parameter;
    	}
    	
    	public String getValue() {
    		return String.valueOf(meta.getParameter(parameter));
    	}
    	
    	public void setValue(String value) {
    		try {
    			double num = Double.valueOf(value);
    			meta.setParameter(getName(), num);
    		} catch (NumberFormatException e) {
    			//Do nothing for the moment...
    		}
    	}
    }

	@Override
	public void setImageMetaData(MarsImageMetadata meta) {
		this.meta = meta;
    	loadData();
	}
}
