package com.petterroea.MD5Lookup;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {
	public Thread trayThread;
	public Thread md5Thread;
	public TrayIconManager tray;
	public MD5GeneratorThread generator;
	public static void main(String[] args) {
		new Main();
	}
	public Main()
	{
		tray = new TrayIconManager();
		trayThread = new Thread(tray);
		trayThread.start();
		generator = new MD5GeneratorThread(this);
		md5Thread = new Thread(generator);
		md5Thread.start();
	}

}
class TrayIconManager implements Runnable
{
	public TrayIcon icon;
	public SystemTray tray;
	Image[] frames;
	public TrayIconManager()
	{
		tray = SystemTray.getSystemTray();
		
		BufferedImage temp = null;
		try {
			temp = ImageIO.read(Main.class.getResourceAsStream("process-working-2.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		frames = new Image[((temp.getWidth()/32)*(temp.getHeight()/32))-1];
		int w = temp.getWidth()/32;
		int h = temp.getHeight()/32;
		for(int x = 0; x < w; x++)
		{
			for(int y = 0; y < h; y++)
			{
				if(x==0&&y==0) continue; //Because first frame is blank
				frames[(x+(y*w))-1] = (Image)temp.getSubimage(x*32, y*32, 32, 32).getScaledInstance(16, 16, Image.SCALE_SMOOTH);
			}
		}
		
		icon = new TrayIcon(frames[0]);
		icon.setToolTip("Loading...");
		try {
			tray.add(icon);
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		int index = 0;
		while(true)
		{
			if(index>=frames.length) index = index % (frames.length-1);
			icon.setImage(frames[index]);
			index++;
			try {
				Thread.sleep(1000/15);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}