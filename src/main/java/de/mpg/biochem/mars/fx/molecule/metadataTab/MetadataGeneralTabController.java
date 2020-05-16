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

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE;

import java.util.ArrayList;

import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXDefaultChip;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.skins.JFXChipViewSkin;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXChip;

import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.jensd.fx.glyphs.octicons.utils.OctIconFactory;
import de.mpg.biochem.mars.fx.event.DefaultMoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.MetadataEvent;
import de.mpg.biochem.mars.fx.event.MetadataTagsChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeTagsChangedEvent;
import de.mpg.biochem.mars.fx.util.MarsJFXChipViewSkin;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class MetadataGeneralTabController implements MetadataSubPane {

	@FXML
	private AnchorPane rootPane;
	
	@FXML
	private BorderPane UIDIconContainer;
	
	@FXML
	private JFXTextField UIDLabel;
	
	@FXML
	private JFXButton UIDClippyButton;
	
	@FXML
	private Label Tags;
	
	@FXML
	private JFXChipView<String> chipView;
	
	@FXML
	private Label Notes;
	
	@FXML
	private JFXTextArea notesTextArea;
	
	final Clipboard clipboard = Clipboard.getSystemClipboard();
	
	private MoleculeArchive<Molecule,MarsMetadata,MoleculeArchiveProperties> archive;
	
	private ListChangeListener<String> chipsListener;
	private ChangeListener<String> notesListener;
	
	private MarsMetadata marsImageMetadata;
	
	@FXML
    public void initialize() {
		Region microscopeIcon = new Region();
        microscopeIcon.getStyleClass().add("microscopeIcon");
        UIDIconContainer.setCenter(microscopeIcon);
        UIDClippyButton.setGraphic(OctIconFactory.get().createIcon(de.jensd.fx.glyphs.octicons.OctIcon.CLIPPY, "1.3em"));
		
		UIDLabel.setEditable(false);
		
		notesTextArea.setPromptText("none");
		
		getNode().addEventHandler(MetadataEvent.METADATA_EVENT, this);
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new DefaultMoleculeArchiveEventHandler() {
        	@Override
        	public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> newArchive) {
        		archive = newArchive;
        	}
        });
		
		chipsListener = new ListChangeListener<String>() {
			@Override
			public void onChanged(Change<? extends String> c) {
				if (marsImageMetadata == null)
					return;
				
				while (c.next()) {
		             if (c.wasRemoved()) {
		            	 marsImageMetadata.removeTag(c.getRemoved().get(0));
		             } else if (c.wasAdded()) {
		            	 marsImageMetadata.addTag(c.getAddedSubList().get(0));
		             }
				}
				getNode().fireEvent(new MetadataTagsChangedEvent(marsImageMetadata));
			}
		};
	
		notesListener = new ChangeListener<String>() {
		    @Override
		    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
		        marsImageMetadata.setNotes(notesTextArea.getText());
		    }
		};
		
		MarsJFXChipViewSkin<String> skin = new MarsJFXChipViewSkin<>(chipView);
		chipView.setSkin(skin);
    }
	
	@FXML
	private void handleUIDClippy() {
		ClipboardContent content = new ClipboardContent();
	    content.putString(UIDLabel.getText());
	    clipboard.setContent(content);
	}

	public Node getNode() {
		return rootPane;
	}

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMetadataSelectionChangedEvent(MarsMetadata marsImageMetadata) {
		this.marsImageMetadata = marsImageMetadata;
		
		UIDLabel.setText(marsImageMetadata.getUID());
		
		chipView.getChips().removeListener(chipsListener);
		chipView.getChips().clear();
		if (marsImageMetadata.getTags().size() > 0)
			chipView.getChips().addAll(marsImageMetadata.getTags());

		chipView.getSuggestions().clear();
		chipView.getSuggestions().addAll(archive.properties().getTagSet());
		chipView.getChips().addListener(chipsListener);
		
		notesTextArea.textProperty().removeListener(notesListener);
		notesTextArea.setText(marsImageMetadata.getNotes());
		notesTextArea.textProperty().addListener(notesListener);
	}

	@Override
	public void handle(MetadataEvent event) {
		event.invokeHandler(this);
		event.consume();
	}
}
