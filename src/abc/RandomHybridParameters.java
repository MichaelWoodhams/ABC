package abc;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;
import org.biojavax.bio.phylo.io.nexus.NexusFile;
import biojavaExtensions.GenericNexusStuff;
import palExtensions.ExtRandom;
import hybridseq.HybridNetworkParameters;

/*
 * There is an inconsistency in design between this class (extends HybridNetworkParameters)
 * and GridDimension which is rather similar but contains HybridNetworkParameters. 
 */

public class RandomHybridParameters extends HybridNetworkParameters {
	private Vector<RandomDimension> randDimensions;
	private ExtRandom rng;
	
	// if seedOverride is non-zero, use that as seed rather than seed specified by Nexus file.
	public RandomHybridParameters(NexusFile nexusFile, long seedOverride) {
		super(nexusFile);
		if (seedOverride!=0) this.seed = seedOverride;
		rng = new ExtRandom(this.seed);
		randDimensions = new Vector<RandomDimension>();
	}
	
	public void parseAndAddRandomDimension(GenericNexusStuff field) {
		if (!HYBRIDSIM_VALID_KEYS.contains(field.getKey())) throw new IllegalArgumentException("Unrecognized field name '"+field.getKey()+"'");
		addRandomDimension(new RandomDimension(field,rng));
	}
	
	public void addRandomDimension(RandomDimension dim) {
		randDimensions.add(dim);
	}
	
	public Iterator<RandomDimension> getRandomDimensionIterator() {
		return randDimensions.iterator();
	}

	public void randomize() {
		for (RandomDimension rDim : randDimensions) {
			// For the sake of generality of setParameter(), we're converting number to 
			// string which will immediately be converted back again. Consider code changes to avoid this.
 			setParameter(rDim.getFullName(), rDim.getNewValueAsString()); 
		}
		seed++; // very important: else every run generates network on the same seed
	}
	public void printHeaders(PrintWriter out) {
		for (RandomDimension rDim : randDimensions) {
			out.printf("%s\t", rDim.getShortName());
		}
	}
		
	public void printCurrentValues(PrintWriter out) {
		for (RandomDimension rDim : randDimensions) {
			out.printf("%7f\t", rDim.getLastValue());
		}
	}
	
	public String[] getRandDimensionNames() {
		int n = randDimensions.size();
		String[] names = new String[n];
		for (int i=0; i<n; i++) {
			names[i]=randDimensions.get(i).getShortName();
		}
		return names;
	}
	
	public String[] getRandDimensionValues() {
		int n = randDimensions.size();
		String[] values = new String[n];
		for (int i=0; i<n; i++) {
			values[i]=Double.toString(randDimensions.get(i).getLastValue());
		}
		return values;
	}
}
