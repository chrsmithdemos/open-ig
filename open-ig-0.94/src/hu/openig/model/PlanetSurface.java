/*
 * Copyright 2008-2012, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */
package hu.openig.model;

import hu.openig.core.Location;
import hu.openig.core.PlanetType;
import hu.openig.core.RoadType;
import hu.openig.core.Sides;
import hu.openig.core.Tile;
import hu.openig.utils.U;
import hu.openig.utils.XElement;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The container class for the planetary surface objects, including the base surface map,
 * builings, roads and vehicles.
 * @author akarnokd
 */
public class PlanetSurface {
	/** 
	 * The map's width in cells. 
	 * The width is defined as a slightly descending horizontal dimension of the map, 
	 * but in coordinate terms it is equal to the sequence 0,0 1,-1, 2,-2 etc.
	 * Note that the rendering coordinate system is different from the original IG's map definition. 
	 */
	public int width;
	/** The height with in cells. The width is defined as a vertical dimension of the map,
	 *  but in coordinate terms it is equal to the sequence 0,0 -1,-1, -2,-2 etc. */
	public int height;
	/**
	 * The accessible rectangle of the surface defined in pixels. The accessible origin is encoded relative to the top-left corner of where the Location(0,0) is rendered.
	 */
	public final Rectangle accessibleRect = new Rectangle();
	/**
	 * The base map of the surface. Kept separate from the building maps for the case the user
	 * demolishes a building.
	 */
	public Map<Location, SurfaceEntity> basemap = new HashMap<Location, SurfaceEntity>();
	/**
	 * The buildings and roads map.
	 */
	public final Map<Location, SurfaceEntity> buildingmap = new HashMap<Location, SurfaceEntity>();
	/** The pre-computed locations where the angular rendering should start. */
	public final List<Location> renderingOrigins = new ArrayList<Location>();
	/** The pre-computed locations where the angular rendering should end. */
	public final List<Integer> renderingLength = new ArrayList<Integer>();
	/** The base X offset to shift the (0,0) cell horizontally. */
	public int baseXOffset;
	/** The base Y offset to shift the (0,0) vertically. */
	public int baseYOffset;
	/** The bounding rectangle that fits all cells of this map. */
	public Rectangle boundingRectangle;
	/** The list of building instances. */
	public final List<Building> buildings = new ArrayList<Building>();
	/** The list of surface features. */
	public List<SurfaceFeature> features = new ArrayList<SurfaceFeature>();
	/** The placement helper. */
	public PlacementHelper placement = new PlacementHelper() {
		@Override
		protected Map<Location, SurfaceEntity> basemap() {
			return basemap;
		}
		@Override
		protected Map<Location, SurfaceEntity> buildingmap() {
			return buildingmap;
		}
		@Override
		protected boolean cellInMap(int x, int y) {
			return PlanetSurface.this.cellInMap(x, y);
		}
		@Override
		protected int height() {
			return height;
		}
		@Override
		protected int width() {
			return width;
		};
		@Override
		protected List<Building> buildings() {
			return buildings;
		}
	};
	/** Compute the rendering start-stop locations. */
	public void computeRenderingLocations() {
		renderingOrigins.clear();
		renderingLength.clear();
		// y -> x
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int i = 0; i < height; i++) {
			map.put(-i, -i);
		}
		for (int j = 1; j < width; j++) {
			map.put(- (height - 1) - j, -(height - 1) + j);
		}
		for (int i = 0; i < width; i++) {
			int x = i;
			int y = -i;
			renderingOrigins.add(Location.of(i, -i));
			renderingLength.add(x - map.get(y) + 1);
		}
		for (int j = 1; j < height; j++) {
			int x = (width - 1) - j;
			int y = - (width - 1) - j;
			renderingOrigins.add(Location.of(x, y));
			renderingLength.add(x - map.get(y) + 1);
		}
		baseYOffset = 0;
		baseXOffset = -Tile.toScreenX(-height, -height);
		int x0 = 0;
		int y0 = 0;
		int x1 = Tile.toScreenX(width - 1, -width + 1) + baseXOffset;
		Location loc = renderingOrigins.get(renderingOrigins.size() - 1); 
		int y1 = Tile.toScreenY(loc.x, loc.y);
		boundingRectangle = new Rectangle(x0, y0, x1 - x0 + 57, y1 - y0 + 28); 
	}
	/**
	 * Check if the given cell coordinates fall into the bounds of the surface map's visible (and rendered) zone.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return true if the cell is within the map bounds
	 */
	public boolean cellInMap(int x, int y) {
		if (y > 0 || y < -(width + height - 2)) {
			return false;
		}
		if (x > renderingOrigins.get(-y).x || x < renderingOrigins.get(-y).x - renderingLength.get(-y) + 1) {
			return false;
		}		
		return true;
	}
	/**
	 * Place a building tile onto the current surface map.
	 * Does not check for overlapping.
	 * @param tile the tile
	 * @param x the tile's leftmost coordinate
	 * @param y the tile's topmost coordinate
	 * @param building the building object to assign
	 */
	public void placeBuilding(Tile tile, int x, int y, Building building) {
		for (int a = x; a < x + tile.width; a++) {
			for (int b = y; b > y - tile.height; b--) {
				SurfaceEntity se = new SurfaceEntity();
				se.type = SurfaceEntityType.BUILDING;
				se.virtualRow = y - b;
				se.virtualColumn = a - x;
				se.tile = tile;
				se.building = building;
				buildingmap.put(Location.of(a, b), se);
			}
		}
		buildings.add(building);
	}
	/**
	 * Place a tile onto the current surface map.
	 * Does not check for overlapping.
	 * @param tile the tile
	 * @param x the tile's leftmost coordinate
	 * @param y the tile's topmost coordinate
	 * @param id the base tile id
	 * @param surface the surface type
	 */
	public void placeBase(Tile tile, int x, int y, int id, String surface) {
		for (int a = x; a < x + tile.width; a++) {
			for (int b = y; b > y - tile.height; b--) {
				SurfaceEntity se = new SurfaceEntity();
				se.type = SurfaceEntityType.BASE;
				se.virtualRow = y - b;
				se.virtualColumn = a - x;
				se.tile = tile;
				basemap.put(Location.of(a, b), se);
			}
		}
		SurfaceFeature sf = new SurfaceFeature();
		sf.id = id;
		sf.type = surface;
		sf.tile = tile;
		sf.location = Location.of(x, y);
		features.add(sf);
	}
	/**
	 * Set the size of the surface map.
	 * @param width the width (direction +1, -1)
	 * @param height the height (direction -1, -1)
	 */
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		computeRenderingLocations();
	}
	/**
	 * Parse a map definition XML.
	 * @param map the map
	 * @param gm the galaxy model, null if no surface needs to be loaded
	 * @param bm the building model, null if no building needs to be loaded
	 */
	public void parseMap(XElement map, GalaxyModel gm, BuildingModel bm) {
		if (gm != null) {
			XElement surface = map.childElement("surface");
			if (surface != null) {
				this.features.clear();
				this.basemap.clear();
				int width = Integer.parseInt(surface.get("width"));
				int height = Integer.parseInt(surface.get("height"));
				setSize(width, height);
				for (XElement tile : surface.childrenWithName("tile")) {
					String type = tile.get("type");
					int id = Integer.parseInt(tile.get("id"));
					int x = Integer.parseInt(tile.get("x"));
					int y = Integer.parseInt(tile.get("y"));
					PlanetType pt = gm.planetTypes.get(type);
					if (pt == null) {
						System.err.println("Missing planet type: " + type);
					}
					Tile t = pt.tiles.get(id);
					if (t == null) {
						System.err.println("Missing tile: " + id + " on planet type " + type);
					}
					
					placeBase(t, x, y, id, type);
				}
			}
		}
		if (bm != null) {
			XElement buildings = map.childElement("buildings");
			if (buildings != null) {
				setBuildings(bm, buildings);
			}
		}
	}
	/**
	 * Set the surface buildings from the given XElement.
	 * @param bm the building model
	 * @param buildings the buildings XElement
	 */
	void setBuildings(BuildingModel bm, XElement buildings) {
		this.buildings.clear();
		this.buildingmap.clear();
		
		for (XElement tile : buildings.childrenWithName("building")) {
			String id = tile.get("id");
			if (id.equals("FusionProjector")) {
				id = "ParticleProjector";
			}
			String tech = tile.get("tech");
			
			Building b = new Building(bm.buildings.get(id), tech);
			int x = tile.getInt("x");
			int y = tile.getInt("y");
		
			b.location = Location.of(x, y);
			
			String bp = tile.get("build");
			if (bp == null || bp.isEmpty()) {
				b.buildProgress = b.type.hitpoints;
			} else {
				b.buildProgress = Integer.parseInt(bp);
			}
			String hp = tile.get("hp");
			if (hp == null || hp.isEmpty()) {
				b.hitpoints = b.type.hitpoints;
			} else {
				b.hitpoints = Integer.parseInt(hp);
			}
			b.setLevel(Math.min(Integer.parseInt(tile.get("level")), b.type.upgrades.size())
			);
			b.assignedEnergy = Integer.parseInt(tile.get("energy"));
			b.assignedWorker = Integer.parseInt(tile.get("worker"));
			b.enabled = "true".equals(tile.get("enabled"));
			b.repairing = "true".equals(tile.get("repairing"));
			
			placeBuilding(b.tileset.normal, x, y, b);
		}
		placeRoads(getTechnology(), bm);
	}
	/**
	 * Store the map elements under the given XElement.
	 * @param map the map to store the surface and/or buildings
	 * @param withSurface store the surface
	 * @param withBuildings store the buildings?
	 */
	public void storeMap(XElement map, boolean withSurface, boolean withBuildings) {
		if (withSurface) {
			XElement surfaces = map.add("surface");
			surfaces.set("width", width);
			surfaces.set("height", height);
			for (SurfaceFeature sf : features) {
				XElement tile = surfaces.add("tile");
				tile.set("type", sf.type);
				tile.set("id", sf.id);
				tile.set("x", sf.location.x);
				tile.set("y", sf.location.y);
			}
		}
		if (withBuildings) {
			XElement xbuildings = map.add("buildings");
			for (Building b : buildings) {
				XElement xb = xbuildings.add("building");
				xb.set("id", b.type.id);
				xb.set("tech", b.techId);
				xb.set("x", b.location.x);
				xb.set("y", b.location.y);
				xb.set("build", b.buildProgress);
				xb.set("hp", b.hitpoints);
				xb.set("level", b.upgradeLevel);
				xb.set("worker", b.assignedWorker);
				xb.set("energy", b.assignedEnergy);
				xb.set("enabled", b.enabled);
				xb.set("repairing", b.repairing);
			}
		}
	}
	/**
	 * @return the technology id of the last building placed on the planet, null if no buildings present
	 */
	public String getTechnology() {
		if (buildings.size() > 0) {
			return buildings.get(buildings.size() - 1).techId;
		}
		return null;
	}
	/**
	 * Create a deep copy of the surface by sharing the basemap but
	 * copying the buildings.
	 * @return the planet surface copy
	 */
	public PlanetSurface copy() {
		PlanetSurface result = new PlanetSurface();
		
		result.setSize(width, height);
		
		for (Building b : buildings) {
			Building bc = b.copy();
			result.placeBuilding(bc.tileset.normal, bc.location.x, bc.location.y, bc);
		}
		// share basemap
		result.basemap = basemap;
		result.features = features;
		
		for (Map.Entry<Location, SurfaceEntity> se : buildingmap.entrySet()) {
			if (se.getValue().type == SurfaceEntityType.ROAD) {
				result.buildingmap.put(se.getKey(), se.getValue());
			}
		}
		
		return result;
	}
	/**
	 * Place roads around buildings for the given race.
	 * @param raceId the race who builds the roads
	 * @param bm the building model for the roads
	 */
	public void placeRoads(String raceId, BuildingModel bm) {
		Map<RoadType, Tile> rts = bm.roadTiles.get(raceId);
		Map<Tile, RoadType> trs = bm.tileRoads.get(raceId);
		// remove all roads
		Iterator<SurfaceEntity> it = buildingmap.values().iterator();
		while (it.hasNext()) {
			SurfaceEntity se = it.next();
			if (se.type == SurfaceEntityType.ROAD) {
				it.remove();
			}
		}
		
		Set<Location> corners = new HashSet<Location>();
		for (Building bld : buildings) {
			Rectangle rect = new Rectangle(bld.location.x - 1, bld.location.y + 1, bld.tileset.normal.width + 2, bld.tileset.normal.height + 2);
			addRoadAround(rts, rect, corners);
		}
		SurfaceEntity[] neighbors = new SurfaceEntity[9];
		for (Location l : corners) {
			SurfaceEntity se = buildingmap.get(l);
			if (se == null || se.type != SurfaceEntityType.ROAD) {
				continue;
			}
			setNeighbors(l.x, l.y, buildingmap, neighbors);
			int pattern = 0;
			
			RoadType rt1 = null;
			if (neighbors[1] != null && neighbors[1].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.TOP;
				rt1 = trs.get(neighbors[1].tile);
			}
			RoadType rt3 = null;
			if (neighbors[3] != null && neighbors[3].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.LEFT;
				rt3 = trs.get(neighbors[3].tile);
			}
			RoadType rt5 = null;
			if (neighbors[5] != null && neighbors[5].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.RIGHT;
				rt5 = trs.get(neighbors[5].tile);
			}
			RoadType rt7 = null;
			if (neighbors[7] != null && neighbors[7].type == SurfaceEntityType.ROAD) {
				pattern |= Sides.BOTTOM;
				rt7 = trs.get(neighbors[7].tile);
			}
			RoadType rt = RoadType.get(pattern);
			// place the new tile fragment onto the map
			// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
			se = createRoadEntity(rts.get(rt));
			buildingmap.put(l, se);
			// alter the four neighboring tiles to contain road back to this
			if (rt1 != null) {
				rt1 = RoadType.get(rt1.pattern | Sides.BOTTOM);
				buildingmap.put(l.delta(0, 1), createRoadEntity(rts.get(rt1)));
			}
			if (rt3 != null) {
				rt3 = RoadType.get(rt3.pattern | Sides.RIGHT);
				buildingmap.put(l.delta(-1, 0), createRoadEntity(rts.get(rt3)));
			}
			if (rt5 != null) {
				rt5 = RoadType.get(rt5.pattern | Sides.LEFT);
				buildingmap.put(l.delta(1, 0), createRoadEntity(rts.get(rt5)));
			}
			if (rt7 != null) {
				rt7 = RoadType.get(rt7.pattern | Sides.TOP);
				buildingmap.put(l.delta(0, -1), createRoadEntity(rts.get(rt7)));
			}
			
		}
	}
	/**
	 * Create a road entity for the tile.
	 * @param tile the tile
	 * @return the entity
	 */
	SurfaceEntity createRoadEntity(Tile tile) {
		SurfaceEntity result = new SurfaceEntity();
		result.tile = tile;
		result.type = SurfaceEntityType.ROAD;
		return result;
	}
	/**
	 * Fills the fragment array of the 3x3 rectangle centered around x and y.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param map the map
	 * @param fragments the fragments
	 */
	void setNeighbors(int x, int y, Map<Location, SurfaceEntity> map, SurfaceEntity[] fragments) {
		fragments[0] = map.get(Location.of(x - 1, y + 1));
		fragments[1] = map.get(Location.of(x, y + 1));
		fragments[2] = map.get(Location.of(x + 1, y + 1));
		
		fragments[3] = map.get(Location.of(x - 1, y));
		fragments[4] = map.get(Location.of(x, y));
		fragments[5] = map.get(Location.of(x + 1, y));
		
		fragments[6] = map.get(Location.of(x - 1, y - 1));
		fragments[7] = map.get(Location.of(x, y - 1));
		fragments[8] = map.get(Location.of(x + 1, y - 1));
	}
	/**
	 * Places a road frame around the tilesToHighlight rectangle.
	 * @param rts the road to tile map for a concrete race
	 * @param rect the rectangle to use
	 * @param corners where to place the created corners
	 */
	void addRoadAround(Map<RoadType, Tile> rts, Rectangle rect, Collection<Location> corners) {
		Location la = Location.of(rect.x, rect.y);
		Location lb = Location.of(rect.x + rect.width - 1, rect.y);
		Location lc = Location.of(rect.x, rect.y - rect.height + 1);
		Location ld = Location.of(rect.x + rect.width - 1, rect.y - rect.height + 1);
		
		corners.add(la);
		corners.add(lb);
		corners.add(lc);
		corners.add(ld);
		
		buildingmap.put(la, createRoadEntity(rts.get(RoadType.RIGHT_TO_BOTTOM)));
		buildingmap.put(lb, createRoadEntity(rts.get(RoadType.LEFT_TO_BOTTOM)));
		buildingmap.put(lc, createRoadEntity(rts.get(RoadType.TOP_TO_RIGHT)));
		buildingmap.put(ld, createRoadEntity(rts.get(RoadType.TOP_TO_LEFT)));
		// add linear segments
		
		Tile ht = rts.get(RoadType.HORIZONTAL);
		for (int i = rect.x + 1; i < rect.x + rect.width - 1; i++) {
			buildingmap.put(Location.of(i, rect.y), createRoadEntity(ht));
			buildingmap.put(Location.of(i, rect.y - rect.height + 1), createRoadEntity(ht));
		}
		Tile vt = rts.get(RoadType.VERTICAL);
		for (int i = rect.y - 1; i > rect.y - rect.height + 1; i--) {
			buildingmap.put(Location.of(rect.x, i), createRoadEntity(vt));
			buildingmap.put(Location.of(rect.x + rect.width - 1, i), createRoadEntity(vt));
		}
	}
	/**
	 * Removes the given building from the map.
	 * @param building the building to remove
	 */
	public void removeBuilding(Building building) {
		if (buildings.remove(building)) {
			for (int a = building.location.x; a < building.location.x + building.tileset.normal.width; a++) {
				for (int b = building.location.y; b > building.location.y - building.tileset.normal.height; b--) {
					buildingmap.remove(Location.of(a, b));
				}
			}
		}
	}
	/**
	 * The placement helper class to determine if a specific region size is available on the surface.
	 * @author akarnokd, 2011.12.27.
	 */
	public abstract static class PlacementHelper {
		/** @return the map's horizontal width. */
		protected abstract int width();
		/** @return the map's vertical height. */
		protected abstract int height();
		/**
		 * Test if the given coordinate is within the map.
		 * @param x the X coordinate
		 * @param y the Y coordinate
		 * @return true if within the map
		 */
		protected abstract boolean cellInMap(int x, int y);
		/** @return the building map */
		protected abstract Map<Location, SurfaceEntity> buildingmap();
		/** @return the base map. */
		protected abstract Map<Location, SurfaceEntity> basemap();
		/** @return the existing buildings. */
		protected abstract List<Building> buildings();
		/**
		 * Test if the given rectangular region is eligible for building placement, e.g.:
		 * all cells are within the map's boundary, no other buildings are present within the given bounds,
		 * no multi-tile surface object is present at the location.
		 * @param rect the surface rectangle
		 * @return true if the building can be placed
		 */
		public boolean canPlaceBuilding(Rectangle rect) {
			return canPlaceBuilding(rect.x, rect.y, rect.width, rect.height);
		}
		/**
		 * Test if the given rectangular region is eligible for building placement, e.g.:
		 * all cells are within the map's boundary, no other buildings are present within the given bounds,
		 * no multi-tile surface object is present at the location.
		 * @param x the left coordinate
		 * @param y the top coordinate
		 * @param width the width into +X direction
		 * @param height the height into -Y direction
		 * @return true if the building can be placed
		 */
		public boolean canPlaceBuilding(int x, int y, int width, int height) {
			for (int i = x; i < x + width; i++) {
				for (int j = y; j > y - height; j--) {
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
			if (!cellInMap(x, y)) {
				return false;
			} else {
				SurfaceEntity se = buildingmap().get(Location.of(x, y));
				if (se != null && se.type == SurfaceEntityType.BUILDING) {
					return false;
				} else {
					se = basemap().get(Location.of(x, y));
					if (se != null && (se.tile.width > 1 || se.tile.height > 1)) {
						return false;
					}
				}
			}
			return true;
		}
		/**
		 * Find a location for the given dimensions.
		 * <p>Note: the dimensions should incorporate 1+1 road on both axis.</p>
		 * @param dim the dimensions
		 * @return the location or null if not found
		 */
		public Point findLocation(Dimension dim) {
			return findLocation(dim.width, dim.height);
		}
		/**
		 * Find a location on the surface which can support a building (and surrounding roads)
		 * with the given size. The location search starts of from the center of the map
		 * @param width should be the building tile width + 2
		 * @param height should be the builindg tile height + 2
		 * @return the top-left point where this building could be built, null indicates that
		 * no suitable location is present
		 */
		public Point findLocation(int width, int height) {
			int cx = this.width() / 2 - this.height() / 2 - width / 2;
			int cy = -this.width() / 2 - this.height() / 2 + height / 2;

			int rx1 = Math.abs(this.width() - cx);
			int rx2 = Math.abs(-this.height() - cx);
			int ry1 = Math.abs(this.width() + this.height() + cy);
			int ry2 = Math.abs(cy);
			int maxr = Math.max(Math.max(rx1, rx2), Math.max(ry1, ry2));
			// the square size
			List<PlaceCandidate> candidates = U.newArrayList();
			for (int i = 0; i < maxr; i++) {
				int len = i * 2 + 1;
				int size = len > 1 ? len * 2 + (len - 2) * 2 : 1;
				int[] xs = new int[size];
				int[] ys = new int[size];
				clockwise(xs, ys, len);
				for (int k = 0; k < size; k++) {
					int x0 = cx + xs[k] - i;
					int y0 = cy - ys[k] + i;
					if (canPlaceBuilding(x0, y0, width, height)) {
						int d = (cx - x0) * (cx - x0) + (cy - y0) * (cy - y0);
						PlaceCandidate pc = createCandidate(x0, y0, width, height, d);
						if (pc != null) {
							candidates.add(pc);
						}
					}
				}
				// if only check for placement is running
				if (buildings().isEmpty() && !candidates.isEmpty()) {
					PlaceCandidate pc = Collections.max(candidates);
					return new Point(pc.x, pc.y);
				}
			}
			if (candidates.size() > 0) {
				PlaceCandidate pc = Collections.max(candidates);
				return new Point(pc.x, pc.y);
			}
			
			return null;
		}
		/**
		 * Create a candidate place for the location.
		 * @param x0 coordinates
		 * @param y0 coordinates
		 * @param width placement width
		 * @param height placement height
		 * @param d distance to center
		 * @return the candidate or null if not near a road
		 */
		PlaceCandidate createCandidate(int x0, int y0, int width, int height, int d) {
			// no buildings at all
			if (buildingmap().isEmpty()) {
				return new PlaceCandidate(x0, y0, 1, 0, d);
			}
			int roads = 1;
			int edges = 0;
			for (int k = x0; k < x0 + width; k++) {
				for (int j = y0; j > y0 - height; j--) {
					if (isRoad(k, j)) {
						roads++;
						if (isCrossRoad(k, j)) {
							edges++;
						}
					}
				}
			}
//			if (roads > 0) {
				return new PlaceCandidate(x0, y0, roads, edges, d);
//			}
//			return null;
		}
		/**
		 * Fill in a clockwise coordinate pair of a given length rectangle.
		 * @param xs the xs
		 * @param ys the ys
		 * @param len the rectangle length
		 */
		void clockwise(int[] xs, int[] ys, int len) {
			int j = 0;
			for (int i = 0; i < len; i++) {
				xs[j] = i;
				ys[j] = 0;
				j++;
			}
			for (int i = 1; i < len; i++) {
				xs[j] = len - 1;
				ys[j] = i;
				j++;
			}
			for (int i = len - 2; i >= 0; i--) {
				xs[j] = i;
				ys[j] = len - 1;
				j++;
			}
			for (int i = len - 2; i >= 1; i--) {
				xs[j] = 0;
				ys[j] = i;
				j++;
			}
		}
		/**
		 * Check if the cell is the edge of a building road.
		 * @param x the center x
		 * @param y the center y
		 * @return true if edge road
		 */
		boolean isCrossRoad(int x, int y) {
			if (isEdge(x - 1, y - 1) && isRoad(x, y - 1) && isRoad(x - 1, y)) {
				return true;
			}
			if (isEdge(x + 1, y - 1) && isRoad(x, y - 1) && isRoad(x + 1, y)) {
				return true;
			}
			if (isEdge(x - 1, y + 1) && isRoad(x - 1, y) && isRoad(x, y + 1)) {
				return true;
			}
			if (isEdge(x + 1, y + 1) && isRoad(x, y + 1) && isRoad(x + 1, y)) {
				return true;
			}
			return false;
		}
		/**
		 * Check if the given location is a road.
		 * @param x the X coordinate
		 * @param y the Y coordinate
		 * @return true if road
		 */
		boolean isRoad(int x, int y) {
			SurfaceEntity e = buildingmap().get(Location.of(x, y));
			return e != null && e.type == SurfaceEntityType.ROAD;
		}
		/**
		 * Check if the given location is a building edge.
		 * @param x the X coordinate
		 * @param y the Y coordinate
		 * @return true if road
		 */
		boolean isEdge(int x, int y) {
			SurfaceEntity e = buildingmap().get(Location.of(x, y));
			if (e != null && e.building != null) {
				if (e.virtualColumn == 0 && e.virtualRow == 0) {
					return true;
				}
				if (e.virtualColumn == 0 && e.virtualRow == e.building.tileset.normal.height - 1) {
					return true;
				}
				if (e.virtualColumn == e.building.tileset.normal.width - 1 && e.virtualRow == e.building.tileset.normal.height - 1) {
					return true;
				}
				if (e.virtualColumn == e.building.tileset.normal.width - 1 && e.virtualRow == 0) {
					return true;
				}
			}
			return false;
		}
		/**
		 * A building place candidate.
		 * @author akarnokd, 2011.12.30.
		 */
		public static class PlaceCandidate implements Comparable<PlaceCandidate> {
			/** Location X. */
			public final int x;
			/** Location Y. */
			public final int y;
			/** Number of contacting building cells. */
			public final int contact;
			/** Number of connected buildings. */
			public final int edges;
			/** Distance to center. */
			public final int distance;
			/**
			 * Constructor. Initializes the fields.
			 * @param x location X
			 * @param y location Y
			 * @param contact Number of contacting building cells
			 * @param edges number of edges
			 * @param distance distance to center
			 */
			public PlaceCandidate(int x, int y, int contact, int edges, int distance) {
				this.x = x;
				this.y = y;
				this.contact = contact;
				this.edges = edges;
				this.distance = distance;
			}
			@Override
			public int compareTo(PlaceCandidate o) {
//				int c = 0;
//				if (c == 0) {
//					c = contact - o.contact;
//				}
//				if (c == 0) {
//					c = edges - o.edges;
//				}
//				if (c == 0) {
//					c = distance - o.distance;
//				}
				double v1 = (contact + edges * 4.0) / (distance + 1);
				double v2 = (o.contact + o.edges * 4.0) / (o.distance + 1);
				return v1 < v2 ? -1 : (v1 > v2 ? 1 : 0);
			}
		}
		/**
		 * Compute the gravitational effects of the surrounding building and move
		 * to the direction while still placeable.
		 * @param original the original location
		 * @param width the building width containing +2 for roads
		 * @param height the building height containing +2 for roads
		 * @return the adjusted location
		 */
		Point gravityAdjust(Point original, int width, int height) {
			List<Building> bs = buildings();
			if (bs.isEmpty()) {
				return original;
			}
			Point result = new Point(original);
			int countDown = 1;
			while (!Thread.currentThread().isInterrupted() && (countDown-- > 0)) {
				Point2D.Double force0 = new Point2D.Double(0, 0);
				double mass0 = width * height;
				for (Building b : bs) {
					int bw = b.tileset.normal.width;
					int bh = b.tileset.normal.height;
					// vector from origin to building
					Point2D.Double vector = new Point2D.Double(
							b.location.x + bw / 2.0 - (result.x + width / 2.0), 
							b.location.y - bh / 2.0 - (result.y - height / 2.0));
					double distance2 = vector.x * vector.x + vector.y * vector.y;
					double distance = Math.sqrt(distance2);
					double mass = bw * bh;
	
					// unit vector
					vector.x = vector.x / distance;
					vector.y = vector.y / distance;
	
					Point2D.Double force = new Point2D.Double(0, 0);
					
					// F = k * m0 * m / r2 * |v|
					force.x = mass0 * mass * vector.x / distance2;
					force.y = mass0 * mass * vector.y / distance2;
					
					force0.x += force.x;
					force0.y += force.y;
				}
				// determine movement direction
				double angle = Math.atan2(force0.y, force0.x);
				
				Point save = new Point(result);
				if (angle <= Math.PI / 4 && angle >= -Math.PI / 4) {
					result.x++;
				} else
				if (angle > Math.PI / 4 && angle <= Math.PI * 3 / 4) {
					result.y++;
				} else
				if (angle < -Math.PI / 4 && angle > -Math.PI * 3 / 4) {
					result.y--;
				} else {
					result.x--;
				}
				if (!canPlaceBuilding(result.x, result.y, width, height)) {
					result = save;
					break;
				}
			}
			return result;
		}
	}
}
