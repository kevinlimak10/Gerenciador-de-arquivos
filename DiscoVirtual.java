
/*Autores:

	Kevin Lima
	Rodrigo Ortiz
	Edmarcio Borges
	Ederson Hahn
 */
import java.io.FileInputStream;
import java.io.IOException;

public class DiscoVirtual {

	DirEntry[] dirAtual;
	int aux;
	Driver d;

	int clusterSize = 240; // 240 bytes
	int quantClusters = 2160; // 2160 clusters

	byte[] buffer;
	int[] vetFAT;

	public DiscoVirtual() {

		d = new Driver(clusterSize, quantClusters);
		buffer = new byte[clusterSize * 20]; // buffer com espaço para 20 clusters
		vetFAT = new int[quantClusters];
		dirAtual = new DirEntry[10];
	}

	public DirEntry[] getDirEntryAnterior(DirEntry[] dir) {
		return dir[10].pasta;
	}

	public void inicializaFAT() {
		// seta valores iniciais na vetFAT
		vetFAT[0] = 0; // cluster de boot
		for (int i = 1; i < 19; i++) {
			vetFAT[i] = i + 1; // encadeamento dos clusters da FAT
		}
		vetFAT[19] = 0; // ultimo cluster da FAT
		vetFAT[20] = 0; // cluster do diretorio raiz

		for (int i = 21; i < 2160; i++) {
			vetFAT[i] = 9999; // clusters livres
		}
		// copia vetFAT para disco (clusters)
	}

	public void writeBufferOnDisk(int clusterInicial, int quantosClusters) {
		byte[] cluster = new byte[clusterSize];
		for (int qClusters = 0; qClusters < quantosClusters; qClusters++) {
			for (int i = 0; i < clusterSize; i++) {
				cluster[i] = buffer[qClusters * clusterSize + 1];
			}
			d.writeCluster(cluster, clusterInicial + qClusters);
		}

	}

	public void importaArquivo(String nomeArquivo, DirEntry[] dir) {
		try {
			FileInputStream fis = new FileInputStream(nomeArquivo);

			byte byteLido;
			int tam = 0;
			while ((byteLido = (byte) fis.read()) != -1) {
				buffer[tam++] = byteLido;
			}
			fis.close();

			int pos = nomeArquivo.indexOf(".");
			String nome = nomeArquivo.substring(0, pos);
			String ext = nomeArquivo.substring(pos + 1);
			try {
				criaArquivo(nome, ext, (short) tam, dir);
			} catch (DiscFullException disc) {
				System.out.println("disco cheio");
			}
		} catch (IOException e) {
			System.out.println("ERRO AO LER ARQUIVO: ");
		}
	}

	public void atualizaDisco() {

		atualizaFATnoDisco();
		try {
			// copia vetFAT para buffer

			for (int x = 0; x < 106; x++) {
				int repete = 21;
				int j = 0;
				for (int i = 0; i < quantClusters; i++) {
					buffer[j] = (byte) (vetFAT[i] / 256);
					buffer[j + 1] = (byte) (vetFAT[i] - buffer[j] * 256);
					j += 2;
				}
				repete += 20;
				// grava buffer no disco
				writeBufferOnDisk(repete, 20); // grava 20 clusters a partir do cluster repete
			}
		} catch (Exception e)

		{
			System.out.println("erro atualiza disco");
		}

		buffer = new byte[clusterSize * 20];// deixa zerado

		atualizaRaiz();
	}

	public void atualizaRaiz() {
		int j = 0;
		for (int i = 0; i < dirAtual.length; i++) {
			if (dirAtual[i] != null) {
				for (int x = 0; x < 16 && x < dirAtual[i].nome.length(); x++) {// pegar nome
					buffer[j] = (byte) dirAtual[i].nome.charAt(x);
					j++;
				}
				if (dirAtual[i].type == 'A')
					for (int k = 0; k < 3; k++) {// pegar extençao
						buffer[j] = (byte) dirAtual[i].ext.charAt(k);
						j++;
					}
				buffer[j] = (byte) dirAtual[i].type; // pegar tipo
				j++;

				buffer[j] = (byte) (dirAtual[i].inicio / 256); // pegar cluster inicio
				buffer[j + 1] = (byte) (dirAtual[i].inicio - buffer[j] * 256);
				j += 2;

				if (dirAtual[i].type == 'A') { // pegar tamanho
					buffer[j] = (byte) (dirAtual[i].tamanho / 256);
					buffer[j + 1] = (byte) (dirAtual[i].tamanho - buffer[j] * 256);
					j += 2;
				}
			}
		}
		writeBufferOnDisk(20, 1);

		buffer = new byte[clusterSize * 20]; // deixa zerado
	}

	public void criaArquivo(String nome, String ext, short tamanho, DirEntry[] dir) throws DiscFullException {

		if (!isFullCluster()) {
			short primeiro = (short) achaPrimeiro();
			for (int i = 0; i < dir.length; i++) {
				if (dir[i] == null) {
					dir[i] = new DirEntry(nome, ext, (short) primeiro, tamanho);
					if (dir[i].qtClusterUsado(tamanho) > qtClusterLivres())
						throw new DiscFullException();
					for (int j = 0; j < dir[i].qtClusterUsado(tamanho); j++)
						if (j == dir[i].qtClusterUsado(tamanho) - 1)
							vetFAT[achaVazio()] = 0;
						else {
							int temp = achaVazio();
							vetFAT[achaVazio()] = 0;
							vetFAT[temp] = achaVazio();
						}
					break;
				}
			}
			atualizaDisco();
		}
	}

	public void apagaArquivo(int cluster) {// recebe o cluster inicial e apaga o arquivo
		int prox = vetFAT[cluster];
		if (vetFAT[prox] == 0) {
			vetFAT[cluster] = 9999;
			vetFAT[prox] = 9999;
			atualizaDisco();
			return;
		}

		vetFAT[cluster] = 9999;
		apagaArquivo(prox);
	}

	public void novaPasta(String nome, DirEntry[] dir) {
		short primeiro = (short) achaPrimeiro();
		for (int i = 0; i < dir.length; i++) {
			if (dir[i] == null) {
				dir[i] = new DirEntry(nome, (short) primeiro);
				vetFAT[primeiro] = 0;
				break;
			}
		}
	}

	private int achaVazio() {
		for (int i = 0; i < 2160; i++)
			if (vetFAT[i] == 9999)
				return i;
		return -1;

	}

	private int achaPrimeiro() {
		for (int i = 0; i < 2160; i++)
			if (vetFAT[i] == 9999)
				return i;
		return -1;
	}

	public boolean isFullCluster() { // mudei aqui para clusterfull
		for (int i = 0; i < this.vetFAT.length; i++)
			if (vetFAT[i] == 9999)
				return false;
		return true;
	}

	public boolean isFullDiretorio() {
		for (int i = 0; i < dirAtual.length; i++)
			if (dirAtual[i] == null)
				return false;
		return true;
	}

	public int calculaEspacoVazio() {
		int cont = 0;
		int porcentagem = 0;
		for (int i = 21; i < 2160; i++)
			if (vetFAT[i] == 9999)
				cont++;
		porcentagem = cont * 100 / 2140;
		return porcentagem;
	}

	private void atualizaFATnoDisco() {
		// copia vetFAT para buffer
		int j = 0;
		for (int i = 0; i < quantClusters; i++) {
			buffer[j] = (byte) (vetFAT[i] / 256);
			buffer[j + 1] = (byte) (vetFAT[i] - buffer[j] * 256);
			j += 2;
		}
		// grava buffer no disco
		writeBufferOnDisk(1, 19); // grava 19 clusters a partir do cluster 1
	}

	/* retorna a inicial da fat onde estava */
	public int remove(String element, DirEntry[] dir) {
		int retorna = 0;
		int temp = 0;
		for (int i = 0; i < dir.length; i++)
			if (dir[i].nome.equals(element)) {
				temp = dir[i].inicio;
				retorna = dir[i].inicio;
				dir[i] = null;
				while (this.vetFAT[temp] != 0) {
					int exclui = temp;
					temp = vetFAT[temp];
					vetFAT[exclui] = 9999;
				}
				this.vetFAT[temp] = 9999;
				atualizaDisco();
				return retorna;
			}
		return -1;
	}

	public int qtArquivosEPastas() {
		int soma = 0;
		for (int i = 0; i < dirAtual.length; i++)
			if (dirAtual[i] != null) {
				soma++;
			}
		return soma;
	}

	public void imprimePasta(DirEntry[] dir) {
		for (int i = 0; i < dir.length; i++) {
			if (dir[i] != null && dir[i].type == 'D') {
				System.out.println("nome: " + dir[i].nome + " tipo: " + dir[i].type);
			}
		}
	}

	public void visualizaArquivo(DirEntry[] dir, String arq) {
		for (int i = 0; i < dir.length; i++)
			if (dir[i] != null && (dir[i].nome.equals(arq) && dir[i].type == 'A')) {
				System.out.println("nome: " + dir[i].nome);
				System.out.println("extenção: " + dir[i].ext);
				System.out.println("tipo: Arquivo");
				System.out.println("tamanho: " + dir[i].tamanho);
			}
	}

	public DirEntry[] getPasta(DirEntry[] dir, String nome) {
		for (int i = 0; i < dir.length; i++) {
			if (dir[i] != null && dir[i].type == 'D')
				if (dir[i].nome.equalsIgnoreCase(nome))
					return dir[i].pasta;
		}
		return null;
	}

	public void movePasta(DirEntry[] diretorio, DirEntry[] dir) {
		Teclado t = new Teclado();
		for (int i = 0; i < dir.length; i++) {
			if (dir[i] == null || dir[i].type == 'D') {
				imprimePasta(dir);
				if (dir[i] == null) {
					char opcao = t.leChar("colar nessa pasta? S / N");
					if (opcao == 'S' || opcao == 's')
						dir[i].pasta = diretorio;
				}

				else {
					String pasta;
					boolean decision = false;
					do {
						pasta = t.leString("digite a pasta que deseja acessar ou exit para sair");
						if (getPasta(dir, pasta) != null)
							decision = true;
						if (pasta.equalsIgnoreCase("return"))
							decision = true;
					} while (!(decision));
					if (!(pasta.equalsIgnoreCase("return")))
						movePasta(diretorio, getPasta(dir, pasta));
				}
			}
		}
	}


	public DirEntry[] buscaPastaPai(DirEntry[] dir, DirEntry[] search) {
		DirEntry[] retorno = search;

		for (int i = 0; i < search.length; i++) {
			if (search[i] != null)
				if (search[i].pasta != null)
					if (search[i].pasta == dir) {
						return search;
					} else {
						return buscaPastaPai(dir, search[i].pasta);
					}
		}
		return retorno;
	}

	public void acessaPasta(DirEntry[] dir) throws StopException {
		Teclado t = new Teclado();
		String opcao;
		imprimePasta(dir);

		do {
			opcao = menu();
			if (opcao.equalsIgnoreCase("acessa")) {
				imprimePasta(dir);
				boolean decision = false;
				String diretorio;
				do {
					diretorio = t.leString("digite a pasta que deseja acessar ou exit para sair");
					if (getPasta(dir, diretorio) != null)
						decision = true;
					if (diretorio.equalsIgnoreCase("exit"))
						decision = true;
				} while (!(decision));
				if (!(diretorio.equalsIgnoreCase("exit"))) {
					System.out.println("\n \n pasta atual: " + diretorio);
					acessaPasta(getPasta(dir, diretorio));
				}
			}

			if (opcao.equalsIgnoreCase("criaArq")) {
				importaArquivo(t.leString("digite no nome para ser importado"), dir);
			}
			if (opcao.equalsIgnoreCase("criaPasta")) {
				novaPasta(t.leString("digite no nome da pasta"), dir);
			}
			if (opcao.equalsIgnoreCase("visualizar")) {

				String arquivo = t.leString("digite o nome do arquivo");
				visualizaArquivo(dir, arquivo);
			}
			if (opcao.equalsIgnoreCase("visualizarElementos")) {
				imprimeArquivos(dir);
				imprimePasta(dir);
			}
			if (opcao.equalsIgnoreCase("movePaste")) {
				imprimePasta(dir);
				String diretorio;
				do {
					diretorio = t.leString("digite a pasta que deseja mover");
				} while (getPasta(dir, diretorio) == null);
				movePasta(getPasta(dir, diretorio), this.dirAtual);
			}
			if (opcao.equalsIgnoreCase("sobePai")) {
				if (dir != dirAtual) {
					String diretorio;
					do {
						diretorio = t.leString("digite a pasta que deseja mover");
					} while (getPasta(dir, diretorio) != null);
					DirEntry[] pasta = (getPasta(dir, diretorio));
					remove(diretorio, dir);
					DirEntry[] muda = buscaPastaPai(pasta, dirAtual);
					for (int i = 0; i < muda.length; i++) {
						if (muda[i] == null) {
							muda[i].pasta = pasta;
						}
					}
				}
			}

			if (opcao.equalsIgnoreCase("deleteArq")) {
				String arq;
				imprimeArquivos(dir);
				do {
					arq = t.leString("digite a pasta que deseja deletar");
				} while (verificaArquvios(arq, dir));
				remove(arq, dir);
			}

			if (opcao.equalsIgnoreCase("deletePaste")) {
				String diretorio;
				imprimePasta(dir);
				do {
					diretorio = t.leString("digite a pasta que deseja deletar");
				} while (getPasta(dir, diretorio) == null);
				remove(diretorio, dir);
			}

			if (opcao.equalsIgnoreCase("copyarq")) {
				String arq;
				imprimeArquivos(dir);
				do {
					arq = t.leString("digite o arquivo que deseja mover");
				} while (verificaArquvios(arq, dir));
				moveArq(getArq(arq, dir), dir);
			}

			if (opcao.equalsIgnoreCase("copyPaste")) {
				String pasta;
				imprimePasta(dir);
				do {
					pasta = t.leString("digite a pasta que deseja copiar");
				} while (getPasta(dir, pasta) == null);
				movePasta(getPasta(dir, pasta), dir);
			}

			if (opcao.equalsIgnoreCase("mostraDados")) {
				System.out.println("quantidade de bytes livres: " + calculaBytesLivres());
				System.out.println("quantidade de arquivos e pastas: " + qtArquivosEPastas());
				System.out.println("quantidade de clusters livres: " + qtClusterLivres());
			}

			if (opcao.equalsIgnoreCase("parar")){
				atualizaDisco();
				throw new StopException();
			}
		} while (!(opcao.equalsIgnoreCase("EXIT")));
	}

	private void moveArq(DirEntry arq, DirEntry[] dir) {
		Teclado t = new Teclado();
		for (int i = 0; i < dir.length; i++) {
			if (dir[i] == null || dir[i].type == 'D') {
				imprimePasta(dir);
				if (dir[i] == null) {
					char opcao = t.leChar("colar nessa pasta? S / N");
					if (opcao == 'S' || opcao == 's')
						dir[i] = arq;
				}

				else {
					String pasta;
					do {

						pasta = t.leString("digite a pasta que deseja acessar ou return para voltar");
					} while (getPasta(dir, pasta) != null || pasta.equalsIgnoreCase("return"));
					if (!(pasta.equalsIgnoreCase("return"))) {
						moveArq(arq, getPasta(dir, pasta));
					}
				}
			}
		}
	}

	private DirEntry getArq(String arq, DirEntry[] dir) {
		for (int i = 0; i < dir.length; i++) {
			if (dir[i].nome.equalsIgnoreCase(arq))
				return dir[i];
		}
		return null;
	}

	private boolean verificaArquvios(String arq, DirEntry[] dir) {
		for (int i = 0; i < dir.length; i++) {
			if (dir[i] != null)
				if (dir[i].nome.equalsIgnoreCase(arq))
					return true;
		}
		return false;
	}

	public void fechaDisco() {
		atualizaFATnoDisco();
		d.closeDisk();
	}

	public void imprime() {
		for (int i = 0; i < 2160; i++)
			System.out.println(vetFAT[i]);
	}

	public void imprimeArquivos(DirEntry[] dir) {
		for (int i = 0; i < dir.length; i++) {
			if (dir[i] != null && dir[i].type == 'A') {
				System.out.println("nome: " + dir[i].nome + " tipo: " + dir[i].type);
			}
		}
	}

	/* calcula os bytes ocupaveis nos cluster */
	public int calculaBytesLivres() { // um cluster quando ocupado por um aqruivo não pode ser utilizado por outro
		int livre = 0;
		for (int j = 0; j < 2160; j++) {
			if (vetFAT[j] == 9999) {
				livre += 240;
			}
		}
		return livre;
	}

	/* calcula a quantidade de clusters já alocados */
	public int quantidadeCluster() {
		int soma = 0;
		for (int i = 0; i < 2160; i++) {
			if (vetFAT[i] != 9999)
				soma++;
		}
		return soma;

	}

	/* clusters livres */
	public int qtClusterLivres() {
		int cont = 0;
		for (int i = 21; i < 2160; i++)
			if (vetFAT[i] == 9999)
				cont++;
		return cont;
	}

	public String menu() {
		Teclado t = new Teclado();

		mostraComandos();

		return t.leString();

	}

	private void mostraComandos() {

		System.out.println("se houver pasta anterior para acessa-lá,digite exit");
		System.out.println("para acessar uma pasta do diretorio atual,digite Acessa");

		System.out.println("para criar novo arquivo no diretorio atual, digite: criaArq");
		System.out.println("para criar uma novo pasta no diretorio atual, digite: criaPasta");

		System.out.println("para visualizar um arquivo, digite: visualizar");
		System.out.println("para visualizar os elementos da pasta, digite: visualizarElementos");
		System.out.println("para mover pasta, digite: movePaste");

		System.out.println("para remover um elemento, digite: deleteArq");
		System.out.println("para remover uma pasta, digite : deletePaste");

		System.out.println("para copiar um arquivo, digite: copyarq");
		System.out.println("para copiar uma pasta, digite: copyPaste");

		System.out.println("para mostrar dados no hd, digite: mostraDados");

		System.out.println("para encerar o programa digite parar");

	}
}
