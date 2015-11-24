package abc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.biojava.bio.seq.io.ParseException;
import org.biojavax.bio.phylo.io.nexus.NexusBlock;

import hybridseq.HybridNetworkParameters;
import hybridstats.CompoundStat;
import hybridstats.SummaryStatParameters;
import palExtensions.ExtRandom;
import biojavaExtensions.GenericBlock;
import biojavaExtensions.GenericBlockBuilder;
import biojavaExtensions.GenericNexusStuff;

/**
 * Read ABC block from Nexus file, parse, store info in appropriate places, etc.
 * @author woodhams
 *
 */

public class ABCConfiguration {
	private final int iterations;
	private final double scale;
	private final GenericBlock nexusBlock;
	
	public static final String BLOCK_NAME = "ABC";
	// Field names for parameters
	final static String ITER  = "iterations"; 
	final static String SCALE = "scale";
	final static String COMP = "compound";
	public final static Set<String> ABC_VALID_KEYS = new HashSet<String>(); 
	static {
		// Not 100% correct: only parameters that can be specified by a scalar should be valid keys
		ABC_VALID_KEYS.addAll(HybridNetworkParameters.HYBRIDSIM_VALID_KEYS);
		ABC_VALID_KEYS.add(ITER);
		ABC_VALID_KEYS.add(SCALE);
		ABC_VALID_KEYS.add(COMP);
	}
	
	public double getScale()   { return scale;   	}
	public int getIterations() { return iterations; }
	
	
	/* 
	 * Some of contents of ABC block gets stored here, other bits modify
	 * SummaryStatParameters and RandomHybridParameters objects. This is
	 * a mess. However, until it gets refactored usage is to
	 * create an ABCConfiguration object (which caches the ABC block) and
	 * then use that object to update the SSP and RHPs.
	 */
	public ABCConfiguration(GenericBlock abcBlock) {
		nexusBlock = abcBlock;
		scale = Double.valueOf(nexusBlock.getValueTrimmed(SCALE, "1.0"));
		iterations = Integer.valueOf(nexusBlock.getValueTrimmed(ITER, "1000"));
	}
		
	public void updateStatsAndParams(SummaryStatParameters stats, RandomHybridParameters params, ExtRandom rng) {
		CompoundStat compound;
		// TODO: make a 'fieldIterator' in GenericBlock to simplify this code
		Iterator<GenericNexusStuff> iter = nexusBlock.stuffIterator();
		while (iter.hasNext()) {
			GenericNexusStuff stuff = iter.next();
			if (stuff.isField()) {
				String key = stuff.getKey();
				if (key.equals(COMP)) {
					try {
						compound = new CompoundStat(stuff.getValue());
					} catch (ParseException e) {
						e.printStackTrace();
						throw new RuntimeException("Parsing of Nexus file failed");
					}
					stats.addCompoundStat(compound);
				} else if (HybridNetworkParameters.HYBRIDSIM_VALID_KEYS.contains(key)) {
					params.addRandomDimension(new RandomDimension(stuff, rng));
				}
			}
		}
	}     
	
	// TODO: Whole program structure around ABC parameters is a mess. This should
	// probably move elsewhere once it gets tidied up.
	/*
	 * Note: While we could simply return 'nexusBlock', we instead reconstruct all the data.
	 * This ensures what the output file shows is what the program actually used - there is
	 * no space for file parsing errors to silently cause chaos.
	 */
	public NexusBlock getABCBlock(SummaryStatParameters stats, RandomHybridParameters params) {

		GenericBlockBuilder builder = new GenericBlockBuilder();
		builder.startBlock(BLOCK_NAME); 
		try {
			builder.addFieldPlusWhitespace(ITER, "=", Integer.toString(iterations));
			builder.addFieldPlusWhitespace(SCALE, "=", Double.toString(scale));
			Iterator<RandomDimension> iterRD = params.getRandomDimensionIterator();
			while (iterRD.hasNext()) {
				RandomDimension rd = iterRD.next();
				builder.addFieldPlusWhitespace(rd.getFullName(), "=", rd.toString());
			}
			Iterator<CompoundStat> iterCS = stats.compoundStatIterator();
			while (iterCS.hasNext()) {
				CompoundStat cs = iterCS.next();
				builder.addFieldPlusWhitespace(COMP, "=", cs.toString());
			}
		} catch (ParseException e) {
			throw new RuntimeException("Parse error creating ABC block - can't happen");
		}
		builder.endBlock();
		return builder.getNexusBlock();
	}
}
