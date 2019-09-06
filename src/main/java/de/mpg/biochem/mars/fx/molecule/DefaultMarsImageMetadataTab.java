package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.fx.molecule.metadataTab.DefaultMetadataCenterPane;
import de.mpg.biochem.mars.fx.molecule.metadataTab.DefaultMetadataPropertiesPane;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;

public class DefaultMarsImageMetadataTab extends AbstractMarsImageMetadataTab<MarsImageMetadata, DefaultMetadataCenterPane, DefaultMetadataPropertiesPane> {
	
	public DefaultMarsImageMetadataTab() {
		super();
	}

	@Override
	public DefaultMetadataCenterPane createMetadataCenterPane() {
		return new DefaultMetadataCenterPane();
	}

	@Override
	public DefaultMetadataPropertiesPane createMetadataPropertiesPane() {
		return new DefaultMetadataPropertiesPane();
	}
}
