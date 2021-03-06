/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.core;


import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

/**
 * Utility class to load graphics into annotated fields.
 * @author akarnokd, 2009.11.09.
 */
public final class GFXLoader {
	/** Constructor. */
	private GFXLoader() {
		// utility class
	}
	/**
	 * Load resources of the given annotated target object.
	 * @param target the target object
	 * @param rl the resource locator
	 * @param language the language
	 */
	public static void loadResources(Object target, ResourceLocator rl, String language) {
		try {
			for (Field f : target.getClass().getFields()) {
				Img ia = f.getAnnotation(Img.class);
				if (ia != null) {
					f.set(target, rl.getImage(language, ia.name()));
				} else {
					Btn2 ib2 = f.getAnnotation(Btn2.class);
					if (ib2 != null) {
						f.set(target, getButton2(rl, language, ib2.name()));
					} else {
						Btn3 ib3 = f.getAnnotation(Btn3.class);
						if (ib3 != null) {
							f.set(target, getButton3(rl, language, ib3.name()));
						} else {
							Cat ic = f.getAnnotation(Cat.class);
							if (ic != null) {
								f.set(target, getCategory(rl, language, ic.name()));
							} else {
								Anim ian = f.getAnnotation(Anim.class);
								if (ian != null) {
									f.set(target, rl.getAnimation(language, ian.name(), ian.width(), ian.step()));
								} else {
									Btn3H ib3h = f.getAnnotation(Btn3H.class);
									if (ib3h !=  null) {
										f.set(target, getButton3H(rl, language, ib3h.name()));
									}
								}
							}
						}
					}
				}
			}
			if (target instanceof ResourceSelfLoader) {
				((ResourceSelfLoader)target).load(rl, language);
			}
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		}
	}
	/**
	 * Get a two phase button image (with and without _pressed).
	 * @param rl the resource locator
	 * @param language the target language
	 * @param name the button name
	 * @return the array cointaining the normal and the pressed state
	 */
	private static BufferedImage[] getButton2(ResourceLocator rl, String language, String name) {
		return new BufferedImage[] {
			rl.getImage(language, name),
			rl.getImage(language, name + "_pressed")
		};
	}
	/**
	 * Get a two phase button image (with and without _selected).
	 * @param rl the resource locator
	 * @param language the target language
	 * @param name the button name
	 * @return the array cointaining the normal and the pressed state
	 */
	private static BufferedImage[] getCategory(ResourceLocator rl, String language, String name) {
		return new BufferedImage[] {
			rl.getImage(language, name),
			rl.getImage(language, name + "_selected")
		};
	}
	/**
	 * Get a three phase button image (normal, _selected_pressed, _selected).
	 * @param rl the resource locator
	 * @param language the target language
	 * @param name the button name
	 * @return the array cointaining the normal, selected and pressed state
	 */
	private static BufferedImage[] getButton3(ResourceLocator rl, String language, String name) {
		return new BufferedImage[] {
			rl.getImage(language, name),
			rl.getImage(language, name + "_selected"),
			rl.getImage(language, name + "_selected_pressed")
		};
	}
	/**
	 * Get a three phase button image (normal, _pressed, _hovered).
	 * @param rl the resource locator
	 * @param language the target language
	 * @param name the button name
	 * @return the array cointaining the normal, pressed and hovered state
	 */
	private static BufferedImage[] getButton3H(ResourceLocator rl, String language, String name) {
		return new BufferedImage[] {
			rl.getImage(language, name),
			rl.getImage(language, name + "_pressed"),
			rl.getImage(language, name + "_hovered")
		};
	}
}
