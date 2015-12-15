package br.com.jonathan.crf.dto;

import java.util.List;
import java.util.Map;

import br.com.jonathan.crf.indexer.Index;
import br.com.jonathan.crf.utils.Constantes;

public class CRFCliqueTree< E > {
	private final FactorTable[ ] factorTables;
	private final Map< String, Integer > classIndex;
	private final double z;
	private final int windowSize;

	public CRFCliqueTree( FactorTable[ ] factorTables, Map< String, Integer > classIndex, int windowSize ){
		this.factorTables = factorTables;
		this.classIndex = classIndex;
		this.z = factorTables[ 0 ].totalMass();
		this.windowSize = windowSize;
	}

	public static < E > CRFCliqueTree< E > getCalibratedCliqueTree( int[ ][ ][ ] data, List< Index< CRFLabel > > labelIndices, int numClasses, double[ ][ ] weights, Map< String, Integer > classIndex, int windowSize ) {
		FactorTable[ ] factorTables = new FactorTable[ data.length ];
		FactorTable[ ] messages = new FactorTable[ data.length - 1 ];

		for ( int i = 0; i < data.length; i++ ) {
			factorTables[ i ] = getFactorTable( data[ i ], labelIndices, numClasses, weights );
			if ( i > 0 ) {
				messages[ i - 1 ] = factorTables[ i - 1 ].sumOutFront();
				factorTables[ i ].multiplyInFront( messages[ i - 1 ] );
			}
		}

		for ( int i = factorTables.length - 2; i >= 0; i-- ) {
			FactorTable summedOut = factorTables[ i + 1 ].sumOutEnd();
			summedOut.divideBy( messages[ i ] );
			factorTables[ i ].multiplyInEnd( summedOut );
		}

		return new CRFCliqueTree< E >( factorTables, classIndex, windowSize );
	}

	private static FactorTable getFactorTable( int[ ][ ] data, List< Index< CRFLabel > > labelIndices, int numClasses, double[ ][ ] weights ) {
		FactorTable factorTable = null;

		for ( int j = 0, sz = labelIndices.size(); j < sz; j++ ) {
			Index< CRFLabel > labelIndex = labelIndices.get( j );
			FactorTable ft = new FactorTable( numClasses, j + 1 );

			for ( int k = 0, liSize = labelIndex.size(); k < liSize; k++ ) {
				int[ ] label = labelIndex.get( k ).getLabel();
				double cliquePotential = computeCliquePotential( data[ j ], k, weights );
				ft.setValue( label, cliquePotential );
			}
			if ( j > 0 ) {
				ft.multiplyInEnd( factorTable );
			}
			factorTable = ft;
		}

		return factorTable;
	}

	private static double computeCliquePotential( int[ ] cliqueFeatures, int labelIndex, double[ ][ ] weights ) {
		double output = 0.0;
		for ( int m = 0; m < cliqueFeatures.length; m++ ) {
			double dotProd = weights[ cliqueFeatures[ m ] ][ labelIndex ];
			output += dotProd;
		}
		return output;
	}

	public double logProbStartPos( int backgroundIndex ) {
		double u = factorTables[ 0 ].unnormalizedLogProbFront( backgroundIndex );
		return u - z;
	}

	public double condLogProbGivenPrevious( int position, int label, int[ ] prevLabels ) {
		if ( prevLabels.length + 1 == windowSize ) {
			return factorTables[ position ].conditionalLogProbGivenPrevious( prevLabels, label );
		} else if ( prevLabels.length + 1 < windowSize ) {
			FactorTable ft = factorTables[ position ].sumOutFront();
			while ( ft.windowSize() > prevLabels.length + 1 ) {
				ft = ft.sumOutFront();
			}
			return ft.conditionalLogProbGivenPrevious( prevLabels, label );
		} else {
			int[ ] p = new int[ windowSize - 1 ];
			System.arraycopy( prevLabels, prevLabels.length - p.length, p, 0, p.length );
			return factorTables[ position ].conditionalLogProbGivenPrevious( p, label );
		}
	}

	public double prob( int position, int[ ] labels ) {
		return Math.exp( logProb( position, labels ) );
	}

	public double prob( int position, int label ) {
		return Math.exp( logProb( position, label ) );
	}

	public double logProb( int position, int label ) {
		double u = factorTables[ position ].unnormalizedLogProbEnd( label );
		return u - z;
	}

	public double logProb( int position, int[ ] labels ) {
		if ( labels.length < windowSize ) {
			return factorTables[ position ].unnormalizedLogProbEnd( labels ) - z;
		} else if ( labels.length == windowSize ) {
			return factorTables[ position ].unnormalizedLogProb( labels ) - z;
		} else {
			int[ ] l = new int[ windowSize ];
			System.arraycopy( labels, 0, l, 0, l.length );
			int position1 = position - labels.length + windowSize;
			double p = factorTables[ position1 ].unnormalizedLogProb( l ) - z;
			l = new int[ windowSize - 1 ];
			System.arraycopy( labels, 1, l, 0, l.length );
			position1++;
			for ( int i = windowSize; i < labels.length; i++ ) {
				p += condLogProbGivenPrevious( position1++, labels[ i ], l );
				System.arraycopy( l, 1, l, 0, l.length - 1 );
				l[ windowSize - 2 ] = labels[ i ];
			}
			return p;
		}
	}

	public int window() {
		return windowSize;
	}

	public int getNumClasses() {
		return classIndex.size();
	}

	public int backgroundIndex() {
		return classIndex.get( Constantes.DEFAULT_SYMBOL );
	}

	public int length() {
		return factorTables.length;
	}
}