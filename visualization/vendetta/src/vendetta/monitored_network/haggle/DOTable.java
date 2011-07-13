/* Haggle testbed
 * Uppsala University
 *
 * Haggle internal release
 *
 * Copyright Haggle
 */

package vendetta.monitored_network.haggle;

import java.util.TreeMap;

/**
 */
public class DOTable {
	private DataObject[]	do_table;
	private TreeMap<String, Integer> ID_mapping;
	private TreeMap<String, Integer> RowID_mapping;
	private TreeMap<String, Integer> NodeID_mapping;
	private int do_is_important_counter;
	
	public DOTable() {
		do_is_important_counter = 0;
		clear_table();
	}
	
	public void setImportantFor(int time)
	{
		if(do_is_important_counter < time)
		{
			do_is_important_counter = time;
		}
	}
	
	public boolean hasImportant()
	{
		return true || do_is_important_counter > 0;
	}
	
	void updateState()
	{
		if(do_is_important_counter > 0)
			do_is_important_counter--;
	}

	
	public void clear_table()
	{
		do_table = new DataObject[0];
		ID_mapping = new TreeMap<String, Integer>();
		RowID_mapping = new TreeMap<String, Integer>();
		NodeID_mapping = new TreeMap<String, Integer>();
	}
	
	public int getLength()
	{
		return do_table.length;
	}
	
	public void compact()
	{
		int i, j;
		
		j = 0;
		for(i = 0; i < do_table.length; i++)
		{
			if(do_table[i].isAlive() || do_table[i].isVisible())
				j++;
		}
		
		if(j != do_table.length)
		{
			if(j > 0)
			{
				DataObject[] tmp = new DataObject[j];
				ID_mapping = new TreeMap<String, Integer>();
				RowID_mapping = new TreeMap<String, Integer>();
				NodeID_mapping = new TreeMap<String, Integer>();
				
				j = 0;
				for(i = 0; i < do_table.length; i++)
				{
					if(do_table[i].isAlive() || do_table[i].isVisible())
					{
						tmp[j] = do_table[i];
						Integer pos = new Integer(j);
						ID_mapping.put(tmp[j].getID(), pos);
						if(tmp[j].getRowID() != null)
							RowID_mapping.put(tmp[j].getRowID(), pos);
						if(tmp[j].isNodeDescription())
						{
							NodeID_mapping.put(tmp[j].getNodeID(), pos);
						}
						j++;
					}
				}
				do_table = tmp;
			}else{
				clear_table();
			}
		}
	}
	
	public void associateNodeID(DataObject dObj)
	{
		int i = indexByRowID(dObj.getRowID());
		if(i != -1)
		{
			Integer pos = new Integer(i);
			NodeID_mapping.put(do_table[i].getNodeID(), pos);
		}
	}
	
	public void ensure_do_is_in_table(String id, String rowid)
	{
		int	i;
		DataObject[] tmp;
		int len;
		
		if(id == null)
			return;
		
		if(do_table != null)
		{
			i = indexByID(id);
			if(i != -1)
			{
				Integer pos = new Integer(i);
				if(do_table[i].getRowID() == null && rowid != null)
				{
					do_table[i].setRowID(rowid);
					RowID_mapping.put(do_table[i].getRowID(), pos);
				}
				return;
			}
		
			tmp = new DataObject[do_table.length + 1];
			len = do_table.length+1;
		}else{
			tmp = new DataObject[1];
			len = 1;
		}
		
		if(do_table != null)
			for(i = 0; i < do_table.length; i++)
			{
				tmp[i] = do_table[i];
			}
		
		tmp[len-1] = new DataObject(id, rowid);
		tmp[len-1].setOwner(this);
		Integer pos = new Integer(len-1);
		ID_mapping.put(tmp[len-1].getID(), pos);
		if(tmp[len-1].getRowID() != null)
			RowID_mapping.put(tmp[len-1].getRowID(), pos);
		
		do_table = tmp;
	}
	
	int indexByID(String id)
	{
		if(id == null)
			return -1;
		
		Integer pos = ID_mapping.get(id);
		if(pos != null)
			return pos.intValue();
		return -1;
	}
	
	int indexByRowID(String id)
	{
		if(id == null)
			return -1;
		
		Integer pos = RowID_mapping.get(id);
		if(pos != null)
			return pos.intValue();
		return -1;
	}
	
	int indexByNodeID(String id)
	{
		if(id == null)
			return -1;
		
		Integer pos = NodeID_mapping.get(id);
		if(pos != null)
			return pos.intValue();
		return -1;
	}
	
	public DataObject getDO(int i)
	{
		if(i >= 0 && i < do_table.length)
			return do_table[i];
		return null;
	}
	
	public DataObject getThisNodeDO()
	{
		int	i;
		for(i = 0; i < do_table.length; i++)
			if(do_table[i] != null)
				if(do_table[i].isThisNode())
					return do_table[i];
		return null;
	}
}
