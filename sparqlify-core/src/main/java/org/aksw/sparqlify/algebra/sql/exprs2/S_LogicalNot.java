package org.aksw.sparqlify.algebra.sql.exprs2;

import org.aksw.sparqlify.core.TypeToken;
import org.openjena.atlas.io.IndentedWriter;

public class S_LogicalNot
	extends SqlExpr1
{
	public S_LogicalNot(SqlExpr arg) {
		super(TypeToken.Boolean, "logicalNot", arg);
	}
	
	@Override
	public void asString(IndentedWriter writer) {
		writer.print("LogicalNot");
		writeArgs(writer);
	}

	@Override
	public S_LogicalNot copy(SqlExpr arg) {
		S_LogicalNot result = new S_LogicalNot(arg);
		return result;
	}
	
	public static S_LogicalNot create(SqlExpr a) {
		return new S_LogicalNot(a);
	}

}