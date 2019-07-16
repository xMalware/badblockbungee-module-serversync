package fr.badblock.bungee.modules.serversync;

import java.util.ArrayList;
import java.util.List;

import fr.badblock.bungee.modules.serversync.docker.ServerSyncDockerTask;
import fr.badblock.bungee.modules.serversync.docker.ServerSyncStaticTask;
import net.md_5.bungee.api.plugin.Plugin;

public class ModuleServerSync extends Plugin
{

	private List<Thread>	syncThreads;

	@Override
	public void onEnable()
	{
		syncThreads = new ArrayList<>();
		syncThreads.add(new ServerSyncDockerTask());
		syncThreads.add(new ServerSyncStaticTask());
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onDisable()
	{
		if (syncThreads == null)
		{
			return;
		}

		for (Thread thread : syncThreads)
		{
			thread.stop();
		}
	}

}