package br.com.jonathan.crf.trainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.jonathan.crf.dto.CRFDatum;
import br.com.jonathan.crf.dto.CRFLabel;
import br.com.jonathan.crf.dto.Clique;
import br.com.jonathan.crf.dto.Token;
import br.com.jonathan.crf.dto.Triple;
import br.com.jonathan.crf.factory.FeatureFactory;
import br.com.jonathan.crf.factory.GeneratorFeatureFactory;
import br.com.jonathan.crf.indexer.HashIndex;
import br.com.jonathan.crf.indexer.Index;
import br.com.jonathan.crf.model.CRFModel;
import br.com.jonathan.crf.utils.Constantes;
import br.com.jonathan.crf.utils.Serializer;

public class CRFTrainer{
	private final List< Token > tokens;
	private final Map< String, Integer > classIndex;
	private final String shape = "chris2useLC";

	private final Integer windowSize;
	private final Serializer< CRFModel > serializer;
	private final String path;

	public CRFTrainer( String texto, String path ){
		this.classIndex = new HashMap< >();
		this.classIndex.put( Constantes.DEFAULT_SYMBOL, 0 );

		List< Token > tokens = new SeparadorTags().getTokens( texto );

		tokens.stream().forEach( token -> {
			Integer index = classIndex.get( token.getType() );
			if ( index == null ) {
				index = classIndex.size();
				classIndex.put( token.getType(), index );
			}
		} );

		this.tokens = tokens;
		this.windowSize = 3 + 1;
		this.serializer = new Serializer< CRFModel >();
		this.path = path;
	}

	public List< Token > getTokens() {
		return tokens;
	}

	public Map< String, Integer > getClassIndex() {
		return classIndex;
	}

	public void treinar() {
		List< FeatureFactory > featureFactory = GeneratorFeatureFactory.createFeaturesFactory( shape );

		Index< CRFLabel > labelIndex = new HashIndex< CRFLabel >();

		Set< String >[ ] featureIndices = new HashSet[ windowSize ];
		//Init
		for ( int index = 0; index < windowSize; index++ ) {
			featureIndices[ index ] = new HashSet< String >();
		}

		List< Index< CRFLabel > > labelIndices = new ArrayList< Index< CRFLabel > >( windowSize );
		//Init
		for ( int i = 0; i < windowSize; i++ ) {
			labelIndices.add( new HashIndex< CRFLabel >() );
		}
		//Ref
		Index< CRFLabel > labelRef = labelIndices.get( windowSize - 1 );

		AtomicInteger position = new AtomicInteger( 0 );
		List< CRFDatum< List< String >, CRFLabel > > docs = new LinkedList< >();

		tokens.stream().forEach( token -> {
			CRFDatum< List< String >, CRFLabel > data = makeDatum( token, position.getAndIncrement(), featureFactory );
			labelRef.add( data.getLabel() );
			docs.add( data );

			labelIndex.add( data.getLabel() );
			List< List< String > > featureLists = data.getFeatures();

			for ( int index = 0; index < featureLists.size(); index++ ) {
				Collection< String > cliqueFeatures = featureLists.get( index );
				featureIndices[ index ].addAll( cliqueFeatures );
			}

		} );

		treinar( featureIndices, labelIndex, docs, labelIndices, shape );
	}

	public void treinar( Set< String >[ ] featureIndices, Index< CRFLabel > labelIndex, List< CRFDatum< List< String >, CRFLabel > > docs, List< Index< CRFLabel > > labelIndices, String shape ) {
		int numFeatures = 0;
		for ( int i = 0; i < windowSize; i++ ) {
			numFeatures += featureIndices[ i ].size();
		}

		Index< String > featureIndex = new HashIndex< String >();
		int[ ] map = new int[ numFeatures ];

		for ( int i = 0; i < windowSize; i++ ) {
			Index< Integer > featureIndexMap = new HashIndex< Integer >();

			featureIndex.addAll( featureIndices[ i ] );

			for ( String feature : featureIndices[ i ] ) {
				int index = featureIndex.indexOf( feature );
				map[ index ] = i;
				featureIndexMap.add( index );
			}

		}

		for ( int i = 0; i < labelIndex.size(); i++ ) {
			CRFLabel label = labelIndex.get( i );

			for ( int j = windowSize - 2; j >= 0; j-- ) {
				label = label.getOneSmallerLabel();
				labelIndices.get( j ).add( label );
			}

		}

		Triple< int[ ][ ][ ][ ], int[ ][ ], double[ ][ ][ ][ ] > dataAndLabelsAndFeatureVals = documentsToDataAndLabels( docs, featureIndex );

		TrainerExecutor trainer = new TrainerExecutor( dataAndLabelsAndFeatureVals );

		int numClasses = classIndex.size();
		double[ ] oneDimWeights = trainer.trainWeights( map, labelIndices, windowSize, classIndex.get( Constantes.DEFAULT_SYMBOL ), numClasses, classIndex );
		if ( oneDimWeights != null ) {
			double[ ][ ] weights = to2D( oneDimWeights, labelIndices, map );
			/*int ch = word.codePointAt(0);
			  if (Character.isLowerCase(ch)) {
			    knownLCWords.add(word);*/
			CRFModel model = new CRFModel( labelIndices, classIndex, featureIndex, windowSize, weights, shape );
			this.serializer.save( path, model );
		} else {
			throw new RuntimeException( "Nao foi possivel efetuar o treinamento!" );
		}

	}

	public double[ ][ ] to2D( double[ ] weights, List< Index< CRFLabel > > labelIndices, int[ ] map ) {
		double[ ][ ] newWeights = new double[ map.length ][ ];
		int index = 0;
		for ( int i = 0; i < map.length; i++ ) {
			newWeights[ i ] = new double[ labelIndices.get( map[ i ] ).size() ];
			System.arraycopy( weights, index, newWeights[ i ], 0, labelIndices.get( map[ i ] ).size() );
			index += labelIndices.get( map[ i ] ).size();
		}
		return newWeights;
	}

	private Triple< int[ ][ ][ ][ ], int[ ][ ], double[ ][ ][ ][ ] > documentsToDataAndLabels( List< CRFDatum< List< String >, CRFLabel > > docs, Index< String > featureIndex ) {
		List< int[ ][ ][ ] > data = new ArrayList< int[ ][ ][ ] >();
		List< double[ ][ ][ ] > featureVal = new ArrayList< double[ ][ ][ ] >();
		List< int[ ] > labels = new ArrayList< int[ ] >();

		Triple< int[ ][ ][ ], int[ ], double[ ][ ][ ] > docTriple = documentToDataAndLabels( docs, featureIndex );
		data.add( docTriple.first() );
		labels.add( docTriple.second() );
		int numDatums = docs.size();

		System.err.println( "numClasses: " + classIndex.size() + ' ' + classIndex );
		System.err.println( "numDocuments: " + data.size() );
		System.err.println( "numDatums: " + numDatums );
		System.err.println( "numFeatures: " + featureIndex.size() );

		return new Triple< int[ ][ ][ ][ ], int[ ][ ], double[ ][ ][ ][ ] >( data.toArray( new int[ data.size() ][ ][ ][ ] ), labels.toArray( new int[ labels.size() ][ ] ), featureVal.toArray( new double[ data.size() ][ ][ ][ ] ) );
	}

	private Triple< int[ ][ ][ ], int[ ], double[ ][ ][ ] > documentToDataAndLabels( List< CRFDatum< List< String >, CRFLabel > > docs, Index< String > featureIndex ) {
		int docSize = docs.size();
		int[ ][ ][ ] data = new int[ docSize ][ windowSize ][ ];
		double[ ][ ][ ] featureVals = new double[ docSize ][ windowSize ][ ];
		int[ ] labels = new int[ docSize ];

		for ( int i = 0; i < docs.size(); i++ ) {
			CRFDatum< List< String >, CRFLabel > doc = docs.get( i );
			List< List< String > > features = doc.getFeatures();

			for ( int index = 0; index < features.size(); index++ ) {
				Collection< String > cliqueFeatures = features.get( index );
				data[ i ][ index ] = new int[ cliqueFeatures.size() ];

				int m = 0;
				for ( String feature : cliqueFeatures ) {
					int indexFeature = featureIndex.indexOf( feature );
					data[ i ][ index ][ m ] = indexFeature;
					m++;
				}
			}

			String answer = i > -1 && i < tokens.size() ? tokens.get( i ).getType() : Constantes.DEFAULT_SYMBOL;
			labels[ i ] = classIndex.get( answer );
		}

		return new Triple< int[ ][ ][ ], int[ ], double[ ][ ][ ] >( data, labels, featureVals );
	}

	public CRFDatum< List< String >, CRFLabel > makeDatum( Token token, int position, List< FeatureFactory > featureFactory ) {
		Collection< Clique > done = new HashSet< >();
		ArrayList< List< String > > features = new ArrayList< List< String > >();

		for ( int window = 0; window < windowSize; window++ ) {
			List< String > cliqueFeatures = new ArrayList< String >();
			List< Clique > windowCliques = FeatureFactory.getCliques( window, 0 );
			windowCliques.removeAll( done );
			done.addAll( windowCliques );

			windowCliques.stream().forEach( clique -> {
				featureFactory.stream().forEach( featureGenerator -> {
					cliqueFeatures.addAll( featureGenerator.getCliqueFeatures( position, clique, tokens ) );
				} );
			} );

			features.add( cliqueFeatures );
		}

		int[ ] labels = new int[ windowSize ];
		for ( int i = 0; i < windowSize; i++ ) {
			int pos = position + i - windowSize + 1;
			String answer = pos > -1 && pos < tokens.size() ? tokens.get( pos ).getType() : Constantes.DEFAULT_SYMBOL;
			labels[ i ] = classIndex.get( answer );
		}

		CRFDatum< List< String >, CRFLabel > retorno = new CRFDatum< List< String >, CRFLabel >( features, new CRFLabel( labels ) );
		return retorno;
	}

}