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

public class MoleculeTrackOverlay extends BdvOverlay {

	private String tColumn;
	private String xColumn;
	private String yColumn;
	private boolean showLabel;
	
	private double radius = 5;
	private Molecule molecule;
	
	public MoleculeTrackOverlay(final boolean showLabel, final String tColumn, final String xColumn, final String yColumn) {
		this.showLabel = showLabel;
		this.tColumn = tColumn;
		this.xColumn = xColumn;
		this.yColumn = yColumn;
	}
	
	@Override
	protected void draw(Graphics2D g) {
		if (molecule != null) {
			if (molecule.getTable().hasColumn(xColumn) && molecule.getTable().hasColumn(yColumn) && molecule.getTable().hasColumn(tColumn)) {
				Optional<MarsTableRow> currentRow = molecule.getTable().rows().filter(row -> row.getValue(tColumn) == info.getTimePointIndex()).findFirst();
				if (currentRow.isPresent()) {
					double x = currentRow.get().getValue(xColumn);
					double y = currentRow.get().getValue(yColumn);
					if (!Double.isNaN(x) && !Double.isNaN(y))
						drawOval(g, x, y);
				}
			}
		}
	}
	
	private void drawOval(Graphics2D g, double x, double y) {
		//Color.CYAN.darker()

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
	
	public void setTLocation(String tLocation) {
		this.tColumn = tLocation;
	}
	
	public void setXColumn(String xColumn) {
		this.xColumn = xColumn;
	}
	
	public void setYColumn(String yColumn) {
		this.yColumn = yColumn;
	}
	
	public void setLabelVisible(boolean showLabel) {
		this.showLabel = showLabel;
	}
}
