/*
 * Stolen from my gamedev starterkit. Raped so it works better.
 * 
 * CHANGELOG!
 * 
 *  - instead of packed, used byte[]
 */
package com.petterroea.MD5Lookup.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.petterroea.util.CommandLineUtils;

/**
 * Used for a server side connection that can handle tonnes of connections. Use this if you want a game option to host a game, or if you are making a dedicated server
 * @author petterroea
 *
 */
public class ConnectionThread{
	static Thread serverThread;
	static Server serverCode;
	public static void main(String[] args)
	{
		ConnectionThread server = new ConnectionThread(25301);
		serverCode = new Server(server);
		server.start();
		serverThread = new Thread(serverCode);
		serverThread.start();
		
		while(true)
		{
			String in = CommandLineUtils.getInput();
			if(in.equalsIgnoreCase("exit")||in.equalsIgnoreCase("stop"))
			{
				try {
					serverCode.stop();
					server.stop();
					serverThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			else if(in.equalsIgnoreCase("help"))
			{
				System.out.println("HELP PAGE:");
				System.out.println("  help - Displays this message");
				System.out.println("  exit - closes server");
				System.out.println("  stop - same as exit");
				System.out.println("  packetstats - shows statistics for packets");
			}
			else if(in.equalsIgnoreCase("packetstats"))
			{
				System.out.println("Your packet stats:");
				System.out.println("    " + errorPackets + " failed packets, " + successPackets + " sucessful. That's " + (errorPackets+successPackets) + " packets in total.");
			}
			else
			{
				System.out.println("Unknown command! Type \"help\" for help");
			}
		}
	}
	/**
	 * The writing-buffer
	 */
    private ByteBuffer writeBuffer;
    /**
     * The read buffer
     */
    private ByteBuffer readBuffer;
	/**
	 * The thread that listens to the network
	 */
	Thread listener;
	/**
	 * The clients connected to the server
	 */
	LinkedList<ServerClient> clients;
	/**
	 * Some random stuff. I dont know what it is, i just use it.
	 */
	private Selector readSelector;
	/**
	 * True if we are monitoring for commections and listening to packets
	 */
	private boolean running = true;
	/**
	 * The server socket channel
	 */
	private ServerSocketChannel sSChannel;
	/**
	 * List of packets going in
	 */
	LinkedList<byte[]> in;
	/**
	 * List of packets going out
	 */
	LinkedList<byte[]> out;
	/**
	 * Synchronization object
	 */
	public static Object sync = new Object();
	/**
	 * Constructor for a serverside connection
	 * @param port The port to bind to
	 */
	public ConnectionThread(int port)
	{
		in = new LinkedList<byte[]>();
		out = new LinkedList<byte[]>();
		readBuffer = ByteBuffer.allocateDirect(255);
		clients = new LinkedList<ServerClient>();
		writeBuffer = ByteBuffer.allocateDirect(255);
		try {
			//Open a non-blocking server socket channel
			sSChannel = ServerSocketChannel.open();
			sSChannel.configureBlocking(false);
			System.out.println("Opened the socket channel");
			//Bind to localhost
			InetAddress iAddr = InetAddress.getLocalHost();
			sSChannel.socket().bind(new InetSocketAddress(iAddr, port));
			System.out.println("Binded to locahost");
			System.out.println("Use localhost to connect from this pc, or " + InetAddress.getLocalHost().getHostAddress());
			//Some random stuff for "Multiplexing client channels"
			readSelector = Selector.open();
		} catch (java.net.BindException be) {
			System.out.println("FAILED TO BIND TO PORT(" + port + ")! Is the port allready used?");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Runs the server network stuff
	 */
	public void start()
	{
		listener = new Thread(){
			@Override
			public void run()
			{
				while(running)
				{
					synchronized(sync)
					{
						acceptNewPeeps();
						readIncomingMessages();
						sendPackets();
					}
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		listener.start();
	}
	/**
	 * Used by the seperate thread to send packets in the queue
	 */
	private void sendPackets()
	{
		for(int i = 0; i < clients.size(); i++)
		{
			for(int a = 0; a < clients.get(i).out.size(); a++)
			{
				sendMessage(clients.get(i).socket, clients.get(i).out.get(a));
			}
			clients.get(i).out.clear();
		}
		for(int i = 0; i < out.size(); i++)
		{
			for(int a = 0; a < clients.size(); a++)
			{
				sendMessage(clients.get(a).socket, out.get(i));
			}
		}
		out.clear();
		
	}
	/**
	 * Sends a message to the client. Again, dont touch this
	 * @param channel The channel from the client
	 * @param mesg The message to be sendt
	 */
	private void sendMessage(SocketChannel channel, byte[] mesg) {
    	prepWriteBuffer(mesg);
    	channelWrite(channel, writeBuffer);
        }
	static long errorPackets = 0;
	static long successPackets = 0;
	/**
	 * Reads incoming packets
	 */
	private void readIncomingMessages() {
		try {
		    // non-blocking select, returns immediately regardless of how many keys are ready
		    readSelector.selectNow();
		    
		    // fetch the keys
		    Set readyKeys = readSelector.selectedKeys();
		    
		    // run through the keys and process
		    Iterator i = readyKeys.iterator();
		    while (i.hasNext()) {
			SelectionKey key = (SelectionKey) i.next();
			i.remove();
			SocketChannel channel = (SocketChannel) key.channel();
			readBuffer.clear();
			
			// read from the channel into our buffer
			long nbytes = channel.read(readBuffer);
			
			// check for end-of-stream
			if (nbytes == -1) { 
			    System.out.println("disconnect: " + channel.socket().getInetAddress() + ", end-of-stream");
			    for(int a = 0; a < clients.size(); a++)
			    {
			    	if(clients.get(a).socket.hashCode() == channel.hashCode())
			    	{
			    		clients.remove(a);
			    		System.out.println("Removed the socket sucessfully");
			    		break;
			    	}
			    }
			    channel.close();
			    clients.remove(channel);
			}
			else {
			    // grab the StringBuffer we stored as the attachment
			    StringBuffer sb = (StringBuffer)key.attachment();
			    
			    // use a CharsetDecoder to turn those bytes into a string
			    // and append to our StringBuffer
			    readBuffer.flip();
			    byte[] data = readBuffer.array();
			    readBuffer.clear();
			    if(  ByteBuffer.wrap(data).getInt()==data.length) { successPackets++; } else { errorPackets++; }
				//Build packet
				in.add(data);
			}
			
		    }		
		}
		catch (IOException ioe) {
		    System.out.println("error during select(): " + ioe);
		}
		catch (Exception e) {
		    //System.out.println("exception in run()" + e);
			e.printStackTrace();
		}
		
	    }
	/**
	 * Writes to a channel
	 * @param channel The channel
	 * @param writeBuffer The buffer
	 */
	private void channelWrite(SocketChannel channel, ByteBuffer writeBuffer) {
    	long nbytes = 0;
    	long toWrite = writeBuffer.remaining();
    	// loop on the channel.write() call since it will not necessarily
    	// write all bytes in one shot
    	try {
    	    while (nbytes != toWrite) {
    		nbytes += channel.write(writeBuffer);
    		
    		try {
    		    Thread.sleep(30);
    		}
    		catch (InterruptedException e) {}
    	    }
    	}
    	catch (ClosedChannelException cce) {
    	}
    	catch (Exception e) {
    	} 
    	
    	// get ready for another write if needed
    	writeBuffer.rewind();
        }
	/**
	 * Used for preparing writing to the client
	 * @param mesg
	 */
	private void prepWriteBuffer(byte[] mesg) {
    	// fills the buffer from the given string
    	// and prepares it for a channel write
    	writeBuffer.clear();
    	writeBuffer.putInt(mesg.length+4);
    	writeBuffer.put(mesg);
    	writeBuffer.flip();
        }
	/**
	 * Used by the seperate thread to accept new connections
	 */
	private void acceptNewPeeps()
	{
		try {
		    SocketChannel clientChannel;
		    // since sSockChan is non-blocking, this will return immediately 
		    // regardless of whether there is a connection available
		    while ((clientChannel = sSChannel.accept()) != null) {
			System.out.println("got connection from: " + clientChannel.socket().getInetAddress()); 
			clientChannel.configureBlocking( false);
		    SelectionKey readKey = clientChannel.register(readSelector, SelectionKey.OP_READ, new StringBuffer());
		    clients.add(new ServerClient(clientChannel));
		    }		
		}
		catch (IOException ioe) {
		    System.out.println("error during accept(): " + ioe);
		}
		catch (Exception e) {
		    System.out.println("exception in acceptNewConnections()" + e);
		}
	}
	/**
	 * Stops the server network thingie. Use this before disposing the network code
	 * @throws InterruptedException If faied to join thread
	 * @throws IOException If failed to close the socket
	 */
	public void stop() throws InterruptedException, IOException
	{
		running = false;
		listener.join();
		sSChannel.close();
	}
}
class ServerClient {
	LinkedList<byte[]> out;
	SocketChannel socket;
	public ServerClient(SocketChannel socket)
	{
		out = new LinkedList<byte[]>();
		this.socket = socket;
	}
}