package abc;

import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.Vector;

import org.biojavax.bio.phylo.io.nexus.CharactersBlockBuilder;
import org.biojavax.bio.phylo.io.nexus.NexusBlock;
/*
 * Stores random stats of accepted simulations
 */
// TODO: Combine with ABCConfiguration?

public class ABCResults {
	private Vector<String> headings = null;
	private Vector<String[]> values = null;
	
	public ABCResults() {
	}
	
	public void add(int seq, RandomHybridParameters hybridParams, SortedMap<String,Double> compoundStats) {
		if (headings == null) {
			// This is the first time we've added.
			headings = new Vector<String>();
			headings.add("SEQ");
			for (String name : hybridParams.getRandDimensionNames()) {
				headings.add(name);
			}
			for (String name : compoundStats.keySet()) {
				headings.add(name);
			}
			this.values = new Vector<String[]>();
		}
		// Construct the array of value strings
		String[] valueStrings = new String[headings.size()];
		int i=0;
		valueStrings[i++] = Integer.toString(seq);
		for (String number : hybridParams.getRandDimensionValues()) {
			valueStrings[i++] = String.format("%s", number);
		}
		for (Double number : compoundStats.values()) {
			valueStrings[i++] = String.format("%f", number);
		}
		values.add(valueStrings);
	}
	
	public NexusBlock outputCharactersBlock() { 		
		CharactersBlockBuilder builder = new CharactersBlockBuilder();
		int nChar = headings.size();
		builder.startBlock("characters");
		builder.setDimensionsNTax(values.size());
		builder.setDimensionsNChar(nChar);
		builder.setDataType("continuous");
		builder.setGap("-"); // not used, but bioJava throws exception if left as null
		for (String heading : headings) {
			builder.addCharLabel(heading);
		}
		for (String[] row : values) {
			String taxon = "SEQ"+row[0];
			builder.addMatrixEntry(taxon);
			for (int i=1; i<nChar; i++) {
				builder.appendMatrixData(taxon, row[i]+((i+1==nChar)?"":","));
			}
		}
		builder.endBlock();
		return builder.getNexusBlock();
	}
	
	public void outputCommaDelimited(PrintWriter out) {
		if (headings==null) {
			System.err.println("No hits to report!");
			return;
		}
		int nCol=headings.size();
		int nRow=values.size();
		String[] formats = new String[nCol];
		for (int col=0; col<nCol; col++) {
			int width=headings.get(col).length();
			for (int row=0; row<nRow; row++) {
				width = Math.max(width, values.get(row)[col].length());
			}
			formats[col]="%"+Integer.toString(width)+((col+1==nCol) ? "s\n" : "s, ");
		}

		for (int col=0; col<nCol; col++) {
			out.printf(formats[col], headings.get(col));
		}
		for (int row=0; row<nRow; row++) {
			for (int col=0; col<nCol; col++) {
				out.printf(formats[col], values.get(row)[col]);
			}
		}
	}
	
	public int getNumberHits() {
		return values.size();
	}
}
