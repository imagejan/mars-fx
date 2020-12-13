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
package de.mpg.biochem.mars.fx.bdv;

import static java.util.stream.Collectors.toList;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.BigDataViewerActions;
import bdv.SpimSource;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvHandlePanel;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bdv.tools.HelpDialog;
import bdv.util.volatiles.SharedQueue;
import mpicbg.spim.data.SpimDataException;
import de.mpg.biochem.mars.metadata.MarsBdvSource;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.*;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import net.imglib2.util.Util;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import mpicbg.spim.data.registration.*;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ConverterSetups;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Real1dBinMapper;

import bdv.util.Bounds;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.controlsfx.control.ToggleSwitch;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;

import static bdv.ui.BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD;
import static bdv.ui.BdvDefaultCards.DEFAULT_VIEWERMODES_CARD;
import static bdv.ui.BdvDefaultCards.DEFAULT_SOURCES_CARD;

public class MarsBdvFrame< T extends NumericType< T > & NativeType< T > > {
	
	private final JFrame frame;
	
	private int numTimePoints = 1;

	private final HelpDialog helpDialog;
	
	private final SharedQueue sharedQueue;
	
	private HashMap<String, List<Source<T>>> bdvSources;
	private HashMap<String, N5Reader> n5Readers;
	
	private final boolean useVolatile;
	
	private String metaUID = "";
	
	private BdvHandlePanel bdv;
	
	private MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;
	
	protected Molecule molecule;
	protected MoleculeLocationOverlay moleculeLocationOverlay;
	protected MoleculeTrackOverlay moleculeTrackOverlay;
	protected final LocationCard locationCard;
	
	protected AffineTransform3D viewerTransform;
	
	public MarsBdvFrame(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive, Molecule molecule, boolean useVolatile) {
		this.archive = archive;
		this.molecule = molecule;
		this.useVolatile = useVolatile;
		
		bdvSources = new HashMap<String, List<Source<T>>>();
		n5Readers = new HashMap<String, N5Reader>();
		sharedQueue = new SharedQueue( Math.max( 1, Runtime.getRuntime().availableProcessors() / 2 ) );
		
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		frame = new JFrame( archive.getName() + " Bdv" );
		helpDialog = new HelpDialog(frame);
		
		bdv = new BdvHandlePanel( frame, Bdv.options().is2D() );
		bdv.getBdvHandle().getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);
		bdv.getBdvHandle().getCardPanel().removeCard(DEFAULT_VIEWERMODES_CARD);

		bdv.getBdvHandle().getCardPanel().addCard("Display", "Display", new NavigationPanel( bdv.getViewerPanel().state(), this), true);
		
		locationCard = new LocationCard(archive);
		bdv.getBdvHandle().getCardPanel().addCard("Location", "Location", locationCard, true);
		bdv.getBdvHandle().getCardPanel().addCard("Tracks", "Tracks", new JPanel(), true);
		
		frame.add( bdv.getSplitPanel(), BorderLayout.CENTER );
		
		frame.setPreferredSize( new Dimension( 800, 600 ) );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		
		setMolecule(molecule);
		
		frame.setVisible( true );
	}
	
	public void setMolecule(Molecule molecule) {
		if (molecule != null && locationCard.roverSync()) {	
			this.molecule = molecule;
			MarsMetadata meta = archive.getMetadata(molecule.getMetadataUID());
			if (!metaUID.equals(meta.getUID())) {
				metaUID = meta.getUID();
				createView(meta);
			}
			double x = getXLocation();
			double y = getYLocation();
			
			if (!Double.isNaN(x) && !Double.isNaN(y))
				goTo(x, y);
			
			if (locationCard.showLocationOverlay() && moleculeLocationOverlay == null) {
				moleculeLocationOverlay = new MoleculeLocationOverlay(archive, locationCard.useParameters(), locationCard.showLabel(), locationCard.getXLocation(), locationCard.getYLocation());
				BdvFunctions.showOverlay(moleculeLocationOverlay, "Location", Bdv.options().addTo(bdv));
			}

			if (locationCard.showLocationOverlay()) {
				moleculeLocationOverlay.useParameters(locationCard.useParameters());
				moleculeLocationOverlay.setXLocation(locationCard.getXLocation());
				moleculeLocationOverlay.setYLocation(locationCard.getYLocation());
				moleculeLocationOverlay.setLabelVisible(locationCard.showLabel());
				moleculeLocationOverlay.setShowAll(locationCard.showAll());
				moleculeLocationOverlay.setRadius(locationCard.getRadius());
				moleculeLocationOverlay.setMolecule(molecule);
			}
		 }
	}
	
	private double getXLocation() {
		if (molecule != null) {
			if (locationCard.useParameters() && molecule.hasParameter(locationCard.getXLocation()))
				return molecule.getParameter(locationCard.getXLocation());
			else if (molecule.getTable().hasColumn(locationCard.getXLocation()))
				return molecule.getTable().mean(locationCard.getXLocation());
		}
			
		return Double.NaN;
	}
	
	private double getYLocation() {
		if (molecule != null) {
			if (locationCard.useParameters() && molecule.hasParameter(locationCard.getYLocation()))
				return molecule.getParameter(locationCard.getYLocation());
			else if (molecule.getTable().hasColumn(locationCard.getYLocation()))
				return molecule.getTable().mean(locationCard.getYLocation());
		}
			
		return Double.NaN;
	}
	
	private void createView(MarsMetadata meta) {
		if (!bdvSources.containsKey(meta.getUID())) {
			try {
				bdvSources.put(meta.getUID(), loadSources(meta));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
			
		bdv.getViewerPanel().state().removeSources(bdv.getViewerPanel().state().getSources());
		
		for (Source<T> source : bdvSources.get(meta.getUID()))
			BdvFunctions.show( source, numTimePoints, Bdv.options().addTo( bdv ) );
		
		initBrightness( 0.001, 0.999, bdv.getViewerPanel().state(), bdv.getConverterSetups() );
	}
	
	public void resetView() {
		ViewerPanel viewer = bdv.getViewerPanel();
		Dimension dim = viewer.getDisplay().getSize();
		viewerTransform = initTransform( (int)dim.getWidth(), (int)dim.getHeight(), false, viewer.state() );
		viewer.setCurrentViewerTransform(viewerTransform);
	}
	
	//How can we deal with Volatile views? Somehow use isVolatile here to wait if pixels are not loaded??
	public ImagePlus exportView(int x0, int y0, int width, int height) {
		int numSources = bdvSources.get(metaUID).size();
		
		double xCenter = getXLocation();
		double yCenter = getYLocation();
		
		if (Double.isNaN(xCenter) || Double.isNaN(yCenter))
			return new ImagePlus();
		
		int TOP_left_x0 = (int)xCenter + x0;
		int TOP_left_y0 = (int)yCenter + y0;
		
		ImagePlus[] images = new ImagePlus[numSources];
		
		for ( int i = 0; i < numSources; i++ ) {
			ArrayList< RandomAccessibleInterval< T > > raiList = new ArrayList< RandomAccessibleInterval< T > >(); 
			Source<T> bdvSource = bdvSources.get(metaUID).get(i);
			
			for ( int t = 0; t < numTimePoints; t++ ) {

				//t, level, interpolation
				final RealRandomAccessible< T > raiRaw = ( RealRandomAccessible< T > )bdvSource.getInterpolatedSource( t, 0, Interpolation.NLINEAR );
				
				//retrieve transform
				AffineTransform3D affine = new AffineTransform3D();
				bdvSource.getSourceTransform(t, 0, affine);
				final AffineRandomAccessible< T, AffineGet > rai = RealViews.affine( raiRaw, affine );
				RandomAccessibleInterval< T > view = Views.interval( Views.raster( rai ), new long[] { TOP_left_x0, TOP_left_y0, 0 }, new long[]{ TOP_left_x0 + width, TOP_left_y0 + height, 0 } );
				
				raiList.add( view );
			}
			RandomAccessibleInterval< T > raiStack = Views.stack( raiList );
			images[i] = ImageJFunctions.wrap( raiStack, "channel " + i );
		}
		if (numSources == 1)
			return images[0];
		
		//image arrays, boolean keep original.
		ImagePlus ip = ij.plugin.RGBStackMerge.mergeChannels(images, false);
		ip.setTitle("molecule " + molecule.getUID());
		return ip;
	}
	
	private List<Source<T>> loadSources(MarsMetadata meta) throws IOException {
		List<Source<T>> sources = new ArrayList<Source<T>>();
		for (MarsBdvSource source : meta.getBdvSources()) {
			if (source.isN5()) {
				N5Reader reader;
				if (n5Readers.containsKey(source.getPath())) { 
					reader = n5Readers.get(source.getPath());
				} else {
					reader = new N5Importer.N5ViewerReaderFun().apply(source.getPath());
					n5Readers.put(source.getPath(), reader);
				}
				
				@SuppressWarnings( "rawtypes" )
				final RandomAccessibleInterval wholeImage = (useVolatile) ? 
						N5Utils.openVolatile( reader, source.getN5Dataset() ) :
						N5Utils.open( reader, source.getN5Dataset() );
						
				//wholeImage should be XYT or XYCT. If XYCT, we hyperSlice to get one channel.
				//XYZCT should also be supported
				int dims = wholeImage.numDimensions();
				
				@SuppressWarnings( "rawtypes" )
				final RandomAccessibleInterval image = (dims > 3) ? Views.hyperSlice(wholeImage, wholeImage.numDimensions() - 2, source.getChannel()) : wholeImage;

				int tSize = (int) image.dimension(image.numDimensions() - 1);
				
				if (tSize > numTimePoints)
					numTimePoints = tSize;

				AffineTransform3D[] transforms = new AffineTransform3D[tSize];
				
				for (int t = 0; t < tSize; t++) {
					if (source.getCorrectDrift()) {
						double dX = meta.getPlane(0, 0, 0, t).getXDrift();
						double dY = meta.getPlane(0, 0, 0, t).getYDrift();
						transforms[t] = source.getAffineTransform3D(dX, dY);
					} else
						transforms[t] = source.getAffineTransform3D();
				}
				
				@SuppressWarnings( "rawtypes" )
				final RandomAccessibleInterval[] images = new RandomAccessibleInterval[1];
				images[0] = image;

				@SuppressWarnings( "unchecked" )
				final MarsN5Source<T> n5Source = new MarsN5Source<>((T)Util.getTypeFromInterval(image), source.getName(), images, transforms);
				
				if (useVolatile)
					sources.add((Source<T>) n5Source.asVolatile(sharedQueue));
				else
					sources.add(n5Source);
				
			} else {
				SpimDataMinimal spimData;
				try {
					spimData = new XmlIoSpimDataMinimal().load( source.getPath() );
					
					//Add transforms to spimData...
					Map< ViewId, ViewRegistration > registrations = spimData.getViewRegistrations().getViewRegistrations();
						
					for (ViewId id : registrations.keySet()) {
						if (source.getCorrectDrift()) {
							double dX = meta.getPlane(0, 0, 0, id.getTimePointId()).getXDrift();
							double dY = meta.getPlane(0, 0, 0, id.getTimePointId()).getYDrift();
							registrations.get(id).getModel().set(source.getAffineTransform3D(dX, dY));
						} else
							registrations.get(id).getModel().set(source.getAffineTransform3D());
					}
					
					if (spimData.getSequenceDescription().getTimePoints().size() > numTimePoints)
						numTimePoints = spimData.getSequenceDescription().getTimePoints().size();
					
					sources.add(new SpimSource<T>(spimData, 0, "source"));
				} catch (SpimDataException e) {
					e.printStackTrace();
				}
			}
		}
		
		return sources;
	}
	
	public void showHelp(boolean showHelp) {
		helpDialog.setVisible(showHelp);
	}

	public JFrame getFrame() {
		return frame;
	}
	
	public BdvHandle getBdvHandle() {
		return bdv;
	}

	public void setArchive(MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive) {
		this.archive = archive;
	}

	public void goTo(double x, double y) {
		resetView();
		
		Dimension dim = bdv.getViewerPanel().getDisplay().getSize();
		viewerTransform = bdv.getViewerPanel().getDisplay().getTransformEventHandler().getTransform();
		AffineTransform3D affine = viewerTransform;
		
		double[] source = new double[3];
		source[0] = 0;
		source[1] = 0;
		source[2] = 0;
		double[] target = new double[3];
		target[0] = 0;
		target[1] = 0;
		target[2] = 0;
		
		viewerTransform.apply(source, target);
		
		affine.set( affine.get( 0, 3 ) - target[0], 0, 3 );
		affine.set( affine.get( 1, 3 ) - target[1], 1, 3 );

		double scale = locationCard.getMagnification();
		
		//check it was set correctly?
		
		// scale
		affine.scale( scale );
		
		source[0] = x;
		source[1] = y;
		source[2] = 0;
		
		affine.apply(source, target);

		affine.set( affine.get( 0, 3 ) - target[0] + dim.getWidth()/2, 0, 3 );
		affine.set( affine.get( 1, 3 ) - target[1] + dim.getHeight()/2, 1, 3 );
		
		bdv.getViewerPanel().setCurrentViewerTransform( affine );
	}

	/**
	 * Get a "good" initial viewer transform. The viewer transform is chosen
	 * such that for the first source,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * at z = 0
	 * <li>centered and scaled such that the full <em>dim_x</em> by
	 * <em>dim_y</em> is visible.
	 * </ul>
	 *
	 * @param viewerWidth
	 *            width of the viewer display
	 * @param viewerHeight
	 *            height of the viewer display
	 * @param state
	 *            the {@link ViewerState} containing at least one source.
	 * @return proposed initial viewer transform.
	 */
	public static AffineTransform3D initTransform( final int viewerWidth, final int viewerHeight, final boolean zoomedIn, final ViewerState state ) {
		final AffineTransform3D viewerTransform = new AffineTransform3D();
		final double cX = viewerWidth / 2.0;
		final double cY = viewerHeight / 2.0;

		final SourceAndConverter< ? > current = state.getCurrentSource();
		if ( current == null )
			return viewerTransform;
		final Source< ? > source = current.getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		if ( !source.isPresent( timepoint ) )
			return viewerTransform;

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, 0, sourceTransform );

		final Interval sourceInterval = source.getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		//final double sZ0 = sourceInterval.min( 2 );
		//final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 + 1 ) / 2;
		final double sY = ( sY0 + sY1 + 1 ) / 2;
		final double sZ = 0;//( sZ0 + sZ1 + 1 ) / 2;

		final double[][] m = new double[ 3 ][ 4 ];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale;
		if ( zoomedIn )
			scale = Math.max( scaleX, scaleY );
		else
			scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY, 1, 3 );
		return viewerTransform;
	}
	
	public static void initBrightness( final double cumulativeMinCutoff, final double cumulativeMaxCutoff, final ViewerState state, final ConverterSetups converterSetups )
	{
		final SourceAndConverter< ? > current = state.getCurrentSource();
		if ( current == null )
			return;
		final Source< ? > source = current.getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		final Bounds bounds = estimateSourceRange( source, timepoint, cumulativeMinCutoff, cumulativeMaxCutoff );
		for ( SourceAndConverter< ? > s : state.getSources() )
		{
			final ConverterSetup setup = converterSetups.getConverterSetup( s );
			setup.setDisplayRange( bounds.getMinBound(), bounds.getMaxBound() );
		}
	}
	
	/**
	 * @param cumulativeMinCutoff
	 * 		fraction of pixels that are allowed to be saturated at the lower end of the range.
	 * @param cumulativeMaxCutoff
	 * 		fraction of pixels that are allowed to be saturated at the upper end of the range.
	 */
	public static Bounds estimateSourceRange( final Source< ? > source, final int timepoint, final double cumulativeMinCutoff, final double cumulativeMaxCutoff )
	{
		final Object type = source.getType();
		if ( type instanceof UnsignedShortType && source.isPresent( timepoint ) )
		{
			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval< UnsignedShortType > ) source.getSource( timepoint, source.getNumMipmapLevels() - 1 );
			final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

			final int numBins = 6535;
			final Histogram1d< ? > histogram = new Histogram1d<>( Views.hyperSlice( img, 2, z ), new Real1dBinMapper<>( 0, 65535, numBins, false ) );
			final DiscreteFrequencyDistribution dfd = histogram.dfd();
			final long[] bin = new long[] { 0 };
			double cumulative = 0;
			int i = 0;
			for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
			{
				bin[ 0 ] = i;
				cumulative += dfd.relativeFrequency( bin );
			}
			final int min = i * 65535 / numBins;
			for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
			{
				bin[ 0 ] = i;
				cumulative += dfd.relativeFrequency( bin );
			}
			final int max = i * 65535 / numBins;
			return new Bounds( min, max );
		}
		else if ( type instanceof UnsignedByteType )
			return new Bounds( 0, 255 );
		else
			return new Bounds( 0, 65535 );
	}
}
