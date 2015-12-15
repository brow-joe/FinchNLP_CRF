package br.com.jonathan.crf.indexer;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

public interface Index< E > extends Iterable< E >, Serializable{

	public abstract int size();

	public abstract E get( int i );

	public abstract int indexOf( E o );

	public abstract int addToIndex( E o );

	@Deprecated
	public abstract int indexOf( E o, boolean add );

	public List< E > objectsList();

	public Collection< E > objects( int[ ] indices );

	public boolean isLocked();

	public void lock();

	public void unlock();

	public void saveToWriter( Writer out ) throws IOException;

	public void saveToFilename( String s );

	public boolean contains( Object o );

	public boolean add( E e );

	public boolean addAll( Collection< ? extends E > c );

	public void clear();

}