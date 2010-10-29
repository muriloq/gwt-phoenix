package com.muriloq.gwt.phoenix.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("phoenix")
public interface PhoenixService extends RemoteService {
	byte[] loadRoms();
}
