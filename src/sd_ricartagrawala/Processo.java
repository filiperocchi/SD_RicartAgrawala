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

	private PriorityQueue<Mensagem> filaMensagens;

	private HashMap<Integer, ArrayList<Integer>> mapMsgIdToOkList; // id da mensagem, lista de ids de processos que faltam dar ok
	//(conjunto de oks faltando, e quando vazio está pronta?)
	
	private Queue<Mensagem> msgToDo;
	private Queue<Ok> oksToDo;

	private int relogioLogico;
	
	public Processo(int i)
	{
		processos.add(this);
		
		idProcesso = i;
		aplicacao = new Aplicacao(idProcesso);

		Comparator<Mensagem> comparador = new ComparadorTempoMsg();
		filaMensagens = new PriorityQueue<Mensagem>(10, comparador);

		mapMsgIdToOkList = new HashMap<Integer, ArrayList<Integer>>();
		
		msgToDo = new ConcurrentLinkedQueue<>();
		oksToDo = new ConcurrentLinkedQueue<>();

		relogioLogico = 0;
	}
	
	public void enviarMensagem(String conteudo)
	{
		relogioLogico++;

		Mensagem msg = criarMensagem(idProcesso, relogioLogico, conteudo);

		//System.out.println("PROCESSO "+idProcesso+": enviando msg "+msg.id);
		
		for(Processo p : processos)
			p.receberMensagem(msg);
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
		//System.out.println("PROCESSO "+idProcesso+": processando msg "+msg.id);
		        
                // REMOVI ISSO AQUI, PODE?
		//relogioLogico = max(relogioLogico, msg.tempoRemetente);

		relogioLogico++;
		
		ArrayList<Integer> listaOksFaltando = new ArrayList<>();
		for(Processo p : processos)
		{
			if(p.idProcesso != idProcesso) // exceto ele mesmo
                            listaOksFaltando.add(p.idProcesso);
		}

		filaMensagens.add(msg); // insere na lista ordenado pelo tempo
		mapMsgIdToOkList.put(msg.id, listaOksFaltando);
		
		enviarOk(msg.id); // multicast ok
		
	}
	
	public void enviarOk(int idMensagem)
	{
		//System.out.println("PROCESSO "+idProcesso+": enviando ok "+idMensagem);
		
		relogioLogico++;

		// simula mandar o ok para todos processos
		for(Processo p : processos)
		{
			if(p.idProcesso != idProcesso) // exceto ele mesmo
				p.receberOk(new Ok(idProcesso, idMensagem));
		}
	}
	
	public void receberOk(Ok a)
	{
		//synchronized(this){
			oksToDo.add(a);
		//}
	}
	
	public void processarOk()
	{
		//?relogioLogico++;
		Ok ok;
		
		//synchronized(this){
			ok = oksToDo.poll();	
		//}
		
		ArrayList<Integer> missingOks = mapMsgIdToOkList.get(ok.idMensagem); // lista de oks faltando da msg que foi ok
		
		if(missingOks != null){
                    missingOks.remove(new Integer(ok.idProcessoEnviandoOk));
                }
	}

	@Override
	public void run()
	{
		System.out.println("Started process "+idProcesso);
		
		while(true)
		{
			while(!msgToDo.isEmpty())
				processarMensagem();
			
			while(!oksToDo.isEmpty())
				processarOk(); // multicast ok
			
			Random rand = new Random();
			
			if(rand.nextInt(50000) < 1)
			{
				enviarMensagem(UUID.randomUUID().toString());
				//System.out.println("PROCESSO "+idProcesso+": enviando msg");
			}
			
			if(filaMensagens.peek() != null)
			{
				if(mapMsgIdToOkList.get(filaMensagens.peek().id).isEmpty())
				{
					relogioLogico++;
					if(idProcesso == 0)
					{
						System.out.println("PROCESSO "+idProcesso+": (relogio "+relogioLogico+") Executando msg idRem: "+filaMensagens.peek().idRemetente+" id: "+filaMensagens.peek().id+" t: "+filaMensagens.peek().tempoRemetente);
						//aplicacao.send(filaMensagens.peek());
					
						//filaMensagens.peek().imprimir(); // executa cabeça da fila, "mensagem é executada na aplicação"
					}
					
					mapMsgIdToOkList.remove(filaMensagens.peek().id);
					filaMensagens.poll();
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