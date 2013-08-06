package org.aksw.sparqlify.core.sparql;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.aksw.sparqlify.core.MakeExprPermissive;
import org.aksw.sparqlify.core.MakeNodeValue;
import org.aksw.sparqlify.core.domain.input.RestrictedExpr;
import org.apache.jena.riot.process.normalize.CanonicalizeLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingHashMap;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.util.ExprUtils;

public class RowMapperSparqlifyBinding
	implements RowMapper<Binding>
{
	private static final Logger logger = LoggerFactory.getLogger(IteratorResultSetSparqlifyBinding.class);
	
	// Canonicalize values, e.g. 20.0 -> 2.0e1
	private static CanonicalizeLiteral canonicalizer = CanonicalizeLiteral.get();
	
	//private NodeExprSubstitutor substitutor;// = new NodeExprSubstitutor(sparqlVarMap);
	private Multimap<Var, RestrictedExpr> sparqlVarMap;
	
	//private long nextRowId;
	private Var rowIdVar;

	public static boolean isCharType(String typeName) {
		String tmp = typeName.toLowerCase();
		
		Set<String> charNames = new HashSet<String>(Arrays.asList("char"));
		
		boolean result = charNames.contains(tmp);
		return result;
	}

	public RowMapperSparqlifyBinding(Multimap<Var, RestrictedExpr> sparqlVarMap)
	{
		this(sparqlVarMap, null);
	}


	public RowMapperSparqlifyBinding(Multimap<Var, RestrictedExpr> sparqlVarMap, String rowIdName)
	{
		this.sparqlVarMap = sparqlVarMap;
		this.rowIdVar = rowIdName == null ? null : Var.alloc(rowIdName);
 	}

	@Override
	public Binding mapRow(ResultSet rs, int rowId) {
		Binding result;
		try {
			result = _map(rs, rowId);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return result;
	}
	
	public Binding _map(ResultSet rs, long rowId) throws Exception {

		// OPTIMIZE refactor these to attributes
		//NodeExprSubstitutor substitutor = new NodeExprSubstitutor(sparqlVarMap);
		BindingMap binding = new BindingHashMap();

		
		ResultSetMetaData meta = rs.getMetaData();
		
		/*
		for(int i = 1; i <= meta.getColumnCount(); ++i) {
			binding.add(Var.alloc("" + i), node)
		}*/	

		
		// Substitute the variables in the expressions
		for(int i = 1; i <= meta.getColumnCount(); ++i) {
			String colName = meta.getColumnLabel(i);
			Object colValue = rs.getObject(i);

			NodeValue nodeValue;

			// NOTE Char right padding is handled as a special expression (similar to urlEncode)
//			String colType = meta.getColumnTypeName(i);
//			
//			//System.out.println(colValue == null ? "null" : colValue.getClass());
//			
//			// TODO: Make datatype serialization configurable
//			if(isCharType(colType)) {
//				if(colValue == null) {
//					nodeValue = null;
//				} else {
//					int displaySize = meta.getPrecision(i);
//					int scale = meta.getScale(i);
//					String tmp = "" + colValue;
//					String v = StringUtils.rightPad(tmp, displaySize);
//					nodeValue = NodeValue.makeString(v);
//				}
//			}
//			else
			if(colValue instanceof Date) {
				String tmp = colValue.toString();
				nodeValue = NodeValue.makeDate(tmp); 
			}
			else if(colValue instanceof Timestamp) {
				String tmp = colValue.toString();
				String val = tmp.replace(' ', 'T');
				nodeValue = NodeValue.makeDateTime(val);
			} else {
				nodeValue = MakeNodeValue.makeNodeValue(colValue);
			}
			
			if(nodeValue == null) {
				continue;
			}
			
//			if(nodeValue.isDateTime()) {
//				XSDDateTime val = nodeValue.getDateTime();
//				String str = val.timeLexicalForm();
//				String b = val.toString();
//
//				System.out.println("foo");
//			}
			
			Node node = nodeValue.asNode();
			
			
			// FIXME We also add bindings that enable us to reference the columns by their index
			// However, indexes and column-names are in the same namespace here, so there might be clashes
			Var indexVar = Var.alloc("" + i);
			binding.add(indexVar, node);
			
			Var colVar = Var.alloc(colName);
			if(!binding.contains(colVar)) {
				binding.add(colVar, node);
			}
		}
		

		
		// Additional "virtual" columns
		// FIXME Ideally this should be part of a class "ResultSetExtend" that extends a result set with additional columns
		if(rowIdVar != null) {
			Node node = NodeValue.makeInteger(rowId).asNode();
			
			binding.add(rowIdVar, node);
		}

		
		boolean debugMode = true;
		
		BindingMap result = new BindingHashMap();
		
		for(Entry<Var, Collection<RestrictedExpr>> entry : sparqlVarMap.asMap().entrySet()) {
			
			Var bindingVar = entry.getKey();
			Collection<RestrictedExpr> candidateExprs = entry.getValue();
			
//			if(bindingVar.getName().equals("o")) {
//				System.out.println("BindingVar o ");
//			}
			
			//RDFNode rdfNode = null;
			NodeValue value = null;
			//Node value = Node.NULL;
			
			// We distinguish on how to create a variable by the columns that are used
			// We use the most specific rdfTerm constructor
			Set<Var> usedVars = new HashSet<Var>();
			for(RestrictedExpr def : candidateExprs) {
	
				Expr expr = def.getExpr();
				
				
				// Check if all variables are bound
				// Null columns may appear on left joins
				boolean allBound = true;
				Set<Var> exprVars = expr.getVarsMentioned();
				for(Var var : exprVars) {
					if(!binding.contains(var)) {
						allBound = false;
						break;
					}
				}
				
				if(allBound) {
					if(value != null) {
						// If the new rdfTerm constructor makes use of only a subset of the columns
						// from which the current node was created, we ignore the new bindings
						if(usedVars.containsAll(expr.getVarsMentioned())) {
							continue;
						} else if(usedVars.equals(expr.getVarsMentioned())) {
							throw new RuntimeException("Multiple expressions binding the variable (ambiguity) " + bindingVar + ": " + entry.getValue());							
						} else if(!expr.getVarsMentioned().containsAll(usedVars)) {
							throw new RuntimeException("Multiple expressions binding the variable (overlap) " + bindingVar + ": " + entry.getValue());
						}
					}
					
				
					expr = MakeExprPermissive.getInstance().deepCopy(expr);
					
					value = ExprUtils.eval(expr, binding);
					//rdfNode = ModelUtils.convertGraphNodeToRDFNode(value.asNode(), null);

					if(!debugMode) {
						break;
					}
				}
			}

			//qs.add(entry.getKey().getName(), rdfNode);
			// TODO Add a switch for this warning/debugging message (also decide on the logging level)
			Node resultValue = value == null ? null : value.asNode();
			
			if(resultValue == null) {
				logger.trace("Null node for variable " + bindingVar + " - Might be undesired.");
				//throw new RuntimeException("Null node for variable " + entry.getKey() + " - Should not happen.");
			} else {
			
				//result.add((Var)entry.getKey(), resultValue);

				boolean isDatatypeCanonicalization = false;
				
				Node canonResultValue = canonicalizer.convert(resultValue);
				//System.out.println("Canonicalization: " + resultValue + " -> " + canonResultValue);
				if(!isDatatypeCanonicalization) {
					
					if(canonResultValue.isLiteral()) {
						String lex = canonResultValue.getLiteralLexicalForm();
						
						if(resultValue.isLiteral()) {
							RDFDatatype originalType = resultValue.getLiteralDatatype();

							if(originalType != null) {
							//String typeUri = resultValue.getLiteralDatatypeURI();
								canonResultValue = Node.createLiteral(lex, originalType);
							}
						} else {
							throw new RuntimeException("Should not happen: Non-literal canonicalized to literal: " + resultValue + " became " + canonResultValue);
						}

						
					}
					
				}
				
				result.add(bindingVar, canonResultValue);
			}
		}

		return result;
	}
	
}