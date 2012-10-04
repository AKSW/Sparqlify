package cornercases;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import javax.sql.DataSource;

import org.aksw.sparqlify.algebra.sparql.expr.old.ExprSqlBridge;
import org.aksw.sparqlify.algebra.sql.exprs.SqlExprColumn;
import org.aksw.sparqlify.algebra.sql.nodes.SqlOp;
import org.aksw.sparqlify.core.DatatypeSystem;
import org.aksw.sparqlify.core.algorithms.DatatypeAssigner;
import org.aksw.sparqlify.core.algorithms.DatatypeAssignerMap;
import org.aksw.sparqlify.core.algorithms.MappingOpsImpl;
import org.aksw.sparqlify.core.algorithms.SqlExprSerializerPostgres;
import org.aksw.sparqlify.core.algorithms.SqlOpSelectBlockCollectorImpl;
import org.aksw.sparqlify.core.algorithms.SqlOpSerializerImpl;
import org.aksw.sparqlify.core.domain.Mapping;
import org.aksw.sparqlify.core.domain.VarBinding;
import org.aksw.sparqlify.core.domain.ViewDefinition;
import org.aksw.sparqlify.core.domain.ViewInstance;
import org.aksw.sparqlify.core.interfaces.MappingOps;
import org.aksw.sparqlify.core.interfaces.SqlExprSerializer;
import org.aksw.sparqlify.core.interfaces.SqlOpSerializer;
import org.aksw.sparqlify.util.MapReader;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class MappingOpsImplTest {

	private static final Logger logger = LoggerFactory.getLogger(MappingOpsImplTest.class);

	@Test
	public void creationTest() throws RecognitionException, SQLException, IOException {

		DataSource dataSource = TestUtils.createTestDatabase(); 
		Connection conn = dataSource.getConnection();

		// typeAliases for the H2 datatype
		Map<String, String> typeAlias = MapReader.readFile(new File("src/main/resources/type-map.h2.tsv"));
		
		
		ViewDefinitionFactory vdFactory = TestUtils.createViewDefinitionFactory(conn, typeAlias);
		
		String testView = "Create View testview As Construct { ?s a ?t } With ?s = uri(?ID) ?t = uri(?NAME) From person";
		ViewDefinition coreVd = vdFactory.create(testView);
		
		
		
		Mapping m1 = coreVd.getMapping();
		
		
		VarBinding binding = new VarBinding();
		ViewInstance vi = new ViewInstance(coreVd, binding);

		DatatypeAssigner da = DatatypeAssignerMap.createDefaultAssignments(vdFactory.getDatatypeSystem());
		//ExprDatatypeNorm exprNormalizer = new ExprDatatypeNorm(da);
		
		MappingOps ops = new MappingOpsImpl(da);
		
		Mapping m2 = ops.join(m1, m1);
		Mapping m3 = ops.join(m2, m1);
		Mapping m4 = ops.union(Arrays.asList(m1, m3));
		
		Mapping mTest = m4;
		//System.out.println(m2);
		
		System.out.println(mTest.getSqlOp());
		
		SqlExprColumn x;
		
//		 Context ctx = new InitialContext();
//		 ctx.bind("jdbc/dsName", ds);		

		ExprSqlBridge b;

		System.out.println(coreVd.getMapping().getSqlOp().getSchema());
		
		SqlOp block = SqlOpSelectBlockCollectorImpl._makeSelect(mTest.getSqlOp());
		System.out.println(block);
		
		
		SqlExprSerializer exprSerializer = new SqlExprSerializerPostgres(da);
		
		SqlOpSerializer serializer = new SqlOpSerializerImpl(exprSerializer);
		
		String sqlQueryString = serializer.serialize(block);
		
		System.out.println(sqlQueryString);
		
		//SqlSelectBlock x;
		
		//ViewDefinition vd = new ViewDefinition(name, );
		
	}
}
