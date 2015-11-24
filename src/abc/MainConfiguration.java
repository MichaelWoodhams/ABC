package abc;

import hybridseq.HybridNetworkParameters;
import hybridstats.Forest;
import hybridstats.SummaryStatParameters;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

import org.biojava.bio.seq.io.ParseException;
import org.biojavax.bio.phylo.io.nexus.CharactersBlock;
import org.biojavax.bio.phylo.io.nexus.DataBlock;
import org.biojavax.bio.phylo.io.nexus.DistancesBlock;
import org.biojavax.bio.phylo.io.nexus.NexusBlockParser;
import org.biojavax.bio.phylo.io.nexus.NexusFile;
import org.biojavax.bio.phylo.io.nexus.NexusFileBuilder;
import org.biojavax.bio.phylo.io.nexus.NexusFileFormat;
import org.biojavax.bio.phylo.io.nexus.TaxaBlock;
import org.biojavax.bio.phylo.io.nexus.TreesBlock;

import pal.tree.TreeParseException;
import palExtensions.ExtRandom;

import biojavaExtensions.GenericBlock;
import biojavaExtensions.GenericBlockParser;
import biojavaExtensions.NexusUtils;
import biojavaExtensions.UseableUnknownBlockParser;

import org.kohsuke.args4j.Option;


public class MainConfiguration {
	/**
	 * Holds all the configuration variables for Main.
	 * @author Michael Woodhams
	 *
	 */
	
	public RandomHybridParameters params = null;
	public SummaryStatParameters stats = null;
	public ABCConfiguration abc = null;
	public Grid grid = null;
	@Option(name="-i",usage="Input file name")
	public String inputFile = "input.nex";
	@Option(name="-o",usage="Output file name")
	public String outputFile = "output.nex";
	@Option(name="-s",usage="Random number generator seed")
	long cmdLineSeed = 0;
	public NexusFile nexusFile;
	public Forest targetForest = null;
	
	public void parseNexus(PrintWriter out) {
		
		File file = new File(inputFile);
		NexusFileBuilder builder=new NexusFileBuilder();
		// The two custom Nexus blocks used by this program:
		builder.setBlockParser(hybridseq.HybridNetworkParameters.BLOCK_NAME, 
				new GenericBlockParser(false,HybridNetworkParameters.HYBRIDSIM_VALID_KEYS));
		HashSet<String> validGridKeys = new HashSet<String>(HybridNetworkParameters.HYBRIDSIM_VALID_KEYS);
		validGridKeys.addAll(SummaryStatParameters.HYBRID_STATS_VALID_KEYS);
		builder.setBlockParser("hybridabcgrid", new GenericBlockParser(false, validGridKeys));
		builder.setBlockParser(ABCConfiguration.BLOCK_NAME, new GenericBlockParser(false, ABCConfiguration.ABC_VALID_KEYS)); // TODO: update valid keys list
		builder.setBlockParser("filo", new GenericBlockParser(true));
		
		/*
		 *  For Nexus block types NOT used by this program, use the simple 'unknown' parser.
		 *  This will parse pretty much anything, so we avoid issues with parse errors on
		 *  stuff that will never be used anyhow. 
		 */
		builder.setBlockParser(TaxaBlock.TAXA_BLOCK, new UseableUnknownBlockParser());
		builder.setBlockParser(CharactersBlock.CHARACTERS_BLOCK, new UseableUnknownBlockParser());
		builder.setBlockParser(DataBlock.DATA_BLOCK, new UseableUnknownBlockParser());
		builder.setBlockParser(DistancesBlock.DISTANCES_BLOCK, new UseableUnknownBlockParser());
		builder.setBlockParser(NexusBlockParser.UNKNOWN_BLOCK,	new UseableUnknownBlockParser());
		
		try {
			NexusFileFormat.parseFile(builder, file);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		nexusFile = builder.getNexusFile();
		params = new RandomHybridParameters(nexusFile,cmdLineSeed); // may not actually contain any random dimensions
		GenericBlock gridBlock = (GenericBlock)NexusUtils.getUniqueBlockByName(nexusFile, Grid.BLOCK_NAME);
		
		GenericBlock abcBlock = (GenericBlock)NexusUtils.getUniqueBlockByName(nexusFile, ABCConfiguration.BLOCK_NAME);
		if (abcBlock != null) {
			ExtRandom rng = new ExtRandom(params.seed);
			abc = new ABCConfiguration(abcBlock);
			abc.updateStatsAndParams(stats, params, rng);
		}

		if (gridBlock != null) {
			stats = new SummaryStatParameters(gridBlock);
			grid = new Grid(params, gridBlock);
		} else if (abc != null) {
			stats = new SummaryStatParameters();
		} else {
			// stats not used when in simple hybridsim mode, and can cause an error 
			// (e.g. if we're generating only a single output tree and using SI-2 stat)
			stats = null;
		}
		
		/*
		 *  If we have a real-life set of gene trees and we want to compare sets of simulated gene trees to it,
		 *  it is helpful to get the sizes of the problem (how many taxa and gene trees) from the input.
		 *   If we're in simple hybridsim mode (generate one network, and then generate trees on this) 
		 *   we don't need to do this, and trying to do this breaks the 'round trip repeatability' property
		 *   (output file should be usable as an input file, and will regenerate itself.) 
		 */
		if (abc!=null || grid!=null) {
			TreesBlock treesBlock = (TreesBlock)NexusUtils.getUniqueBlockByName(nexusFile, "trees");
			if (treesBlock != null) {
				try {
					targetForest = new Forest(treesBlock);
				} catch (IOException | TreeParseException e) {
					throw new RuntimeException("Could not parse trees block");
				}
				params.numRandomTrees=targetForest.size();
				params.maxTaxa=targetForest.get(0).getIdCount(); // assumption: all trees are of same size.
				out.printf("Read %d trees of %d taxa\n", params.numRandomTrees, params.maxTaxa);
			}
		}
		// Range checking
		if (stats!=null) stats.rangeCheck(params.numRandomTrees);
		if (grid!=null && abc!=null) 
			throw new RuntimeException("Cannot have both a "+Grid.BLOCK_NAME+" and a "+ABCConfiguration.BLOCK_NAME+" block in input file");
		
	}
}
