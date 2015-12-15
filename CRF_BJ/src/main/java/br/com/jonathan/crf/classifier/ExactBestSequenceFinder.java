package br.com.jonathan.crf.classifier;

import java.util.Arrays;

public class ExactBestSequenceFinder{

	//TODO
	public int[ ] bestSequence( SequenceModel sequence ) {
		int length = sequence.length();
		int leftWindow = sequence.leftWindow();
		int rightWindow = sequence.rightWindow();

		int padLength = length + leftWindow + rightWindow;

		int[ ][ ] tags = new int[ padLength ][ ];
		int[ ] tagNum = new int[ padLength ];

		for ( int pos = 0; pos < padLength; pos++ ) {
			tags[ pos ] = sequence.getPossibleValues( pos );
			tagNum[ pos ] = tags[ pos ].length;
		}

		int[ ] tempTags = new int[ padLength ];
		int[ ] productSizes = new int[ padLength ];

		int curProduct = 1;
		for ( int i = 0; i < leftWindow + rightWindow; i++ ) {
			curProduct *= tagNum[ i ];
		}

		for ( int pos = leftWindow + rightWindow; pos < padLength; pos++ ) {
			if ( pos > leftWindow + rightWindow ) {
				curProduct /= tagNum[ pos - leftWindow - rightWindow - 1 ]; // shift off
			}
			curProduct *= tagNum[ pos ]; // shift on
			productSizes[ pos - rightWindow ] = curProduct;
		}

		double[ ][ ] windowScore = new double[ padLength ][ ];
		for ( int pos = leftWindow; pos < leftWindow + length; pos++ ) {
			windowScore[ pos ] = new double[ productSizes[ pos ] ];
			Arrays.fill( tempTags, tags[ 0 ][ 0 ] );

			for ( int product = 0; product < productSizes[ pos ]; product++ ) {
				int p = product;
				int shift = 1;
				for ( int curPos = pos + rightWindow; curPos >= pos - leftWindow; curPos-- ) {
					tempTags[ curPos ] = tags[ curPos ][ p % tagNum[ curPos ] ];
					p /= tagNum[ curPos ];
					if ( curPos > pos ) {
						shift *= tagNum[ curPos ];
					}
				}

				if ( tempTags[ pos ] == tags[ pos ][ 0 ] ) {
					double[ ] scores = sequence.scoresOf( tempTags, pos );
					for ( int t = 0; t < tagNum[ pos ]; t++ ) {
						windowScore[ pos ][ product + t * shift ] = scores[ t ];
					}
				}
			}
		}

		double[ ][ ] score = new double[ padLength ][ ];
		int[ ][ ] trace = new int[ padLength ][ ];
		for ( int pos = 0; pos < padLength; pos++ ) {
			score[ pos ] = new double[ productSizes[ pos ] ];
			trace[ pos ] = new int[ productSizes[ pos ] ];
		}

		for ( int pos = leftWindow; pos < length + leftWindow; pos++ ) {
			for ( int product = 0; product < productSizes[ pos ]; product++ ) {
				if ( pos == leftWindow ) {
					score[ pos ][ product ] = windowScore[ pos ][ product ];
					trace[ pos ][ product ] = -1;
				} else {
					score[ pos ][ product ] = Double.NEGATIVE_INFINITY;
					trace[ pos ][ product ] = -1;
					int sharedProduct = product / tagNum[ pos + rightWindow ];
					int factor = productSizes[ pos ] / tagNum[ pos + rightWindow ];

					for ( int newTagNum = 0; newTagNum < tagNum[ pos - leftWindow - 1 ]; newTagNum++ ) {
						int predProduct = newTagNum * factor + sharedProduct;
						double predScore = score[ pos - 1 ][ predProduct ] + windowScore[ pos ][ product ];

						if ( predScore > score[ pos ][ product ] ) {
							score[ pos ][ product ] = predScore;
							trace[ pos ][ product ] = predProduct;
						}
					}
				}
			}
		}

		double bestFinalScore = Double.NEGATIVE_INFINITY;
		int bestCurrentProduct = -1;
		for ( int product = 0; product < productSizes[ leftWindow + length - 1 ]; product++ ) {
			if ( score[ leftWindow + length - 1 ][ product ] > bestFinalScore ) {
				bestCurrentProduct = product;
				bestFinalScore = score[ leftWindow + length - 1 ][ product ];
			}
		}

		int lastProduct = bestCurrentProduct;
		for ( int last = padLength - 1; last >= length - 1 && last >= 0; last-- ) {
			tempTags[ last ] = tags[ last ][ lastProduct % tagNum[ last ] ];
			lastProduct /= tagNum[ last ];
		}

		for ( int pos = leftWindow + length - 2; pos >= leftWindow; pos-- ) {
			int bestNextProduct = bestCurrentProduct;
			bestCurrentProduct = trace[ pos + 1 ][ bestNextProduct ];
			tempTags[ pos - leftWindow ] = tags[ pos - leftWindow ][ bestCurrentProduct / ( productSizes[ pos ] / tagNum[ pos - leftWindow ] ) ];
		}

		return tempTags;
	}

}
