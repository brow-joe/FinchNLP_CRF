
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import br.com.jonathan.crf.classifier.CRFClassifier;
import br.com.jonathan.crf.dto.Span;
import br.com.jonathan.crf.trainer.CRFTrainer;
import br.com.jonathan.crf.utils.Constantes;

public class Main{

	public static void main( String[ ] args ) {
		String texto = "<START:Pessoa> Marcelo <END> foi trabalhar em <START:Cidade> Piratininga <END>";

		//TODO teste txt
		/*StringBuffer sb = new StringBuffer();
		Path p = Paths.get( "data/corpus_AMAZONIA.txt" );
		try (Stream< String > lines = Files.lines( p )) {
			lines.forEach( s -> sb.append( s ) );
		} catch ( IOException ex ) {
			ex.printStackTrace();
		}
		texto = new String( sb.toString().getBytes(), Charset.forName( "UTF-8" ) );
		texto = texto.replaceAll( "<", " <" ).replaceAll( ">", "> " ).replaceAll( "  +", " " ).replaceAll( "=", "" );//*/

		String path = "data/treiner.ser";
		CRFTrainer crfTrainer = new CRFTrainer( texto, path );
		crfTrainer.treinar();

		texto = "Juliana trabalha em Ubatuba";
		//texto = "depois se encontram com a dissidência do grupo , os Bacamarteiros de Pinga Fogo";

		CRFClassifier classifier = new CRFClassifier( path );

		List< Span > spans = classifier.classify( texto );

		for ( Span span : spans ) {
			if ( !StringUtils.equals( span.getType(), Constantes.DEFAULT_SYMBOL ) ) {
				System.out.println( span.getCoveredText( texto ) + " - " + span.getType() + " : " + span.getProb() );
			}
		}

	}

}
