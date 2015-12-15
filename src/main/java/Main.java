
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import br.com.jonathan.crf.classifier.CRFClassifier;
import br.com.jonathan.crf.dto.Span;
import br.com.jonathan.crf.trainer.CRFTrainer;
import br.com.jonathan.crf.utils.Constantes;

public class Main{

	public static void main( String[ ] args ) {
		String texto = "<START:Pessoa> Marcelo <END> foi trabalhar em <START:Cidade> Piratininga <END>";

		String path = "data/treiner.ser";
		CRFTrainer crfTrainer = new CRFTrainer( texto, path );
		crfTrainer.treinar();

		texto = "Juliana trabalha em Ubatuba";
		CRFClassifier classifier = new CRFClassifier( path );

		List< Span > spans = classifier.classify( texto );

		for ( Span span : spans ) {
			if ( !StringUtils.equals( span.getType(), Constantes.DEFAULT_SYMBOL ) ) {
				System.out.println( span.getCoveredText( texto ) + " - " + span.getType() + " : " + span.getProb() );
			}
		}

	}

}