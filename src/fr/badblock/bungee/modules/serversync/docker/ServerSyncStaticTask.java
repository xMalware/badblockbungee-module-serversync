package fr.badblock.bungee.modules.serversync.docker;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;

import fr.badblock.api.common.sync.bungee.objects.ServerObject;
import fr.badblock.api.common.tech.mongodb.MongoService;
import fr.badblock.bungee.BadBungee;
import fr.badblock.bungee.utils.time.TimeUtils;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;

/**
 * 
 * Task to send data in a recurrent time frame to synchronize nodes
 * 
 * @author xMalware
 *
 */
public class ServerSyncStaticTask extends Thread {

	public static Map<String, ServerObject> serverData = new HashMap<>();

	/**
	 * If the task is still running
	 * 
	 * @param New
	 *            state of the task
	 * @return If the task is still running or not
	 */
	public static boolean run = true;

	/**
	 * Task thread
	 */
	private static Thread syncThread = Thread.currentThread();

	/**
	 * Get the current BungeeCord node IP
	 * 
	 * @return
	 */
	public static String getIP() {
		return BungeeCord.getInstance().config.getListeners().iterator().next().getHost().getAddress().getHostAddress();
	}

	/**
	 * Keep alive the current BungeeCord node
	 */
	public static void sync() {
		Map<String, ServerObject> tempServerData = new HashMap<>();
		BadBungee badBungee = BadBungee.getInstance();

		MongoService mongoService = badBungee.getMongoService();
		DB db = mongoService.getDb();
		for (String cluster : badBungee.getConfig().getDockerClusters())
		{
			DBCollection collection = db.getCollection("staticservers_" + cluster);
			BasicDBObject query = new BasicDBObject();
			Cursor cursor = collection.find(query);
			while (cursor.hasNext())
			{
				BasicDBObject object = (BasicDBObject) cursor.next();
				String name = object.getString("name");
				String ip = object.getString("ip");
				int port = object.getInt("port");
				ServerObject serverObject = new ServerObject(name, ip, port);

				tempServerData.put(name, serverObject);
			}
		}

		synchronized (syncThread)
		{
			for (String key : tempServerData.keySet())
			{
				if (!serverData.containsKey(key))
				{
					ServerObject serverObject = tempServerData.get(key);
					ServerInfo server = BungeeCord.getInstance().constructServerInfo(serverObject.getName(),
							new InetSocketAddress(serverObject.getIp(), serverObject.getPort()), serverObject.getName(), false);
					// Add the server
					BungeeCord.getInstance().getServers().put(serverObject.getName(), server);
					BadBungee.log("§d[StaticServers] §aAdded server: §e" + key + " §ahosted by §e" + serverObject.getIp());
				}
				else
				{
					try
					{
						ServerObject dockerServerData = tempServerData.get(key);
						ServerObject current = serverData.get(key);
						if (!dockerServerData.equals(current))
						{
							tempServerData.remove(key);
							BadBungee.log("§d[StaticServers] §cRemoved server: §e" + key + " §c(because of difference, wait for being updated).");
						}
					}
					catch (Exception error)
					{
						error.printStackTrace();
					}
				}
			}

			for (String key : serverData.keySet())
			{
				if (!tempServerData.containsKey(key))
				{
					ServerObject serverObject = serverData.get(key);
					BungeeCord.getInstance().getServers().remove(serverObject.getName());
					BadBungee.log("§d[StaticServers] §cRemoved server: §e" + key + " §chosted by §e" + serverObject.getIp());
				}
			}
		}

		serverData = tempServerData;
	}

	/**
	 * Constructor of a new Bungee task Automatic task start when instantiating
	 */
	public ServerSyncStaticTask() {
		// Start the thread
		this.start();
	}

	@Override
	/**
	 * Data sending loop method
	 */
	public void run() {
		// While the task is allowed to run
		while (run) {
			try
			{
				// Send a keep alive packet
				sync();
			}
			catch (Exception error)
			{

			}
			// Sleep 1 second
			TimeUtils.sleepInSeconds(2);
		}
	}

}
