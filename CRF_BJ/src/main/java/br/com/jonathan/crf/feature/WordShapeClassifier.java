package br.com.jonathan.crf.feature;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class WordShapeClassifier{
	public static final int NOWORDSHAPE = -1;
	public static final int WORDSHAPEDAN1 = 0;
	public static final int WORDSHAPECHRIS1 = 1;
	public static final int WORDSHAPEDAN2 = 2;
	public static final int WORDSHAPEDAN2USELC = 3;
	public static final int WORDSHAPEDAN2BIO = 4;
	public static final int WORDSHAPEDAN2BIOUSELC = 5;
	public static final int WORDSHAPEJENNY1 = 6;
	public static final int WORDSHAPEJENNY1USELC = 7;
	public static final int WORDSHAPECHRIS2 = 8;
	public static final int WORDSHAPECHRIS2USELC = 9;
	public static final int WORDSHAPECHRIS3 = 10;
	public static final int WORDSHAPECHRIS3USELC = 11;
	public static final int WORDSHAPECHRIS4 = 12;
	public static final int WORDSHAPEDIGITS = 13;

	private static int USE_SHAPE;
	private static Set< String > knownLCWords = new HashSet< >();

	public WordShapeClassifier( String shape ){
		if ( shape == null ) {
			USE_SHAPE = NOWORDSHAPE;
		} else if ( shape.equalsIgnoreCase( "dan1" ) ) {
			USE_SHAPE = WORDSHAPEDAN1;
		} else if ( shape.equalsIgnoreCase( "chris1" ) ) {
			USE_SHAPE = WORDSHAPECHRIS1;
		} else if ( shape.equalsIgnoreCase( "dan2" ) ) {
			USE_SHAPE = WORDSHAPEDAN2;
		} else if ( shape.equalsIgnoreCase( "dan2useLC" ) ) {
			USE_SHAPE = WORDSHAPEDAN2USELC;
		} else if ( shape.equalsIgnoreCase( "dan2bio" ) ) {
			USE_SHAPE = WORDSHAPEDAN2BIO;
		} else if ( shape.equalsIgnoreCase( "dan2bioUseLC" ) ) {
			USE_SHAPE = WORDSHAPEDAN2BIOUSELC;
		} else if ( shape.equalsIgnoreCase( "jenny1" ) ) {
			USE_SHAPE = WORDSHAPEJENNY1;
		} else if ( shape.equalsIgnoreCase( "jenny1useLC" ) ) {
			USE_SHAPE = WORDSHAPEJENNY1USELC;
		} else if ( shape.equalsIgnoreCase( "chris2" ) ) {
			USE_SHAPE = WORDSHAPECHRIS2;
		} else if ( shape.equalsIgnoreCase( "chris2useLC" ) ) {
			USE_SHAPE = WORDSHAPECHRIS2USELC;
		} else if ( shape.equalsIgnoreCase( "chris3" ) ) {
			USE_SHAPE = WORDSHAPECHRIS3;
		} else if ( shape.equalsIgnoreCase( "chris3useLC" ) ) {
			USE_SHAPE = WORDSHAPECHRIS3USELC;
		} else if ( shape.equalsIgnoreCase( "chris4" ) ) {
			USE_SHAPE = WORDSHAPECHRIS4;
		} else if ( shape.equalsIgnoreCase( "digits" ) ) {
			USE_SHAPE = WORDSHAPEDIGITS;
		} else {
			USE_SHAPE = NOWORDSHAPE;
		}
	}

	public String wordShape( String word ) {
		if ( StringUtils.isEmpty( word ) ) {
			return null;
		}
		int ch = word.codePointAt( 0 );
		if ( Character.isLowerCase( ch ) ) {
			knownLCWords.add( word );
		}

		if ( knownLCWords != null && dontUseLC( USE_SHAPE ) ) {
			knownLCWords = null;
		}
		switch ( USE_SHAPE ) {
			case NOWORDSHAPE:
				return word;
			case WORDSHAPEDAN1:
				return wordShapeDan1( word );
			case WORDSHAPECHRIS1:
				return wordShapeChris1( word );
			case WORDSHAPEDAN2:
				return wordShapeDan2( word );
			case WORDSHAPEDAN2USELC:
				return wordShapeDan2( word );
			case WORDSHAPEDAN2BIO:
				return wordShapeDan2Bio( word );
			case WORDSHAPEDAN2BIOUSELC:
				return wordShapeDan2Bio( word );
			case WORDSHAPEJENNY1:
				return wordShapeJenny1( word );
			case WORDSHAPEJENNY1USELC:
				return wordShapeJenny1( word );
			case WORDSHAPECHRIS2:
				return wordShapeChris2( word, false );
			case WORDSHAPECHRIS2USELC:
				return wordShapeChris2( word, false );
			case WORDSHAPECHRIS3:
				return wordShapeChris2( word, true );
			case WORDSHAPECHRIS3USELC:
				return wordShapeChris2( word, true );
			case WORDSHAPECHRIS4:
				return wordShapeChris4( word, false );
			case WORDSHAPEDIGITS:
				return wordShapeDigits( word );
			default:
				throw new IllegalStateException( "Bad WordShapeClassifier" );
		}
	}

	private static String wordShapeDan1( String s ) {
		boolean digit = true;
		boolean upper = true;
		boolean lower = true;
		boolean mixed = true;
		for ( int i = 0; i < s.length(); i++ ) {
			char c = s.charAt( i );
			if ( !Character.isDigit( c ) ) {
				digit = false;
			}
			if ( !Character.isLowerCase( c ) ) {
				lower = false;
			}
			if ( !Character.isUpperCase( c ) ) {
				upper = false;
			}
			if ( ( i == 0 && !Character.isUpperCase( c ) ) || ( i >= 1 && !Character.isLowerCase( c ) ) ) {
				mixed = false;
			}
		}
		if ( digit ) {
			return "ALL-DIGITS";
		}
		if ( upper ) {
			return "ALL-UPPER";
		}
		if ( lower ) {
			return "ALL-LOWER";
		}
		if ( mixed ) {
			return "MIXED-CASE";
		}
		return "OTHER";
	}

	private static String wordShapeChris1( String s ) {
		int length = s.length();
		if ( length == 0 ) {
			return "SYMBOL";
		}

		boolean cardinal = false;
		boolean number = true;
		boolean seenDigit = false;
		boolean seenNonDigit = false;

		for ( int i = 0; i < length; i++ ) {
			char ch = s.charAt( i );
			boolean digit = Character.isDigit( ch );
			if ( digit ) {
				seenDigit = true;
			} else {
				seenNonDigit = true;
			}
			digit = digit || ch == '.' || ch == ',' || ( i == 0 && ( ch == '-' || ch == '+' ) );
			if ( !digit ) {
				number = false;
			}
		}

		if ( !seenDigit ) {
			number = false;
		} else if ( !seenNonDigit ) {
			cardinal = true;
		}

		if ( cardinal ) {
			if ( length < 4 ) {
				return "CARDINAL13";
			} else if ( length == 4 ) {
				return "CARDINAL4";
			} else {
				return "CARDINAL5PLUS";
			}
		} else if ( number ) {
			return "NUMBER";
		}

		boolean seenLower = false;
		boolean seenUpper = false;
		boolean allCaps = true;
		boolean allLower = true;
		boolean initCap = false;
		boolean dash = false;
		boolean period = false;

		for ( int i = 0; i < length; i++ ) {
			char ch = s.charAt( i );
			boolean up = Character.isUpperCase( ch );
			boolean let = Character.isLetter( ch );
			boolean tit = Character.isTitleCase( ch );
			if ( ch == '-' ) {
				dash = true;
			} else if ( ch == '.' ) {
				period = true;
			}

			if ( tit ) {
				seenUpper = true;
				allLower = false;
				seenLower = true;
				allCaps = false;
			} else if ( up ) {
				seenUpper = true;
				allLower = false;
			} else if ( let ) {
				seenLower = true;
				allCaps = false;
			}
			if ( i == 0 && ( up || tit ) ) {
				initCap = true;
			}
		}

		if ( length == 2 && initCap && period ) {
			return "ACRONYM1";
		} else if ( seenUpper && allCaps && !seenDigit && period ) {
			return "ACRONYM";
		} else if ( seenDigit && dash && !seenUpper && !seenLower ) {
			return "DIGIT-DASH";
		} else if ( initCap && seenLower && seenDigit && dash ) {
			return "CAPITALIZED-DIGIT-DASH";
		} else if ( initCap && seenLower && seenDigit ) {
			return "CAPITALIZED-DIGIT";
		} else if ( initCap && seenLower && dash ) {
			return "CAPITALIZED-DASH";
		} else if ( initCap && seenLower ) {
			return "CAPITALIZED";
		} else if ( seenUpper && allCaps && seenDigit && dash ) {
			return "ALLCAPS-DIGIT-DASH";
		} else if ( seenUpper && allCaps && seenDigit ) {
			return "ALLCAPS-DIGIT";
		} else if ( seenUpper && allCaps && dash ) {
			return "ALLCAPS";
		} else if ( seenUpper && allCaps ) {
			return "ALLCAPS";
		} else if ( seenLower && allLower && seenDigit && dash ) {
			return "LOWERCASE-DIGIT-DASH";
		} else if ( seenLower && allLower && seenDigit ) {
			return "LOWERCASE-DIGIT";
		} else if ( seenLower && allLower && dash ) {
			return "LOWERCASE-DASH";
		} else if ( seenLower && allLower ) {
			return "LOWERCASE";
		} else if ( seenLower && seenDigit ) {
			return "MIXEDCASE-DIGIT";
		} else if ( seenLower ) {
			return "MIXEDCASE";
		} else if ( seenDigit ) {
			return "SYMBOL-DIGIT";
		} else {
			return "SYMBOL";
		}
	}

	private static String wordShapeDan2( String s ) {
		StringBuilder sb = new StringBuilder( "WT-" );
		char lastM = '~';
		boolean nonLetters = false;
		int len = s.length();
		for ( int i = 0; i < len; i++ ) {
			char c = s.charAt( i );
			char m = c;
			if ( Character.isDigit( c ) ) {
				m = 'd';
			} else if ( Character.isLowerCase( c ) || c == '_' ) {
				m = 'x';
			} else if ( Character.isUpperCase( c ) ) {
				m = 'X';
			}
			if ( m != 'x' && m != 'X' ) {
				nonLetters = true;
			}
			if ( m != lastM ) {
				sb.append( m );
			}
			lastM = m;
		}
		if ( len <= 3 ) {
			sb.append( ':' ).append( len );
		}
		if ( knownLCWords != null ) {
			if ( !nonLetters && knownLCWords.contains( s.toLowerCase() ) ) {
				sb.append( 'k' );
			}
		}
		return sb.toString();
	}

	private static String wordShapeDan2Bio( String s ) {
		if ( containsGreekLetter( s ) ) {
			return wordShapeDan2( s ) + "-GREEK";
		} else {
			return wordShapeDan2( s );
		}
	}

	private static final Pattern biogreek = Pattern.compile( "alpha|beta|gamma|delta|epsilon|zeta|theta|iota|kappa|lambda|omicron|rho|sigma|tau|upsilon|omega", Pattern.CASE_INSENSITIVE );

	private static boolean containsGreekLetter( String s ) {
		Matcher m = biogreek.matcher( s );
		return m.find();
	}

	private static final String[ ] greek = { "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "theta", "iota", "kappa", "lambda", "omicron", "rho", "sigma", "tau", "upsilon", "omega" };

	private static String wordShapeJenny1( String s ) {
		StringBuilder sb = new StringBuilder( "WT-" );
		char lastM = '~';
		boolean nonLetters = false;
		for ( int i = 0; i < s.length(); i++ ) {
			char c = s.charAt( i );
			char m = c;

			if ( Character.isDigit( c ) ) {
				m = 'd';
			} else if ( Character.isLowerCase( c ) ) {
				m = 'x';
			} else if ( Character.isUpperCase( c ) ) {
				m = 'X';
			}

			for ( String gr : greek ) {
				if ( s.startsWith( gr, i ) ) {
					m = 'g';
					i = i + gr.length() - 1;
					break;
				}
			}

			if ( m != 'x' && m != 'X' ) {
				nonLetters = true;
			}
			if ( m != lastM ) {
				sb.append( m );
			}
			lastM = m;

		}
		if ( s.length() <= 3 ) {
			sb.append( ':' ).append( s.length() );
		}
		if ( knownLCWords != null ) {
			if ( !nonLetters && knownLCWords.contains( s.toLowerCase() ) ) {
				sb.append( 'k' );
			}
		}
		return sb.toString();
	}

	private static final int BOUNDARY_SIZE = 2;

	private static String wordShapeChris2( String s, boolean omitIfInBoundary ) {
		int len = s.length();
		if ( len <= BOUNDARY_SIZE * 2 ) {
			return wordShapeChris2Short( s, len );
		} else {
			return wordShapeChris2Long( s, omitIfInBoundary, len );
		}
	}

	private static String wordShapeChris2Short( String s, int len ) {
		int sbLen = ( knownLCWords != null ) ? len + 1 : len;
		final StringBuilder sb = new StringBuilder( sbLen );
		boolean nonLetters = false;

		for ( int i = 0; i < len; i++ ) {
			char c = s.charAt( i );
			char m = c;
			if ( Character.isDigit( c ) ) {
				m = 'd';
			} else if ( Character.isLowerCase( c ) ) {
				m = 'x';
			} else if ( Character.isUpperCase( c ) || Character.isTitleCase( c ) ) {
				m = 'X';
			}
			for ( String gr : greek ) {
				if ( s.startsWith( gr, i ) ) {
					m = 'g';
					i += gr.length() - 1;
					break;
				}
			}
			if ( m != 'x' && m != 'X' ) {
				nonLetters = true;
			}

			sb.append( m );
		}

		if ( knownLCWords != null ) {
			if ( !nonLetters && knownLCWords.contains( s.toLowerCase() ) ) {
				sb.append( 'k' );
			}
		}
		return sb.toString();
	}

	private static String wordShapeChris2Long( String s, boolean omitIfInBoundary, int len ) {
		final char[ ] beginChars = new char[ BOUNDARY_SIZE ];
		final char[ ] endChars = new char[ BOUNDARY_SIZE ];
		int beginUpto = 0;
		int endUpto = 0;
		final Set< Character > seenSet = new TreeSet< Character >();

		boolean nonLetters = false;

		for ( int i = 0; i < len; i++ ) {
			int iIncr = 0;
			char c = s.charAt( i );
			char m = c;
			if ( Character.isDigit( c ) ) {
				m = 'd';
			} else if ( Character.isLowerCase( c ) ) {
				m = 'x';
			} else if ( Character.isUpperCase( c ) || Character.isTitleCase( c ) ) {
				m = 'X';
			}
			for ( String gr : greek ) {
				if ( s.startsWith( gr, i ) ) {
					m = 'g';
					iIncr = gr.length() - 1;
					break;
				}
			}
			if ( m != 'x' && m != 'X' ) {
				nonLetters = true;
			}

			if ( i < BOUNDARY_SIZE ) {
				beginChars[ beginUpto++ ] = m;
			} else if ( i < len - BOUNDARY_SIZE ) {
				seenSet.add( Character.valueOf( m ) );
			} else {
				endChars[ endUpto++ ] = m;
			}
			i += iIncr;
		}

		int sbSize = beginUpto + endUpto + seenSet.size();
		if ( knownLCWords != null ) {
			sbSize++;
		}
		final StringBuilder sb = new StringBuilder( sbSize );
		sb.append( beginChars, 0, beginUpto );
		if ( omitIfInBoundary ) {
			for ( Character chr : seenSet ) {
				char ch = chr.charValue();
				boolean insert = true;
				for ( int i = 0; i < beginUpto; i++ ) {
					if ( beginChars[ i ] == ch ) {
						insert = false;
						break;
					}
				}
				for ( int i = 0; i < endUpto; i++ ) {
					if ( endChars[ i ] == ch ) {
						insert = false;
						break;
					}
				}
				if ( insert ) {
					sb.append( ch );
				}
			}
		} else {
			for ( Character chr : seenSet ) {
				sb.append( chr.charValue() );
			}
		}
		sb.append( endChars, 0, endUpto );

		if ( knownLCWords != null ) {
			if ( !nonLetters && knownLCWords.contains( s.toLowerCase() ) ) {
				sb.append( 'k' );
			}
		}
		return sb.toString();
	}

	public static String wordShapeChris4( String s ) {
		return wordShapeChris4( s, false );
	}

	private static String wordShapeChris4( String s, boolean omitIfInBoundary ) {
		int len = s.length();
		if ( len <= BOUNDARY_SIZE * 2 ) {
			return wordShapeChris4Short( s, len );
		} else {
			return wordShapeChris4Long( s, omitIfInBoundary, len );
		}
	}

	private static String wordShapeChris4Short( String s, int len ) {
		int sbLen = ( knownLCWords != null ) ? len + 1 : len;
		final StringBuilder sb = new StringBuilder( sbLen );
		boolean nonLetters = false;

		for ( int i = 0; i < len; i++ ) {
			char c = s.charAt( i );
			char m = chris4equivalenceClass( c );
			for ( String gr : greek ) {
				if ( s.startsWith( gr, i ) ) {
					m = 'g';
					i += gr.length() - 1;
					break;
				}
			}
			if ( m != 'x' && m != 'X' ) {
				nonLetters = true;
			}

			sb.append( m );
		}

		if ( knownLCWords != null ) {
			if ( !nonLetters && knownLCWords.contains( s.toLowerCase() ) ) {
				sb.append( 'k' );
			}
		}
		return sb.toString();
	}

	private static char chris4equivalenceClass( final char c ) {
		int type = Character.getType( c );
		if ( Character.isDigit( c ) || type == Character.LETTER_NUMBER || type == Character.OTHER_NUMBER || "Ã¤Â¸â‚¬Ã¤ÂºÅ’Ã¤Â¸â€°Ã¥â€ºâ€ºÃ¤Âºâ€Ã¥â€¦Â­Ã¤Â¸Æ’Ã¥â€¦Â«Ã¤Â¹ï¿½Ã¥ï¿½ï¿½Ã©â€ºÂ¶Ã£â‚¬â€¡Ã§â„¢Â¾Ã¥ï¿½Æ’Ã¤Â¸â€¡Ã¤ÂºÂ¿Ã¥â€¦Â©Ã¢â€”â€¹Ã¢â€”Â¯".indexOf( c ) > 0 ) {
			return 'd';
		} else if ( Character.isLowerCase( c ) ) {
			return 'x';
		} else if ( Character.isUpperCase( c ) || Character.isTitleCase( c ) ) {
			return 'X';
		} else if ( Character.isWhitespace( c ) || Character.isSpaceChar( c ) ) {
			return 's';
		} else if ( type == Character.OTHER_LETTER ) {
			return 'c'; // Chinese characters, etc. without case
		} else if ( type == Character.CURRENCY_SYMBOL ) {
			return '$';
		} else if ( type == Character.MATH_SYMBOL ) {
			return '+';
		} else if ( type == Character.OTHER_SYMBOL || c == '|' ) {
			return '|';
		} else if ( type == Character.START_PUNCTUATION ) {
			return '(';
		} else if ( type == Character.END_PUNCTUATION ) {
			return ')';
		} else if ( type == Character.INITIAL_QUOTE_PUNCTUATION ) {
			return '`';
		} else if ( type == Character.FINAL_QUOTE_PUNCTUATION || c == '\'' ) {
			return '\'';
		} else if ( c == '%' ) {
			return '%';
		} else if ( type == Character.OTHER_PUNCTUATION ) {
			return '.';
		} else if ( type == Character.CONNECTOR_PUNCTUATION ) {
			return '_';
		} else if ( type == Character.DASH_PUNCTUATION ) {
			return '-';
		} else {
			return 'q';
		}
	}

	private static String wordShapeChris4Long( String s, boolean omitIfInBoundary, int len ) {
		StringBuilder sb = new StringBuilder( s.length() + 1 );
		StringBuilder endSB = new StringBuilder( BOUNDARY_SIZE );
		Set< Character > boundSet = new HashSet< >();
		Set< Character > seenSet = new TreeSet< Character >();
		boolean nonLetters = false;
		for ( int i = 0; i < len; i++ ) {
			char c = s.charAt( i );
			char m = chris4equivalenceClass( c );
			int iIncr = 0;
			for ( String gr : greek ) {
				if ( s.startsWith( gr, i ) ) {
					m = 'g';
					iIncr = gr.length() - 1;
					break;
				}
			}
			if ( m != 'x' && m != 'X' ) {
				nonLetters = true;
			}

			if ( i < BOUNDARY_SIZE ) {
				sb.append( m );
				boundSet.add( Character.valueOf( m ) );
			} else if ( i < len - BOUNDARY_SIZE ) {
				seenSet.add( Character.valueOf( m ) );
			} else {
				boundSet.add( Character.valueOf( m ) );
				endSB.append( m );
			}
			i += iIncr;
		}
		for ( Character chr : seenSet ) {
			if ( !omitIfInBoundary || !boundSet.contains( chr ) ) {
				char ch = chr.charValue();
				sb.append( ch );
			}
		}
		sb.append( endSB );

		if ( knownLCWords != null ) {
			if ( !nonLetters && knownLCWords.contains( s.toLowerCase() ) ) {
				sb.append( 'k' );
			}
		}
		return sb.toString();
	}

	private static String wordShapeDigits( final String s ) {
		char[ ] outChars = null;

		for ( int i = 0; i < s.length(); i++ ) {
			char c = s.charAt( i );
			if ( Character.isDigit( c ) ) {
				if ( outChars == null ) {
					outChars = s.toCharArray();
				}
				outChars[ i ] = '9';
			}
		}
		if ( outChars == null ) {
			return s;
		} else {
			return new String( outChars );
		}
	}

	private static boolean dontUseLC( int shape ) {
		return shape == WORDSHAPEDAN2 || shape == WORDSHAPEDAN2BIO || shape == WORDSHAPEJENNY1 || shape == WORDSHAPECHRIS2 || shape == WORDSHAPECHRIS3;
	}

}