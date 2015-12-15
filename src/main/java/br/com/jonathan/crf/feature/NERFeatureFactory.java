package br.com.jonathan.crf.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import br.com.jonathan.crf.dto.Clique;
import br.com.jonathan.crf.dto.Token;
import br.com.jonathan.crf.factory.FeatureFactory;

public class NERFeatureFactory extends FeatureFactory{
	private static final long serialVersionUID = 1L;

	private final WordShapeClassifier shapeClassifeir;
	private final Integer maxNGramLeng = 6;
	private final Integer disjunctionWidth = 4;

	public NERFeatureFactory( WordShapeClassifier shapeClassifeir ){
		this.shapeClassifeir = shapeClassifeir;
	}

	@Override
	public Collection< String > getCliqueFeatures( int loc, Clique clique, List< Token > tokens ) {

		Collection< String > cliqueFeatures = new ArrayList< >();
		Collection< String > features = new HashSet< String >();
		String suffix = new String();
		if ( clique == cliqueC ) {
			cliqueFeatures = featuresC( loc, tokens );
			suffix = "C";
		} else if ( clique == cliqueCpC ) {
			cliqueFeatures = featuresCpC( loc, tokens );
			suffix = "CpC";
			addAllInterningAndSuffixing( features, cliqueFeatures, suffix );

			cliqueFeatures = featuresCnC( loc - 1, tokens );
			suffix = "CnC";
		} else if ( clique == cliqueCp2C ) {
			cliqueFeatures = featuresCp2C( loc, tokens );
			suffix = "Cp2C";
		} else if ( clique == cliqueCp3C ) {
			cliqueFeatures = featuresCp3C( loc, tokens );
			suffix = "Cp3C";
		} else if ( clique == cliqueCp4C ) {
			cliqueFeatures = featuresCp4C( loc, tokens );
			suffix = "Cp4C";
		} else if ( clique == cliqueCp5C ) {
			cliqueFeatures = featuresCp5C( loc, tokens );
			suffix = "Cp5C";
		} else if ( clique == cliqueCpCp2C ) {
			cliqueFeatures = featuresCpCp2C( loc, tokens );
			suffix = "CpCp2C";
			addAllInterningAndSuffixing( features, cliqueFeatures, suffix );

			cliqueFeatures = featuresCpCnC( loc - 1, tokens );
			suffix = "CpCnC";
		} else if ( clique == cliqueCpCp2Cp3C ) {
			cliqueFeatures = featuresCpCp2Cp3C( loc, tokens );
			suffix = "CpCp2Cp3C";
		} else if ( clique == cliqueCpCp2Cp3Cp4C ) {
			cliqueFeatures = featuresCpCp2Cp3Cp4C( loc, tokens );
			suffix = "CpCp2Cp3Cp4C";
		} else {
			throw new IllegalArgumentException( "Unknown clique: " + clique );
		}

		addAllInterningAndSuffixing( features, cliqueFeatures, suffix );
		return features;

	}

	protected Collection< String > featuresCpCp2Cp3Cp4C( int loc, List< Token > tokens ) {
		Collection< String > features = new ArrayList< String >();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		return features;
	}

	protected Collection< String > featuresCpCp2Cp3C( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String p2Word = ( loc - 1 ) < 1 ? new String() : tokens.get( loc - 2 ).getText();
		String p3Word = ( loc - 2 ) < 1 ? new String() : tokens.get( loc - 3 ).getText();
		Collection< String > features = new ArrayList< String >();
		return features;
	}

	protected Collection< String > featuresCpCnC( int loc, List< Token > tokens ) {
		Collection< String > features = new ArrayList< String >();
		return features;
	}

	protected Collection< String > featuresCpCp2C( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String p2Word = ( loc - 1 ) < 1 ? new String() : tokens.get( loc - 2 ).getText();

		String cShape = shapeClassifeir.wordShape( cWord );
		String pShape = shapeClassifeir.wordShape( pWord );
		String p2Shape = shapeClassifeir.wordShape( p2Word );

		Collection< String > features = new ArrayList< String >();
		features.add( p2Shape + '-' + pShape + '-' + cShape + "-TYPETYPES" );

		return features;
	}

	protected Collection< String > featuresCp5C( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String p2Word = ( loc - 1 ) < 1 ? new String() : tokens.get( loc - 2 ).getText();
		String p3Word = ( loc - 2 ) < 1 ? new String() : tokens.get( loc - 3 ).getText();
		String p4Word = ( loc - 3 ) < 1 ? new String() : tokens.get( loc - 4 ).getText();
		String p5Word = ( loc - 4 ) < 1 ? new String() : tokens.get( loc - 5 ).getText();
		Collection< String > features = new ArrayList< String >();
		return features;
	}

	protected Collection< String > featuresCp4C( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String p2Word = ( loc - 1 ) < 1 ? new String() : tokens.get( loc - 2 ).getText();
		String p3Word = ( loc - 2 ) < 1 ? new String() : tokens.get( loc - 3 ).getText();
		String p4Word = ( loc - 3 ) < 1 ? new String() : tokens.get( loc - 4 ).getText();
		Collection< String > features = new ArrayList< String >();
		return features;
	}

	protected Collection< String > featuresCp3C( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String p2Word = ( loc - 1 ) < 1 ? new String() : tokens.get( loc - 2 ).getText();
		String p3Word = ( loc - 2 ) < 1 ? new String() : tokens.get( loc - 3 ).getText();
		Collection< String > features = new ArrayList< String >();
		return features;
	}

	protected Collection< String > featuresCp2C( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String p2Word = ( loc - 1 ) < 1 ? new String() : tokens.get( loc - 2 ).getText();
		Collection< String > features = new ArrayList< String >();
		return features;
	}

	protected Collection< String > featuresCnC( int loc, List< Token > tokens ) {
		Collection< String > features = new ArrayList< String >();
		return features;
	}

	protected Collection< String > featuresCpC( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String nWord = loc < ( tokens.size() - 1 ) ? tokens.get( loc + 1 ).getText() : new String();

		String cShape = shapeClassifeir.wordShape( cWord );
		String pShape = shapeClassifeir.wordShape( pWord );
		pShape = pShape == null ? new String() : pShape;

		String nShape = shapeClassifeir.wordShape( nWord );

		Collection< String > features = new ArrayList< String >();
		features.add( "PSEQ" );
		features.add( cWord + "-PSEQW" );
		features.add( pWord + '-' + cWord + "-PSEQW2" );
		features.add( pWord + "-PSEQpW" );
		features.add( pShape + "-PSEQpS" );
		features.add( cShape + "-PSEQcS" );
		features.add( pShape + '-' + cShape + "-TYPES" );
		features.add( cShape + "-TPS2" );
		features.add( nShape + "-TNS1" );

		return features;
	}

	protected Collection< String > featuresC( int loc, List< Token > tokens ) {
		String cWord = tokens.get( loc ).getText();
		String pWord = loc < 1 ? new String() : tokens.get( loc - 1 ).getText();
		String nWord = loc < ( tokens.size() - 1 ) ? tokens.get( loc + 1 ).getText() : new String();

		String cShape = shapeClassifeir.wordShape( cWord );
		String pShape = shapeClassifeir.wordShape( pWord );
		pShape = pShape == null ? new String() : pShape;

		String nShape = shapeClassifeir.wordShape( nWord );
		nShape = nShape == null ? new String() : nShape;

		Collection< String > features = new ArrayList< String >();
		features.add( cWord + "-WORD" );
		features.add( pWord + "-PW" );
		features.add( nWord + "-NW" );

		features.add( "###" );

		Collection< String > subs = new ArrayList< String >();
		String word = '<' + cWord + '>';

		int max = maxNGramLeng >= 0 ? Math.min( maxNGramLeng, word.length() ) : word.length();
		for ( int j = 2; j <= max; j++ ) {
			subs.add( '#' + word.substring( 0, j ) + '#' );
		}
		int start = maxNGramLeng >= 0 ? Math.max( 0, word.length() - maxNGramLeng ) : 0;
		int lenM1 = word.length() - 1;

		for ( int i = start; i < lenM1; i++ ) {
			subs.add( '#' + word.substring( i ) + '#' );
		}

		features.addAll( subs );
		features.add( cShape + "-TYPE" );
		features.add( pShape + "-PTYPE" );
		features.add( nShape + "-NTYPE" );
		features.add( pWord + "..." + cShape + "-PW_CTYPE" );
		features.add( cShape + "..." + nWord + "-NW_CTYPE" );
		features.add( pShape + "..." + cShape + "-PCTYPE" );
		features.add( cShape + "..." + nShape + "-CNTYPE" );
		features.add( pShape + "..." + cShape + "..." + nShape + "-PCNTYPE" );

		for ( int i = 1; i <= disjunctionWidth; i++ ) {
			String wordDn = ( loc + i ) < ( tokens.size() ) ? tokens.get( loc + i ).getText() : new String();
			features.add( wordDn + "-DISJN" );
			String wordDp = ( loc - i ) < 0 ? new String() : tokens.get( loc - i ).getText();
			features.add( wordDp + "-DISJP" );
		}

		return features;
	}

	protected void addAllInterningAndSuffixing( Collection< String > accumulator, Collection< String > addend, String suffix ) {
		boolean nonNullSuffix = suffix != null && !suffix.isEmpty();
		if ( nonNullSuffix ) {
			suffix = '|' + suffix;
		}
		for ( String feat : addend ) {
			if ( nonNullSuffix ) {
				feat = feat.concat( suffix );
			}
			accumulator.add( feat );
		}
	}

}