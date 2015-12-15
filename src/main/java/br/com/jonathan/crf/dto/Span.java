package br.com.jonathan.crf.dto;

import java.io.Serializable;

/**
 * 
 * Classe responsável pelo dto do Span
 *
 * @since 3 de dez de 2015 09:20:27
 * @author Jonathan de Souza <jonathansouza@finchsolucoes.com.br>
 * @author Caio Rodrigo Paulucci <caiopaulucci@finchsolucoes.com.br>
 *
 */
public class Span implements Comparable< Span >, Serializable{
	private static final long serialVersionUID = 1L;

	private final int start;
	private final int end;
	private final double prob;
	private final String type;

	public Span( int s, int e, String type ){

		if ( s < 0 ) {
			throw new IllegalArgumentException( "start index must be zero or greater: " + s );
		}
		if ( e < 0 ) {
			throw new IllegalArgumentException( "end index must be zero or greater: " + e );
		}
		if ( s > e ) {
			throw new IllegalArgumentException( "start index must not be larger than end index: " + "start=" + s + ", end=" + e );
		}

		start = s;
		end = e;
		this.type = type;
		this.prob = 0d;
	}

	public Span( int s, int e, String type, double prob ){

		if ( s < 0 ) {
			throw new IllegalArgumentException( "start index must be zero or greater: " + s );
		}
		if ( e < 0 ) {
			throw new IllegalArgumentException( "end index must be zero or greater: " + e );
		}
		if ( s > e ) {
			throw new IllegalArgumentException( "start index must not be larger than end index: " + "start=" + s + ", end=" + e );
		}

		start = s;
		end = e;
		this.prob = prob;
		this.type = type;
	}

	public Span( int s, int e ){
		this( s, e, null, 0d );
	}

	public Span( int s, int e, double prob ){
		this( s, e, null, prob );
	}

	public Span( Span span, int offset ){
		this( span.start + offset, span.end + offset, span.getType(), span.getProb() );
	}

	public Span( Span span, double prob ){
		this( span.start, span.end, span.getType(), prob );
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getType() {
		return type;
	}

	public int length() {
		return end - start;
	}

	public boolean contains( Span s ) {
		return start <= s.getStart() && s.getEnd() <= end;
	}

	public boolean contains( int index ) {
		return start <= index && index < end;
	}

	public boolean startsWith( Span s ) {
		return getStart() == s.getStart() && contains( s );
	}

	public boolean intersects( Span s ) {
		int sstart = s.getStart();
		return this.contains( s ) || s.contains( this ) || getStart() <= sstart && sstart < getEnd() || sstart <= getStart() && getStart() < s.getEnd();
	}

	public boolean crosses( Span s ) {
		int sstart = s.getStart();
		return !this.contains( s ) && !s.contains( this ) && ( getStart() <= sstart && sstart < getEnd() || sstart <= getStart() && getStart() < s.getEnd() );
	}

	public CharSequence getCoveredText( CharSequence text ) {
		if ( getEnd() > text.length() ) {
			throw new IllegalArgumentException( "The span " + toString() + " is outside the given text which has length " + text.length() + "!" );
		}

		return text.subSequence( getStart(), getEnd() );
	}

	public int compareTo( Span s ) {
		if ( getStart() < s.getStart() ) {
			return -1;
		} else if ( getStart() == s.getStart() ) {
			if ( getEnd() > s.getEnd() ) {
				return -1;
			} else if ( getEnd() < s.getEnd() ) {
				return 1;
			} else {
				if ( getType() == null && s.getType() == null ) {
					return 0;
				} else if ( getType() != null && s.getType() != null ) {
					return getType().compareTo( s.getType() );
				} else if ( getType() != null ) {
					return -1;
				}
				return 1;
			}
		} else {
			return 1;
		}
	}

	@Override
	public int hashCode() {
		int res = 23;
		res = res * 37 + getStart();
		res = res * 37 + getEnd();
		if ( getType() == null ) {
			res = res * 37;
		} else {
			res = res * 37 + getType().hashCode();
		}

		return res;
	}

	@Override
	public boolean equals( Object o ) {

		boolean result;

		if ( o == this ) {
			result = true;
		} else if ( o instanceof Span ) {
			Span s = (Span) o;

			result = ( getStart() == s.getStart() ) && ( getEnd() == s.getEnd() ) && ( getType() != null ? type.equals( s.getType() ) : true ) && ( s.getType() != null ? s.getType().equals( getType() ) : true );
		} else {
			result = false;
		}

		return result;
	}

	@Override
	public String toString() {
		StringBuilder toStringBuffer = new StringBuilder( 15 );
		toStringBuffer.append( "[" );
		toStringBuffer.append( getStart() );
		toStringBuffer.append( ".." );
		toStringBuffer.append( getEnd() );
		toStringBuffer.append( ")" );
		if ( getType() != null ) {
			toStringBuffer.append( " " );
			toStringBuffer.append( getType() );
		}

		return toStringBuffer.toString();
	}

	public static String[ ] spansToStrings( Span[ ] spans, CharSequence s ) {
		String[ ] tokens = new String[ spans.length ];

		for ( int si = 0, sl = spans.length; si < sl; si++ ) {
			tokens[ si ] = spans[ si ].getCoveredText( s ).toString();
		}

		return tokens;
	}

	public static String[ ] spansToStrings( Span[ ] spans, String[ ] tokens ) {
		String[ ] chunks = new String[ spans.length ];
		StringBuilder cb = new StringBuilder();
		for ( int si = 0, sl = spans.length; si < sl; si++ ) {
			cb.setLength( 0 );
			for ( int ti = spans[ si ].getStart(); ti < spans[ si ].getEnd(); ti++ ) {
				cb.append( tokens[ ti ] ).append( " " );
			}
			chunks[ si ] = cb.substring( 0, cb.length() - 1 );
		}
		return chunks;
	}

	public double getProb() {
		return prob;
	}

}