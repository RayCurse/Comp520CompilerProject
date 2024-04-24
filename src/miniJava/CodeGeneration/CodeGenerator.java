package miniJava.CodeGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

public class CodeGenerator implements Visitor<Object, Object> {
    public List<String> errorMessages = new ArrayList<String>();
	private InstructionList asm; // our list of instructions that are used to make the code section
    private MethodDecl mainMethodDecl;
    private MethodDecl printlnMethodDecl;
    private FieldDecl outFieldDecl;
    private Map<Integer, Declaration> callMethodPatches = new HashMap<Integer, Declaration>();

    private int mainMethodAddr = 0;
	
	public CodeGenerator() {}
	
	public void parse(Package AST, MethodDecl mainMethodDecl, MethodDecl printlnMethodDecl, FieldDecl outFieldDecl) {
		asm = new InstructionList();
        this.mainMethodDecl = mainMethodDecl;
        this.printlnMethodDecl = printlnMethodDecl;
        this.outFieldDecl = outFieldDecl;
		
		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.
		
		// Simple operations:
		// asm.add( new Push(0) ); // push the value zero onto the stack
		// asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new ModRMSIB( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		
        // Generate code
		AST.visit(this,null);

        // Patch method calls
        for (Map.Entry<Integer, Declaration> entry : callMethodPatches.entrySet()) {
            int currentIdx = entry.getKey();
            int currentAddr = asm.get(currentIdx).startAddress;
            int destinationIdx = entry.getValue().offset;
            int destinationAddr = asm.get(destinationIdx).startAddress;

            asm.patch(currentIdx, new Call(currentAddr, destinationAddr));
        }

		// Output the file "a.out" if no errors
        if (errorMessages.isEmpty()) {
			makeElf("a.out");
        }
    }
	
	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(errorMessages, asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, asm.getBytes(), 0); // COMPLETED: set the location of the main method
	}
	
	private int makeMalloc() {
		int idxStart = asm.add( new Mov_rmi(new ModRMSIB(Reg64.RAX,true),0x09) ); // mmap
		
		asm.add( new Xor(		new ModRMSIB(Reg64.RDI,Reg64.RDI)) 	); // addr=0
		asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RSI,true),0x1000) ); // 4kb alloc
		asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RDX,true),0x03) 	); // prot read|write
		asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R10,true),0x22) 	); // flags= private, anonymous
		asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R8, true),-1) 	); // fd= -1
		asm.add( new Xor(		new ModRMSIB(Reg64.R9,Reg64.R9)) 	); // offset=0
		asm.add( new Syscall() );
		
		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}
	
    // print RBX value, then null byte
	private int makePrintln() {
		// COMPLETED: how can we generate the assembly to println?
		int idxStart = asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX,true), 1));
        asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDI, true), 1));
        asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDX, true), 1));

        asm.add(new Lea(new ModRMSIB(Reg64.RBP, 24, Reg64.RSI)));
        asm.add(new And(new ModRMSIB(Reg64.RBP, 24), 127));

		asm.add(new Syscall());
		return idxStart;
	}

    private int makeSysExit() {
		int idxStart = asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX,true), 60));
        asm.add(new Xor(new ModRMSIB(Reg64.RDI, Reg64.RDI)));
		asm.add(new Syscall());
        return idxStart;
    }

    // Visitor methods
    int currentStackOffset = 1;
	@Override
	public Object visitPackage(Package prog, Object arg) {
        // Add static variables at beginning of stack, set offsets for instance variables
        asm.add(new Mov_rmr(new ModRMSIB(Reg64.R15, Reg64.RSP)));
        int staticVarOffset = 1;
        for (ClassDecl classDecl : prog.classDeclList) {
            int instanceVarOffset = 0;
            for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                if (fieldDecl.isStatic) {
                    asm.add(new Push(0));
                    fieldDecl.offset = staticVarOffset++;
                } else {
                    fieldDecl.offset = instanceVarOffset++;
                }
            }
        }

        // Init some memory for the out _PrintStream obj for System.out.println
        // Otherwise we have a null pointer and seg fault error
        makeMalloc();
        asm.add(new Mov_rmr(new ModRMSIB(Reg64.R15, outFieldDecl.offset*8, Reg64.RAX)));

        // Add main method call, exit afterwards
        int mainMethodCallIdx = asm.add(new Call(0));

        // Output null byte
        asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX,true), 1));
        asm.add(new Xor(new ModRMSIB(Reg64.RDI, Reg64.RDI)));
        asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDX, true), 1));

        asm.add(new Push(0));
        asm.add(new Lea(new ModRMSIB(Reg64.RBP, -8, Reg64.RSI)));

		asm.add(new Syscall());

        // Exit
        makeSysExit();

        // Visit all classes
        for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, null);
        }

        // Patch main method call now that we know where it is
        callMethodPatches.put(mainMethodCallIdx, mainMethodDecl);


		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
        for (MethodDecl methodDecl : cd.methodDeclList) {
            methodDecl.visit(this, null);
        }
        for (FieldDecl fieldDecl : cd.fieldDeclList) {
            fieldDecl.visit(this, null);
        }
        return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
        return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
        md.offset = asm.getNumInstructions();

        // check if this is the main method
        if (md == mainMethodDecl) {
            mainMethodAddr = asm.getSize();
        }

        // write param offsets from RBP
        // offset 0 is old RBP, offset 1 is return address, so starts at 2
        // if instance method, then starts at 3 since first param is "this"
        int parameterOffset = 2;
        if (!md.isStatic) { parameterOffset = 3; }
        for (ParameterDecl parameterDecl : md.parameterDeclList) {
            parameterDecl.offset = parameterOffset++;
            parameterDecl.visit(this, null);
        }

        // Init stack frame
        asm.add(new Push(Reg64.RBP));
        asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
        currentStackOffset = 1;

        // Check if this is println
        if (md == printlnMethodDecl) {
            makePrintln();
        }

        // Visit statements
        for (Statement statement : md.statementList) {
            statement.visit(this, null);
        }

        // De init stack frame
        asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSP, Reg64.RBP)));
        asm.add(new Pop(Reg64.RBP));

        // Add ret
        asm.add(new Ret());

        return null;
	}

	@Override public Object visitParameterDecl(ParameterDecl pd, Object arg) { return null; }

	@Override public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.offset = currentStackOffset++;
        return null;
    }

	@Override public Object visitBaseType(BaseType type, Object arg) { return null; }
	@Override public Object visitClassType(ClassType type, Object arg) { return null; }
	@Override public Object visitArrayType(ArrayType type, Object arg) { return null; }

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        int offsetStart = currentStackOffset; // so we know how much to increment RSP by afterwards
        for (Statement statement : stmt.sl) {
            statement.visit(this, null);
        }

        // remove local vars from stack
        int stackCount = currentStackOffset - offsetStart;
        if (stackCount > 0) {
            asm.add(new Add(new ModRMSIB(Reg64.RSP, true), 8*stackCount));
        }
        currentStackOffset = offsetStart;
        return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        stmt.varDecl.visit(this, null);

        // Assume expression visit puts result on stack
        stmt.initExp.visit(this, null);

        return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {

        // Assume expression result put on stack
        stmt.val.visit(this, null);

        // Assume reference visit puts on stack
        stmt.ref.visit(this, null);

        // Move rax into [rbx]
        asm.add(new Pop(Reg64.RBX));
        asm.add(new Pop(Reg64.RAX));
        asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBX, 0, Reg64.RAX)));

        return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ix.visit(this, null);
        stmt.ref.visit(this, null);
        stmt.exp.visit(this, null);

        asm.add(new Pop(Reg64.RAX));
        asm.add(new Pop(Reg64.RBX));
        asm.add(new Pop(Reg64.RCX));

        // RCX contains index, RBX base of array, RAX the value to be assigned
        asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBX, Reg64.RCX, 8, 0, Reg64.RAX)));
        return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
        MethodDecl method = null;
        if (stmt.methodRef instanceof IdRef) {
            IdRef methodRef = (IdRef) stmt.methodRef;
            method = (MethodDecl) methodRef.id.declaration;
        } else {
            // Cannot be ThisRef, context analysis should have caught that
            // Must be QualRef
            QualRef methodRef = (QualRef) stmt.methodRef;
            method = (MethodDecl) methodRef.id.declaration;
        }

        // Push args in reverse order
        int argCount = stmt.argList.size();
        for (int i = argCount - 1; i >= 0; i--) {
            Expression expr = stmt.argList.get(i);
            expr.visit(this, null);
        }

        // If instance method, instance is an arg too
        if (!method.isStatic) {
            argCount += 1;
            if (stmt.methodRef instanceof IdRef) {
                // Method being called is instance method, is in the same class
                // Therefore, we must currently be in an instance method
                // Pass on the "this" to the called method
                asm.add(new Push(new ModRMSIB(Reg64.RBP, 16))); // push [rbp + 16]
            } else {
                // Instance is somewhere else
                QualRef methodRef = (QualRef) stmt.methodRef;
                Reference instanceRef = methodRef.ref;
                instanceRef.visit(this, null); // this will push *address of address of* instance of our QualRef (not the instance address itself!)
                asm.add(new Pop(Reg64.RAX));
                asm.add(new Push(new ModRMSIB(Reg64.RAX, 0))); // this is what we want, push the value at the reference address which is the instance address
            }
        }

        // Add call instruction, patch in the location of the method afterwards
        int callIdx = asm.add(new Call(0));
        callMethodPatches.put(callIdx, method);

        // Pop off the arguments
        for (int i = 0; i < argCount; i++) {
            asm.add(new Pop(Reg64.RBX));
        }

        return null;
    }

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        stmt.returnExpr.visit(this, null);
        asm.add(new Pop(Reg64.RAX));
        asm.add(new Ret());
        return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        asm.add(new Pop(Reg64.RAX));
        asm.add(new Cmp(new ModRMSIB(Reg64.RAX, true), 0));

        // Jump if condition was false
        int jmpAfterThenIdx = asm.add(new CondJmp(Condition.E, 0, 0, false));
        int jmpAfterThenAddr = asm.get(jmpAfterThenIdx).startAddress;

        // Generate then code
        stmt.thenStmt.visit(this, null);

        // Jump to end of else if there is else code
        int continueIdx = 0;
        int continueJumpAddr = 0;
        if (stmt.elseStmt != null) {
            continueIdx = asm.add(new Jmp(0, 0, false));
            continueJumpAddr = asm.get(continueIdx).startAddress;
        }

        // Patch conditional jump
        asm.patch(jmpAfterThenIdx, new CondJmp(Condition.E, jmpAfterThenAddr, asm.getSize(), false));

        // Generate else code
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);

            // Patch previous jump instructions in the then code that skips the else code
            asm.patch(continueIdx, new Jmp(continueJumpAddr, asm.getSize(), false));
        }
        return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        int loopBeginAddr = asm.getSize();

        // Evaluating condition
        stmt.cond.visit(this, null);
        asm.add(new Pop(Reg64.RAX));
        asm.add(new Cmp(new ModRMSIB(Reg64.RAX, true), 0));

        // End/skip loop if condition false
        int jmpAfterLoopIdx = asm.add(new CondJmp(Condition.E, 0, 0, false));
        int jmpAfterLoopAddr = asm.get(jmpAfterLoopIdx).startAddress;

        // Generate loop code
        stmt.body.visit(this, null);
        asm.add(new Jmp(asm.getSize(), loopBeginAddr, false));

        // Patch jmp that skips the loop
        asm.patch(jmpAfterLoopIdx, new CondJmp(Condition.E, jmpAfterLoopAddr, asm.getSize(), false));

        return null;
	}

    // Visiting an expression should put its value on the stack
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.expr.visit(this, null);
        asm.add(new Pop(Reg64.RAX));
        if (expr.operator.spelling.equals("-")) {
            asm.add(new Neg(new ModRMSIB(Reg64.RAX, true)));
        } else if (expr.operator.spelling.equals("!"))  {
            asm.add(new Not(new ModRMSIB(Reg64.RAX, true)));
        }
        asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.left.visit(this, null);
        expr.right.visit(this, null);
        asm.add(new Pop(Reg64.RBX));
        asm.add(new Pop(Reg64.RAX));
        if (expr.operator.spelling.equals("+")) {
            asm.add(new Add(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
        } else if (expr.operator.spelling.equals("-")) {
            asm.add(new Sub(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
        } else if (expr.operator.spelling.equals("*")) {
            asm.add(new Imul(Reg64.RAX, new ModRMSIB(Reg64.RBX, true)));
        } else if (expr.operator.spelling.equals("/")) {
            asm.add(new Xor(new ModRMSIB(Reg64.RDX, Reg64.RDX)));
            asm.add(new Idiv(new ModRMSIB(Reg64.RBX, true)));
        } else if (expr.operator.spelling.equals("&&")) {
            asm.add(new And(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
        } else if (expr.operator.spelling.equals("||")) {
            asm.add(new Or(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
        } else {
            asm.add(new Xor(new ModRMSIB(Reg64.RCX, Reg64.RCX)));
            asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RBX)));
            if (expr.operator.spelling.equals(">")) {
                asm.add(new SetCond(Condition.GT, Reg8.CL));
            } else if (expr.operator.spelling.equals("<")) {
                asm.add(new SetCond(Condition.LT, Reg8.CL));
            } else if (expr.operator.spelling.equals("==")) {
                asm.add(new SetCond(Condition.E, Reg8.CL));
            } else if (expr.operator.spelling.equals("<=")) {
                asm.add(new SetCond(Condition.LTE, Reg8.CL));
            } else if (expr.operator.spelling.equals(">=")) {
                asm.add(new SetCond(Condition.GTE, Reg8.CL));
            } else if (expr.operator.spelling.equals("!=")) {
                asm.add(new SetCond(Condition.NE, Reg8.CL));
            }
            asm.add(new Mov_rmr(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
        }
        asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        asm.add(new Pop(Reg64.RAX));
        asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, 0, Reg64.RAX)));
        asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ixExpr.visit(this, null);
        expr.ref.visit(this, null);
        asm.add(new Pop(Reg64.RAX));
        asm.add(new Pop(Reg64.RBX));
        asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, 0, Reg64.RAX)));
        asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, Reg64.RBX, 8, 0, Reg64.RAX)));
        asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {

        MethodDecl method = null;
        if (expr.functionRef instanceof IdRef) {
            IdRef methodRef = (IdRef) expr.functionRef;
            method = (MethodDecl) methodRef.id.declaration;
        } else {
            // Cannot be ThisRef, context analysis should have caught that
            // Must be QualRef
            QualRef methodRef = (QualRef) expr.functionRef;
            method = (MethodDecl) methodRef.id.declaration;
        }

        // Push args in reverse order
        int argCount = expr.argList.size();
        for (int i = argCount - 1; i >= 0; i--) {
            Expression argExpr = expr.argList.get(i);
            argExpr.visit(this, null);
        }

        // If instance method, instance "this" is an arg too
        if (!method.isStatic) {
            argCount += 1;
            if (expr.functionRef instanceof IdRef) {
                // Method being called is instance method, is in the same class
                // Therefore, we must currently be in an instance method
                // Pass on the "this" to the called method
                asm.add(new Push(new ModRMSIB(Reg64.RBP, 16))); // push [rbp + 16]
            } else {
                // Instance is somewhere else
                QualRef methodRef = (QualRef) expr.functionRef;
                Reference instanceRef = methodRef.ref;
                instanceRef.visit(this, null); // this will push *address of address of* instance of our QualRef (not the instance address itself!)
                asm.add(new Pop(Reg64.RAX));
                asm.add(new Push(new ModRMSIB(Reg64.RAX, 0))); // this is what we want, push the value at the reference address which is the instance address
            }
        }

        // Add call instruction, patch in the location of the method afterwards
        int callIdx = asm.add(new Call(0));
        callMethodPatches.put(callIdx, method);

        // Pop off the arguments
        for (int i = 0; i < argCount; i++) {
            asm.add(new Pop(Reg64.RBX));
        }

        // Push return value
        asm.add(new Push(Reg64.RAX));

        return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, null);
        return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        makeMalloc();
        asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        makeMalloc();
        asm.add(new Push(Reg64.RAX));
        return null;
	}

    // Visting a reference should result in the address of whatever is being referenced to be put on the stack
	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
        asm.add(new Lea(new ModRMSIB(Reg64.RBP, 16, Reg64.RAX)));
        asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
        // On the stack, decl offset tells negative offset from RBP
        int offset = -8*ref.id.declaration.offset;
        asm.add(new Lea(new ModRMSIB(Reg64.RBP, offset, Reg64.RAX)));
        asm.add(new Push(Reg64.RAX));
        return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, null);
        asm.add(new Pop(Reg64.RAX));
        asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, 0, Reg64.RAX)));

        if (ref.id.declaration instanceof FieldDecl) {
            FieldDecl field = (FieldDecl) ref.id.declaration;
            if (!field.isStatic) {
                int offset = 8*field.offset;
                asm.add(new Lea(new ModRMSIB(Reg64.RAX, offset, Reg64.RAX)));
                asm.add(new Push(Reg64.RAX));
            } else {
                int offset = 8*field.offset;
                asm.add(new Lea(new ModRMSIB(Reg64.R15, offset, Reg64.RAX)));
                asm.add(new Push(Reg64.RAX));
            }
        }
        return null;
	}

	@Override public Object visitIdentifier(Identifier id, Object arg) { return null; }

	@Override public Object visitOperator(Operator op, Object arg) { return null; }

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
        int n = Integer.parseInt(num.spelling);
        asm.add(new Push(n));
        return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        if (bool.spelling.equals("true")) {
            asm.add(new Push(1));
        } else {
            asm.add(new Push(0));
        }
        return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral num, Object arg) {
        asm.add(new Push(0));
        return null;
	}
}
