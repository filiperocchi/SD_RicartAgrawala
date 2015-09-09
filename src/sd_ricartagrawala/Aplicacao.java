/*
	UFSCar - Campus Sorocaba
	Sistemas Distribuídos - prof. Fábio
	
	Rafael Brandão Barbosa Fairbanks 552372
	Filipe Santos Rocchi 552194
 */
package sd_ricartagrawala;


public class Aplicacao {
	private int id;
	
	Aplicacao(int i){
		id = i;
	}
	
	public void send(Mensagem mensagem){
		System.out.println("APP "+id+": enviou mensagem para aplicação: "+mensagem.conteudo);
		//mensagem.imprimir();
	}
}
