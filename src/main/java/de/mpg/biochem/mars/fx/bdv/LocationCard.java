package de.mpg.biochem.mars.fx.bdv;

import static java.util.stream.Collectors.toList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import de.mpg.biochem.mars.table.MarsTableRow;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

public class LocationCard extends JPanel implements MarsBdvCard {

	private final JTextField magnificationField, radiusField;
	private final JCheckBox showCircle, showTrack, followTrack, showLabel, roverSync, rainbowColor, showAll;
	
	private final JComboBox<String> locationSource, tLocation, xLocation, yLocation;
	
	private MoleculeLocationOverlay moleculeLocationOverlay;
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	private Molecule molecule;
	
	private boolean active = false;
	
	public static HashMap<String, Color> moleculeRainbowColors;
	public Random ran = new Random();
	
	public LocationCard(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
		setLayout(new GridLayout(0, 2));
		
		Set<String> columnNames = archive.properties().getColumnSet();
		Set<String> parameterNames = archive.properties().getParameterSet();
		
		add(new JLabel("Source"));
		locationSource = new JComboBox<>(new String[] {"Table", "Parameters"});
		add(locationSource);
		add(new JLabel("T"));
		tLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		tLocation.setSelectedItem("T");
		add(tLocation);
		add(new JLabel("X"));
		xLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		xLocation.setSelectedItem("x");
		add(xLocation);
		add(new JLabel("Y"));
		yLocation = new JComboBox<>(columnNames.stream().sorted().collect(toList()).toArray(new String[0]));
		yLocation.setSelectedItem("y");
		add(yLocation);
		
		locationSource.addActionListener(new ActionListener( ) {
		      public void actionPerformed(ActionEvent e) {
		        String sourceName = (String)locationSource.getSelectedItem();
		        
		        tLocation.removeAllItems();
		        xLocation.removeAllItems();
		    	yLocation.removeAllItems();  
		        
		    	for (String item : parameterNames.stream().sorted().collect(toList())) {
					tLocation.addItem(item);
					xLocation.addItem(item);
					yLocation.addItem(item);
		    	}
						
				if (!sourceName.equals("Parameters")) {
					tLocation.setSelectedItem("T");
					xLocation.setSelectedItem("x");
					yLocation.setSelectedItem("y");
				}
			}
		});
		
		showCircle = new JCheckBox("circle", false);
		add(showCircle);
		showLabel = new JCheckBox("label", false);
		add(showLabel);
		showTrack = new JCheckBox("track", false);
		add(showTrack);
		followTrack = new JCheckBox("follow", false);
		add(followTrack);
		showAll = new JCheckBox("all", false);
		add(showAll);
		roverSync = new JCheckBox("rover sync", false);
		add(roverSync);
		rainbowColor = new JCheckBox("rainbow", false);
		add(rainbowColor);
		add(new JPanel());
		
		add(new JLabel("Radius"));
		
		radiusField = new JTextField(6);
		radiusField.setText("5");
		Dimension dimScaleField = new Dimension(100, 20);
		radiusField.setMinimumSize(dimScaleField);
		
		add(radiusField);
		add(new JLabel("Scale factor"));
		
		magnificationField = new JTextField(6);
		magnificationField.setText("10");
		magnificationField.setMinimumSize(dimScaleField);
		
		add(magnificationField);
	}
	
	public boolean showLocationOverlay() {
		if (showCircle.isSelected() || showTrack.isSelected() || followTrack.isSelected() || showLabel.isSelected())
			return true;
		else
			return false;
	}
	
	public boolean showCircle() {
		return showCircle.isSelected();
	}
	
	public boolean showLabel() {
		return showLabel.isSelected();
	}
	
	public boolean showAll() {
		return showAll.isSelected();
	}
	
	public boolean roverSync() {
		return roverSync.isSelected();
	}
	
	public boolean rainbowColor() {
		return rainbowColor.isSelected();
	}
	
	public boolean showTrack() {
		return showTrack.isSelected();
	}
	
	public boolean followTrack() {
		return followTrack.isSelected();
	}
	
	public String getTLocationSource() {
		return (String) tLocation.getSelectedItem();
	}
	
	public String getXLocationSource() {
		return (String) xLocation.getSelectedItem();
	}
	
	public String getYLocationSource() {
		return (String) yLocation.getSelectedItem();
	}
	
	public boolean useParameters() {
		return locationSource.getSelectedItem().equals("Parameters");
	}

	public double getMagnification() {
		return Double.valueOf(magnificationField.getText());
	}
	
	public double getRadius() {
		return Double.valueOf(radiusField.getText());
	}

	@Override
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}

	@Override
	public String getCardName() {
		return "Location";
	}

	@Override
	public BdvOverlay getBdvOverlay() {
		if (moleculeLocationOverlay == null)
			moleculeLocationOverlay = new MoleculeLocationOverlay();
		
		return moleculeLocationOverlay;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public class MoleculeLocationOverlay extends BdvOverlay {
		
		public MoleculeLocationOverlay() {
		}
		
		@Override
		protected void draw(Graphics2D g) {
			if (!showCircle.isSelected())
				return;
			
			if (showAll.isSelected())
				archive.molecules().forEach(molecule -> drawMolecule(g, molecule));
			else
				drawMolecule(g, molecule);
		}
		
		private void drawMolecule(Graphics2D g, Molecule molecule) {
			if (molecule != null) {
				Color color = (rainbowColor()) ? getMoleculeColor(molecule.getUID()) : getColor();
				g.setColor(color);
				g.setStroke( new BasicStroke( 2 ) );
				
				if (showTrack())
					drawTrack(g, molecule);
				
				double centerX = Double.NaN;
				double centerY = Double.NaN;
				
				if (useParameters() && molecule.hasParameter(getXLocationSource()) && molecule.hasParameter(getYLocationSource())) {
					centerX = molecule.getParameter(getXLocationSource());
					centerY = molecule.getParameter(getYLocationSource());
				} else if (molecule.getTable().hasColumn(getXLocationSource()) && molecule.getTable().hasColumn(getYLocationSource())) {
					centerX = molecule.getTable().mean(getXLocationSource());
					centerY = molecule.getTable().mean(getYLocationSource());
				}
				
				if (Double.isNaN(centerX) || Double.isNaN(centerY))
					return;
				
				if (showLabel())
					drawLabel(g, molecule.getUID().substring(0, 6), centerX, centerY);
				
				if (followTrack() && molecule.getTable().hasColumn(getXLocationSource()) && molecule.getTable().hasColumn(getYLocationSource()) && molecule.getTable().hasColumn(getTLocationSource())) {
					Optional<MarsTableRow> currentRow = molecule.getTable().rows().filter(row -> row.getValue(getTLocationSource()) == info.getTimePointIndex()).findFirst();
					if (currentRow.isPresent()) {
						double x = currentRow.get().getValue(getXLocationSource());
						double y = currentRow.get().getValue(getYLocationSource());
						if (!Double.isNaN(x) && !Double.isNaN(y))
							drawOval(g, x, y);
					}
				} else if (showCircle())
					drawOval(g, centerX, centerY);
			}
		}
		
		private void drawOval(Graphics2D g, double x, double y) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);

			final double vx = transform.get( 0, 0 );
			final double vy = transform.get( 1, 0 );
			final double transformScale = Math.sqrt( vx * vx + vy * vy );

			final double[] globalCoords = new double[] { x, y };
			final double[] viewerCoords = new double[ 2 ];
			transform.apply( globalCoords, viewerCoords );

			final double rad = getRadius() * transformScale;

			final double arad = Math.sqrt( rad * rad );
			g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad ), ( int ) ( viewerCoords[ 1 ] - arad ), ( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );
		}
		
		private void drawTrack(Graphics2D g, Molecule molecule) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);
			
			if (molecule.getTable().getRowCount() < 2)
				return;
			
			boolean sourceInitialized = false;
			int xSource = 0; 
			int ySource = 0;
			for (int row = 0; row < molecule.getTable().getRowCount(); row++) {
				double x = molecule.getTable().getValue(getXLocationSource(), row);
				double y = molecule.getTable().getValue(getYLocationSource(), row);
				
				if (Double.isNaN(x) || Double.isNaN(y))
					continue;
				
				final double[] globalCoords = new double[] { x, y };
				final double[] viewerCoords = new double[ 2 ];
				transform.apply( globalCoords, viewerCoords );
				
				int xTarget =  ( int ) Math.round( viewerCoords[ 0 ] );
				int yTarget =  ( int ) Math.round( viewerCoords[ 1 ] );
				
				if (sourceInitialized)
					g.drawLine( xSource, ySource, xTarget, yTarget );
				
				xSource = xTarget;
				ySource = yTarget;
				sourceInitialized = true;
			}
		}
		
		private void drawLabel(Graphics2D g, String uid, double x, double y) {
			AffineTransform2D transform = new AffineTransform2D();
			getCurrentTransform2D(transform);

			final double vx = transform.get( 0, 0 );
			final double vy = transform.get( 1, 0 );
			final double transformScale = Math.sqrt( vx * vx + vy * vy );

			final double[] globalCoords = new double[] { x, y };
			final double[] viewerCoords = new double[ 2 ];
			transform.apply( globalCoords, viewerCoords );

			final double rad = getRadius() * transformScale;

			final double arad = Math.sqrt( rad * rad );
			final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
			final int ty = ( int ) viewerCoords[ 1 ];
			g.drawString( uid, tx, ty );
		}
		
		public synchronized Color getMoleculeColor(String UID) {
			if (moleculeRainbowColors == null) {
				moleculeRainbowColors = new HashMap<String, Color>();
				archive.molecules().forEach(m -> moleculeRainbowColors.put(m.getUID(), new Color(ran.nextFloat(), ran.nextFloat(), ran.nextFloat())));
			} else if (!moleculeRainbowColors.containsKey(UID)) {
				moleculeRainbowColors.put(UID, new Color(ran.nextFloat(), ran.nextFloat(), ran.nextFloat()));
			}
			
			return moleculeRainbowColors.get(UID);
		}
		
		private Color getColor()
		{
			int alpha = (int) info.getDisplayRangeMax();
			
			if (alpha > 255 || alpha < 0)
				alpha = 255;

			final int r = ARGBType.red( info.getColor().get() );
			final int g = ARGBType.green( info.getColor().get() );
			final int b = ARGBType.blue( info.getColor().get() );
			return new Color( r , g, b, alpha );
		}	
	}
}
