package br.com.jonathan.crf.trainer;

import java.util.List;
import java.util.Map;
import java.util.Random;

import br.com.jonathan.crf.dto.CRFLabel;
import br.com.jonathan.crf.dto.Triple;
import br.com.jonathan.crf.function.CRFLogConditionalObjectiveFunction;
import br.com.jonathan.crf.indexer.Index;
import br.com.jonathan.crf.utils.Constantes;

public class TrainerExecutor{
	private final Triple< int[ ][ ][ ][ ], int[ ][ ], double[ ][ ][ ][ ] > triple;

	protected Random rand = new Random( 2147483647L );
	protected final double smallConst = 1e-6;
	protected final double tolerance = 1.0E-4;
	protected final int qnMem = 25;

	public TrainerExecutor( Triple< int[ ][ ][ ][ ], int[ ][ ], double[ ][ ][ ][ ] > triple ){
		this.triple = triple;
	}

	public double[ ] trainWeights( int[ ] map, List< Index< CRFLabel > > labelIndices, int window, int backgroundIndex, int numClasses, Map< String, Integer > classIndex ) {
		CRFLogConditionalObjectiveFunction func = new CRFLogConditionalObjectiveFunction( triple.first, triple.second, window, classIndex, labelIndices, map, "QUADRATIC", Constantes.DEFAULT_SYMBOL, 1.0, null, 1 );

		double[ ] initialWeights = func.initial();
		System.err.println( "numWeights: " + initialWeights.length );

		Minimizer minimizer = new Minimizer( qnMem, false, 1, 0, 0 );

		return minimizer.minimize( func, tolerance, initialWeights, -1 );
	}

}