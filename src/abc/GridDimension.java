package abc;

import java.util.Vector;

/**
 * Has behaviour similar to an iterator, but with getCurrent() and reset() methods
 * @author woodhams
 *
 */

public class GridDimension {
	private String fieldName;
	private String shortName; 
	private Vector<String> values;
	private int currentPointer;
	
	public GridDimension(String name, String heading, int start, int end, int incr) {
		if (incr==0) throw new IllegalArgumentException("Increment can't be zero");
		fieldName = name;
		shortName = heading;
		currentPointer=0;
		int n=(end-start)/incr+1;
		values = new Vector<String>(n);
		// "for (int x=start; x<=end; x+=incr)" doesn't work with incr<0.
		int x=start;
		for (int i=0; i<n; i++) {
			values.add(Integer.toString(x));
			x+=incr;
		}
	}
	
	public GridDimension(String name, String heading, int start, int end) {
		this(name,heading,start,end,1);
	}
	
	public GridDimension(String name, String heading, double start, double end, double incr) {
		if (incr==0) throw new IllegalArgumentException("Increment can't be zero");
		fieldName = name;
		shortName = heading;
		currentPointer=0;
		// How many decimal places do we need?
		int dp = mdwUtils.Doubles.numDecimalPlaces(start);
		dp = Math.max(dp, mdwUtils.Doubles.numDecimalPlaces(end));
		dp = Math.max(dp, mdwUtils.Doubles.numDecimalPlaces(incr));
		String format = String.format("%%.%df", dp); // e.g. "%.3f" if dp=3
		/*
		 *  How many values in list? NOTE: if numbers are not exactly representable,
		 *  rounding could cause problems here, e.g. new GridIterator("foo",0.0,1.0,1.0/3) 
		 *  may not produce 4 numbers. 
		 */
		int n=(int)((end-start)/incr+1);
		values = new Vector<String>(n);
		// "for (int x=start; x<=end; x+=incr)" doesn't work with incr<0.
		double x=start;
		for (int i=0; i<n; i++) {
			values.add(String.format(format, x));
			x+=incr;
		}
	}
	
	public GridDimension(String name, String heading, String[] valueStrings) {
		fieldName = name;
		shortName = heading;
		currentPointer=0;
		values = new Vector<String>(valueStrings.length);
		for (String str : valueStrings) values.add(str);
	}
	
	/**
	 * headAndValues format: short name followed by values specification enclosed by '()' or '{}'
	 * () format: (<start>:<stop>[:<incr>])
	 * {} format contains '|' separated list of values.
	 * e.g. "COAL(5:15:10)" or "SEED(1:20)" or "EPOCH{(1,2)|(1,3)}"
	 * (actually the code will parse some other strings also, such as "SEED)1:20")
	 * 
	 * @param name Field name of HybridSim nexus block
	 * @param headAndValues 
	 * @return
	 */
	public static GridDimension parseGridDimension(String name, String headAndValues) {
		GridDimension gd = null;
		// First try for "{}" delimiters
		String[] tokens = headAndValues.split("[{}]");
		if (tokens.length>1) {
			// Found '{' or '}'
			gd = new GridDimension(name,tokens[0],tokens[1].split("\\|"));
		} else {
			tokens = headAndValues.split("[()]");
			if (tokens.length==1) throw new RuntimeException("Could not parse grid values from '"+headAndValues+"'");
			String tokens2[] = tokens[1].split(":");
			if (tokens[1].contains(".")) {
				// real numbers so use doubles
				double start = Double.valueOf(tokens2[0]);
				double end = Double.valueOf(tokens2[1]);
				double incr = (tokens2.length==2) ? 1 : Double.valueOf(tokens2[2]);
				gd = new GridDimension(name,tokens[0],start,end,incr);
			} else {
				// integers
				int start = Integer.valueOf(tokens2[0]);
				int end = Integer.valueOf(tokens2[1]);
				int incr = (tokens2.length==2) ? 1 : Integer.valueOf(tokens2[2]);
				gd = new GridDimension(name,tokens[0],start,end,incr);
			}
		}
		return gd;
	}
	
	public String getFullName() {
		return fieldName;
	}

	public String getShortName() {
		return shortName;
	}
	
	/**
	 * NOTE: After reset(), first element is obtainable by getCurrent().
	 * If you immediately do a next(), you'll be getting the second element.
	 */
	public void reset() {
		currentPointer = 0;
	}
	
	public String getCurrent() {
		return values.get(currentPointer);
	}
	
	public boolean hasNext() {
		return currentPointer+1<values.size();
	}
	
	public String next() {
		if (!hasNext()) throw new IndexOutOfBoundsException("Dimension has no more entries");
		currentPointer++;
		return getCurrent();
	}

	// TODO: put some indicator for current location in.
	public String toString() {
		StringBuffer buf = (new StringBuffer("(")).append(fieldName);
		String sep = ":";
		for (String str : values) {
			buf.append(sep).append(str);
			sep = ",";
		}
		buf.append(")");
		return buf.toString();
	}
}
