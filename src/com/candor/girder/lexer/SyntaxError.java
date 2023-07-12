package com.candor.girder.lexer;

public class SyntaxError extends LexerException {
    public SyntaxError(String errorMessage, int line, int column, String filePath) {
        super("Syntax error at " + line + ":" + column + " in " + filePath + ". " + errorMessage);
    }

    public SyntaxError(String errorMessage) {
        super("Syntax error at unknown location (this is not a good sign, good luck out there <3). " + errorMessage);
    }
}
