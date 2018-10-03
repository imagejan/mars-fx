package de.mpg.biochem.sdmm.plot;

import java.lang.reflect.Field;
import java.awt.GraphicsEnvironment;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.Panel;
import java.awt.Label;
import java.awt.Insets;
import java.awt.Choice;
import java.awt.TextField;
import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
//import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
//import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.mpg.biochem.sdmm.molecule.Molecule;
import de.mpg.biochem.sdmm.molecule.MoleculeChangedListener;
import ij.gui.GenericDialog;
import ij.IJ;
import ij.Prefs;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
//import ij.text.TextWindow;
import ij.ImagePlus;

public class Plot extends JComponent implements ActionListener {

	protected enum Type {
		LINE,
		SCATTER,
		BAR,
		SEGMENTS,
		FIT
	};
	
	protected enum Style {
		SOLID,
		STRIPES
	};
	
	private static final long serialVersionUID = 1L;
	
	private AffineTransform transform;
	
	//will allow for entry of starting and stopping times in the points table.
	private static int pointsStart = 0;
	private Molecule molecule;
	
	//Added KED for getting points with p button:
	Point2D.Double selectedCoordinate;
	
	public ArrayList<Type> plotTypes = new ArrayList<Type>();
	public ArrayList<String> plotNames = new ArrayList<String>();
	public ArrayList<Color> plotColors = new ArrayList<Color>();
	public ArrayList<Float> plotLineWidths = new ArrayList<Float>();
	public ArrayList<Style> plotStyles = new ArrayList<Style>();
	protected Font font = new Font(Prefs.get("plot.font_type","Arial"), Font.PLAIN, (int)Prefs.get("plot.font_size", 14));
	protected Font label_font = new Font(Prefs.get("plot.font_type","Arial"), Font.PLAIN, (int)Prefs.get("plot.label_font_size", 14));
	protected Font title_font = new Font(Prefs.get("plot.font_type","Arial"), Font.PLAIN, (int)Prefs.get("plot.title_font_size", 14));
	protected Font legend_font = new Font(Prefs.get("plot.font_type","Arial"), Font.PLAIN, (int)Prefs.get("plot.legend_font_size", 14));
	protected String plotTitle = "";
	protected String xAxisLabel = "";
	protected String yAxisLabel = "";
	protected int xaxis_precision = (int)Prefs.get("plot.xaxis_precision", 1);
	protected int yaxis_precision = (int)Prefs.get("plot.yaxis_precision", 1);
	protected ArrayList<String> legend_text;
	protected String caption = "";
	
	public int group_number = 0;
	
	public int leftMargin = 100;
	public int rightMargin = 50;
	public int bottomMargin = 50;
	public int topMargin = 0;
	protected double gap = Prefs.get("plot.bar_gap_size", 0.2);
	protected String[] colors = {"black", "blue", "cyan", "gray", "green", "dark green", "magenta", "orange", "pink", "red", "white", "yellow"};
	
	protected ArrayList<double[]> plotCoordinates = new ArrayList<double[]>();
	protected ArrayList<double[]> pixelCoordinates = new ArrayList<double[]>();
	
	protected Rectangle2D.Double originalBounds;
	protected Rectangle2D.Double bounds;
	protected Rectangle legendBounds = new Rectangle(0, 0, 0, 0);
	protected Point mousePosition = new Point();
	
	private boolean gridlines = false;
	private boolean sigmaStartStop = Prefs.get("plot.sigma_start_stop", false);
	private boolean show_legend = Prefs.get("plot.show_legend", true);
	private boolean legend_box = Prefs.get("plot.legend_box", true);
	private boolean show_tracker = Prefs.get("plot.show_tracker", true);
	
	private JPopupMenu menu = new JPopupMenu();
	private JRadioButtonMenuItem zoomInMenuItem = new JRadioButtonMenuItem("Zoom In", true);
	private JRadioButtonMenuItem zoomOutMenuItem = new JRadioButtonMenuItem("Zoom Out");
	
	private JMenuItem saveMenuItem = new JMenuItem("Save");
	private JMenuItem copyMenuItem = new JMenuItem("Copy");
	private JMenuItem selectRegionMenuItem = new JMenuItem("Select Region");
	//private JMenuItem addDataSet = new JMenuItem("add Plot");
	private JMenuItem fitData = new JMenuItem("fit");
	private JMenuItem propertiesMenuItem = new JMenuItem("Properties");
	
	private String group_name;
	
	private double zoomFactor = 1.5;
	private File currentDirectory;
	private boolean isRegionSelection = false;
	private boolean isLegendSelected = false;
	private boolean updatePlotBoundaries = true;
	private Rectangle regionSelection = new Rectangle();
	
	//More Colors
	Color darkGreen = new Color(25, 123, 48);
	
	public Plot() {
		saveMenuItem.addActionListener(this);
		copyMenuItem.addActionListener(this);
		selectRegionMenuItem.addActionListener(this);
		//addDataSet.addActionListener(this);
		fitData.addActionListener(this);
		propertiesMenuItem.addActionListener(this);
		
		menu.add(zoomInMenuItem);
		menu.add(zoomOutMenuItem);
		menu.add(selectRegionMenuItem);
		menu.add(saveMenuItem);
		menu.add(copyMenuItem);
		//menu.add(adjustAxisMenuItem);
		//menu.add(addDataSet);
		menu.add(fitData);
		menu.add(propertiesMenuItem);
		
		ButtonGroup group = new ButtonGroup();
	    group.add(zoomInMenuItem);
	    group.add(zoomOutMenuItem);
	    
		addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				
				if (!isRegionSelection) {
					if (e.getWheelRotation() < 0)
						zoom(1 / zoomFactor);
					else
						zoom(zoomFactor);
				}
			}
			
		});
		
		addKeyListener(new KeyAdapter() {

	           @Override
	           public void keyPressed(KeyEvent e) {
	               // add selected peak to results table
	        	   if (e.getKeyChar() == 'p') {
	        		   if (sigmaStartStop) {
		        		   switch (pointsStart) {
			        		   case 0:
			        			   molecule.setParameter("bg_start", selectedCoordinate.getX());
			        			   pointsStart = 1;
			        			   break;
			        		   case 1:
			        			   molecule.setParameter("bg_end", selectedCoordinate.getX());
			        			   pointsStart = 2;
			        			   break;
			        		   case 2:
			        			   molecule.setParameter("start", selectedCoordinate.getX());
			        			   pointsStart = 3;
			        			   break;
			        		   case 3:
			        			   molecule.setParameter("end", selectedCoordinate.getX());
			        			   pointsStart = 0;
			        			   break;
		        		   }
	        		   } else {
	        			   switch (pointsStart) {
		        		   case 0:
		        			   molecule.setParameter("start", selectedCoordinate.getX());
		        			   pointsStart = 1;
		        			   break;
		        		   case 1:
		        			   molecule.setParameter("end", selectedCoordinate.getX());
		        			   pointsStart = 0;
		        			   break;
		        		   case 2:
		        			   pointsStart = 0;
		        			   break;
		        		   case 3:
		        			   pointsStart = 0;
		        			   break;
	        			   }
	        		   }
	        		   notifyMoleculeChangedListeners();
	        	   }
	           }
	       });
		
		setFocusable(true);    // necessary for key listener
		
		addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				//Will put this here to make sure the p button will work for selecting points...
				requestFocus();
				
				mousePosition = e.getPoint();
				regionSelection.setLocation(e.getPoint()); 
				repaint();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				
				if (isRegionSelection) {
					regionSelection.width = e.getX() - regionSelection.x;
					regionSelection.height = e.getY() - regionSelection.y;
				} else if (isLegendSelected) {
					double dx = mousePosition.getX() - e.getPoint().getX();
					double dy = mousePosition.getY() - e.getPoint().getY();
					
					mousePosition = e.getPoint();
					
					legendBounds.x -= dx;
					legendBounds.y -= dy;
				} else {
					double dx = mousePosition.getX() - e.getPoint().getX();
					double dy = mousePosition.getY() - e.getPoint().getY();
					
					mousePosition = e.getPoint();
					
					bounds.x += dx / transform.getScaleX();
					bounds.y += dy / transform.getScaleY();
				}
				
				notifyBoundsChangedListeners();
				
				repaint();
			}
		});
		
		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (legendBounds.contains(e.getX(),e.getY()) && show_legend) {
					if (e.getClickCount() == 2) 
						setProperties();
					else 
						isLegendSelected = true;
				} else if (isLegendSelected) {
					isLegendSelected = false;
				} else if (e.getClickCount() == 2) {
					resetBounds();
				} else if (e.getButton() == MouseEvent.BUTTON1 && !e.isShiftDown()) {
					
					if (zoomInMenuItem.isSelected())
						zoom(1 / 1.5);
					else
						zoom(1.5);
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e){
				if (e.isPopupTrigger() || (e.isShiftDown() && e.getButton() == MouseEvent.BUTTON1))
					menu.show(e.getComponent(), e.getX(), e.getY());
				
				if (isRegionSelection) {
					
					try {
						Point2D.Double upperLeft = new Point2D.Double(regionSelection.x, regionSelection.y);
						Point2D.Double lowerRight = new Point2D.Double(regionSelection.x + regionSelection.width, regionSelection.y + regionSelection.height);

						transform.inverseTransform(upperLeft, upperLeft);
						transform.inverseTransform(lowerRight, lowerRight);
						
						bounds = new Rectangle2D.Double(upperLeft.x, upperLeft.y, 0, 0);
						bounds.add(lowerRight);
						
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
						isRegionSelection = false;
						
						repaint();
					} catch (NoninvertibleTransformException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
				
			}
		});
		
	}
	public void setBounds(double x, double y, double width, double height) {
		bounds.x = x;
		bounds.y = y;
		bounds.width = width;
		bounds.height = height;
		
		notifyBoundsChangedListeners();
		
		repaint();
	}
	
	public void setOriginalBounds(double x, double y, double width, double height) {
		originalBounds.x = x;
		originalBounds.y = y;
		originalBounds.width = width;
		originalBounds.height = height;
		setBounds(x, y, width, height);
	}
	
	public Rectangle2D.Double getOriginalBounds() {
		return (Rectangle2D.Double)originalBounds.clone();
	}
	
	public void resetBounds() {
		if (originalBounds != null)
			bounds = (Rectangle2D.Double)originalBounds.clone();
		repaint();
	}
	
	public Rectangle2D.Double getPlotBounds() {
		return (Rectangle2D.Double)bounds.clone();
	}
	
	public void setPlotBounds(Rectangle2D.Double bounds) {
		this.bounds = (Rectangle2D.Double)bounds.clone();
		repaint();
	}
	
	public void addLinePlot(double[] x, double[] y, int from, int to, Color c, float lineWidth) {
		addLinePlot(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth);
	}
	
	public void addLinePlot(double[] x, double[] y, int from, int to, Color c, float lineWidth, String plotName) {
		addLinePlot(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth, plotName);
	}
	
	public void addFitPlot(double[] x, double[] y, Color c, float lineWidth, String plotName) {
		addPlot(x, y, Type.FIT, c, lineWidth, plotName);
	}
	
	public void addScatterPlot(double[] x, double[] y, int from, int to, Color c, float lineWidth) {
		addScatterPlot(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth);
	}
	
	public void addScatterPlot(double[] x, double[] y, int from, int to, Color c, float lineWidth, String plotName) {
		addScatterPlot(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth, plotName);
	}
	
	public void addBarGraph(double[] x, double[] y, int from, int to, Color c, float lineWidth) {
		addBarGraph(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth);
	}
	
	public void addBarGraph(double[] x, double[] y, int from, int to, Color c, float lineWidth, String plotName) {
		addBarGraph(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth, plotName);
	}
	
	public void addSegmentPlot(double[] x, double[] y, int from, int to, Color c, float lineWidth) {
		addSegmentPlot(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth);
	}
	
	public void addSegmentPlot(double[] x, double[] y, int from, int to, Color c, float lineWidth, String plotName) {
		addSegmentPlot(Arrays.copyOfRange(x, from, to), Arrays.copyOfRange(y, from, to), c, lineWidth, plotName);
	}
	
	public void addBarGraph(double[] x, double[] y,Color c, float lineWidth) {
		addPlot(x,y,Type.BAR,c,lineWidth);
	}
	
	public void addBarGraph(double[] x, double[] y,Color c, float lineWidth, String plotName) {
		addPlot(x,y,Type.BAR,c,lineWidth, plotName);
	}
	
	public void addLinePlot(double[] x, double[] y, Color c, float lineWidth) {
		addPlot(x, y, Type.LINE, c, lineWidth);
	}
	
	public void addLinePlot(double[] x, double[] y, Color c, float lineWidth, String plotName) {
		addPlot(x, y, Type.LINE, c, lineWidth, plotName);
	}
	
	public void addSegmentPlot(double[] x, double[] y, Color c, float lineWidth) {
		addPlot(x, y, Type.SEGMENTS, c, lineWidth);
	}
	
	public void addSegmentPlot(double[] x, double[] y, Color c, float lineWidth, String plotName) {
		addPlot(x, y, Type.SEGMENTS, c, lineWidth, plotName);
	}
	
	public void addScatterPlot(double[] x, double[] y, Color c, float lineWidth, String plotName) {
		addPlot(x, y, Type.SCATTER, c, lineWidth, plotName);
	}
	
	public void addScatterPlot(double[] x, double[] y, Color c, float lineWidth) {
		addPlot(x, y, Type.SCATTER, c, lineWidth);
	}
	
	private void addPlot(double[] x, double[] y, Type type, Color c, float lineWidth) {
		addPlot(x, y, type, c, lineWidth, "");
	}
	
	private void addPlot(double[] x, double[] y, Type type, Color c, float lineWidth, String plotName) {
		double[] plot = new double[x.length * 2];
		
		if (originalBounds == null)
			originalBounds = new Rectangle2D.Double(x[0], y[0], 0, 0);
		
		for (int i = 0; i < x.length; i++) {
			plot[i * 2] = x[i];
			plot[i * 2 + 1] = y[i];
			if (type == Type.BAR) {
				double binWidth = x[1]-x[0];
				if (!Double.isNaN(x[i]) && !Double.isNaN(y[i]))
					originalBounds.add(new Rectangle2D.Double(x[i] - binWidth/2, 0, binWidth, y[i]));
			} else {
				if (!Double.isNaN(x[i]) && !Double.isNaN(y[i]))
					originalBounds.add(x[i], y[i]);
			}
				
		}
		
		if (bounds == null)
			bounds = (Rectangle2D.Double)originalBounds.clone();
		else
			bounds.add(originalBounds);
		
		plotCoordinates.add(plot);
		pixelCoordinates.add(new double[plot.length]);	// make space for translated coordinates
		plotTypes.add(type);
		
		plotColors.add(c);
		plotLineWidths.add(lineWidth);
		plotStyles.add(Style.SOLID);
		
		plotNames.add(plotName);
	}
	
	public void deletePlot(int index) {
		//every delete operation will obviously shift all index numbers..
		plotCoordinates.remove(index);
		pixelCoordinates.remove(index);
		plotTypes.remove(index);
		plotColors.remove(index);
		plotLineWidths.remove(index);
		plotStyles.remove(index);
		plotNames.remove(index);
		
		//Now we need to reset the originalBounds of the plot.
		originalBounds = null;
		for (int j=0;j<plotCoordinates.size();j++) {
			if (originalBounds == null)
				originalBounds = new Rectangle2D.Double(plotCoordinates.get(j)[0], plotCoordinates.get(j)[1], 0, 0);
			for (int i = 0; i < plotCoordinates.get(j).length/2; i++) {
				if (plotTypes.get(j) == Type.BAR) {
					double binWidth = plotCoordinates.get(j)[2] - plotCoordinates.get(j)[0];
					if (!Double.isNaN(plotCoordinates.get(j)[i * 2]) && !Double.isNaN(plotCoordinates.get(j)[i * 2 + 1]))
						originalBounds.add(new Rectangle2D.Double(plotCoordinates.get(j)[i * 2] - binWidth/2, 0, binWidth, plotCoordinates.get(j)[i * 2 + 1]));
				} else {
					if (!Double.isNaN(plotCoordinates.get(j)[i * 2]) && !Double.isNaN(plotCoordinates.get(j)[i * 2 + 1]))
						originalBounds.add(plotCoordinates.get(j)[i * 2], plotCoordinates.get(j)[i * 2 + 1]);
				}
					
			}
		}
	}
	
	public void clear() {
		plotCoordinates.clear();
		pixelCoordinates.clear();
		plotColors.clear();
		plotLineWidths.clear();
		plotTypes.clear();
		plotNames.clear();
		bounds = null;
		originalBounds = null;
	}
	
	public void setxAxisLabel(String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
	}

	public void setyAxisLabel(String yAxisLabel) {
		this.yAxisLabel = yAxisLabel;
	}
	
	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	public void setGroup(String group_name, int group_number) {
		this.group_number = group_number;
		this.group_name = group_name;
	}
	
	public void setLegend(ArrayList<String> contents) {
		this.legend_text = contents;
		legendBounds.x = leftMargin + 20;
		legendBounds.y = topMargin + 20;
		legend_box = true;
	}

	public void setFont(Font font) {
		this.font = font;			
	}
	
	public void paintAxis(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.setStroke(new BasicStroke(2.0f));
		
		int width = getWidth() - leftMargin - rightMargin;
		int height = getHeight() - bottomMargin - topMargin; 
		
		double xStepSize = getStepSize(bounds.width, width / 50);	// draw a number every 50 pixels
		double yStepSize = getStepSize(bounds.height, height / 50);
		
		for (double x = bounds.x - (bounds.x % xStepSize); x < bounds.x + bounds.width; x += xStepSize) {
			Point2D.Double p = new Point2D.Double(x, 0);
			transform.transform(p, p);
			
			if (p.x > leftMargin) {
				if (gridlines) {
					g2d.setColor(Color.LIGHT_GRAY);
					g2d.drawLine((int)p.x, topMargin, (int)p.x, topMargin + height);
				}
				g2d.setColor(Color.BLACK);
				g2d.drawLine((int)p.x, topMargin + height, (int)p.x, topMargin + height + 5);
				g2d.drawString(String.format("%." + xaxis_precision + "f", x), (int)p.x - g.getFontMetrics(font).stringWidth(String.format("%." + xaxis_precision + "f", x))/2, topMargin + height + g.getFontMetrics(font).getHeight() + 2);
			}
		}
		
		g.setFont(label_font);
		
		AffineTransform at = g2d.getTransform();
		g2d.translate(g.getFontMetrics(label_font).getHeight(), topMargin + height / 2 + g.getFontMetrics().stringWidth(yAxisLabel) / 2);
		g2d.rotate(-Math.PI / 2);
		g2d.drawString(yAxisLabel, 0, 0);		
		g2d.setTransform(at);
		
		g2d.drawString(xAxisLabel, leftMargin + width / 2 - g.getFontMetrics().stringWidth(xAxisLabel) / 2, topMargin + height + g.getFontMetrics(label_font).getHeight() + g.getFontMetrics(font).getHeight() + 7);
		
		g.setFont(title_font);
		g2d.drawString(plotTitle, leftMargin + width / 2 - g.getFontMetrics().stringWidth(plotTitle) / 2, g.getFontMetrics(title_font).getHeight());
		
		g.setFont(font);
		
		for (double y = bounds.y - (bounds.y % yStepSize); y < bounds.y + bounds.height; y += yStepSize) {
			Point2D.Double p = new Point2D.Double(0, y);
			transform.transform(p, p);
			
			if (p.y < height + topMargin) {
				if (gridlines) {
					g2d.setColor(Color.LIGHT_GRAY);
					g2d.drawLine(leftMargin, (int)p.y, getWidth() - rightMargin, (int)p.y);
				}
				g2d.setColor(Color.BLACK);
				g2d.drawLine(leftMargin - 5, (int)p.y, leftMargin, (int)p.y);
				g2d.drawString(String.format("%." + yaxis_precision + "f", y), (int)leftMargin - g.getFontMetrics(font).stringWidth(String.format("%." + yaxis_precision + "f", y)) - 7, (int)p.y + (int)(g.getFontMetrics(font).getHeight()/3.1));
			}
		}
	}
	
	public void paintPlot(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		
		Point selectedPoint = new Point();
		selectedCoordinate = new Point2D.Double();
		
		int width = getWidth() - leftMargin - rightMargin;
		int height = getHeight() - bottomMargin - topMargin;
		
		// clip to the plot area (to prevent drawing outside of the plot area)
		Rectangle2D plot_area = new Rectangle2D.Double(leftMargin, topMargin, width, height);
		g2d.clip(plot_area);
		//g.clipRect(leftMargin, topMargin, width, height);
		
		//This is for the pattern...
		Path2D p2D = new Path2D.Double(Path2D.WIND_EVEN_ODD);
		
		// draw plots
		for (int i = 0; i < plotCoordinates.size(); i++) {
			double[] coordinates = pixelCoordinates.get(i);
			
			g2d.setColor(plotColors.get(i));
			g2d.setStroke(new BasicStroke(plotLineWidths.get(i)));
			
			for (int j = 0; j < coordinates.length; j += 2) {
				
				switch (plotTypes.get(i)) {
				case LINE:
					if (j + 2 < coordinates.length)
						g2d.drawLine((int)coordinates[j], (int)coordinates[j + 1], (int)coordinates[j + 2], (int)coordinates[j + 3]);
					break;
				case FIT:
					if (j + 2 < coordinates.length)
						g2d.drawLine((int)coordinates[j], (int)coordinates[j + 1], (int)coordinates[j + 2], (int)coordinates[j + 3]);
					break;
				case SCATTER:
						g2d.drawOval((int)coordinates[j] - 1, (int)coordinates[j + 1] - 1, 2, 2);
					break;
				case BAR:
					Rectangle2D r1 = new Rectangle2D.Double(
							coordinates[j] + (gap-0.5)*(coordinates[2] - coordinates[0]),
							coordinates[j + 1],
							(1 - 2*gap)*(coordinates[2] - coordinates[0]),
							(transform.getTranslateY() - coordinates[j + 1]));
					
					if (plotStyles.get(i).equals(Style.SOLID)) {
						g2d.fill(r1);
						g2d.setColor(Color.BLACK);
						if (plotLineWidths.get(i) != 0)
							g2d.draw(r1);
						g2d.setColor(plotColors.get(i));
					} else {
						g2d.setColor(Color.WHITE);
						g2d.fill(r1);
						g2d.setColor(plotColors.get(i));
						g2d.draw(r1);
					}
					p2D.append(r1, false);
					break;
				case SEGMENTS:
					//Should only connect every other set of consecutive points
					if (j + 2 < coordinates.length && (j % 4 == 0))
						g2d.drawLine((int)coordinates[j], (int)coordinates[j + 1], (int)coordinates[j + 2], (int)coordinates[j + 3]);
					break;
				}
				
				if (mousePosition.distance((int)coordinates[j], (int)coordinates[j + 1]) < mousePosition.distance(selectedPoint)) {
					selectedPoint = new Point((int)coordinates[j], (int)coordinates[j + 1]);
					selectedCoordinate = new Point2D.Double(plotCoordinates.get(i)[j], plotCoordinates.get(i)[j + 1]);
				}	
			}
			
		}
		g2d.setStroke(new BasicStroke(2.0f));
		
		// draw selected point
		if (mousePosition.distance(selectedPoint) < 100 && show_tracker) {
			g2d.setColor(Color.RED);
			g2d.drawOval(selectedPoint.x - 3, selectedPoint.y - 3, 6, 6);
			g2d.drawString(String.format("%." + xaxis_precision + "f, %." + yaxis_precision + "f", selectedCoordinate.x, selectedCoordinate.y), selectedPoint.x - 3, selectedPoint.y - 3);
		}
		
		// draw selection region
		if (isRegionSelection) {
			g2d.setColor(Color.RED);
			int Rx = regionSelection.x;
			int Ry = regionSelection.y;
			int Rwidth = regionSelection.width;
			int Rheight = regionSelection.height;
			if (regionSelection.width < 0) {
				Rx += Rwidth;
				Rwidth *= -1;
			}
			if (regionSelection.height < 0) {
				Ry += Rheight;
				Rheight *= -1;
			}
			g2d.drawRect(Rx, Ry, Rwidth, Rheight);

		}
		
		g2d.setColor(Color.BLACK);
		g2d.drawString(caption, leftMargin + 20, topMargin + 20);
		
		g2d.setClip(new Rectangle(0, 0, (int)plot_area.getWidth() + leftMargin + rightMargin, (int)plot_area.getHeight() + topMargin + bottomMargin));
		
		// box around plot
		g2d.drawRect(leftMargin, topMargin, width, height);
		
		if (legend_text != null & show_legend) {
			g.setFont(legend_font);
			legendBounds.width = 0;
			for (int i=0; i < legend_text.size(); i++) {
				if(g.getFontMetrics(legend_font).stringWidth(legend_text.get(i)) > legendBounds.width)
					legendBounds.width = g.getFontMetrics(legend_font).stringWidth(legend_text.get(i));
			}
			legendBounds.width += 20;
			legendBounds.height = (4 + g.getFontMetrics(legend_font).getHeight())*legend_text.size() + 15;
			
			if (legend_box) {
				g2d.setColor(Color.WHITE);
				g2d.fill(legendBounds);
				
				g2d.setColor(Color.BLACK);
				g2d.draw(legendBounds);
			}
				
			for (int i=0; i < legend_text.size(); i++) {
				g2d.setColor(Color.BLACK);
				g2d.drawString(legend_text.get(i), legendBounds.x + 10, legendBounds.y + (4 + g.getFontMetrics(legend_font).getHeight())*(i+1));
			}
			
			if (isLegendSelected) {
				g2d.fill(new Rectangle(legendBounds.x-5, legendBounds.y-5, 10, 10));
				g2d.fill(new Rectangle(legendBounds.x+legendBounds.width-5, legendBounds.y-5, 10, 10));
				g2d.fill(new Rectangle(legendBounds.x-5, legendBounds.y+legendBounds.height-5, 10, 10));
				g2d.fill(new Rectangle(legendBounds.x+legendBounds.width-5, legendBounds.y+legendBounds.height-5, 10, 10));
			}
			g.setFont(font);
		} 
		
	}
	
	@Override
	public void paint(Graphics g) {
		
		if (bounds == null)
			return;
		
		if (updatePlotBoundaries) {
			updatePlotBoundaries(g);
			updatePlotBoundaries = false;
		}
		
		int width = getWidth() - leftMargin - rightMargin;
		int height = getHeight() - bottomMargin - topMargin;
		
		transform = new AffineTransform();
		transform.translate(leftMargin, topMargin + height);
		transform.scale(width / bounds.width, -height / bounds.height);
		transform.translate(-bounds.x, -bounds.y);
		
		// transform all plot coordinates to pixel coordinates
		for (int i = 0; i < plotCoordinates.size(); i++) {
			double[] src = plotCoordinates.get(i);
			double[] dst = pixelCoordinates.get(i);
			
			transform.transform(src, 0, dst, 0, src.length / 2);
		}
		
		g.setFont(font);
		paintAxis(g);
		paintPlot(g);
	}
	
	public void updatePlotBoundaries(Graphics g) {
		//Need to find the longest Y to properly set margin...
		int yMaxSize = Math.max(g.getFontMetrics(font).stringWidth(String.format("%." + yaxis_precision + "f", bounds.y)), g.getFontMetrics(font).stringWidth(String.format("%." + yaxis_precision + "f", bounds.y + bounds.height)));
		
		leftMargin = yMaxSize + 10;
		bottomMargin = 2*g.getFontMetrics(font).getHeight();
		topMargin = 0;
		
		if (!yAxisLabel.equals("")) {
			leftMargin += 2*g.getFontMetrics(label_font).getHeight() - 10; 
		}
		if (!xAxisLabel.equals("")) {
			bottomMargin += 2*g.getFontMetrics(label_font).getHeight() - g.getFontMetrics(font).getHeight()/2;
		}
		if (!plotTitle.equals("")) {
			topMargin = (int)(1.5*g.getFontMetrics(title_font).getHeight());
		}
	}
	
	private double getStepSize(double range, int steps) {
		double step = range / steps;    // e.g. 0.00321
		double magnitude = Math.pow(10, Math.floor(Math.log10(step)));  // e.g. 0.001
		double mostSignificantDigit = Math.ceil(step / magnitude); // e.g. 3.21
		
        if (mostSignificantDigit > 5.0)
            return magnitude * 10.0;
        else if (mostSignificantDigit > 2.0)
            return magnitude * 5.0;
        else
            return magnitude * 2.0;
	}
	
	public void showPlot(String title) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(500, 500);
		frame.getContentPane().setLayout(new BorderLayout());
		updatePlotBoundaries = true;
		frame.getContentPane().add(this, BorderLayout.CENTER);
		frame.setBackground(Color.WHITE);
		frame.setVisible(true);
	}
	
	public void zoom(double factor) {
		bounds.x -= bounds.width * (factor - 1) / 2;
		bounds.y -= bounds.height * (factor - 1) / 2;
		bounds.width *= factor;
		bounds.height *= factor;
		
		updatePlotBoundaries = true;
		
		repaint();
		
		notifyBoundsChangedListeners();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == saveMenuItem) {
			savePlot();
		}
		else if (e.getSource() == copyMenuItem) {
			copyPlot();
		}
		else if (e.getSource() == propertiesMenuItem) {
			setProperties();
			updatePlotBoundaries = true;
		}
		//else if (e.getSource() == addDataSet) {
		//	addDataSet();
		//	updatePlotBoundaries = true;
		//	repaint();
		//}
		else if (e.getSource() == fitData) {
			//We assume there is only one plot and select the first plot in the set...
			LMCurveFitter fitter = new LMCurveFitter();
			fitter.fit(this);
			updatePlotBoundaries = true;
			repaint();
		}
		else if (e.getSource() == selectRegionMenuItem) {
			regionSelection.width = 0;
			regionSelection.height = 0;
			setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			isRegionSelection = true;
			updatePlotBoundaries = true;
		}
		
	}
	
	public void setProperties() {
		GenericDialog dia = new GenericDialog("Plot Properties");
		
		dia.addMessage("Bounds:");
		FieldOptionPanel bounds_options = new FieldOptionPanel(2);
		bounds_options.addNumericField("X from", bounds.x, 6, 15);
		bounds_options.addNumericField(" to", bounds.x + bounds.width, 6, 15);
		bounds_options.addNumericField("Y from", bounds.y, 6, 15);
		bounds_options.addNumericField(" to", bounds.y + bounds.height, 6, 15);		
		dia.addPanel(bounds_options, GridBagConstraints.WEST, new Insets(15,15,0,0));
		
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

		dia.addMessage("Plot label settings:");
		dia.addChoice("Font", fonts, font.getName());
		dia.addNumericField("Axis size", font.getSize(), 0);
		FieldOptionPanel axis_label_options = new FieldOptionPanel(3);
		axis_label_options.addStringField("    Y-axis ", yAxisLabel, 15);
		axis_label_options.addNumericField(" precision ", yaxis_precision, 0);
		axis_label_options.addNumericField("size", label_font.getSize(), 0);
		axis_label_options.addStringField("    X-axis ", xAxisLabel, 15);
		axis_label_options.addNumericField(" precision ", xaxis_precision, 0);
		axis_label_options.advanceRows();
		axis_label_options.addStringField("    Title  ", plotTitle, 15);
		axis_label_options.addNumericField(" size ", title_font.getSize(), 0);
		dia.addPanel(axis_label_options, GridBagConstraints.WEST, new Insets(15,15,0,0));
		
		dia.addMessage("Legend:");
		
		String all_text = new String("");
		if (legend_text!=null) {
			for (String line:legend_text) {
				all_text += line + "\n";
			}
		}
		dia.addTextAreas(all_text,null,6,35);
		dia.addNumericField("size", legend_font.getSize(), 0);
		dia.addCheckbox("box", legend_box);
		dia.addCheckbox("show legend", show_legend);
		
		dia.addMessage("Plots:");
		
		ArrayList<Panel> plotPanels = new ArrayList<Panel>();
		for (int i=0; i<plotCoordinates.size() ; i++) {
			Panel plotpref = plotPrefPpanel(i);
			dia.addPanel(plotpref, GridBagConstraints.WEST, new Insets(0,0,0,0));
			plotPanels.add(plotpref);
		}
		
		dia.addMessage("Misc:");
		dia.addNumericField("bar gap size", gap, 2);
		dia.addCheckbox("track curve", show_tracker);
		dia.addCheckbox("gridlines",gridlines);
		dia.addCheckbox("include bg start/stop in points table", sigmaStartStop);
		
		dia.enableYesNoCancel("OK", "Save & Run");
		dia.showDialog();
		
		if (!dia.wasCanceled()) {
			double xfrom = bounds_options.getNextNumber();
			double xto = bounds_options.getNextNumber();
			double yfrom = bounds_options.getNextNumber();
			double yto = bounds_options.getNextNumber();
			
			setBounds(xfrom, yfrom, xto - xfrom, yto - yfrom);
			
			String font_choice = dia.getNextChoice();
			int font_size = (int)dia.getNextNumber();
			
			yAxisLabel = axis_label_options.getNextString();
			yaxis_precision = (int)axis_label_options.getNextNumber();
			int label_font_size = (int)axis_label_options.getNextNumber();
			xAxisLabel = axis_label_options.getNextString();
			xaxis_precision = (int)axis_label_options.getNextNumber();
			plotTitle = axis_label_options.getNextString();
			int title_font_size = (int)axis_label_options.getNextNumber();
			
			int legend_font_size = (int)dia.getNextNumber();
			String text_Legend_Area = dia.getNextText();
			
			for (int w = plotPanels.size() - 1; w >= 0; w--) {
				Panel pan = plotPanels.get(w);
				plotColors.set(w, getColorFromName(((Choice)pan.getComponent(1)).getSelectedItem()));
				plotLineWidths.set(w, Float.parseFloat(((TextField)pan.getComponent(2)).getText()));
				plotStyles.set(w, getStyleFromName(((Choice)pan.getComponent(3)).getSelectedItem()));
				if(((Checkbox)pan.getComponent(4)).getState())
					deletePlot(w);
			}
			
			if (!text_Legend_Area.equals("")) {
				if (legend_text == null) {
					legendBounds.x = leftMargin + 20;
					legendBounds.y = topMargin + 20;
				}
				legend_text = new ArrayList<String>();
				String[] legText = text_Legend_Area.split("\n");
				for (String line:legText) 
					legend_text.add(line);
			} else {
				legend_text=null;
			}
			legend_box = dia.getNextBoolean();
			show_legend = dia.getNextBoolean();
			
			gap = dia.getNextNumber();
			show_tracker = dia.getNextBoolean();
			gridlines = dia.getNextBoolean();
			sigmaStartStop = dia.getNextBoolean();
			
			font = new Font(font_choice, Font.PLAIN, font_size);
			label_font = new Font(font_choice, Font.PLAIN, label_font_size);
			title_font = new Font(font_choice, Font.PLAIN, title_font_size);
			legend_font = new Font(font_choice, Font.PLAIN, legend_font_size);
			if (!dia.wasOKed()) {
				//Means save setting was selected...
				Prefs.set("plot.sigma_start_stop", sigmaStartStop);
				Prefs.set("plot.gridlines", gridlines);
				Prefs.set("plot.font_type", font_choice);
				Prefs.set("plot.font_size", font_size);
				Prefs.set("plot.label_font_size", label_font_size);
				Prefs.set("plot.yaxis_precision", yaxis_precision);
				Prefs.set("plot.xaxis_precision", xaxis_precision);
				Prefs.set("plot.title_font_size", title_font_size);
				Prefs.set("plot.bar_gap_size",gap);
				Prefs.set("plot.show_legend", show_legend);
				Prefs.set("plot.legend_font_size", legend_font_size);
				Prefs.set("plot.legend_box", legend_box);
				Prefs.set("plot.show_tracker", show_tracker);
			}
			repaint();
		}
	}
	
	private String getTypeName(int index) {
		switch (plotTypes.get(index)) {
		case LINE:
			return "Line - ";
		case SCATTER:
			return "Scatter - ";
		case BAR:
			return "Bar - ";
		case SEGMENTS:
			return "Segments - ";
		case FIT:
			return "Fit - ";
		}
		return "          ";
	}
	
	private Style getStyleFromName(String style_name) {
		if (style_name.equals("solid")) {
			return Style.SOLID;
		} else if (style_name.equals("stripes")) {
			return Style.STRIPES;
		}
		return Style.SOLID;
	}
	
	private String getNameFromStyle(Style style) {
		if (style.equals(Style.SOLID)) {
			return "solid";
		} else if (style.equals(Style.STRIPES)) {
			return "stripes";
		}
		return "solid";
	}
	
	private Color getColorFromName(String color_name) {
		try {
			if (color_name.equals("dark green"))
				return darkGreen;
			Field field = Class.forName("java.awt.Color").getField(color_name);
			return (Color)field.get(null);
		} catch (Exception e) {
			//can't happen since only the colors specified above can be picked and those all exist...
		}
		return Color.black;
	}
	
	private String getNameFromColor(Color color) {
		for (int i = 0; i < colors.length ; i++) {
			if (color == getColorFromName(colors[i]))
				return colors[i];
		}
		return "black";
	}
	
	private Panel plotPrefPpanel(int index) {
		Panel plotpan = new Panel();
		plotpan.add(new Label(getTypeName(index) + plotNames.get(index)));
		Choice color_choice = new Choice();
		for (int w = 0; w < colors.length; w++) {
			color_choice.add(colors[w]);
		}
		color_choice.select(getNameFromColor(plotColors.get(index)));
		plotpan.add(color_choice);
		TextField LineWidth = new TextField();
		LineWidth.setColumns(4);
		LineWidth.setText(plotLineWidths.get(index).toString());
		plotpan.add(LineWidth);
		Choice patt = new Choice();
		if (plotTypes.get(index).equals(Type.BAR)) {
			patt.add("solid");
			patt.add("stripes");
		} else {
			patt.add("solid");
		}
		patt.select(getNameFromStyle(plotStyles.get(index)));
		plotpan.add(patt);
		Checkbox check = new Checkbox("delete", false);
		plotpan.add(check);
		return plotpan;
	}
	
	public void savePlot() {
		JFileChooser fileChooser = new JFileChooser(currentDirectory);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Portable Network Graphics (*.png)", "png"));
		
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			savePlot(file);
		}
	}
	
	public void savePlot(File file) {
		currentDirectory = file.getParentFile();
		
		// make sure the file extension is .png
		if (!file.getName().toLowerCase().endsWith(".png"))
			file = new File(file.getPath() + ".png");
		
		if (file.exists()) {
			if (JOptionPane.showConfirmDialog(this, String.format("Overwrite existing file: %s ?", file.getName())) != JOptionPane.OK_OPTION)
				return;
		}
		
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		paint(image.createGraphics());
		
		try {
			ImageIO.write(image, "png", file);
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, String.format("could not save image : %s", e.getMessage()), "Exception", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public BufferedImage getImage(int width, int height) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(width, height);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(this, BorderLayout.CENTER);
		frame.setVisible(true);
		
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		Graphics g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		paint(g);
		
		frame.dispose();
		
        return image;
	}
	
	public void copyPlot() {
		
		Transferable transferable = new Transferable() {
			
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.imageFlavor.equals(flavor);
			}
			
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[]{DataFlavor.imageFlavor, DataFlavor.stringFlavor};
			}
			
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				
				if (flavor.equals(DataFlavor.imageFlavor)) {				
					BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
					
					Graphics g = image.createGraphics();
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, getWidth(), getHeight());
					paint(g);
					
			        return image;
				}
				else if (flavor.equals(DataFlavor.stringFlavor)) {
					String csv = "";
					
					for (int i = 0; i < plotCoordinates.size(); i++) {
						double[] values = plotCoordinates.get(i);
						
						for (int j = 0; j < values.length; j += 2)
							csv += String.format("%d\t%f\t%f\n", i, values[j], values[j + 1]);
					}
					
					return csv;
				}
				else {
					throw new UnsupportedFlavorException(flavor);
				}
			}
		};
		
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
	}
	
	public void exportData() {
		JFileChooser fileChooser = new JFileChooser(currentDirectory);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Comma Separated Values (*.csv)", "csv"));
		
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			exportData(file);
		}
	}
	
	public void exportData(File file) {
		currentDirectory = file.getParentFile();
		
		// make sure the file extension is .csv
		if (!file.getName().toLowerCase().endsWith(".csv"))
			file = new File(file.getPath() + ".csv");
		
		if (file.exists()) {
			if (JOptionPane.showConfirmDialog(this, String.format("Overwrite existing file: %s ?", file.getName())) != JOptionPane.OK_OPTION)
				return;
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(file);
			
			fos.write("dataset,row,x,y\r\n".getBytes("utf-8"));
			
			for (int i = 0; i < plotCoordinates.size(); i++) {
				double[] values = plotCoordinates.get(i);
				
				for (int j = 0; j < values.length; j += 2)
					fos.write(String.format("%d,%d,%f,%f\r\n", i, j, values[j], values[j + 1]).getBytes("utf-8"));
			}
			
			fos.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, String.format("could not export data : %s", e.getMessage()), "Exception", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private ArrayList<BoundsChangedListener> listeners =  new ArrayList<BoundsChangedListener>();
	private ArrayList<MoleculeChangedListener> molListeners = new ArrayList<MoleculeChangedListener>();
	
	public void addBoundsChangedListener(BoundsChangedListener listener) {
		listeners.add(listener);
	}
	
	public void notifyBoundsChangedListeners() {
		for (BoundsChangedListener listener: listeners)
			listener.boundsChanged(bounds, leftMargin);
	}
	
	public void addMoleculeChangedListener(MoleculeChangedListener listener) {
		molListeners.add(listener);
	}
	
	public void notifyMoleculeChangedListeners() {
		for (MoleculeChangedListener listener: molListeners)
			listener.MoleculeChanged(molecule);
	}
	
	public void setPlotTitle(String plotTitle) {
		this.plotTitle = plotTitle;
	}
	
	public void setMolecule(Molecule molecule) {
		this.molecule = molecule;
	}
	
	public Molecule getMolecule() {
		return molecule;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Plot plot = new Plot();
		
		int n = 100;
		double[] x = new double[n];
		double[] y = new double[n];
		double[] x2 = new double[n];
		double[] y2 = new double[n];
		
		for (int i = 0; i < n; i++) {
			x[i] = (2 * Math.PI * i) / n;
			y[i] = Math.sin(x[i]);
			x2[i] = (2 * Math.PI * i) / n;
			y2[i] = Math.tan(x[i]);
		}
		
		//plot.addHistogram(y, (int)Math.sqrt(n), Color.BLUE, 3.0f);
		plot.addLinePlot(x, y, Color.BLACK, 1.0f);
		plot.addLinePlot(x2, y2, Color.RED, 1.0f);
		plot.setOriginalBounds(0, -1, Math.PI * 2, 2);
		plot.showPlot("test");
	}

}
