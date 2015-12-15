package br.com.jonathan.crf.dto;

public class Token{

	private String text;
	private String type;

	public Token(){
		super();
	}

	public Token( String text, String type ){
		super();
		this.text = text;
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public void setText( String text ) {
		this.text = text;
	}

	public String getType() {
		return type;
	}

	public void setType( String type ) {
		this.type = type;
	}

}