package fr.badblock.bungee.modules.serversync;

import fr.badblock.bungee.modules.serversync.docker.BungeeServerSyncTask;
import net.md_5.bungee.api.plugin.Plugin;

public class ModuleServerSync extends Plugin
{

	private Thread	thread;
	
	@Override
	public void onEnable()
	{
		thread = new BungeeServerSyncTask();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onDisable()
	{
		if (thread == null)
		{
			return;
		}
		
		thread.stop();
	}
	
}