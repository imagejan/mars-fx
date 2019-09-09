package de.mpg.biochem.mars.fx.molecule.metadataTab;

import java.io.IOException;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.net.URL;
import java.util.ArrayList;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.mpg.biochem.mars.fx.event.DefaultMoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MarsImageMetadataEvent;
import de.mpg.biochem.mars.fx.event.MarsImageMetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
//import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public abstract class AbstractMetadataPropertiesPane<I extends MarsImageMetadata> implements MetadataSubPane {
	
	protected StackPane stackPane;
	protected JFXTabPane tabsContainer;
	protected Tab generalTab;
	protected AnchorPane generalTabContainer;
	
	protected Tab propertiesTab;
	protected AnchorPane propertiesTabContainer;
	
	protected MetadataGeneralTabController metadataGeneralTabController;
	protected MetadataPropertiesTable metadataPropertiesTable;
	
	protected double tabWidth = 60.0;
	protected int lastSelectedTabIndex = 0;
	
	protected I marsImageMetadata;
	
	protected MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
	public AbstractMetadataPropertiesPane() {
		stackPane = new StackPane();
		stackPane.prefWidth(220.0);
		stackPane.prefHeight(220.0);
		stackPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/metadataTab/MetadataOverview.css");
		
		tabsContainer = new JFXTabPane();
		tabsContainer.prefHeight(350.0);
		tabsContainer.prefWidth(220.0);
		tabsContainer.setSide(Side.TOP);
		tabsContainer.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabsContainer.setTabMinWidth(tabWidth);
        tabsContainer.setTabMaxWidth(tabWidth);
        tabsContainer.setTabMinHeight(30.0);
        tabsContainer.setTabMaxHeight(30.0);
        tabsContainer.disableAnimationProperty();
        
        stackPane.getChildren().add(tabsContainer);
        
        getNode().addEventHandler(MarsImageMetadataEvent.MARS_IMAGE_METADATA_EVENT, this);
        getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new DefaultMoleculeArchiveEventHandler() {
        	@Override
        	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> newArchive) {
        		archive = newArchive;
        		
        		metadataGeneralTabController.fireEvent(new InitializeMoleculeArchiveEvent(newArchive));
        		metadataPropertiesTable.fireEvent(new InitializeMoleculeArchiveEvent(newArchive));
        	}
        });
		
		configureTabs();
	}
	
	private void configureTabs() {        
        //Build general Tab
        BorderPane tabPane = new BorderPane();
        tabPane.setMaxWidth(tabWidth);
        tabPane.setCenter(FontAwesomeIconFactory.get().createIcon(INFO_CIRCLE, "1.1em"));

        generalTab = new Tab();
        generalTabContainer = new AnchorPane();
        generalTab.setText("");
        generalTab.setGraphic(tabPane);
        generalTab.closableProperty().set(false);
        
        URL resourceURL = getClass().getResource("MetadataGeneralTab.fxml");
        
        generalTabContainer = new AnchorPane();
        generalTabContainer.minHeight(0.0);
        generalTabContainer.minWidth(0.0);
        generalTabContainer.prefHeight(250.0);
        generalTabContainer.prefWidth(220.0);
        generalTab.setContent(generalTabContainer);
        
        try {
        	FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(resourceURL);
            Parent contentView = loader.load();
            
            metadataGeneralTabController = (MetadataGeneralTabController) loader.getController();
            generalTabContainer.getChildren().add(contentView);
            
            AnchorPane.setTopAnchor(contentView, 0.0);
            AnchorPane.setBottomAnchor(contentView, 0.0);
            AnchorPane.setRightAnchor(contentView, 0.0);
            AnchorPane.setLeftAnchor(contentView, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        //Build properties Tab
        metadataPropertiesTable = new MetadataPropertiesTable();
       	
    	BorderPane propertiesTabPane = new BorderPane();
    	propertiesTabPane.setMaxWidth(tabWidth);
    	propertiesTabPane.setCenter(FontAwesomeIconFactory.get().createIcon(LIST_ALT, "1.1em"));

    	propertiesTab = new Tab();
        propertiesTab.setText("");
        propertiesTab.setGraphic(propertiesTabPane);
        propertiesTab.closableProperty().set(false);
        
        propertiesTabContainer = new AnchorPane();
        propertiesTabContainer.minHeight(0.0);
        propertiesTabContainer.minWidth(0.0);
        propertiesTabContainer.prefHeight(250.0);
        propertiesTabContainer.prefWidth(220.0);
        
        propertiesTabContainer.getChildren().add(metadataPropertiesTable.getNode());
        AnchorPane.setTopAnchor(metadataPropertiesTable.getNode(), 0.0);
        AnchorPane.setBottomAnchor(metadataPropertiesTable.getNode(), 0.0);
        AnchorPane.setRightAnchor(metadataPropertiesTable.getNode(), 0.0);
        AnchorPane.setLeftAnchor(metadataPropertiesTable.getNode(), 0.0);
        propertiesTab.setContent(propertiesTabContainer);

        tabsContainer.getTabs().add(generalTab);
        tabsContainer.getTabs().add(propertiesTab);
	}
	
	@Override
	public Node getNode() {
		return stackPane;
	}
	
	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMarsImageMetadataSelectionChangedEvent(MarsImageMetadata marsImageMetadata) {
		this.marsImageMetadata = (I) marsImageMetadata;
		
		metadataGeneralTabController.fireEvent(new MarsImageMetadataSelectionChangedEvent(marsImageMetadata));
		metadataPropertiesTable.fireEvent(new MarsImageMetadataSelectionChangedEvent(marsImageMetadata));
    }
	
	@Override
    public void handle(MarsImageMetadataEvent event) {
	   event.invokeHandler(this);
	   event.consume();
    } 
}