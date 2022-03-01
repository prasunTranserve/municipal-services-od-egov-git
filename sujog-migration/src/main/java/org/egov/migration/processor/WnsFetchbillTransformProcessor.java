package org.egov.migration.processor;

import org.egov.migration.reader.model.WSConnection;
import org.springframework.batch.item.ItemProcessor;

public class WnsFetchbillTransformProcessor implements ItemProcessor<WSConnection, WSConnection> {
	
	@Override
	public WSConnection process(WSConnection connection) throws Exception {
		connection.setBusinessservice("WS");
		return connection;
	}
}
