package fr.badblock.bungee.modules.serversync.docker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DockerServerData
{

	private String	serverName;

	private String	source;
	private String	ip;
	private int			port;

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 19 * hash + (this.serverName != null ? this.serverName.hashCode() : 0);
		hash = 19 * hash + (this.source != null ? this.source.hashCode() : 0);
		hash = 19 * hash + (this.ip != null ? this.ip.hashCode() : 0);
		hash = 19 * hash + ((Integer) port).hashCode();
		return hash;
	}

	@Override
	public String toString()
	{
		return "serverName = " + serverName + ", source = " + source + ", ip = " + ip + ", port = " + port;
	}

}
