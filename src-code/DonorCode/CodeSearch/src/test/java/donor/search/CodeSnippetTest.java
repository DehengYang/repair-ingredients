package donor.search;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import donor.search.CodeSnippet;
import donor.search.FileUtils;
import donor.search.LocalLog;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CodeSnippetTest extends TestCase{
	private static Logger log = Logger.getLogger(CodeSnippetTest.class.getName());
	
	public CodeSnippetTest( String testName )
    {
        super( testName );
    }
	
	// test
	public void test_simple_extend(){
		log.info("test_simple_extend");
		String line_path = "/home/apr/d4j/Chart/Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
//		int lineNo = 1797;
		int lineNo = 860; // 188 250 293 296
		CompilationUnit unit = (CompilationUnit)FileUtils.genASTFromFile(line_path, ASTParser.K_COMPILATION_UNIT);
		CodeSnippet codeSnippet = new CodeSnippet(unit, lineNo, 5, null, 3, 5);
		List<ASTNode> astnodes = codeSnippet.getASTNodes();
		
		log.info("print astnodes");
		for (ASTNode astnode : astnodes){
			LocalLog.log(astnode.toString());
		}
	}
}
