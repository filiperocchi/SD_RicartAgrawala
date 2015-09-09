/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sd_ricartagrawala;

import java.util.Comparator;

/**
 *
 * @author Administrator
 */
class ComparadorTempoMsg implements Comparator<Mensagem>
{

	public ComparadorTempoMsg()
	{
	}

	@Override
	public int compare(Mensagem x, Mensagem y)
	{
		if (x.tempoRemetente < y.tempoRemetente)
		{
			return -1;
		}
		
		else if (x.tempoRemetente > y.tempoRemetente)
		{
			return 1;
		}
		
		else
			return 0;
	}

}
