/*
	UFSCar - Campus Sorocaba
	Sistemas Distribuídos - prof. Fábio
	
	Rafael Brandão Barbosa Fairbanks 552372
	Filipe Santos Rocchi 552194
 */
package sd_ricartagrawala;

import java.util.ArrayList;


public class Mensagem {
	public Integer idRemetente; // número do processo remetente
	public Integer tempoRemetente; 
	public String conteudo; // agora guarda o recurso

	public int id; // para fins de impressão apenas
	
	//ArrayList<Integer> missingAcks;
	
	public Mensagem(int idrem, int i, Integer t, String c){
		idRemetente = idrem;
		tempoRemetente = t;
		
		conteudo = c;

		id = i; // cria id única
		//missingAcks = new ArrayList<>();
	}

	public Mensagem(Mensagem m){
		idRemetente = m.idRemetente;
		tempoRemetente = m.tempoRemetente;
		conteudo = m.conteudo;

		id = m.id;
		
		//for(Integer i : m.missingAcks)
		//	missingAcks.add(i);
	}
	
	public void imprimir(){
		System.out.println("MSG "+id+": Imprimindo mensagem{id:"+id+", t:"+tempoRemetente+"}: " + conteudo);
	}
}
