/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.jsc;

import java.io.*;
import java.util.*;

import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.*;
import org.mozilla.javascript.optimizer.ClassCompiler;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.ToolErrorReporter;

/**
 * @author Norris Boyd
 */
public class Main {

    /**
     * Main entry point.
     *
     * Process arguments as would a normal Java program.
     * Then set up the execution environment and begin to
     * compile scripts.
     */
    public static void main(String args[])
    {
        Main main = new Main();
        String filename[] = {"F:/奺庬帒椏/benchmark.js"};
        
        if (!main.reporter.hasReportedError()) {
            main.processSource(filename);
        }
    }
	
	/*public static void main(String args[])
    {
        Main main = new Main();
        String filename = "F:/奺庬帒椏/test.js";
        File f = new File(filename);
        String source = main.readSource(f);
        Parser p = new Parser(main.compilerEnv);
        Node ast = p.parse(source, filename, 1);
        Node child = ast.getFirstChild();
        while(child != null) {
        	if(child.getType() == Token.FUNCTION) {
        		FunctionNode fnode = (FunctionNode)child;
        		//get function name
        		System.out.println(fnode.getName());
        		//move to function body 
        		AstNode fbody = fnode.getBody();
        		Node fc = fbody.getFirstChild();
        		while(fc != null) {
        			System.out.println(Token.typeToName(fc.getType()));
        			//get while body
        			if(fc.getType() == Token.WHILE) {
        				WhileLoop wl = (WhileLoop)fc;
        				AstNode condition = wl.getCondition();
        				AstNode left = ((InfixExpression)condition).getLeft();
        				String operator = Token.typeToName(((InfixExpression)condition).getOperator());
        				AstNode right = ((InfixExpression)condition).getRight();
        				System.out.println(((Name)left).getIdentifier());
        				System.out.println(operator);
        				System.out.println(((NumberLiteral)right).getNumber());
        				Node wlbody = wl.getBody().getFirstChild();
        				System.out.println(Token.typeToName(wlbody.getType()));
        				AstNode exp = ((ExpressionStatement)wlbody).getExpression();
        				left = ((InfixExpression)exp).getLeft();
        				right = ((InfixExpression)exp).getRight();
        				System.out.print(((Name)left).getIdentifier());
        				operator = Token.typeToName(((InfixExpression)exp).getOperator());
        				System.out.print(operator);
        				if(right instanceof InfixExpression) {
        					left = ((InfixExpression)right).getLeft();
        					operator = Token.typeToName(((InfixExpression)right).getOperator());
            				right = ((InfixExpression)right).getRight();
            				System.out.print(((Name)left).getIdentifier());
            				System.out.print(operator);
            				System.out.print(((NumberLiteral)right).getNumber());
            				System.out.println();
        				}
        			}
        			if(fc.getType() == Token.BLOCK) {
        				
        				System.out.println("block");
        			}
        			//get returnName test
        			if(fc.getType() == Token.RETURN) {
        				AstNode rex = ((ReturnStatement)fc).getReturnValue();
        				System.out.println(((Name)rex).getIdentifier());
        			}
        			fc = fc.getNext();
        		}		
        	}
        	child = child.getNext();
        }
    }
    */
    public Main()
    {
        reporter = new ToolErrorReporter(true);
        compilerEnv = new CompilerEnvirons();
        compilerEnv.setErrorReporter(reporter);
        compiler = new ClassCompiler(compilerEnv);
    }

    /**
     * Parse arguments.
     *
     */
    public String[] processOptions(String args[])
    {
        targetPackage = "";        // default to no package
        compilerEnv.setGenerateDebugInfo(false);   // default to no symbols
        for (int i=0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                int tail = args.length - i;
                if (targetName != null && tail > 1) {
                    addError("msg.multiple.js.to.file", targetName);
                    return null;
                }
                String[] result = new String[tail];
                for (int j = 0; j != tail; ++j) {
                    result[j] = args[i + j];
                }
                return result;
            }
            if (arg.equals("-help") || arg.equals("-h")
                || arg.equals("--help"))
            {
                printHelp = true;
                return null;
            }

            try {
                if (arg.equals("-version") && ++i < args.length) {
                    int version = Integer.parseInt(args[i]);
                    compilerEnv.setLanguageVersion(version);
                    continue;
                }
                if ((arg.equals("-opt") || arg.equals("-O"))  &&
                    ++i < args.length)
                {
                    int optLevel = Integer.parseInt(args[i]);
                    compilerEnv.setOptimizationLevel(optLevel);
                    continue;
                }
            }
            catch (NumberFormatException e) {
                badUsage(args[i]);
                return null;
            }
            if (arg.equals("-nosource")) {
                compilerEnv.setGeneratingSource(false);
                continue;
            }
            if (arg.equals("-debug") || arg.equals("-g")) {
                compilerEnv.setGenerateDebugInfo(true);
                continue;
            }
            if (arg.equals("-main-method-class") && ++i < args.length) {
                compiler.setMainMethodClass(args[i]);
                continue;
            }
            if (arg.equals("-encoding") && ++i < args.length) {
                characterEncoding = args[i];
                continue;
            }
            if (arg.equals("-o") && ++i < args.length) {
                String name = args[i];
                int end = name.length();
                if (end == 0
                    || !Character.isJavaIdentifierStart(name.charAt(0)))
                {
                    addError("msg.invalid.classfile.name", name);
                    continue;
                }
                for (int j = 1; j < end; j++) {
                    char c = name.charAt(j);
                    if (!Character.isJavaIdentifierPart(c)) {
                        if (c == '.') {
                            // check if it is the dot in .class
                            if (j == end - 6 && name.endsWith(".class")) {
                                name = name.substring(0, j);
                                break;
                            }
                        }
                        addError("msg.invalid.classfile.name", name);
                        break;
                    }
                }
                targetName = name;
                continue;
            }
            if (arg.equals("-observe-instruction-count")) {
                compilerEnv.setGenerateObserverCount(true);
            }
            if (arg.equals("-package") && ++i < args.length) {
                String pkg = args[i];
                int end = pkg.length();
                for (int j = 0; j != end; ++j) {
                    char c = pkg.charAt(j);
                    if (Character.isJavaIdentifierStart(c)) {
                        for (++j; j != end; ++j) {
                            c = pkg.charAt(j);
                            if (!Character.isJavaIdentifierPart(c)) {
                                break;
                            }
                        }
                        if (j == end) {
                            break;
                        }
                        if (c == '.' && j != end - 1) {
                            continue;
                        }
                    }
                    addError("msg.package.name", targetPackage);
                    return null;
                }
                targetPackage = pkg;
                continue;
            }
            if (arg.equals("-extends") && ++i < args.length) {
                String targetExtends = args[i];
                Class<?> superClass;
                try {
                    superClass = Class.forName(targetExtends);
                } catch (ClassNotFoundException e) {
                    throw new Error(e.toString()); // TODO: better error
                }
                compiler.setTargetExtends(superClass);
                continue;
            }
            if (arg.equals("-implements") && ++i < args.length) {
                // TODO: allow for multiple comma-separated interfaces.
                String targetImplements = args[i];
                StringTokenizer st = new StringTokenizer(targetImplements,
                                                         ",");
                List<Class<?>> list = new ArrayList<Class<?>>();
                while (st.hasMoreTokens()) {
                    String className = st.nextToken();
                    try {
                        list.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new Error(e.toString()); // TODO: better error
                    }
                }
                Class<?>[] implementsClasses = list.toArray(new Class<?>[list.size()]);
                compiler.setTargetImplements(implementsClasses);
                continue;
            }
            if (arg.equals("-d") && ++i < args.length) {
                destinationDir = args[i];
                continue;
            }
            badUsage(arg);
            return null;
        }
        // no file name
        p(ToolErrorReporter.getMessage("msg.no.file"));
        return null;
    }
    /**
     * Print a usage message.
     */
    private static void badUsage(String s) {
        System.err.println(ToolErrorReporter.getMessage(
            "msg.jsc.bad.usage", Main.class.getName(), s));
    }

    /**
     * Compile JavaScript source.
     *
     */
    public void processSource(String[] filenames)
    {
        for (int i = 0; i != filenames.length; ++i) {
            String filename = filenames[i];
            if (!filename.endsWith(".js")) {
                addError("msg.extension.not.js", filename);
                return;
            }
            File f = new File(filename);
            String source = readSource(f);
            if (source == null) return;

            
            
            Parser p = new Parser(compilerEnv);
            Node ast = p.parse(source, filename, 1);
            FlowTransformer ftf = new FlowTransformer(compilerEnv);
            List<AstNode> result = ftf.NodeTransformer(ast);
           
            
            String encodedSource;

            File targetTopDir = null;
            if (destinationDir != null) {
                targetTopDir = new File(destinationDir);
            } else {
                String parent = f.getParent();
                if (parent != null) {
                    targetTopDir = new File(parent);
                }
            }
            String newFileName = "testThread.js";
                File outfile = getOutputFile(targetTopDir, newFileName);
                try {
                    FileOutputStream os = new FileOutputStream(outfile);
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    bw = new BufferedWriter(osw);

                    try {
                    	for(int j=0;j<result.size();j++){
                    		encodedSource = result.get(j).toSource();
                    		bw.write(encodedSource);
                    		bw.newLine();
                    		
                    	}
                    } finally {
                    	bw.close();
                    	osw.close();
                        os.close();
                    }
                } catch (IOException ioe) {
                    addFormatedError(ioe.toString());
                }
            
        }
    }
    BufferedWriter bw;
   public void printNodeChildren(AstNode root) throws IOException {    
	   if(root.getType() == Token.FUNCTION) {
			String Name = ((FunctionNode) root).getName();
			List<AstNode> args = ((FunctionNode) root).getParams();
			bw.write("function " + Name + "(");
			for(int i = 0;i < args.size();i++) {
				bw.write(args.get(i).toSource());
				if(i < args.size() - 1)
					bw.write(",");
			}
			bw.write(") {\r\n");
			
			AstNode fbody = ((FunctionNode)root).getBody();
			Node child = fbody.getFirstChild();
			while(child != null) {
				printNodeChildren((AstNode)child);
				child = child.getNext();
				if(child != null)
					child = child.getNext();
			}
			bw.write("}");
			bw.newLine();
		}
		else if(root.getType() == Token.IF) {
			AstNode ifbody = ((IfStatement)root).getThenPart();
			Node child = ifbody.getFirstChild();
			while(child != null) {
				printNodeChildren((AstNode)child);
				child = child.getNext();
			}
			ifbody = ((IfStatement)root).getElsePart();
			if(ifbody != null) {
				child = ifbody.getFirstChild();
				while(child != null) {
					printNodeChildren((AstNode)child);
					child = child.getNext();
				}
			}
		}
		
		else {
			bw.write(root.toSource());
			bw.newLine();
			
			
		}
    }
   
   /*if(root.getType() == Token.FUNCTION) {
    		AstNode fbody = ((FunctionNode)root).getBody();
    		Node child = fbody.getFirstChild();
    		while(child != null) {
    			child = printNodeChildren((AstNode)child);
    			child = child.getNext();
    			if(child != null)
    				child = child.getNext();
    		}
    	}
    	else if(root.getType() == Token.IF) {
    		AstNode ifbody = ((IfStatement)root).getThenPart();
    		Node child = ifbody.getFirstChild();
    		while(child != null) {
    			child = printNodeChildren((AstNode)child);
    			child = child.getNext();
    		}
    		ifbody = ((IfStatement)root).getElsePart();
    		if(ifbody != null) {
    			child = ifbody.getFirstChild();
    			while(child != null) {
    				child = printNodeChildren((AstNode)child);
    				child = child.getNext();
    			}
    		}
    	}
    	
    	else {
    		Name enter = new Name();
    		enter.setIdentifier("\r\n");
    		root.addChildToBack((Node)enter);
    		
    	}
    	return root;*/
    public String readSource(File f)
    {
        String absPath = f.getAbsolutePath();
        if (!f.isFile()) {
            addError("msg.jsfile.not.found", absPath);
            return null;
        }
        try {
            return (String)SourceReader.readFileOrUrl(absPath, true,
                    characterEncoding);
        } catch (FileNotFoundException ex) {
            addError("msg.couldnt.open", absPath);
        } catch (IOException ioe) {
            addFormatedError(ioe.toString());
        }
        return null;
    }

    private File getOutputFile(File parentDir, String className)
    {
        File f = new File(parentDir, className);
        String dirPath = f.getParent();
        if (dirPath != null) {
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return f;
    }


    /**
     * Verify that class file names are legal Java identifiers.  Substitute
     * illegal characters with underscores, and prepend the name with an
     * underscore if the file name does not begin with a JavaLetter.
     */

    String getClassName(String name) {
        char[] s = new char[name.length()+1];
        char c;
        int j = 0;

        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            s[j++] = '_';
        }
        for (int i=0; i < name.length(); i++, j++) {
            c = name.charAt(i);
            if ( Character.isJavaIdentifierPart(c) ) {
                s[j] = c;
            } else {
                s[j] = '_';
            }
        }
        return (new String(s)).trim();
     }

    private static void p(String s) {
        System.out.println(s);
    }

    private void addError(String messageId, String arg)
    {
        String msg;
        if (arg == null) {
            msg = ToolErrorReporter.getMessage(messageId);
        } else {
            msg = ToolErrorReporter.getMessage(messageId, arg);
        }
        addFormatedError(msg);
    }

    private void addFormatedError(String message)
    {
        reporter.error(message, null, -1, null, -1);
    }

    private boolean printHelp;
    private ToolErrorReporter reporter;
    private CompilerEnvirons compilerEnv;
    private ClassCompiler compiler;
    private String targetName;
    private String targetPackage;
    private String destinationDir;
    private String characterEncoding;
}
