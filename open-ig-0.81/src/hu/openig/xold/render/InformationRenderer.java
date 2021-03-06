/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */
package hu.openig.xold.render;

import hu.openig.xold.core.AllocationPreference;
import hu.openig.xold.core.Btn;
import hu.openig.xold.core.BtnAction;
import hu.openig.xold.core.InfoScreen;
import hu.openig.xold.core.LabInfo;
import hu.openig.xold.core.PopularityType;
import hu.openig.xold.core.StarmapSelection;
import hu.openig.xold.core.TaxRate;
import hu.openig.xold.model.GameBuildingPrototype;
import hu.openig.xold.model.GameFleet;
import hu.openig.xold.model.GamePlanet;
import hu.openig.xold.model.GamePlayer;
import hu.openig.xold.model.GameWorld;
import hu.openig.xold.model.PlanetStatus;
import hu.openig.xold.model.ResearchProgress;
import hu.openig.xold.model.ResearchTech;
import hu.openig.xold.model.GameBuildingPrototype.BuildingImages;
import hu.openig.xold.res.GameResourceManager;
import hu.openig.xold.res.gfx.CommonGFX;
import hu.openig.xold.res.gfx.InformationGFX;
import hu.openig.xold.res.gfx.TextGFX;
import hu.openig.sound.SoundFXPlayer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Planet surface renderer class.
 * @author karnokd, 2009.01.16.
 * @version $Revision 1.0$
 */
public class InformationRenderer extends JComponent implements MouseListener, MouseMotionListener, 
MouseWheelListener, ActionListener {
	/** Serial version. */
	private static final long serialVersionUID = 1638048442106816873L;
	/** The information graphics. */
	private final InformationGFX gfx;
	/** The common graphics. */
	private final CommonGFX cgfx;
	/** The last width. */
	private int lastWidth;
	/** The last height. */
	private int lastHeight;
	/** The text renderer. */
	private TextGFX text;
	/** The user interface sounds. */
	private SoundFXPlayer uiSound;
	/** Buttons which change state on click.*/
	private final List<Btn> toggleButtons = new ArrayList<Btn>();
	/** The various buttons. */
	private final List<Btn> releaseButtons = new ArrayList<Btn>();
	/** Buttons which fire on the press mouse action. */
	private final List<Btn> pressButtons = new ArrayList<Btn>();
	/** The fixed size of this renderer. */
	private final Dimension controlSize = new Dimension();
	/** The main information area. */
	private Rectangle mainArea = new Rectangle();
	/** The top right area for title text. */
	private Rectangle titleArea = new Rectangle();
	/** Secondary information area. */
	private Rectangle secondaryArea = new Rectangle();
	/** Starmap, building picture, alien picture area depending on the current information page. */
	private Rectangle pictureArea = new Rectangle();
	/** The currently showing screen. */
	private InfoScreen currentScreen;
	/** Colony info button. */
	private Btn btnColonyInfo;
	/** Military info button. */
	private Btn btnMilitaryInfo;
	/** Financial info button. */
	private Btn btnFinancialInfo;
	/** Buildings button. */
	private Btn btnBuildings;
	/** Planets button. */
	private Btn btnPlanets;
	/** Fleets button. */
	private Btn btnFleets;
	/** Inventions button. */
	private Btn btnInventions;
	/** Aliens button. */
	private Btn btnAliens;
	/** The first large button beneath the picture area. Its function depends on the current screen. */ 
	private Btn btnLarge1;
	/** The first large button rectangle. */
	private Rectangle btnLarge1Rect = new Rectangle();
	/** Image used for the first large button. */
	private BufferedImage btnLarge1Normal;
	/** Image used for the first large button down state. */
	private BufferedImage btnLarge1Down;
	/** The second large button beneath the picture area. Its function depends on the current screen. */ 
	private Btn btnLarge2;
	/** The second large button rectangle. */
	private Rectangle btnLarge2Rect = new Rectangle();
	/** The second large button normal image. */
	private BufferedImage btnLarge2Normal;
	/** The second large button down image. */
	private BufferedImage btnLarge2Down;
	/** Colony button. */
	private Btn btnColony;
	/** Starmap button. */
	private Btn btnStarmap;
	/** Equipment button. */
	private Btn btnEquipment;
	/** Research button. */
	private Btn btnResearch;
	/** Production button. */
	private Btn btnProduction;
	/** Diplomacy button. */
	private Btn btnDiplomacy;
	/** The screen rectangle. */
	private final Rectangle screen = new Rectangle();
	/** Action when the user clicks on the colony button. */
	private BtnAction onColonyClicked;
	/** Action when the user clicks on the starmap button. */
	private BtnAction onStarmapClicked;
	/** The game world. */
	private GameWorld gameWorld;
	/** The information bar renderer. */
	private InfobarRenderer infobarRenderer;
	/** Less tax. */
	private Btn btnTaxLess;
	/** More tax. */
	private Btn btnTaxMore;
	/** The last rendering position. */
	private final AchievementRenderer achievementRenderer;
	/** The problematic planet blinker timer. */
	private Timer blinkTimer;
	/** The steps of blink transition. */
	private static final int BLINK_STEPS = 1;
	/** The timer interval for blinking. */
	private static final int BLINK_INTERVAL = 1000;
	/** The current blink step. */
	private int blinkStep;
	/** Action on cancel information screen. */
	private BtnAction onCancelInfoscreen;
	/** Perform the action on double clicking on a building name. */
	private BtnAction onDblClickBuilding;
	/** Perform action on double clicking on a planet name. */
	private BtnAction onDblClickPlanet;
	/** The button for changing energy allocation strategy. */
	private final Rectangle energyAllocRect = new Rectangle();
	/** The button for changing worker allocation strategy. */
	private final Rectangle workerAllocRect = new Rectangle();
	/** If the user double clicks on a research. */
	private BtnAction onResearchDblClick;
	/** The research button clicked. */
	private BtnAction onResearchClick;
	/** The production button clicked. */
	private BtnAction onProductionClick;
	/**
	 * Constructor, expecting the planet graphics and the common graphics objects.
	 * @param grm the game resource manager
	 * @param uiSound the user interface sounds
	 * @param infobarRenderer the information bar renderer
	 * @param achievementRenderer the achievement renderer
	 */
	public InformationRenderer(GameResourceManager grm, 
			SoundFXPlayer uiSound, InfobarRenderer infobarRenderer,
			AchievementRenderer achievementRenderer) {
		this.gfx = grm.infoGFX;
		this.cgfx = grm.commonGFX;
		this.text = cgfx.text;
		this.uiSound = uiSound;
		this.infobarRenderer = infobarRenderer;
		this.achievementRenderer = achievementRenderer;
		
		controlSize.width = gfx.infoScreen.getWidth();
		controlSize.height = gfx.infoScreen.getHeight();
		blinkTimer = new Timer(BLINK_INTERVAL, new ActionListener() { @Override public void actionPerformed(ActionEvent e) { doBlink(); } });
		
		initButtons();
		
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addMouseListener(this);
//		setOpaque(true);
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		int w = getWidth();
		int h = getHeight();

		if (true) {
			Composite cp = g2.getComposite();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, w, h);
			g2.setComposite(cp);
		}
		if (w != lastWidth || h != lastHeight) {
			lastWidth = w;
			lastHeight = h;
			// if the render window changes, re-zoom to update scrollbars
			updateRegions();
		}
		// RENDER INFOBARS
		infobarRenderer.renderInfoBars(this, g2);
		
		g2.drawImage(gfx.infoScreen, screen.x, screen.y, null);
		
		renderButton(g2, btnColonyInfo, InfoScreen.COLONY_INFORMATION, gfx.btnColonyInfo, gfx.btnColonyInfoLight, gfx.btnColonyInfoLightDown);
		renderButton(g2, btnMilitaryInfo, InfoScreen.MILITARY_INFORMATION, gfx.btnMilitaryInfo, gfx.btnMilitaryInfoLight, gfx.btnMilitaryInfoLightDown);
		renderButton(g2, btnFinancialInfo, InfoScreen.FINANCIAL_INFORMATION, gfx.btnFinancialInfo, gfx.btnFinancialInfoLight, gfx.btnFinancialInfoLightDown);
		renderButton(g2, btnBuildings, InfoScreen.BUILDINGS, gfx.btnBuildings, gfx.btnBuildingsLight, gfx.btnBuildingsLightDown);
		
		renderButton(g2, btnPlanets, InfoScreen.PLANETS, gfx.btnPlanets, gfx.btnPlanetsLight, gfx.btnPlanetsLightDown);
		renderButton(g2, btnFleets, InfoScreen.FLEETS, gfx.btnFleets, gfx.btnFleetsLight, gfx.btnFleetsLightDown);
		renderButton(g2, btnInventions, InfoScreen.INVENTIONS, gfx.btnInventions, gfx.btnInventionsLight, gfx.btnInventionsLightDown);
		renderButton(g2, btnAliens, InfoScreen.ALIENS, gfx.btnAliens, gfx.btnAliensLight, gfx.btnAliensLightDown);
		
		if (btnLarge1 != null) {
			if (btnLarge1.down) {
				g2.drawImage(btnLarge1Down, btnLarge1Rect.x, btnLarge1Rect.y, null);
			} else {
				g2.drawImage(btnLarge1Normal, btnLarge1Rect.x, btnLarge1Rect.y, null);
			}
		} else {
			g2.drawImage(gfx.btnEmptyLarge, btnLarge1Rect.x, btnLarge1Rect.y, null);
		}
		if (btnLarge2 != null) {
			if (btnLarge2.down) {
				g2.drawImage(btnLarge2Down, btnLarge2Rect.x, btnLarge2Rect.y, null);
			} else {
				g2.drawImage(btnLarge2Normal, btnLarge2Rect.x, btnLarge2Rect.y, null);
			}
		} else {
			g2.drawImage(gfx.btnEmptyLarge, btnLarge2Rect.x, btnLarge2Rect.y, null);
		}
		
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.YELLOW, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.GREEN, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.GRAY, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.RED, "Sample text");
//		
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.DARK_GREEN, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.ORANGE, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.WHITE, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.CYAN, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.PURPLE, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.LIGHT_GREEN, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.BLUE, "Sample text");
//		text.paintTo(g2, mainArea.x, mainArea.y + (y++ * 16), size, TextGFX.LIGHT_BLUE, "Sample text");
		switch (currentScreen) {
		case PLANETS:
			renderPlanets(g2);
			break;
		case COLONY_INFORMATION:
			renderColonyInfo(g2);
			break;
		case FLEETS:
			renderFleets(g2);
			break;
		case BUILDINGS:
			renderBuildings(g2);
			break;
		case INVENTIONS:
			renderResearch(g2);
			break;
		default:
		}
		achievementRenderer.renderAchievements(g2, this);
	}
	/**
	 * Render the minimap with grids onto the picture area.
	 * @param g2 the graphics object
	 */
	private void renderMinimapBackground(Graphics2D g2) {
		// DRAW GRID
		Shape sp = g2.getClip();
		g2.setClip(pictureArea);
		int viewerRank = 4;
		g2.drawImage(cgfx.minimapInfo[viewerRank], pictureArea.x, pictureArea.y, null);
		g2.setColor(CommonGFX.GRID_COLOR);
		Stroke st = g2.getStroke();
		g2.setStroke(CommonGFX.GRID_STROKE);
		int y0 = 34;
		int x0 = 40;
		for (int i = 1; i < 5; i++) {
			g2.drawLine(pictureArea.x + x0, pictureArea.y, pictureArea.x + x0, pictureArea.y + pictureArea.height);
			g2.drawLine(pictureArea.x, pictureArea.y + y0, pictureArea.x + pictureArea.width, pictureArea.y + y0);
			x0 += 41;
			y0 += 34;
		}
		int i = 0;
		y0 = 28;
		x0 = 2;
		for (char c = 'A'; c < 'Z'; c++) {
			text.paintTo(g2, pictureArea.x + x0, pictureArea.y + y0, 5, TextGFX.GRAY, String.valueOf(c));
			x0 += 41;
			i++;
			if (i % 5 == 0) {
				x0 = 2;
				y0 += 34;
			}
		}
		
		g2.setStroke(st);
		g2.setClip(sp);
	}
	/**
	 * Renders a button based on its state.
	 * @param g2 the graphics object
	 * @param button the button to render
	 * @param screen the currently shown information screen
	 * @param normal the normal image
	 * @param light the highlighted image
	 * @param down the down image
	 */
	private void renderButton(Graphics2D g2, Btn button, InfoScreen screen, 
			BufferedImage normal, BufferedImage light, BufferedImage down) {
		if (button.visible) {
			if (button.down) {
				g2.drawImage(down, button.rect.x, button.rect.y, null);
			} else
			if (currentScreen == screen) {
				g2.drawImage(light, button.rect.x, button.rect.y, null);
			} else {
				g2.drawImage(normal, button.rect.x, button.rect.y, null);
			}
		} else {
			g2.drawImage(gfx.btnEmpty, button.rect.x, button.rect.y, null);
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dimension getPreferredSize() {
		return controlSize.getSize();
	}
	/** Initialize buttons. */
	private void initButtons() {
		btnColonyInfo = new Btn(new BtnAction() { @Override public void invoke() { doColonyInfoClick(); } });
		pressButtons.add(btnColonyInfo);
		btnMilitaryInfo = new Btn(new BtnAction() { @Override public void invoke() { doMilitaryInfoClick(); } });
		pressButtons.add(btnMilitaryInfo);
		btnFinancialInfo = new Btn(new BtnAction() { @Override public void invoke() { doFinancialInfoClick(); } });
		pressButtons.add(btnFinancialInfo);
		btnBuildings = new Btn(new BtnAction() { @Override public void invoke() { doBuildingsClick(); } });
		pressButtons.add(btnBuildings);
		btnPlanets = new Btn(new BtnAction() { @Override public void invoke() { doPlanetsClick(); } });
		pressButtons.add(btnPlanets);
		btnFleets = new Btn(new BtnAction() { @Override public void invoke() { doFleetsClick(); } });
		pressButtons.add(btnFleets);
		btnInventions = new Btn(new BtnAction() { @Override public void invoke() { doInventionsClick(); } });
		pressButtons.add(btnInventions);
		btnAliens = new Btn(new BtnAction() { @Override public void invoke() { doAliensClick(); } });
		pressButtons.add(btnAliens);
		btnColony = new Btn(new BtnAction() { @Override public void invoke() { doColonyClick(); } });
		releaseButtons.add(btnColony);
		btnStarmap = new Btn(new BtnAction() { @Override public void invoke() { doStarmapClick(); } });
		releaseButtons.add(btnStarmap);
		btnEquipment = new Btn(new BtnAction() { @Override public void invoke() { doEquipmentClick(); } });
		releaseButtons.add(btnEquipment);
		btnProduction = new Btn(new BtnAction() { @Override public void invoke() { doProductionClick(); } });
		releaseButtons.add(btnProduction);
		btnResearch = new Btn(new BtnAction() { @Override public void invoke() { doResearchClick(); } });
		releaseButtons.add(btnResearch);
		btnDiplomacy = new Btn(new BtnAction() { @Override public void invoke() { doDiplomacyClick(); } });
		releaseButtons.add(btnDiplomacy);
		
		btnTaxLess = new Btn(new BtnAction() { @Override public void invoke() { doLessTax(); } });
		pressButtons.add(btnTaxLess);
		btnTaxMore = new Btn(new BtnAction() { @Override public void invoke() { doMoreTax(); } });
		pressButtons.add(btnTaxMore);
		
	}
	/** Diplomacy click action. */
	protected void doDiplomacyClick() {
		uiSound.playSound("Diplomacy");
	}
	/** Research click action. */
	protected void doResearchClick() {
		if (onResearchClick != null) {
			onResearchClick.invoke();
		}
	}
	/** Research click action. */
	protected void doProductionClick() {
		if (onProductionClick != null) {
			onProductionClick.invoke();
		}
	}
	/** Equipment click action. */
	protected void doEquipmentClick() {
		uiSound.playSound("Equipment");
	}
	/** Starmap click action. */
	protected void doStarmapClick() {
		uiSound.playSound("Starmap");
		if (onStarmapClicked != null) {
			onStarmapClicked.invoke();
		}
	}
	/** Colony click action. */
	protected void doColonyClick() {
		uiSound.playSound("Colony");
		if (onColonyClicked != null) {
			onColonyClicked.invoke();
		}
	}
	/** Aliens click action. */
	protected void doAliensClick() {
		if (currentScreen != InfoScreen.ALIENS) {
			uiSound.playSound("AlienRaces");
			setScreenButtonsFor(InfoScreen.ALIENS);
			repaint();
		}
	}
	/** Inventions click action. */
	protected void doInventionsClick() {
		if (currentScreen != InfoScreen.INVENTIONS) {
			uiSound.playSound("Inventions");
			setScreenButtonsFor(InfoScreen.INVENTIONS);
			repaint();
		}
	}
	/** Fleets click action. */
	protected void doFleetsClick() {
		if (currentScreen != InfoScreen.FLEETS) {
			uiSound.playSound("Fleets");
			setScreenButtonsFor(InfoScreen.FLEETS);
			repaint();
		}
	}
	/** Planets click action. */
	protected void doPlanetsClick() {
		if (currentScreen != InfoScreen.PLANETS) {
			uiSound.playSound("Planets");
			setScreenButtonsFor(InfoScreen.PLANETS);
			repaint();
		}
	}
	/** Buildings click action. */
	protected void doBuildingsClick() {
		if (currentScreen != InfoScreen.BUILDINGS) {
			uiSound.playSound("Buildings");
			setScreenButtonsFor(InfoScreen.BUILDINGS);
			gameWorld.selectFirstPlanet();
			repaint();
		}
	}
	/** Financial info click action. */
	protected void doFinancialInfoClick() {
		if (currentScreen != InfoScreen.FINANCIAL_INFORMATION) {
			uiSound.playSound("FinancialInformation");
			setScreenButtonsFor(InfoScreen.FINANCIAL_INFORMATION);
			repaint();
		}
	}
	/** Military info click action. */
	protected void doMilitaryInfoClick() {
		if (currentScreen != InfoScreen.MILITARY_INFORMATION) {
			uiSound.playSound("MilitaryInformation");
			setScreenButtonsFor(InfoScreen.MILITARY_INFORMATION);
			repaint();
		}
	}
	/** Colony info click action. */
	protected void doColonyInfoClick() {
		if (currentScreen != InfoScreen.COLONY_INFORMATION) {
			uiSound.playSound("ColonyInformation");
			setScreenButtonsFor(InfoScreen.COLONY_INFORMATION);
			gameWorld.selectFirstPlanet();
			repaint();
		}
	}
	/**
	 * Update location of various interresting rectangles of objects.
	 */
	private void updateRegions() {
		
		infobarRenderer.updateRegions(this);
		
		screen.x = (getWidth() - gfx.infoScreen.getWidth()) / 2;
		screen.y = (getHeight() - gfx.infoScreen.getHeight()) / 2;
		screen.width = gfx.infoScreen.getWidth();
		screen.height = gfx.infoScreen.getHeight();
		mainArea.setBounds(screen.x + 2, screen.y + 2, 411, 362);
		
		titleArea.setBounds(screen.x + 415, screen.y + 2, 203, 26);
		
		secondaryArea.setBounds(screen.x + 415, screen.y + 30, 203, 179);
		
		pictureArea.setBounds(screen.x + 415, screen.y + 211, 203, 170);
		
		btnPlanets.rect.setBounds(screen.x + 1, screen.y + 364, 102, 28);
		btnColonyInfo.rect.setBounds(screen.x + 104, screen.y + 364, 102, 28);
		btnMilitaryInfo.rect.setBounds(screen.x + 207, screen.y + 364, 102, 28);
		btnFinancialInfo.rect.setBounds(screen.x + 310, screen.y + 364, 102, 28);
		
		btnFleets.rect.setBounds(screen.x + 1, screen.y + 392, 102, 28);
		btnBuildings.rect.setBounds(screen.x + 104, screen.y + 392, 102, 28);
		btnInventions.rect.setBounds(screen.x + 207, screen.y + 392, 102, 28);
		btnAliens.rect.setBounds(screen.x + 310, screen.y + 392, 102, 28);
		
		btnLarge1Rect.setBounds(screen.x + 413, screen.y + 381, 102, 39);
		btnLarge2Rect.setBounds(screen.x + 516, screen.y + 381, 102, 39);
		
		if (btnLarge1 != null) {
			btnLarge1.rect.setBounds(btnLarge1Rect);
		}
		if (btnLarge2 != null) {
			btnLarge2.rect.setBounds(btnLarge2Rect);
		}
		btnTaxLess.setBounds(screen.x + 250, screen.y + 260, gfx.btnTaxLess.getWidth(), gfx.btnTaxLess.getHeight());
		btnTaxMore.setBounds(screen.x + 260 + gfx.btnTaxLess.getWidth(), screen.y + 260, 
				gfx.btnTaxMore.getWidth(), gfx.btnTaxMore.getHeight());
		
		energyAllocRect.setBounds(btnTaxLess.rect.x, btnTaxLess.rect.y + btnTaxLess.rect.height + 7, 10 + btnTaxLess.rect.width + btnTaxMore.rect.width, 9);
		workerAllocRect.setBounds(btnTaxLess.rect.x, btnTaxLess.rect.y + btnTaxLess.rect.height + 17, 10 + btnTaxLess.rect.width + btnTaxMore.rect.width, 9);
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		Point pt = e.getPoint();
		if (e.getButton() == MouseEvent.BUTTON1) {
//			if (e.getClickCount() == 1) {
				for (Btn b : pressButtons) {
					if (b.test(pt)) {
						b.down = true;
						b.click();
						repaint(b.rect);
					}
				}
				for (Btn b : toggleButtons) {
					if (b.test(pt)) {
						b.down = !b.down;
						b.click();
						repaint(b.rect);
					}
				}
				for (Btn b : releaseButtons) {
					if (b.test(pt)) {
						b.down = true;
						repaint(b.rect);
					}
				}
//			}
		}
		if (currentScreen == InfoScreen.PLANETS) {
			doPlanetsMousePressed(e);
		} else
		if (currentScreen == InfoScreen.COLONY_INFORMATION) {
			doColonyMousePressed(e);
		} else
		if (currentScreen == InfoScreen.FLEETS) {
			doFleetsMousePressed(e);
		} else
		if (currentScreen == InfoScreen.BUILDINGS) {
			doBuildingPrototypeMousePressed(e);
		}
		if (currentScreen == InfoScreen.INVENTIONS) {
			doResearchMousePressed(e);
		}
	}
	/**
	 * Assign a large button to the first or second place and repaint the screen.
	 * @param index the index to assign, either 1 or 2
	 * @param button the button to assign, or null to remove assignment
	 * @param normal the normal image
	 * @param down the down image
	 */
	protected void selectButtonFor(int index, Btn button, BufferedImage normal, BufferedImage down) {
		if (index == 1) {
			if (btnLarge1 != null) {
				btnLarge1.visible = false;
			}
			if (button != null) {
				button.rect.setBounds(btnLarge1Rect);
				btnLarge1 = button;
				btnLarge1.visible = true;
				btnLarge1Normal = normal;
				btnLarge1Down = down;
				repaint(button.rect);
			} else {
				btnLarge1 = null;
			}
		} else {
			if (btnLarge2 != null) {
				btnLarge2.visible = false;
			}
			if (button != null) {
				button.rect.setBounds(btnLarge2Rect);
				btnLarge2 = button;
				btnLarge2.visible = true;
				btnLarge2Normal = normal;
				btnLarge2Down = down;
				repaint(button.rect);
			} else {
				btnLarge2 = null;
			}
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		Point pt = e.getPoint();
		boolean needRepaint = false;
		for (Btn b : releaseButtons) {
			needRepaint |= b.down;
			b.down = false;
			if (b.test(pt)) {
				b.click();
			}
		}
		for (Btn b : pressButtons) {
			needRepaint |= b.down;
			b.down = false;
		}
		if (!screen.contains(pt)) {
			if (onCancelInfoscreen != null) {
				onCancelInfoscreen.invoke();
			}
		}
		if (needRepaint) {
			repaint();
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
	}
	/**
	 * @param onColonyClicked the onColonyClicked to set
	 */
	public void setOnColonyClicked(BtnAction onColonyClicked) {
		this.onColonyClicked = onColonyClicked;
	}
	/**
	 * @return the onColonyClicked
	 */
	public BtnAction getOnColonyClicked() {
		return onColonyClicked;
	}
	/**
	 * @param onStarmapClicked the onStarmapClicked to set
	 */
	public void setOnStarmapClicked(BtnAction onStarmapClicked) {
		this.onStarmapClicked = onStarmapClicked;
	}
	/**
	 * @return the onStarmapClicked
	 */
	public BtnAction getOnStarmapClicked() {
		return onStarmapClicked;
	}
	/**
	 * Set the two large buttons for various information screen.
	 * @param screen the screen to use
	 */
	public void setScreenButtonsFor(InfoScreen screen) {
		if (screen != null) {
			switch (screen) {
			case COLONY_INFORMATION:
				selectButtonFor(1, btnColony, gfx.btnColonyLarge, gfx.btnColonyLargeDown);
				selectButtonFor(2, btnStarmap, gfx.btnStarmapLarge, gfx.btnStarmapLargeDown);
				break;
			case MILITARY_INFORMATION:
				selectButtonFor(1, btnColony, gfx.btnColonyLarge, gfx.btnColonyLargeDown);
				selectButtonFor(2, btnStarmap, gfx.btnStarmapLarge, gfx.btnStarmapLargeDown);
				break;
			case FINANCIAL_INFORMATION:
				selectButtonFor(1, btnColony, gfx.btnColonyLarge, gfx.btnColonyLargeDown);
				selectButtonFor(2, btnStarmap, gfx.btnStarmapLarge, gfx.btnStarmapLargeDown);
				break;
			case BUILDINGS:
				selectButtonFor(1, btnColony, gfx.btnColonyLarge, gfx.btnColonyLargeDown);
				selectButtonFor(2, btnStarmap, gfx.btnStarmapLarge, gfx.btnStarmapLargeDown);
				break;
			case PLANETS:
				selectButtonFor(1, btnColony, gfx.btnColonyLarge, gfx.btnColonyLargeDown);
				selectButtonFor(2, btnStarmap, gfx.btnStarmapLarge, gfx.btnStarmapLargeDown);
				break;
			case FLEETS:
				selectButtonFor(1, btnEquipment, gfx.btnEquipmentLarge, gfx.btnEquipmentLargeDown);
				selectButtonFor(2, btnStarmap, gfx.btnStarmapLarge, gfx.btnStarmapLargeDown);
				break;
			case INVENTIONS:
				selectButtonFor(1, btnProduction, gfx.btnProductionLarge, gfx.btnProductionLargeDown);
				selectButtonFor(2, btnResearch, gfx.btnResearchLarge, gfx.btnResearchLargeDown);
				break;
			case ALIENS:
				selectButtonFor(1, btnDiplomacy, gfx.btnDiplomacyLarge, gfx.btnDiplomacyLargeDown);
				selectButtonFor(2, null, null, null);
				break;
			default:
			}
		} else {
			selectButtonFor(1, null, null, null);
			selectButtonFor(2, null, null, null);
		}
		currentScreen = screen;
	}
	/**
	 * @param gameWorld the gameWorld to set
	 */
	public void setGameWorld(GameWorld gameWorld) {
		this.gameWorld = gameWorld;
	}
	/**
	 * @return the gameWorld
	 */
	public GameWorld getGameWorld() {
		return gameWorld;
	}
	/**
	 * Render planets information screen.
	 * @param g2 the graphics
	 */
	private void renderPlanets(Graphics2D g2) {
		Shape cs = g2.getClip();
		GamePlayer player = gameWorld.player;
		
		int columnWidth = (mainArea.width - 7) / 4;
		int x = 0;
		int y = 0;
		List<GamePlanet> planets = gameWorld.getKnownPlanetsByWithOwner();
		g2.setClip(7 + mainArea.x + x, mainArea.y + y, columnWidth, mainArea.height);
		for (GamePlanet p : planets) {
			if (y + 13 >= mainArea.height) {
				x += columnWidth;
				y = 0;
				g2.setClip(7 + mainArea.x + x, mainArea.y + y, columnWidth, mainArea.height);
			}
			int color = p.owner.race.color;
			if (p.owner == player && p.hasProblems()) {
				color = cgfx.mixColors(TextGFX.RED, color, 
						blinkStep <= BLINK_STEPS ? (1.0f * blinkStep / BLINK_STEPS) : ((2.0f * BLINK_STEPS - blinkStep) / BLINK_STEPS));
			}
			if (p.owner == player) {
				text.paintTo(g2, 9 + mainArea.x + x, mainArea.y + y + 6, 10, color, p.name);
			} else {
				if (p.planetListImage == null || p.planetListImageOwner != p.owner) {
					int len = text.getTextWidth(10, p.name);
					p.planetListImage = new BufferedImage(len, 10, BufferedImage.TYPE_INT_ARGB);
					Graphics2D tg = p.planetListImage.createGraphics();
					text.paintTo(tg, 0, 0, 10, color, p.name);
					tg.dispose();
				}
				g2.drawImage(p.planetListImage, 9 + mainArea.x + x, mainArea.y + y + 6, null);
			}
			if (p == player.selectedPlanet) {
				g2.setColor(player.getColor());
				g2.drawRect(mainArea.x + x + 7, mainArea.y + y + 4, columnWidth - 3, 13);
			}
			y += 13;
		}
		
		renderMinimapWithPlanetsAndFleets(g2, true, false, false);
		
		renderPlanetShortInfo(g2);
		
		g2.setClip(cs);
	}
	/**
	 * Renders the planet short information into the secondary area.
	 * @param g2 the graphics object
	 */
	private void renderPlanetShortInfo(Graphics2D g2) {
		// display details for the selected planet
		GamePlayer player = gameWorld.player;
		if (player.selectedPlanet != null) {
			GamePlanet planet = player.selectedPlanet;
			g2.setClip(titleArea);
			int h = (titleArea.height - 14) / 2;
			int color = TextGFX.GRAY;
			if (player.knownPlanetsByName.contains(planet) && planet.owner != null) {
				color = planet.owner.race.color;
			}
			text.paintTo(g2, titleArea.x + 2, titleArea.y + h, 14, color, planet.name);
			g2.setClip(secondaryArea);
			// display owner name
			if (planet.owner != null) {
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 10, 10, color, planet.owner.name);
				// if planet is own by the current player
				if (gameWorld.player == planet.owner) {
					// or planet has any satellite
					text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 30, 10, color, 
							gameWorld.getLabel("RaceNames." + planet.populationRace.id));
					text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 70, 10, color, 
							gameWorld.getLabel("PopulationStatusInfo",
							planet.population,
							gameWorld.getLabel("PopulatityName." + PopularityType.find(planet.popularity).id)));
					text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 90, 10, color, 
							gameWorld.getLabel("Taxation",
							gameWorld.getLabel("TaxRate." + planet.tax.id)));
					
					// display problematic areas

					List<String> problems = new ArrayList<String>();
					List<Integer> serious = new ArrayList<Integer>();
					PlanetStatus ps = planet.getStatus();
					int lsp = ps.livingSpace;
					if (lsp < planet.population) {
						problems.add(gameWorld.getLabel("ColonyInfo.LivingSpace"));
						serious.add(getColorForRelation(planet.population, lsp, 1.1f));
					}
					int hsp = ps.hospital;
					if (hsp < planet.population) {
						problems.add(gameWorld.getLabel("ColonyInfo.Hospital"));
						serious.add(getColorForRelation(planet.population, hsp, 1.1f));
					}
					int fsp = ps.food;
					if (fsp < planet.population) {
						problems.add(gameWorld.getLabel("ColonyInfo.Food"));
						serious.add(getColorForRelation(planet.population, fsp, 1.1f));
					}
					int esp = ps.energyDemand;
					int emp = ps.energyProduction;
					if (emp < esp) {
						problems.add(gameWorld.getLabel("ColonyInfo.Energy"));
						serious.add(getColorForRelation(esp, emp, 2f));
					}
					int wsp = ps.workerDemand;
					if (wsp > planet.population) {
						problems.add(gameWorld.getLabel("ColonyInfo.Worker"));
						serious.add(getColorForRelation(planet.population, lsp, 1.1f));
					}
					int x0 = secondaryArea.x + 6;
					int x = x0;
					int y = secondaryArea.y + 150;
					int seplen = text.getTextWidth(7, ", ");
					for (int i = 0; i < problems.size(); i++) {
						int len = text.getTextWidth(7, problems.get(i));
						if (x + seplen + len > secondaryArea.x + secondaryArea.width - 6) {
							y += 10;
							x = x0;
							text.paintTo(g2, x, y, 7, serious.get(i), problems.get(i));
							x += len;
						} else {
							if (i > 0) {
								text.paintTo(g2, x, y, 7, TextGFX.GREEN, ", ");
								x += seplen;
							}
							text.paintTo(g2, x, y, 7, serious.get(i), problems.get(i));
							x += len;
						}
						
					}
				} else {
					text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 70, 10, color, 
							gameWorld.getLabel("PopulationStatusInfo",
							planet.population,
							gameWorld.getLabel("Aliens")));
				}
			} else {
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 10, 10, color, gameWorld.getLabel("EmpireNames.Empty"));
			}
			text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 50, 10, color, 
					gameWorld.getLabel("NoRaceOnSurface", 
					gameWorld.getLabel("SurfaceTypeNames." + planet.surfaceType.planetXmlString)));
			StringBuilder b = new StringBuilder();
			for (String s : planet.inOrbit) {
				if (b.length() > 0) {
					b.append(", ");
				}
				b.append(s);
			}
			text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 120, 7, color, b.toString());
		}
	}
	/**
	 * Render the minimap with planets.
	 * @param g2 the graphics object
	 * @param renderPlanetSelection render the selection box around the selected planet?
	 * @param renderFleets render fleets icons?
	 * @param renderFleetSelection render fleet selection?
	 */
	private void renderMinimapWithPlanetsAndFleets(Graphics2D g2, 
			boolean renderPlanetSelection, boolean renderFleets, boolean renderFleetSelection) {
		GamePlayer player = gameWorld.player;
		renderMinimapBackground(g2);
		g2.setClip(pictureArea);
		// render known planet dots into the picture area
		double w2 = pictureArea.getWidth() * 1.0 / cgfx.fullMap.getWidth();
		double h2 = pictureArea.getHeight() * 1.0 / cgfx.fullMap.getHeight();
		for (GamePlanet p : player.knownPlanets) {
			int color = TextGFX.GRAY;
			if (player.knownPlanetsByName.contains(p) && p.owner != null) {
				color = p.owner.race.smallColor;
			}
			// draw the planet dot onto the minimap
			int x2 = (int)(p.x * w2);
			int y2 = (int)(p.y * h2);
			g2.setColor(new Color(color));
			g2.fillRect(pictureArea.x + x2 - 1, pictureArea.y + y2 - 1, 3, 3);
			if (renderPlanetSelection && player.selectedPlanet == p) {
				g2.setColor(Color.WHITE);
				g2.drawRect(pictureArea.x + x2 - 3, pictureArea.y + y2 - 3, 6, 6);
			}
		}
		if (renderFleets) {
			for (GameFleet f : player.knownFleets) {
				if (f.visible) {
					BufferedImage fleetImg = cgfx.shipImages[f.owner.fleetIcon];
					int x = (int)(f.x * w2 - fleetImg.getWidth() / 2.0);
					int y = (int)(f.y * h2 - fleetImg.getHeight() / 2.0);
					g2.drawImage(fleetImg, pictureArea.x + x, pictureArea.y + y, null);
					if (renderFleetSelection && player.selectedFleet == f) {
						g2.setColor(Color.WHITE);
						g2.drawRect(pictureArea.x + x - 1, pictureArea.y + y - 1,
								fleetImg.getWidth() + 2, fleetImg.getHeight() + 2);
					}					
				}
			}
		}
	}
	/**
	 * Returns the index within the planet listing of the given point.
	 * @param pt the point
	 * @return the index, or -1 if out of list
	 */
	private GamePlanet getPlanetForPositionList(Point pt) {
		int x = pt.x - mainArea.x - 7;
		int y = pt.y - mainArea.y - 6;
		int h = mainArea.height - 11;
		if (x >= 0 && y >= 0 && x < mainArea.width && y < h) {
			int columnWidth = (mainArea.width - 7) / 4;
			int numLen = h / 13;
			int col = x / columnWidth;
			int row = y / 13;
			int idx = col * numLen + row;
			List<GamePlanet> list = gameWorld.getKnownPlanetsByWithOwner();
			if (idx < list.size()) {
				return list.get(idx);
			}
		}
		return null;
	}
	/**
	 * Returns the planet for the minimap position pt.
	 * @param pt the point on minimap to check for planet
	 * @return the planet at the position or null for none
	 */
	private GamePlanet getPlanetForPositionMini(Point pt) {
		int x = pt.x - pictureArea.x;
		int y = pt.y - pictureArea.y;
		double w2 = pictureArea.getWidth() * 1.0 / cgfx.fullMap.getWidth();
		double h2 = pictureArea.getHeight() * 1.0 / cgfx.fullMap.getHeight();
		for (GamePlanet p : gameWorld.player.knownPlanets) {
			int x2 = (int)(p.x * w2);
			int y2 = (int)(p.y * h2);
			if (x >= x2 - 2 && x <= x2 + 2 && y >= y2 - 2 && y <= y2 + 2) {
				return p;
			}
		}
		return null;
	}
	/**
	 * Perform actions for mouse events in the planets tab and main area.
	 * @param e the mouse event
	 */
	private void doPlanetsMousePressed(MouseEvent e) {
		Point pt = e.getPoint();
		if (e.getButton() == MouseEvent.BUTTON1) {
			GamePlanet planet = null;
			if (mainArea.contains(pt)) {
				planet = getPlanetForPositionList(pt);
			} else
			if (pictureArea.contains(pt)) {
				planet = getPlanetForPositionMini(pt);
			}
			if (planet != null) {
				gameWorld.player.selectedPlanet = planet;
				gameWorld.player.selectionType = StarmapSelection.PLANET;
				repaint();
			}
			if (e.getClickCount() == 2) {
				if (onDblClickPlanet != null) {
					onDblClickPlanet.invoke();
				}
			}
		}
	}
	/** 
	 * Action for mouse events on colony info tab.
	 * @param e the mouse events
	 */
	private void doColonyMousePressed(MouseEvent e) {
		Point pt = e.getPoint();
		if (energyAllocRect.contains(pt)) {
			doEnergyAllocChange(e);
		} else
		if (workerAllocRect.contains(pt)) {
			doWorkerAllocChange(e);
		}
		if (e.getButton() == MouseEvent.BUTTON1) {
			GamePlanet planet = getPlanetForPositionMini(pt);
			if (planet != null) {
				gameWorld.player.selectedPlanet = planet;
				gameWorld.player.selectionType = StarmapSelection.PLANET;
				repaint();
			}
		}		
	}
	/**
	 * Change the worker allocation strategy back and forth based on the mouse button.
	 * @param e the mouse event
	 */
	private void doWorkerAllocChange(MouseEvent e) {
		GamePlanet p = gameWorld.player.selectedPlanet;
		if (p != null) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				p.workerAllocation = AllocationPreference.values()[(p.workerAllocation.ordinal() + 1) % AllocationPreference.values().length];
			} else {
				int i = p.workerAllocation.ordinal() - 1;
				if (i < 0) {
					i = AllocationPreference.values().length - 1;
				}
				p.workerAllocation = AllocationPreference.values()[i];
			}
			repaint();
		}
	}
	/**
	 * Change the energy allocation strategy back and forth based on the mouse button.
	 * @param e the mouse event
	 */
	private void doEnergyAllocChange(MouseEvent e) {
		GamePlanet p = gameWorld.player.selectedPlanet;
		if (p != null) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				p.energyAllocation = AllocationPreference.values()[(p.energyAllocation.ordinal() + 1) % AllocationPreference.values().length];
			} else {
				int i = p.energyAllocation.ordinal() - 1;
				if (i < 0) {
					i = AllocationPreference.values().length - 1;
				}
				p.energyAllocation = AllocationPreference.values()[i];
			}
			repaint();
		}
	}
	/**
	 * Renders the colony information.
	 * @param g2 the graphics object
	 */
	private void renderColonyInfo(Graphics2D g2) {
		Shape cs = g2.getClip();
		g2.setClip(mainArea);
		
		GamePlanet planet = gameWorld.player.selectedPlanet;
		if (planet != null) {
			text.paintTo(g2, mainArea.x + 10, mainArea.y + 8, 14, TextGFX.GREEN, planet.name);
			// if planet belongs to the player, display detailed info
			if (planet.owner == gameWorld.player) {
				
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 30, 10, TextGFX.GREEN,
					gameWorld.getLabel("ColonyInfoEntry",
						gameWorld.getLabel("ColonyInfo.Owner"),
						planet.owner.name
					));
				
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 50, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Race"),
							gameWorld.getLabel("RaceNames." + planet.populationRace.id)
						));
				
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 70, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Surface"),
							gameWorld.getLabel("SurfaceTypeNames." + planet.surfaceType.planetXmlString)
						));
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 90, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Population"),
							gameWorld.getLabel("PopulationStatus",
									planet.population,
									gameWorld.getLabel("PopulatityName." + PopularityType.find(planet.popularity).id), 
									planet.populationGrowth)
						));
				PlanetStatus ps = planet.getStatus();
				int color = getColorForRelation(planet.population, ps.livingSpace, 1.1f);
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 110, 10, color,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.LivingSpace"),
							planet.population + "/" + ps.livingSpace + " " + gameWorld.getLabel("ColonyInfo.Dweller")
						));

				color = getColorForRelation(ps.workerDemand, planet.population, 1.1f);
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 130, 10, color,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Worker"),
							planet.population + "/" + ps.workerDemand + " " + gameWorld.getLabel("ColonyInfo.Dweller")
						));

				color = getColorForRelation(planet.population, ps.hospital, 1.1f);
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 150, 10, color,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Hospital"),
							planet.population + "/" + ps.hospital + " " + gameWorld.getLabel("ColonyInfo.Dweller")
						));
				
				color = getColorForRelation(planet.population, ps.food, 1.1f);
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 170, 10, color,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Food"),
							planet.population + "/" + ps.food + " " + gameWorld.getLabel("ColonyInfo.Dweller")
						));
				
				color = getColorForRelation(ps.energyDemand, ps.energyProduction, 2f);
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 190, 10, color,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Energy"),
							ps.energyProduction + " " + gameWorld.getLabel("ColonyInfo.KWH")
							+ "   " + gameWorld.getLabel("ColonyInfo.Demand") + " : " + ps.energyDemand
							+ " " + gameWorld.getLabel("ColonyInfo.KWH")
						));
				
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 240, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfoEntry2",
							gameWorld.getLabel("ColonyInfo.TaxIncome"),
							planet.taxIncome
						));
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 260, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfoEntry2",
							gameWorld.getLabel("ColonyInfo.TradeIncome"),
							planet.tradeIncome
						));
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 280, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfoEntry2",
							gameWorld.getLabel("ColonyInfo.TaxMorale"),
							planet.taxMorale + "%"
						));
				// render tax buttons
				String tr = gameWorld.getLabel("Taxation",
						gameWorld.getLabel("TaxRate." + planet.tax.id));
				int trLen = text.getTextWidth(10, tr);
				int trx = btnTaxLess.rect.x + (btnTaxMore.rect.x + btnTaxMore.rect.width - btnTaxLess.rect.x - trLen) / 2;
				text.paintTo(g2, trx, mainArea.y + 240, 10, TextGFX.GREEN, tr);
				
				btnTaxLess.disabled = planet.tax == TaxRate.NONE;
				btnTaxMore.disabled = planet.tax == TaxRate.OPPRESSIVE;
				if (btnTaxLess.disabled) {
					g2.drawImage(gfx.btnTaxLess, btnTaxLess.rect.x, btnTaxLess.rect.y, null);
					paintDisablePattern(g2, btnTaxLess.rect);
				} else {
					if (btnTaxLess.down) {
						g2.drawImage(gfx.btnTaxLessDown, btnTaxLess.rect.x, btnTaxLess.rect.y, null);
					} else {
						g2.drawImage(gfx.btnTaxLess, btnTaxLess.rect.x, btnTaxLess.rect.y, null);
					}
				}
				if (btnTaxMore.disabled) {
					g2.drawImage(gfx.btnTaxMore, btnTaxMore.rect.x, btnTaxMore.rect.y, null);
					paintDisablePattern(g2, btnTaxMore.rect);
				} else {
					if (btnTaxMore.down) {
						g2.drawImage(gfx.btnTaxMoreDown, btnTaxMore.rect.x, btnTaxMore.rect.y, null);
					} else {
						g2.drawImage(gfx.btnTaxMore, btnTaxMore.rect.x, btnTaxMore.rect.y, null);
					}
				}
				
				// allocation preference settings
				String s = gameWorld.getLabel("ColonyInfo.AllocationPreference.EnergyAlloc") + ":" + gameWorld.getLabel("ColonyInfo.AllocationPreference." + planet.energyAllocation.id);
				int len = text.getTextWidth(7, s);
				g2.setColor(Color.LIGHT_GRAY);
//				g2.fill(energyAllocRect);
				text.paintTo(g2, energyAllocRect.x + energyAllocRect.width - len - 1, energyAllocRect.y + 1, 7, TextGFX.YELLOW, s);
//				g2.fill(workerAllocRect);
				
				s = gameWorld.getLabel("ColonyInfo.AllocationPreference.WorkerAlloc") + ":" + gameWorld.getLabel("ColonyInfo.AllocationPreference." + planet.workerAllocation.id);
				len = text.getTextWidth(7, s);
				text.paintTo(g2, workerAllocRect.x + workerAllocRect.width - len - 1, workerAllocRect.y + 1, 7, TextGFX.YELLOW, s);
				
			} else {
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 30, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfoEntry",
							gameWorld.getLabel("ColonyInfo.Owner"),
							planet.owner != null ? planet.owner.name : gameWorld.getLabel("EmpireNames.Empty")
						));
					
					text.paintTo(g2, mainArea.x + 10, mainArea.y + 50, 10, TextGFX.GREEN,
							gameWorld.getLabel("ColonyInfoEntry",
									gameWorld.getLabel("ColonyInfo.Race"),
								(planet.populationRace != null && gameWorld.player.knownPlanetsByName.contains(planet)
									? gameWorld.getLabel("RaceNames." + planet.populationRace.id)
									: "?")
							));
					
					text.paintTo(g2, mainArea.x + 10, mainArea.y + 70, 10, TextGFX.GREEN,
							gameWorld.getLabel("ColonyInfoEntry",
								gameWorld.getLabel("ColonyInfo.Surface"),
								gameWorld.getLabel("SurfaceTypeNames." + planet.surfaceType.planetXmlString)
							));
					text.paintTo(g2, mainArea.x + 10, mainArea.y + 90, 10, TextGFX.GREEN,
							gameWorld.getLabel("ColonyInfoEntry",
								gameWorld.getLabel("ColonyInfo.Population"),
								(planet.populationRace != null && gameWorld.player.knownPlanetsByName.contains(planet)
								? planet.population + " " + gameWorld.getLabel("Aliens")
								: "?")
							));
				
			}
			if (planet.owner == gameWorld.player || gameWorld.player.knownPlanetsByName.contains(planet)) {
				text.paintTo(g2, mainArea.x + 10, mainArea.y + 310, 10, TextGFX.GREEN,
						gameWorld.getLabel("ColonyInfo.Deployed"));
				StringBuilder b = new StringBuilder();
				for (String s : planet.inOrbit) {
					if (b.length() > 0) {
						b.append(", ");
					}
					b.append(s);
				}
				text.paintTo(g2, mainArea.x + 20, mainArea.y + 330, 7, TextGFX.GREEN, b.toString());
			}
		} else {
			text.paintTo(g2, mainArea.x + 10, mainArea.y + 30, 10, TextGFX.YELLOW,
					gameWorld.getLabel("ColonyInfo.NoColonySelected"));
		}
		renderPlanetShortInfo(g2);
		renderMinimapWithPlanetsAndFleets(g2, true, false, false);
		g2.setClip(cs);
	}
	/**
	 * Paint the disablingPattern onto the given rectangle area.
	 * @param g2 the graphics object to paint to
	 * @param rect the rectangle to fill
	 */
	private void paintDisablePattern(Graphics2D g2, Rectangle rect) {
		Paint p = g2.getPaint();
		g2.setPaint(new TexturePaint(cgfx.disablingPattern, 
				new Rectangle(rect.x + 1, rect.y + 1, 
						cgfx.disablingPattern.getWidth(), cgfx.disablingPattern.getHeight())));
		g2.fill(new Rectangle(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2));
		g2.setPaint(p);
	}
	/**
	 * Returns GREEN, if value < limit, YELLOW if value < limit * percent, RED otherwise.
	 * @param value the value
	 * @param limit the limit
	 * @param percent the limit percent to switch to yellow
	 * @return the color
	 */
	static int getColorForRelation(int value, int limit, float percent) {
		if (value <= limit) {
			return TextGFX.GREEN;
		} else
		if (value <= limit * percent) {
			return TextGFX.YELLOW;
		}
		return TextGFX.RED;
	}
	/** Set tax level to a lesser level. */
	private void doLessTax() {
		GamePlanet planet = gameWorld.player.selectedPlanet;
		if (planet != null && planet.tax != TaxRate.NONE) {
			planet.tax = TaxRate.values()[planet.tax.ordinal() - 1];
			repaint();
		}
	}
	/** Set tax level to a greater level. */
	private void doMoreTax() {
		GamePlanet planet = gameWorld.player.selectedPlanet;
		if (planet != null && planet.tax != TaxRate.OPPRESSIVE) {
			planet.tax = TaxRate.values()[planet.tax.ordinal() + 1];
			repaint();
		}
	}
	/**
	 * Returns the index within the fleet listing of the given point.
	 * @param pt the point
	 * @return the fleet or null if not in the list
	 */
	private GameFleet getFleetForPositionList(Point pt) {
		int x = pt.x - mainArea.x - 7;
		int y = pt.y - mainArea.y - 6;
		if (x >= 0 && y >= 0 && x < mainArea.width && y < mainArea.height) {
			int columnWidth = (mainArea.width - 7) / 4;
			int numLen = mainArea.height / 13;
			int col = x / columnWidth;
			int row = y / 13;
			int idx = col * numLen + row;
			List<GameFleet> list = gameWorld.getKnownFleets();
			if (idx < list.size()) {
				return list.get(idx);
			}
		}
		return null;
	}
	/**
	 * Returns the planet for the minimap position pt.
	 * @param pt the point on minimap to check for planet
	 * @return the planet at the position or null for none
	 */
	private GameFleet getFleetForPositionMini(Point pt) {
		int x = pt.x - pictureArea.x;
		int y = pt.y - pictureArea.y;
		double w2 = pictureArea.getWidth() * 1.0 / cgfx.fullMap.getWidth();
		double h2 = pictureArea.getHeight() * 1.0 / cgfx.fullMap.getHeight();
		for (GameFleet f : gameWorld.player.knownFleets) {
			int x2 = (int)(f.x * w2);
			int y2 = (int)(f.y * h2);
			BufferedImage fimg = cgfx.shipImages[f.owner.fleetIcon];
			int w = fimg.getWidth() / 2;
			int h = fimg.getHeight() / 2;
			if (x >= x2 - w && x <= x2 + w && y >= y2 - h && y <= y2 + h) {
				return f;
			}
		}
		return null;
	}
	/**
	 * Perform actions for mouse events in the fleets tab and main area.
	 * @param e the mouse event
	 */
	private void doFleetsMousePressed(MouseEvent e) {
		Point pt = e.getPoint();
		if (e.getButton() == MouseEvent.BUTTON1) {
			GameFleet fleet = null;
			if (mainArea.contains(pt)) {
				fleet = getFleetForPositionList(pt);
			} else
			if (pictureArea.contains(pt)) {
				fleet = getFleetForPositionMini(pt);
			}
			if (fleet != null) {
				gameWorld.player.selectedFleet = fleet;
				gameWorld.player.selectionType = StarmapSelection.FLEET;
				repaint();
			}
		}
	}
	/**
	 * Renders the planet short information into the secondary area.
	 * @param g2 the graphics object
	 */
	private void renderFleetShortInfo(Graphics2D g2) {
		// display details for the selected planet
		GamePlayer player = gameWorld.player;
		if (player.selectedFleet != null) {
			GameFleet fleet = player.selectedFleet;
			g2.setClip(titleArea);
			int h = (titleArea.height - 14) / 2;
			text.paintTo(g2, titleArea.x + 2, titleArea.y + h, 14, TextGFX.RED, fleet.name);
			g2.setClip(secondaryArea);
			// display owner name
			text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 10, 10, fleet.owner.race.color, fleet.owner.name);
			// if fleet is own by the current player
			if (gameWorld.player == fleet.owner) {
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 30, 10, TextGFX.GREEN, 
						gameWorld.getLabel("FleetStatus." + fleet.status.id));
				
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 50, 10, TextGFX.GREEN, 
						gameWorld.getLabel("FleetStatistics.PlanetNearBy"));
				
				text.paintTo(g2, secondaryArea.x + 20, secondaryArea.y + 70, 10, TextGFX.GREEN, 
						"----"); // TODO find nearby planet
				
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 100, 7, TextGFX.GREEN, 
						gameWorld.getLabel("FleetStatistics.Entry",
							gameWorld.getLabel("FleetStatistics.Speed"),
							fleet.getSpeed()
					));

				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 110, 7, TextGFX.GREEN, 
						gameWorld.getLabel("FleetStatistics.Entry",
							gameWorld.getLabel("FleetStatistics.Firepower"),
							fleet.getFirepower()
					));

				int battleships = fleet.getBattleshipCount();
				int destroyers = fleet.getDestroyerCount();
				int fighters = fleet.getFighterCount();
				int tanks = fleet.getTankCount();
				
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 130, 7, TextGFX.GREEN, 
					gameWorld.getLabel("FleetStatistics.Entry",
						gameWorld.getLabel("FleetStatistics.Battleships"),
						battleships > 0 ? String.valueOf(battleships) : "-"
				));
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 140, 7, TextGFX.GREEN, 
						gameWorld.getLabel("FleetStatistics.Entry",
							gameWorld.getLabel("FleetStatistics.Destroyers"),
							battleships > 0 ? String.valueOf(destroyers) : "-"
					));
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 150, 7, TextGFX.GREEN, 
						gameWorld.getLabel("FleetStatistics.Entry",
							gameWorld.getLabel("FleetStatistics.Fighters"),
							battleships > 0 ? String.valueOf(fighters) : "-"
					));
				text.paintTo(g2, secondaryArea.x + 6, secondaryArea.y + 160, 7, TextGFX.GREEN, 
						gameWorld.getLabel("FleetStatistics.Entry",
							gameWorld.getLabel("FleetStatistics.Tanks"),
							battleships > 0 ? String.valueOf(tanks) : "-"
					));
			}
		}
	}
	/**
	 * Render fleet list panel.
	 * @param g2 the graphics object.
	 */
	private void renderFleets(Graphics2D g2) {
		Shape cs = g2.getClip();
		GamePlayer player = gameWorld.player;
		
		int columnWidth = (mainArea.width - 7) / 4;
		int x = 0;
		int y = 0;
		List<GameFleet> fleets = gameWorld.getKnownFleets();
		g2.setClip(9 + mainArea.x + x, mainArea.y + y, columnWidth - 5, mainArea.height);
		for (GameFleet f : fleets) {
			if (y + 13 >= mainArea.height) {
				x += columnWidth;
				y = 0;
				g2.setClip(9 + mainArea.x + x, mainArea.y + y, columnWidth - 5, mainArea.height);
			}
			int color = f.owner.race.color;
			text.paintTo(g2, 9 + mainArea.x + x, mainArea.y + y + 6, 10, color, f.name);
			if (f == player.selectedFleet) {
				g2.setColor(new Color(TextGFX.ORANGE));
				Shape c1 = g2.getClip();
				// widen the clip area for the box-around
				g2.setClip(7 + mainArea.x + x, mainArea.y + y, columnWidth, mainArea.height);
				g2.drawRect(mainArea.x + x + 7, mainArea.y + y + 4, columnWidth - 3, 13);
				g2.setClip(c1);
			}
			y += 13;
		}
		
		renderFleetShortInfo(g2);
		renderMinimapWithPlanetsAndFleets(g2, false, true, true);
		g2.setClip(cs);
	}
	/**
	 * Start all animation timers.
	 */
	public void startAnimations() {
		blinkTimer.start();
	}
	/** Stop all animation timers. */
	public void stopAnimations() {
		blinkTimer.stop();
	}
	/** Perform the blink operations. */
	public void doBlink() {
		blinkStep++;
		if (blinkStep >= BLINK_STEPS * 2) {
			blinkStep = 0;
		}
//		if (currentScreen == InfoScreen.PLANETS) {
// FIXME		repaint(mainArea);
//		}
	}
	/**
	 * Returns the index within the fleet listing of the given point.
	 * @param pt the point
	 * @return the fleet or null if not in the list
	 */
	private GameBuildingPrototype getBuildingPrototypeForPositionList(Point pt) {
		int x = pt.x - mainArea.x - 7;
		int y = pt.y - mainArea.y - 6;
		int h = mainArea.height - 50;
		if (x >= 0 && y >= 0 && x < mainArea.width && y < h) {
			int columnWidth = (mainArea.width - 7) / 2;
			int numLen = h / 13;
			int col = x / columnWidth;
			int row = y / 13;
			int idx = col * numLen + row;
			String techId;
			if (gameWorld.player.selectedPlanet != null 
					&& gameWorld.player.selectedPlanet.populationRace != null) {
				techId = gameWorld.player.selectedPlanet.populationRace.techId; 
			} else {
				techId = gameWorld.player.race.techId;
			}
			
			List<GameBuildingPrototype> list = gameWorld.getTechIdBuildingPrototypes(techId);
			if (idx < list.size()) {
				return list.get(idx);
			}
		}
		return null;
	}
	/**
	 * Perform actions for mouse events in the fleets tab and main area.
	 * @param e the mouse event
	 */
	private void doBuildingPrototypeMousePressed(MouseEvent e) {
		Point pt = e.getPoint();
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (mainArea.contains(pt)) {
				GameBuildingPrototype bp = getBuildingPrototypeForPositionList(pt);
				if (bp != null) {
					gameWorld.player.selectedBuildingPrototype = bp;
					if (bp.researchTech != null) {
						gameWorld.player.selectedTech = bp.researchTech;
					}
					// on double click go to the planet surface and into build mode
					if (e.getClickCount() == 2) {
						if (onDblClickBuilding != null) {
							onDblClickBuilding.invoke();
						}
					}
				}
				repaint();
			}
		}
	}
	/**
	 * Render fleet list panel.
	 * @param g2 the graphics object.
	 */
	private void renderBuildings(Graphics2D g2) {
		Shape cs = g2.getClip();
		GamePlayer player = gameWorld.player;
		GamePlanet planet = player.selectedPlanet;
		if (planet != null) {
			
			int columnWidth = (mainArea.width - 7) / 2;
			int x = 0;
			int y = 0;
			// use technology id of the current planet's population race or the players race
			String techId;
			if (player.selectedPlanet != null && player.selectedPlanet.populationRace != null) {
				techId = player.selectedPlanet.populationRace.techId; 
			} else {
				techId = player.race.techId;
			}
			
			List<GameBuildingPrototype> list = gameWorld.getTechIdBuildingPrototypes(techId);
			g2.setClip(9 + mainArea.x + x, mainArea.y + y, columnWidth - 5, mainArea.height);
			for (GameBuildingPrototype bp : list) {
				if (y + 52 >= mainArea.height) {
					x += columnWidth;
					y = 0;
					g2.setClip(9 + mainArea.x + x, mainArea.y + y, columnWidth - 5, mainArea.height);
				}
				int curr = planet.getCountOfBuilding(bp);
				int tot = player.getTotalCountOfBuildings(bp);
				
				int color = TextGFX.YELLOW;
				if (!gameWorld.isBuildableOnPlanet(bp)) {
					color = TextGFX.GRAY;
				}
				text.paintTo(g2, 9 + mainArea.x + x, mainArea.y + y + 7, 7, TextGFX.GREEN, curr + "/" + tot);
				text.paintTo(g2, 39 + mainArea.x + x, mainArea.y + y + 6, 10, color, bp.name);
				if (bp == player.selectedBuildingPrototype) {
					g2.setColor(new Color(TextGFX.ORANGE));
					Shape c1 = g2.getClip();
					// widen the clip area for the box-around
					g2.setClip(7 + mainArea.x + x, mainArea.y + y, columnWidth, mainArea.height);
					g2.drawRect(mainArea.x + x + 37, mainArea.y + y + 4, columnWidth - 31, 13);
					g2.setClip(c1);
				}
				y += 13;
			}
			// render thumbnail of the selected building
			GameBuildingPrototype bp = player.selectedBuildingPrototype;
			if (bp != null) {
				g2.setClip(pictureArea);
				BuildingImages bimg = bp.images.get(techId);
				if (bimg != null) {
					BufferedImage img = bimg.thumbnail; 
					g2.drawImage(img, pictureArea.x + (pictureArea.width - img.getWidth()) / 2, 
							pictureArea.y + (pictureArea.height - img.getHeight()) / 2, null);
				}
				// render building name
				g2.setClip(titleArea);
				int len = text.getTextWidth(10, bp.name);
				text.paintTo(g2, titleArea.x + (titleArea.width - len) / 2, 
						titleArea.y + (titleArea.height - 10) / 2, 10, TextGFX.RED, 
						bp.name);
				// render the building description
				g2.setClip(mainArea);
				text.paintTo(g2, mainArea.x + (mainArea.width - len) / 2, 
						mainArea.y + mainArea.height - 43, 10, TextGFX.RED, 
						bp.name);
				y = mainArea.y + mainArea.height - 30;
				for (String s : bp.description) {
					if (s != null) {
						text.paintTo(g2, mainArea.x + 6, y, 7, TextGFX.GREEN, s);
					}
					y += 10;
				}
				g2.setClip(secondaryArea);
				// render building properties
				int color = TextGFX.GREEN;
				int h = secondaryArea.y + 6;
				text.paintTo(g2, secondaryArea.x + 6, h, 10, color, 
					gameWorld.getLabel("BuildingInfo.Entry",
						gameWorld.getLabel("BuildingInfo.Cost"),
						bp.cost + " " + gameWorld.getLabel("BuildingInfo.ProductionUnitFor.credit")
					)
				);
				text.paintTo(g2, secondaryArea.x + 6, h + 17, 10, color, 
						gameWorld.getLabel("BuildingInfo.Entry",
							gameWorld.getLabel("BuildingInfo.Energy"),
							bp.energy + " "
							+ gameWorld.getLabel("BuildingInfo.ProductionUnitFor.energy")
						)
					);
				text.paintTo(g2, secondaryArea.x + 6, h + 34, 10, color, 
						gameWorld.getLabel("BuildingInfo.Entry",
							gameWorld.getLabel("BuildingInfo.Worker"),
							bp.workers
						)
					);
				int i = 0;
				h += 51;
				List<Map.Entry<String, ?>> plist = new LinkedList<Map.Entry<String, ?>>();
				plist.addAll(bp.values.entrySet());
				plist.addAll(bp.properties.entrySet());
				for (Map.Entry<String, ?> e : plist) {
					if (GameBuildingPrototype.PRODUCT_TYPES.contains(e.getKey())) {
						String value = e.getValue() + " " 
						+ gameWorld.getLabel("BuildingInfo.ProductionUnitFor." + e.getKey());
						text.paintTo(g2, secondaryArea.x + 6, h, 10, color, 
								gameWorld.getLabel("BuildingInfo.Entry",
									(i == 0 ? gameWorld.getLabel("BuildingInfo.Production") : ""),
									value
								)
							);
						h += 17;
						i++;
					}
				}
			}
			g2.setClip(secondaryArea);
			int color = TextGFX.GRAY;
			int h = secondaryArea.y + secondaryArea.height - 68;
			text.paintTo(g2, secondaryArea.x + 6, h, 10, color, planet.name);
			// display owner name
			if (planet.owner != null) {
				text.paintTo(g2, secondaryArea.x + 6, h + 17, 10, color, planet.owner.name);
				// if planet is own by the current player
				if (gameWorld.player == planet.owner) {
					// or planet has any satellite
					text.paintTo(g2, secondaryArea.x + 6, h + 34, 10, color, 
							gameWorld.getLabel("RaceNames." + planet.populationRace.id));
				}
			} else {
				text.paintTo(g2, secondaryArea.x + 6, h + 17, 10, color, gameWorld.getLabel("EmpireNames.Empty"));
			}
			text.paintTo(g2, secondaryArea.x + 6, h + 51, 10, color, 
					gameWorld.getLabel("NoRaceOnSurface", 
					gameWorld.getLabel("SurfaceTypeNames." + planet.surfaceType.planetXmlString)));
			g2.setClip(cs);
		} else {
			text.paintTo(g2, mainArea.x + 10, mainArea.y + 30, 10, TextGFX.YELLOW,
					gameWorld.getLabel("ColonyInfo.NoColonySelected"));
		}
	}
	/**
	 * @param onCancelInfoscreen the onCancelInfoscreen to set
	 */
	public void setOnCancelInfoscreen(BtnAction onCancelInfoscreen) {
		this.onCancelInfoscreen = onCancelInfoscreen;
	}
	/**
	 * @return the onCancelInfoscreen
	 */
	public BtnAction getOnCancelInfoscreen() {
		return onCancelInfoscreen;
	}
	/**
	 * @param onDblClickBuilding the onDblClickBuilding to set
	 */
	public void setOnDblClickBuilding(BtnAction onDblClickBuilding) {
		this.onDblClickBuilding = onDblClickBuilding;
	}
	/**
	 * @return the onDblClickBuilding
	 */
	public BtnAction getOnDblClickBuilding() {
		return onDblClickBuilding;
	}
	/**
	 * @param onDblClickPlanet the onDblClickPlanet to set
	 */
	public void setOnDblClickPlanet(BtnAction onDblClickPlanet) {
		this.onDblClickPlanet = onDblClickPlanet;
	}
	/**
	 * @return the onDblClickPlanet
	 */
	public BtnAction getOnDblClickPlanet() {
		return onDblClickPlanet;
	}
	/**
	 * Renders the research tab.
	 * @param g2 the graphics object
	 */
	private void renderResearch(Graphics2D g2) {
		List<List<List<ResearchTech>>> rtl = gameWorld.getPlayerResearchList();
		int columnWidth = (mainArea.width) / 4;
		Shape sp = g2.getClip();
		g2.setClip(mainArea);
		Color hseparator = new Color(80, 80, 80);
		Color vseparator = new Color(76, 108, 180);
		for (int i = 0; i < rtl.size(); i++) {
			int x = mainArea.x + i * columnWidth;
			int y = mainArea.y;
			if (i > 0) {
				g2.setColor(vseparator);
				g2.drawLine(x, mainArea.y, x, mainArea.y + mainArea.height - 60);
			}
			List<List<ResearchTech>> clazz = rtl.get(i);
			for (int j = 0; j < clazz.size(); j++) {
				if (j > 0) {
					g2.setColor(hseparator);
					int x2 = i < 3 ? x + columnWidth - 1 : mainArea.x + mainArea.width - 1;
					g2.drawLine(x, y + 2, x2, y + 2);
				}
				List<ResearchTech> type = clazz.get(j);
				for (ResearchTech rt : type) {
					int color = gameWorld.isAvailable(rt) 
					? TextGFX.ORANGE : (gameWorld.isResearchable(rt) 
							? (gameWorld.isActiveResearch(rt) ? TextGFX.YELLOW : TextGFX.GREEN) : TextGFX.GRAY);
					text.paintTo(g2, x + 4, y + 5, 7, color, rt.name);
					if (rt == gameWorld.player.selectedTech) {
						g2.setColor(Color.LIGHT_GRAY);
						g2.drawRect(x + 2 , y + 2, columnWidth - 4, 11);
					}
					y += 12;
				}
			}
		}
		g2.setColor(vseparator);
		g2.drawLine(mainArea.x, mainArea.y + mainArea.height - 59, mainArea.x + mainArea.width - 1, mainArea.y + mainArea.height - 59);
		// render selected technology descriptions
		ResearchTech rt = gameWorld.player.selectedTech;
		if (rt != null) {
			boolean researchable = gameWorld.isResearchable(rt);
			boolean researched = gameWorld.player.availableTechnology.contains(rt);
			String n = rt.description[0];
			int l = text.getTextWidth(10, n);
			text.paintTo(g2, mainArea.x + (mainArea.width - l) / 2, mainArea.y + mainArea.height - 56, 10, TextGFX.RED, n);
			if (researched || researchable) {
				String desc = rt.description[1] + " " + rt.description[2];
				String[] words = desc.split("\\s+");
				int y = mainArea.y + mainArea.height - 41;
				int x = mainArea.x + 3;
				int sl = text.getTextWidth(7, " ");
				for (String w : words) {
					int len = text.getTextWidth(7, w);
					if (x + len > mainArea.x + mainArea.width - 4) {
						y += 12;
						x = mainArea.x + 3;
					}
					text.paintTo(g2, x, y, 7, TextGFX.GREEN, w);
					x += len + sl;
				}
			}
			l = text.getTextWidth(14, rt.name);
			g2.setClip(titleArea);
			text.paintTo(g2, titleArea.x + (titleArea.width - l) / 2, titleArea.y + (titleArea.height - 14) / 2, 14, TextGFX.RED, rt.name);

			g2.setClip(secondaryArea);
			if (researched) {
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 4, 10, TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Status.Researched"));
				if (!"Buildings".equals(rt.clazz)) {
					text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 24, 10, TextGFX.GREEN, 
							gameWorld.getLabel("ResearchInfo.Entry",
								gameWorld.getLabel("ResearchInfo.Inventory"), 
								gameWorld.getInventoryCount(rt)
							));
				}
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 44, 10, TextGFX.GREEN, 
						gameWorld.getLabel("ResearchInfo.Entry",
							gameWorld.getLabel("ResearchInfo.Cost"), rt.buildCost
						));
				g2.setClip(pictureArea);
				if (rt.infoImage != null) {
					g2.drawImage(rt.infoImage, pictureArea.x, pictureArea.y, null);
				} else {
					int len = text.getTextWidth(14, "?");
					text.paintTo(g2, pictureArea.x + (pictureArea.width - len) / 2, pictureArea.y + (pictureArea.height - 14) / 2, 14, TextGFX.RED, "?");
				}
			} else 
			if (researchable) {
				boolean ar = gameWorld.isActiveResearch(rt);
				if (ar) {
					text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 4, 10, 
							TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Status.Researching"));
				} else {
					text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 4, 10, 
							TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Status.Researchable"));
				}
				ResearchProgress rp = gameWorld.player.researchProgresses.get(rt);
				String percent = "0%";
				int cost = 0;
				if (rp != null) {
					if (rp.research.maxCost > 0) {
						percent = Integer.toString((rp.research.maxCost - rp.moneyRemaining) * 100 / rp.research.maxCost) + "%"; 
					} else {
						percent = "-";
					}
					cost = rp.allocatedRemainingMoney;
				}
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 24, 10, TextGFX.GREEN, 
						gameWorld.getLabel("ResearchInfo.Researchable.Entry",
							gameWorld.getLabel("ResearchInfo.Researchable.Complete"), 
							percent
						));
				int time = 0;
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 44, 10, TextGFX.GREEN, 
						gameWorld.getLabel("ResearchInfo.Researchable.Entry",
							gameWorld.getLabel("ResearchInfo.Researchable.RemainingTime"), 
							time
						));
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 64, 10, TextGFX.GREEN, 
						gameWorld.getLabel("ResearchInfo.Researchable.Entry",
							gameWorld.getLabel("ResearchInfo.Researchable.Money"), 
							cost + "/" + rt.maxCost
						));
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 84, 10, TextGFX.GREEN, 
						gameWorld.getLabel("ResearchInfo.Researchable.Entry2",
						gameWorld.getLabel("ResearchInfo.Researchable.Knowledge"))
				);
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 104, 10, TextGFX.GREEN,
						gameWorld.getLabel("ResearchInfo.Researchable.Entry2",
						gameWorld.getLabel("ResearchInfo.Researchable.Lab")));

				LabInfo li = gameWorld.player.getLabInfo();
				int color = rt.civil <= li.currentCivil ? TextGFX.GREEN : TextGFX.RED;
				text.paintTo(g2, secondaryArea.x + 120, secondaryArea.y + 82, 14, color, 
						Integer.toString(rt.civil));
				color = rt.mechanic <= li.currentMechanic ? TextGFX.GREEN : TextGFX.RED;
				text.paintTo(g2, secondaryArea.x + 135, secondaryArea.y + 82, 14, color, 
						Integer.toString(rt.mechanic));
				color = rt.computer <= li.currentComputer ? TextGFX.GREEN : TextGFX.RED;
				text.paintTo(g2, secondaryArea.x + 150, secondaryArea.y + 82, 14, color, 
						Integer.toString(rt.computer));
				color = rt.ai <= li.currentAi ? TextGFX.GREEN : TextGFX.RED;
				text.paintTo(g2, secondaryArea.x + 165, secondaryArea.y + 82, 14, color, 
						Integer.toString(rt.ai));
				color = rt.military <= li.currentMilitary ? TextGFX.GREEN : TextGFX.RED;
				text.paintTo(g2, secondaryArea.x + 180, secondaryArea.y + 82, 14, color, 
						Integer.toString(rt.military));
				//------------------------------------------------------------------------
				color = li.currentCivil < li.totalCivil ? TextGFX.YELLOW : TextGFX.GREEN;
				text.paintTo(g2, secondaryArea.x + 120, secondaryArea.y + 102, 14, color, 
						Integer.toString(li.currentCivil));
				
				color = li.currentMechanic < li.totalMechanic ? TextGFX.YELLOW : TextGFX.GREEN;
				text.paintTo(g2, secondaryArea.x + 135, secondaryArea.y + 102, 14, color, 
						Integer.toString(li.currentMechanic));
				
				color = li.currentComputer < li.totalComputer ? TextGFX.YELLOW : TextGFX.GREEN;
				text.paintTo(g2, secondaryArea.x + 150, secondaryArea.y + 102, 14, color, 
						Integer.toString(li.currentComputer));
				
				color = li.currentAi < li.totalAi ? TextGFX.YELLOW : TextGFX.GREEN;
				text.paintTo(g2, secondaryArea.x + 165, secondaryArea.y + 102, 14, color, 
						Integer.toString(li.currentAi));
				
				color = li.currentMilitary < li.totalMilitary ? TextGFX.YELLOW : TextGFX.GREEN;
				text.paintTo(g2, secondaryArea.x + 180, secondaryArea.y + 102, 14, color, 
						Integer.toString(li.currentMilitary));
				//------------------------------------------------------------------------
				text.paintTo(g2, secondaryArea.x + 123, secondaryArea.y + 122, 7, TextGFX.GREEN, 
						Integer.toString(li.totalCivil));
				text.paintTo(g2, secondaryArea.x + 138, secondaryArea.y + 122, 7, TextGFX.GREEN, 
						Integer.toString(li.totalMechanic));
				text.paintTo(g2, secondaryArea.x + 153, secondaryArea.y + 122, 7, TextGFX.GREEN, 
						Integer.toString(li.totalComputer));
				text.paintTo(g2, secondaryArea.x + 168, secondaryArea.y + 122, 7, TextGFX.GREEN, 
						Integer.toString(li.totalAi));
				text.paintTo(g2, secondaryArea.x + 183, secondaryArea.y + 122, 7, TextGFX.GREEN, 
						Integer.toString(li.totalMilitary));
				
				
				g2.setClip(pictureArea);
				if (rt.wiredInfoImage != null) {
					g2.drawImage(rt.wiredInfoImage, pictureArea.x, pictureArea.y, null);
				} else {
					int len = text.getTextWidth(14, "?");
					text.paintTo(g2, pictureArea.x + (pictureArea.width - len) / 2, pictureArea.y + (pictureArea.height - 14) / 2, 14, TextGFX.RED, "?");
				}
			} else {
				text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 4, 10, TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Status.NotResearchable"));
			}
			g2.setClip(secondaryArea);
			text.paintTo(g2, secondaryArea.x + 4, secondaryArea.y + 135, 7, TextGFX.GREEN, gameWorld.getLabel("ResearchInfo.Requires"));
			int y = secondaryArea.y + 145;
			int x = secondaryArea.x + 15;
			for (ResearchTech rrt : rt.requires) {
				int color = gameWorld.player.availableTechnology.contains(rrt) 
				? TextGFX.ORANGE : (gameWorld.isResearchable(rrt) 
						? (gameWorld.isActiveResearch(rrt) ? TextGFX.YELLOW : TextGFX.GREEN) : TextGFX.GRAY);
				text.paintTo(g2, x, y, 7, color, rrt.name);
				y += 10;
			}
		}
		
		g2.setClip(sp);
	}
	/**
	 * Perform actions for mouse events in the research tab and main area.
	 * @param e the mouse event
	 */
	private void doResearchMousePressed(MouseEvent e) {
		Point pt = e.getPoint();
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (mainArea.contains(pt)) {
				ResearchTech rt = getResearchForPosition(pt);
				if (rt != null) {
					gameWorld.player.selectedTech = rt;
					// select the building prototype for this tech if any
					gameWorld.selectBuildingFor(gameWorld.player.selectedTech);
					// on double click jump to either the research or to the production screen
					if (e.getClickCount() == 2) {
						if (onResearchDblClick != null) {
							onResearchDblClick.invoke();
						}
					}
				}
				repaint();
			}
		}
	}
	/**
	 * Returns the research for the given mouse coordinate.
	 * @param pt the point
	 * @return the research or null if there is nothing listed at the point
	 */
	private ResearchTech getResearchForPosition(Point pt) {
		int columnWidth = (mainArea.width) / 4;
		int rowHeight = 12;
		int col = (pt.x - mainArea.x) / columnWidth;
		int row = (pt.y - mainArea.y - 2) / rowHeight;
		List<List<List<ResearchTech>>> rtl = gameWorld.getPlayerResearchList();
		if (col < rtl.size()) {
			List<List<ResearchTech>> clazz = rtl.get(col);
			int cnt = 0;
			for (int i = 0; i < clazz.size(); i++) {
				List<ResearchTech> type = clazz.get(i);
				for (int j = 0; j < type.size(); j++) {
					if (cnt == row) {
						return type.get(j);
					}
					cnt++;
				}
			}
		}
		return null;
	}
	/**
	 * @param onResearchDblClick the onResearchDblClick to set
	 */
	public void setOnResearchDblClick(BtnAction onResearchDblClick) {
		this.onResearchDblClick = onResearchDblClick;
	}
	/**
	 * @return the onResearchDblClick
	 */
	public BtnAction getOnResearchDblClick() {
		return onResearchDblClick;
	}
	/**
	 * @param onResearchClick the onResearchClick to set
	 */
	public void setOnResearchClick(BtnAction onResearchClick) {
		this.onResearchClick = onResearchClick;
	}
	/**
	 * @return the onResearchClick
	 */
	public BtnAction getOnResearchClick() {
		return onResearchClick;
	}
	/**
	 * @param onProductionClick the onProductionClick to set
	 */
	public void setOnProductionClick(BtnAction onProductionClick) {
		this.onProductionClick = onProductionClick;
	}
	/**
	 * @return the onProductionClick
	 */
	public BtnAction getOnProductionClick() {
		return onProductionClick;
	}
}
