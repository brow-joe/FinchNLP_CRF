package br.com.jonathan.crf.dto;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Triple< T1, T2, T3 > implements Comparable< Triple< T1, T2, T3 > >, Serializable{
	private static final long serialVersionUID = 1L;

	public T1 first;
	public T2 second;
	public T3 third;

	public Triple( T1 first, T2 second, T3 third ){
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public T1 first() {
		return first;
	}

	public T2 second() {
		return second;
	}

	public T3 third() {
		return third;
	}

	public void setFirst( T1 o ) {
		first = o;
	}

	public void setSecond( T2 o ) {
		second = o;
	}

	public void setThird( T3 o ) {
		third = o;
	}

	@Override
	public int compareTo( Triple< T1, T2, T3 > o ) {
		int comp = ( (Comparable< T1 >) first() ).compareTo( o.first() );
		if ( comp != 0 ) {
			return comp;
		} else {
			comp = ( (Comparable< T2 >) second() ).compareTo( o.second() );
			if ( comp != 0 ) {
				return comp;
			} else {
				return ( (Comparable< T3 >) third() ).compareTo( o.third() );
			}
		}
	}

	public List< Object > asList() {
		return Arrays.asList( first, second, third );
	}

	public static < X, Y, Z > Triple< X, Y, Z > makeTriple( X x, Y y, Z z ) {
		return new Triple< X, Y, Z >( x, y, z );
	}

	public boolean equals( Object o ) {

		if ( this == o ) {
			return true;
		}

		if ( !( o instanceof Triple ) ) {
			return false;
		}

		final Triple triple = (Triple) o;

		if ( first != null ? !first.equals( triple.first ) : triple.first != null ) {
			return false;
		}
		if ( second != null ? !second.equals( triple.second ) : triple.second != null ) {
			return false;
		}
		if ( third != null ? !third.equals( triple.third ) : triple.third != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = ( first != null ? first.hashCode() : 0 );
		result = 29 * result + ( second != null ? second.hashCode() : 0 );
		result = 29 * result + ( third != null ? third.hashCode() : 0 );
		return result;
	}

}