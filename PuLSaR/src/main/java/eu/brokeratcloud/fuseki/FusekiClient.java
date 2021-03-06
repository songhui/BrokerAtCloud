package eu.brokeratcloud.fuseki;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

public class FusekiClient implements eu.brokeratcloud.persistence.SparqlServiceClient {

	private Properties properties;
	private String updateService;
	private String queryService;
	private boolean debugOn = false;
	private java.io.PrintStream logger;
	
	public FusekiClient() throws java.io.IOException {
		this("fuseki-client.properties");
	}
	
	public FusekiClient(String propertiesFile) throws java.io.IOException {
		properties = new Properties();
		properties.load( getClass().getClassLoader().getResourceAsStream(propertiesFile) );
		updateService = properties.getProperty("update-service");
		queryService = properties.getProperty("query-service");
		
		// debugging
		String debugStr = properties.getProperty("debug", "off").trim().toLowerCase();
		debugOn = debugStr.equals("on") || debugStr.equals("yes") || debugStr.equals("1");
		debug("<init>: Debug messages are 'on'");
		
		// log file
		String logFile = properties.getProperty("log-file");
		if (debugOn && logFile!=null && !logFile.isEmpty()) {
			logger = new java.io.PrintStream(new java.io.FileOutputStream(logFile, true));
		}
	}
	
	protected void debug(String mesg) {
		if (!debugOn) return;
		System.err.println(getClass()+": "+mesg);
		if (logger!=null) { logger.println(mesg); logger.flush(); }
	}
	
	public String getSelectEndpoint() { return queryService; }
	public String getUpdateEndpoint() { return updateService; }
	
	public void execute(String sparqlUpdate) {
		debug("execute: Query=\n"+sparqlUpdate);
		UpdateRequest updReq = new UpdateRequest();
		updReq.add(sparqlUpdate);
		UpdateProcessor upInsertData = UpdateExecutionFactory.createRemote(updReq, updateService);
		
		// Perform the SPARQL Update operation
		upInsertData.execute();
	}
	
	public Map<String,String> queryBySubject(String subjectUri) {
		debug("queryBySubject: ...");
		String qry = String.format("SELECT ?p ?o WHERE { <%s> ?p ?o }", subjectUri);
		debug("queryBySubject: Query="+qry);
		QueryExecution qeSelect = query(qry);
		try {
			ResultSet resultSet = qeSelect.execSelect();
			
			// Iterating over the SPARQL Query results
			Map<String,String> results = new HashMap<String,String>();
			while (resultSet.hasNext()) {
				QuerySolution soln = resultSet.nextSolution();
				//System.out.println(soln.toString());
				String p = soln.get("?p").asResource().getURI();
				RDFNode oNd = soln.get("?o");
				String o;
				if (oNd.isLiteral()) {
					com.hp.hpl.jena.rdf.model.Literal lit = oNd.asLiteral();
					if (lit.getDatatypeURI()!=null && !lit.getDatatypeURI().isEmpty()) {
						o = String.format("\"%s\"^^<%s>", lit.getLexicalForm(), lit.getDatatypeURI());
					} else {
						o = String.format("\"%s\"", lit.getLexicalForm());
					}
					// append language suffix if specified
					if (!lit.getLanguage().isEmpty()) {
						String langSuffix = "@"+lit.getLanguage();
						o += langSuffix;
						p += langSuffix;
					}
				} else o = "<"+oNd.asResource().getURI()+">";
//XXX: 2014-11-17: Addition
if (results.containsKey(p)) o = results.get(p)+", "+o;
//-OR-
//if (!results.containsKey(p)) results.put(p, new Vector<String>()); results.get(p).add(o);  ==> THEN return value MUST BE  "Map<String,List<String>>"
				results.put(p, o);
			}
			debug("queryBySubject: Results=\n"+results);
			return results;
		} finally {
			qeSelect.close();
		}
	} // end method
	
	public List<Map<String,RDFNode>> queryAndProcess(String selectQuery) {
		debug("queryAndProcess: ...");
		QueryExecution qeSelect = query(selectQuery);
		try {
			ResultSet results = qeSelect.execSelect();
			
			// Iterating over the SPARQL Query results
			List<Map<String,RDFNode>> resultsList = new LinkedList<Map<String,RDFNode>>();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				//System.out.println(soln.toString());
				HashMap<String,RDFNode> row = new HashMap<String,RDFNode>();
				Iterator<String> it = soln.varNames();
				while (it.hasNext()) {
					String varName = it.next();
					RDFNode value = soln.get(varName);
					row.put(varName, value);
				}
				resultsList.add(row);
			}
			debug("queryAndProcess: Results=\n"+resultsList);
			return resultsList;
		} finally {
			qeSelect.close();
		}
	} // end method
	
	public List<String> queryForIds(String selectQuery, String idCol) {
		debug("queryForIds: ...");
		QueryExecution qeSelect = query(selectQuery);
		try {
			ResultSet results = qeSelect.execSelect();
			
			// Iterating over the SPARQL Query results
			List<String> resultsList = new LinkedList<String>();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				if (idCol==null) {
					Iterator<String> it = soln.varNames();
					while (idCol==null && it.hasNext()) idCol = it.next();
				}
				//System.out.println(soln.toString());
				RDFNode node = soln.get(idCol);
				String id;
				if (node.isLiteral()) id = node.asLiteral().getLexicalForm();
				else id = "<"+node.asResource().getURI()+">";
				resultsList.add(id);
			}
			debug("queryForIds: Results=\n"+resultsList);
			return resultsList;
		} finally {
			qeSelect.close();
		}
	} // end method
	
/*	public ResultSet query(String selectQuery) {
		QueryExecution qeSelect = QueryExecutionFactory.sparqlService(queryService, selectQuery);
		// Perform the simple SPARQL SELECT query
		try {
			// Retrieving the SPARQL Query results
			return qeSelect.execSelect();
		} finally {
			qeSelect.close();
		}
	} // end method
	*/
	public QueryExecution query(String selectQuery) {
		debug("query: Query=\n"+selectQuery);
		return QueryExecutionFactory.sparqlService(queryService, selectQuery);
	} // end method
	
	public Object queryValue(String selectQuery) {
		debug("queryValue: ...");
		QueryExecution qeSelect = query(selectQuery);
		try {
			ResultSet rs = qeSelect.execSelect();
			if (rs.hasNext()) {
				QuerySolution soln = rs.next();
				Iterator<String> it = soln.varNames();
				if (it.hasNext()) {
					String key = it.next();
					RDFNode val = soln.get(key);
					debug("queryValue: Result="+val);
					return val;
				}
			}
			debug("queryValue: Result=<null>");
			return null;
		} finally {
			qeSelect.close();
		}
	} // end method

	public boolean ask(String askQuery) {
		debug("queryValue: ...");
		QueryExecution qeAsk = query(askQuery);
		try {
			return qeAsk.execAsk();
		} finally {
			qeAsk.close();
		}
	} // end method

	public static void main(String[] args) throws Exception {
		FusekiClient client = new FusekiClient();
		long dur = 0;
		long cnt = Long.parseLong(args[0]);
		int qryCnt = args.length-1;
		long startTm = System.currentTimeMillis();
		for (int i=0; i<cnt; i++) {
			long startTm1 = System.currentTimeMillis();
			client.queryAndProcess(args[(i % qryCnt)+1]);
			dur += (System.currentTimeMillis() - startTm1);
			if (i%1000==0) { System.out.print("."); System.out.flush(); }
		}
		System.out.println();
		long endTm = System.currentTimeMillis();
		double avgDur = dur / cnt;
		dur = endTm - startTm;
		System.out.println(String.format("Run duration: %dms,  iterations: %d,  avg. iteration time: %fms", dur, cnt, avgDur));
	}
}