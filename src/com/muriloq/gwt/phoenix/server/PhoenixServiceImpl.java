package com.muriloq.gwt.phoenix.server;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.muriloq.gwt.phoenix.client.PhoenixService;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class PhoenixServiceImpl extends RemoteServiceServlet implements
		PhoenixService {
	
	@Override
	public byte[] loadRoms() {
		byte buffer[] = new byte[0x6000];
		try {
			loadRom("program.rom", buffer, 0, 0x4000);
			loadRom("graphics.rom", buffer, 0x4000, 0x2000);
		} catch (Exception e){
			throw new RuntimeException("Error loading ROMs");
		}
		return buffer; 
	}
	
	private void loadRom(String name, byte[] buffer, int offset, int len) throws IOException {
		int readbytes = 0;
		System.out.print("Reading ROM "+name+"...");
		InputStream is  = new FileInputStream(name);		
		BufferedInputStream bis = new BufferedInputStream(is, len);
		int n = len;
		int toRead = len;
		while (toRead > 0) {
			int nRead = bis.read(buffer, offset + n - toRead, toRead);
			toRead -= nRead;
			readbytes += nRead;
		}
		System.out.println(readbytes + " bytes");
	}

}
