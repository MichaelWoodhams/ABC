package abc;

import hybridseq.HybridNetworkParameters;
import biojavaExtensions.GenericNexusStuff;
import palExtensions.ExtRandom;

// TODO: (Possibly) subclass into integer and double.
public class RandomDimension {
	private String fieldName;
	private String shortName; 
	private boolean log;
	private ExtRandom rng;
	private double min;
	private double range; // max value = min+range
	private double lastValue;
	private boolean integer; 
	// Currently, only support a uniform random distribution, parameterized by 'min' and 'max'.
	// A likely extension is to have some 'distribution' object which could be normal, uniform, etc.	
	
	public RandomDimension(String name, String heading, ExtRandom rng, double min, double max, boolean log) {
		if (
				name==null    || name.length()==0 || 
				heading==null || heading.length()==0 || 
				rng== null || min > max || (log && min<=0)
			) throw new IllegalArgumentException("Bad arguments to RandomDimension constructor");
		fieldName = name;
		shortName = heading;
		this.rng = rng;
		this.log = log;
		if (log) {
			this.min = Math.log(min);
			range = Math.log(max)-this.min;
		} else {
			this.min = min;
			range = max-min;	
		}
		lastValue = Double.NaN;
		integer = isIntegerParameter(fieldName);
		if (integer && (min!=Math.floor(min) || max!=Math.floor(max))) {
			throw new IllegalArgumentException("Non-integer bounds for integer parameter");
		}
	}
	
	public RandomDimension(GenericNexusStuff field, ExtRandom rng) {
		// TODO: subclass GenericNexusStuff to make this redundant.
		if (!field.isField()) throw new IllegalArgumentException("Can only accept a field"); 
		fieldName = field.getKey();
		this.rng = rng;
		String[] tokens = field.getValue().split("[()]");
		if (tokens.length==1) throw new RuntimeException("Could not parse short name from '"+field.toString()+"'");
		if (tokens.length==2) {
			// just short name and range
			this.log = false;
		} else if (tokens.length==3 && "log".equals(tokens[2].trim())) {
			// just short name, range and "log"
			this.log = true;
		} else { 
			throw new RuntimeException("Could not parse min/max values from '"+field.toString()+"'");
		}
		shortName = tokens[0].trim();
		tokens = tokens[1].split(",");
		double rawmin = Double.valueOf(tokens[0]);
		double rawmax = Double.valueOf(tokens[1]);
		if (this.log) {
			if (rawmin<=0 || rawmax<=0) 
				throw new RuntimeException("Range must be positive for log distribution, in '"+field.toString()+"'");
			min = Math.log(rawmin);
			range = Math.log(rawmax)-min;
		} else {
			min = rawmin;
			range = rawmax - min;
		}
		if (range <=0) throw new RuntimeException("Max must be greater than min from '"+field.toString()+"'");
		integer = isIntegerParameter(fieldName);
		if (integer) {
			if (min!=Math.floor(min) || rawmax!=Math.floor(rawmax)) {
				throw new IllegalArgumentException("Non-integer bounds for integer parameter");
			}
			range++; // required because of how rng.nextInt works, to avoid out by one error.
		}
	}
	
	private boolean isIntegerParameter(String parameterName) {
		// TODO: Bad engineering - same string hard-coded in several places, looking for match
		String datatype = HybridNetworkParameters.getParameterDatatype(parameterName);
		switch (datatype) {
		case "int" : return true;
		case "StepwiseFunction" :  // StepwiseFunction can take a scalar value (i.e. if it has no steps) 
		case "double" :	return false;
		default : throw new IllegalArgumentException("Parameter "+parameterName+" is not a scalar");
		}
	}
		
	/**
	 * headAndValues format: short name followed by "(<min>,<max>)" optionally followed by "log"
	 *  
	 * @param name Field name from HybridSim nexus block
	 * @param headAndValues 
	 * @return
	 */
	// TODO: consider whether passing rng in here is the correct thing to do.
	// Made redundant by second constructor.
/*	
	public static RandomDimension parseRandomDimension(String name, String headAndValues, ExtRandom rng) {
		boolean log;
		String[] tokens = headAndValues.split("[()]");
		if (tokens.length==1) throw new RuntimeException("Could not parse grid values from '"+headAndValues+"'");		if (tokens.length==2) {
			// just short name and range
			log = false;
		} else if (tokens.length==3 && "log".equals(tokens[2].trim())) {
			// just short name, range and "log"
			log = true;
		} else { 
			throw new RuntimeException("Could not parse min/max values from '"+headAndValues.toString()+"'");
		}
		String shortName = tokens[0];
		tokens = tokens[1].split(",");
		if (tokens.length!=2) throw new RuntimeException("Could not parse grid values from '"+headAndValues+"'");
		double min = Double.valueOf(tokens[0]);
		double max = Double.valueOf(tokens[1]);
		return new RandomDimension(name,shortName,rng,min,max,log);
	}
*/		
	public String getFullName() {
		return fieldName;
	}

	public String getShortName() {
		return shortName;
	}
		
	public double getLastValue() {
		return lastValue;
	}

	public double getNewValue() {
		if (integer) {
			lastValue = min+rng.nextInt((int)range);
		} else if (log) {
			lastValue = Math.exp(min+rng.nextDouble()*range);
		} else {
			lastValue = min+rng.nextDouble()*range;
		}
		return lastValue;
	}
	
	public String getNewValueAsString() {
		return (integer) ? Integer.toString((int)getNewValue()) : Double.toString(getNewValue());
	}
	
	public boolean isInteger() {
		return integer;
	}

	public String toString() {
		
		StringBuffer buf = (new StringBuffer(shortName)).append("(");
		if (log) {
			buf.append(Math.exp(min)).append(",").append(Math.exp(min+range)).append(")").append("log");
		} else {
			buf.append(min).append(",").append(min+range).append(")");
		}
		return buf.toString();
	}
}
