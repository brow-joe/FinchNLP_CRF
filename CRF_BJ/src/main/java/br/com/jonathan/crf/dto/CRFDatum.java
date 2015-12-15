package br.com.jonathan.crf.dto;

import java.io.Serializable;
import java.util.List;

public class CRFDatum< FEATURE, LABEL > implements Serializable{
	private static final long serialVersionUID = 1L;

	private final List< FEATURE > features;
	private final LABEL label;

	public CRFDatum( List< FEATURE > features, LABEL label ){
		this.features = features;
		this.label = label;
	}

	public LABEL getLabel() {
		return label;
	}

	public List< FEATURE > getFeatures() {
		return features;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( "CRFDatum[\n" );
		sb.append( "    label=" ).append( label ).append( '\n' );
		for ( int i = 0, sz = features.size(); i < sz; i++ ) {
			sb.append( "    features(" ).append( i ).append( "):" ).append( features.get( i ) );
			sb.append( '\n' );
		}
		sb.append( ']' );
		return sb.toString();
	}

	@Override
	public boolean equals( Object o ) {
		CRFDatum d = (CRFDatum) o;
		return features.equals( d.getFeatures() );
	}

	@Override
	public int hashCode() {
		return features.hashCode();
	}

}