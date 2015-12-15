package br.com.jonathan.crf.factory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import br.com.jonathan.crf.dto.Clique;
import br.com.jonathan.crf.dto.Token;

public abstract class FeatureFactory implements Serializable{
	private static final long serialVersionUID = 1L;

	public static final Clique cliqueC = Clique.valueOf( new int[ ] { 0 } );
	public static final Clique cliqueCpC = Clique.valueOf( new int[ ] { -1, 0 } );
	public static final Clique cliqueCp2C = Clique.valueOf( new int[ ] { -2, 0 } );
	public static final Clique cliqueCp3C = Clique.valueOf( new int[ ] { -3, 0 } );
	public static final Clique cliqueCp4C = Clique.valueOf( new int[ ] { -4, 0 } );
	public static final Clique cliqueCp5C = Clique.valueOf( new int[ ] { -5, 0 } );
	public static final Clique cliqueCpCp2C = Clique.valueOf( new int[ ] { -2, -1, 0 } );
	public static final Clique cliqueCpCp2Cp3C = Clique.valueOf( new int[ ] { -3, -2, -1, 0 } );
	public static final Clique cliqueCpCp2Cp3Cp4C = Clique.valueOf( new int[ ] { -4, -3, -2, -1, 0 } );
	public static final Clique cliqueCpCp2Cp3Cp4Cp5C = Clique.valueOf( new int[ ] { -5, -4, -3, -2, -1, 0 } );
	public static final Clique cliqueCnC = Clique.valueOf( new int[ ] { 0, 1 } );
	public static final Clique cliqueCpCnC = Clique.valueOf( new int[ ] { -1, 0, 1 } );

	public static final List< Clique > knownCliques = Arrays.asList( cliqueC, cliqueCpC, cliqueCp2C, cliqueCp3C, cliqueCp4C, cliqueCp5C, cliqueCpCp2C, cliqueCpCp2Cp3C, cliqueCpCp2Cp3Cp4C, cliqueCpCp2Cp3Cp4Cp5C, cliqueCnC, cliqueCpCnC );

	public static List< Clique > getCliques( int maxLeft, int maxRight ) {
		List< Clique > cliques = new ArrayList< Clique >();
		knownCliques.stream().forEach( clique -> {
			if ( -clique.maxLeft() <= maxLeft && clique.maxRight() <= maxRight ) {
				cliques.add( clique );
			}
		} );
		return cliques;
	}

	public abstract Collection< String > getCliqueFeatures( int position, Clique clique, List< Token > tokens );

}