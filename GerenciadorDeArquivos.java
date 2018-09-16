/*Autores:
	Kevin Lima
	Rodrigo Ortiz
	Edmarcio Borges
	Ederson Hahn
 */

public class GerenciadorDeArquivos {
	
	public static void main(String args[]) throws DiscFullException {
		
	
	DiscoVirtual disc = new DiscoVirtual();
	
	disc.inicializaFAT();
	
	disc.atualizaDisco();
	try {	
	disc.acessaPasta(disc.dirAtual);
	}catch(StopException e) {
		e.getMessage();
	}
	 finally {
	System.out.println("Espaco do disco livre:  " + disc.calculaEspacoVazio() + "%");
	}
	}
}