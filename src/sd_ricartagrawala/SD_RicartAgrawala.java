/*
	UFSCar - Campus Sorocaba
	Sistemas Distribuídos - prof. Fábio
	
	Rafael Brandão Barbosa Fairbanks 552372
	Filipe Santos Rocchi 552194
 */
package sd_ricartagrawala;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class SD_RicartAgrawala {
	
	public static void main(String[] args) throws InterruptedException
	{
		int NUM_THREADS = 3;
		
		for(int i = 0; i < NUM_THREADS; i++)
			new Processo(i);
		
		System.out.println("MAIN: Starting PROCESS threads.");
		for(Processo p : Processo.processos)
			p.start();
		
		Thread.sleep(1000);
		
		for(Processo p : Processo.processos)
			p.initAuth = true;
		
		Processo.processos.get(0).setQueroUsar("recurso1");
		Processo.processos.get(1).setQueroUsar("recurso1");
		Processo.processos.get(2).setQueroUsar("recurso1");
		Processo.processos.get(1).setQueroUsar("recurso2");
		
		for(Processo p : Processo.processos)
			p.join();
		
		Thread.sleep(1000);
	}
}