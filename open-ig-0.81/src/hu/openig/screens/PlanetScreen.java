/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.screens;


import hu.openig.core.Act;
import hu.openig.core.Location;
import hu.openig.core.Tile;
import hu.openig.gfx.ColonyGFX;
import hu.openig.model.Building;
import hu.openig.model.PlanetSurface;
import hu.openig.model.SurfaceEntity;
import hu.openig.model.SurfaceEntityType;
import hu.openig.render.TextRenderer;
import hu.openig.utils.ImageUtils;
import hu.openig.xold.res.gfx.TextGFX;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.Timer;

/**
 * @author karnokd, 2010.01.11.
 * @version $Revision 1.0$
 */
public class PlanetScreen extends ScreenBase {
	/** The planet surface definition. */
	PlanetSurface surface;
	/** The offset X. */
	int offsetX;
	/** The offset Y. */
	int offsetY;
	/** The current location based on the mouse pointer. */
	Location current;
	/** 
	 * The selected rectangular region. The X coordinate is the smallest, the Y coordinate is the largest
	 * the width points to +X and height points to -Y direction
	 */
	Rectangle selectedRectangle;
	/** Show the buildings? */
	boolean showBuildings = true;
	/** The selection tile. */
	Tile selection;
	/** The placement tile for allowed area. */
	Tile areaAccept;
	/** The empty tile indicator. */
	Tile areaEmpty;
	/** The placement tile for denied area. */
	Tile areaDeny;
	/** The current cell tile. */
	Tile areaCurrent;
	/** The current scaling factor. */
	double scale = 1;
	/** Used to place buildings on the surface. */
	final Rectangle placementRectangle = new Rectangle();
	/** The building bounding box. */
	Rectangle buildingBox;
	/** Are we in placement mode? */
	boolean placementMode;
	/** Render the buildings as symbolic cells. */ 
	boolean minimapMode;
	/** The text renderer. */
	TextRenderer txt;
	/** The colony graphics. */
	ColonyGFX colonyGFX;
	/** The simple blinking state. */
	boolean blink;
	/** The animation index. */
	int animation;
	/** The animation timer. */
	Timer animationTimer;
	/** Enable the drawing of black boxes behind building names and percentages. */
	boolean textBackgrounds = true;
	/** Render placement hints on the surface. */
	boolean placementHints;
	/** The surface cell image. */
	static class SurfaceCell {
		/** The tile target. */
		public int a;
		/** The tile target. */
		public int b;
		/** The image to render. */
		public BufferedImage image;
		/** The Y coordinate compensation. */
		public int yCompensation;
	}
	/** The surface cell helper. */
	final SurfaceCell cell = new SurfaceCell();
	/** The last mouse coordinate. */
	int lastX;
	/** The last mouse coordinate. */
	int lastY;
	/** Is the map dragged. */
	boolean drag;
	/** Is a selection box dragged. */
	boolean sel;
	/** The originating location. */
	Location orig;

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#doResize()
	 */
	@Override
	public void doResize() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#finish()
	 */
	@Override
	public void finish() {
		animationTimer.stop();
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#initialize()
	 */
	@Override
	public void initialize() {
		surface = new PlanetSurface();
		surface.width = 33;
		surface.height = 66;
		surface.computeRenderingLocations();
		animationTimer = new Timer(100, new Act() {
			@Override
			public void act() {
				//wrap animation index
				if (animation == Integer.MAX_VALUE) {
					animation = -1;
				}
				animation++;
				blink = (animation % 10) >= 5;
				repaint();
			}
		});
		selection = new Tile(1, 1, ImageUtils.recolor(commons.colony.tileEdge, 0xFFFFFF00), null);
		areaAccept = new Tile(1, 1, ImageUtils.recolor(commons.colony.tileEdge, 0xFF00FFFF), null);
		areaEmpty = new Tile(1, 1, ImageUtils.recolor(commons.colony.tileEdge, 0xFF808080), null);
		areaDeny = new Tile(1, 1, ImageUtils.recolor(commons.colony.tileCrossed, 0xFFFF0000), null);
		areaCurrent  = new Tile(1, 1, ImageUtils.recolor(commons.colony.tileCrossed, 0xFFFFCC00), null);
		
		selection.alpha = 1.0f;
		areaAccept.alpha = 1.0f;
		areaDeny.alpha = 1.0f;
		areaCurrent.alpha = 1.0f;
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#keyTyped(int, int)
	 */
	@Override
	public void keyTyped(int key, int modifiers) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#mouseMoved(int, int, int, int)
	 */
	@Override
	public void mouseMoved(int button, int x, int y, int modifiers) {
		if (drag) {
			offsetX += x - lastX;
			offsetY += y - lastY;
			
			lastX = x;
			lastY = y;
			repaint();
		} else
		if (sel) {
			Location loc = getLocationAt(x, y);
			current = loc;
			placementRectangle.x = current.x - placementRectangle.width / 2;
			placementRectangle.y = current.y + placementRectangle.height / 2;
			selectedRectangle.x = Math.min(orig.x, loc.x);
			selectedRectangle.y = Math.max(orig.y, loc.y);
			selectedRectangle.width = Math.max(orig.x, loc.x) - selectedRectangle.x + 1;
			selectedRectangle.height = - Math.min(orig.y, loc.y) + selectedRectangle.y + 1;
			repaint();
		} else {
			current = getLocationAt(x, y);
			if (current != null) {
				placementRectangle.x = current.x - placementRectangle.width / 2;
				placementRectangle.y = current.y + placementRectangle.height / 2;
				repaint();
			}
		}
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#mousePressed(int, int, int, int)
	 */
	@Override
	public void mousePressed(int button, int x, int y, int modifiers) {
		if (isRightButton(button)) {
			drag = true;
			lastX = x;
			lastY = y;
		} else
		if (isMiddleButton(button)) {
			offsetX = 0;
			offsetY = 0;
			if (isCtrl(modifiers)) {
				scale = 1;
			}
			repaint();
		}
		if (isLeftButton(button) && surface != null) {
			sel = true;
			selectedRectangle = new Rectangle();
			orig = getLocationAt(x, y);
			selectedRectangle.x = orig.x;
			selectedRectangle.y = orig.y;
			selectedRectangle.width = 1;
			selectedRectangle.height = 1;
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#mouseReleased(int, int, int, int)
	 */
	@Override
	public void mouseReleased(int button, int x, int y, int modifiers) {
		if (isRightButton(button)) {
			drag = false;
		}
		if (isLeftButton(button)) {
			sel = false;
		}
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#mouseScrolled(int, int, int, int)
	 */
	@Override
	public void mouseScrolled(int direction, int x, int y, int modifiers) {
		if (isCtrl(modifiers)) {
			double pre = scale;
			double mx = (x - offsetX) * pre;
			double my = (y - offsetY) * pre;
			if (direction < 0) {
				doZoomIn();
			} else {
				doZoomOut();
			}
			double mx0 = (x - offsetX) * scale;
			double my0 = (y - offsetY) * scale;
			double dx = (mx - mx0) / pre;
			double dy = (my - my0) / pre;
			offsetX += (int)(dx);
			offsetY += (int)(dy);
			requestRepaint();
		}
	}
	/**
	 * Zoom to 100%.
	 */
	protected void doZoomNormal() {
		scale = 1.0;
		repaint();
	}
	/**
	 * 
	 */
	protected void doZoomOut() {
		scale = Math.max(0.1, scale - 0.1);
	}
	/**
	 * 
	 */
	protected void doZoomIn() {
		scale = Math.min(2.0, scale + 0.1);
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#onEnter()
	 */
	@Override
	public void onEnter() {
		animationTimer.start();
		offsetX = -(surface.boundingRectangle.width - getWidth()) / 2;
		offsetY = -(surface.boundingRectangle.height - getHeight()) / 2;
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#onLeave()
	 */
	@Override
	public void onLeave() {
		animationTimer.stop();
	}

	/* (non-Javadoc)
	 * @see hu.openig.v1.ScreenBase#paintTo(java.awt.Graphics2D)
	 */
	@Override
	public void paintTo(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		g2.setColor(new Color(96, 96, 96));
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		if (surface == null) {
			return;
		}
		AffineTransform at = g2.getTransform();
		g2.translate(offsetX, offsetY);
		g2.scale(scale, scale);
		
		int x0 = surface.baseXOffset;
		int y0 = surface.baseYOffset;

		Rectangle br = surface.boundingRectangle;
		g2.setColor(new Color(128, 0, 0));
		g2.fillRect(br.x, br.y, br.width, br.height);
		g2.setColor(Color.YELLOW);
		g2.drawRect(br.x, br.y, br.width, br.height);
		
		
		BufferedImage empty = areaEmpty.getStrip(0);
		Rectangle renderingWindow = new Rectangle(0, 0, getWidth(), getHeight());
		for (int i = 0; i < surface.renderingOrigins.size(); i++) {
			Location loc = surface.renderingOrigins.get(i);
			for (int j = 0; j < surface.renderingLength.get(i); j++) {
				int x = x0 + Tile.toScreenX(loc.x - j, loc.y);
				int y = y0 + Tile.toScreenY(loc.x - j, loc.y);
				Location loc1 = Location.of(loc.x - j, loc.y);
				SurfaceEntity se = surface.buildingmap.get(loc1);
				if (se == null || !showBuildings) {
					se = surface.basemap.get(loc1);
				}
				if (se != null) {
					getImage(se, minimapMode, loc1, cell);
					int yref = y0 + Tile.toScreenY(cell.a, cell.b) + cell.yCompensation;
					if (renderingWindow.intersects(x * scale + offsetX, yref * scale + offsetY, 57 * scale, se.tile.imageHeight * scale)) {
						if (cell.image != null) {
							g2.drawImage(cell.image, x, yref, null);
						}
					}
				} else {
					if (renderingWindow.intersects(x * scale + offsetX, y * scale + offsetY, 57 * scale, 27 * scale)) {
						g2.drawImage(empty, x, y, null);
					}
				}
			}
		}
		if (placementHints) {
			for (Location loc : surface.basemap.keySet()) {
				if (!canPlaceBuilding(loc.x, loc.y)) {
					int x = x0 + Tile.toScreenX(loc.x, loc.y);
					int y = y0 + Tile.toScreenY(loc.x, loc.y);
					g2.drawImage(areaDeny.getStrip(0), x, y, null);
				}
			}
		}
		if (!placementMode) {
			if (selectedRectangle != null) {
				for (int i = selectedRectangle.x; i < selectedRectangle.x + selectedRectangle.width; i++) {
					for (int j = selectedRectangle.y; j > selectedRectangle.y - selectedRectangle.height; j--) {
						int x = x0 + Tile.toScreenX(i, j);
						int y = y0 + Tile.toScreenY(i, j);
						g2.drawImage(selection.getStrip(0), x, y, null);
					}
				}
			}
			if (current != null) {
				int x = x0 + Tile.toScreenX(current.x, current.y);
				int y = y0 + Tile.toScreenY(current.x, current.y);
				g2.drawImage(areaCurrent.getStrip(0), x, y, null);
			}
		} else
		if (placementRectangle.width > 0) {
			for (int i = placementRectangle.x; i < placementRectangle.x + placementRectangle.width; i++) {
				for (int j = placementRectangle.y; j > placementRectangle.y - placementRectangle.height; j--) {
					
					BufferedImage img = areaAccept.getStrip(0);
					// check for existing building
					if (!canPlaceBuilding(i, j)) {
						img = areaDeny.getStrip(0);
					}
					
					int x = x0 + Tile.toScreenX(i, j);
					int y = y0 + Tile.toScreenY(i, j);
					g2.drawImage(img, x, y, null);
				}
			}
		}
		g2.setColor(Color.RED);
		if (showBuildings) {
			if (buildingBox != null) {
				g2.drawRect(buildingBox.x, buildingBox.y, buildingBox.width, buildingBox.height);
			}
			for (Building b : surface.buildings) {
				Rectangle r = getBoundingRect(b.location);
//				if (r == null) {
//					continue;
//				}
				String label = commons.labels.get(b.type.label);
				int nameLen = txt.getTextWidth(7, label);
				int h = (r.height - 7) / 2;
				int nx = r.x + (r.width - nameLen) / 2;
				int ny = r.y + h;
				
				Composite compositeSave = null;
				Composite a1 = null;
				
				if (textBackgrounds) {
					compositeSave = g2.getComposite();
					a1 = AlphaComposite.SrcOver.derive(0.8f);
					g2.setComposite(a1);
					g2.setColor(Color.BLACK);
					g2.fillRect(nx - 2, ny - 2, nameLen + 4, 12);
					g2.setComposite(compositeSave);
				}
				
				txt.paintTo(g2, nx + 1, ny + 1, 7, TextGFX.LIGHT_BLUE, label);
				txt.paintTo(g2, nx, ny, 7, 0xD4FC84, label);

				// paint upgrade level indicator
				int uw = b.upgradeLevel * colonyGFX.upgrade.getWidth();
				int ux = r.x + (r.width - uw) / 2;
				int uy = r.y + h - colonyGFX.upgrade.getHeight() - 4; 

				String percent = null;
				int color = TextGFX.LIGHT_BLUE;
				if (b.isConstructing()) {
					percent = (b.buildProgress * 100 / b.type.hitpoints) + "%";
				} else
				if (b.hitpoints < b.type.hitpoints) {
					percent = ((b.type.hitpoints - b.hitpoints) * 100 / b.type.hitpoints) + "%";
					if (!blink) {
						color = TextGFX.RED;
					}
				}
				if (percent != null) {
					int pw = txt.getTextWidth(10, percent);
					int px = r.x + (r.width - pw) / 2;
					int py = uy - 14;

					if (textBackgrounds) {
						g2.setComposite(a1);
						g2.setColor(Color.BLACK);
						g2.fillRect(px - 2, py - 2, pw + 4, 15);
						g2.setComposite(compositeSave);
					}
					
					txt.paintTo(g2, px + 1, py + 1, 10, color, percent);
					txt.paintTo(g2, px, py, 10, 0xD4FC84, percent);
				}
				
				for (int i = 1; i <= b.upgradeLevel; i++) {
					g2.drawImage(colonyGFX.upgrade, ux, uy, null);
					ux += colonyGFX.upgrade.getWidth();
				}
				
				if (b.enabled) {
					int ey = r.y + h + 11;
					int w = 0;
					if (b.isEnergyShortage()) {
						w += colonyGFX.unpowered[0].getWidth();
					}
					if (b.isWorkerShortage()) {
						w += colonyGFX.worker[0].getWidth();
					}
					if (b.repairing) {
						w += colonyGFX.repair[0].getWidth();
					}
					int ex = r.x + (r.width - w) / 2;
					// paint power shortage
					if (b.isEnergyShortage()) {
						g2.drawImage(colonyGFX.unpowered[blink ? 0 : 1], ex, ey, null);
						ex += colonyGFX.unpowered[0].getWidth();
					}
					if (b.isWorkerShortage()) {
						g2.drawImage(colonyGFX.worker[blink ? 0 : 1], ex, ey, null);
						ex += colonyGFX.worker[0].getWidth();
					}
					if (b.repairing) {
						g2.drawImage(colonyGFX.repair[(animation / 3) % 3], ex, ey, null);
						ex += colonyGFX.repair[0].getWidth();
					}
				} else {
					int ey = r.y + h + 13;
					String offline = commons.labels.get("buildings.offline");
					int w = txt.getTextWidth(10, offline);
					color = TextGFX.LIGHT_BLUE;
					if (!blink) {
						color = TextGFX.RED;
					}
					int ex = r.x + (r.width - w) / 2;
					if (textBackgrounds) {
						g2.setComposite(a1);
						g2.setColor(Color.BLACK);
						g2.fillRect(ex - 2, ey - 2, w + 4, 15);
						g2.setComposite(compositeSave);
					}
					
					txt.paintTo(g2, ex + 1, ey + 1, 10, color, offline);
					txt.paintTo(g2, ex, ey, 10, 0xD4FC84, offline);
				}
			}
		}
		
		g2.setTransform(at);
		g2.setColor(Color.WHITE);
	}
	@Override
	public void mouseDoubleClicked(int button, int x, int y, int modifiers) {
		// TODO Auto-generated method stub
		
	}
	/**
	 * Return the image (strip) representing this surface entry.
	 * The default behavior returns the tile strips along its lower 'V' arc.
	 * This method to be overridden to handle the case of damaged or in-progress buildings
	 * @param se the surface entity
	 * @param symbolic display a symbolic tile instead of the actual building image.
	 * @param loc1 the the target cell location
	 * @param cell the output for the image and the Y coordinate compensation
	 */
	public void getImage(SurfaceEntity se, boolean symbolic, Location loc1, SurfaceCell cell) {
		if (se.type != SurfaceEntityType.BUILDING) {
			cell.yCompensation = 27 - se.tile.imageHeight;
			cell.a = loc1.x - se.virtualColumn;
			cell.b = loc1.y + se.virtualRow - se.tile.height + 1;
			if (se.virtualColumn == 0 && se.virtualRow < se.tile.height) {
				cell.image = se.tile.getStrip(se.virtualRow);
				return;
			} else
			if (se.virtualRow == se.tile.height - 1) {
				cell.image = se.tile.getStrip(se.tile.height - 1 + se.virtualColumn);
				return;
			}
			cell.image = null;
			return;
		}
		if (symbolic) {
			Tile tile = null;

			if (se.building.isConstructing()) {
				if (se.building.isSeverlyDamaged()) {
					tile = se.building.type.minimapTiles.constructingDamaged;
				} else {
					tile = se.building.type.minimapTiles.constructing;
				}
			} else {
				if (se.building.isDestroyed()) {
					tile = se.building.type.minimapTiles.destroyed;
				} else
				if (se.building.isSeverlyDamaged()) {
					tile = se.building.type.minimapTiles.damaged;
				} else
				if (se.building.getEfficiency() < 0.5f) {
					tile = se.building.type.minimapTiles.inoperable;
				} else {
					tile = se.building.type.minimapTiles.normal;
				}
			}
			
			cell.yCompensation = 27 - tile.imageHeight;
			cell.image = tile.getStrip(0);
			cell.a = loc1.x;
			cell.b = loc1.y;
			
			return;
		}
		if (se.building.isConstructing()) {
			Tile tile = null;
			if (se.building.isSeverlyDamaged()) {
				int constructIndex = se.building.buildProgress * se.building.scaffolding.damaged.size() / se.building.type.hitpoints;
				tile =  se.building.scaffolding.damaged.get(constructIndex);
			} else {
				int constructIndex = se.building.buildProgress * se.building.scaffolding.normal.size() / se.building.type.hitpoints;
				tile =  se.building.scaffolding.normal.get(constructIndex);
			}
			cell.yCompensation = 27 - tile.imageHeight;
			cell.image = tile.getStrip(0);
			cell.a = loc1.x;
			cell.b = loc1.y;
		} else {
			Tile tile = null;
			if (se.building.isSeverlyDamaged()) {
				tile = se.building.tileset.damaged;
			} else 
			if (se.building.getEfficiency() < 0.5f) {
				tile = se.building.tileset.nolight;
			} else {
				tile = se.building.tileset.normal;
			}
			tile.alpha = se.tile.alpha;
			cell.yCompensation = 27 - tile.imageHeight;
			cell.a = loc1.x - se.virtualColumn;
			cell.b = loc1.y + se.virtualRow - se.tile.height + 1;
			if (se.virtualColumn == 0 && se.virtualRow < se.tile.height) {
				cell.image = tile.getStrip(se.virtualRow);
				return;
			} else
			if (se.virtualRow == se.tile.height - 1) {
				cell.image = tile.getStrip(se.tile.height - 1 + se.virtualColumn);
				return;
			}
			cell.image = null;
			return;
		}
			
	}
	/**
	 * Test if the given rectangular region is eligible for building placement, e.g.:
	 * all cells are within the map's boundary, no other buildings are present within the given bounds,
	 * no multi-tile surface object is present at the location.
	 * @param rect the surface rectangle
	 * @return true if the building can be placed
	 */
	public boolean canPlaceBuilding(Rectangle rect) {
		for (int i = rect.x; i < rect.x + rect.width; i++) {
			for (int j = rect.y; j > rect.y - rect.height; j--) {
				if (!canPlaceBuilding(i, j)) {
					return false;
				}
			}
		}
		return true;
	}
	/**
	 * Test if the coordinates are suitable for building placement.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return true if placement is allowed
	 */
	public boolean canPlaceBuilding(int x, int y) {
		if (!surface.cellInMap(x, y)) {
			return false;
		} else {
			SurfaceEntity se = surface.buildingmap.get(Location.of(x, y));
			if (se != null && se.type == SurfaceEntityType.BUILDING) {
				return false;
			} else {
				se = surface.basemap.get(Location.of(x, y));
				if (se != null && (se.tile.width > 1 || se.tile.height > 1)) {
					return false;
				}
			}
		}
		return true;
	}
	/**
	 * Get a location based on the mouse coordinates.
	 * @param mx the mouse X coordinate
	 * @param my the mouse Y coordinate
	 * @return the location
	 */
	public Location getLocationAt(int mx, int my) {
		if (surface != null) {
			double mx0 = mx - (surface.baseXOffset + 28) * scale - offsetX; // Half left
			double my0 = my - (surface.baseYOffset + 27) * scale - offsetY; // Half up
			int a = (int)Math.floor(Tile.toTileX((int)mx0, (int)my0) / scale);
			int b = (int)Math.floor(Tile.toTileY((int)mx0, (int)my0) / scale) ;
			return Location.of(a, b);
		}
		return null;
	}
	/**
	 * Compute the bounding rectangle of the rendered building object.
	 * @param loc the location to look for a building.
	 * @return the bounding rectangle or null if the target does not contain a building
	 */
	public Rectangle getBoundingRect(Location loc) {
		SurfaceEntity se = surface.buildingmap.get(loc);
		if (se != null && se.type == SurfaceEntityType.BUILDING) {
			int a0 = loc.x - se.virtualColumn;
			int b0 = loc.y + se.virtualRow;
			
			int x = surface.baseXOffset + Tile.toScreenX(a0, b0);
			int y = surface.baseYOffset + Tile.toScreenY(a0, b0 - se.tile.height + 1) + 27;
			
			return new Rectangle(x, y - se.tile.imageHeight, se.tile.imageWidth, se.tile.imageHeight);
		}
		return null;
	}
}
