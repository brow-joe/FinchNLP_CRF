package br.com.jonathan.crf.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import br.com.jonathan.crf.dto.CRFLabel;
import br.com.jonathan.crf.indexer.Index;

public class CRFModel implements Serializable{
	private static final long serialVersionUID = 1L;

	private List< Index< CRFLabel > > labelIndices;
	private Map< String, Integer > classIndex;
	private Index< String > featureIndex;
	private Integer windowSize;
	private double[ ][ ] weights;
	private String shape;

	public CRFModel( List< Index< CRFLabel > > labelIndices, Map< String, Integer > classIndex, Index< String > featureIndex, Integer windowSize, double[ ][ ] weights, String shape ){
		this.labelIndices = labelIndices;
		this.classIndex = classIndex;
		this.featureIndex = featureIndex;
		this.windowSize = windowSize;
		this.weights = weights;
		this.shape = shape;
	}

	public List< Index< CRFLabel > > getLabelIndices() {
		return labelIndices;
	}

	public void setLabelIndices( List< Index< CRFLabel > > labelIndices ) {
		this.labelIndices = labelIndices;
	}

	public Map< String, Integer > getClassIndex() {
		return classIndex;
	}

	public void setClassIndex( Map< String, Integer > classIndex ) {
		this.classIndex = classIndex;
	}

	public Index< String > getFeatureIndex() {
		return featureIndex;
	}

	public void setFeatureIndex( Index< String > featureIndex ) {
		this.featureIndex = featureIndex;
	}

	public Integer getWindowSize() {
		return windowSize;
	}

	public void setWindowSize( Integer windowSize ) {
		this.windowSize = windowSize;
	}

	public double[ ][ ] getWeights() {
		return weights;
	}

	public void setWeights( double[ ][ ] weights ) {
		this.weights = weights;
	}

	public String getShape() {
		return shape;
	}

	public void setShape( String shape ) {
		this.shape = shape;
	}

}
