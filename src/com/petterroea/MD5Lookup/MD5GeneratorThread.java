package com.petterroea.MD5Lookup;

import com.petterroea.util.MathUtils;
import com.petterroea.util.MiscUtils;

public class MD5GeneratorThread implements Runnable {
	Main main;
	public MD5GeneratorThread(Main main)
	{
		this.main = main;
	}
	@Override
	public void run() {
		//System.out.println("Starting");
		long startTime = System.currentTimeMillis();
		for(long i = 0; i < Long.MAX_VALUE; i++)
		{
			if(i%1000==0&&i!=0)
			{
				long timePassed = System.currentTimeMillis()-startTime;
				if(timePassed>1000)
				{
					main.tray.icon.setToolTip("Calculated " + i + " hashes("+(i/(timePassed/1000))+" hashes/s)");
				}
				else
				{
					main.tray.icon.setToolTip("Calculated " + i + " hashes");
				}
			}
			byte[] data = MathUtils.longToBytes((long)i);
			String s = MiscUtils.getMd5(data);
			s = s + "";
		}
		//System.out.println("Done!");
	}
}
