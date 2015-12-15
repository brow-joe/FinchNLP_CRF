package br.com.jonathan.crf.classifier;

import br.com.jonathan.crf.dto.CRFCliqueTree;

public class SequenceModel{
	private final int window;
	private final CRFCliqueTree cliqueTree;
	private final int[ ] backgroundTag;

	private final int[ ] allTags;
	private final int[ ][ ] allowedTagsAtPosition;

	public SequenceModel( CRFCliqueTree cliqueTree ){
		this.cliqueTree = cliqueTree;
		this.window = cliqueTree.window();
		int numClasses = cliqueTree.getNumClasses();
		this.backgroundTag = new int[ ] { cliqueTree.backgroundIndex() };
		allTags = new int[ numClasses ];
		for ( int i = 0; i < allTags.length; i++ ) {
			allTags[ i ] = i;
		}
		allowedTagsAtPosition = null;
	}

	public int length() {
		return cliqueTree.length();
	}

	public int leftWindow() {
		return window - 1;
	}

	public int rightWindow() {
		return 0;
	}

	public int[ ] getPossibleValues( int pos ) {
		if ( pos < leftWindow() ) {
			return backgroundTag;
		}
		int realPos = pos - window + 1;
		return allowedTagsAtPosition == null ? allTags : allowedTagsAtPosition[ realPos ];
	}

	public double[ ] scoresOf( int[ ] tags, int pos ) {
		int[ ] allowedTags = getPossibleValues( pos );
		int realPos = pos - window + 1;
		int[ ] previous = new int[ window - 1 ];
		for ( int i = 0; i < window - 1; i++ ) {
			previous[ i ] = tags[ realPos + i ];
		}
		double[ ] scores = new double[ allowedTags.length ];
		for ( int i = 0; i < allowedTags.length; i++ ) {
			scores[ i ] = cliqueTree.condLogProbGivenPrevious( realPos, allowedTags[ i ], previous );
		}
		return scores;
	}

}
