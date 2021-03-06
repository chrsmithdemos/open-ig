/*
 * Copyright 2008-2009, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */
package hu.openig.gfx;

import hu.openig.utils.ImageUtils;
import hu.openig.utils.PACFile;
import hu.openig.utils.PCXImage;
import hu.openig.utils.ResourceMapper;
import hu.openig.utils.PACFile.PACEntry;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Record to store the graphical elements of the starmap screen. */
public class StarmapGFX {
	/** The starmap contents object. */
	public final StarmapContents contents;
	/** Map from surface type to drawing size to list of animation phase images. */
	public final Map<String, Map<Integer, List<BufferedImage>>> starmapPlanets;
	/** The border button image. */
	public final BufferedImage btnBorder;
	/** The radars button image. */
	public final BufferedImage btnRadars;
	/** Stars button image. */
	public final BufferedImage btnStars;
	/** Fleets button image. */
	public final BufferedImage btnFleets;
	/** Destination button image. */
	public final BufferedImage btnDest;
	/** Grids button image. */
	public final BufferedImage btnGrids;
	/** Borders highlighted image. */
	public final BufferedImage btnBorderLight;
	/** Radars highlighted image. */
	public final BufferedImage btnRadarsLight;
	/** Stars highlight button image. */
	public final BufferedImage btnStarsLight;
	/** Fleets highlight button image. */
	public final BufferedImage btnFleetsLight;
	/** Destinations highlight button image. */
	public final BufferedImage btnDestLight;
	/** Grids highlight button image. */
	public final BufferedImage btnGridsLight;
	/** Name off button image. */
	public final BufferedImage btnNameOff;
	/** Name colony button image. */
	public final BufferedImage btnNameColony;
	/** Name fleets button image. */
	public final BufferedImage btnNameFleets;
	/** Name both button image. */
	public final BufferedImage btnNameBoth;
	/** Colonise button image. */
	public final BufferedImage btnColonise;
	/** Colonise disabled button image. */
	public final BufferedImage btnColoniseDisabled;
	/** Add satelite button. */
	public final BufferedImage btnAddSat;
	/** Add spy sat 1 button image. */
	public final BufferedImage btnAddSpySat1;
	/** Add spy sat 2 button image. */
	public final BufferedImage btnAddSpySat2;
	/** Add hubble 2 button image. */
	public final BufferedImage btnAddHubble2;
	/** Equipment disabled button image. */
	public final BufferedImage btnEquipmentDisabled;
	/** Equipment down button image. */
	public final BufferedImage btnEquipmentDown;
	/** Colony button down image. */
	public final BufferedImage btnColonyDown;
	/** Zoom button hightlighted. */
	public final BufferedImage btnMagnifyLight;
	/** Magnify disabled button image. */
	public final BufferedImage btnMagnifyDisabled;
	/** Normal disabled button image. */
	public final BufferedImage btnNormalDisabled;
	/** Scroll disabled button image. */
	public final BufferedImage btnScrollDisabled;
	/** Zoom disabled button image. */
	public final BufferedImage btnZoomDisabled;
	/** Normal light button image. */
	public final BufferedImage btnNormalLight;
	/** Scroll light button image. */
	public final BufferedImage btnScrollLight;
	/** Zoom light button image. */
	public final BufferedImage btnZoomLight;
	/** Prev disabled button image. */
	public final BufferedImage btnPrevDisabled;
	/** Prev down button image. */
	public final BufferedImage btnPrevDown;
	/** Next disabled button image. */
	public final BufferedImage btnNextDisabled;
	/** Next down button image. */
	public final BufferedImage btnNextDown;
	/** Info down button image. */
	public final BufferedImage btnInfoDown;
	/** Bridge down button image. */
	public final BufferedImage btnBridgeDown;
	/** Ship move button light. */
	public final BufferedImage btnMoveLight;
	/** Attack button highlight image. */
	public final BufferedImage btnAttackLight;
	/** Stop button highlicht image. */
	public final BufferedImage btnStopLight;
	/**
	 * Constructor. Loads all images belonging to the starmap.
	 * @param resMap the resource mapper object.
	 */
	public StarmapGFX(ResourceMapper resMap) {
		// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
		// STARMAP BODY
		// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
		BufferedImage body = PCXImage.from(resMap.get("SCREENS/STARMAP.PCX"), -1);
		
		contents = new StarmapContents();
		
		contents.bottomLeft = ImageUtils.subimage(body, 0, 331, 33, 111);
		contents.bottomFiller = ImageUtils.subimage(body, 33, 331, 2, 111);
		contents.bottomRight = ImageUtils.subimage(body, 33, 331, 607, 111);
		contents.shipControls = ImageUtils.subimage(body, 285, 359, 106, 83);
		contents.rightTop = ImageUtils.subimage(body, 505, 0, 135, 42);
		contents.rightFiller = ImageUtils.subimage(body, 505, 42, 135, 2);
		contents.rightBottom = ImageUtils.subimage(body, 505, 42, 135, 289);
		
		// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
		// SCROLLBAR SEGMENTS
		// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
		contents.hscrollLeft = ImageUtils.subimage(body, 37, 334, 18, 18);
		contents.hscrollFiller = ImageUtils.subimage(body, 55, 334, 2, 18);
		contents.hscrollRight = ImageUtils.subimage(body, 55, 334, 17, 18);
		
		contents.vscrollTop = ImageUtils.subimage(body, 508, 291, 18, 18);
		contents.vscrollFiller = ImageUtils.subimage(body, 508, 309, 18, 2);
		contents.vscrollBottom = ImageUtils.subimage(body, 508, 309, 18, 17);
		
		// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
		// LOADING VARIOUS PLANET ANIMATIONS
		// oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

		starmapPlanets = new HashMap<String, Map<Integer, List<BufferedImage>>>();
		for (PACEntry pe : PACFile.parseFully(resMap.get("DATA/BOLYGOK.PAC"))) {
			String type = pe.filename.substring(0, 4);
			Map<Integer, List<BufferedImage>> aplanet = starmapPlanets.get(type);
			if (aplanet == null) {
				aplanet = new HashMap<Integer, List<BufferedImage>>();
				starmapPlanets.put(type, aplanet);
			}
			BufferedImage planetImg = PCXImage.parse(pe.data, -2);
			
			int width = planetImg.getWidth();
			int count = planetImg.getHeight() / width;
			List<BufferedImage> list = new ArrayList<BufferedImage>(count);
			for (int i = 0; i < count; i++) {
				list.add(ImageUtils.subimage(planetImg, 0, width * i, width, width));
			}
			
			aplanet.put(planetImg.getWidth(), list);
		}
		
		BufferedImage starx = PCXImage.from(resMap.get("SCREENS/STAR_X.PCX"), -1);
		
		btnBorderLight = ImageUtils.subimage(starx, 0, 0, 53, 18);
		btnRadarsLight = ImageUtils.subimage(starx, 53, 0, 53, 18);
		btnStarsLight = ImageUtils.subimage(starx, 0, 18, 53, 18);
		btnFleetsLight = ImageUtils.subimage(starx, 53, 18, 53, 18);
		btnDestLight = ImageUtils.subimage(starx, 0, 36, 53, 18);
		btnGridsLight = ImageUtils.subimage(starx, 53, 36, 53, 18);

		btnBorder = ImageUtils.subimage(starx, 108, 0, 53, 18);
		btnRadars = ImageUtils.subimage(starx, 161, 0, 53, 18);
		btnStars = ImageUtils.subimage(starx, 108, 18, 53, 18);
		btnFleets = ImageUtils.subimage(starx, 161, 18, 53, 18);
		btnDest = ImageUtils.subimage(starx, 108, 36, 53, 18);
		btnGrids = ImageUtils.subimage(starx, 161, 36, 53, 18);

		btnNameOff = ImageUtils.subimage(starx, 0, 54, 108, 18);
		btnNameColony = ImageUtils.subimage(starx, 0, 72, 108, 18);
		btnNameFleets = ImageUtils.subimage(starx, 0, 90, 108, 18);
		btnNameBoth = ImageUtils.subimage(starx, 0, 108, 108, 18);
		
		btnColonise = ImageUtils.subimage(starx, 0, 126, 108, 15);
		btnColoniseDisabled = ImageUtils.subimage(starx, 108, 126, 108, 15);
		
		btnAddSat = ImageUtils.subimage(starx, 109, 54, 84, 17);
		btnAddSpySat1 = ImageUtils.subimage(starx, 109, 71, 84, 17);
		btnAddSpySat2 = ImageUtils.subimage(starx, 109, 88, 84, 17);
		btnAddHubble2 = ImageUtils.subimage(starx, 109, 105, 84, 17);
		
		btnEquipmentDisabled = ImageUtils.subimage(starx, 216, 0, 103, 28);
		btnEquipmentDown = ImageUtils.subimage(starx, 216, 28, 103, 28);
		btnColonyDown = ImageUtils.subimage(starx, 216, 84, 103, 28);
		
		btnMagnifyLight = ImageUtils.subimage(starx, 352, 60, 33, 64);
		btnMagnifyDisabled = ImageUtils.subimage(starx, 599, 78, 33, 64);
		
		btnNormalDisabled = ImageUtils.subimage(starx, 319, 138, 66, 20);
		btnScrollDisabled = ImageUtils.subimage(starx, 385, 138, 66, 20);
		btnZoomDisabled = ImageUtils.subimage(starx, 451, 138, 66, 20);
		
		btnNormalLight = ImageUtils.subimage(starx, 533, 78, 66, 20);
		btnScrollLight = ImageUtils.subimage(starx, 533, 98, 66, 20);
		btnZoomLight = ImageUtils.subimage(starx, 533, 118, 66, 20);
		
		btnPrevDisabled = ImageUtils.subimage(starx, 483, 0, 50, 20);
		btnPrevDown = ImageUtils.subimage(starx, 483, 41, 50, 18);
		btnNextDisabled = ImageUtils.subimage(starx, 483, 60, 49, 20);
		btnNextDown = ImageUtils.subimage(starx, 483, 101, 49, 18);
		
		btnInfoDown = ImageUtils.subimage(starx, 533, 0, 102, 39);
		btnBridgeDown = ImageUtils.subimage(starx, 533, 39, 102, 39);
		
		btnMoveLight = ImageUtils.subimage(starx, 385, 69, 98, 23);
		btnAttackLight = ImageUtils.subimage(starx, 385, 92, 98, 23);
		btnStopLight = ImageUtils.subimage(starx, 385, 115, 98, 23);
	}
}
