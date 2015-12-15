package br.com.jonathan.crf.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Clique implements Serializable{
	private static final long serialVersionUID = 1L;

	private int hashCode = -1;

	private final int[ ] relativeIndices;
	protected static final Map< CliqueEqualityWrapper, Clique > interner = new HashMap< >();

	private Clique( int[ ] relativeIndices ){
		this.relativeIndices = relativeIndices;
	}

	public static Clique valueOf( int[ ] relativeIndices ) {
		Clique clique = new Clique( relativeIndices );
		return intern( clique );
	}

	private static Clique intern( Clique clique ) {
		CliqueEqualityWrapper wrapper = new CliqueEqualityWrapper( clique );
		Clique newClique = interner.get( wrapper );
		if ( newClique == null ) {
			interner.put( wrapper, clique );
			newClique = clique;
		}
		return newClique;
	}

	public int maxLeft() {
		return relativeIndices[ 0 ];
	}

	public int maxRight() {
		return relativeIndices[ relativeIndices.length - 1 ];
	}

	private static class CliqueEqualityWrapper{
		private final Clique clique;

		public CliqueEqualityWrapper( Clique clique ){
			this.clique = clique;
		}

		@Override
		public boolean equals( Object o ) {
			if ( !( o instanceof CliqueEqualityWrapper ) ) {
				return false;
			}
			CliqueEqualityWrapper otherC = (CliqueEqualityWrapper) o;
			if ( otherC.clique.relativeIndices.length != clique.relativeIndices.length ) {
				return false;
			}
			for ( int i = 0; i < clique.relativeIndices.length; i++ ) {
				if ( clique.relativeIndices[ i ] != otherC.clique.relativeIndices[ i ] ) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			int h = 1;
			for ( int i : clique.relativeIndices ) {
				h *= 17;
				h += i;
			}
			return h;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( '[' );
		for ( int i = 0; i < relativeIndices.length; i++ ) {
			sb.append( relativeIndices[ i ] );
			if ( i != relativeIndices.length - 1 ) {
				sb.append( ", " );
			}
		}
		sb.append( ']' );
		return sb.toString();
	}

	@Override
	public int hashCode() {
		if ( hashCode == -1 ) {
			hashCode = toString().hashCode();
		}
		return hashCode;
	}

}