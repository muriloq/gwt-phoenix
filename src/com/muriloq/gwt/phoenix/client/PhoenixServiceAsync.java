package com.muriloq.gwt.phoenix.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface PhoenixServiceAsync {
	void loadRoms(AsyncCallback<byte[]> callback);
}
