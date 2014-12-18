package org.mozilla.javascript;
import org.mozilla.javascript.ast.*; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


import java.io.*;

public class FlowTransfer {
	CompilerEnvirons compilerEnv;
    private ErrorReporter errorReporter;
    private IdeErrorReporter errorCollector;
    
    public FlowTransfer(CompilerEnvirons compilerEnv) {
        this(compilerEnv, compilerEnv.getErrorReporter());
    }

    public FlowTransfer(CompilerEnvirons compilerEnv, ErrorReporter errorReporter) {
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
    public Yield CreateYieldStatement(AstNode expr) {
    	Yield result = new Yield();
    	result.setValue(expr);
    	return result;
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
    
    public ExpressionStatement CreateGeneratorCall(AstNode name,List<AstNode> arguments) {
    	//still required to discuss whether use the ExpressionStatement 
    	AstNode right = new ExpressionStatement(CreateFunctionCallNode(name,arguments),false);
    	AstNode expr =  CreateVariableInitializer(CreateNameNode("g"),right);
    	VariableDeclaration vd = new VariableDeclaration();
    	vd.addVariable((VariableInitializer)expr);
    	ExpressionStatement parentNode = new ExpressionStatement(vd,false);
    	return parentNode;
    }
    
    
    /*the functions which are not implemented or totally implemented*/
    public WhileLoop CreateWhileLoop() {
    	WhileLoop result = new WhileLoop();
    	return result;
    }
    public TryStatement CreateTryStatement() {
    	TryStatement result = new TryStatement();
    	return result;
    }
    public CatchClause CreateCatchClause() {
    	CatchClause result = new CatchClause();
    	return result;
    }
    public List<AstNode> NodeTransfer(Node root) {
    	/*FunctionTransfer();*/
    	List<AstNode> result = new ArrayList<AstNode>();
    	return result;
    }
    public List<AstNode> FunctionTransfer(FunctionNode FNode) {
    	List<AstNode> result = new ArrayList<AstNode>();
    	return result;
    }
    public List<AstNode> StatementTransfer(FunctionNode FNode) {
    	List<AstNode> result = new ArrayList<AstNode>();
    	return result;
    }
}
