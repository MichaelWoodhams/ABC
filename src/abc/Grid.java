package abc;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;

import org.biojavax.bio.phylo.io.nexus.NexusFile;

import biojavaExtensions.GenericBlock;
import biojavaExtensions.GenericNexusStuff;
import biojavaExtensions.NexusUtils;

import hybridseq.HybridNetworkParameters;

// TODO: (possibly). Give GridDimension a pointer to params, so reset() and next() methods auto-update params. 
// TODO: (possibly) Store a clone of input params (so original won't get altered.)

public class Grid {
	private HybridNetworkParameters params;
	private Vector<GridDimension> dimensions;
	static final String BLOCK_NAME = "hybridABCgrid";
	
	public Grid(HybridNetworkParameters params) {
		this.params = params;
		dimensions = new Vector<GridDimension>(3);
	}
		
	public Grid(HybridNetworkParameters params, GenericBlock block) {
		this(params); 
		Iterator<GenericNexusStuff> stuffIter = block.stuffIterator();
		while (stuffIter.hasNext()) {
			GenericNexusStuff stuff = stuffIter.next();
			if (stuff.isField()) {
				dimensions.add(GridDimension.parseGridDimension(stuff.getKey(),stuff.getValue().trim()));
			}
		}
		reset();
	}
	
	public Grid(HybridNetworkParameters params, NexusFile nexusFile) {
		this(params,(GenericBlock)NexusUtils.getUniqueBlockByName(nexusFile, BLOCK_NAME)); 
	}
	
	public void addDimension(GridDimension dimension) {
		dimensions.add(dimension);
		dimension.reset();
		params.setParameter(dimension.getFullName(), dimension.getCurrent());
	}
	
	public void printHeaders(PrintWriter out) {
		for (GridDimension dimension : dimensions) {
			out.printf("%s\t", dimension.getShortName());
		}
	}
	
	public HybridNetworkParameters getParameters() {
		return params;
	}
	
	public void printCurrentValues(PrintWriter out) {
		for (GridDimension dimension : dimensions) {
			out.printf("%s\t", dimension.getCurrent());
		}
	}
	
	public void reset() {
		for (GridDimension dimension : dimensions) {
			dimension.reset();
			params.setParameter(dimension.getFullName(), dimension.getCurrent());
		}
	}
	
	public boolean hasNext() {
		for (GridDimension dimension : dimensions) {
			if (dimension.hasNext()) return true;
		}
		return false;
	}
	
	/**
	 * Like 'next()' except returns whether it was able to advance or not.
	 * (Not able = is at end of sequence)
	 * If it returns 'false', it leaves the grid in the same state
	 * as a 'reset()' (i.e. loops back to starting state).
	 */
	public boolean advance() {
		for (GridDimension dimension : dimensions) {
			if (dimension.hasNext()) {
				dimension.next();
				params.setParameter(dimension.getFullName(), dimension.getCurrent());
				return true;
			} else {
				dimension.reset();
				params.setParameter(dimension.getFullName(), dimension.getCurrent());
			}
		}
		return false;
	}
}
