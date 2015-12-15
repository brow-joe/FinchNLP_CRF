package br.com.jonathan.crf.function;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import br.com.jonathan.crf.dto.CRFCliqueTree;
import br.com.jonathan.crf.dto.CRFLabel;
import br.com.jonathan.crf.indexer.Index;
import br.com.jonathan.crf.utils.Constantes;

public class CRFLogConditionalObjectiveFunction{
	public static final int NO_PRIOR = 0;
	public static final int QUADRATIC_PRIOR = 1;
	/* Use a Huber robust regression penalty (L1 except very near 0) not L2 */
	public static final int HUBER_PRIOR = 2;
	public static final int QUARTIC_PRIOR = 3;
	public static final int DROPOUT_PRIOR = 4;

	// public static final boolean DEBUG2 = true;
	public static final boolean DEBUG2 = false;
	public static final boolean DEBUG3 = false;
	public static final boolean TIMED = false;
	// public static final boolean TIMED = true;
	public static final boolean CONDENSE = true;
	// public static final boolean CONDENSE = false;
	public static boolean VERBOSE = false;

	protected final int prior;
	protected final double sigma;
	protected final double epsilon = 0.1; // You can't actually set this at present
	/** label indices - for all possible label sequences - for each feature */
	protected final List< Index< CRFLabel > > labelIndices;
	protected final Map< String, Integer > classIndex; // didn't have <String> before. Added since that's what is assumed everywhere.
	protected final double[ ][ ] Ehat; // empirical counts of all the features [feature][class]
	protected final double[ ][ ] E;
	protected double[ ][ ][ ] parallelE;
	protected double[ ][ ][ ] parallelEhat;

	protected final int window;
	protected final int numClasses;
	// public static Index<String> featureIndex;  // no idea why this was here [cdm 2013]
	protected final int[ ] map;
	protected int[ ][ ][ ][ ] data; // data[docIndex][tokenIndex][][]
	protected double[ ][ ][ ][ ] featureVal; // featureVal[docIndex][tokenIndex][][]
	protected int[ ][ ] labels; // labels[docIndex][tokenIndex]
	protected final int domainDimension;
	// protected double[][] eHat4Update, e4Update;

	protected int[ ][ ] weightIndices;
	protected final String backgroundSymbol;

	protected int[ ][ ] featureGrouping = null;

	protected static final double smallConst = 1e-6;
	// protected static final double largeConst = 5;

	protected Random rand = new Random( 2147483647L );

	protected final int multiThreadGrad;
	// need to ensure the following two objects are only read during multi-threading
	// to ensure thread-safety. It should only be modified in calculate() via setWeights()
	protected double[ ][ ] weights;

	protected LinearCliquePotentialFunction cliquePotentialFunc;

	public CRFLogConditionalObjectiveFunction( int[ ][ ][ ][ ] data,
	        int[ ][ ] labels,
	        int window,
	        Map< String, Integer > classIndex,
	        List< Index< CRFLabel > > labelIndices,
	        int[ ] map,
	        String priorType,
	        String backgroundSymbol,
	        double sigma,
	        double[ ][ ][ ][ ] featureVal,
	        int multiThreadGrad ){

		this.window = window;
		this.classIndex = classIndex;
		this.numClasses = classIndex.size();
		this.labelIndices = labelIndices;
		this.map = map;
		this.data = data;
		this.featureVal = featureVal;
		this.labels = labels;
		this.prior = getPriorType( priorType );
		this.backgroundSymbol = backgroundSymbol;
		this.sigma = sigma;
		this.multiThreadGrad = multiThreadGrad;
		// takes docIndex, returns Triple<prob, E, dropoutGrad>
		Ehat = empty2D();
		E = empty2D();
		weights = empty2D();
		empiricalCounts( Ehat );
		int myDomainDimension = 0;
		for ( int dim : map ) {
			myDomainDimension += labelIndices.get( dim ).size();
		}
		domainDimension = myDomainDimension;
	}

	protected void empiricalCounts( double[ ][ ] eHat ) {
		for ( int m = 0; m < data.length; m++ ) {
			empiricalCountsForADoc( eHat, m );
		}
	}

	protected void empiricalCountsForADoc( double[ ][ ] eHat, int docIndex ) {
		int[ ][ ][ ] docData = data[ docIndex ];
		int[ ] docLabels = labels[ docIndex ];
		int[ ] windowLabels = new int[ window ];
		Arrays.fill( windowLabels, classIndex.get( backgroundSymbol ) );
		double[ ][ ][ ] featureValArr = null;
		if ( featureVal != null )
			featureValArr = featureVal[ docIndex ];

		if ( docLabels.length > docData.length ) { // only true for self-training
			// fill the windowLabel array with the extra docLabels
			System.arraycopy( docLabels, 0, windowLabels, 0, windowLabels.length );
			// shift the docLabels array left
			int[ ] newDocLabels = new int[ docData.length ];
			System.arraycopy( docLabels, docLabels.length - newDocLabels.length, newDocLabels, 0, newDocLabels.length );
			docLabels = newDocLabels;
		}
		for ( int i = 0; i < docData.length; i++ ) {
			//Caminha cada posicao
			//0 0 0 0
			//0 0 0 1
			//0 0 1 1
			//TODO Brow-Joe
			System.arraycopy( windowLabels, 1, windowLabels, 0, window - 1 );
			windowLabels[ window - 1 ] = docLabels[ i ];
			for ( int j = 0; j < docData[ i ].length; j++ ) {
				int[ ] cliqueLabel = new int[ j + 1 ];
				System.arraycopy( windowLabels, window - 1 - j, cliqueLabel, 0, j + 1 );
				CRFLabel crfLabel = new CRFLabel( cliqueLabel );
				int labelIndex = labelIndices.get( j ).indexOf( crfLabel );
				//System.err.println(crfLabel + " " + labelIndex);
				for ( int n = 0; n < docData[ i ][ j ].length; n++ ) {
					double fVal = 1.0;
					if ( featureValArr != null && j == 0 ) // j == 0 because only node features gets feature values
						fVal = featureValArr[ i ][ j ][ n ];
					eHat[ docData[ i ][ j ][ n ] ][ labelIndex ] += fVal;
				}
			}
		}
	}

	public static int getPriorType( String priorTypeStr ) {
		if ( priorTypeStr == null )
			return QUADRATIC_PRIOR; // default
		if ( "QUADRATIC".equalsIgnoreCase( priorTypeStr ) ) {
			return QUADRATIC_PRIOR;
		} else if ( "HUBER".equalsIgnoreCase( priorTypeStr ) ) {
			return HUBER_PRIOR;
		} else if ( "QUARTIC".equalsIgnoreCase( priorTypeStr ) ) {
			return QUARTIC_PRIOR;
		} else if ( "DROPOUT".equalsIgnoreCase( priorTypeStr ) ) {
			return DROPOUT_PRIOR;
		} else if ( "NONE".equalsIgnoreCase( priorTypeStr ) ) {
			return NO_PRIOR;
		} else if ( priorTypeStr.equalsIgnoreCase( "lasso" ) || priorTypeStr.equalsIgnoreCase( "ridge" ) || priorTypeStr.equalsIgnoreCase( "gaussian" ) || priorTypeStr.equalsIgnoreCase( "ae-lasso" ) || priorTypeStr.equalsIgnoreCase( "sg-lasso" ) || priorTypeStr.equalsIgnoreCase( "g-lasso" ) ) {
			return NO_PRIOR;
		} else {
			throw new IllegalArgumentException( "Unknown prior type: " + priorTypeStr );
		}
	}

	protected double[ ][ ] empty2D() {
		double[ ][ ] d = new double[ map.length ][ ];
		// int index = 0;
		for ( int i = 0; i < map.length; i++ ) {
			d[ i ] = new double[ labelIndices.get( map[ i ] ).size() ];
		}
		return d;
	}

	public double[ ] initial() {
		return initial( rand );
	}

	public double[ ] initial( Random randGen ) {
		double[ ] initial = new double[ domainDimension() ];
		for ( int i = 0; i < initial.length; i++ ) {
			initial[ i ] = randGen.nextDouble() + smallConst;
			// initial[i] = generator.nextDouble() * largeConst;
			// initial[i] = -1+2*(i);
			// initial[i] = (i == 0 ? 1 : 0);
		}
		return initial;
	}

	public int domainDimension() {
		return domainDimension;
	}

	protected double[ ] derivative;
	double[ ] lastX;
	int fEvaluations;

	public double[ ] derivativeAt( double[ ] x ) {
		ensure( x );
		return derivative;
	}

	public void ensure( double[ ] x ) {
		if ( Arrays.equals( x, lastX ) ) {
			return;
		}
		if ( lastX == null ) {
			lastX = new double[ domainDimension() ];
		}
		if ( derivative == null ) {
			derivative = new double[ domainDimension() ];
		}
		copy( lastX, x );
		fEvaluations += 1;
		calculate( x );
	}

	protected static void copy( double[ ] copy, double[ ] orig ) {
		System.arraycopy( orig, 0, copy, 0, orig.length );
	}

	protected double value;

	public void calculate( double[ ] x ) {
		double prob = 0.0;
		to2D( x, weights );
		setWeights( weights );
		clear2D( E );
		prob = regularGradientAndValue();

		if ( Double.isNaN( prob ) ) { // shouldn't be the case
			throw new RuntimeException( "Got NaN for prob in CRFLogConditionalObjectiveFunction.calculate()" + " - this may well indicate numeric underflow due to overly long documents." );
		}

		value = -prob;

		int index = 0;
		for ( int i = 0; i < E.length; i++ ) {
			for ( int j = 0; j < E[ i ].length; j++ ) {
				// because we minimize -L(\theta)
				derivative[ index ] = ( E[ i ][ j ] - Ehat[ i ][ j ] );
				index++;
			}
		}

		applyPrior( x, 1.0 );

	}

	protected void applyPrior( double[ ] x, double batchScale ) {
		// incorporate priors
		if ( prior == QUADRATIC_PRIOR ) {
			double lambda = 1 / ( sigma * sigma );
			for ( int i = 0; i < x.length; i++ ) {
				double w = x[ i ];
				value += batchScale * w * w * lambda * 0.5;
				derivative[ i ] += batchScale * w * lambda;
			}
		} else if ( prior == HUBER_PRIOR ) {
			double sigmaSq = sigma * sigma;
			for ( int i = 0; i < x.length; i++ ) {
				double w = x[ i ];
				double wabs = Math.abs( w );
				if ( wabs < epsilon ) {
					value += batchScale * w * w / 2.0 / epsilon / sigmaSq;
					derivative[ i ] += batchScale * w / epsilon / sigmaSq;
				} else {
					value += batchScale * ( wabs - epsilon / 2 ) / sigmaSq;
					derivative[ i ] += batchScale * ( ( w < 0.0 ) ? -1.0 : 1.0 ) / sigmaSq;
				}
			}
		} else if ( prior == QUARTIC_PRIOR ) {
			double sigmaQu = sigma * sigma * sigma * sigma;
			double lambda = 1 / 2.0 / sigmaQu;
			for ( int i = 0; i < x.length; i++ ) {
				double w = x[ i ];
				value += batchScale * w * w * w * w * lambda;
				derivative[ i ] += batchScale * w / sigmaQu;
			}
		}
	}

	private double regularGradientAndValue() {
		int[ ][ ][ ] docData = data[ 0 ];

		CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree( docData, labelIndices, numClasses, weights, classIndex, window );
		double prob = documentLogProbability( docData, cliqueTree );

		documentExpectedCounts( docData, cliqueTree );

		return prob;
	}

	private double documentLogProbability( int[ ][ ][ ] docData, CRFCliqueTree cliqueTree ) {
		int[ ] docLabels = labels[ 0 ];
		int[ ] given = new int[ window - 1 ];

		int backgroundIndex = classIndex.get( Constantes.DEFAULT_SYMBOL );
		Arrays.fill( given, backgroundIndex );
		double startPosLogProb = cliqueTree.logProbStartPos( backgroundIndex );
		double prob = startPosLogProb;

		for ( int i = 0; i < docData.length; i++ ) {
			int label = docLabels[ i ];
			double p = cliqueTree.condLogProbGivenPrevious( i, label, given );
			prob += p;
			System.arraycopy( given, 1, given, 0, given.length - 1 );
			given[ given.length - 1 ] = label;
		}

		return prob;
	}

	protected void documentExpectedCounts( int[ ][ ][ ] docData, CRFCliqueTree cliqueTree ) {
		for ( int i = 0; i < docData.length; i++ ) {
			for ( int j = 0; j < docData[ i ].length; j++ ) {
				Index< CRFLabel > labelIndex = labelIndices.get( j );
				for ( int k = 0, liSize = labelIndex.size(); k < liSize; k++ ) {
					int[ ] label = labelIndex.get( k ).getLabel();
					double p = cliqueTree.prob( i, label );
					for ( int n = 0; n < docData[ i ][ j ].length; n++ ) {
						double fVal = 1.0;
						E[ docData[ i ][ j ][ n ] ][ k ] += p * fVal;
					}
				}
			}
		}
	}

	public void to2D( double[ ] weights1D, double[ ][ ] newWeights ) {
		to2D( weights1D, this.labelIndices, this.map, newWeights );
	}

	public static void to2D( double[ ] weights, List< Index< CRFLabel > > labelIndices, int[ ] map, double[ ][ ] newWeights ) {
		int index = 0;
		for ( int i = 0; i < map.length; i++ ) {
			int labelSize = labelIndices.get( map[ i ] ).size();
			try {
				System.arraycopy( weights, index, newWeights[ i ], 0, labelSize );
			} catch ( Exception ex ) {
				System.err.println( "weights: " + Arrays.toString( weights ) );
				System.err.println( "newWeights[" + i + "]: " + Arrays.toString( newWeights[ i ] ) );
				throw new RuntimeException( ex );
			}
			index += labelSize;
		}
	}

	public void setWeights( double[ ][ ] weights ) {
		this.weights = weights;
		cliquePotentialFunc = new LinearCliquePotentialFunction( weights );
	}

	public static void clear2D( double[ ][ ] arr2D ) {
		for ( int i = 0; i < arr2D.length; i++ )
			for ( int j = 0; j < arr2D[ i ].length; j++ )
				arr2D[ i ][ j ] = 0;
	}

	public double valueAt( double[ ] x ) {
		ensure( x );
		return value;
	}

}
