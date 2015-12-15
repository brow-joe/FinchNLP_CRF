package br.com.jonathan.crf.trainer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.jonathan.crf.dto.Token;
import br.com.jonathan.crf.utils.Constantes;

public class SeparadorTags{
	private static final Pattern START_TAG_PATTERN = Pattern.compile( "<START(:([^:>\\s]*))?>" );

	public static List< Token > getTokens( String texto ) {
		StringTokenizer tokenizer = new StringTokenizer( texto );
		List< Token > tokens = new ArrayList< >();

		List< String > parts = new ArrayList< >();
		while ( tokenizer.hasMoreTokens() ) {
			parts.add( tokenizer.nextToken().trim() );
		}

		boolean catchingName = false;
		String nameType = Constantes.DEFAULT_SYMBOL;

		for ( int pi = 0; pi < parts.size(); pi++ ) {
			Matcher startMatcher = START_TAG_PATTERN.matcher( parts.get( pi ) );
			if ( startMatcher.matches() ) {
				if ( catchingName ) {
					System.out.println( "Found unexpected annotation" + " while handling a name sequence: " + parts.get( pi ) );
					System.exit( 0 );
				}

				catchingName = true;
				String nameTypeFromSample = startMatcher.group( 2 );
				if ( nameTypeFromSample != null ) {
					if ( nameTypeFromSample.length() == 0 ) {
						System.out.println( "Missing a name type: " + parts.get( pi ) );
						System.exit( 0 );
					}
					nameType = nameTypeFromSample;
				}
			} else if ( parts.get( pi ).equals( "<END>" ) ) {
				if ( catchingName == false ) {
					System.out.println( "Found unexpected annotation: " + parts.get( pi ) );
				}
				catchingName = false;
				nameType = Constantes.DEFAULT_SYMBOL;
			} else {
				Token token = new Token( parts.get( pi ), nameType );
				tokens.add( token );
			}

		}

		return tokens;
	}

}
