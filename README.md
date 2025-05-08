# Comp 520 Compiler Project
My implementation of the COMP 520 (Compilers) compiler assignment taught by Syed Ali during the spring semester of 2024 at UNC. The "PA" branches checkpoint what I submitted for each part of the assignment.

<details>
<summary>Original README submitted as part of PA5 requirements</summary>

---
# Syntactic Analysis

Syntactic analysis is in package `miniJava.SyntacticAnalyzer`. `Scanner` represents a stream of tokens. It's `scan()` method returns the next token in the file until it reaches the end of file upon which it will return `TokenType.EOT` forever.

To determine tokens, the tokens are divided into three categories: single char, double char, and keyword. All tokens within the same category are handled the same way, just with different characters.

To handle the `UnOp` token, we violate the separation of scanner and parser and use knowledge of the grammar to determine when `-` is a unary vs. binary.

# AST Generation

`Parser` contains a `Scanner` and uses recursive descent to check the grammar of the tokens. ASTs are built along the way during recursive descent.

# Contextual Analysis

Contextual analysis is done in one traversal. An `Identifier` field was added to `IdRef` and `TypeDenoter` field to `Expression`. The assumption upheld for visitor methods is that visiting a node should associate all `IdRef`s in its descendants with an `Identifier` and all `Expression`s in its descendants with a `TypeDenoter`. Note that `TypeDenoter` here does not represent anything in the source code but rather is used to check type equality with other expressions for type checking.

The general strategy for a visit method is to visit child nodes, then use the `Identifier` and `TypeDenoter` fields to perform both identification and type checking. The `Environment` class keeps track of all state between visitor methods including identifiers and class names currently in scope.

# Code Generation

Static variables were put at the beginning of the stack. Register `R15` is used to keep track of the original `RBP`. For code generation, an offset field was added to AST declarations. It has a different meaning depending on the type of declaration. For `MethodDecl`, it is the code offset. For `LocalDecl`, it is negative offset from `RBP`. For non-static `MemberDecl`, it is offset from the memory address of the object. For static `MemberDecl`, it is offset from the stack bottom. For `ParamDecl`, it is postive offset from `RBP`.

Visiting a reference places its address on the stack and visiting an expression puts its value on the stack. Any code that visits a reference or expression should immediately pop the stack into a register.

For any `call` instructions, a patch list is maintained that contains all the method code locations to be patched in later. 8 bytes are greedily allocated for any type of data value. Immediates are not optimized to reduce instruction size (for example using imm32 when only imm8 is needed).

---
</details>
