/*
	UFSCar - Campus Sorocaba
	Sistemas Distribuídos - prof. Fábio
	
	Rafael Brandão Barbosa Fairbanks 552372
	Filipe Santos Rocchi 552194
 */
package sd_ricartagrawala;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Processo extends Thread
{
	public static int contadorIdsMensagens;

	public static ArrayList<Processo> processos = new ArrayList<>();
	public int idProcesso;
	
	private Queue<String> filaRecursosRequisitados;

	private Queue<Mensagem> filaMsgsEsperandoOks;
	private HashMap<Integer, ArrayList<Integer>> mapMsgIdToOkList; // id da mensagem, lista de ids de processos que faltam dar ok
	//(conjunto de oks faltando, e quando vazio está pronta?)
	
	private Queue<Mensagem> msgInBuffer;
	
	private String recursoQueroUsar;
	private String recursoSendoUsado;
	private Queue<Mensagem> requestsNegadosEsperando;
	
	private int tsCriticalSection;

	private int relogioLogico;
	
	private long countdown;
	
	public boolean initAuth;
	
	public Processo(int i)
	{
		idProcesso = i;
		
		filaRecursosRequisitados = new LinkedList<>();

		filaMsgsEsperandoOks = new LinkedList<Mensagem>();
		mapMsgIdToOkList = new HashMap<Integer, ArrayList<Integer>>();
		
		msgInBuffer = new ConcurrentLinkedQueue<>();

		recursoQueroUsar = "";
		recursoSendoUsado = "";
		requestsNegadosEsperando = new LinkedList<>();
		
		tsCriticalSection = -1;
		
		relogioLogico = 0+i*9;
		
		countdown = -1;
		
		processos.add(this);
		
		initAuth = false;
	}
	
	public void setQueroUsar(String s)
	{
		filaRecursosRequisitados.add(s);
	}
	
	public void liberarRecursoAtual()
	{
		System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") liberando recurso '"+recursoSendoUsado+"'");
		recursoSendoUsado = "";
		
		for(Mensagem msg : requestsNegadosEsperando)
		{
			ArrayList<Integer> missingOks = processos.get(msg.idRemetente).mapMsgIdToOkList.get(msg.id); // lista de oks faltando da msg que foi ok
			if(missingOks != null)
				missingOks.remove(new Integer(idProcesso));
			else
				throw new RuntimeException();
		}
		
		tsCriticalSection = -1;
		
		requestsNegadosEsperando.clear();
		
		if(recursoQueroUsar.equals("") && recursoSendoUsado.equals("") && !filaRecursosRequisitados.isEmpty())
		{
			enviarMensagemQuero(filaRecursosRequisitados.poll());
		}
	}
	
	public void enviarMensagemQuero(String nomeRecurso)
	{
		relogioLogico++;
		
		Mensagem msg = criarMensagem(idProcesso, relogioLogico, nomeRecurso);

		filaMsgsEsperandoOks.add(msg);

		// CRIAR LISTA DE OKS FALTANDO
		ArrayList<Integer> listaOksFaltando = new ArrayList<>();
		for(Processo p : processos)
		{
			//if(p.idProcesso != idProcesso) // exceto ele mesmo
				listaOksFaltando.add(p.idProcesso);
		}
		
		mapMsgIdToOkList.put(msg.id, listaOksFaltando);
		
		//System.out.println("PROCESSO "+idProcesso+": enviando msg "+msg.id);
		
		recursoQueroUsar = nomeRecurso;
		
		tsCriticalSection = relogioLogico;
		
		for(Processo p : processos)
			p.receberMensagem(msg);
	}
	public void receberMensagem(Mensagem m)
	{
		msgInBuffer.add(new Mensagem(m));
	}
	
	public void processarMensagem()
	{
		Mensagem msg;
		
		msg = msgInBuffer.poll();
		
		relogioLogico++;
		
		// SE NÃO ESTOU ACESSANDO O RECURSO
		if(!recursoSendoUsado.equals(msg.conteudo))
		{
			// E NEM QUERO ACESSAR, RESPONDO OK
			if(!recursoQueroUsar.equals(
					msg.conteudo))
			{
				System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+
								   " recurso '"+msg.conteudo+"' | não está usando o recurso e nem quero acessar");
				
				ArrayList<Integer> missingOks = processos.get(msg.idRemetente).mapMsgIdToOkList.get(msg.id); // lista de oks faltando da msg de request
				if(missingOks != null)
					missingOks.remove(new Integer(idProcesso)); // remove indicação de que esse processo ainda não deu ok
				else
					throw new RuntimeException();
			}
			
			// MAS PRETENDO USAR, COMPARO RELOGIO DA MSG COM O MEU
			else
			{
				// SE EU FOR MENOR, COLOCO MSG NA FILA msgsEsperando
				//if(tsCriticalSection < msg.tempoRemetente)
				if(idProcesso < msg.idRemetente)
				{
					System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+" "
									   + "recurso '"+msg.conteudo+"' | não está usando o recurso mas quero acessar, sou menor e enfileirei msg");
					
					requestsNegadosEsperando.add(msg);
				}

				// SE EU FOR MAIOR, MANDO OK
				else
				{
					System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+" "
									   + "recurso '"+msg.conteudo+"' | não está usando o recurso mas quero acessar, sou maior e mando ok");
					
					ArrayList<Integer> missingOks = processos.get(msg.idRemetente).mapMsgIdToOkList.get(msg.id); // lista de oks faltando da msg que foi ok
					if(missingOks != null)
						missingOks.remove(new Integer(idProcesso));
					else
						throw new RuntimeException();
				}
				
				if(tsCriticalSection == -1)
					throw new RuntimeException();
			}
		}
		
		// SE ESTOU USANDO O RECURSO, COLOCA NA FILA msgsEsperando
		else
		{
			System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+" "
							   + "recurso '"+msg.conteudo+"' | estou usando o recurso, enfileirei msg");
			
			requestsNegadosEsperando.add(msg);
		}
	}
	
	@Override
	public void run()
	{
		synchronized(this){
			System.out.println("Started process "+idProcesso);
		}
		
		while(initAuth == false)
		{System.out.print("");}
		
		while(true)
		{
			for(Processo p1 : processos)
				for(Processo p2 : processos)
				{
					if(p1.idProcesso != p2.idProcesso)
					{
						if(!p1.recursoSendoUsado.equals("") && 
						   !p2.recursoSendoUsado.equals("") && 
						   p1.recursoSendoUsado.equals(p2.recursoSendoUsado))
						{
							throw new RuntimeException(">>CONFLITO DE USO<<");
						}
					}
				}
			
			if(countdown > 0)
			{
				countdown--;
				//System.out.println(countdown);
			}
			
			if(countdown == 0)
			{
				liberarRecursoAtual();
				countdown = -1;
			}
			
			while(!msgInBuffer.isEmpty())
				processarMensagem();
			
			if(recursoQueroUsar.equals("") && recursoSendoUsado.equals("") && !filaRecursosRequisitados.isEmpty())
			{
				enviarMensagemQuero(filaRecursosRequisitados.poll());
			}
			
			if(filaMsgsEsperandoOks.peek() != null)
			{
				if(mapMsgIdToOkList.get(filaMsgsEsperandoOks.peek().id) != null)
				{
					if(mapMsgIdToOkList.get(filaMsgsEsperandoOks.peek().id).isEmpty())
					{
						relogioLogico++;

						recursoSendoUsado = recursoQueroUsar;
						recursoQueroUsar = "";
						
						Random rand = new Random();
						
						countdown = 100000 + rand.nextInt(2000000);

						System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") Utilizando recurso '"+recursoSendoUsado+"'");

						mapMsgIdToOkList.remove(filaMsgsEsperandoOks.peek().id);
						filaMsgsEsperandoOks.poll();
					}
					else
					{
						//System.out.println(filaMsgsOksFaltando.peek().id + " --- "+mapMsgIdToOkList.get(filaMsgsOksFaltando.peek().id));
					}
				}
				
				else
				{
					throw new RuntimeException("MSG "+filaMsgsEsperandoOks.peek().id+" SEM LISTA DE OKS");
				}
			}
		}
	}

	private static synchronized Mensagem criarMensagem(int idCriador, int relogioLogico, String conteudo)
	{
		contadorIdsMensagens++;
		Mensagem m = new Mensagem(idCriador, contadorIdsMensagens, relogioLogico, conteudo); // cria mensagem com relogio e conteudo
		
		return m;
	}
}