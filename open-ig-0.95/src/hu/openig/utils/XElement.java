/*
 * Copyright 2008-2012, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A simplified XML element model.
 * @author akarnokd
 */
public class XElement implements Iterable<XElement> {
	/** The element name. */
	public final String name;
	/** The content of a simple node. */
	public String content;
	/** The parent element. */
	public XElement parent;
	/** The attribute map. */
	protected final Map<String, String> attributes = new LinkedHashMap<String, String>();
	/** The child elements. */
	protected final List<XElement> children = new ArrayList<XElement>();
	/**
	 * Gregorian calendar for XSD dateTime.
	 */
	private static final ThreadLocal<GregorianCalendar> XSD_CALENDAR = new ThreadLocal<GregorianCalendar>() {
		@Override
		protected GregorianCalendar initialValue() {
			return new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		}
	};
	/**
	 * Constructor. Sets the name.
	 * @param name the element name
	 */
	public XElement(String name) {
		this.name = name;
	}
	/**
	 * Retrieve an attribute or throw an IllegalArgumentException.
	 * @param attributeName the attribute name
	 * @return the attribute value or null if no such attribute
	 */
	public String get(String attributeName) {
		String s = attributes.get(attributeName);
		if (s == null) {
			throw new IllegalArgumentException(this + ": missing attribute: " + attributeName);
		}
		return s;
	}
	/**
	 * The element has the given attribute.
	 * @param attributeName the name
	 * @return attribute exists?
	 */
	public boolean has(String attributeName) {
		return attributes.containsKey(attributeName);
	}
	/**
	 * Retrieve an attribute.
	 * @param attributeName the attribute name
	 * @param def the default value if not present
	 * @return the attribute value or null if no such attribute
	 */
	public String get(String attributeName, String def) {
		String s = attributes.get(attributeName); 
		return s != null ? s : def;
	}
	/**
	 * Get an integer attribute or return the default value if not present.
	 * @param attributeName the attribute name
	 * @param def the default value if the attribute is not present
	 * @return the integer value
	 */
	public int getInt(String attributeName, int def) {
		String val = attributes.get(attributeName);
		return val != null ? Integer.parseInt(val) : def;
	}
	/**
	 * Get an integer attribute.
	 * @param attributeName the attribute name
	 * @return the integer value
	 */
	public long getLong(String attributeName) {
		String val = get(attributeName);
		return Long.parseLong(val);
	}
	/**
	 * Get an integer attribute or return the default value if not present.
	 * @param attributeName the attribute name
	 * @param def the default value if the attribute is not present
	 * @return the integer value
	 */
	public long getLong(String attributeName, long def) {
		String val = attributes.get(attributeName);
		return val != null ? Long.parseLong(val) : def;
	}
	/**
	 * Get an integer attribute.
	 * @param attributeName the attribute name
	 * @return the integer value
	 */
	public int getInt(String attributeName) {
		String val = get(attributeName);
		return Integer.parseInt(val);
	}
	/**
	 * Get an float attribute.
	 * @param attributeName the attribute name
	 * @return the float value
	 */
	public float getFloat(String attributeName) {
		String val = get(attributeName);
		return Float.parseFloat(val);
	}
	@Override
	public Iterator<XElement> iterator() {
		return children.iterator();
	}
	/**
	 * Returns an iterator which enumerates all children with the given name.
	 * @param name the name of the children to select
	 * @return the iterator
	 */
	public Iterable<XElement> childrenWithName(String name) {
		List<XElement> result = new ArrayList<XElement>(children.size() + 1);
		for (XElement e : children) {
			if (e.name.equals(name)) {
				result.add(e);
			}
		}
		return result;
	}
	/**
	 * Returns the content of the first child which has the given name.
	 * @param name the child name
	 * @return the content or null if no such child
	 */
	public String childValue(String name) {
		for (XElement e : children) {
			if (e.name.equals(name)) {
				return e.content;
			}
		}
		return null;
	}
	/**
	 * Returns the first child element with the given name.
	 * @param name the child name
	 * @return the XElement or null if not present
	 */
	public XElement childElement(String name) {
		for (XElement e : children) {
			if (e.name.equals(name)) {
				return e;
			}
		}
		return null;
	}
	/**
	 * Parse an XML document from the given input stream.
	 * Does not close the stream.
	 * @param in the input stream
	 * @return az XElement object
	 * @throws XMLStreamException on error
	 */
	public static XElement parseXML(InputStream in) throws XMLStreamException {
		XMLInputFactory inf = XMLInputFactory.newInstance();
		XMLStreamReader ir = inf.createXMLStreamReader(in);
		return parseXML(ir);
	}
	/**
	 * Parse an XML from an XML stream reader. Does not close the stream
	 * @param in the XMLStreamReader object
	 * @return az XElement object
	 * @throws XMLStreamException on error
	 */
	private static XElement parseXML(XMLStreamReader in) throws XMLStreamException {
		XElement node = null;
		XElement root = null;
		final StringBuilder emptyBuilder = new StringBuilder();
		StringBuilder b = null;
		Deque<StringBuilder> stack = new LinkedList<StringBuilder>();
		
		while (in.hasNext()) {
			int type = in.next();
			switch(type) {
			case XMLStreamConstants.START_ELEMENT:
				if (b != null) {
					stack.push(b);
					b = null;
				} else {
					stack.push(emptyBuilder);
				}
				XElement n = new XElement(in.getName().getLocalPart());
				n.parent = node;
				int attCount = in.getAttributeCount();
				if (attCount > 0) {
					for (int i = 0; i < attCount; i++) {
						n.attributes.put(in.getAttributeLocalName(i), in.getAttributeValue(i));
					}
				}
				if (node != null) {
					node.children.add(n);
				}
				node = n;
				if (root == null) {
					root = n;
				}
				break;
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.CHARACTERS:
				if (node != null && !in.isWhiteSpace()) {
					if (b == null) {
						b = new StringBuilder();
					}
					b.append(in.getText());
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				if (b != null) {
					node.content = b.toString();
				}
				if (node != null) {
					node = node.parent;
				}
				b = stack.pop();
				if (b == emptyBuilder) {
					b = null;
				}
				break;
			default:
				// ignore others.
			}
		}
		in.close();
		return root;
	}
	/**
	 * Parse an XML document from the given reader. Does not close the stream.
	 * @param in the InputStream object
	 * @return az XElement object
	 * @throws XMLStreamException on error
	 */
	public static XElement parseXML(Reader in) throws XMLStreamException {
		XMLInputFactory inf = XMLInputFactory.newInstance();
		XMLStreamReader ir = inf.createXMLStreamReader(in);
		return parseXML(ir);
	}
	/**
	 * Parse an XML from the given local filename.
	 * @param fileName the file name
	 * @return az XElement object
	 * @throws XMLStreamException on error
	 */
	public static XElement parseXML(String fileName) throws XMLStreamException {
		try {
			InputStream in = new FileInputStream(fileName);
			try {
				return parseXML(in);
			} finally {
				in.close();
			}
		} catch (IOException ex) {
			throw new XMLStreamException(ex);
		}
	}
	/**
	 * Converts all sensitive characters to its HTML entity equivalent.
	 * @param s the string to convert, can be null
	 * @return the converted string, or an empty string
	 */
	public static String sanitize(String s) {
		if (s != null) {
			StringBuilder b = new StringBuilder(s.length());
			for (int i = 0, count = s.length(); i < count; i++) {
				char c = s.charAt(i);
				switch (c) {
				case '<':
					b.append("&lt;");
					break;
				case '>':
					b.append("&gt;");
					break;
				case '\'':
					b.append("&#39;");
					break;
				case '"':
					b.append("&quot;");
					break;
				case '&':
					b.append("&amp;");
					break;
				default:
					b.append(c);
				}
			}
			return b.toString();
		}
		return "";
	}
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toStringRep("", new HashMap<String, String>(), b);
		return b.toString();
	}
	/**
	 * Convert the element into a pretty printed string representation.
	 * @param indent the current line indentation
	 * @param nss the namespace cache
	 * @param out the output
	 */
	void toStringRep(String indent, Map<String, String> nss, StringBuilder out) {
		out.append(indent).append("<");
		out.append(name);
		if (attributes.size() > 0) {
			for (String an : attributes.keySet()) {
				out.append(" ").append(an).append("='").append(sanitize(attributes.get(an))).append("'");
			}
		}
		
		if (children.size() == 0) {
			if (content == null) {
				out.append("/>");
			} else {
				out.append(">");
				out.append(sanitize(content));
				out.append("</");
				out.append(name);
				out.append(">");
			}
		} else {
			if (content == null) {
				out.append(String.format(">%n"));
			} else {
				out.append(">");
				out.append(sanitize(content));
				out.append(String.format("%n"));
			}
			for (XElement e : children) {
				e.toStringRep(indent + "  ", nss, out);
			}
			out.append(indent).append("</");
			out.append(name);
			out.append(">");
		}
		out.append(String.format("%n"));
	}
	/**
	 * Set an attribute value.
	 * @param name the attribute name
	 * @param value the content value, null will remove any existing
	 */
	public void set(String name, Object value) {
		if (value != null) {
			attributes.put(name, String.valueOf(value));
		} else {
			attributes.remove(name);
		}
		
	}
	/**
	 * Add a new child element with the given name.
	 * @param name the element name
	 * @return the created XElement
	 */
	public XElement add(String name) {
		XElement result = new XElement(name);
		children.add(result);
		return result;
	}
	/**
	 * Add the array of elements as children.
	 * @param elements the elements to add
	 */
	public void add(XElement... elements) {
		for (XElement e : elements) {
			children.add(e);
		}
	}
	/**
	 * Add the array of elements as children.
	 * @param element the element to add
	 */
	public void add(XElement element) {
		children.add(element);
	}
	/**
	 * Add the iterable of elements as children.
	 * @param elements the elements to add
	 */
	public void add(Iterable<XElement> elements) {
		for (XElement e : elements) {
			children.add(e);
		}
	}
	/** @return are there any children? */
	public boolean hasChildren() {
		return !children.isEmpty();
	}
	/**
	 * Save this XML into the given file.
	 * @param fileName the file name
	 * @throws IOException on error
	 */
	public void save(String fileName) throws IOException {
		save(new File(fileName));
	}
	/**
	 * Save this XML into the given file.
	 * @param file the file
	 * @throws IOException on error
	 */
	public void save(File file) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
		try {
			out.println("<?xml version='1.0' encoding='UTF-8'?>");
			out.print(toString());
		} finally {
			out.close();
		}
	}
	/**
	 * Remove attributes and children.
	 */
	public void clear() {
		attributes.clear();
		children.clear();
	}
	/**
	 * Detach this element from its parent.
	 */
	public void detach() {
		if (parent != null) {
			parent.children.remove(this);
			parent = null;
		}
	}
	/**
	 * Convert the given date to string.
	 * Always contains the milliseconds and timezone.
	 * @param date the date, not null
	 * @return the formatted date
	 */
	public static String formatDateTime(Date date) {
		StringBuilder b = new StringBuilder(24);
		
		GregorianCalendar cal = XSD_CALENDAR.get();
		cal.setTime(date);
		
		int value = 0;
		
		// Year-Month-Day
		value = cal.get(GregorianCalendar.YEAR);
		b.append(value);
		b.append('-');
		value = cal.get(GregorianCalendar.MONTH) + 1;
		if (value < 10) {
			b.append('0');
		}
		b.append(value);
		b.append('-');
		value = cal.get(GregorianCalendar.DATE);
		if (value < 10) {
			b.append('0');
		}
		b.append(value);
		
		b.append('T');
		// hour:minute:second:milliseconds
		value = cal.get(GregorianCalendar.HOUR_OF_DAY);
		if (value < 10) {
			b.append('0');
		}
		b.append(value);
		b.append(':');
		value = cal.get(GregorianCalendar.MINUTE);
		if (value < 10) {
			b.append('0');
		}
		b.append(value);
		b.append(':');
		value = cal.get(GregorianCalendar.SECOND);
		if (value < 10) {
			b.append('0');
		}
		b.append(value);
		b.append('.');
		
		value = cal.get(GregorianCalendar.MILLISECOND);
		// add leading zeros if needed
		if (value < 100) {
			b.append('0');
		}
		if (value < 10) {
			b.append('0');
		}
		b.append(value);
		
		value = cal.get(GregorianCalendar.DST_OFFSET) + cal.get(GregorianCalendar.ZONE_OFFSET);
		
		if (value == 0) {
			b.append('Z');
		} else {
			if (value < 0) {
				b.append('-');
				value = -value;
			} else {
				b.append('+');
			}
			int hour = value / 3600000;
			int minute = value / 60000 % 60;
			if (hour < 10) {
				b.append('0');
			}
			b.append(hour);
			b.append(':');
			if (minute < 10) {
				b.append('0');
			}
			b.append(minute);
		}
		
		
		return b.toString();
	}
	/**
	 * Parse an XSD dateTime.
	 * @param date the date string
	 * @return the date
	 * @throws ParseException format exception
	 */
	public static Date parseDateTime(String date) throws ParseException {
		GregorianCalendar cal = XSD_CALENDAR.get();
		cal.set(GregorianCalendar.MILLISECOND, 0);
		// format yyyy-MM-dd'T'HH:mm:ss[.sss][zzzzz] no milliseconds no timezone
		int offset = 0;
		try {
			offset = 0;
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(date.substring(offset, offset + 4)));
			offset = 5;
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(date.substring(offset, offset + 2)) - 1);
			offset = 8;
			cal.set(GregorianCalendar.DATE, Integer.parseInt(date.substring(offset, offset + 2)));
			offset = 11;
			cal.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(date.substring(offset, offset + 2)));
			offset = 14;
			cal.set(GregorianCalendar.MINUTE, Integer.parseInt(date.substring(offset, offset + 2)));
			offset = 17;
			cal.set(GregorianCalendar.SECOND, Integer.parseInt(date.substring(offset, offset + 2)));
			
			if (date.length() > 19) {
				offset = 19;
				char c = date.charAt(offset);
				// check milliseconds
				if (c == '.') {
					offset++;
					int endOffset = offset;
					// can be multiple
					while (endOffset < date.length() && Character.isDigit(date.charAt(endOffset))) {
						endOffset++;
					}
					int millisec = Integer.parseInt(date.substring(offset, endOffset));
					int len = endOffset - offset - 1;
					if (len >= 3) {
						while (len-- >= 3) {
							millisec /= 10;
						}
					} else {
						while (++len < 3) {
							millisec *= 10;
						}
					}
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					if (date.length() > endOffset) {
						offset = endOffset;
						c = date.charAt(offset);
					} else {
						c = '\0';
					}
				}
				if (c == 'Z') {
					cal.set(GregorianCalendar.ZONE_OFFSET, 0);
				} else
				if (c == '-' || c == '+') {
					int sign = c == '-' ? -1 : 1;
					offset++;
					int tzHour = Integer.parseInt(date.substring(offset, offset + 2));
					offset += 3;
					int tzMinute = Integer.parseInt(date.substring(offset, offset + 2));
					cal.set(GregorianCalendar.ZONE_OFFSET, sign * (tzHour * 3600000 + tzMinute * 60000));
				} else
				if (c != '\0') {
					throw new ParseException("Unknown milliseconds or timezone", offset);
				}
			}
		} catch (NumberFormatException ex) {
			throw new ParseException(ex.toString(), offset);
		} catch (IndexOutOfBoundsException ex) {
			throw new ParseException(ex.toString(), offset);
		}
		return cal.getTime();
	}
	/**
	 * Retrieve a boolean attribute value.
	 * <p>The attribute must exist.</p>
	 * @param name the attribute name
	 * @return true or false
	 */
	public boolean getBoolean(String name) {
		return "true".equals(get(name));
	}
	/**
	 * Retrieve a boolean attribute value.
	 * @param name the attribute name
	 * @param defaultValue the default value if the attribute doesn't exist
	 * @return true or false
	 */
	public boolean getBoolean(String name, boolean defaultValue) {
		String s = attributes.get(name);
		return s != null ? "true".equals(s) : defaultValue;
	}
	/** @return the attribute map. */
	public Map<String, String> attributes() {
		return attributes;
	}
	/**
	 * Retrieve a double attribute value.
	 * <p>Attribute must exist.</p>
	 * @param name the attribute name
	 * @return the value
	 */
	public double getDouble(String name) {
		return Double.parseDouble(get(name));
	}
	/**
	 * Retrieve a double attribute value.
	 * @param name the attribute name
	 * @param defaultValue the default value if the attribute doesn't exist
	 * @return the value
	 */
	public double getDouble(String name, double defaultValue) {
		String s = attributes.get(name);
		return s != null ? Double.parseDouble(s) : defaultValue;
	}
	/**
	 * Returns an instance of the given enumeration from the attribute.
	 * <p>The attribute should exist.</p>
	 * @param <T> the enum type
	 * @param name the attribute name
	 * @param clazz the attribute class
	 * @return the enumeration value
	 */
	public <T extends Enum<T>> T getEnum(String name, Class<T> clazz) {
		String s = get(name);
		T[] values = clazz.getEnumConstants();
		for (T t : values) {
			if (t.toString().equals(s)) {
				return t;
			}
		}
		throw new IllegalArgumentException(String.format("Attribute %s = %s is not a valid enum for %s", name, s, clazz.getName()));
	}
	/**
	 * Returns an instance of the given enumeration from the attribute.
	 * @param <T> the enum type
	 * @param name the attribute name
	 * @param clazz the attribute class
	 * @param defaultValue the default value if the attribute is missing or not supported
	 * @return the enumeration value
	 */
	public <T extends Enum<T>> T getEnum(String name, Class<T> clazz, T defaultValue) {
		String s = attributes.get(name);
		if (s != null) {
			T[] values = clazz.getEnumConstants();
			for (T t : values) {
				if (t.toString().equals(s)) {
					return t;
				}
			}
		}
		return defaultValue;
	}
	/**
	 * @return The child elements.
	 */
	public List<XElement> children() {
		return children;
	}
}
