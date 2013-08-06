package com.petterroea.MD5Lookup.Server;

import java.sql.ResultSet;
import java.sql.SQLException;

import tk.ifunny.MySQL;

public class Server implements Runnable {
	ConnectionThread server;
	private MySQL sql;
	private boolean running = true;
	
	public Server(ConnectionThread thread)
	{
		this.server = thread;
		try {
			sql = new MySQL("testbed", "testbed", "1234", "localhost"); //Local test server. Release will be more elegant.:P
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		checkAndFixSqlErrors();
	}
	
	@Override
	public void run() {
		while(running)
		{
			synchronized(server.sync)
			{
				if(server.in.size()!=0)
				{
					for(int i = 0; i < server.in.size(); i++)
					{
						handlePacket(server.in.get(i));
					}
				}
			}
		}
	}
	
	public void queryMd5(String md5)
	{
		System.out.println(Integer.parseInt(md5.toLowerCase(), 16));
	}
	
	private void handlePacket(byte[] bs) {
		if(bs[4]==(byte)0)
		{
			
		}
	}
	private void checkAndFixSqlErrors() {
		try
		{
			ResultSet result = sql.query("SHOW TABLES LIKE 'users';");
			if(!result.next())
			{
				System.out.println("Missing table users! Generating...");
				sql.manipulateData("CREATE TABLE `users`;");
			}
			result = sql.query("SHOW TABLES LIKE 'blocks';");
			if(!result.next())
			{
				System.out.println("Missing table blocks! Generating...");
				sql.manipulateData("CREATE TABLE `blocks`;");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		running = false;
	}
}
