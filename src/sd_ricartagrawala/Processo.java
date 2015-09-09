/*
	UFSCar - Campus Sorocaba
	Sistemas Distribuídos - prof. Fábio
	
	Rafael Brandão Barbosa Fairbanks 552372
	Filipe Santos Rocchi 552194
 */
package sd_ricartagrawala;

import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Processo extends Thread
{
	public static int contadorIdsMensagens;

	public static ArrayList<Processo> processos = new ArrayList<>();
	public int idProcesso;
	private Aplicacao aplicacao;

	private Queue<Mensagem> filaMsgsOksFaltando;
	private HashMap<Integer, ArrayList<Integer>> mapMsgIdToOkList; // id da mensagem, lista de ids de processos que faltam dar ok
	//(conjunto de oks faltando, e quando vazio está pronta?)
	
	private Queue<Mensagem> msgToDo;
	
	private boolean msgQueroJaEnviada;
	private String recursoQueroUsar;
	private String recursoSendoUsado;
	private Queue<Mensagem> msgsEsperando;

	private int relogioLogico;
	
	private long countdown;
	
	public Processo(int i)
	{
		processos.add(this);
		
		idProcesso = i;
		aplicacao = new Aplicacao(idProcesso);

		filaMsgsOksFaltando = new LinkedList<Mensagem>();
		mapMsgIdToOkList = new HashMap<Integer, ArrayList<Integer>>();
		
		msgToDo = new ConcurrentLinkedQueue<>();

		msgQueroJaEnviada = true;
		recursoQueroUsar = "";
		recursoSendoUsado = "";
		msgsEsperando = new LinkedList<>();
		
		relogioLogico = 0+i;
		
		countdown = -1;
	}
	
	public void setQueroUsar(String s)
	{
		recursoQueroUsar = s;
		msgQueroJaEnviada = false;
	}
	
	public void liberarRecursoAtual()
	{
		System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") liberando recurso '"+recursoSendoUsado+"'");
		recursoSendoUsado = "";
		
		for(Mensagem msg : msgsEsperando)
		{
			ArrayList<Integer> missingOks = processos.get(msg.idRemetente).mapMsgIdToOkList.get(msg.id); // lista de oks faltando da msg que foi ok
			if(missingOks != null)
				missingOks.remove(new Integer(idProcesso));
			else
				throw new RuntimeException();
		}
		
		msgsEsperando.clear();
	}
	
	public void enviarMensagemQuero(String nomeRecurso)
	{
		relogioLogico++;
		
		Mensagem msg = criarMensagem(idProcesso, relogioLogico, nomeRecurso);

		filaMsgsOksFaltando.add(msg);

		// CRIAR LISTA DE OKS FALTANDO
		ArrayList<Integer> listaOksFaltando = new ArrayList<>();
		for(Processo p : processos)
		{
			//if(p.idProcesso != idProcesso) // exceto ele mesmo
				listaOksFaltando.add(p.idProcesso);
		}
		
		mapMsgIdToOkList.put(msg.id, listaOksFaltando);
		
		//System.out.println("PROCESSO "+idProcesso+": enviando msg "+msg.id);
		
		for(Processo p : processos)
			p.receberMensagem(msg);

		msgQueroJaEnviada = true;
	}
	public void receberMensagem(Mensagem m)
	{
		//synchronized(this){
			msgToDo.add(new Mensagem(m));
		//}
	}
	
	public void processarMensagem()
	{
		Mensagem msg;
		//synchronized(this){
			msg = msgToDo.poll();
		//}
		
		relogioLogico++;
		
		// SE NÃO ESTOU ACESSANDO O RECURSO
		if(!recursoSendoUsado.equals(msg.conteudo))
		{
			// E NEM QUERO ACESSAR, RESPONDO OK
			if(!recursoQueroUsar.equals(msg.conteudo))
			{
				System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+" recurso '"+msg.conteudo+"' | não está usando o recurso e nem quero acessar");
				
				ArrayList<Integer> missingOks = processos.get(msg.idRemetente).mapMsgIdToOkList.get(msg.id); // lista de oks faltando da msg que foi ok
				if(missingOks != null)
					missingOks.remove(new Integer(idProcesso));
				else
					throw new RuntimeException();
			}
			
			// MAS PRETENDO USAR, COMPARO RELOGIO DA MSG COM O MEU
			else
			{
				// SE EU FOR MENOR, COLOCO MSG NA FILA msgsEsperando
				if(relogioLogico < msg.tempoRemetente)
				{
					System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+" recurso '"+msg.conteudo+"' | não está usando o recurso mas quero acessar, sou menor e enfileirei msg");
					
					msgsEsperando.add(msg);
				}

				// SE EU FOR MAIOR, MANDO OK
				else
				{
					System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+" recurso '"+msg.conteudo+"' | não está usando o recurso mas quero acessar, sou maior e mando ok");
					
					ArrayList<Integer> missingOks = processos.get(msg.idRemetente).mapMsgIdToOkList.get(msg.id); // lista de oks faltando da msg que foi ok
					if(missingOks != null)
						missingOks.remove(new Integer(idProcesso));
					else
						throw new RuntimeException();
				}
			}
		}
		
		// SE ESTOU USANDO O RECURSO, COLOCA NA FILA msgsEsperando
		else
		{
			System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") processando msg ("+msg.tempoRemetente+") "+msg.id+" recurso '"+msg.conteudo+"' | estou usando o recurso, enfileirei msg");
			
			msgsEsperando.add(msg);
		}
	}
	
	@Override
	public void run()
	{
		synchronized(this){
			System.out.println("Started process "+idProcesso);
		}
		
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
							System.out.println(">>CONFLITO DE USO<<");
					}
				}
			
			if(countdown > 0)
				countdown--;
			
			if(countdown == 0)
			{
				liberarRecursoAtual();
				countdown = -1;
			}
			
			while(!msgToDo.isEmpty())
				processarMensagem();
			
			if(!recursoQueroUsar.equals("") && !msgQueroJaEnviada)
			{
				enviarMensagemQuero(recursoQueroUsar);
			}
			
			if(filaMsgsOksFaltando.peek() != null)
			{
				if(mapMsgIdToOkList.get(filaMsgsOksFaltando.peek().id) != null)
				{
					if(mapMsgIdToOkList.get(filaMsgsOksFaltando.peek().id).isEmpty())
					{
						relogioLogico++;

						recursoSendoUsado = recursoQueroUsar;
						recursoQueroUsar = "";
						
						Random rand = new Random();
						
						countdown = 10000000 + rand.nextInt(2000000000);

						System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") Utilizando recurso '"+recursoSendoUsado+"'");

						mapMsgIdToOkList.remove(filaMsgsOksFaltando.peek().id);
						filaMsgsOksFaltando.poll();
					}
					else
					{
						System.out.println(filaMsgsOksFaltando.peek().id + " --- "+mapMsgIdToOkList.get(filaMsgsOksFaltando.peek().id));
					}
				}
				
				else
				{
					throw new RuntimeException("MSG "+filaMsgsOksFaltando.peek().id+" SEM LISTA DE OKS");
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