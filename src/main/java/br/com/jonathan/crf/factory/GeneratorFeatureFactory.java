package br.com.jonathan.crf.factory;

import java.util.Arrays;
import java.util.List;

import br.com.jonathan.crf.feature.NERFeatureFactory;
import br.com.jonathan.crf.feature.WordShapeClassifier;

public class GeneratorFeatureFactory{

	public static List< FeatureFactory > createFeaturesFactory( String shape ) {
		return Arrays.asList( new NERFeatureFactory( new WordShapeClassifier( shape ) ) );
	}

}