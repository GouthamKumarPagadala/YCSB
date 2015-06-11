package com.yahoo.ycsb.db;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Properties;

import java.io.IOException;

import org.tarantool.TarantoolConnection16;
import org.tarantool.TarantoolConnection16Impl;
import org.tarantool.TarantoolException;
import org.tarantool.CommunicationException;

import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

public class TarantoolClient extends DB {
	
	public static final String HOST_PROPERTY  = "tarantool.host";
	public static final String PORT_PROPERTY  = "tarantool.port";
	public static final String SPACE_PROPERTY = "tarantool.space";

	public static final String DEFAULT_HOST   = "localhost";
	public static final int    DEFAULT_PORT   = 3301;
	public static final int    DEFAULT_SPACE  = 1024;
	
	private static final Logger log = Logger.getLogger(TarantoolClient.class.getName());
	private TarantoolConnection16Impl connection;
	private int spaceNo;

	public void init() throws DBException {
		Properties props = getProperties();

		int port = DEFAULT_PORT;
		String portString = props.getProperty(PORT_PROPERTY);
		if (portString != null) {
			port = Integer.parseInt(portString);
		}

		String host = props.getProperty(HOST_PROPERTY);
		if (host == null) {
			host = DEFAULT_HOST;
		}
		
		spaceNo = DEFAULT_SPACE;
		String spaceString = props.getProperty(SPACE_PROPERTY);
		if (spaceString != null) {
			spaceNo = Integer.parseInt(spaceString);
		}

		try {
			this.connection = new TarantoolConnection16Impl(host, port);
		}
		catch (Exception exc) {
			System.err.println("Can't init Tarantool connection:" + exc.toString());
			exc.printStackTrace();
			return;
		}
	}
	
	public void cleanup() throws DBException{
		this.connection.close();
	}

	@Override
	public int insert(String table, String key, HashMap<String, ByteIterator> values) {
		int j = 0;
		String[] tuple = new String[1 + 2 * values.size()];
		tuple[0] = key;
		for (Map.Entry<String, ByteIterator> i: values.entrySet()) {
			tuple[j + 1] = i.getKey();
			tuple[j + 2] = i.getValue().toString();
			j += 2;
		}
		try {
			this.connection.insert(this.spaceNo, tuple);
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		}
		return 0;
	}

	private HashMap<String, ByteIterator> tuple_convert_filter (List<String> input,
			Set<String> fields) {
		HashMap<String, ByteIterator> result = new HashMap<String, ByteIterator>();
		if (input == null)
			return result;
		for (int i = 1; i < input.toArray().length; i += 2)
			if (fields == null || fields.contains(input.get(i)))
				result.put(input.get(i), new StringByteIterator(input.get(i+1)));
		return result;
	}

	@Override
	public int read(String table, String key, Set<String> fields,
			HashMap<String, ByteIterator> result) {
		try {
			List<String> response;
			response = this.connection.select(this.spaceNo, 0, Arrays.asList(key), 0, 1, 0);
			result = tuple_convert_filter(response, fields);
			return 0;
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		} catch (IndexOutOfBoundsException e) {
			return 1;
		}
	}

	@Override
	public int scan(String table, String startkey,
			int recordcount, Set<String> fields,
			Vector<HashMap<String, ByteIterator>> result) {
		List<String> response;
		try {
			response = this.connection.select(this.spaceNo, 0, Arrays.asList(startkey), 0, recordcount, 6);
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		}
		HashMap<String, ByteIterator> temp = tuple_convert_filter(response, fields);
		if (!temp.isEmpty())
			result.add((HashMap<String, ByteIterator>) temp.clone());
		return 0;
	}

	@Override
	public int delete(String table, String key) {
		try {
			this.connection.delete(this.spaceNo, Arrays.asList(key));
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		} catch (IndexOutOfBoundsException e) {
			return 1;
		}
		return 0;
	}
	@Override
	public int update(String table, String key,
			HashMap<String, ByteIterator> values) {
		int j = 0;
		String[] tuple = new String[1 + 2 * values.size()];
		tuple[0] = key;
		for (Map.Entry<String, ByteIterator> i: values.entrySet()) {
			tuple[j + 1] = i.getKey();
			tuple[j + 2] = i.getValue().toString();
			j += 2;
		}
		try {
			this.connection.replace(this.spaceNo, tuple);
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		}
		return 0;

	}
}
