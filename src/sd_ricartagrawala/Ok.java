/*
	UFSCar - Campus Sorocaba
	Sistemas Distribuídos - prof. Fábio
	
	Rafael Brandão Barbosa Fairbanks 552372
	Filipe Santos Rocchi 552194
 */
package sd_ricartagrawala;

	
public class Ok {
	public int idProcessoEnviandoOk; // para fins de impressão
	public int idMensagem; // id do processo que enviou a mesagem a qual o Ok se refere
	
	public Ok(Integer p, Integer m)
	{
		idProcessoEnviandoOk = p;
		idMensagem = m;
	}
}
