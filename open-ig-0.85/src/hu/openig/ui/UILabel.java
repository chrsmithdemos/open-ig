/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.ui;

import hu.openig.render.TextRenderer;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

/**
 * A static label with option to set color, text, alignment
 * content and word wrapping.
 * @author akarnokd, 2011.03.04.
 */
public class UILabel extends UIComponent {
	/** The text renderer. */
	private TextRenderer tr;
	/** The content text. */
	private String text;
	/** The font size. */
	private int size;
	/** The row spacing. */
	private int spacing = 2;
	/** The ARGB color to use when the label is disabled. */
	private int disabledColor = TextRenderer.GRAY;
	/** The ARGB color to use when drawing the label. */
	private int textColor = TextRenderer.GREEN;
	/** 
	 * The shadow ARGB color to use underneath the label text. Set it to zero
	 * to disable shadowing.
	 */
	private int shadowColor;
	/** Is the text wrapped. */
	private boolean wrap;
	/** The horizontal alignment. */
	private HorizontalAlignment align = HorizontalAlignment.LEFT;
	/** The vertical alignment. */
	private VerticalAlignment valign = VerticalAlignment.MIDDLE;
	/**
	 * Construct a non wrapping label with the given text size.
	 * The label's dimensions are adjusted to the text width and height.
	 * @param text the initial label text
	 * @param size the font size
	 * @param tr the text renderer
	 */
	public UILabel(String text, int size, TextRenderer tr) {
		this.tr = tr;
		this.text = text;
		this.size = size;
		width = tr.getTextWidth(size, text);
		height = size;
	}
	/**
	 * Construct a label width the given size boundaries. The
	 * text is automatically wrapped to the width.
	 * You need to set the <code>height</code> of this label explicitely
	 * to a value or use the <code>getWrappedHeight()</code> value.
	 * @param text the initial text of the label
	 * @param size the font size
	 * @param width the width of the label
	 * @param tr the text renderer component
	 */
	public UILabel(String text, int size, int width, TextRenderer tr) {
		this.tr = tr;
		this.text = text;
		this.size = size;
		this.width = width;
		this.wrap = true;
	}
	@Override
	public void draw(Graphics2D g2) {
		Shape clip = g2.getClip();
		g2.setClip(new Rectangle(0, 0, width, height));
		if (wrap) {
			List<String> lines = new ArrayList<String>();
			if (shadowColor == 0) {
				tr.wrapText(text, width, size, lines);
			} else {
				tr.wrapText(text, width - 1, size, lines);
			}
			int totalHeight = lines.size() * (size + spacing) - (lines.size() > 1 ? spacing : 0);
			int py = 0;
			switch (valign) {
			case BOTTOM:
				py = height - totalHeight;
				break;
			case MIDDLE:
				py = (height - totalHeight) / 2;
				break;
			default:
			}
			int row = 0;
			for (String line : lines) {
				row++;
				drawAligned(g2, py, line);
				py += size + spacing;
			}
		} else {
			int totalHeight = size;
			int py = 0;
			switch (valign) {
			case BOTTOM:
				py = height - totalHeight;
				break;
			case MIDDLE:
				py = (height - totalHeight) / 2;
				break;
			default:
			}
			drawAligned(g2, py, text);
		}
		g2.setClip(clip);
	}
	/**
	 * Draw the text with alignment.
	 * @param g2 the graphics context
	 * @param py the top position to start drawing
	 * @param line a text line
	 */
	void drawAligned(Graphics2D g2, int py, String line) {
		if (align != HorizontalAlignment.JUSTIFY) {
			int px = 0;
			switch (align) {
			case LEFT:
				break;
			case CENTER:
				px = (width - tr.getTextWidth(size, line)) / 2;
				break;
			case RIGHT:
				px = width - tr.getTextWidth(size, line);
				break;
			default:
			}
			if (shadowColor != 0) {
				tr.paintTo(g2, px + 1, py + 1, size, shadowColor, line);
			}
			int c = 0;
			if (enabled || disabledColor == 0) {
				c = textColor;
			} else {
				c = disabledColor;
			}
			tr.paintTo(g2, px, py, size, c, line);
		} else {
			if (line.length() > 0) {
				String[] words = line.split("\\s+");
				int perword = 0;
				for (String s : words) {
					perword += tr.getTextWidth(size, s);
				}
				float space = 1.0f * (width - perword) / words.length;
				float px = 0;
				if (shadowColor != 0) {
					for (int i = 0; i < words.length; i++) {
						tr.paintTo(g2, (int)px + 1, py + 1, size, shadowColor, words[i]);
						px += tr.getTextWidth(size, words[i]) + space;
					}
				}
				int c = 0;
				if (enabled || disabledColor == 0) {
					c = textColor;
				} else {
					c = disabledColor;
				}
				px = 0;
				for (int i = 0; i < words.length; i++) {
					tr.paintTo(g2, (int)px, py, size, c, words[i]);
					px += tr.getTextWidth(size, words[i]) + space;
				}
			}
		}
	}
	/**
	 * Compute the height in pixels when the text content
	 * is wrapped by the current component width.
	 * Use this to compute and set the height of a multi-line label.
	 * @return the wrapped case height
	 */
	public int getWrappedHeight() {
		List<String> lines = new ArrayList<String>();
		if (shadowColor == 0) {
			tr.wrapText(text, width, size, lines);
		} else {
			tr.wrapText(text, width - 1, size, lines);
		}
		return lines.size() * size + 1;
	}
	/**
	 * Set the label text.
	 * @param text the text to set
	 * @return this
	 */
	public UILabel text(String text) {
		this.text = text;
//		if (!wrap) {
//			width = tr.getTextWidth(size, text);
//		}
		return this;
	}
	/**
	 * Set the label color.
	 * @param textColor the text color to set
	 * @return this
	 */
	public UILabel color(int textColor) {
		this.textColor = textColor;
		return this;
	}
	/**
	 * Set the shadow color. Use 0 to disable the shadow.
	 * @param shadowColor the shadow color
	 * @return this
	 */
	public UILabel shadow(int shadowColor) {
		this.shadowColor = shadowColor;
		return this;
	}
	/**
	 * Set the horizontal alignment.
	 * @param a the horizontal alignment constant
	 * @return this
	 */
	public UILabel horizontally(HorizontalAlignment a) {
		this.align = a;
		return this;
	}
	/**
	 * Set the vertical alignment.
	 * @param a the vertical alignment constant
	 * @return this
	 */
	public UILabel vertically(VerticalAlignment a) {
		this.valign = a;
		return this;
	}
	/**
	 * Set the font size in pixels.
	 * @param h the font size
	 * @return this
	 */
	public UILabel size(int h) {
		this.size = h;
//		if (!wrap) {
//			width = tr.getTextWidth(size, text);
//		}
		return this;
	}
	/**
	 * @return the unwrapped text width.
	 */
	public int getTextWidth() {
		return tr.getTextWidth(size, text) + (shadowColor != 0 ? 1 : 0);
	}
	/**
	 * Set the wrapping mode.
	 * @param state the state
	 * @return wrap
	 */
	public UILabel wrap(boolean state) {
		this.wrap = state;
		return this;
	}
	/**
	 * Set the row spacing for multiline display.
	 * @param value the spacing in pixels
	 * @return this
	 */
	public UILabel spacing(int value) {
		this.spacing = value;
		return this;
	}
}
