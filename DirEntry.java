/*Autores:
	Kevin Lima
	Rodrigo Ortiz
	Edmarcio Borges
	Ederson Hahn
 */
public class DirEntry{

	String nome;
	String ext;
	char type;
	short inicio;
	short tamanho;
	DirEntry[] pasta;
	
	
	public DirEntry(String nome,String ext,short inicio,short tamanho) {
		this.nome = nome;
		this.ext = ext;
		this.type = 'A';
		this.inicio = inicio;
		this.tamanho = tamanho;
	}
	
	public DirEntry(String nome,short inicio) {
		this.nome = nome;
		this.type = 'D';
		this.inicio = inicio;
		this.pasta = new DirEntry[10];
	}
	
	public int qtClusterUsado(int tamanho) {
	return (tamanho / 240) + 1;	
	}
	
	
}
