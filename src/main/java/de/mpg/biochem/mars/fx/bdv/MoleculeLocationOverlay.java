package de.mpg.biochem.mars.fx.bdv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Optional;

import bdv.util.BdvOverlay;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.table.MarsTableRow;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

public class MoleculeLocationOverlay extends BdvOverlay {

	private String xLocation;
	private String yLocation;
	private boolean useParameters;
	private boolean showLabel;
	
	private double radius = 5;
	private Molecule molecule;
	
	public MoleculeLocationOverlay(final boolean useProperties, final boolean showLabel, final String xLocation, final String yLocation) {
		this.xLocation = xLocation;
		this.yLocation = yLocation;
		this.useParameters = useProperties;
		this.showLabel = showLabel;
	}
	
	@Override
	protected void draw(Graphics2D g) {
		if (molecule != null) {
			if (useParameters && molecule.hasParameter(xLocation) && molecule.hasParameter(yLocation)) {
				double x = molecule.getParameter(xLocation);
				double y = molecule.getParameter(yLocation);
				if (!Double.isNaN(x) && !Double.isNaN(y))
					drawOval(g, x, y);
			} else if (molecule.getTable().hasColumn(xLocation) && molecule.getTable().hasColumn(yLocation)) {
				double x = molecule.getTable().mean(xLocation);
				double y = molecule.getTable().mean(yLocation);
				if (!Double.isNaN(x) && !Double.isNaN(y))
					drawOval(g, x, y);
			}
		}
	}
	
	private void drawOval(Graphics2D g, double x, double y) {
		g.setColor( getColor() );
		g.setStroke( new BasicStroke( 2 ) );
		
		AffineTransform2D transform = new AffineTransform2D();
		getCurrentTransform2D(transform);

		final double vx = transform.get( 0, 0 );
		final double vy = transform.get( 1, 0 );
		final double transformScale = Math.sqrt( vx * vx + vy * vy );

		final double[] globalCoords = new double[] { x, y };
		final double[] viewerCoords = new double[ 2 ];
		transform.apply( globalCoords, viewerCoords );

		final double rad = radius * transformScale;

		final double arad = Math.sqrt( rad * rad );
		g.drawOval( ( int ) ( viewerCoords[ 0 ] - arad ), ( int ) ( viewerCoords[ 1 ] - arad ), ( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );

		if (showLabel) {
			final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
			final int ty = ( int ) viewerCoords[ 1 ];
			g.drawString( molecule.getUID().substring(0, 6), tx, ty );
		}
	}
	
	private Color getColor()
	{
		int alpha = (int) info.getDisplayRangeMax();

		final int r = ARGBType.red( info.getColor().get() );
		final int g = ARGBType.green( info.getColor().get() );
		final int b = ARGBType.blue( info.getColor().get() );
		return new Color( r , g, b, alpha );
	}

	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}
	
	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	public void useParameters(boolean useParameters) {
		this.useParameters = useParameters;
	}
	
	public void setXLocation(String xLocation) {
		this.xLocation = xLocation;
	}
	
	public void setYLocation(String yLocation) {
		this.yLocation = yLocation;
	}
	
	public void setLabelVisible(boolean showLabel) {
		this.showLabel = showLabel;
	}
}
