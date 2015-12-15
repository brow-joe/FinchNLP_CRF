package br.com.jonathan.crf.indexer;

import java.io.IOException;
import java.io.Writer;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.Semaphore;

public class HashIndex< E > extends AbstractCollection< E >implements Index< E >, RandomAccess{
	private static final long serialVersionUID = 1L;

	private final List< E > objects;
	private final Map< E, Integer > indexes;
	private boolean locked;

	public HashIndex(){
		super();
		objects = new ArrayList< E >();
		indexes = new HashMap< >();
	}

	@Override
	public void clear() {
		objects.clear();
		indexes.clear();
	}

	public int[ ] indices( Collection< E > elements ) {
		int[ ] indices = new int[ elements.size() ];
		int i = 0;
		for ( E elem : elements ) {
			indices[ i++ ] = indexOf( elem );
		}
		return indices;
	}

	@Override
	public E get( int i ) {
		if ( i < 0 || i >= objects.size() )
			throw new ArrayIndexOutOfBoundsException( "Index " + i + " outside the bounds [0," + size() + ")" );
		return objects.get( i );
	}

	@Override
	public int indexOf( E o ) {
		Integer index = indexes.get( o );
		if ( index == null ) {
			return -1;
		}
		return index;
	}

	private final Semaphore semaphore = new Semaphore( 1 );

	@Override
	public int addToIndex( E o ) {
		Integer index = indexes.get( o );
		if ( index == null ) {
			if ( !locked ) {
				try {
					semaphore.acquire();
					index = indexes.get( o );
					if ( index == null ) {
						index = objects.size();
						objects.add( o );
						indexes.put( o, index );
					}
					semaphore.release();
				} catch ( InterruptedException e ) {
					throw new RuntimeException( e );
				}
			} else {
				return -1;
			}
		}
		return index;
	}

	@Override
	public int indexOf( E o, boolean add ) {
		if ( add ) {
			return addToIndex( o );
		} else {
			return indexOf( o );
		}
	}

	@Override
	public List< E > objectsList() {
		return objects;
	}

	@Override
	public Collection< E > objects( final int[ ] indices ) {
		return new AbstractList< E >(){
			@Override
			public E get( int index ) {
				return objects.get( indices[ index ] );
			}

			@Override
			public int size() {
				return indices.length;
			}
		};
	}

	@Override
	public boolean isLocked() {
		return locked;
	}

	@Override
	public void lock() {
		locked = true;
	}

	@Override
	public void unlock() {
		locked = false;
	}

	@Override
	public void saveToWriter( Writer out ) throws IOException {

	}

	@Override
	public void saveToFilename( String s ) {

	}

	@Override
	public Iterator< E > iterator() {
		return null;
	}

	@Override
	public int size() {
		return objects.size();
	}

	@Override
	public boolean addAll( Collection< ? extends E > c ) {
		boolean changed = false;
		for ( E element : c ) {
			changed |= add( element );
		}
		return changed;
	}

	@Override
	public boolean add( E o ) {
		Integer index = indexes.get( o );
		if ( index == null && !locked ) {
			index = objects.size();
			objects.add( o );
			indexes.put( o, index );
			return true;
		}
		return false;
	}

	@Override
	public boolean contains( Object o ) {
		return indexes.containsKey( o );
	}

	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder( "[" );
		int sz = objects.size();
		int i;
		for ( i = 0; i < objects.size(); i++ ) {
			E e = objects.get( i );
			buff.append( i ).append( '=' ).append( e );
			if ( i < ( sz - 1 ) )
				buff.append( ',' );
		}
		if ( i < sz )
			buff.append( "..." );
		buff.append( ']' );
		return buff.toString();
	}

	@Override
	public boolean equals( Object o ) {
		if ( this == o )
			return true;
		if ( !( o instanceof HashIndex ) )
			return false;
		HashIndex hashIndex = (HashIndex) o;
		return indexes.equals( hashIndex.indexes ) && objects.equals( hashIndex.objects );

	}

	@Override
	public int hashCode() {
		int result = objects.hashCode();
		result = 31 * result + indexes.hashCode();
		return result;
	}

}