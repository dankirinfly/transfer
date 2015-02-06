package org.mozilla.javascript;
import org.mozilla.javascript.ast.*; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


import java.io.*;

public class FlowTransformer {
	CompilerEnvirons compilerEnv;
    private ErrorReporter errorReporter;
    private IdeErrorReporter errorCollector;
    
    public FlowTransformer(CompilerEnvirons compilerEnv) {
        this(compilerEnv, compilerEnv.getErrorReporter());
    }

    public FlowTransformer(CompilerEnvirons compilerEnv, ErrorReporter errorReporter) {
        this.compilerEnv = compilerEnv;
        this.errorReporter = errorReporter;
        if (errorReporter instanceof IdeErrorReporter) {
            errorCollector = (IdeErrorReporter)errorReporter;
        }
    }
    public Name CreateNameNode(String str){
    	Name name = new Name();
    	name.setIdentifier(str);
    	return name;
    }
    public NumberLiteral CreateNumberNode(double num){
    	NumberLiteral result = new NumberLiteral();
    	result.setNumber(num);
    	result.setValue(Double.toString(num));
    	return result;
    }
    //create yield END;
    public ExpressionStatement CreateYieldStatement(AstNode value) {
    	Yield result = new Yield();
    	result.setValue(value);
    	return CreateExpressionStatement((AstNode)result);
    }
    //create yield;
    public ExpressionStatement CreateYieldStatement() {
    	Yield result = new Yield();
    	result.setValue(null);
    	return CreateExpressionStatement((AstNode)result);
    }
    public ExpressionStatement CreateExpressionStatement(AstNode exp) {
    	ExpressionStatement es = new ExpressionStatement();
    	es.setExpression(exp);
    	return es;
    }
    public FunctionCall CreateFunctionCallNode(AstNode target,List<AstNode> arguments){
    	FunctionCall result = new FunctionCall();
    	result.setTarget(target);
    	result.setArguments(arguments);
    	return result;
    }
    public VariableDeclaration CreateVariableDeclaration(List<VariableInitializer> variables){
    	VariableDeclaration result = new VariableDeclaration();
    	result.setVariables(variables);
    	return result;
    }
    public VariableInitializer CreateVariableInitializer(AstNode target,AstNode init){
    	VariableInitializer result = new VariableInitializer();
    	result.setTarget(target);
    	result.setInitializer(init);
    	return result;
    }
    public ExpressionStatement CreateVariableDeInit(AstNode expr) {
    	VariableDeclaration vd = new VariableDeclaration();
    	vd.addVariable((VariableInitializer)expr);
    	ExpressionStatement parentNode = new ExpressionStatement(vd,false);
    	return parentNode;
    }
    public ExpressionStatement CreateGeneratorCall(AstNode fcallNode,List<AstNode> args) {
    	AstNode right = CreateFunctionCallNode(fcallNode,args);
    	AstNode expr =  CreateVariableInitializer(CreateNameNode("g"),right);
    	return CreateVariableDeInit(expr);
    }
    
    
    public AstNode CreateNewBlock(Node oldNode){
    	Block block = new Block();
    	if(oldNode != null){
    		block.addChildrenToBack(oldNode);
    	}
    	return block;
    }

    public InfixExpression CreateInfixExpression(AstNode left,int operator,AstNode right) {
    	InfixExpression ife = new InfixExpression();
    	ife.setLeftAndRight(left, right);
    	ife.setOperator(operator);
    	return ife;
    }
    public InfixExpression CreateInfixExpression(int operator,AstNode right) {
    	InfixExpression ife = new InfixExpression();
    	ife.setLeft(CreateNameNode(""));
    	ife.setRight(right);
    	ife.setOperator(operator);
    	return ife;
    }
    public WhileLoop CreateWhileLoop(AstNode trycatch,AstNode condition) {
    	WhileLoop whileloop = new WhileLoop();
    	Block block = new Block();
    	whileloop.setCondition(condition);
    	block.addChildrenToBack(trycatch);
    	whileloop.setBody(block);
    	return whileloop;
    }
    public TryStatement CreateTryCatchClause(AstNode tryBlock) {
    	TryStatement tryNode = new TryStatement();
    	Block block = new Block();
    	tryNode.setTryBlock(tryBlock);
    	CatchClause catchNode = new CatchClause();
    	catchNode.setVarName(CreateNameNode("e"));
    	catchNode.setBody(block);
    	List<CatchClause> catchList = new ArrayList<CatchClause>();
    	catchList.add(catchNode);
    	tryNode.setCatchClauses(catchList);
    	return tryNode;
    }
    public AstNode CreateTryBody(AstNode nextValue) {
    	Block tryBlock = new Block();
    	AstNode right = CreateNameNode("g.next()");
    	AstNode expr =  CreateVariableInitializer(nextValue,right);
    	AstNode cvd = CreateExpressionStatement(expr);
    	tryBlock.addChildrenToBack(cvd);
    	tryBlock.addChildrenToBack(CreateYieldStatement());
    	return tryBlock;
    }
    public UnaryExpression CreateEnter() {
    	UnaryExpression ue = new UnaryExpression();
    	ue.setOperator(Token.COMMA);
    	return ue;
    }
    //閬嶅巻鍔犳崲琛岀
    public void insertEnter(List<AstNode> result) {
    	for(int i = 0;i < result.size();i++) {
    		if(result.get(i).hasChildren()) {
    			Node child = result.get(i);
    			while(child != null) {
    				
    			}
    		}
    		
    	}
    }
    public List<AstNode> NodeTransformer(Node root) {
    	Node child = root.getFirstChild();
    	List<AstNode> result = new ArrayList<AstNode>();
    	while(child != null) {
    		if(child.getType() == Token.FUNCTION) {
    			List<AstNode> tNodes = FunctionTransformer((FunctionNode)child);
    			result.addAll(tNodes);
    		}
    		else
    			result.add((AstNode)child);
    		child = child.getNext();
    	}
    	
    	return result;
    }
    public void transformBody(AstNode fbody) {
    	Node sn = fbody.getFirstChild();
    	while(sn != null) {
    		switch(sn.getType()) {
    		case Token.EXPR_RESULT:
    		case Token.EXPR_VOID:
    			if(hasFunctionCall(sn)) {
    				transformStatement(fbody,sn);
    				Node pre = fbody.getChildBefore(sn);
    				fbody.removeChild(sn);
    				sn = pre;
    			}	
    			break;
    		case Token.IF:		
    			transformIfStatement((IfStatement)sn);
    			break;
    		case Token.WHILE:
    			transformWhileLoopStatement((WhileLoop)sn);
    			break;
    		case Token.RETURN:
    			ChangeReturnToYield(fbody,sn);
    			break;
    		}
    		
    		sn = sn.getNext();
    	}
    }
    public List<AstNode> FunctionTransformer(FunctionNode fnode) {
    	List<AstNode> result = new ArrayList<AstNode>();
    	AstNode fbody = fnode.getBody();
    	transformBody(fbody);
    	fbody.addChildAfter(CreateYieldStatement(CreateNameNode("END")),fbody.getLastChild());
    	result.add(fnode);
    	return result;
    }
    public void transformStatement(AstNode fbody,Node sn) {	
    	//String gname = null;
    	//String vname = null;
    	List<AstNode> result = new ArrayList<AstNode>();
    	//List<AstNode> args = new ArrayList<AstNode>();
    	AstNode es = ((ExpressionStatement)sn).getExpression();
    	//浠ｅ叆銇倝
    	if(es instanceof Assignment) {
    		result = transformAssignment(es);
    		for(int i = 0;i < result.size();i++) {
    			fbody.addChildBefore(result.get(i), sn);
    		}
    		/*AstNode left = ((Assignment)es).getLeft();
    		AstNode right = ((Assignment)es).getRight();
    		vname = ((Name)left).getIdentifier();
    		gname = ((Name)((FunctionCall)right).getTarget()).getIdentifier();
    		args = ((FunctionCall)right).getArguments();	
    		AstNode tryBlock = CreateTryBody(CreateNameNode(vname),null);
    		AstNode trycatch = CreateTryCatchClause(tryBlock);
        	fbody.addChildBefore(CreateGeneratorCall(CreateNameNode(gname),args),sn); 
        	//result.add(CreateVariableInitializer(CreateNameNode("result"),CreateNumberNode(0)));
        	AstNode ife = CreateInfixExpression(CreateNameNode(""),Token.NOT,CreateNameNode(vname));
        	AstNode whileloop = CreateWhileLoop(trycatch,ife);
        	fbody.addChildBefore(whileloop,sn);*/
    	}
    	//闁㈡暟鍛煎嚭銇椼仾銈�
    	else if(es instanceof FunctionCall) {		
    		result = transformFunctionCall(es);
    		for(int i = 0;i < result.size();i++) {
    			fbody.addChildBefore(result.get(i), sn);
    		}
    	}	
		
    }
    public List<AstNode> transformAssignment(AstNode assign) {
    	List<AstNode> result = new ArrayList<AstNode>();
    	AstNode left = ((Assignment)assign).getLeft();
		AstNode right = ((Assignment)assign).getRight();
		String vname = ((Name)left).getIdentifier();
		String gname = ((Name)((FunctionCall)right).getTarget()).getIdentifier();
		List<AstNode> args = ((FunctionCall)right).getArguments();	
		AstNode tryBlock = CreateTryBody(CreateNameNode(vname));
		AstNode trycatch = CreateTryCatchClause(tryBlock);
    	result.add(CreateGeneratorCall(CreateNameNode(gname),args)); 
    	
    	AstNode whileCondition = CreateInfixExpression(Token.NOT,CreateNameNode(vname));
    	AstNode whileloop = CreateWhileLoop(trycatch,whileCondition);
    	result.add(whileloop);
    	
    	return result;
    }
    public List<AstNode> transformFunctionCall(AstNode functionCall) {
    	List<AstNode> result = new ArrayList<AstNode>();
		AstNode right = functionCall;
		String gname = ((Name)((FunctionCall)right).getTarget()).getIdentifier();
		List<AstNode> args = ((FunctionCall)right).getArguments();	
    	result.add(CreateGeneratorCall(CreateNameNode(gname),args)); 
    	right = CreateNameNode("0");
    	AstNode expr =  CreateVariableInitializer(CreateNameNode("result"),right);
    	AstNode cvd = CreateVariableDeInit(expr);
    	result.add(cvd);
    	AstNode tryBlock = CreateTryBody(CreateNameNode("result"));
		AstNode trycatch = CreateTryCatchClause(tryBlock);
    	AstNode whileCondition = CreateInfixExpression(Token.NOT,CreateNameNode("result"));
    	AstNode whileloop = CreateWhileLoop(trycatch,whileCondition);
    	result.add(whileloop);
    	return result;
    }
    
    public void transformIfStatement(IfStatement ifnode) {
    	AstNode part = ifnode.getThenPart();
    	Node expr = part.getFirstChild();
    	while(expr != null){
    		transformBody(part);
    		expr = expr.getNext();
    	}	
    	part = ifnode.getElsePart();
    	if(part != null) {
    		expr = part.getFirstChild();
    		while(expr != null){
    			transformBody(part);
    			expr = expr.getNext();
    		}
    	}
    }
    public void transformWhileLoopStatement(WhileLoop wlnode) {
    	AstNode body = wlnode.getBody();
    	Node bodyChild = body.getFirstChild();
    	while(bodyChild != null){
    		transformBody(body);
    		bodyChild = bodyChild.getNext();
    	}

    }
    public void ChangeReturnToYield(AstNode fbody,Node returnNode) {
    	AstNode returnValue = ((ReturnStatement)returnNode).getReturnValue();
    	AstNode ys = CreateYieldStatement(returnValue);
    	fbody.addChildAfter(ys, returnNode);
    	fbody.removeChild(returnNode);
    }
    public boolean hasFunctionCall(Node node) {    	
    	switch(node.getType()){
    	case Token.CALL: 
    		return true;
    	case Token.ASSIGN : //=
    	case Token.ASSIGN_ADD : //+=
    	case Token.ASSIGN_SUB : //-=
    	case Token.ASSIGN_MUL : //*=
    	case Token.ASSIGN_DIV : ///=
    	case Token.ASSIGN_MOD : // %=
    		Node expr = ((InfixExpression)node).getLeft();
    		if(hasFunctionCall(expr))return true;
    		expr = ((InfixExpression)node).getRight();
    		if(hasFunctionCall(expr))return true;
    		break;	
    	case Token.EXPR_VOID:
    	case Token.EXPR_RESULT:
    		expr = ((ExpressionStatement)node).getExpression();
    		if(hasFunctionCall(expr)) return true;
    		break;	
    	}
    	return false;
    
    }
    public AstNode hasCallInWhileLoop(WhileLoop whileLoop){
    	Node body = whileLoop.getBody();
    	body = body.getFirstChild();
    	while(body != null){
    		if(hasFunctionCall(body)) return (AstNode)body;
    		body = body.getNext();
    	}
    	return null;
    }

}
