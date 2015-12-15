package br.com.jonathan.crf.function;

public class LinearCliquePotentialFunction{
	private final double[ ][ ] weights;

	LinearCliquePotentialFunction( double[ ][ ] weights ){
		this.weights = weights;
	}

	public double computeCliquePotential( int cliqueSize, int labelIndex, int[ ] cliqueFeatures, double[ ] featureVal, int posInSent ) {
		double output = 0.0;
		for ( int m = 0; m < cliqueFeatures.length; m++ ) {
			double dotProd = weights[ cliqueFeatures[ m ] ][ labelIndex ];
			if ( featureVal != null ) {
				dotProd *= featureVal[ m ];
			}
			output += dotProd;
		}
		return output;
	}
}
