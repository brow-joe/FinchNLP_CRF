package br.com.jonathan.crf.classifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import br.com.jonathan.crf.dto.CRFCliqueTree;
import br.com.jonathan.crf.dto.CRFDatum;
import br.com.jonathan.crf.dto.CRFLabel;
import br.com.jonathan.crf.dto.Clique;
import br.com.jonathan.crf.dto.Span;
import br.com.jonathan.crf.dto.Token;
import br.com.jonathan.crf.dto.Triple;
import br.com.jonathan.crf.factory.FeatureFactory;
import br.com.jonathan.crf.factory.GeneratorFeatureFactory;
import br.com.jonathan.crf.indexer.Index;
import br.com.jonathan.crf.model.CRFModel;
import br.com.jonathan.crf.utils.Constantes;
import br.com.jonathan.crf.utils.Serializer;

public class CRFClassifier{

	private final CRFModel model;

	public CRFClassifier( String path ){
		Serializer< CRFModel > serializer = new Serializer< CRFModel >();
		this.model = serializer.get( path );
	}

	public List< Span > classify( String texto ) {
		List< Token > documents = getTokens( texto );
		Triple< int[ ][ ][ ], int[ ], double[ ][ ][ ] > triple = documentToDataAndLabels( documents );

		//Params
		int[ ][ ][ ] data = triple.first;
		List< Index< CRFLabel > > labelIndices = model.getLabelIndices();
		Map< String, Integer > classIndex = model.getClassIndex();
		int numClasses = classIndex.size();
		int windowSize = model.getWindowSize();
		double[ ][ ] weights = model.getWeights();

		CRFCliqueTree cliqueTree = CRFCliqueTree.getCalibratedCliqueTree( data, labelIndices, numClasses, weights, classIndex, windowSize );

		SequenceModel sequence = new SequenceModel( cliqueTree );

		ExactBestSequenceFinder extractFindes = new ExactBestSequenceFinder();
		int[ ] bestSequence = extractFindes.bestSequence( sequence );
		Map< Integer, String > reverseClassIndex = getClassIndexReverse();

		int start = 0;
		int end = 0;

		Map< String, List< Span > > spans = new HashMap< >();
		for ( int j = 0, docSize = documents.size(); j < docSize; j++ ) {
			String text = documents.get( j ).getText();
			String guess = reverseClassIndex.get( bestSequence[ j + windowSize - 1 ] );

			end = start + text.length();

			//Set span by type
			Span span = getSpan( start, end, guess, cliqueTree, j, classIndex.get( guess ) );
			List< Span > value = spans.get( guess );
			if ( value == null ) {
				value = new LinkedList< >();
			}
			value.add( span );
			spans.put( guess, value );

			start = end + 1;
		}

		return normalizeAndMerge( spans );
	}

	private List< Span > normalizeAndMerge( Map< String, List< Span > > spans ) {
		List< Span > retorno = new LinkedList< >();

		Map< Span, Integer > merge = new HashMap< >();

		for ( Entry< String, List< Span > > byType : spans.entrySet() ) {
			List< Span > spanList = byType.getValue();

			Span anterior = null;
			for ( Span span : spanList ) {

				retorno.add( span );
				if ( anterior != null ) {
					if ( anterior.getEnd() + 1 == span.getStart() ) {

						Integer quantidade = merge.get( anterior );
						if ( quantidade == null ) {
							//Cache + 1
							quantidade = 2;
						} else {
							quantidade += 1;
						}

						merge.remove( anterior );

						double prob = anterior.getProb() + span.getProb();
						anterior = new Span( anterior.getStart(), span.getEnd(), byType.getKey(), prob );
						merge.put( anterior, quantidade );

						continue;
					}
				}

				anterior = span;
			}
		}

		for ( Entry< Span, Integer > entry : merge.entrySet() ) {
			Span span = entry.getKey();

			double prob = span.getProb() / entry.getValue();
			Span spanMerge = new Span( span.getStart(), span.getEnd(), span.getType(), prob );
			retorno.add( spanMerge );
		}

		return retorno;
	}

	private Span getSpan( int start, int end, String type, CRFCliqueTree cliqueTree, int position, int label ) {
		double prob = cliqueTree.prob( position, label );
		Span span = new Span( start, end, type, prob );
		return span;
	}

	private Map< Integer, String > getClassIndexReverse() {
		return model.getClassIndex().entrySet().stream().collect( Collectors.toMap( Entry::getValue, Entry::getKey ) );
	}

	private Triple< int[ ][ ][ ], int[ ], double[ ][ ][ ] > documentToDataAndLabels( List< Token > documents ) {
		List< FeatureFactory > featureFactory = GeneratorFeatureFactory.createFeaturesFactory( model.getShape() );

		int docSize = documents.size();

		int windowSize = model.getWindowSize();
		int[ ][ ][ ] data = new int[ docSize ][ windowSize ][ ];

		double[ ][ ][ ] featureVals = new double[ docSize ][ windowSize ][ ];
		int[ ] labels = new int[ docSize ];

		AtomicInteger position = new AtomicInteger( 0 );
		for ( int i = 0; i < documents.size(); i++ ) {
			CRFDatum< List< String >, CRFLabel > doc = makeDatum( documents, position.getAndIncrement(), featureFactory );

			List< List< String > > features = doc.getFeatures();

			for ( int index = 0; index < features.size(); index++ ) {
				Collection< String > cliqueFeatures = filter( features.get( index ) );
				data[ i ][ index ] = new int[ cliqueFeatures.size() ];

				int m = 0;
				for ( String feature : cliqueFeatures ) {
					int indexFeature = model.getFeatureIndex().indexOf( feature );
					data[ i ][ index ][ m ] = indexFeature;
					m++;
				}
			}

			String answer = i > -1 && i < documents.size() ? documents.get( i ).getType() : Constantes.DEFAULT_SYMBOL;
			Integer label = model.getClassIndex().get( answer );
			labels[ i ] = label == null ? -1 : label.intValue();
		}

		return new Triple< int[ ][ ][ ], int[ ], double[ ][ ][ ] >( data, labels, featureVals );
	}

	private Collection< String > filter( List< String > list ) {

		Predicate< String > predicate = new Predicate< String >(){
			@Override
			public boolean test( String t ) {
				return model.getFeatureIndex().indexOf( t ) > 0;
			}
		};

		return list.stream().filter( predicate ).collect( Collectors.toList() );
	}

	public CRFDatum< List< String >, CRFLabel > makeDatum( List< Token > tokens, int position, List< FeatureFactory > featureFactory ) {
		Collection< Clique > done = new HashSet< >();
		ArrayList< List< String > > features = new ArrayList< List< String > >();

		int windowSize = model.getWindowSize();
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

			Integer label = model.getClassIndex().get( answer );
			labels[ i ] = label == null ? -1 : label.intValue();
		}

		CRFDatum< List< String >, CRFLabel > retorno = new CRFDatum< List< String >, CRFLabel >( features, new CRFLabel( labels ) );
		return retorno;
	}

	private List< Token > getTokens( String texto ) {
		List< Token > tokens = new LinkedList< >();
		StringTokenizer tokenizer = new StringTokenizer( texto );
		while ( tokenizer.hasMoreTokens() ) {
			String token = tokenizer.nextToken();
			tokens.add( new Token( token.trim(), null ) );
		}
		return tokens;
	}

}
