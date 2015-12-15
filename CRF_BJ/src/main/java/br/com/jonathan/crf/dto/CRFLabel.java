package br.com.jonathan.crf.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CRFLabel implements Serializable{
	private static final long serialVersionUID = 1L;

	private final int[ ] label;

	private int hashCode = -1;
	private static final int maxNumClasses = 10;

	public CRFLabel( int[ ] label ){
		this.label = label;
	}

	public int[ ] getLabel() {
		return label;
	}

	public CRFLabel getSmallerLabel( int size ) {
		int[ ] newLabel = new int[ size ];
		System.arraycopy( label, label.length - size, newLabel, 0, size );
		return new CRFLabel( newLabel );
	}

	public CRFLabel getOneSmallerLabel() {
		return getSmallerLabel( label.length - 1 );
	}

	@Override
	public String toString() {
		List< Integer > l = new ArrayList< Integer >();
		for ( int i = 0; i < label.length; i++ ) {
			l.add( Integer.valueOf( label[ i ] ) );
		}
		return l.toString();
	}

	@Override
	public boolean equals( Object o ) {
		if ( !( o instanceof CRFLabel ) ) {
			return false;
		}
		CRFLabel other = (CRFLabel) o;

		if ( other.label.length != label.length ) {
			return false;
		}
		for ( int i = 0; i < label.length; i++ ) {
			if ( label[ i ] != other.label[ i ] ) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		if ( hashCode < 0 ) {
			hashCode = 0;
			for ( int i = 0; i < label.length; i++ ) {
				hashCode *= maxNumClasses;
				hashCode += label[ i ];
			}
		}
		return hashCode;
	}

}