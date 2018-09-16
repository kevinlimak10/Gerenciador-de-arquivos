/*Autores:
	Kevin Lima
	Rodrigo Ortiz
	Edmarcio Borges
	Ederson Hahn
 */
public class StopException extends Exception {

	private static final long serialVersionUID = 1L;

	public StopException() {
		System.out.println("fechando programa...");
	}
}