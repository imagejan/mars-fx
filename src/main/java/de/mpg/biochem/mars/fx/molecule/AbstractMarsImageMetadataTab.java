package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;
import org.controlsfx.control.textfield.CustomTextField;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.molecule.AbstractMoleculesTab.MoleculeIndexRow;
import de.mpg.biochem.mars.fx.molecule.metadataTab.*;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;

public abstract class AbstractMarsImageMetadataTab<I extends MarsImageMetadata, C extends MetadataSubPane, O extends MetadataSubPane> extends AbstractMoleculeArchiveTab implements MarsImageMetadataTab<C,O> {
	
	protected SplitPane rootPane;
	protected C metadataCenterPane;
	protected O metadataPropertiesPane;
	
	protected I marsImageMetadata;
	
	protected CustomTextField filterField;
	protected Label nOfHitCountLabel;
	protected TableView<MetaIndexRow> metaIndexTable;
	protected ObservableList<MetaIndexRow> metaRowList = FXCollections.observableArrayList();
    
	protected FilteredList<MetaIndexRow> filteredData;
	
	protected ChangeListener<MetaIndexRow> metaIndexTableListener;

	public AbstractMarsImageMetadataTab() {
		super();
		
		Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
        
		setIcon(microscopeIcon);
		
		rootPane = new SplitPane();
		ObservableList<Node> splitItems = rootPane.getItems();
		
		Node metadataTableIndexContainer = buildMetadataTableIndex();
		SplitPane.setResizableWithParent(metadataTableIndexContainer, Boolean.FALSE);
		splitItems.add(metadataTableIndexContainer);
		
		metadataCenterPane = createMetadataCenterPane();
		splitItems.add(metadataCenterPane.getNode());
		
		metadataPropertiesPane = createMetadataPropertiesPane();
		SplitPane.setResizableWithParent(metadataPropertiesPane.getNode(), Boolean.FALSE);
		splitItems.add(metadataPropertiesPane.getNode());
		
		rootPane.setDividerPositions(0.15f, 0.87f);
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		getNode().addEventFilter(MetadataEvent.METADATA_EVENT, new EventHandler<MetadataEvent>() { 
			@SuppressWarnings("unchecked")
			@Override
			public void handle(MetadataEvent e) {
				if (e.getEventType().getName().equals("REFRESH_METADATA_EVENT")) {
					//We reload the record from the archive.. If virtual this will reload from disk...
					marsImageMetadata = (I) archive.getImageMetadata(marsImageMetadata.getUID());
		        	
		        	//Update center pane and properties pane.
		        	metadataCenterPane.fireEvent(new MetadataSelectionChangedEvent(marsImageMetadata));
		        	metadataPropertiesPane.fireEvent(new MetadataSelectionChangedEvent(marsImageMetadata));
					Platform.runLater(() -> {
						metaIndexTable.requestFocus();
						//metaIndexTable.getSelectionModel().select(metaIndexTable.getSelectionModel().selectedItemProperty().get());
					});
					e.consume();
				} else if (e.getEventType().getName().equals("TAGS_CHANGED")) {
					metaIndexTable.refresh();
				    e.consume();
			    }
			}
		});
		
		
		setContent(rootPane);
	}
	
	@SuppressWarnings("unchecked")
	protected Node buildMetadataTableIndex() {
    	metaIndexTable = new TableView<MetaIndexRow>();
    	
        TableColumn<MetaIndexRow, Integer> rowIndexCol = new TableColumn<>("Index");
        rowIndexCol.setCellValueFactory(metaIndexRow ->
                new ReadOnlyObjectWrapper<>(metaIndexRow.getValue().getIndex())
        );
        rowIndexCol.setPrefWidth(50);
        rowIndexCol.setSortable(false);
        metaIndexTable.getColumns().add(rowIndexCol);

        TableColumn<MetaIndexRow, String> UIDColumn = new TableColumn<>("UID");
        UIDColumn.setCellValueFactory(metaIndexRow ->
                new ReadOnlyObjectWrapper<>(metaIndexRow.getValue().getUID())
        );
        UIDColumn.setSortable(false);
        metaIndexTable.getColumns().add(UIDColumn);
        
        TableColumn<MetaIndexRow, String> TagsColumn = new TableColumn<>("Tags");
        TagsColumn.setCellValueFactory(metaIndexRow ->
                new ReadOnlyObjectWrapper<>(metaIndexRow.getValue().getTags())
        );
        TagsColumn.setSortable(false);
        metaIndexTable.getColumns().add(TagsColumn);
        
        metaIndexTableListener = new ChangeListener<MetaIndexRow> () {
        	public void changed(ObservableValue<? extends MetaIndexRow> observable, MetaIndexRow oldMetaIndexRow, MetaIndexRow newMetaIndexRow) {
        		//Need to save the current record when we change in the case the virtual storage.
	        	saveCurrentRecord();
        		
        		if (newMetaIndexRow != null) {
                	marsImageMetadata = (I) archive.getImageMetadata(newMetaIndexRow.getUID());
                	
                	//Update center pane and properties pane.
                	metadataCenterPane.getNode().fireEvent(new MetadataSelectionChangedEvent(marsImageMetadata));
                	metadataPropertiesPane.getNode().fireEvent(new MetadataSelectionChangedEvent(marsImageMetadata));
            		Platform.runLater(() -> {
            			metaIndexTable.requestFocus();
            		});
                }
        	}
        };
        
        metaIndexTable.getSelectionModel().selectedItemProperty().addListener(metaIndexTableListener);
        
        filteredData = new FilteredList<>(metaRowList, p -> true);
        
        filterField = new CustomTextField();
        nOfHitCountLabel = new Label();
        
        filterField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH));
        filterField.setRight(nOfHitCountLabel);
        filterField.getStyleClass().add("find");
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(metaIndexRow -> {
                // If filter text is empty, display everything.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                if (Integer.toString(metaIndexRow.getIndex()).contains(newValue)) {
                    return true;
                } else if (metaIndexRow.getUID().contains(newValue)) {
                    return true;
                } else if (metaIndexRow.getTags().contains(newValue)) {
                	return true;
                }
                return false;
            });
            nOfHitCountLabel.setText(filterField.getText().isEmpty() ? "" : "" + filteredData.size());
        });
        
        metaIndexTable.setItems(filteredData);
        
        filterField.setStyle("-fx-background-radius: 2em; ");

        BorderPane borderPane = new BorderPane();
        Insets insets = new Insets(5);
       
        borderPane.setTop(filterField);
        BorderPane.setMargin(filterField, insets);
        
        borderPane.setCenter(metaIndexTable);
        
        return borderPane;
	}
    
    public void saveCurrentRecord() {
    	if (marsImageMetadata != null)
    		archive.putImageMetadata(marsImageMetadata);
    }
    
    public MarsImageMetadata getSelectedMetadata() {
    	return marsImageMetadata;
    }

    @Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);

    	metadataCenterPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	metadataPropertiesPane.fireEvent(new InitializeMoleculeArchiveEvent(archive));
    	onMoleculeArchiveUnlockEvent();
    }
	
	@Override
	public Node getNode() {
		return rootPane;
	}
	
	@Override
	public ArrayList<Menu> getMenus() {
		return new ArrayList<Menu>();
	}
	
	public abstract C createMetadataCenterPane();
	
	public abstract O createMetadataPropertiesPane();
	
	protected class MetaIndexRow {
    	private int index;
    	
    	MetaIndexRow(int index) {
    		this.index = index;
    	}
    	
    	int getIndex() {
    		return index;
    	}
    	
    	String getUID() {
    		return archive.getImageMetadataUIDAtIndex(index);
    	}
    	
    	String getTags() {
    		return archive.getImageMetadataTagList(archive.getImageMetadataUIDAtIndex(index));
    	}
    }

	@Override
	public void onMoleculeArchiveLockEvent() {
		saveCurrentRecord();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeArchiveUnlockEvent() {
		metaIndexTable.getSelectionModel().selectedItemProperty().removeListener(metaIndexTableListener);
    	metaRowList.clear();
    	if (archive.getNumberOfImageMetadataRecords() > 0) {
    		for (int index = 0; index < archive.getNumberOfImageMetadataRecords(); index++) {
    			metaRowList.add(new MetaIndexRow(index));
    		}
		
    		MetaIndexRow newMetaIndexRow = new MetaIndexRow(0);
    		marsImageMetadata = (I) archive.getImageMetadata(newMetaIndexRow.getUID());
    		metaIndexTable.getSelectionModel().select(0);
        	metadataCenterPane.fireEvent(new MetadataSelectionChangedEvent(marsImageMetadata));
        	metadataPropertiesPane.fireEvent(new MetadataSelectionChangedEvent(marsImageMetadata));
    	}
		Platform.runLater(() -> {
			metaIndexTable.requestFocus();
		});
		metaIndexTable.getSelectionModel().selectedItemProperty().addListener(metaIndexTableListener);
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {
		saveCurrentRecord();
	}
}
