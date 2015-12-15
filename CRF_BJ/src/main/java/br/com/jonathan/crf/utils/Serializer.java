package br.com.jonathan.crf.utils;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serializer< T > {

	public void save( String serializePath, T save ) {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream( new BufferedOutputStream( new FileOutputStream( serializePath ) ) );
			oos.writeObject( save );
			System.err.println( "done." );

		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			try {
				oos.close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}

	}

	public T get( String path ) {
		FileInputStream fileStream = null;
		ObjectInputStream stream = null;
		T t = null;
		try {
			fileStream = new FileInputStream( path );
			stream = new ObjectInputStream( fileStream );
			Object object = stream.readObject();
			t = extracted( object );
		} catch ( IOException | ClassNotFoundException e ) {
			System.err.println( "Erro ao obter o objeto serializado! " + e.getMessage() );
			e.printStackTrace();
		} finally {
			try {
				stream.close();
				fileStream.close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
		return t;
	}

	@SuppressWarnings( "unchecked" )
	private T extracted( Object object ) {
		return (T) object;
	}

}
